import java.io.*;
import java.util.ArrayList;
import java.util.Vector;

/**
 * A wrapper class for messages/tokens to be sent.
 */
public class DataWrapper implements Serializable{
   /*
    * Code for a String: 0
    * Code for a RollRequest: 1
    * Code for a ControlToken: 2
    * Code for Player list
    */
   protected static final int STRINGCODE = 0, RRCODE = 1, CTCODE = 2, PLYLISTCODE = 3;
   private int type;
   String message;
   RollRequest rr;
   ControlToken ct;
   boolean isGameMessage;
   Vector<GameLogic.Player> vecPlayers;

   /** 
    * A constructor for the DataWrapper class.
    * Accepts a message.
    * @param type the type code of Object being sent
    * @param message the message to be sent.
    */
   public DataWrapper(int type, String message, boolean _isGameMessage ){
      this.type = type;
      this.message = message;
      this.isGameMessage = _isGameMessage;
   }
   
   /** 
    * A constructor for the DataWrapper class.
    * Accepts a RollRequest.
    * @param type the type code of Object being sent
    * @param rr the RollRequest to be sent.
    */
   public DataWrapper(int type, RollRequest rr){
      this.type = type;
      this.rr = rr;
   }
   
   /** 
    * A constructor for the DataWrapper class.
    * Accepts a ControlToken.
    * @param type the type code of Object being sent
    * @param ct the ControlToken to be sent.
    */
   public DataWrapper(int type, ControlToken ct){
      this.type = type;
      this.ct = ct;
   }


   public DataWrapper(int type, Vector<GameLogic.Player> _vecPlayers){
      this.type = type;
      vecPlayers = _vecPlayers;
   }

   /**
    * Returns a code representing the Object type.
    * @return type code representing the Object type.
    */
   public int getType(){
      return type;
   }
   
   /**
    * Returns the message to be sent.
    * @return message the message to be sent
    */
   public String getMessage(){
      return message;
   }
   
   /**
    * Returns the RollRequest to be sent.
    * @return rr the RollRequest to be sent
    */
   public RollRequest getRR(){
      return rr;
   }
   
   /**
    * Returns the ControlToken to be sent.
    * @return ct the ControlToken to be sent
    */
   public ControlToken getCT(){
      return ct;
   }

   public Vector<GameLogic.Player> getVecPlayers(){
      return vecPlayers;
   }
   
}