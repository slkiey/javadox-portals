import java.io.*;
import javax.swing.*;
public class ControlToken implements Serializable{
   private JButton jbRoll;
   private int opcode;
   public ControlToken(int _opcode){
      opcode = _opcode;
   }
   
   public int getCode(){
      return opcode;
   }
   
}