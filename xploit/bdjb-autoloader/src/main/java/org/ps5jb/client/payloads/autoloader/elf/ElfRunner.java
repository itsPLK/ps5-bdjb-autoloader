package org.ps5jb.client.payloads.autoloader.elf;

import org.ps5jb.sdk.core.Library;
import org.ps5jb.sdk.core.Pointer;

public class ElfRunner extends Library implements Runnable {
    private int retVal;
    private final Pointer entryPoint;
    private final Pointer args;

    public ElfRunner(Pointer entryPoint, Pointer args) {
        super(0x2001);
        this.entryPoint = entryPoint;
        this.args = args;
    }

    @Override
    public void run() {
        retVal = (int) call(entryPoint, args.addr());
    }

    public int getReturnValue() {
        return retVal;
    }
}
