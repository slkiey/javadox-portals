import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

public class Server extends JFrame{
   private ServerSocket ss;
   private Vector<Socket> clients = new Vector<Socket>();
   private Vector<ClientHandler> clientThreads = new Vector<ClientHandler>();
   private final int PORT = 4242;
   private JTextField jtfConsole;
   private JButton jbRoll;
   /**
    * The server's default constructor.
    * Accepts client connections.
    */
   public Server(){
      
      //Accept and handle client connections
      try{
         Socket cs = null;
         ss = new ServerSocket(PORT);
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
    * Prints a String to all clients in the clients Vector.
    */
   private void sendToAll(String msg){
      for(Socket s: clients){
         try{
            //Creates a PrintWriter using the socket
            PrintWriter opw = new PrintWriter(new OutputStreamWriter(s.getOutputStream()), true);
            //Print the message to the client
            opw.println(msg);
         } catch(IOException ioe) {
            System.out.println("IOException occurred: " + ioe.getMessage());
            ioe.printStackTrace();
         }
      }
   } //end of sendToAll method
   
   public String searchClients(String client){
      for(ClientHandler ch:clientThreads){
         if(ch.getName().equals(client)){
            return String.format("%s is connected!\nIndex: %d\n", client, clientThreads.indexOf(ch));
         }
      }
      return String.format("%s is not connected.\n", client);
   }
   
   public int getIndex(String client){
      for(ClientHandler ch:clientThreads){
         if(ch.getName().equals(client)){
            return clientThreads.indexOf(ch);
         }
      }
      return -1;
   }
   protected class ClientHandler extends Thread {
      private BufferedReader rin;
      private PrintWriter pout;
      private ObjectInputStream ois;
      private ObjectOutputStream oos;
      private Socket mySocket;
      private RollRequest srr;
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
            rin = new BufferedReader(new InputStreamReader(mySocket.getInputStream()));
            pout = new PrintWriter(new OutputStreamWriter(mySocket.getOutputStream()), true);
            ois = new ObjectInputStream(mySocket.getInputStream());
            oos = new ObjectOutputStream(mySocket.getOutputStream());
            //Listen for RollRequest objects.
            while(true){
               if(!nameGet){
                  //Get the name from the BufferedReader
                  String line = rin.readLine();
                  sendToAll(line);
                  System.out.println(line);
                  /* Following code splits the line into an array and removes
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
               }
               srr = (RollRequest)ois.readObject();
               jbRoll = srr.getButton();
               //Create a roll result and send it to all clients.
               sendToAll(String.format("%s rolled a %d!", srr.getSender(), rollResult()));
            }
         } catch(ClassNotFoundException cnfe) {
            System.err.println("Error: class not found " + cnfe.getMessage());
         } catch(SocketException se) {
            System.err.println(this.getName() + " disconnected.");
         } catch(EOFException eofe) {
            System.err.println(this.getName() + " disconnected.");
         } catch(IOException ioe) {
            ioe.printStackTrace();
         }
      } //end of run method
      
      /**
       * Returns a number from 1 to 6.
       * @return a number from 1 to 6.
       */
      public int rollResult(){
         Random rand = new Random();
         return rand.nextInt(6) + 1;
      }

      public void enableClient(){
         try{
            oos.writeObject(new ControlToken(1));
            oos.flush();
         } catch(IOException ioe) {
            ioe.printStackTrace();
         }
      }
      
   } //end of ClientHandler class
   
   protected class ConsoleHandler extends Thread{
      private Scanner scan;
      
      public ConsoleHandler(){
         scan = new Scanner(System.in);
      }
      
      public void run(){
         System.out.println("Console started");
         while(true){
            String cmd = scan.nextLine();
            String[] cmdSplit = cmd.split("\\s+");
            //Prints the current list of clients
            if(cmd.equals("printnames")){
               if(clientThreads.size() == 0){
                  System.out.println("No clients are connected.");
               }
               for(ClientHandler ch:clientThreads){
                  System.out.printf("Index: %-3d | Name: %-15s\n", clientThreads.indexOf(ch), ch.getName());
               }
            }
            //Searches for a client in the Vector
            /* Works by splitting command string by spaces, removing first element, and
             * re-building the String with spaces. Then passes a trimmed String to the
             * searchClients() method.
             */
            if(cmdSplit[0].equals("search")){
               String name = "";
               cmdSplit[0] = "";
               for(String s: cmdSplit){
                  name += s + " ";
               }
               name = name.trim();
               System.out.print(searchClients(name));
            } //end of name search command
            //Enable roll for a player
            if(cmdSplit[0].equals("enable")){
               String name = "";
               cmdSplit[0] = "";
               for(String s: cmdSplit){
                  name += s + " ";
               }
               name = name.trim();
               if(getIndex(name) != -1){
                  clientThreads.get(getIndex(name)).enableClient();
               } else {
                  System.out.printf("Could not enable button, client %s not found.\n", name);
               }
            }
         }
      }
   }
} //end of Server class