/**
 * A token class that informs the client about what action to take.
 */
import java.io.*;
import javax.swing.*;

public class ControlToken implements Serializable{
   /*
    * Code to enable roll button: 0
    * Code to disable roll button: 1
    */
   private int opcode;
   
   /**
    * A constructor for the ControlToken class.
    * @param _opcdoe the int specifying which action should be taken.
    */
   public ControlToken(int _opcode){
      opcode = _opcode;
   }
   
   /**
    * Returns the operation code for this ControlToken.
    * @return the opcode
    */
   public int getCode(){
      return opcode;
   }
   
}