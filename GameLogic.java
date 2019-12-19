import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;

/**
 * A class that displays the game board, controls portal placement, and moves players.
 * Team Javadox
 * @author Alan Chu
 * @version 20191218
 */
public class GameLogic extends JPanel implements Serializable {
    private BoardTile[][] board;
    private final int BOARD_SIZE;
    private static final long serialVersionUID = 41L;
    private Vector<Player> playerVector = new Vector<>();
    private ArrayList<BoardTile> bluePortals = new ArrayList<>();
    private ArrayList<BoardTile> orangePortals = new ArrayList<>();
    private volatile boolean turnFinished = false;
    private final String orangePortalIconPath = "media/orangeportal160by160.png";
    private final String bluePortalIconPath = "media/blueportal160by160.png";
    private final String floorIconPath = "media/floorTexture.png";
    private final Random rand = new Random();

    /**
     * Construct the GUI and make a board with the specified parameters.
     * @param boardSize the side length of the board
     */
    public GameLogic(int boardSize, boolean generatePortals) {
        BOARD_SIZE = boardSize;

        //Panel initialization
        Dimension panelSz = new Dimension(800,800);
        setPreferredSize(panelSz);
        setMinimumSize(panelSz);
        setMaximumSize(panelSz);
        setLayout(new GridLayout(BOARD_SIZE, BOARD_SIZE));

        //Create a 2D array of JPanels and fill it up. We do this so we can reference them later in the code.
        board = new BoardTile[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                BoardTile jpIJ = new BoardTile(i, j);
                Dimension squareSize = new Dimension(800 / BOARD_SIZE, 800 / BOARD_SIZE);
                jpIJ.setPreferredSize(squareSize);
                jpIJ.setMinimumSize(squareSize);
                jpIJ.setMaximumSize(squareSize);
                jpIJ.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
                board[i][j] = jpIJ;
                add(board[i][j]);
            }
        }

        if(generatePortals) {
            //Generate portals randomly
            generatePortals(10, "Blue");
            generatePortals(10, "Orange");
        }

