import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.event.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.text.JTextComponent;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;

/**
 * The client class for the Portals game.
 * Team Javadox
 * @author Alan Chu
 * @version 20191218
 */
public class Client extends JFrame {
   private final int PORT = 4242;
   private final int BOARD_SIZE = 10;
   private GameLogic glClient;
   private String ip;
   private String alias;
   private ObjectInputStream ois;
   private ObjectOutputStream oos;
   private Socket sock;
   private JFrame jfMainMenu;
   private JTextField jtfAlias;
   private JTextArea jtaDisplayMsgs;
   private JTextArea jtaDisplayGM;
   private JButton jbRoll;
   private StringBuilder chatLog = new StringBuilder();
   private StringBuilder gmLog = new StringBuilder();
   private JPanel jpCenter;
   private final String portalLogoPath = "media/portalsLogo.png";

   /**
    * Default constructor for the Client class.
    * Creates the main menu GUI.
    */
   public Client(){
      //Initialize variables
      ActionListener alMainMenu;
      JButton jbPlay;
      JButton jbHelp;
      JButton jbExit;

      //Create main menu GUI
      jfMainMenu = new JFrame();
      JPanel jpMainMenu = new JPanel();
      jpMainMenu.setPreferredSize(new Dimension(500, 750));
      jpMainMenu.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
      JLabel jlPortalsLogo = new JLabel("", SwingConstants.CENTER);
      //Load the portal image and set the label's icon to it
      BufferedImage biPortalsLogo = null;
      ImageIcon iiPortalsLogo;
      try {
         biPortalsLogo = ImageIO.read(getClass().getResourceAsStream(portalLogoPath));
      } catch (IOException ioe) {
         ioe.printStackTrace();
      }
      iiPortalsLogo = new ImageIcon(biPortalsLogo);
      jlPortalsLogo.setIcon(iiPortalsLogo);


      //Create buttons and configure their properties
      Dimension buttonDimension = new Dimension(250, 150);
      jbPlay = new JButton("PLAY");
      jbPlay.setFont(new Font("Verdana", Font.BOLD, 35));
      jbPlay.setPreferredSize(buttonDimension);

      jbHelp = new JButton("HELP");
      jbHelp.setFont(new Font("Verdana", Font.BOLD, 35));
      jbHelp.setPreferredSize(buttonDimension);

      jbExit = new JButton("EXIT");
      jbExit.setFont(new Font("Verdana", Font.BOLD, 35));
      jbExit.setPreferredSize(buttonDimension);

      jpMainMenu.add(jlPortalsLogo);
      jpMainMenu.add(jbPlay);
      jpMainMenu.add(jbHelp);
      jpMainMenu.add(jbExit);
      //Create ActionListener
      alMainMenu = new ActionListener() {
         @Override
         public void actionPerformed(ActionEvent ae) {
            Object eoSource = ae.getSource();
            if(eoSource == jbPlay){
               jfMainMenu.dispose();
               createGameGUI();
            }
            if(eoSource == jbHelp){
               createHelpGUI();
            }
            if(eoSource == jbExit){
               System.exit(0);
            }

         }
      };
      //Add the actionListener
      jbPlay.addActionListener(alMainMenu); jbHelp.addActionListener(alMainMenu); jbExit.addActionListener(alMainMenu);
      //Add the JPanel to the JFrame
      jfMainMenu.add(jpMainMenu);

      //Initialize the JFrame
      jfMainMenu.pack();
      jfMainMenu.setTitle("Portals Main Menu");
      jfMainMenu.setDefaultCloseOperation(EXIT_ON_CLOSE);
      jfMainMenu.setVisible(true);
      jfMainMenu.setLocationRelativeTo(null);
   }
   
   /**
    * The main method.
    * @param args arguments from the command line
    */
   public static void main(String[] args){
      new Client();
   }

