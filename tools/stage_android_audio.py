"""Wrap converted original mono PCM samples in WAV containers, without resampling."""
import struct
import wave
from pathlib import Path


def stage_audio(source: Path, target: Path):
    output = target / 'audio'
    output.mkdir(parents=True, exist_ok=True)
    banks = [(0, 'core00'), (1, 'arms01'), (2, 'arms02')]
    banks += [(100 + int(p.stem[4:]), p.stem) for p in sorted((source / 'sound').glob('enem[0-9][0-9].bin'))]
    banks += [(1000 + (int(p.stem[4])-1)*256 + int(p.stem[5:7],16), p.stem)
              for p in sorted((source / 'sound').glob('room*0.bin'))]
    count = 0
    for bank, name in banks:
        data = (source / 'sound' / (name + '.bin')).read_bytes()
        magic, entries, _ = struct.unpack_from('<IHH', data)
        if magic != 0x31534252 or len(data) < 8 + entries * 12:
            raise ValueError(f'Invalid sound bank: {name}')
        index = bytearray()
        for i in range(entries):
            low, high, rate, offset, size = struct.unpack_from('<BBHII', data, 8 + i * 12)
            if not size:
                continue
            if not rate or size % 2 or offset + size > len(data) or size > 1000000:
                raise ValueError(f'Invalid/oversized PCM sample: {name} #{i}')
            with wave.open(str(output / f'b{bank}_{i}.wav'), 'wb') as wav:
                wav.setnchannels(1)
                wav.setsampwidth(2)
                wav.setframerate(rate)
                wav.writeframes(data[offset:offset + size])
            index += struct.pack('<BBH', low, high, i)
            count += 1
        (output / f'b{bank}.bin').write_bytes(struct.pack('<H', len(index) // 4) + index)
    print(f'Staged {count} original PCM sound samples in {len(banks)} banks (no resampling)')
