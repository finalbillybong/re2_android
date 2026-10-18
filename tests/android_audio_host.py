"""Verify staged WAV payloads are byte-identical to the converted disc PCM."""
from pathlib import Path
import struct
import wave

root = Path(__file__).resolve().parents[1]
staged = root / 'build/android-assets/re2/audio'
count = 0
for index in staged.glob('b*.bin'):
    bank = int(index.stem[1:])
    name = f'room{(bank-1000)//256+1}{(bank-1000)%256:02x}0' if bank >= 1000 else f'enem{bank-100:02d}' if bank >= 100 else ['core00', 'arms01', 'arms02'][bank]
    source = (root / 'nitrofiles/sound' / (name + '.bin')).read_bytes()
    data = index.read_bytes()
    assert len(data) == 2 + 4 * struct.unpack_from('<H', data)[0]
    for low, high, entry in struct.iter_unpack('<BBH', data[2:]):
        lo, hi, rate, offset, size = struct.unpack_from('<BBHII', source, 8 + entry * 12)
        assert (low, high) == (lo, hi)
        with wave.open(str(staged / f'b{bank}_{entry}.wav'), 'rb') as sample:
            assert sample.getnchannels() == 1 and sample.getsampwidth() == 2
            assert sample.getframerate() == rate and sample.getnframes() * 2 == size
            assert sample.readframes(sample.getnframes()) == source[offset:offset + size]
        count += 1
assert count > 0
print(f'Audio: {count} WAV samples match original converted PCM byte for byte, at original rates')
