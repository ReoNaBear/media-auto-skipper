"""Generates the Simplified-Chinese-only character table for filter-engine.

A character is listed when ALL of these hold:
  1. it is a hanzi in GB2312 (the Simplified Chinese standard);
  2. Unicode's Unihan data lists a different Traditional form for it (kTraditionalVariant);
  3. it is not in Big5 (the Traditional Chinese standard used in Taiwan);
  4. it is not a common Japanese kanji (JIS X 0208 level 1, about 2,965 kanji; read from EUC-JP).

Characters shared with Traditional Chinese or Japanese (国, 学, 会, 里, 后 ...) are left out on
purpose, so a Japanese or Traditional title made of them is never flagged.

Usage (UTF-8 output):
    python tools/generate_simplified_chars.py path/to/Unihan_Variants.txt
Unihan_Variants.txt comes from https://www.unicode.org/Public/UCD/latest/ucd/Unihan.zip
(Unicode License, permissive). Everything else uses Python's built-in codecs.
"""
import re
import sys


def codepoint(token):
    return chr(int(token[2:], 16))


def unihan_traditional_variants(path):
    """Maps a character to the set of its Traditional variants (kTraditionalVariant)."""
    result = {}
    with open(path, encoding="utf-8") as f:
        for line in f:
            if line.startswith("#") or "\tkTraditionalVariant\t" not in line:
                continue
            code, _, value = line.rstrip("\n").split("\t")
            result[codepoint(code)] = {codepoint(t) for t in re.findall(r"U\+[0-9A-F]+", value)}
    return result


def gb2312_hanzi():
    for row in range(0xB0, 0xF8):  # rows 16-87: level 1 and level 2 hanzi
        for col in range(0xA1, 0xFF):
            try:
                ch = bytes([row, col]).decode("gb2312")
            except UnicodeDecodeError:
                continue
            if "一" <= ch <= "鿿":
                yield ch


def in_codec(ch, codec):
    try:
        ch.encode(codec)
        return True
    except UnicodeEncodeError:
        return False


def japanese_level1():
    chars = set()
    for row in range(0xB0, 0xD0):  # JIS X 0208 rows 16-47 = level 1 kanji
        for col in range(0xA1, 0xFF):
            try:
                chars.add(bytes([row, col]).decode("euc_jp"))
            except UnicodeDecodeError:
                pass
    return chars


def build(variants_path):
    trad = unihan_traditional_variants(variants_path)
    japanese = japanese_level1()
    out = []
    for ch in gb2312_hanzi():
        if not trad.get(ch, set()) - {ch}:
            continue
        if in_codec(ch, "big5") or ch in japanese:
            continue
        out.append(ch)
    return sorted(set(out), key=ord)


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    chars = build(sys.argv[1])
    print(len(chars), "characters", file=sys.stderr)
    print("".join(chars))
