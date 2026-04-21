package org.ps5jb.client.payloads.autoloader.elf;

import org.ps5jb.client.payloads.autoloader.FileIO;
import org.ps5jb.client.payloads.autoloader.elf.constants.ELF;
import org.ps5jb.client.payloads.autoloader.elf.constants.MEM;
import org.ps5jb.client.payloads.autoloader.elf.lib.LibKernelExtended;
import org.ps5jb.client.payloads.autoloader.elf.parser.ElfParser;
import org.ps5jb.client.payloads.autoloader.elf.parser.ElfProgramHeader;
import org.ps5jb.client.payloads.autoloader.elf.parser.ElfRelocation;
import org.ps5jb.client.payloads.autoloader.elf.parser.ElfSectionHeader;
import org.ps5jb.client.utils.init.KernelReadWriteUnavailableException;
import org.ps5jb.client.utils.init.SdkInit;
import org.ps5jb.client.payloads.autoloader.utils.AutoloaderProcessUtils;
import org.ps5jb.loader.KernelAccessor;
import org.ps5jb.loader.KernelReadWrite;
import org.ps5jb.loader.Status;
import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.SdkSoftwareVersionUnsupportedException;
import org.ps5jb.sdk.core.kernel.KernelAccessorIPv6;
import org.ps5jb.sdk.core.kernel.KernelOffsets;
import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.include.sys.proc.Process;

import org.ps5jb.sdk.io.File;
import java.io.IOException;
import java.io.InputStream;

public final class GenericElfLoader {
    private final LibKernelExtended libKernel = new LibKernelExtended();
    private AutoloaderProcessUtils procUtils;
    private SdkInit sdk;

