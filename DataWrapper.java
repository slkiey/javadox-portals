import java.io.Serializable;

/**
 * A wrapper class for messages/tokens to be sent.
 * Team Javadox
 * @author Alan Chu
 * @version 20191218
 */
public class DataWrapper implements Serializable{
   /*
    * Code for a String: 0
    * Code for a RollRequest: 1
    * Code for a ControlToken: 2
    * Code for GameLogic object: 3
    */
   private static final long serialVersionUID = 40L;
   protected static final int STRINGCODE = 0, CTCODE = 1, BICODE = 2; //RRCODE is deprecated
   private int type;
   private String message;
   private ControlToken ct;
   private boolean isGameMessage;
   private GameLogic.BoardInformation bi;

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
    * Accepts a ControlToken.
    * @param type the type code of Object being sent
    * @param ct the ControlToken to be sent.
    */
   public DataWrapper(int type, ControlToken ct){
      this.type = type;
      this.ct = ct;
   }


   public DataWrapper(int type, GameLogic.BoardInformation boardInfo){
      this.type = type;
      bi = boardInfo;
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
    * Returns the ControlToken to be sent.
    * @return ct the ControlToken to be sent
    */
   public ControlToken getCT(){
      return ct;
   }

   public GameLogic.BoardInformation getBoardInformation(){
      return bi;
   }

   public boolean getIsGameMessage() {
      return isGameMessage;
   }

   
}