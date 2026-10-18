"""Check converted foreground masks still cover their original screen rectangles."""
from pathlib import Path
import json
import struct
import sys
root = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(root / 'tools'))
from bg2ds import lz77_decompress
from mask2ds import camera_basis

room = json.loads((root / 'build/room/ROOM1010.json').read_text())
zero_depth = 0
for i, camera in enumerate(room['cameras']):
    if not camera['masks']:
        continue
    data = lz77_decompress((root / f'build/android-assets/re2/mask/room101_cam{i:02d}.bin').read_bytes())
    magic, count, height, _, _ = struct.unpack_from('<I4H', data)
    assert magic == 0x314b4d52 and count == len(camera['masks'])
    eye, forward, right, down, screen = camera_basis(camera)
    for j, mask in enumerate(camera['masks']):
        o = 524 + 256 * height + 56 * j
        u, v, w, h, depth = struct.unpack_from('<BB3H', data, o)
        assert [u, v] == mask['src'] and (w, h, depth) == (mask['w'], mask['h'], mask['depth'])
        x, y = mask['dest']
        for k, (sx, sy) in enumerate(((x, y), (x+w, y), (x+w, y+h), (x, y+h))):
            vertex = struct.unpack_from('<3i', data, o+8+12*k)
            delta = [vertex[a]-eye[a] for a in range(3)]
            z = sum(delta[a]*forward[a] for a in range(3))
            assert z > 204.8, (i, j, 'mask clipped/degenerate', z)
            px = 160 + screen * sum(delta[a]*right[a] for a in range(3))/z
            py = 120 + screen * sum(delta[a]*down[a] for a in range(3))/z
            assert abs(px-sx)<2 and abs(py-sy)<2, (i, j, px, py, sx, sy)
        zero_depth += depth == 0
assert zero_depth == 12
print(f'Gun-shop masks: original screen coverage preserved, including {zero_depth} zero-depth counter/foreground pieces')
