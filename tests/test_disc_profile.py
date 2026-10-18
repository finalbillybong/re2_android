"""Run: python3 -m unittest discover -s tests -p 'test_disc_profile.py'.

Set RE2_DISC_DIR to an extracted NTSC-U disc for asset-backed checks.
No disc data is included in the tests.
"""
import os
from pathlib import Path
import struct
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "tools"))
from disc_profile import NTSC_U, PAL, disc_executable, profile_for
from em2ds import model_table, convert
from menu2ds import names
from xa2ds import clip_table, STAGE_BASE


class ProfileTests(unittest.TestCase):
    def test_unknown_us_revision_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / NTSC_U.serial
            path.write_bytes(b"PS-X EXE" + bytes(0x1000))
            with self.assertRaisesRegex(ValueError, "unsupported executable revision"):
                profile_for(path)

    def test_invalid_executable_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / PAL.serial
            path.write_bytes(bytes(0x1000))
            with self.assertRaisesRegex(ValueError, "not a PS-X"):
                profile_for(path)

    def test_stage_pointer_uses_region_and_full_address(self):
        for profile in (PAL, NTSC_U):
            with self.subTest(region=profile.serial):
                blob = bytearray(0x60)
                pointer = STAGE_BASE + 0x54
                # lui v0,8012; addiu v0,v0,a054; lui at,...; sw v0,...(at)
                for high, expected in ((0x800C, [(3, 4, 5), (6, 7, 8)]), (0x800D, [])):
                    struct.pack_into("<4I", blob, 4, 0x3C028012,
                                     0x24420000 | (pointer & 0xFFFF),
                                     0x3C010000 | high,
                                     0xAC220000 | (profile.stage_voice_pointer & 0xFFFF))
                    struct.pack_into("<6H", blob, 0x54, 3, 4, 5, 6, 7, 8)
                    self.assertEqual(clip_table(blob, profile), expected)


@unittest.skipUnless(os.environ.get("RE2_DISC_DIR"), "requires user's extracted disc")
class NtscDiscTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.disc = Path(os.environ["RE2_DISC_DIR"])
        cls.exe_path = disc_executable(cls.disc)
        cls.exe = cls.exe_path.read_bytes()

    def test_identity_and_item_names(self):
        self.assertEqual(profile_for(self.exe_path), NTSC_U)
        items = names(self.exe, NTSC_U)
        self.assertEqual(len(items), 139)
        self.assertEqual(items[:5], ["Knife", "Hand Gun", "Hand Gun", "C. Hand Gun", "Magnum"])

    def test_enemy_models_point_to_real_models(self):
        ems = (self.disc / "PL0/PLD/CDEMD0.EMS").read_bytes()
        entries = model_table(self.exe, NTSC_U)
        self.assertEqual(len(entries), 55)
        for enemy, (tim, emd) in entries.items():
            with self.subTest(enemy=hex(enemy)):
                self.assertLess(emd, len(ems))
                model, texture, sets = convert(ems, tim, emd)
                self.assertTrue(model["animations"])
                self.assertTrue(texture["palettes"])
                self.assertTrue(sets)

    def test_enemy_sound_banks(self):
        sound = (self.disc / "COMMON/SOUND/ENEMSE.VBS").read_bytes()
        start = NTSC_U.enemy_pairs
        self.assertNotIn(0xFF, self.exe[start:start + 144])
        self.assertEqual(self.exe[start + 144], 0xFF)
        for i in range(72):
            sa, a, ha, _, sb, b, hb, _ = struct.unpack_from(
                "<IHBBIHBB", self.exe, NTSC_U.enemy_parts + 16 * i)
            a, b = (a | ha << 16) * 2048, (b | hb << 16) * 2048
            self.assertLessEqual(a + sa, len(sound))
            self.assertLessEqual(b + sb, len(sound))
            self.assertEqual(sound[a + 128:a + 132], b"pBAV")

    def test_music_stage_ranges_match_rooms(self):
        starts = self.exe[NTSC_U.stage_firsts:NTSC_U.stage_firsts + 7]
        self.assertEqual(starts[0], 0)
        self.assertEqual(list(starts), sorted(starts))
        for room in (self.disc / "PL0/RDT").glob("ROOM???0.RDT"):
            stage, number = int(room.stem[4]), int(room.stem[5:7], 16)
            end = starts[stage] if stage < 7 else 141
            self.assertLess(starts[stage - 1] + number, end)

    def test_voice_tables_reference_available_sectors(self):
        common = [struct.unpack_from("<3H", self.exe, NTSC_U.common_voice + 6 * i)
                  for i in range(NTSC_U.common_voice_count)]
        self.assertEqual(len(common), 13)
        sources = [(common, "PL0/VOICE/VOICE00.XAS.raw2352")]
        for stage in range(1, 8):
            overlay = (self.disc / f"COMMON/BIN/STAGE{stage}.BIN").read_bytes()
            entries = clip_table(overlay, NTSC_U)
            self.assertTrue(entries, f"stage {stage} has no voice table")
            sources.append((entries, f"PL0/VOICE/VOICE0{stage}.XAS.raw2352"))
        for entries, path in sources:
            sectors = (self.disc / path).stat().st_size // 2352
            for channel, start, length in entries:
                self.assertLessEqual(channel, 15)
                self.assertLessEqual((start + length) * 16, sectors + 16)


if __name__ == "__main__":
    unittest.main()
