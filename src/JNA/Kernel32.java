package JNA;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;

public interface Kernel32 extends StdCallLibrary {

    Kernel32 INSTANCE = (Kernel32) Native.loadLibrary("Kernel32", Kernel32.class);

    Pointer OpenProcess(int dwDesiredAccess, boolean bInheritHandle, int dwProcessId);
    
    boolean WriteProcessMemory(Pointer p, long address, Pointer buffer, int size, IntByReference written);
    
    boolean ReadProcessMemory(Pointer hProcess, long inBaseAddress, Pointer outputBuffer, int nSize, IntByReference outNumberOfBytesRead);

    boolean CloseHandle(Pointer hObject);

    public Pointer GetCurrentProcess();

}
