package org.ps5jb.client.payloads.autoloader.elf.parser;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class ElfRelocation {
    private final long offset;
    private final long info;
    private final long addend;

    private ElfRelocation(long offset, long info, long addend) {
        this.offset = offset;
        this.info = info;
        this.addend = addend;
    }

    public static ElfRelocation fromByteBuffer(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        long relocOffset = buffer.getLong();
        long relocInfo = buffer.getLong();
        long relocAddend = buffer.getLong();
        return new ElfRelocation(relocOffset, relocInfo, relocAddend);
    }

    public long getOffset() {
        return offset;
    }

    public long getAddend() {
        return addend;
    }

    public int getType() {
        return (int) (info & 0xFFFFFFFFL);
    }
}
