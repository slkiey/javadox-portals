import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Timer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;

public class Server extends JFrame{
   private Vector<ClientHandler> clientThreads = new Vector<>();
   private Queue<String> turnQueue = new LinkedList<>();
   private static final int PORT = 4242;
   private GameLogic updatedGL;
   private final Random rand;
   private volatile boolean turnFinished = false;
   private StringBuilder sbCommandHistory = new StringBuilder();

   /**
    * The server's default constructor.
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

      //Text field for entering commands
      JTextField jtfConsole = new JTextField(15);

      //Binding the Enter key to reset
      jtfConsole.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "sendMessage");
      jtfConsole.getActionMap().put("sendMessage", new AbstractAction(){
         public void actionPerformed(ActionEvent ae){

            //Store the command in history
            sbCommandHistory.append(">>");
            sbCommandHistory.append(jtfConsole.getText());
            sbCommandHistory.append("\n");

            //Execute the command
            parseCommand(jtfConsole.getText());

            //Reset the text field
            jtfConsole.setText("");
         }
      });
      add(jtaCommandHistory, BorderLayout.CENTER);
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

   private void consoleAppend(String string){
      sbCommandHistory.append(string);
      sbCommandHistory.append("\n");
   }

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

            //Displays the updated board in a new window
            case "showboard":
               if(updatedGL != null) {
                  JFrame jfBoard = new JFrame();
                  jfBoard.setTitle("Current Board");
                  jfBoard.add(updatedGL, BorderLayout.CENTER);
                  jfBoard.pack();
                  jfBoard.setVisible(true);
                  jfBoard.setDefaultCloseOperation(EXIT_ON_CLOSE);
               } else {
                  consoleAppend("Board doesn't exist.");
               }
               break;

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

                  //Instruct all clients to add the new player
                  sendCTToAll(new ControlToken(ControlToken.ADDCODE, alias, rand.nextInt(8)));

                  /*
                   * For all players other than the first, request an updated board from the first Client in the Vector
                   * and send it players when the first connect.
                   */
                  if(clientThreads.get(0) != this) {
                     //Retrieve an updated board from a player and send it to this client
                     requestBoard();
                     //Wait 1250 milliseconds before writing updatedGL. The Client will send an updated board in this time.
                     Timer timer = new Timer();
                     TimerTask ttWriteBoard = new TimerTask() {
                        @Override
                        public void run() {
                           try {
                              oos.writeObject(new DataWrapper(DataWrapper.GLCODE, updatedGL));
                              oos.flush();
                           } catch (IOException ioe) {
                              ioe.printStackTrace();
                           }
                        }
                     };
                     timer.schedule(ttWriteBoard, 1250);
                  }

               }
               //Handle DataWrapper objects
               switch(dw.getType()){
                  //Handle chat messages
                  case DataWrapper.STRINGCODE:
                     //Send client messages to all clients, appending sender name
                     String fmtMessage = String.format("%s: %s", this.getName(), dw.getMessage());
                     sendToAll(fmtMessage);
                     consoleAppend(fmtMessage);
                     break;

                  //Handle RollRequest objects
                  case DataWrapper.RRCODE:
                     RollRequest srr = dw.getRR();
                     //Obtain the roll result and send it to all clients.
                     int rolledResult = rollResult();
                     String fmtRR = String.format("%s rolled a %d!", srr.getSender(), rolledResult);
                     sendGMToAll(fmtRR);
                     //Move the player on all the boards by writing a ControlToken with the rolledResult
                     sendCTToAll(new ControlToken(
                             ControlToken.MOVECODE, this.getName(), rolledResult, true));
                     consoleAppend(fmtRR);
                     break;

                  //Store the updated board in a variable to be sent to connecting players
                  case DataWrapper.GLCODE:
                     updatedGL = dw.getGL();
                     consoleAppend(String.format("Received a GameLogic with %d players.\n", dw.getGL().getPlayerVector().size()));
                     consoleAppend(updatedGL.toString());
                     break;

                  case DataWrapper.CTCODE:
                     if(dw.getCT().getCode() == ControlToken.TURNFINISHEDCODE){
//                        consoleAppend("Token received, setting turnFinished to true..");
                        turnFinished = true;
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

      public TurnHandler(){}

      public void run(){
         while(true){
            String currentPlayer = turnQueue.peek();
            ClientHandler chPlayer;

            sendGMToAll(String.format("It is now %s's turn.", currentPlayer));
            chPlayer = clientThreads.get(getIndex(currentPlayer));
            chPlayer.enableClient();

            while(true){
//               System.out.println("Waiting for player to finish turn..");
               if(turnFinished) {
                  chPlayer.disableClient();
                  //Resetting the turnFinished variable for the next turn
                  turnFinished = false;
                  break;
               }
            }

            turnQueue.remove();
            sendGMToAll(String.format("%s has finished their turn.", currentPlayer));
            turnQueue.add(currentPlayer);
         }
      }
   }

} //end of Server class