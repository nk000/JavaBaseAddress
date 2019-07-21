
package baseaddr;
import JNA.JNACore;

/**
 *
 * @author live:yanmin0_2
 */
public class BaseAddr {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        System.out.println("Hello World");    
        String processName = new String("calculator.exe");
                
        boolean bOpen = JNACore.getInstance().getProcessesByName(processName);
        if (!bOpen) {
            System.out.println("failed to get the process name");    
        } else {
            int address = JNACore.getInstance().getBaseAddress();               
        }        
        
    }   
    

}
    
    
    

