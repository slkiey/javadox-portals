import java.io.*;
import java.net.*;
import java.awt.event.*;
import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.border.EmptyBorder;

public class Client extends JFrame {
   private final int PORT = 4242;
   private String ip;
   private String alias;
   private ObjectInputStream ois;
   private ObjectOutputStream oos;
   private Socket sock;
   private RollRequest rr;
   private JTextField jtfAlias;
   private JTextArea jtaDisplayMsgs;
   private JTextArea jtaDisplayGM;
   private JButton jbRoll;
   private StringBuilder chatLog = new StringBuilder();
   private StringBuilder gmLog = new StringBuilder();
   private GameLogic glClient;
   
   /**
    * Default constructor for the Client class.
    * Handles GUI.
    */
   public Client(){

      //NORTH Panel: Alias, Roll Dice, IP, Connect, Disconnect
      JPanel jpNorth = new JPanel();
      //Creating the alias text field
      jtfAlias = new JTextField("Enter alias", 10);
      jtfAlias.setToolTipText("Enter name here");
      jtfAlias.addFocusListener(new FocListener());
      //Creating the IP text field
      JTextField jtfIP = new JTextField("Enter IP Address", 10);
      jtfIP.setToolTipText("Enter IP Address here");
      jtfIP.addFocusListener(new FocListener());
      //Creating the disconnect and connect buttons
      JButton jbDisconnect = new JButton("Disconnect");
      jbDisconnect.setEnabled(false);
      JButton jbConnect = new JButton("Connect");
      //Adding action listeners to the disconnect + connect buttons
      jbDisconnect.addActionListener(new ActionListener() {
         /**
          * Attempts to close the connection and clear the board.
          * Re-enables the connect button after successfully disconnecting.
          * @param ae the ActionEvent trigger
          */
         public void actionPerformed(ActionEvent ae){
            try{
               //Clear the board when disconnecting
               System.out.println("Disconnecting..");
               glClient.clearBoard();
               sock.close();
               jbConnect.setEnabled(true);
            } catch(IOException ioe) {
               ioe.printStackTrace();
            }
         }
      });
      jbConnect.addActionListener(new ActionListener() {
         /**
          * Attempts to connect to server and instantiates streams.
          * Disables the connect button and enables the disconnect button.
          * @param ae the ActionEvent trigger
          */
         public void actionPerformed(ActionEvent ae) {
            try{
               ip = jtfIP.getText();
               alias = jtfAlias.getText();
               if(alias.length() > 15) {
                  System.out.println("Please enter a name less than or equal to 15 characters long.");
               }
               if(ip.equals("Enter IP Address")) {
                  System.out.println("Please enter an IP address.");
               }
               else {
                  System.out.printf("Attempting to connect..\nIP Address: %s\nPort: %d\n", ip, PORT);
                  sock = new Socket(ip, PORT);
                  //Hangs if ois is created here instead of in the ObjectListener class?
                  //ois = new ObjectInputStream(sock.getInputStream());
                  oos = new ObjectOutputStream(sock.getOutputStream());
                  sendMessage(alias + " connected.");
                  new ObjectListener().start();
                  jbDisconnect.setEnabled(true);
                  jbConnect.setEnabled(false);
               }
            } catch(ConnectException ce) {
               System.err.println("Error: couldn't connect.");
               ce.printStackTrace();
            } catch(UnknownHostException uhe) {
               System.err.println("Error: unknown host. Please make sure the IP you entered is valid.");
               uhe.printStackTrace();
            } catch(IOException ioe) {
               System.err.println("Error: couldn't connect.");
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
               oos.writeObject(new DataWrapper(DataWrapper.RRCODE, rr));
            } catch(NullPointerException npe) {
               System.err.println("Error: you are not connected.");
               //npe.printStackTrace();
            } catch(IOException ioe) {
               System.err.println("Error: IOException: " + ioe.getMessage());
            } finally {
               //jbRoll.setEnabled(false);
            }
         }
      });
      jbRoll.setEnabled(false);
      
      //WEST Panel: Display Incoming Messages and Send Messages
      JPanel jpWest = new JPanel(new BorderLayout(5, 5));
      //Create the JTextArea for displaying messages. Include a border and enable word wrapping
      jtaDisplayMsgs = new JTextArea(0, 15);
      jtaDisplayMsgs.setLineWrap(true);
      jtaDisplayMsgs.setWrapStyleWord(true);
      jtaDisplayMsgs.setBorder(BorderFactory.createCompoundBorder
                              (BorderFactory.createRaisedBevelBorder(), 
                               BorderFactory.createLoweredBevelBorder())); 
      //Create the JScrollPanel to house the JTextArea for messages
      JScrollPane jspScroll = new JScrollPane(jtaDisplayMsgs, 
                                              ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, 
                                              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      jtaDisplayMsgs.setEditable(false);
      //Creating the JTextField for sending messages
      JTextField jtfSendMsgs = new JTextField(15);
      //Binding the Enter key to sendMessage
      jtfSendMsgs.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "sendMessage");
      jtfSendMsgs.getActionMap().put("sendMessage", new AbstractAction(){
         public void actionPerformed(ActionEvent ae){
            //Send the message to the server
            sendMessage(jtfSendMsgs.getText());
            jtfSendMsgs.setText("");
         }
      });
      
      //EAST Panel: Incoming game messages from Server
      JPanel jpEast = new JPanel(new BorderLayout(5,5));
      //Create a JLabel to differentiate game messages from chat messages
      JLabel jlPlayerMoves = new JLabel("Player Moves");
      jlPlayerMoves.setHorizontalAlignment(JLabel.CENTER);
      jlPlayerMoves.setBackground(new Color(255,155,255));
      jlPlayerMoves.setOpaque(true);
      jlPlayerMoves.setBorder(BorderFactory.createCompoundBorder
                             (BorderFactory.createRaisedBevelBorder(), 
                              BorderFactory.createLoweredBevelBorder())); 
      //Create the JTextArea for displaying game messages. Include a border and enable word wrapping.
      jtaDisplayGM = new JTextArea(0, 15);
      jtaDisplayGM.setLineWrap(true);
      jtaDisplayGM.setWrapStyleWord(true);
      jtaDisplayGM.setBorder(BorderFactory.createCompoundBorder
                            (BorderFactory.createRaisedBevelBorder(), 
                             BorderFactory.createLoweredBevelBorder()));
      jtaDisplayGM.setEditable(false);
      //Create the JScrollPanel to house the JTextArea for game messages
      JScrollPane jspScrollGM = new JScrollPane(jtaDisplayGM, 
                                                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, 
                                                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      
      //Add components to the North, West, and East panels
      jpNorth.add(jtfAlias);
      jpNorth.add(jtfIP);
      jpNorth.add(jbRoll);
      jpNorth.add(jbConnect);
      jpNorth.add(jbDisconnect);
      jpWest.add(jspScroll, BorderLayout.CENTER);
      jpWest.add(jtfSendMsgs, BorderLayout.SOUTH);
      jpEast.add(jlPlayerMoves, BorderLayout.NORTH);
      jpEast.add(jspScrollGM, BorderLayout.CENTER);
      //Add padding to make West and East regions prettier
      jpWest.setBorder(new EmptyBorder(0, 10, 5, 10));
      jpEast.setBorder(new EmptyBorder(0, 10, 30, 10));
      //Adding the North, West, and East panels to the JFrame
      add(jpNorth, BorderLayout.NORTH);
      add(jpWest, BorderLayout.WEST);
      add(jpEast,BorderLayout.EAST);
      add(glClient = new GameLogic(5), BorderLayout.CENTER);
      //Initialize the JFrame
      setSize(1000,600);
      setDefaultCloseOperation(EXIT_ON_CLOSE);
      setVisible(true);
      setLocationRelativeTo(null);
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
         oos.writeObject(new DataWrapper(DataWrapper.STRINGCODE, message, false));
      } catch(SocketException se) {
         System.err.println("Error: not connected. Message not sent.");
      } catch(NullPointerException npe) {
         System.err.println("Error: not connected. Message not sent.");
      } catch(IOException ioe) {
         ioe.printStackTrace();
      }
   }
   
