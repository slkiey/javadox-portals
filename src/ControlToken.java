import java.io.*;

/**
 * A token class used for communication between Server and Client.
 * Team Javadox
 * @author Alan Chu
 * @version 20191218
 */
public class ControlToken implements Serializable{
   /*
    * Code to enable roll button: 0
    * Code to disable roll button: 1
    * Code to add a player to the starting tile: 2
    * Code to move a player: 3
    * Code to remove a player: 4
    * Code to request updated board information from a client: 5
    * Code for a client to indicate that they finished a turn: 6
    * Code for a client to indicate that they rolled the dice: 7;
    */
   private static final long serialVersionUID = 44L;
   protected static final int ENABLECODE = 0, DISABLECODE = 1, ADDCODE = 2, MOVECODE = 3,
   REMOVECODE = 4, BOARDREQUEST = 5, TURNFINISHEDCODE = 6, ROLLREQUESTCODE = 7;
   private int opcode;
   private int tilesToMove;
   private int pawnCode;
   private boolean oneByOne;
   private String playerName;
   /**
    * A constructor for the ControlToken class.
    * @param _opcode the int specifying which action should be taken.
    */
   public ControlToken(int _opcode){
      opcode = _opcode;
   }

   public ControlToken(int _opcode, String name) {
      opcode = _opcode;
      playerName = name;
   }

   public ControlToken(int _opcode, String name, int _pawnCode){
      opcode = _opcode;
      playerName = name;
      pawnCode = _pawnCode;
   }

   public ControlToken(int _opcode, String name, int _tilesToMove, boolean _oneByOne){
      opcode = _opcode;
      playerName = name;
      tilesToMove = _tilesToMove;
      oneByOne = _oneByOne;
   }

   /**
    * Returns the operation code for this ControlToken.
    * @return the opcode
    */
   public int getCode(){
      return opcode;
   }

   public String getPlayerName(){
      return playerName;
   }

   public int getTilesToMove(){
      return tilesToMove;
   }

   public boolean getOneByOne(){
      return oneByOne;
   }

   public int getPawnCode() { return pawnCode; }
}