    public void loadAndRun(String externalElfPath) throws Exception {
        if (!init()) {
            throw new IllegalStateException("Kernel R/W is unavailable");
        }

        byte[] elfBytes = readElfBytes(externalElfPath);
        validateElfMagic(elfBytes);

        Process curProc = new Process(KernelPointer.valueOf(sdk.curProcAddress));
        patchProcess(curProc);
        if (enableDebug()) {
            Status.println("[+] Debug settings enabled");
        }

        Pointer elfStore = null;
        Pointer mappedBase = null;
        Pointer rwSocketPair = null;
        Pointer payloadOut = null;
        Pointer args = null;
        Pointer rwPipe = null;

        try {
            elfStore = Pointer.malloc(elfBytes.length);
            for (int i = 0; i < elfBytes.length; i++) {
                elfStore.write1(i, elfBytes[i]);
            }

            ElfParser elf = new ElfParser(elfBytes);
            short mmapFlags = (short) (MEM.MAP_PRIVATE | MEM.MAP_ANONYMOUS);
            byte mmapProt = (byte) (MEM.PROT_READ | MEM.PROT_WRITE);

            long baseAddr;
            if (elf.getElfType() == ELF.ET_DYN) {
                baseAddr = 0;
            } else if (elf.getElfType() == ELF.ET_EXEC) {
                baseAddr = elf.getMinVaddr();
                mmapFlags |= MEM.MAP_FIXED;
            } else {
                throw new IllegalArgumentException("Unsupported ELF type: " + elf.getElfType());
            }

            mappedBase = libKernel.mmap(Pointer.valueOf(baseAddr), elf.getElfSize(), mmapProt, mmapFlags, -1, 0);
            if (mappedBase.addr() == -1) {
                throw new IllegalStateException("Failed to map memory for ELF");
            }

            ElfProgramHeader[] loadHeaders = elf.getProgramHeadersByType(ELF.PT_LOAD);
            for (int i = 0; i < loadHeaders.length; i++) {
                ElfProgramHeader ph = loadHeaders[i];
                Pointer dest = mappedBase.inc(ph.getVaddr());
                copySegment(elfStore, dest, ph.getMemsz(), ph.getFilesz(), ph.getOffset());
            }

            ElfSectionHeader[] relaSections = elf.getSectionHeadersByType(ELF.SHT_RELA);
            int relocationCount = 0;
            for (int i = 0; i < relaSections.length; i++) {
                ElfRelocation[] relocs = relaSections[i].getRelocations();
                for (int j = 0; j < relocs.length; j++) {
                    ElfRelocation reloc = relocs[j];
                    if (reloc.getType() == ELF.R_X86_64_RELATIVE) {
                        Pointer relocAddr = mappedBase.inc(reloc.getOffset());
                        long relocVal = mappedBase.addr() + reloc.getAddend();
                        relocAddr.write8(relocVal);
                        relocationCount++;
                    }
                }
            }
            Status.println("[+] Applied relocations: " + relocationCount);

            for (int i = 0; i < loadHeaders.length; i++) {
                ElfProgramHeader ph = loadHeaders[i];
                if (ph.getMemsz() <= 0) {
                    continue;
                }

                Pointer segmentAddr = mappedBase.inc(ph.getVaddr());
                long segmentSize = MEM.roundPage(ph.getMemsz());
                byte memProt = MEM.translateProtection(ph.getFlags());
                if ((ph.getFlags() & ELF.PF_X) == ELF.PF_X) {
                    libKernel.kMprotect(curProc, segmentAddr, memProt);
                } else {
                    libKernel.mprotect(segmentAddr, segmentSize, memProt);
                }
            }

            // ELF arguments
            rwSocketPair = Pointer.malloc(8);
            payloadOut = Pointer.malloc(8);
            args = Pointer.malloc(48); // 8 * 6

            // Get IPv6 Accessor for pipe and socket
            KernelAccessor ka = KernelReadWrite.getAccessor(getClass().getClassLoader());
            KernelAccessorIPv6 ipv6;
            if (ka instanceof KernelAccessorIPv6) {
                ipv6 = (KernelAccessorIPv6) ka;
            } else {
                sdk.restoreNonAgcKernelReadWrite();
                ipv6 = (KernelAccessorIPv6) KernelReadWrite.getAccessor(getClass().getClassLoader());
            }

            // Pipe
            rwPipe = Pointer.malloc(8);
            rwPipe.write4(ipv6.getPipeReadFd());
            rwPipe.write4(4, ipv6.getPipeWriteFd());

            // Pass master/victim pair to payload so it can do read/write
            rwSocketPair.write4(ipv6.getMasterSock());
            rwSocketPair.write4(4, ipv6.getVictimSock());

            // We need getpid, sceKernelDlsym does not work on higher FWs
            Pointer dlsym = libKernel.addrOf("getpid");
            long kdataAddress = sdk.kernelDataAddress;

            args.inc(0x00).write8(dlsym.addr());                 // arg1 = dlsym_t* dlsym
            args.inc(0x08).write8(rwPipe.addr());                // arg2 = int *rwpipe[2]
            args.inc(0x10).write8(rwSocketPair.addr());          // arg3 = int *rwpair[2]
            args.inc(0x18).write8(ipv6.getPipeAddress().addr()); // arg4 = uint64_t kpipe_addr
            args.inc(0x20).write8(kdataAddress);                 // arg5 = uint64_t kdata_base_addr
            args.inc(0x28).write8(payloadOut.addr());            // arg6 = int *payloadout

            Pointer entryPoint = Pointer.valueOf(mappedBase.addr() + elf.getElfEntry());
            ElfRunner runner = new ElfRunner(entryPoint, args);
            
            Status.println("[+] Starting native thread for ELF at " + entryPoint);
            Thread thread = new Thread(runner);
            thread.start();
        } finally {
            if (elfStore != null) {
                elfStore.free();
            }
            // Note: mappedBase and args are NOT freed here because the ELF is running in a background thread
            // and needs this memory to remain resident.
        }
    }

    private boolean init() {
        try {
            try {
                sdk = SdkInit.init(true, false);
            } catch (IllegalStateException alreadyInitialized) {
                sdk = SdkInit.instance();
            }
            procUtils = new AutoloaderProcessUtils(libKernel);
            return true;
        } catch (KernelReadWriteUnavailableException e) {
            Status.println("Kernel R/W is not available");
            return false;
        } catch (SdkSoftwareVersionUnsupportedException e) {
            Status.printStackTrace("Unsupported firmware", e);
            return false;
        }
    }

