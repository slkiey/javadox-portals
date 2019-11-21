import java.io.*;
import javax.swing.*;

public class RollRequest implements Serializable{
   private String sender;
   private JButton jbRoll;
   
   public RollRequest(String s, JButton _jbRoll){
      sender = s;
      jbRoll = jbRoll;
   }
   
   public String getSender(){
      return sender;
   }
   
   public JButton getButton(){
      return jbRoll;
   }
}