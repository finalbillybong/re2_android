"""Stage the opening-route dialogue directly from original NTSC-U XA at 37,800 Hz."""
from pathlib import Path
import struct
import sys
import wave
sys.path.insert(0, str(Path(__file__).resolve().parent))
from xa2ds import clip_table, clip_samples
from disc_profile import profile_for


def stage_voice(root, target):
    disc = root / 'build/disc1-usa'
    profile = profile_for(disc / 'SLUS_004.21')
    entries = clip_table((disc / 'COMMON/BIN/STAGE1.BIN').read_bytes(), profile)
    xa = (disc / 'PL0/VOICE/VOICE01.XAS.raw2352').read_bytes()
    dest = target / 'voice'
    dest.mkdir(parents=True, exist_ok=True)
    for clip in range(12):
        pcm = clip_samples(xa, *entries[clip])
        if not pcm:
            raise ValueError(f'Empty gun-shop voice clip {clip}')
        with wave.open(str(dest / f'v1_{clip:02d}.wav'), 'wb') as out:
            out.setparams((1, 2, 37800, 0, 'NONE', 'not compressed'))
            out.writeframes(struct.pack(f'<{len(pcm)}h', *pcm))
    print('Staged 12 original opening-route voice clips at 37,800 Hz (no resampling)')

if __name__ == '__main__':
    root = Path(__file__).resolve().parents[1]
    stage_voice(root, root / 'build/android-assets/re2')
