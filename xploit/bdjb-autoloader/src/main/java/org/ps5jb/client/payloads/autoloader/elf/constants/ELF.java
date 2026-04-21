package org.ps5jb.client.payloads.autoloader.elf.constants;

public final class ELF {
    public static final byte[] ELF_MAGIC = {0x7F, 0x45, 0x4C, 0x46};

    public static final short ET_EXEC = 2;
    public static final short ET_DYN = 3;

    public static final short PT_LOAD = 1;
    public static final short SHT_RELA = 4;

    public static final int R_X86_64_RELATIVE = 8;

    public static final short PF_X = 1;
    public static final short PF_R = 4;
    public static final short PF_W = 2;

    private ELF() {
    }
}
