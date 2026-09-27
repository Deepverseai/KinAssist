import zlib
import struct
import math
import os

def write_png(filename, width, height, rgba_data):
    def chunk(chunk_type, data):
        return struct.pack(">I", len(data)) + chunk_type + data + struct.pack(">I", zlib.crc32(chunk_type + data) & 0xffffffff)

    raw_data = bytearray()
    for y in range(height):
        raw_data.append(0) # filter type none
        raw_data.extend(rgba_data[y * width * 4 : (y + 1) * width * 4])

    png_bytes = b"\x89PNG\r\n\x1a\n"
    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    png_bytes += chunk(b"IHDR", ihdr)
    png_bytes += chunk(b"IDAT", zlib.compress(bytes(raw_data), 9))
    png_bytes += chunk(b"IEND", b"")

    os.makedirs(os.path.dirname(filename), exist_ok=True)
    with open(filename, "wb") as f:
        f.write(png_bytes)

print("PNG writer defined successfully")
