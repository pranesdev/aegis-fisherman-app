"""
make_bathymetry_mbtiles.py

Regenerates app/src/main/assets/bathymetry/bathymetry.mbtiles from a GEBCO GeoTIFF grid.

Usage:
    python3 make_bathymetry_mbtiles.py <input_geotiff> <output_mbtiles> \
        --min-zoom 6 --max-zoom 11

What it does:
    1. Reads the single-band elevation/depth GeoTIFF (EPSG:4326, negative values = below sea
       level, per GEBCO convention).
    2. Colorizes depth -> RGBA using a light-cyan-to-navy ramp, extends sea colors a short
       distance into land (via a nearest-sea-pixel fill) so downsampling near the coastline never
       blends toward black, and makes land fully transparent.
    3. Reprojects into Web Mercator (EPSG:3857) XYZ tiles for the requested zoom range and packs
       them into an MBTiles (SQLite) archive, in TMS row order as the MBTiles spec requires.
    4. Tiles that are fully transparent (pure land, no sea coverage) are dropped - the archive
       only needs to cover water.

If you have GDAL/rasterio available, that's the more robust path (handles arbitrary CRSes and
proper resampling). This script instead uses only numpy + Pillow + scipy, doing the lon/lat <->
Web Mercator math directly, so it also runs in network-restricted / GDAL-less environments -
useful since this is exactly how the bundled archive was produced. If you do have rasterio
installed, swap the two blocks marked below for rasterio.warp.reproject calls; the output should
be numerically equivalent.

Update BathymetryRepository.MIN_ZOOM / MAX_ZOOM in the app if you change --min-zoom/--max-zoom.
"""
import argparse
import io
import math
import os
import sqlite3

import numpy as np
from PIL import Image
from scipy.ndimage import distance_transform_edt

TILE = 256

STOPS_DEPTH_M = np.array([0, 10, 30, 50, 100, 200, 500, 1000, 2000, 4000], dtype=np.float32)
STOPS_RGB = np.array([
    [214, 241, 246],
    [163, 222, 238],
    [116, 202, 232],
    [84, 181, 224],
    [55, 151, 210],
    [36, 118, 189],
    [23, 89, 160],
    [15, 63, 128],
    [9, 42, 95],
    [4, 24, 58],
], dtype=np.float32)


def colorize(elev: np.ndarray, nodata: float | None):
    """depth GeoTIFF band -> (H,W,4) uint8 RGBA, sea-colored with transparent land."""
    land_mask = (elev >= 0)
    if nodata is not None:
        land_mask |= (elev == nodata)
    sea_mask = ~land_mask
    depth = np.clip(-elev, 0, None)

    r = np.interp(depth, STOPS_DEPTH_M, STOPS_RGB[:, 0])
    g = np.interp(depth, STOPS_DEPTH_M, STOPS_RGB[:, 1])
    b = np.interp(depth, STOPS_DEPTH_M, STOPS_RGB[:, 2])
    rgb = np.stack([r, g, b], axis=-1).astype(np.uint8)

    # Extend sea colors into land so downsampling never blends toward black at the coastline.
    _, (iy, ix) = distance_transform_edt(land_mask, return_indices=True)
    rgb_extended = rgb[iy, ix]
    alpha = np.where(sea_mask, 235, 0).astype(np.uint8)
    return np.dstack([rgb_extended, alpha])


def lon_to_xtile(lon: float, n: int) -> float:
    return (lon + 180.0) / 360.0 * n


def lat_to_ytile(lat: float, n: int) -> float:
    lat_rad = math.radians(lat)
    return (1.0 - math.log(math.tan(lat_rad) + 1.0 / math.cos(lat_rad)) / math.pi) / 2.0 * n


