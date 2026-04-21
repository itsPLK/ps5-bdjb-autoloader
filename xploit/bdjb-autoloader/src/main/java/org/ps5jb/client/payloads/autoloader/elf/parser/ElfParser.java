package org.ps5jb.client.payloads.autoloader.elf.parser;

import org.ps5jb.client.payloads.autoloader.elf.constants.MEM;

import java.util.ArrayList;
import java.util.List;

public final class ElfParser {
    private final ElfHeader header;
    private final ElfProgramHeader[] programHeaders;
    private final ElfSectionHeader[] sectionHeaders;
    private final long minVaddr;
    private final long maxVaddr;

    public ElfParser(byte[] data) {
        header = new ElfHeader(data);
        programHeaders = parseProgramHeaders(data, header);
        sectionHeaders = parseSectionHeaders(data, header);
        minVaddr = computeMinVaddr(programHeaders);
        maxVaddr = computeMaxVaddr(programHeaders);
    }

    private static ElfProgramHeader[] parseProgramHeaders(byte[] data, ElfHeader elfHeader) {
        ElfProgramHeader[] result = new ElfProgramHeader[elfHeader.getPhNumber()];
        for (short i = 0; i < elfHeader.getPhNumber(); i++) {
            long off = elfHeader.getPhOffset() + ((long) i * elfHeader.getPhEntitySize());
            result[i] = new ElfProgramHeader(data, off);
        }
        return result;
    }

    private static ElfSectionHeader[] parseSectionHeaders(byte[] data, ElfHeader elfHeader) {
        ElfSectionHeader[] result = new ElfSectionHeader[elfHeader.getShNumber()];
        for (short i = 0; i < elfHeader.getShNumber(); i++) {
            long off = elfHeader.getShOffset() + ((long) i * elfHeader.getShEntitySize());
            result[i] = new ElfSectionHeader(data, off);
        }
        return result;
    }

    private static long computeMinVaddr(ElfProgramHeader[] headers) {
        long min = Long.MAX_VALUE;
        for (ElfProgramHeader h : headers) {
            if (h.getVaddr() < min) {
                min = h.getVaddr();
            }
        }
        return MEM.truncatePage(min == Long.MAX_VALUE ? 0 : min);
    }

    private static long computeMaxVaddr(ElfProgramHeader[] headers) {
        long max = 0;
        for (ElfProgramHeader h : headers) {
            long end = h.getVaddr() + h.getMemsz();
            if (end > max) {
                max = end;
            }
        }
        return MEM.roundPage(max);
    }

    public long getElfType() {
        return header.getType();
    }

    public long getElfSize() {
        return maxVaddr - minVaddr;
    }

    public long getElfEntry() {
        return header.getEntry();
    }

    public long getMinVaddr() {
        return minVaddr;
    }

    public ElfProgramHeader[] getProgramHeadersByType(short targetType) {
        List<ElfProgramHeader> matches = new ArrayList<ElfProgramHeader>();
        for (ElfProgramHeader header : programHeaders) {
            if (header.getType() == targetType) {
                matches.add(header);
            }
        }
        return matches.toArray(new ElfProgramHeader[matches.size()]);
    }

    public ElfSectionHeader[] getSectionHeadersByType(short targetType) {
        List<ElfSectionHeader> matches = new ArrayList<ElfSectionHeader>();
        for (ElfSectionHeader header : sectionHeaders) {
            if (header.getType() == targetType) {
                matches.add(header);
            }
        }
        return matches.toArray(new ElfSectionHeader[matches.size()]);
    }
}
