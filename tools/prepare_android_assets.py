"""Stage the user's converted rooms and original artwork for the local Android APK."""
from pathlib import Path
import shutil
import json
import subprocess
import struct
import sys
sys.path.insert(0, str(Path(__file__).resolve().parent))
from imgio import read_ppm
from rdt2json import read_doors
from stage_android_audio import stage_audio
from stage_android_voice import stage_voice

ROOT = Path(__file__).resolve().parents[1]
source = ROOT / "nitrofiles"
target = ROOT / "build/android-assets/re2"
index = (source / "room/index.bin").read_bytes()
room_entries = list(struct.iter_unpack("BB", index[2:]))
if len(room_entries) != struct.unpack_from("<H", index)[0]:
    raise SystemExit("Invalid room index")
background_count = 0
files = [source / "room/index.bin", source / "menu/items.bin", source / "menu/pictures.bin"]
for model in ("leon", "leon_w01", "leon_w02"):
    files += [source / f"model/{model}.bin", source / f"model/{model}_tex.bin"]
files += list((source / "model").glob("em*.bin"))

files += [source / "effect/core.bin", source / "effect/core_fx.bin", source / "sound/enem_pairs.bin"]
for stage, room_id in room_entries:
    prefix = f"room{stage}{room_id:02x}"
    room = source / f"room/{prefix}0.bin"
    data = room.read_bytes()
    if struct.unpack_from("<IH", data) != (0x314D5252, 4):
        raise SystemExit(f"Convert the disc first: invalid {room}")
    files.append(room)
    if stage == 1 and room_id in (0,1,2,3,0x18,0x19,0x1a,0x1b):
        files += list((source / "room").glob(f"{prefix}0_esp.bin"))
        files += list((source / "room").glob(f"{prefix}0_rbj*.bin"))
        files += list((source / "room").glob(f"{prefix}0_blk.bin"))
    objects = source / f"room/{prefix}0_obj.bin"
    if objects.exists(): files.append(objects)
    for camera in range(data[6]):
        name = f"{prefix}_cam{camera:02d}"
        ppm = ROOT / f"build/bg/{name}.ppm"
        if not ppm.is_file() and prefix == "room30c":
            # Original room 30C supplies only camera 0; match the DS fallback.
            ppm = ROOT / "build/bg/room30c_cam00.ppm"
        if not ppm.is_file():
            raise SystemExit(f"Missing original decoded background: {ppm}. Run tools/build_assets.sh first.")
        width, height, rgb = read_ppm(ppm)
        if (width, height) != (320, 240) or len(rgb) != width * height * 3:
            raise SystemExit(f"Invalid original background: {ppm}")
        out = target / f"bg/{name}.rgb"
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_bytes(struct.pack("<4sHH", b"RBA1", width, height) + rgb)
        (target / f"bg/{name}.bin").unlink(missing_ok=True)
        background_count += 1
        mask = source / f"mask/{prefix}_cam{camera:02d}.bin"
        if mask.exists():
            files.append(mask)
for path in files:
    if not path.is_file():
        raise SystemExit(f"Missing converted asset: {path}")
for path in files:
    out = target / path.relative_to(source)
    out.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(path, out)
print(f"Staged {len(files)} local game assets ({sum(p.stat().st_size for p in files):,} bytes)")

print(f"Staged {background_count} original 320×240 RGB backgrounds without resampling or palette reduction")

# Rebuild near/zero-depth mask geometry from the original screen rectangles.
# Old converted files lost these rectangles by collapsing all four corners to
# the camera; copying those files would keep holes in the gun-shop counter.
for stage, room_id in room_entries:
    prefix = f"room{stage}{room_id:02x}"
    metadata_path = ROOT / f"build/room/ROOM{stage}{room_id:02X}0.json"
    metadata = json.loads(metadata_path.read_text())
    # Preserve authored RVD enable bytes even with an older converted cache.
    staged_room = target / f"room/{prefix}0.bin"
    raw = bytearray(staged_room.read_bytes())
    offset = 60 + raw[6] * 28
    assert len(metadata["camera_switches"]) == raw[7]
    for i, zone in enumerate(metadata["camera_switches"]):
        at = offset + i * 20 + 3
        raw[at] = (raw[at] & ~2) | (0 if zone["flag"] else 2)
    staged_room.write_bytes(raw)
    if any(m["depth"] < 8 for c in metadata["cameras"] for m in c["masks"]):
        subprocess.run([sys.executable, "-I", str(ROOT / "tools/mask2ds.py"),
                        str(metadata_path), str(ROOT / f"build/mask/{prefix}"),
                        str(target / f"mask/{prefix}")], check=True, stdout=subprocess.DEVNULL)


# Evaluate the existing door interpreter at NTSC's 60 Hz and retain the original
# geometry, textures, part hierarchy and fades. Tracks contain transforms, not video.
exporter = ROOT / "build/android-tools/door_export"
exporter.parent.mkdir(parents=True, exist_ok=True)
subprocess.run(["cc", "-O2", "-DDOOR_HZ=60", "-Itests/stubs", "-Isource",
                "tests/door_host.c", "source/script.c", "source/inventory.c",
                "-lm", "-o", str(exporter)], cwd=ROOT, check=True)
door_dir = target / "door"
door_dir.mkdir(parents=True, exist_ok=True)
tracks = set()
manifest = []
for stage, room_id in room_entries:
    metadata = json.loads((ROOT / f"build/room/ROOM{stage}{room_id:02X}0.json").read_text())
    # Read raw SCD tails: byte 12 selects DO2; byte 13 selects its variant.
    # Older room JSON omitted byte 12 and labelled byte 13 "door_type".
    init = bytes.fromhex(metadata["init_script_hex"])
    execute = bytes.fromhex(metadata["exec_script_hex"])
    blob = b"\0"*4 + init + execute
    definitions = read_doors(blob, {"init_script": 4 if init else 0,
                                   "exec_script": 4+len(init) if execute else 0})
    for door in definitions:
        if (door["next_stage"], door["next_room"]) not in room_entries:
            raise SystemExit("Door destination missing from room index")
        kind, variant = door["door_file"], door["door_variant"]
        tracks.add((kind, variant))
        manifest.append((stage, room_id, door["aot"], kind, variant))
for kind, variant in sorted(tracks):
    prefix = f"door{kind:02x}"
    for suffix in ("model", "tex"):
        shutil.copyfile(source / f"door/{prefix}_{suffix}.bin", door_dir / f"{prefix}_{suffix}.bin")
    subprocess.run([str(exporter), str(kind), str(variant), str(door_dir / f"{prefix}_{variant:02x}.bin")],
                   cwd=ROOT, check=True, stdout=subprocess.DEVNULL)
(door_dir / "index.bin").write_bytes(struct.pack("<I", len(manifest)) +
                                      b"".join(struct.pack("<5B", *entry) for entry in manifest))
print(f"Staged {len(tracks)} original NTSC door animation tracks for {len(manifest)} doorways")

stage_audio(source, target)

stage_voice(ROOT, target)