def build_mbtiles(rgba: np.ndarray, bounds, min_zoom: int, max_zoom: int, out_path: str):
    west, south, east, north = bounds
    H, W = rgba.shape[:2]

    if os.path.exists(out_path):
        os.remove(out_path)
    conn = sqlite3.connect(out_path)
    cur = conn.cursor()
    cur.execute("CREATE TABLE metadata (name TEXT, value TEXT)")
    cur.execute(
        "CREATE TABLE tiles (zoom_level INTEGER, tile_column INTEGER, tile_row INTEGER, tile_data BLOB)"
    )
    cur.execute("CREATE UNIQUE INDEX tile_index ON tiles (zoom_level, tile_column, tile_row)")
    meta = {
        "name": "AEGIS Bathymetry (GEBCO)",
        "type": "overlay",
        "version": "1.0",
        "description": "Depth-shaded bathymetry overlay derived from a GEBCO grid, for offline use on the AEGIS boat-safety map.",
        "format": "png",
        "bounds": ",".join(str(x) for x in bounds),
        "center": f"{(west + east) / 2},{(south + north) / 2},{min_zoom}",
        "minzoom": str(min_zoom),
        "maxzoom": str(max_zoom),
        "attribution": "GEBCO Compilation Group; AEGIS project",
    }
    cur.executemany("INSERT INTO metadata VALUES (?,?)", list(meta.items()))
    conn.commit()

    total_written = total_blank = 0
    for z in range(min_zoom, max_zoom + 1):
        n = 2 ** z
        x_min = max(0, int(math.floor(lon_to_xtile(west, n))))
        x_max = min(n - 1, int(math.floor(lon_to_xtile(east, n))))
        y_min = max(0, int(math.floor(lat_to_ytile(north, n))))
        y_max = min(n - 1, int(math.floor(lat_to_ytile(south, n))))

        rows_to_insert = []
        for ty in range(y_min, y_max + 1):
            for tx in range(x_min, x_max + 1):
                gx = tx * TILE + np.arange(TILE)
                gy = ty * TILE + np.arange(TILE)
                lon = gx / (n * TILE) * 360.0 - 180.0
                lat = np.degrees(np.arctan(np.sinh(np.pi * (1 - 2 * gy / (n * TILE)))))

                col_idx = np.clip(((lon - west) / (east - west) * W).astype(np.int32), 0, W - 1)
                row_idx = np.clip(((north - lat) / (north - south) * H).astype(np.int32), 0, H - 1)
                in_bounds = (
                    (lon[np.newaxis, :] >= west) & (lon[np.newaxis, :] <= east)
                    & (lat[:, np.newaxis] >= south) & (lat[:, np.newaxis] <= north)
                )

                row_grid, col_grid = np.meshgrid(row_idx, col_idx, indexing="ij")
                tile_rgba = rgba[row_grid, col_grid].copy()
                tile_rgba[..., 3] = np.where(in_bounds, tile_rgba[..., 3], 0)

                if tile_rgba[..., 3].max() == 0:
                    total_blank += 1
                    continue

                img = Image.fromarray(tile_rgba, mode="RGBA")
                buf = io.BytesIO()
                img.save(buf, format="PNG", optimize=True)
                tms_row = (n - 1) - ty
                rows_to_insert.append((z, tx, tms_row, buf.getvalue()))
                total_written += 1

        cur.executemany("INSERT INTO tiles VALUES (?,?,?,?)", rows_to_insert)
        conn.commit()
        print(f"z={z}: wrote {len(rows_to_insert)} tiles")

    conn.close()
    print(f"TOTAL written: {total_written}, skipped blank: {total_blank}")
    print(f"File size MB: {os.path.getsize(out_path) / 1e6:.2f}")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("input_geotiff")
    ap.add_argument("output_mbtiles")
    ap.add_argument("--min-zoom", type=int, default=6)
    ap.add_argument("--max-zoom", type=int, default=11)
    # Only needed if the GeoTIFF's georeferencing can't be read (e.g. no GDAL/rasterio
    # available and Pillow strips the geo tags). Pass explicitly as west south east north.
    ap.add_argument("--bounds", type=float, nargs=4, metavar=("WEST", "SOUTH", "EAST", "NORTH"))
    args = ap.parse_args()

    try:
        import rasterio
        ds = rasterio.open(args.input_geotiff)
        elev = ds.read(1).astype(np.float32)
        nodata = ds.nodata
        b = ds.bounds
        bounds = (b.left, b.bottom, b.right, b.top)
    except ImportError:
        if not args.bounds:
            raise SystemExit("rasterio not available - pass --bounds WEST SOUTH EAST NORTH explicitly")
        Image.MAX_IMAGE_PIXELS = None
        im = Image.open(args.input_geotiff)
        elev = np.array(im).astype(np.float32)
        nodata = None
        bounds = tuple(args.bounds)

    rgba = colorize(elev, nodata)
    build_mbtiles(rgba, bounds, args.min_zoom, args.max_zoom, args.output_mbtiles)


if __name__ == "__main__":
    main()