   /**
    * Create the Portals board GUI.
    * This is where the actual game is played.
    */
   public void createGameGUI(){
      JFrame gameGUI = new JFrame();
      //NORTH Panel: Alias, Roll Dice, IP, Connect, Disconnect
      JPanel jpNorth = new JPanel();
      //Creating the alias text field
      jtfAlias = new JTextField("Enter alias", 10);
      jtfAlias.setToolTipText("Enter name here");
      jtfAlias.addFocusListener(new FocListener());
      //Creating the IP text field
      JTextField jtfIP = new JTextField("127.0.0.1", 10);
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
               System.out.println("Clearing the board and vector of " + glClient.getPlayerVector().size() + " players.");
               clearBoard();
               glClient = new GameLogic(BOARD_SIZE, false);
               addBoard(glClient);
               sock.close();
               jbDisconnect.setEnabled(false);
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
               else if(ip.equals("Enter IP Address")) {
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
            try{
               oos.writeObject(new DataWrapper(DataWrapper.CTCODE, new ControlToken(ControlToken.ROLLREQUESTCODE, alias)));
               oos.flush();

               jbRoll.setEnabled(false);
               //Use a timer to check every 50ms  until turnFinished is true. Other implementations tend to freeze the code.
               glClient.resetTurnFinished();
               java.util.Timer timer = new java.util.Timer();
               TimerTask ttQueryTurnFinished = new TimerTask() {
                  @Override
                  public void run() {
                     System.out.println("turnFinished boolean: " + glClient.getTurnFinished());
                     if (glClient.getTurnFinished()) {
                        System.out.println("Turn finished, sending control token.");
                        try {
                           oos.writeObject(
                                   new DataWrapper(
                                           DataWrapper.CTCODE, new ControlToken(ControlToken.TURNFINISHEDCODE)));
                           oos.flush();
                        } catch (IOException e) {
                           e.printStackTrace();
                        }
                        timer.cancel();
                     }
                  }
               };
               timer.schedule(ttQueryTurnFinished, 0 , 50);


            } catch(NullPointerException npe) {
               System.err.println("Error: you are not connected.");
               //npe.printStackTrace();
            } catch(IOException ioe) {
               System.err.println("Error: IOException: " + ioe.getMessage());
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
      DefaultCaret caret = (DefaultCaret)jtaDisplayGM.getCaret();
      caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
      //Create the JScrollPanel to house the JTextArea for game messages
      JScrollPane jspScrollGM = new JScrollPane(jtaDisplayGM,
              ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

      //CENTER Panel: Container for the board, helps with sizing
      jpCenter = new JPanel();

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
      jpCenter.add(glClient = new GameLogic(BOARD_SIZE, false));

      //Add padding to make West and East regions prettier
      jpWest.setBorder(new EmptyBorder(0, 10, 5, 10));
      jpEast.setBorder(new EmptyBorder(0, 10, 30, 10));

      //Adding the North, West, and East panels to the JFrame
      gameGUI.add(jpNorth, BorderLayout.NORTH);
      gameGUI.add(jpWest, BorderLayout.WEST);
      gameGUI.add(jpEast,BorderLayout.EAST);
      gameGUI.add(jpCenter, BorderLayout.CENTER);

      //Use a Timer to repaint and re-validate every 30 milliseconds (could also do this in the addToBoard method)
      ActionListener alRefresh = new ActionListener() {
         public void actionPerformed(ActionEvent ae) {
            gameGUI.repaint();
            gameGUI.revalidate();
         }
      };
      Timer timer = new Timer(30, alRefresh);
      timer.start();

      //Initialize the JFrame
      gameGUI.setTitle("Portals Game Client");
      gameGUI.pack();
//      gameGUI.setSize(1350, 900);
      gameGUI.setDefaultCloseOperation(EXIT_ON_CLOSE);
      gameGUI.setVisible(true);
      gameGUI.setLocationRelativeTo(null);
   }

   /**
    * Create a new window where the game's instructions are displayed.
    */
   public void createHelpGUI(){
      JFrame jfHelp = new JFrame("Help Page");
      JTextArea jtaHelp = new JTextArea();
      jtaHelp.setFont(new Font("Verdana", Font.PLAIN, 14));
      jtaHelp.setEditable(false);
      jtaHelp.setText(String.format("--- HOW TO PLAY ---\n" +
              "1. The host will start the server on their machine and gives the other players the \n" +
              "   IP to connect with. \n" +
              "   \n" +
              "   If Servers and Clients are not started from the same network, you will have to\n" +
              "   forward port 4242 and connect using the Server's public IP.\n" +
              "   \n" +
              "2. Players launch their clients, enter the server IP, and type their desired alias.\n" +
              "\n" +
              "3. After all players have connected, the host can start the game via the \"startgame\" command,\n" +
              "   which will create and begin the dice rolling queue.\n" +
              "   \n" +
              "4. When it is a player's turn (they will know because their \"Roll Die\" button will be enabled) \n" +
              "   they can click the button to roll a random number. This number will then move the player \n" +
              "   that many spaces forward.\n" +
              "   \n" +
              "5. If a player lands on a portal, that player will either teleport backwards to the \n" +
              "   previous orange portal (if there are any) or forwards to the next blue portal.\n" +
              "   \n" +
              "6. The first player to reach the end of the board wins!\n" +
              "\n" +
              "7. If you want to play again, simply repeat steps 1-6.\n" +
              "\n" +
              "Author: Kenny Scott\n" +
              "Revised by Alan Chu"));
      jfHelp.add(jtaHelp);
      //Initialize JFrame
      jfHelp.pack();
      jfHelp.setVisible(true);
      jfHelp.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
   }

   /**
    * Sends a message via ObjectOutputStream.
    * Creates a DataWrapper with the String code: 0 and the message.
    * @param message the message to be sent.
    */
   public void sendMessage(String message){
      try{
         oos.writeObject(new DataWrapper(DataWrapper.STRINGCODE, message, false));
      } catch(SocketException | NullPointerException snpe) {
         System.err.println("Error: not connected. Message not sent.");
      } catch(IOException ioe) {
         ioe.printStackTrace();
      }
   }

   /**
    * Gets an updated BoardInformation object from the GameLogic.
    * BoardInformation obtains its values in it's default constructor.
    * @return an updated BoardInformation object
    */
   public GameLogic.BoardInformation getBoardInformation() {
      return glClient.getUpdatedBoardInformation();
   }

   /**
    * Removes the current board JPanel from jpCenter.
    */
   public void clearBoard(){
      jpCenter.remove(glClient);
   }

   /**
    * Adds a board JPanel to jpCenter.
    * @param gl the GameLogic JPanel to be added
    */
   public void addBoard(GameLogic gl){
      jpCenter.add(gl);
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
               JOptionPane.showMessageDialog(null, "Connection closed. No longer receiving server output.");
               break;
               //se.printStackTrace();
            } catch(EOFException eofe) {
               JOptionPane.showMessageDialog(null, "Connection closed. No longer receiving server output.");
               eofe.printStackTrace();
               break;
            } catch(ClassNotFoundException | IOException cnfe) {
               cnfe.printStackTrace();
            }
            switch(dw.getType()){
               //Handle incoming messages, treating chat messages and game messages differently
               case DataWrapper.STRINGCODE:
                  String incomingMsg = dw.getMessage();
                  //If boolean isGameMessage is true, display the message in the game messages area.
                  if(dw.getIsGameMessage()) {
                     gmLog.append(incomingMsg).append("\n");
                     jtaDisplayGM.setText(gmLog.toString());
                  //Else, display the message in the chat messages area.
                  } else {
                     chatLog.append(incomingMsg).append("\n");
                     jtaDisplayMsgs.setText(chatLog.toString());
                  }
                  break;

               //Handle updated BoardInformation
               case DataWrapper.BICODE:
                  System.out.println("Recieved a BoardInformation:");
                  System.out.println(dw.getBoardInformation().toString());
                  glClient.unpackBoardInformation(dw.getBoardInformation());
                  break;

               //Handle incoming ControlToken objects
               case DataWrapper.CTCODE:
                  ControlToken ct = dw.getCT();
                  switch(ct.getCode()){
                     //Enable the roll button
                     case ControlToken.ENABLECODE:
                        jbRoll.setEnabled(true);
                        break;

                     //Disable the roll button
                     case ControlToken.DISABLECODE:
                        jbRoll.setEnabled(false);
                        break;

                     //Add the player to the starting tile if they're not already on the board
                     case ControlToken.ADDCODE:
                        System.out.println("print an add request");
                        if(!glClient.containsPlayer(ct.getPlayerName())) {
                           GameLogic.Player playerToBeAdded = glClient.new Player(ct.getPlayerName(), ct.getPawnCode());
                           glClient.addToBoard(
                                   playerToBeAdded, glClient.getBoardSize() - 1, glClient.getBoardSize() - 1);
                           glClient.addToPlayersVector(playerToBeAdded);
                        }
                        break;

                     //Search the players list for the player and move that player the number of spots
                     case ControlToken.MOVECODE:
                        for(GameLogic.Player player: glClient.getPlayerVector()){
                           if(player.getContent().equals(ct.getPlayerName())){
                              if(!ct.getOneByOne())
                                 player.move(ct.getTilesToMove(), false, alias);
                              if(ct.getOneByOne())
                                 player.moveOneByOne(ct.getTilesToMove(), alias);
                           }
                        }
                        break;

                     //Remove the specified player from the board and Player Vector
                     case ControlToken.REMOVECODE:
                        for (GameLogic.Player player : glClient.getPlayerVector()) {
                           if (player.getContent().equals(ct.getPlayerName())) {
                              player.remove();
                              break;
                           }
                        }
                        break;

                     //Write BoardInformation to the server
                     case ControlToken.BOARDREQUEST:
                        try {
                           oos.reset();
                           oos.writeObject(new DataWrapper(DataWrapper.BICODE, getBoardInformation()));
                           oos.flush();
                        } catch (IOException ioe) {
                           ioe.printStackTrace();
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