package org.ps5jb.client.payloads.autoloader.elf.constants;

public final class MEM {
    public static final short PAGE_SIZE = 0x4000;

    public static final byte PROT_READ = 1;
    public static final byte PROT_WRITE = 2;
    public static final byte PROT_EXEC = 4;

    public static final short MAP_PRIVATE = 0x2;
    public static final short MAP_FIXED = 0x10;
    public static final short MAP_ANONYMOUS = 0x1000;

    private MEM() {
    }

    public static long truncatePage(long addr) {
        return addr & -PAGE_SIZE;
    }

    public static long roundPage(long addr) {
        return (addr + (PAGE_SIZE - 1)) & -PAGE_SIZE;
    }

    public static byte translateProtection(int flags) {
        byte memFlags = 0;
        if ((flags & ELF.PF_X) == ELF.PF_X) {
            memFlags |= PROT_EXEC;
        }
        if ((flags & ELF.PF_R) == ELF.PF_R) {
            memFlags |= PROT_READ;
        }
        if ((flags & ELF.PF_W) == ELF.PF_W) {
            memFlags |= PROT_WRITE;
        }
        return memFlags;
    }
}
