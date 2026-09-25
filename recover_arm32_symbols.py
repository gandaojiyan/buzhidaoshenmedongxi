#!/usr/bin/env python3
import struct
from pathlib import Path

FILES = [
    "2__system_app_BBKAccount_lib_arm_libvivoseckey.so",
    "3__system_app_IqooSecure_lib_arm_libvivoseckey.so",
    "4__system_app_BBKAppStore_lib_arm_libvivoseckey.so",
]

LOAD_EXEC = 1
PT_LOAD = 1

def cstr(data, off, limit):
    if off < 0 or off >= limit:
        return None
    end = data.find(b"\x00", off, limit)
    if end < 0:
        return None
    s = data[off:end]
    if not s:
        return ""
    if any(b < 0x20 or b >= 0x7f for b in s):
        return None
    try:
        return s.decode("ascii")
    except UnicodeDecodeError:
        return None

def parse(path):
    data = Path(path).read_bytes()

    print("=" * 80)
    print(path)
    print("=" * 80)
    print(f"size = 0x{len(data):x}")

    if data[:4] != b"\x7fELF":
        print("NOT ELF")
        return

    if data[4] != 1 or data[5] != 1:
        print("not ELF32 little-endian")
        return

    # ELF32 header
    eh = struct.unpack_from("<16sHHIIIIIHHHHHH", data, 0)

    e_phoff = eh[5]
    e_shoff = eh[6]
    e_phentsize = eh[9]
    e_phnum = eh[10]
    e_shentsize = eh[11]
    e_shnum = eh[12]
    e_shstrndx = eh[13]

    print()
    print("ELF header:")
    print(f"  phoff     = 0x{e_phoff:x}")
    print(f"  phnum     = {e_phnum}")
    print(f"  shoff     = 0x{e_shoff:x}")
    print(f"  shentsize = {e_shentsize}")
    print(f"  shnum     = {e_shnum}")
    print(f"  shstrndx  = {e_shstrndx}")

    # Program headers
    exec_loads = []

    print()
    print("Program headers:")

    for i in range(e_phnum):
        off = e_phoff + i * e_phentsize
        if off + 32 > len(data):
            continue

        p_type, p_offset, p_vaddr, p_paddr, p_filesz, p_memsz, p_flags, p_align = \
            struct.unpack_from("<IIIIIIII", data, off)

        print(
            f"  [{i}] type={p_type:#x} "
            f"off={p_offset:#x} "
            f"vaddr={p_vaddr:#x} "
            f"filesz={p_filesz:#x} "
            f"memsz={p_memsz:#x} "
            f"flags={p_flags:#x}"
        )

        if p_type == PT_LOAD and (p_flags & 1):
            exec_loads.append(
                (p_offset, p_offset + p_filesz, p_vaddr, p_vaddr + p_filesz)
            )

    def is_exec_value(v):
        return any(v0 <= v < v1 for _, _, v0, v1 in exec_loads)

    # Section headers: DO NOT trust sh_type.
    sections = []

    print()
    print("Raw section headers:")

    for i in range(e_shnum):
        off = e_shoff + i * e_shentsize
        if off + 40 > len(data):
            continue

        vals = struct.unpack_from("<IIIIIIIIII", data, off)
        sh_name, sh_type, sh_flags, sh_addr, sh_offset, sh_size, \
        sh_link, sh_info, sh_addralign, sh_entsize = vals

        sections.append(vals)

        print(
            f"  [{i}] "
            f"type={sh_type:#x} "
            f"flags={sh_flags:#x} "
            f"addr={sh_addr:#x} "
            f"off={sh_offset:#x} "
            f"size={sh_size:#x} "
            f"link={sh_link} "
            f"info={sh_info} "
            f"align={sh_addralign:#x} "
            f"entsize={sh_entsize:#x}"
        )

    # Find JNI_OnLoad and other important strings.
    needles = [
        b"JNI_OnLoad\x00",
        b"vivo_seckey",
        b"libvivoseckey.so\x00",
    ]

    print()
    print("Known strings:")

    found_jni = []

    for needle in needles:
        pos = 0
        hits = []
        while True:
            p = data.find(needle, pos)
            if p < 0:
                break
            hits.append(p)
            if len(hits) >= 20:
                break
            pos = p + 1

        print(f"  {needle!r}:")
        for p in hits:
            print(f"      file+0x{p:x}")
            if needle.startswith(b"JNI_OnLoad"):
                found_jni.append(p)

    # We observed on the ARM64 file that JNI_OnLoad had dynstr-relative
    # name offset 0x1d. Test the same structure here.
    for jni_file_off in found_jni:
        dynstr_guess = jni_file_off - 0x1d

        print()
        print(f"Candidate dynstr for JNI_OnLoad @ 0x{jni_file_off:x}:")
        print(f"  dynstr start guess = 0x{dynstr_guess:x}")

        if dynstr_guess < 0 or dynstr_guess >= len(data):
            print("  invalid")
            continue

        print("  strings near guessed dynstr:")

        for rel in [0x0, 0x1, 0x10, 0x1d, 0x28b, 0x292, 0x333, 0xabe]:
            p = dynstr_guess + rel
            s = cstr(data, p, len(data))
            if s:
                print(f"    +0x{rel:x} -> {s!r}")

        # Search for Elf32_Sym entries where st_name == 0x1d.
        marker = struct.pack("<I", 0x1d)

        print()
        print("Possible Elf32_Sym entries for JNI_OnLoad:")

        search_start = max(0, dynstr_guess - 0x20000)
        search_end = min(len(data), dynstr_guess)

        hits = []

        p = search_start
        while p + 16 <= search_end:
            if p % 4:
                p += 1
                continue

            if data[p:p+4] != marker:
                p += 4
                continue

            st_name, st_value, st_size, st_info, st_other, st_shndx = \
                struct.unpack_from("<IIIBBH", data, p)

            name = cstr(data, dynstr_guess + st_name, len(data))

            if name != "JNI_OnLoad":
                p += 4
                continue

            plausible = (
                is_exec_value(st_value)
                and st_size < 0x100000
                and st_info in (0x10, 0x11, 0x12, 0x20, 0x21, 0x22)
                and st_shndx < 0x1000
            )

            if plausible:
                hits.append(
                    (p, st_name, st_value, st_size,
                     st_info, st_other, st_shndx)
                )

            p += 4

        if not hits:
            print("  NO plausible JNI_OnLoad symbol found")
            continue

        for h in hits:
            p, st_name, st_value, st_size, st_info, st_other, st_shndx = h

            print(
                f"  symbol_file=0x{p:x} "
                f"nameoff=0x{st_name:x} "
                f"value=0x{st_value:x} "
                f"size=0x{st_size:x} "
                f"info=0x{st_info:x} "
                f"other=0x{st_other:x} "
                f"shndx={st_shndx}"
            )

            # Dump surrounding symbols.
            print("  surrounding symbols:")

            base = p - 16 * 24
            if base < 0:
                base = 0

            for q in range(base, p + 16 * 25, 16):
                if q + 16 > len(data):
                    break

                name_off, value, size, info, other, shndx = \
                    struct.unpack_from("<IIIBBH", data, q)

                name = cstr(
                    data,
                    dynstr_guess + name_off,
                    min(len(data), dynstr_guess + 0x10000)
                )

                if name is None:
                    continue

                if not name:
                    name = "<empty>"

                valid_value = is_exec_value(value)

                print(
                    f"    0x{q:x}: "
                    f"nameoff=0x{name_off:x} "
                    f"value=0x{value:x} "
                    f"size=0x{size:x} "
                    f"info=0x{info:x} "
                    f"shndx={shndx} "
                    f"exec={valid_value} "
                    f"name={name}"
                )

    print()

for f in FILES:
    if Path(f).exists():
        parse(f)
    else:
        print(f"NOT FOUND: {f}")