    private byte[] readElfBytes(String externalElfPath) throws IOException {
        if (externalElfPath != null) {
            File f = new File(externalElfPath);
            if (!f.exists() || !f.isFile()) {
                throw new IOException("ELF file not found: " + externalElfPath);
            }
            return FileIO.readAllBytes(f);
        }

        InputStream in = getClass().getResourceAsStream("/elfldr.elf");
        if (in == null) {
            throw new IOException("Bundled /elfldr.elf resource not found");
        }

        try {
            return FileIO.readAllBytes(in);
        } finally {
            in.close();
        }
    }

    private static void validateElfMagic(byte[] elfBytes) {
        if (elfBytes.length < 4) {
            throw new IllegalArgumentException("ELF data is too short");
        }
        for (int i = 0; i < 4; i++) {
            if (elfBytes[i] != ELF.ELF_MAGIC[i]) {
                throw new IllegalArgumentException("Invalid ELF magic");
            }
        }
    }

    private static void copySegment(Pointer src, Pointer dest, long memSize, long fileSize, long offset) {
        for (long i = 0; i < memSize; i += 8) {
            long qword = (i >= fileSize) ? 0 : src.read8(offset + i);
            dest.write8(i, qword);
        }
    }

    private void patchProcess(Process process) {
        procUtils.setUserGroup(process, new int[] {
                0,
                0,
                0,
                1,
                0
        });

        final long DEVICE_AUTHID = 0x4801000000000013L;
        procUtils.setPrivs(process, new long[] {
                DEVICE_AUTHID,
                0xFFFFFFFFFFFFFFFFL,
                0xFFFFFFFFFFFFFFFFL,
                0x80
        });

        KernelPointer dynlibAddr = process.getDynLib();
        dynlibAddr.write4(0x118, 0);
        dynlibAddr.write8(0x18, 1);
        dynlibAddr.write8(0xF0, 0);
        dynlibAddr.write8(0xF8, -1);
    }

    private boolean enableDebug() {
        boolean appliedPatch = false;
        KernelOffsets offsets = sdk.kernelOffsets;
        KernelPointer kdata = KernelPointer.valueOf(sdk.kernelDataAddress, false);

        sdk.switchToAgcKernelReadWrite(true);

        KernelPointer secFlagsPtr = kdata.inc(offsets.OFFSET_KERNEL_DATA_BASE_SECURITY_FLAGS);
        int secFlagsVal = secFlagsPtr.read4();
        if ((secFlagsVal & 0x14) != 0x14) {
            secFlagsPtr.write4(secFlagsVal | 0x14);
            appliedPatch = true;
        }

        KernelPointer targetIdPtr = kdata.inc(offsets.OFFSET_KERNEL_DATA_BASE_TARGET_ID);
        byte targetId = targetIdPtr.read1();
        if (targetId != (byte) 0x82) {
            targetIdPtr.write1((byte) 0x82);
            appliedPatch = true;
        }

        KernelPointer qaFlagsPtr = kdata.inc(offsets.OFFSET_KERNEL_DATA_BASE_QA_FLAGS);
        long qaFlagsVal = qaFlagsPtr.read8();
        final long qaMask = 0x0000000000010300L;
        if ((qaFlagsVal & qaMask) != qaMask) {
            qaFlagsPtr.write8(qaFlagsVal | qaMask);
            appliedPatch = true;
        }

        KernelPointer uTokenFlagsPtr = kdata.inc(offsets.OFFSET_KERNEL_DATA_BASE_UTOKEN_FLAGS);
        byte uTokenFlagsVal = uTokenFlagsPtr.read1();
        if ((uTokenFlagsVal & 0x1) != 0x1) {
            uTokenFlagsPtr.write1((byte) (uTokenFlagsVal | 0x1));
            appliedPatch = true;
        }

        if (appliedPatch) {
            libKernel.sceKernelSendNotificationRequest("Debug Settings enabled");
        }

        sdk.restoreNonAgcKernelReadWrite();
        return appliedPatch;
    }
}
