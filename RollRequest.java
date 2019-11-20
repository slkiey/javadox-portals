import java.io.*;

public class RollRequest implements Serializable{
   private String sender;
   
   public RollRequest(String s){
      sender = s;
   }
   
   public String getSender(){
      return sender;
   }
   
}