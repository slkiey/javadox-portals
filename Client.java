import java.io.*;
import java.net.*;
import java.awt.event.*;
import javax.swing.*;

public class Client extends JFrame {
   private final int PORT = 4242;
   private String IP = "129.21.157.229";
   private String alias;
   private BufferedReader rin;
   private PrintWriter pout;
   private ObjectInputStream ois;
   private ObjectOutputStream oos;
   private Socket sock;
   private RollRequest rr;
   private JTextField jtfAlias;
   private JButton jbRoll;
   private String readLock = "readLock";
   private String writeLock = "writeLock";
   public Client(){
      JPanel jpWest = new JPanel();
      //Creating the alias text field
      JTextField jtfAlias = new JTextField("Enter alias");
      jtfAlias.setToolTipText("Enter name here");
      //Creating the connect button
      JButton jbConnect = new JButton("Connect");
      jbConnect.addActionListener(new ActionListener() {
         /**
          * Attempts to connect to server and instantiates streams.
          */
         public void actionPerformed(ActionEvent ae) {
            try{
               sock = new Socket(IP, PORT);
               //System.out.println("Connected to " + IP);
               rin = new BufferedReader(new InputStreamReader(sock.getInputStream()));
               pout = new PrintWriter(sock.getOutputStream(), true);
               //ois = new ObjectInputStream(sock.getInputStream());
               oos = new ObjectOutputStream(sock.getOutputStream());
               alias = jtfAlias.getText();
               pout.println(alias + " connected.");
               new ChatListener().start();
               new ObjectListener().start();
            } catch(UnknownHostException uhe) {
               System.err.println("Error: unknown host.");
               uhe.printStackTrace();
            } catch(IOException ioe) {
               System.err.println("Error: couldn't connect.");
               ioe.printStackTrace();
            }
         }
      });
      //Creating the disconnect button
      JButton jbDisconnect = new JButton("Disconnect");
      jbDisconnect.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent ae){
            try{
               System.out.println("Disconnecting..");
               sock.close();
            } catch(IOException ioe) {
               ioe.printStackTrace();
            }
         }
      });
      //Creating the roll button
      jbRoll = new JButton("Roll Dice");
      jbRoll.addActionListener(new ActionListener() {
         
         /**
          * Sends a RollRequest to the server if the button is
          * pressed.
          */
         public void actionPerformed(ActionEvent ae){
            rr = new RollRequest(alias, jbRoll);
            try{
               oos.writeObject(rr);
            } catch(NullPointerException npe) {
               System.err.println("Error: you are not connected.");
               //npe.printStackTrace();
            } catch(IOException ioe) {
               System.err.println("Error: IOException: " + ioe.getMessage());
            }
         }
      });
      jbRoll.setEnabled(false);
      jpWest.add(jtfAlias);
      jpWest.add(jbRoll);
      jpWest.add(jbConnect);
      jpWest.add(jbDisconnect);
      add(jpWest);
      //JFrame Initialization
      pack();
      setDefaultCloseOperation(EXIT_ON_CLOSE);
      setVisible(true);
   }
   
   public static void main(String[] args){
      new Client();
   }
   
   protected class ChatListener extends Thread{
      
      /**
       * The thread's run method.
       * Listens for incoming messages and prints them.
       */
      public void run(){
         String line;
         try{
            synchronized(readLock){
               while((line = rin.readLine()) != null){
                  System.out.println(line);
               }
            }
         } catch(SocketException se) {
            System.err.println("Connection closed.");
         } catch(NullPointerException npe) {
            npe.printStackTrace();
         } catch(IOException ioe) {
            ioe.printStackTrace();
         }
      } //end of run
   }
   
   protected class ObjectListener extends Thread{
      private ControlToken ct;
      public void run(){
         try{
            ois = new ObjectInputStream(sock.getInputStream());
         } catch(IOException ioe) {
            ioe.printStackTrace();
         }
         while(true){
            try{
               synchronized(readLock){
                  ct = (ControlToken)ois.readObject();
               }
            } catch(ClassNotFoundException cnfe) {
               cnfe.printStackTrace();
            } catch(IOException ioe) {
               ioe.printStackTrace();
            }
            if(ct.getCode() == 1){
               jbRoll.setEnabled(true);
            }
            if(ct.getCode() == (-1) ){
               jbRoll.setEnabled(false);
            }
         }
      }
   }
}