import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

public class Server extends JFrame{
   private ServerSocket ss;
   private Vector<Socket> clients = new Vector<Socket>();
   private Vector<ClientHandler> clientThreads = new Vector<ClientHandler>();
   private final int PORT = 4242;
   /**
    * The server's default constructor.
    * Accepts client connections.
    */
   public Server(){
      
      //Accept and handle client connections
      try{
         Socket cs = null;
         ss = new ServerSocket(PORT);
         //Print server information
         InetAddress ina = InetAddress.getLocalHost();
         System.out.println("Host name: " + ina.getHostName());
         System.out.println("IP Address: " + ina.getHostAddress());
         new ConsoleHandler().start();
         while(true){
            cs = ss.accept();
            clients.add(cs);
            ClientHandler ct = new ClientHandler(cs);
            ct.start();
            clientThreads.add(ct);
         }
      } catch(UnknownHostException uhe) {
         uhe.printStackTrace();
      } catch(IOException ioe) {
         ioe.printStackTrace();
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
            oos.writeObject(new DataWrapper(0, msg, false));
         } catch(IOException ioe) {
            System.out.println("IOException occurred: " + ioe.getMessage());
            ioe.printStackTrace();
         }
      }
   } //end of sendToAll method
   
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
   
   /** 
    * Extracts a name from a String array, removing the first element.
    * Method written for processing names from console.
    * @param stringArray the String array
    * @return String the extracted name
    */
   public String getNameFromStringArray(String[] stringArray){
      String[] sa = stringArray;
      String name = "";
      sa[0] = "";
      for(String s: sa){
         name += s + " ";
      }
      name = name.trim();
      return name;
   }
   
   //A thread class to handle clients.
   protected class ClientHandler extends Thread {
      private ObjectInputStream ois;
      private ObjectOutputStream oos;
      private Socket mySocket;
      private RollRequest srr;
      
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
            ois = new ObjectInputStream(mySocket.getInputStream());
            oos = new ObjectOutputStream(mySocket.getOutputStream());
            //Listen for RollRequest objects.
            while(true){
               DataWrapper dw = (DataWrapper) ois.readObject();
               if(!nameGet){
                  //Get the name from the InputStream if not retrieved already
                  String line = "";
                  if(dw.getType() == DataWrapper.STRINGCODE){
                     line = dw.getMessage();
                  }
                  sendToAll(line);
                  System.out.println(line);
                  /* Following code extracts the name from the "CLIENT connected"
                   * String. Works by splitting the String into an array and removing
                   * the last element.
                   */
                  String[] aliasArray = line.split("\\s+");
                  String alias = "";
                  for(int i = 0; i < aliasArray.length - 1; i++){
                     //Do not append a space if at the second to last element
                     if( i == (aliasArray.length - 2)){
                        alias += aliasArray[i];
                     } else {
                        alias += aliasArray[i] + " ";
                     }
                  }
                  this.setName(alias);
                  System.out.println("Thread alias: " + alias);
                  nameGet = true;
               } else {
                  switch(dw.getType()){
                     //Handle messages
                     case DataWrapper.STRINGCODE:
                     
                     
                        //Send client messages to all clients, appending sender name
                        String fmtMessage = String.format("%s: %s", this.getName(), dw.getMessage());
                        sendToAll(fmtMessage);
                        System.out.println(fmtMessage);
                        break;
                     //Handle RollRequest objects
                     case DataWrapper.RRCODE:
                        srr = dw.getRR();
                        String fmtRR = String.format("%s rolled a %d!", srr.getSender(), rollResult());
                        //Obtain the roll result and send it to all clients.
                        sendGMToAll(fmtRR);
                        System.out.println(fmtRR);
                        break;
                     default:
                        System.err.println("Error: invalid DataWrapper.type");
                  }
               }
               
            } //end of while loop
         } catch(ClassNotFoundException cnfe) {
            System.err.println("Error: class not found " + cnfe.getMessage());
         } catch(SocketException se) {
            System.err.println(this.getName() + " disconnected.");
            clientThreads.remove(getIndex(this.getName()));
         } catch(EOFException eofe) {
            System.err.println(this.getName() + " disconnected.");
            clientThreads.remove(getIndex(this.getName()));
         } catch(IOException ioe) {
            ioe.printStackTrace();
         }
      } //end of run method
      
      /**
       * Returns a random number from 1 to 6.
       * @return a random number from 1 to 6.
       */
      public int rollResult(){
         Random rand = new Random();
         return rand.nextInt(6) + 1;
      }
      
      /**
       * Sends a ControlToken to this ClientHandler's socket, telling
       * it to enable the roll button.
       */
      public void enableClient(){
         try{
            oos.writeObject(new DataWrapper(2, new ControlToken(1)));
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
            oos.writeObject(new DataWrapper(2, new ControlToken(0)));
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
   
   //Thread class for server console
   protected class ConsoleHandler extends Thread{
      private Scanner scan;
      
      /**
       * Default constructor for the ConsoleHandler class.
       * Creates a scanner to read from console.
       */
      public ConsoleHandler(){
         scan = new Scanner(System.in);
      }
      
      /**
       * Run method for the ConsoleHandler class.
       * Processes commands from the console.
       */
      public void run(){
         System.out.println("Console started");
         while(true){
            String consoleLine = scan.nextLine();
            String[] cmdSplit = consoleLine.split("\\s+");
            //Store the first element in a String command
            String command = cmdSplit[0];
            String name = getNameFromStringArray(cmdSplit);
            System.out.println("Command: " + command);
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
             */
            
            //Prints the current list of clients
            if(consoleLine.equals("printnames")){
               if(clientThreads.size() == 0){
                  System.out.println("No clients are connected.");
               }
               for(ClientHandler ch:clientThreads){
                  System.out.printf("Index: %-3d | Name: \"%s\"\n", clientThreads.indexOf(ch), ch.getName());
               }
            }
            
            //Searches for a client in the Vector
            if(command.equals("search")){
               System.out.print(searchClients(name));
            }
            
            //Enable roll for a player
            if(command.equals("enable")){
               if(getIndex(name) != -1){
                  System.out.printf("Attempting to enable %s's button...\n", name);
                  clientThreads.get(getIndex(name)).enableClient();
               } else {
                  System.out.printf("Could not enable button, client %s not found.\n", name);
               }
            }
            
            //Disable roll for a player
            if(command.equals("disable")){
               if(getIndex(name) != -1){
                  System.out.printf("Attempting to disable %s's button...\n", name);
                  clientThreads.get(getIndex(name)).disableClient();
               } else {
                  System.out.printf("Could not disable button, client %s not found.\n", name);
               }
            }
            
            //Kick a player.
            if(command.equals("kick")){
               if(getIndex(name) != -1){
                  System.out.printf("Attempting to kick %s...\n", name);
                  try{
                     clientThreads.get(getIndex(name)).mySocket.close();
                  } catch(IOException ioe) {
                     System.err.println("Error: couldn't kick " + name);
                     ioe.printStackTrace();
                  }
               } else {
                  System.out.printf("Could not kick client %s, client %s not connected.", name, name);
               }
            }
         } //end of while loop
      } //end of run method
   } //end of ConsoleHandler class
} //end of Server class