   //Listens for incoming messages/objects
   protected class ObjectListener extends Thread{
      
      /**
       * The run method for the ObjectListener class.
       * Processes incoming Objects from the server.
       */
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
            } catch(SocketException se) {
               System.err.println("Connection closed. No longer recieving server output.");
               break;
               //se.printStackTrace();
            } catch(EOFException eofe) {
               System.err.println("Error: connection closed. No longer recieving server output.");
               eofe.printStackTrace();
               break;
            } catch(ClassNotFoundException cnfe) {
               cnfe.printStackTrace();
            } catch(IOException ioe) {
               ioe.printStackTrace();
            }
            switch(dw.getType()){
               //Handle incoming messages, treating chat messages and game messages differently
               case DataWrapper.STRINGCODE:
                  String incomingMsg = dw.getMessage();
                  //If boolean isGameMessage is true, display the message in the game messages area.
                  if(dw.isGameMessage) {
                     gmLog.append(incomingMsg).append("\n");
                     jtaDisplayGM.setText(gmLog.toString());
                  //Else, display the message in the chat messages area.
                  } else {
                     chatLog.append(incomingMsg).append("\n");
                     jtaDisplayMsgs.setText(chatLog.toString());
                  }
                  break;
                  
               //Handle incoming ControlToken objects
               case DataWrapper.CTCODE:
                  ControlToken ct = dw.getCT();
                  switch(ct.getCode()){
                     case ControlToken.ENABLECODE:
                        //Enable the roll button
                        jbRoll.setEnabled(true);
                        break;
                     case ControlToken.DISABLECODE:
                        //Disable the roll button
                        jbRoll.setEnabled(false);
                        break;
                     case ControlToken.ADDCODE:
                        //Add the player to the starting tile
                        GameLogic.Player playerLabel = glClient.new Player(ct.getPlayerName());
                        glClient.addToBoard(playerLabel, glClient.getBoardSize()-1, glClient.getBoardSize()-1);
                        break;
                     case ControlToken.MOVECODE:
                        //Search the players list for the player and move that player the number of spots
                        for(GameLogic.Player player: glClient.getPlayerArrayList()){
                           if(player.getContent().equals(ct.getPlayerName())){
                              if(!ct.getOneByOne())
                                 player.move(ct.getTilesToMove());
                              if(ct.getOneByOne())
                                 player.moveOneByOne(ct.getTilesToMove());
                           }
                        }
                        break;
                     case ControlToken.REMOVECODE:
                        //Search the players list for the player and remove them
                        for(GameLogic.Player player: glClient.getPlayerArrayList()){
                           if(player.getContent().equals(ct.getPlayerName())){
                              player.remove();
                           }
                        }
                        break;
                     default:
                        System.err.println("Error: invalid ControlToken code.");
                        break;
                  }
                  break;
                  
               //Default case
               default:
                  System.err.println("Error: invalid DataWrapper type.");
                  break;
                  
            } //end of switch statement
         } //end of while loop
      }  //end of run method
   } //end of ObjectListener class
   
   /*
    * A class to ensure the client knows what each TextComponent corresponds to.
    * Will reset the TextComponent (from the FocusEvent source) to the initial
    * String to inform the client of what the TextComponent is.
    */
   protected class FocListener implements FocusListener{
      private JTextComponent jtc;
      private String initialString;
      private boolean initGet = false;
      
      /**
       * Sets the JComponent's text to the initial String
       * if the field is empty to show information about the field.
       * @param fe the FocusEvent trigger
       */
      public void focusLost(FocusEvent fe){
         //Set the component text to initial text if empty
         jtc = (JTextComponent) fe.getSource();
         if(jtc.getText().equals("")){
            jtc.setText(initialString);
         }
      }
      
      /**
       * Grabs the initial String and stores it.
       * If the text in the source's JComponent is the initial String,
       * clear the field to allow client to enter text.
       * @param fe the FocusEvent trigger
       */
      public void focusGained(FocusEvent fe){
         //Clears the text field for client to enter text
         jtc = (JTextComponent) fe.getSource();
         //Store the initial String
         if(!initGet){
            initialString = jtc.getText();
            initGet = true;
         }
         //System.out.println("Initial String: " + initialString);
         //If the text in the field equals the initialString, clear the field
         if(jtc.getText().equals(initialString)){
            jtc.setText("");
         }
      }
      
   } //end of FocListener class
} //end of Client class