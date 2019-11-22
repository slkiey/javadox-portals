import java.io.*;
import java.net.*;
import java.awt.event.*;
import javax.swing.*;

public class Client extends JFrame {
   private final int PORT = 4242;
   private String IP = "129.21.157.229";
   private String alias;
   private ObjectInputStream ois;
   private ObjectOutputStream oos;
   private Socket sock;
   private RollRequest rr;
   private JTextField jtfAlias;
   private JButton jbRoll;
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
               //Hangs if ois is created here instead of in the ObjectListener class?
               //ois = new ObjectInputStream(sock.getInputStream());
               oos = new ObjectOutputStream(sock.getOutputStream());
               alias = jtfAlias.getText();
               sendMessage(alias + " connected.");
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
            rr = new RollRequest(alias);
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
   
   /**
    * The main method.
    * @param args arguments from the command line
    */
   public static void main(String[] args){
      new Client();
   }
   
   /**
    * Sends a message via ObjectOutputStream.
    * Creates a DataWrapper with the String code: 0 and the message.
    * @param message the message to be sent.
    */
   public void sendMessage(String message){
      try{
         oos.writeObject(new DataWrapper(0, message));
      } catch(IOException ioe) {
         ioe.printStackTrace();
      }
   }
   
   //Listens for incoming messages/objects
   protected class ObjectListener extends Thread{
      public void run(){
         try{ 
            ois = new ObjectInputStream(sock.getInputStream()); 
         } catch(IOException ioe) {
            ioe.printStackTrace();
         }
         //Listen for DataWrapper objects
         while(true){
            DataWrapper dw = null;
            try{
               dw = (DataWrapper)ois.readObject();
            } catch(ClassNotFoundException cnfe) {
               cnfe.printStackTrace();
            } catch(IOException ioe) {
               ioe.printStackTrace();
            }
            switch(dw.getType()){
               //Handle messages
               case DataWrapper.STRINGCODE:
                  System.out.println(dw.getMessage());
                  break;
               //Handle RollRequest objects
               //Client doesn't recieve RollRequests
               case DataWrapper.RRCODE:
                  break;
               //Handle ControlToken objects
               case DataWrapper.CTCODE:
                  ControlToken ct = dw.getCT();
                  if(ct.getCode() == 1){
                     jbRoll.setEnabled(true);
                  } else if (ct.getCode() == 0){
                     jbRoll.setEnabled(false);
                  }
                  break;
                  
               default:
                  System.err.println("Error: invalid DataWrapper.type");
            }
         } //end of while loop
      }
   }
}