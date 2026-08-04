package jdk.internal.misc;

import java.lang.reflect.Field;
import java.security.ProtectionDomain;

public class Unsafe {
  public static jdk.internal.misc.Unsafe getUnsafe() {
    return null;
  }

  public Class defineClass(String paramString, byte[] paramArrayOfbyte, int paramInt1, int paramInt2, ClassLoader paramClassLoader, ProtectionDomain paramProtectionDomain) {
    return null;
  }
  
  public byte getByte(long address) {
    return 42;
  }

  public short getShort(long address) {
    return 42;
  }

  public int getInt(long address) {
    return 42;
  }

  public long getLong(long address) {
    return 42;
  }

  public long getLong(Object o, long offset) {
    return 42;
  }

  public void putByte(long address, byte x) {}

  public void putShort(long address, short x) {}

  public void putInt(long address, int x) {}

  public void putLong(long address, long x) {}

  public void putObject(Object o, long offset, Object x) {}

  public long objectFieldOffset(Field f) {
    return 42;
  }

  public long allocateMemory(long bytes) {
    return 42;
  }

  public long reallocateMemory(long address, long bytes) {
    return 42;
  }

  public void freeMemory(long address) {}

  public void setMemory(long address, long bytes, byte value) {}

  public void copyMemory(long srcAddress, long destAddress, long bytes) {}
}
