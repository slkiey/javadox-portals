/**
 * A token class representing a request to roll.
 * Sent by a client when they click their roll button.
 */
import java.io.*;

public class RollRequest implements Serializable{
   private String sender;
   
   /**
    * Constructor for the RollRequest class.
    * @param s the name of the sender
    */
   public RollRequest(String s){
      sender = s;
   }
   
   /**
    * Returns the name of the sender.
    * @return sender the name of the sender.
    */
   public String getSender(){
      return sender;
   }
   
}