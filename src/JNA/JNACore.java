package JNA;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Arrays;

public class JNACore {

    private static JNACore instance = null;

    /*
     Access modes
     */
    public final int PROCESS_QUERY_INFORMATION = 0x0400;
    public final int PROCESS_VM_READ = 0x0010;
    public final int PROCESS_VM_WRITE = 0x0020;
    public final int PROCESS_VM_OPERATION = 0x0008;
    public final int PROCESS_ALL_ACCESS = 0x001F0FFF;


    /*
     Zezenia client objects
     */
    public int zezeniaPID;
    public Pointer zezeniaProcessHandle;
    public String exeName = "";
    private int[] processList = new int[512];
    private int[] dummyList = new int[512];
    private Memory pTemp = new Memory(8);
    private Memory toWrite = new Memory(1);
    private IntByReference bytesReturned = new IntByReference();

    /*
     JNACore Constructor
     */
    private JNACore() {
    }

    public static JNACore getInstance() {
        if (instance == null) {
            instance = new JNACore();
        }
        return instance;
    }

    /*
     Finds and sets the zezenia client's pid and a pointer to its process.
     */
    public boolean getProcessesByName(String processName) {
        Psapi.INSTANCE.EnumProcesses(processList, 1024, dummyList);
        int pid;
        int i = 0;
        while (i < processList.length) {
            pid = processList[i];
            if (pid != 0) {
                Pointer ph = Kernel32.INSTANCE.OpenProcess(PROCESS_ALL_ACCESS, false, pid);
                if (ph != null) {
                    try {
                        byte[] byteNameW = new byte[512];
                        byte[] byteName = new byte[512];
                        Psapi.INSTANCE.GetModuleBaseNameW(ph, new Pointer(0), byteNameW, 512);
                        
                        convertWide2Multi(byteNameW, byteName);
                        String findName = new String(byteName);                        
                        exeName = processName;
                        if ( stringCompare(findName, processName) ) {
                        //if (test.contains(processName.toLowerCase())) {
                            zezeniaPID = pid;
                            zezeniaProcessHandle = ph;
                            return true;
                        }
                        Kernel32.INSTANCE.CloseHandle(ph);
                        
                    } catch (Exception ex) {
                        Logger.getLogger(JNACore.class.getName()).log(Level.SEVERE, null, ex);
                        return false;
                    } 
                }
            }
            i++;
        }
        
        return false;
    }

    /*
     Returns a pointer to a process given by a pid.
     */
    public Pointer returnProcess(int pid) {
        Pointer process = Kernel32.INSTANCE.OpenProcess(PROCESS_ALL_ACCESS, false, pid);
        return process;
    }

    /*
     Reads the specified number of bytes in the specified memory location
     of the specified process.
     */
    public Memory readMemory(Pointer process, long address, int bytesToRead) {
        IntByReference read = new IntByReference(0);
        Memory output = new Memory(8);

        boolean ReadProcessMemory = Kernel32.INSTANCE.ReadProcessMemory(process, address, output, bytesToRead, read);
        return output;

    }

    /*
     Writes the specified number of bytes at the specified memory location
     of the specified process.
     */
    public void writeMemory(Pointer process, long address, byte[] data) {
        int size = data.length;

        //i have toWrite size set to 1 byte. if i need to write more than 1 in
        //the future, i will have to change this code.
        for (int i = 0; i < size; i++) {
            toWrite.setByte(i, data[i]);
        }
        IntByReference x = new IntByReference();
        Kernel32.INSTANCE.WriteProcessMemory(process, address, toWrite, size, x);
        if (x.getValue() < 4) {
        }
    }


    /*
     Returns address at the end of a given array of offsets using the base address.
    
     Use Example - readMemory(zezeniaPointer,findDynAddress(zezeniaPointer,xCoord,baseAddress),4)
     -will read the players xCoordinate from the zezenia client.
     */
    public long findDynAddress(int[] offsets, long baseAddress) {
        long address = baseAddress;
        long pointerAddress = 0;

        address = address + offsets[0];
        int i = 1;
        while (i < offsets.length) {
            if (i == 1) {
                boolean ReadProcessMemory = Kernel32.INSTANCE.ReadProcessMemory(zezeniaProcessHandle, address, pTemp, 4, bytesReturned);
            }
            pointerAddress = ((pTemp.getInt(0) + offsets[i]));
            if (i != offsets.length - 1) {
                boolean ReadProcessMemory = Kernel32.INSTANCE.ReadProcessMemory(zezeniaProcessHandle, pointerAddress, pTemp, 4, bytesReturned);
            }
            i++;
            //if pTempt returns 0, that means the value in memory isnt occupied yet
            if (pTemp.getInt(0) == 0) {
                return 0;
            }
        }
        return pointerAddress;
    }

    /*
     Returns the base address of the modules of the given process.
     I'm just using it for debugging.
     */
    public int getBaseAddress() {
        try {
            Pointer hProcess = zezeniaProcessHandle;

            List<JNA.Module> hModules = PsapiTools.getInstance().EnumProcessModules(hProcess);

            for (JNA.Module m : hModules) {
                //if(strFileName.contains(exeName.toLowerCase())){
                System.out.println(m.getFileName() + ": 0x" + Long.toHexString(Pointer.nativeValue(m.getEntryPoint())));
                
                return 1;
                //}                

            }
        } catch (Exception e) {
            System.err.println("Something broke in getbaseaddress method");
            return -1;
        }
        return 0;
    }
    
    public String getStringFromUnicode(String strUnicode) {
        String strResult = "";
        try {
            byte[] byteData = strUnicode.getBytes("UTF-8");
            strResult = new String(byteData, "UTF-16LE");
            
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(JNACore.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return strResult;
    }
    
    public boolean byteCompare(byte[] a, byte[] b, int nSize) {
        boolean bRet = true;
        
        if (a == null || b == null) {
            return false;
        }
        
        int nTempSize = 0;
        if (a.length > b.length) {
            nTempSize = b.length;
        } else {
            nTempSize = a.length;
        }
        
        if (nTempSize < nSize)
            nSize = nTempSize;
        
        for (int i = 0; i < nSize; i++) {
            if (a[i] == b[i]) {
                continue;                
            } else {
                bRet = false;
                return bRet;
            }
        }
        return bRet;
    }
    
    public void convertWide2Multi(byte[] wideByte, byte[] multiByte) {
        if (wideByte == null || multiByte == null) {
            return;
        }
        
        for (int i = 0; i < wideByte.length / 2 ; i++) {
            if (wideByte[i*2+1] != 0)
                return;
            
            multiByte[i] = wideByte[i*2];
        }
    }
    
    public boolean stringCompare(String str1, String str2) 
    {
        str1 = str1.toLowerCase();
        str2 = str2.toLowerCase();
        
        int nSize = str2.length();
        if (str1.length() < str2.length())
            nSize = str1.length();
        
        for (int i = 0; i < nSize; i++) {
            int str1_ch = (int) str1.charAt(i);
            int str2_ch = (int) str2.charAt(i);
            
            if (str1_ch == str2_ch) {
                continue;
            } else {
                return false;
            }            
        }      
        
        return true;
    }
    
}
