"""Executable offsets for supported Leon discs (offsets include the EXE header).

SLUS-00421 is fingerprinted: other US revisions must be analysed separately.
PAL offsets preserve the upstream SLES-00972 converters.
"""
from dataclasses import dataclass
from hashlib import sha256
from pathlib import Path
import sys


@dataclass(frozen=True)
class DiscProfile:
    serial: str
    enemy_models: int
    item_names: int
    enemy_pairs: int
    enemy_parts: int
    stage_firsts: int
    common_voice: int
    common_voice_count: int
    stage_voice_pointer: int
    last_enemy_type: int = 0x5F


PAL = DiscProfile("SLES_009.72", 0x83A8C, 0x871D4, 0x8FCCC, 0x90390,
                  0x90910, 0x83038, 19, 0x800C6184)
NTSC_U = DiscProfile("SLUS_004.21", 0x83DDC, 0x8741C, 0x900B8, 0x9077C,
                     0x90CFC, 0x83388, 13, 0x800C66B4, 0x5A)
NTSC_U_SHA256 = "1cd618bc4390db4bd60735b0db1e91534f6134ede56fae454eb580be2aa686ef"


def profile_for(path):
    path = Path(path)
    data = path.read_bytes()
    if data[:8] != b"PS-X EXE" or len(data) < 0x800:
        raise ValueError(f"{path}: not a PS-X executable")
    if sha256(data).hexdigest() == NTSC_U_SHA256:
        return NTSC_U
    if path.name.upper() == PAL.serial:
        return PAL
    raise ValueError(f"{path}: unsupported executable revision; refusing fixed-offset conversion")


def disc_executable(directory):
    directory = Path(directory)
    # SYSTEM.CNF is authoritative when multiple executables are present.
    import re
    boot = (directory / "SYSTEM.CNF").read_text()
    match = re.search(r"BOOT\s*=\s*cdrom:\\([^;\s]+)", boot, re.IGNORECASE)
    if not match:
        raise ValueError("SYSTEM.CNF has no supported BOOT entry")
    serial = match.group(1).upper()
    if serial not in (PAL.serial, NTSC_U.serial):
        raise ValueError(f"Unsupported disc: {serial}")
    path = directory / serial
    profile_for(path)
    return path


if __name__ == "__main__":
    try:
        print(disc_executable(sys.argv[1]))
    except (ValueError, OSError) as error:
        sys.exit(str(error))
