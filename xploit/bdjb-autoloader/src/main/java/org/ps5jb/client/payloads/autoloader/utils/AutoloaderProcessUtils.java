package org.ps5jb.client.payloads.autoloader.utils;

import org.ps5jb.loader.KernelAccessor;
import org.ps5jb.loader.KernelReadWrite;
import org.ps5jb.sdk.core.SdkRuntimeException;
import org.ps5jb.sdk.core.kernel.KernelOffsets;
import org.ps5jb.sdk.core.kernel.KernelPointer;
import org.ps5jb.sdk.include.sys.proc.Process;
import org.ps5jb.sdk.include.sys.ucred.UCred;
import org.ps5jb.sdk.lib.LibKernel;

/**
 * Specialized utility class for the autoloader to perform kernel process patching.
 * Renamed and moved to a unique package to avoid collisions with the SDK's jar library.
 */
public class AutoloaderProcessUtils {
    private final KernelPointer kbaseAddress;
    private final KernelOffsets offsets;
    private final LibKernel libKernel;

    public AutoloaderProcessUtils(LibKernel libKernel) {
        KernelAccessor kernelAccessor = KernelReadWrite.getAccessor(getClass().getClassLoader());
        if (kernelAccessor == null) {
            throw new SdkRuntimeException("Kernel R/W is required to instantiate this class.");
        }

        this.libKernel = libKernel;
        this.kbaseAddress = KernelPointer.valueOf(kernelAccessor.getKernelBase());

        int sw = libKernel.getSystemSoftwareVersion();
        this.offsets = new KernelOffsets(sw);
    }

    public AutoloaderProcessUtils(LibKernel libKernel, KernelPointer kbaseAddress, KernelOffsets offsets) {
        this.libKernel = libKernel;
        this.offsets = offsets;
        this.kbaseAddress = kbaseAddress;
    }

    public int[] getUserGroup(Process proc) {
        UCred ucred = proc.getUserCredentials();
        KernelPointer groups = ucred.getGroups();
        int groupCount = ucred.getNgroups();

        int[] result = new int[6 + groupCount];
        result[0] = ucred.getUid();
        result[1] = ucred.getRuid();
        result[2] = ucred.getSvuid();
        result[3] = groupCount;
        result[4] = ucred.getRgid();
        result[5] = ucred.getSvgid();
        for (int groupIndex = 0; groupIndex < groupCount; ++groupIndex) {
            result[6 + groupIndex] = groups.read4(groupIndex * 4L);
        }

        return result;
    }

    public int[] setUserGroup(Process proc, int[] ids) {
        int[] prevValue = getUserGroup(proc);

        UCred ucred = proc.getUserCredentials();
        ucred.setUid(ids[0]);
        ucred.setRuid(ids[1]);
        ucred.setSvuid(ids[2]);
        ucred.setNgroups(ids[3]);
        ucred.setRgid(ids[4]);

        if (ids.length > 5) {
            ucred.setSvgid(ids[5]);
            KernelPointer groups = ucred.getGroups();
            for (int groupIndex = 0; groupIndex < ids[3]; ++groupIndex) {
                groups.write4(groupIndex * 4L, ids[6 + groupIndex]);
            }
        }

        return prevValue;
    }

    public long[] getPrivs(Process proc) {
        UCred ucred = proc.getUserCredentials();

        long[] result = new long[4];
        result[0] = ucred.getSceAuthId();
        result[1] = ucred.getSceCaps1();
        result[2] = ucred.getSceCaps2();
        result[3] = ucred.getSceAttrs()[3];

        return result;
    }

    public long[] setPrivs(Process proc, long[] privs) {
        long[] prevValue = getPrivs(proc);

        UCred ucred = ucred = proc.getUserCredentials();
        ucred.setSceAuthId(privs[0]);
        ucred.setSceCaps1(privs[1]);
        ucred.setSceCaps2(privs[2]);

        byte[] attrs = ucred.getSceAttrs();
        attrs[3] = (byte) privs[3];
        ucred.setSceAttrs(attrs);

        return prevValue;
    }
}
