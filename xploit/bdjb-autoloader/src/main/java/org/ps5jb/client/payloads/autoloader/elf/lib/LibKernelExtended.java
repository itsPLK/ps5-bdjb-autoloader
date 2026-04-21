package org.ps5jb.client.payloads.autoloader.elf.lib;

import org.ps5jb.loader.Status;
import org.ps5jb.sdk.core.Pointer;
import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.include.sys.proc.Process;
import org.ps5jb.sdk.lib.LibKernel;

public final class LibKernelExtended extends LibKernel {
    private final short offsetVmRoot;

    public LibKernelExtended() {
        offsetVmRoot = detectOffsetVmRoot();
    }

    private short detectOffsetVmRoot() {
        switch (getSystemSoftwareVersion()) {
            case 0x0100:
            case 0x0101:
            case 0x0102:
            case 0x0105:
            case 0x0110:
            case 0x0111:
            case 0x0112:
            case 0x0113:
            case 0x0114:
                return 0x1C0;
            case 0x0220:
            case 0x0225:
            case 0x0226:
            case 0x0230:
            case 0x0250:
            case 0x0270:
            case 0x0300:
            case 0x0310:
            case 0x0320:
            case 0x0321:
            case 0x0400:
            case 0x0402:
            case 0x0403:
            case 0x0450:
            case 0x0451:
            case 0x0500:
            case 0x0502:
            case 0x0510:
            case 0x0550:
                return 0x1C8;
            case 0x0600:
            case 0x0602:
            case 0x0650:
            case 0x0700:
            case 0x0701:
            case 0x0720:
            case 0x0740:
            case 0x0760:
            case 0x0761:
                return 0x1D0;
            default:
                Status.println("[!] Unsupported firmware for vm_map traversal");
                return 0;
        }
    }

    public void kMprotect(Process proc, Pointer addr, byte prot) {
        KernelPointer vmMapEntry = proc.getVmSpace().getPointer().pptr(offsetVmRoot);
        while (!KernelPointer.NULL.equals(vmMapEntry)) {
            long start = vmMapEntry.read8(0x20);
            long end = vmMapEntry.read8(0x28);

            if (addr.addr() < start) {
                vmMapEntry = vmMapEntry.pptr(0x10);
            } else if (addr.addr() >= end) {
                vmMapEntry = vmMapEntry.pptr(0x18);
            } else {
                vmMapEntry.write1(0x64, prot);
                vmMapEntry.write1(0x65, prot);
                return;
            }
        }
    }
}
