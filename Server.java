import java.io.*;
import java.net.*;
import java.util.*;

public class Server{
   private ServerSocket ss;
   private Vector<Socket> clients = new Vector<Socket>();
   private final int PORT = 4242;
   /**
    * The server's default constructor.
    * Accepts client connections.
    */
   public Server(){
      try{
         Socket cs = null;
         ss = new ServerSocket(PORT);
         InetAddress ina = InetAddress.getLocalHost();
         System.out.println("Host name: " + ina.getHostName());
         System.out.println("IP Address: " + ina.getHostAddress());
         while(true){
            cs = ss.accept();
            clients.add(cs);
            new ClientHandler(cs).start();
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
   
   protected class ClientHandler extends Thread {
      private BufferedReader rin;
      private PrintWriter pout;
      private ObjectInputStream ois;
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
               //Create a roll result and send it to all clients.
               sendToAll(String.format("%s rolled a %d!", srr.getSender(), rollResult()));
            }
         } catch(ClassNotFoundException cnfe) {
            System.err.println("Error: class not found " + cnfe.getMessage());
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
   } //end of ClientHandler class
} //end of Server class