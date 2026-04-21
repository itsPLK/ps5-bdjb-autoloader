package org.ps5jb.client.payloads.autoloader.elf.parser;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class ElfProgramHeader {
    private final int type;
    private final int flags;
    private final long offset;
    private final long vaddr;
    private final long filesz;
    private final long memsz;

    public ElfProgramHeader(byte[] data, long headerOffset) {
        ByteBuffer buffer = ByteBuffer.wrap(data, (int) headerOffset, data.length - (int) headerOffset)
                .order(ByteOrder.LITTLE_ENDIAN);

        type = buffer.getInt();
        flags = buffer.getInt();
        offset = buffer.getLong();
        vaddr = buffer.getLong();
        buffer.getLong();
        filesz = buffer.getLong();
        memsz = buffer.getLong();
    }

    public int getType() {
        return type;
    }

    public int getFlags() {
        return flags;
    }

    public long getOffset() {
        return offset;
    }

    public long getVaddr() {
        return vaddr;
    }

    public long getFilesz() {
        return filesz;
    }

    public long getMemsz() {
        return memsz;
    }
}
