"""Inspect ELF LOAD alignment in an APK/AAB; run zipalign separately on APKs.

Usage: python tools/check_native_alignment.py app/build/outputs/apk/release/app-release-unsigned.apk
This checks packaged binaries, not actual execution on a 16 KB device.
"""
import json
import struct
import sys
import zipfile


def inspect(path):
    results = []
    with zipfile.ZipFile(path) as archive:
        for name in archive.namelist():
            if not name.endswith(".so"):
                continue
            blob = archive.read(name)
            if blob[:4] != b"\x7fELF":
                raise ValueError(f"Not an ELF binary: {name}")
            order = "<" if blob[5] == 1 else ">"
            bits = blob[4]
            if bits == 2:
                offset = struct.unpack_from(order + "Q", blob, 32)[0]
                size, count = struct.unpack_from(order + "HH", blob, 54)
                layout = order + "IIQQQQQQ"
            elif bits == 1:
                offset = struct.unpack_from(order + "I", blob, 28)[0]
                size, count = struct.unpack_from(order + "HH", blob, 42)
                layout = order + "IIIIIIII"
            else:
                raise ValueError(f"Unknown ELF class: {name}")
            alignments = []
            for index in range(count):
                header = struct.unpack_from(layout, blob, offset + index * size)
                if header[0] == 1:  # PT_LOAD
                    alignments.append(header[7])
            # 16 KB Android compatibility concerns 64-bit ABIs; report all ABI alignments.
            required = "/arm64-v8a/" in name or "/x86_64/" in name
            valid = bool(alignments) and all(value >= 16384 for value in alignments)
            results.append({"entry": name, "load_alignments": alignments,
                            "requires_16kb": required, "aligned_16kb": valid})
    return results


if __name__ == "__main__":
    result = inspect(sys.argv[1])
    print(json.dumps(result, indent=2))
    sys.exit(1 if any(row["requires_16kb"] and not row["aligned_16kb"] for row in result) else 0)
