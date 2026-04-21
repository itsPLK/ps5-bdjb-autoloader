package org.ps5jb.client.payloads.autoloader.elf.parser;

import org.ps5jb.client.payloads.autoloader.elf.constants.ELF;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class ElfSectionHeader {
    private final int nameOffset;
    private final int type;
    private final long offset;
    private final long size;
    private final long entsize;
    private final ElfRelocation[] relocations;

    public ElfSectionHeader(byte[] data, long headerOffset) {
        ByteBuffer buffer = ByteBuffer.wrap(data, (int) headerOffset, data.length - (int) headerOffset)
                .order(ByteOrder.LITTLE_ENDIAN);

        nameOffset = buffer.getInt();
        type = buffer.getInt();
        buffer.getLong();
        buffer.getLong();
        offset = buffer.getLong();
        size = buffer.getLong();
        buffer.getInt();
        buffer.getInt();
        buffer.getLong();
        entsize = buffer.getLong();

        if (type == ELF.SHT_RELA && entsize > 0) {
            int count = (int) (size / entsize);
            ElfRelocation[] relocs = new ElfRelocation[count];

            ByteBuffer relocBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            relocBuffer.position((int) offset);
            for (int i = 0; i < count; i++) {
                relocs[i] = ElfRelocation.fromByteBuffer(relocBuffer);
            }
            relocations = relocs;
        } else {
            relocations = new ElfRelocation[0];
        }
    }

    public int getNameOffset() {
        return nameOffset;
    }

    public int getType() {
        return type;
    }

    public long getOffset() {
        return offset;
    }

    public ElfRelocation[] getRelocations() {
        return relocations;
    }
}
