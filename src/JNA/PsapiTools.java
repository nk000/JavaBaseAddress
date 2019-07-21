package JNA;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import luz.dsexplorer.winapi.jna.Psapi.LPMODULEINFO;

public class PsapiTools {

    private static PsapiTools INSTANCE = null;

    private PsapiTools() {
    }

    public static PsapiTools getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PsapiTools();
        }
        return INSTANCE;
    }

    public List<Module> EnumProcessModules(Pointer hProcess) throws Exception {
        List<Module> list = new LinkedList<>();

        Pointer[] lphModule = new Pointer[2560];
        IntByReference lpcbNeededs = new IntByReference();
        boolean success = Psapi.INSTANCE.EnumProcessModules(hProcess, lphModule, lphModule.length, lpcbNeededs);
        if (!success) {
            int err = Native.getLastError();
            throw new Exception("EnumProcessModules failed. Error: " + err);
        }
        for (int i = 0; i < lpcbNeededs.getValue() / 4; i++) {
            list.add(new Module(hProcess, lphModule[i]));
        }

        return list;
    }

    public String GetModuleFileNameExA(Pointer hProcess, Pointer hModule) {
        byte[] lpImageFileName = new byte[256];
        Psapi.INSTANCE.GetModuleBaseNameW(hProcess, hModule, lpImageFileName, 256);
        String str = "";
        try {
            //return Native.toString(lpImageFileName);
            str = new String(lpImageFileName, "UTF-16LE");
            
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(PsapiTools.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return str;
    } 
   

    public LPMODULEINFO GetModuleInformation(Pointer hProcess, Pointer hModule) throws Exception {
        LPMODULEINFO lpmodinfo = new LPMODULEINFO() {
            @Override
            protected List getFieldOrder() {
                List fields = new ArrayList();
                fields.addAll(Arrays.asList(new String[]{"EntryPoint", "SizeOfImage", "lpBaseOfDll"}));
                return fields;
            }
        };
        boolean success = Psapi.INSTANCE.GetModuleInformation(hProcess, hModule, lpmodinfo, lpmodinfo.size());
        if (!success) {
            int err = Native.getLastError();
            throw new Exception("GetModuleInformation failed. Error: " + err);
        }
        return lpmodinfo;
    }
}
