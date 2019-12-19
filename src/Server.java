import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
/**
 * The server class for the Portals game.
 * Team Javadox
 * @author Alan Chu
 * @version 20191218
 */
public class Server extends JFrame{
   private Vector<ClientHandler> clientThreads = new Vector<>();
   private static final int PORT = 4242;
   private GameLogic.BoardInformation boardLayout;
   private final Random rand;
   private volatile boolean turnFinished = false;
   private StringBuilder sbCommandHistory = new StringBuilder();
   private Queue<String> turnQueue = new LinkedList<>();
   private final Object waitForBoardLock = new Object();
   private final Object turnFinishedLock = new Object();
   private boolean boardUpdated = false;

   /**
    * Constructs a command line to control the game from.
    * Accepts client connections.
    */
   public Server(){
      //Create a Random object
      rand = new Random();

      //Create the GUI
      //Text area for displaying console
      JTextArea jtaCommandHistory = new JTextArea(35,45);
      jtaCommandHistory.setFont(new Font("Helvetica", Font.BOLD, 12));
      jtaCommandHistory.setBackground(Color.BLACK);
      jtaCommandHistory.setForeground(Color.GREEN);
      jtaCommandHistory.setEditable(false);
      //Wrap the text area in a JScrollPane
      JScrollPane jspCommandHistory = new JScrollPane(jtaCommandHistory);

      //Text field for entering commands
      JTextField jtfConsole = new JTextField(15);

      //Binding the Enter key to reset
      jtfConsole.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "sendMessage");
      jtfConsole.getActionMap().put("sendMessage", new AbstractAction(){
         public void actionPerformed(ActionEvent ae){

            //Store the command in history
            sbCommandHistory.append("\n");
            sbCommandHistory.append(">>");
            sbCommandHistory.append(jtfConsole.getText());
            sbCommandHistory.append("\n");

            //Execute the command
            parseCommand(jtfConsole.getText());

            //Reset the text field
            jtfConsole.setText("");
         }
      });
      add(jspCommandHistory, BorderLayout.CENTER);
      add(jtfConsole, BorderLayout.SOUTH);

