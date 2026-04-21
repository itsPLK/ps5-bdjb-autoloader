package org.ps5jb.client.payloads.autoloader.elf.parser;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class ElfHeader {
    public static final int EI_NIDENT = 16;

    private final byte[] ident = new byte[EI_NIDENT];
    private final short type;
    private final long entry;
    private final long phoff;
    private final long shoff;
    private final short phentsize;
    private final short phnum;
    private final short shentsize;
    private final short shnum;
    private final short shstrndx;

    public ElfHeader(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        buffer.get(ident);
        type = buffer.getShort();
        buffer.getShort();
        buffer.getInt();
        entry = buffer.getLong();
        phoff = buffer.getLong();
        shoff = buffer.getLong();
        buffer.getInt();
        buffer.getShort();
        phentsize = buffer.getShort();
        phnum = buffer.getShort();
        shentsize = buffer.getShort();
        shnum = buffer.getShort();
        shstrndx = buffer.getShort();
    }

    public short getType() {
        return type;
    }

    public long getEntry() {
        return entry;
    }

    public long getPhOffset() {
        return phoff;
    }

    public long getShOffset() {
        return shoff;
    }

    public short getPhEntitySize() {
        return phentsize;
    }

    public short getPhNumber() {
        return phnum;
    }

    public short getShEntitySize() {
        return shentsize;
    }

    public short getShNumber() {
        return shnum;
    }

    public short getShStringIndex() {
        return shstrndx;
    }
}