        //Fun checkerboard pattern
//        for (int i = 0; i < BOARD_SIZE; i++) {
//            for (int j = 0; j < BOARD_SIZE; j++) {
//                if( (i % 2 == 1 && j % 2 == 1 ) || (i % 2 == 0 && j % 2 == 0 ) ) {
//                    board[i][j].makePortal("Blue");
//                }
//                else{
//                    board[i][j].makePortal("Orange");
//                }
//            }
//        }
        setVisible(true);
    }

    /**
     * Adds a player's JLabel to the specified JPanel position.
     * @param jlPlayer a player's JLabel
     * @param rows     the row position, from top to bottom
     * @param columns  the column position, from left to right
     */
    protected void addToBoard(JLabel jlPlayer, int rows, int columns) {
        board[rows][columns].add(jlPlayer);
    }

    /**
     * Randomly generate a quantity of the specified portal.
     * @param quantity the number of portals to generate
     * @param portalType the type of portal to generate
     */
    protected void generatePortals(int quantity, String portalType){
        for(int i = 0; i < quantity; i++){
            int randInt1 = rand.nextInt(BOARD_SIZE);
            int randInt2 = rand.nextInt(BOARD_SIZE);
            //If the tile is already a portal or the tile is the last tile, generate new numbers
            while(    (randInt1 == 0 && randInt2 == 0)
                    || board[randInt1][randInt2].getPortalType().equals("Orange")
                    || board[randInt1][randInt2].getPortalType().equals("Blue")) {
                randInt1 = rand.nextInt(BOARD_SIZE);
                randInt2 = rand.nextInt(BOARD_SIZE);
            }
            board[randInt1][randInt2].makePortal(portalType);
        }
    }

    /**
     * Resets the turnFinished boolean to false for the next turn iteration.
     */
    protected void resetTurnFinished() {
        turnFinished = false;
    }

    /**
     * Adds this Player to the Player Vector in the GameLogic class.
     */
    protected void addToPlayersVector(Player player){
        playerVector.add(player);
    }

    /**
     * Compares the current player with the player who called the method (in this case, move() ).
     * If they are the same, displays the message on this client's screen.
     * @param currentPlayer the player being moved
     * @param invokingPlayer who called the move method (see Client class)
     * @param message the message to display
     */
    protected void showLocalizedMessage(String currentPlayer, String invokingPlayer, String message) {
        if(currentPlayer.equals(invokingPlayer)){
            JOptionPane.showMessageDialog(null, message);
        }
    }

    /**
     * Takes a BoardInformation class and displays portals and players accordingly.
     * @param boardInfo the BoardInformation object to unpack
     */
    protected void unpackBoardInformation(BoardInformation boardInfo){
        int[] playerPawnCodes = boardInfo.getPlayerPawnCodes();
        String[] playerNames = boardInfo.getPlayerNames();
        int[][] playerLocations = boardInfo.getPlayerPositions();
        int[][] orangePortalLocations = boardInfo.getOrangePortalPositions();
        int[][] bluePortalLocations = boardInfo.getBluePortalPositions();

        //Add the players if there are any
        if(playerNames.length != 0) {
            for (int i = 0; i <= playerNames.length - 1; i++) {
                Player player = new Player(playerNames[i], playerPawnCodes[i]);
                addToBoard(player, playerLocations[i][0], playerLocations[i][1]);
                addToPlayersVector(player);
            }
        }
        for(int[] orangePortalLocation: orangePortalLocations){
            BoardTile tileToBeOrangePortal = board[orangePortalLocation[0]][orangePortalLocation[1]];
            tileToBeOrangePortal.makePortal("Orange");
//            orangePortals.add(tileToBeOrangePortal);
            //no need to add to orangePortals, automatically added when makePortal is called
        }

        for(int[] bluePortalLocation: bluePortalLocations){
            BoardTile tileToBeBluePortal = board[bluePortalLocation[0]][bluePortalLocation[1]];
            tileToBeBluePortal.makePortal("Blue");
//            bluePortals.add(tileToBeBluePortal);
            //no need to add to bluePortals, automatically added when makePortal is called
        }
    }

    /**
     * Returns the size of the board (side length).
     * @return the size of the board
     */
    public int getBoardSize() {
        return BOARD_SIZE;
    }

    /**
     * Returns the Vector of Players.
     * @return the Vector of Players
     */
    public Vector<Player> getPlayerVector() {
        return playerVector;
    }

    /**
     * Checks the Player Vector to see if a Player with the same name is in it.
     * @param name the name to check for in the Vector
     * @return whether or not the Player is in the Vector
     */
    public boolean containsPlayer(String name) {
        boolean bool = false;
        for (Player p : playerVector) {
            if (p.getContent().equals(name)) {
                bool = true;
                return bool;
            }
        }
        return bool;
    }

    /**
     * Return the turn finished boolean.
     * @return the turn finished boolean.
     */
    public boolean getTurnFinished(){
        return turnFinished;
    }

    /**
     * Returns an updated BoardInformation object.
     * Calls the constructor of BoardInformation, which generates updated board information based on the
     * playerVector, orangePortals, and bluePortals lists.
     * @return an updated BoardInformation object
     */
    public BoardInformation getUpdatedBoardInformation(){
        return new BoardInformation();
    }
    /**
     * Returns a description of all players on the board.
     * @return a String describing the board
     */
    public String toString(){
        StringBuilder sbGL = new StringBuilder();
        sbGL.append("Board Summary");
        sbGL.append("\n");
        for(Player p: playerVector){
            sbGL.append(p.returnPos());
        }
        return sbGL.toString();
    }

    protected class Player extends JLabel {
        private final String name;
        private static final long serialVersionUID = 42L;
        private final int pawnCode;

        /**
         * Constructs a Player with a name and pawnCode.
         * @param _name    the player's name
         * @param _pawnCode a number from 0-7 referring to the pawn image's file name
         */
        public Player(String _name, int _pawnCode) {
            pawnCode = _pawnCode;
            //Set this JLabel's icon to a colored pawn
            String pawnIconPath = "media/" + pawnCode + ".png";
            ImageIcon icon = getSizedPawnIcon(pawnIconPath);
            setIcon(icon);

            setText(_name);
            name = _name;
        }

        /**
         * Return a String describing the player's position.
         * @return a String with the rows and columns of the player
         */
        protected String returnPos() {
            String position = String.format("%s not found on the board.\n", getName());
            for (int i = 0; i < board.length; i++) {
                for (int j = 0; j < board.length; j++) {
                    if (board[i][j].isAncestorOf(this)) {
                        //Add one to the indices because arrays start at 0
                        position = String.format("%s's position: Rows: %d Columns: %d\n", getName(), i + 1, j + 1);
                        return position;
                    }
                }
            } //end of for loop
            return position;
        }

        /**
         * Returns an int array with the row and column position of this player.
         * @return position int[rows, columns]
         */
        protected int[] returnIntPos() {
            int[] position = new int[2];
            for (int i = 0; i < board.length; i++) {
                for (int j = 0; j < board.length; j++) {
                    if (board[i][j].isAncestorOf(this)) {
                        position[0] = i;
                        position[1] = j;
                    }
                }
            } //end of for loop
            return position;
        }

        /**
         * Calculates the end position of a player from their current position according to the
         * Snakes and Ladders movement pattern.
         * @param positions number of tiles to move
         * @return the calculated end position
         */
        protected int[] getEndPosition(int positions){
            //Grab the current coordinates
            int[] coords = returnIntPos();
            int rows = coords[0];
            int columns = coords[1];
            //Variables to help track end position
            int rowsTo = rows;
            int columnsTo;
            int moveBank = positions;

            if ((positions / BOARD_SIZE) >= 1) {
                rowsTo = rowsTo - positions / BOARD_SIZE;
                moveBank = moveBank - positions / BOARD_SIZE * BOARD_SIZE;
                if (columns - moveBank < 0) {
                    rowsTo--;
                    columnsTo = (BOARD_SIZE + (columns - moveBank));
                } else {
                    columnsTo = columns - moveBank;
                }
            }

            else if (columns - (positions % BOARD_SIZE) < 0) {
                rowsTo--;
                columnsTo = (BOARD_SIZE + (columns - positions));
            }

            else {
                columnsTo = columns - moveBank;
            }

            return new int[]{rowsTo, columnsTo};
        }

        /**
         * Returns a correctly sized ImageIcon of a pawn from an image path.
         * @param iconPath the path to the pawn image
         * @return an ImageIcon of a pawn resized to fit the board
         */
        protected ImageIcon getSizedPawnIcon(String iconPath){
            ImageIcon iiPawnImage;
            BufferedImage biPawnImage = null;
            Image resizedPawnImage;
            //Read the image in from the iconPath
            try {
                biPawnImage = ImageIO.read(getClass().getResourceAsStream(iconPath));
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            //Resize the image according to the board size
            resizedPawnImage = biPawnImage.getScaledInstance(800 / BOARD_SIZE / 4, 800 / BOARD_SIZE / 4, Image.SCALE_DEFAULT);
            iiPawnImage = new ImageIcon(resizedPawnImage);
            return iiPawnImage;
        }

        /**
         * Removes the player from the board.
         */
        protected void remove() {
            int[] coords = returnIntPos();
            board[coords[0]][coords[1]].remove(this);
            playerVector.remove(this);
            //issue: if the player isn't found the coords will be [0][0]?
        }

        /**
         * Moves the player on the board according to the Snakes and Ladders
         * movement pattern.
         * @param positions number of tiles to move
         */
        protected void move(int positions, boolean ignorePortals, String invokingPlayer) {
            int[] endPositions = getEndPosition(positions);

            //If you overshoot, don't add to the board but display a message.
            if(endPositions[0] < 0 || endPositions[1] < 0){
                showLocalizedMessage(getName(), invokingPlayer,"You failed to land exactly on the last tile.");
            }
            //If you land on the last tile, announce to everyone that you have won the game.
            if(endPositions[0] == 0 && endPositions[1] == 0){
                addToBoard(this, endPositions[0], endPositions[1]);
                JOptionPane.showMessageDialog(null, String.format("%s won!",this.getText()) );
                System.exit(0);
            }
            //For all other tiles, add to the board and evaluate the portal state
            else {
                BoardTile endTile = board[endPositions[0]][endPositions[1]];
                addToBoard(this, endPositions[0], endPositions[1]);
                String tilePortalType = endTile.getPortalType();
                if(!ignorePortals && !tilePortalType.equals("")){
                    //Move to the previous orange portal
                    if(tilePortalType.equals("Orange")){
                        //If no previous orange portal was found, nextPortal will return the same tile.
                        if(endTile == nextPortal(endPositions, "Orange")){
                            showLocalizedMessage(getName(), invokingPlayer, "You landed on an orange Portal! But there were no previous portals.");
                        } else {
                            showLocalizedMessage(getName(), invokingPlayer, "You landed on an orange Portal! Moving to the previous orange portal..");
                        }
                        nextPortal(endPositions, "Orange").add(this);
                    }
                    //Move to the next blue portal
                    if (tilePortalType.equals("Blue")) {
                        //If no next blue portal was found, nextPortal will return the same tile.
                        if(endTile == nextPortal(endPositions, "Blue")){
                            showLocalizedMessage(getName(), invokingPlayer,"You landed on a blue Portal! But there were no forward blue portals.");
                        } else {
                            showLocalizedMessage(getName(), invokingPlayer, "You landed on a blue Portal! Moving to the next blue portal..");
                        }
                        nextPortal(endPositions, "Blue").add(this);
                    }
                }
            }
        }

        /**
         * Returns the next or previous portal from a position depending on portal color.
         * @param position the current position of the player
         * @param portalColor the color of the portal to search for
         * @return the previous (if orange) or next (if blue) portal
         */
        protected BoardTile nextPortal(int[] position, String portalColor){
            BoardTile currentTile = board[position[0]][position[1]];
            //Search for the next blue tile
            if(portalColor.equals("Blue")){
                for (int i = position[0]; i >= 0; i--) {
                    if(i == position[0]){
                        //Search through the current row first from right to left from the current column (exclusive)
                        for(int curCol = position[1] - 1; curCol >= 0; curCol--){
                            if(board[i][curCol].getPortalType().equals(portalColor)){
                                return board[i][curCol];
                            }
                        }
                    } else {
                        //For all other rows, search through the entire column from right to left
                        for (int j = BOARD_SIZE - 1; j >= 0; j--) {
                            if (board[i][j].getPortalType().equals(portalColor)) {
                                return board[i][j];
                            }
                        }
                    }
                }
            }
            if(portalColor.equals("Orange")){
                for (int i = position[0]; i < BOARD_SIZE; i++) {
                    if(i == position[0]){
                        //Search through the current row first from left to right from the current column (exclusive)
                        for(int curCol = position[1] + 1; curCol < BOARD_SIZE; curCol++){
                            if(board[i][curCol].getPortalType().equals(portalColor)){
                                return board[i][curCol];
                            }
                        }
                    } else {
                        //For all other rows, search through the entire column from right to left
                        for (int j = 0; j < BOARD_SIZE; j++) {
                            if (board[i][j].getPortalType().equals(portalColor)) {
                                return board[i][j];
                            }
                        }
                    }
                }
            }
            //Returns the currentTile if can't find the next portal
            return currentTile;
        }

        /**
         * Moves a single tile x number of times.
         * Calls the move() method.
         * @param positions the number of tiles to move
         */
        protected void moveOneByOne(int positions, String invokingPlayer) {
            int[] endPositions = getEndPosition(positions);

            //If you're going to overshoot, display an error message instead.
            if(endPositions[0] < 0 ) {
                showLocalizedMessage(getName(), invokingPlayer,"You failed to land exactly on the last tile.");
                turnFinished = true;
            } else {
                ActionListener alMv1 = new ActionListener() {
                    int counter = positions;

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (counter != 0) {
                            //Move ignoring portals except for the last tile
                            if (counter == 1) {
                                move(1, false, invokingPlayer);
                                turnFinished = true;
                            } else {
                                move(1, true, invokingPlayer);
                            }
                            counter--;
                        }
                    }
                };
                Timer timer = new Timer(500, alMv1);
                timer.start();
            }

        }

        public int getPawnCode() { return pawnCode; }
        /**
         * Returns the player's name.
         * @return the player's name
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the content of the JLabel.
         * @return the content of the JLabel
         */
        public String getContent() {
            return getText();
        }

    }

    protected class BoardTile extends JPanel {
        private boolean isOrangePortal = false;
        private boolean isBluePortal = false;
        private static final long serialVersionUID = 43L;
        private ImageIcon portalIcon;
        private ImageIcon tileIcon;
        private boolean portalImageLoaded = false;
        private int rowIndex;
        private int columnIndex;

        public BoardTile(int _rowIndex, int _columnIndex){
            //Store rowIndex and columnIndex
            rowIndex = _rowIndex;
            columnIndex = _columnIndex;
            //Load the floorTexture image by default
            Image tileImage = null;
            try {
                tileImage = ImageIO.read(getClass().getResourceAsStream(floorIconPath));
            } catch (IOException e) {
                e.printStackTrace();
            }
            tileIcon = new ImageIcon(tileImage.getScaledInstance(800 / BOARD_SIZE, 800 / BOARD_SIZE, Image.SCALE_DEFAULT));
        }

        /**
         * Returns the rows and columns of this tile.
         * @return an integer array of [rows, columns] describing the location of this tile on the grid
         */
        public int[] getTileIntPos(){
            return new int[]{rowIndex, columnIndex};
        }

        /**
         * Returns a textual representation of the type of portal.
         *
         * @return a textual representation of the type of portal. Returns "" if not a portal.
         */
        public String getPortalType() {
            String portalType = "";
            if (isOrangePortal) {
                portalType = "Orange";
            } else if (isBluePortal) {
                portalType = "Blue";
            }
            return portalType;
        }

        /**
         * Makes this tile an orange or blue tile.
         * Sets the isOrangePortal/isBluePortal booleans depending on the parameters.
         * Changes the color of the tile depending on the parameters.
         *
         * @param portalColor the type of tile you wish to change this tile to
         */
        public void makePortal(String portalColor) {
            if (portalColor.equals("Orange") && !isOrangePortal) {
                isOrangePortal = true;
                loadPortalIcon();
                orangePortals.add(this);
                this.setBackground(Color.ORANGE);
            } else if (portalColor.equals("Blue") && !isBluePortal) {
                isBluePortal = true;
                loadPortalIcon();
                bluePortals.add(this);
                this.setBackground(Color.CYAN);
            } else {
                System.out.println("That tile is already a portal.");
            }
            //Set portalImageLoaded so that paintComponent knows to draw the image
            portalImageLoaded = true;
        }

        /**
         * Loads the corresponding portal image according to the type of portal.
         */
        public void loadPortalIcon() {
            Image portalImage = null;
            try {
                if (isOrangePortal) {
                    portalImage = ImageIO.read(getClass().getResourceAsStream(orangePortalIconPath));
                } else if (isBluePortal) {
                    portalImage = ImageIO.read(getClass().getResourceAsStream(bluePortalIconPath));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            portalIcon = new ImageIcon(
                    portalImage.getScaledInstance(800 / BOARD_SIZE, 800 / BOARD_SIZE, Image.SCALE_DEFAULT));
        }

        /**
         * This JPanel's paintComponent method.
         * Draws the portalImage background.
         *
         * @param g the graphics context in which to paint
         */
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            //Draws the portal icon if the image has been loaded
            if(portalImageLoaded) {
                g.drawImage(portalIcon.getImage(), 0, 0, this);
            }
            else {
                g.drawImage(tileIcon.getImage(), 0, 0, this);
            }

        }
    }

    protected class BoardInformation implements Serializable {
        private String[] playerNames = new String[playerVector.size()];
        private int[] playerPawnCodes = new int[playerVector.size()];
        private int[][] playerPositions = new int[playerVector.size()][2];
        private int[][] orangePortalPositions = new int[orangePortals.size()][2];
        private int[][] bluePortalPositions = new int[orangePortals.size()][2];

        public BoardInformation(){
            //Get player names and store them in playerNames
            for(int i = 0; i < playerVector.size() ; i ++){
                String playerName = playerVector.get(i).getName();
                playerNames[i] = playerName;
            }
            //Get player pawn codes and store them in playerPawnCodes
            for(int i = 0; i < playerVector.size() ; i ++){
                int playerPawnCode = playerVector.get(i).getPawnCode();
                playerPawnCodes[i] = playerPawnCode;
            }
            //Get player positions and store them in playerPositions
            for(int i = 0; i < playerVector.size() ; i ++){
                int[] playerPosition = playerVector.get(i).returnIntPos();
                playerPositions[i] = playerPosition;
            }
            //Get orange portal positions and store them in orangePortalPositions
            for(int i = 0; i < orangePortals.size() ; i ++){
                int[] orangePortalPosition = orangePortals.get(i).getTileIntPos();
                orangePortalPositions[i] = orangePortalPosition;
            }
            //Get blue portal positions and store them in bluePortalPositions
            for(int i = 0; i < bluePortals.size() ; i ++){
                int[] bluePortalPosition = bluePortals.get(i).getTileIntPos();
                bluePortalPositions[i] = bluePortalPosition;
            }
        }

        public int[] getPlayerPawnCodes() {
            return playerPawnCodes;
        }

        public String[] getPlayerNames() {
            return playerNames;
        }

        public int[][] getPlayerPositions() {
            return playerPositions;
        }

        public int[][] getOrangePortalPositions() {
            return orangePortalPositions;
        }

        public int[][] getBluePortalPositions() {
            return bluePortalPositions;
        }

        @Override
        public String toString() {
            return String.format("Players List Size: %d\nOrange Portals List Size: %d\nBluePortals List Size: %d",
                    playerNames.length, orangePortalPositions.length, bluePortalPositions.length);
        }
    }
}