      //Use a Timer to set the command history area to the StringBuilder text
      ActionListener alRefresh = new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            jtaCommandHistory.setText(sbCommandHistory.toString());
         }
      };
      javax.swing.Timer timer = new javax.swing.Timer(350, alRefresh);
      timer.start();

      //JFrame Initialization
      setTitle("Portals Server");
      setVisible(true);
      setDefaultCloseOperation(EXIT_ON_CLOSE);
      setLocationRelativeTo(null);
      pack();

      //Creating a GameLogic to serve as the portal layout
      GameLogic glLayout = new GameLogic(10, true);
      boardLayout = glLayout.getUpdatedBoardInformation();

      //Accept and handle client connections
      try {
         Socket cs;
         ServerSocket ss = new ServerSocket(PORT);
         //Print server information
         InetAddress ina = InetAddress.getLocalHost();
         consoleAppend("Host name: " + ina.getHostName());
         consoleAppend("IP Address: " + ina.getHostAddress());
         while (true) {
            cs = ss.accept();
            ClientHandler ct = new ClientHandler(cs);
            ct.start();
            clientThreads.add(ct);
         }
      } catch (UnknownHostException uhe) {
         System.err.println("Could not determine host IP address.");
         uhe.printStackTrace();
      } catch (IOException uhe) {
         uhe.printStackTrace();
      }
   } //end of Server constructor
   
   /**
    * The main method.
    * @param args arguments from the command line
    */
   public static void main(String[] args) {
      new Server();
   } //end of main method
   
   /**
    * Prints a String to all clients in the clientThreads vector.
    * @param msg the message to be sent
    */
   private void sendToAll(String msg){
      for(ClientHandler ch: clientThreads){
         try{
            //Get the client's ObjectOutputStream
            ObjectOutputStream oos = ch.getOOS();
            //Print the message to the client
            oos.writeObject(new DataWrapper(DataWrapper.STRINGCODE, msg, false));
         } catch(IOException ioe) {
            System.out.println("IOException occurred: " + ioe.getMessage());
            ioe.printStackTrace();
         }
      }
   } //end of sendToAll method

   /**
    * Sends a ControlToken to all ClientHandlers.
    * @param ct the ControlToken to be sent
    */
   private void sendCTToAll(ControlToken ct) {
      for(ClientHandler ch: clientThreads){
         try{
            //Get the client's ObjectOutputStream
            ObjectOutputStream oos = ch.getOOS();
            //Write the ControlToken to the client
            oos.writeObject(new DataWrapper(DataWrapper.CTCODE, ct));
         } catch(IOException ioe) {
            System.out.println("IOException occurred: " + ioe.getMessage());
            ioe.printStackTrace();
         }
      }
   }

  /**
    * Prints a GameMessage string to all clients in the clientThreads vector.
    * @param msg the message to be sent
    */
   private void sendGMToAll(String msg){
      for(ClientHandler ch: clientThreads){
         try{
            //Get the client's ObjectOutputStream
            ObjectOutputStream oos = ch.getOOS();
            //Print the message to the client
            oos.writeObject(new DataWrapper(0, msg, true));
         } catch(IOException ioe) {
            System.out.println("IOException occurred: " + ioe.getMessage());
            ioe.printStackTrace();
         }
      }
   } //end of sendToAll method

   /**
    * Append a String to the command
    * @param string
    */
   private void consoleAppend(String string){
      sbCommandHistory.append(string);
      sbCommandHistory.append("\n");
   }

   /**
    * Takes a String and performs a corresponding action.
    * @param consoleLine the command from the console
    */
   private void parseCommand(String consoleLine){
      if(consoleLine.trim().equals("")) {
         System.err.println("Blank line");
      } else {
         ArrayList<String> cmdSplit = new ArrayList<>();

         //Split the line by spaces except between quotes and add to the cmdSplit ArrayList
         Matcher matcher = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(consoleLine);
         while(matcher.find()) cmdSplit.add(matcher.group(1).replace("\"", ""));

         //Store the first element in a String command and the second element in a String name
         String command = cmdSplit.get(0);
         String name = "";
         consoleAppend("Command: " + command);
         if(cmdSplit.size() > 1) {
            name = cmdSplit.get(1);
            consoleAppend("Name: " + name);
         }
         /* COMMANDS BELOW
          * printnames: Prints the current list of clients
          *
          * search name: Searches for the client in the Vector and prints a String
          *
          * enable name: If the player is in the Vector, sends a ControlToken telling
          * them to enable their button.
          *
          * disable name: If the player is in the Vector, sends a ControlToken telling
          * them to disable their button.
          *
          * kick name: Close the socket for this player.
          *
          * move name <int> <obo>: Move the player a number of spaces. Include obo if you want the
          * player to move one tile at a time.
          */

         switch(command){
            //Prints the current list of clients
            case "printnames":
               if(clientThreads.size() == 0){
                  consoleAppend("No clients are connected.");
               }
               StringBuilder sbClientList = new StringBuilder();
               for(ClientHandler ch:clientThreads){
                  sbClientList.append(String.format("Index: %-3d | Name: \"%s\"\n", clientThreads.indexOf(ch), ch.getName()));
               }
               consoleAppend(sbClientList.toString());
               break;

            //Gets an updated board
            case "getboard":
               requestBoard();
               break;

//            //Displays the updated board in a new window
//            case "showboard":
//               if(updatedGL != null) {
//                  JFrame jfBoard = new JFrame();
//                  jfBoard.setTitle("Current Board");
//                  jfBoard.add(updatedGL, BorderLayout.CENTER);
//                  jfBoard.pack();
//                  jfBoard.setVisible(true);
//                  jfBoard.setDefaultCloseOperation(EXIT_ON_CLOSE);
//               } else {
//                  consoleAppend("Board doesn't exist.");
//               }
//               break;

            //Searches for a client in the Vector
            case "search":
               consoleAppend(searchClients(name));
               break;

            //Starts the game
            case "startgame":
               if(clientThreads.size() == 0){
                  consoleAppend("Can't start the game, no players are connected.");
               } else {
                  consoleAppend("Starting game..");
                  TurnHandler th = new TurnHandler();
                  th.start();
               }
               break;

            //Enables roll for a player
            case "enable":
               if(getIndex(name) != -1){
                  consoleAppend(String.format("Attempting to enable %s's button...\n", name));
                  clientThreads.get(getIndex(name)).enableClient();
               } else {
                  consoleAppend(String.format("Could not enable button, client %s not found.\n", name));
               }
               break;

            //Disables roll for a player
            case "disable":
               if(getIndex(name) != -1){
                  consoleAppend(String.format("Attempting to disable %s's button...\n", name));
                  clientThreads.get(getIndex(name)).disableClient();
               } else {
                  consoleAppend(String.format("Could not disable button, client %s not found.\n", name));
               }
               break;
            //Kick a player.
            case "kick":
               if(getIndex(name) != -1){
                  consoleAppend(String.format("Attempting to kick %s...\n", name));
                  try{
                     clientThreads.get(getIndex(name)).mySocket.close();
                  } catch(IOException ioe) {
                     consoleAppend("Error: couldn't kick " + name);
                     ioe.printStackTrace();
                  }
               } else {
                  consoleAppend(String.format("Could not kick client %s, client %s not connected.\n", name, name));
               }
               break;

            //Move a player a number of tiles on all boards
            case "move":
               if (getIndex(name) != -1) {
                  try {
                     int tilesToMove = Integer.parseInt(cmdSplit.get(2));
                     boolean oBo = false;
                     if (cmdSplit.size() >= 4) {
                        String oneByOne = cmdSplit.get(3);
                        if (oneByOne.equals("obo")) {
                           oBo = true;
                        }
                     }
                     sendCTToAll(new ControlToken(
                             ControlToken.MOVECODE, name, tilesToMove, oBo));
                  } catch (NumberFormatException nfe) {
                     System.err.println("Invalid number of moves, please enter an integer.");
                  }
               } else {
                  System.err.printf("Could not move client %s, client %s not connected.\n", name, name);
               }
               break;

            default:
               consoleAppend("Command not recognized.");
               break;

         }
      }
   }

   /**
    * Searches the clientThreads vector for a client name.
    * @param client the name of the client
    * @return String string detailing if client is in vector or not
    */
   public String searchClients(String client){
      for(ClientHandler ch:clientThreads){
         if(ch.getName().equals(client)){
            return String.format("%s is connected!\nIndex: %d\n", client, clientThreads.indexOf(ch));
         }
      }
      return String.format("%s is not connected.\n", client);
   }

   /**
    * Sends a ControlToken.BOARDREQUEST to the first client in the vector,
    * who presumably has the updated positions.
    */
   public void requestBoard(){
      try {
         clientThreads.get(0).getOOS().writeObject(
                 new DataWrapper(DataWrapper.CTCODE, new ControlToken(ControlToken.BOARDREQUEST)));
      } catch(IOException ioe){
         ioe.printStackTrace();
      }
   }

   /**
    * Gets the index of a client in the clientThreads vector from a client's name.
    * @param client the name of the client
    * @return int the index of the player. Returns -1 if not found.
    */
   public int getIndex(String client){
      for(ClientHandler ch:clientThreads){
         if(ch.getName().equals(client)){
            return clientThreads.indexOf(ch);
         }
      }
      return -1;
   }

   //A thread class to handle clients.
   protected class ClientHandler extends Thread {
      private ObjectOutputStream oos;
      private Socket mySocket;

      /**
       * Constructor for the ClientHandler class.
       * @param s the client's socket
       */
      public ClientHandler(Socket s){
         mySocket = s;
      }

      /**
       * The server's run method.
       * Opens I/O streams for clients.
       */
      public void run(){
         boolean nameGet = false;
         try{
            ObjectInputStream ois = new ObjectInputStream(mySocket.getInputStream());
            oos = new ObjectOutputStream(mySocket.getOutputStream());

            //Listen for messages and RollRequest objects.
            while(true){
               DataWrapper dw = (DataWrapper) ois.readObject();
               //Get the name from the InputStream if not retrieved already
               if(!nameGet){
                  //Store the client's first message, which is always in the format "<name> connected."
                  String line = "";
                  if(dw.getType() == DataWrapper.STRINGCODE){
                     line = dw.getMessage();
                  }
                  consoleAppend(line);

                  /*
                   * Extract the alias from the String by getting the substring from 0 to the last index of whitespace,
                   * effectively removing the " connected." part of the String.
                   */
                  String alias = line.substring(0, line.lastIndexOf(" "));

                  //Disconnect the client if the name is already in use
                  if(getIndex(alias) != -1){
                     oos.writeObject(
                             new DataWrapper(
                                     DataWrapper.STRINGCODE, "Sorry, that name is in use. Disconnecting..", false));
                     oos.flush();
                     mySocket.close();
                  }

                  this.setName(alias);
                  turnQueue.add(alias);
                  nameGet = true;

                  //If the first client
                  if(clientThreads.get(0) == this) {
                     //send the boardLayout for them to unpack
                     oos.writeObject(new DataWrapper(DataWrapper.BICODE, boardLayout));
                  }
                  //If not the first client
                  if(clientThreads.get(0) != this) {
                     //request a board from the first client and wait until the updated board is received
                     requestBoard();
                     synchronized (waitForBoardLock) {
                        while (!boardUpdated) {
//                           System.out.println("Thread " + this.getName() + " waiting");
                           waitForBoardLock.wait();
                        }
                     }
                     oos.writeObject(new DataWrapper(DataWrapper.BICODE, boardLayout));
                     boardUpdated = false;
                  }

                  //Instruct all clients to add the new player
                  sendCTToAll(new ControlToken(ControlToken.ADDCODE, alias, rand.nextInt(8)));

               }
               //Handle DataWrapper objects
               switch(dw.getType()){
                  //Ensure all clients see each others' messages
                  case DataWrapper.STRINGCODE:
                     //Send incoming client chat messages to all clients, and append sender name
                     String fmtMessage = String.format("%s: %s", this.getName(), dw.getMessage());
                     sendToAll(fmtMessage);
                     consoleAppend(fmtMessage);
                     break;

                  //Receive updated board information from the first client in the client list
                  case DataWrapper.BICODE:
                     boardLayout = dw.getBoardInformation();
                     System.out.println("Received an updated BoardInformation.");
                     System.out.println(boardLayout.toString());
                     synchronized(waitForBoardLock){
                        boardUpdated = true;
                        waitForBoardLock.notify();
//                        System.out.println("Thread " + this.getName() + " notified because of receiving a BoardInformation.");
                     }
                     break;

                  //Handle ControlToken objects
                  case DataWrapper.CTCODE:
                     //Client informs server that they are finished with their turn
                     if(dw.getCT().getCode() == ControlToken.TURNFINISHEDCODE){
                        synchronized(turnFinishedLock) {
                           turnFinished = true;
                           turnFinishedLock.notify();
                        }
//                        System.out.println("Thread " + this.getName() + " notified because of receiving a turnFinished.");
                     }
                     //Client asks to roll the dice, Server returns a random value from 1-6
                     if(dw.getCT().getCode() == ControlToken.ROLLREQUESTCODE) {
                        //Generate the roll result
                        int rolledResult = rollResult();
                        //Send it as a game message to all clients
                        String rolledResultString = String.format("%s rolled a %d!",
                                dw.getCT().getPlayerName(), rolledResult);
                        sendGMToAll(rolledResultString);
                        //Instruct the clients to move the player on all boards
                        sendCTToAll(new ControlToken(ControlToken.MOVECODE, this.getName(), rolledResult, true));
                     }
                     break;

                  //Default case
                  default:
                     consoleAppend("Error: invalid DataWrapper.type");
                     break;
               } //end of switch statement
            } //end of while loop
         } catch(ClassNotFoundException cnfe) {
            System.err.println("Error: class not found " + cnfe.getMessage());
         } catch(SocketException | EOFException se) {
            handleDisconnect();
         } catch(IOException ioe) {
            ioe.printStackTrace();
         } catch (InterruptedException ioe) {
            ioe.printStackTrace();
         }
      } //end of run method

      /**
       * Handles client disconnections.
       * Removes this client from the clientThreads Vector, informs other clients that the client has disconnected,
       * and tells them remove them from their board.
       */
      public void handleDisconnect(){
         consoleAppend(this.getName() + " disconnected.");
         clientThreads.remove(getIndex(this.getName()));
         sendToAll(this.getName() + " disconnected.");
         sendCTToAll(new ControlToken(ControlToken.REMOVECODE, this.getName()));
      }

      /**
       * Returns a random number from 1 to 6.
       * @return a random number from 1 to 6.
       */
      public int rollResult(){
         return rand.nextInt(6) + 1;
      }

      /**
       * Sends a ControlToken to this ClientHandler's socket, telling
       * it to enable the roll button.
       */
      public void enableClient(){
         try{
            oos.writeObject(new DataWrapper(DataWrapper.CTCODE, new ControlToken(ControlToken.ENABLECODE)));
            oos.flush();
         } catch(IOException ioe) {
            ioe.printStackTrace();
         }
      }

      /**
       * Sends a ControlToken to this ClientHandler's socket, telling
       * it to disable the roll button.
       */
      public void disableClient(){
         try{
            oos.writeObject(new DataWrapper(DataWrapper.CTCODE, new ControlToken(ControlToken.DISABLECODE)));
            oos.flush();
         } catch(IOException ioe) {
            ioe.printStackTrace();
         }
      }

      /**
       * Returns the ObjectOutputStream of this ClientHandler.
       * @return oos this ClientHandler's ObjectOutputStream
       */
      public ObjectOutputStream getOOS(){
         return oos;
      }

   } //end of ClientHandler class

   protected class TurnHandler extends Thread{

      public TurnHandler(){
         setName("TurnHandler");
      }

      public void run(){
         String lastPlayer = "";
         while(true){
            String currentPlayer = turnQueue.peek();
            ClientHandler chPlayer;

            //If the player is not in the clientThreads vector, or there is a duplicate, remove them from the queue
            if(getIndex(currentPlayer) == -1 || currentPlayer.equals(lastPlayer)){
               turnQueue.remove();
            //Otherwise, proceed with enabling/disabling the Client and turn control.
            } else {
               sendGMToAll(String.format("It is now %s's turn.", currentPlayer));
               chPlayer = clientThreads.get(getIndex(currentPlayer));
               chPlayer.enableClient();

               synchronized (turnFinishedLock) {
                  while (!turnFinished) {
//                     System.out.println("Thread " + this.getName() + " waiting for a TurnFinished token..");
                     try {
                        turnFinishedLock.wait();
                     } catch (InterruptedException ioe) {
                        ioe.printStackTrace();
                     }
                  }
                  chPlayer.disableClient();
                  turnFinished = false; //resetting turnFinished
               }


               lastPlayer = turnQueue.remove();
               sendGMToAll(String.format("%s has finished their turn.", currentPlayer));
               turnQueue.add(currentPlayer);
            }
         }
      }
   }

} //end of Server class