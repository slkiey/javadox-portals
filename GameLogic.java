import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Vector;

/**
 * A class implementing the game logic we will use
 * in our Portals game.
 * @author Alan Chu
 * @version 20191207
 */
public class GameLogic extends JPanel implements Serializable {
    private BoardTile[][] board;
    private final int BOARD_SIZE;
    private Vector<Player> playerVector = new Vector<>();
    private ArrayList<BoardTile> bluePortals = new ArrayList<>();
    private ArrayList<BoardTile> orangePortals = new ArrayList<>();
    private final ImageIcon orangePortalIcon = new ImageIcon("media/orangePortal160by160.png");
    private final ImageIcon bluePortalIcon = new ImageIcon("media/bluePortal160by160.png");
    /**
     * Construct the GUI and make a board with the specified parameters.
     * @param boardSize the side length of the board
     */
    public GameLogic(int boardSize) {
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
                BoardTile jpIJ = new BoardTile();
                Dimension squareSz = new Dimension(800 / BOARD_SIZE, 800 / BOARD_SIZE);
                jpIJ.setPreferredSize(squareSz);
                jpIJ.setMinimumSize(squareSz);
                jpIJ.setMaximumSize(squareSz);
                jpIJ.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                board[i][j] = jpIJ;
                add(board[i][j]);
            }
        } //end of for loop

        /*
         * Hard-coding portals:
         * When you add orange portals, make sure you add in ascending board order so that when you land
         * on a portal there will be a portal to go back to.
         */
        //Orange Portals
        board[4][4].makePortal("Orange");
        board[3][3].makePortal("Orange");
        //Blue Portals
        board[3][2].makePortal("Blue");
        board[2][0].makePortal("Blue");

//        System.out.println("Orange portals list size: " + orangePortals.size());
        setVisible(true);
    }

    /**
     * Adds a player's JLabel to the specified JPanel position.
     * Informs the player if they won or overshot the last tile.
     * @param jlPlayer a player's JLabel
     * @param rows     the row position, from top to bottom
     * @param columns  the column position, from left to right
     */
    protected void addToBoard(JLabel jlPlayer, int rows, int columns, String invokingPlayer) {
        board[rows][columns].add(jlPlayer);
    }

    protected void showLocalizedMessage(String alias){
        //
    }
    /**
     * Returns the size of the board (side length).
     * @return the size of the board
     */
    public int getBoardSize() {
        return BOARD_SIZE;
    }

    /**
     * Returns the ArrayList of Players.
     * @return the ArrayList of Players
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
     * A textual representation of the board, including Players.
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

        /**
         * Constructs a Player with a name and sets the JLabel text.
         * @param _name    the player's name
         */
        public Player(String _name) {
            setText(_name);
            name = _name;
        }

        /**
         * Return a String describing the player's position.
         * @return a String with the rows and columns of the player
         */
        public String returnPos() {
            String position = String.format("%s not found on the board.", getName());
            for (int i = 0; i < board.length; i++) {
                for (int j = 0; j < board.length; j++) {
                    if (board[i][j].isAncestorOf(this)) {
                        //Add one to the indices because arrays start at 0
                        position = String.format("%s's position: Rows: %d Columns: %d\n", getName(), i + 1, j + 1);
                    }
                }
            } //end of for loop
            return position;
        }

        /**
         * Returns an int array with the rows and columns of
         * this player.
         * @return position int[rows, columns]
         */
        public int[] returnIntPos() {
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
         * Removes the player from the board.
         */
        protected void remove() {
            int[] coords = returnIntPos();
            /*
             * Get the coordinates from returnIntPos() then use them to access
             * that position in the board[][] array and call the remove() method.
             */
            board[coords[0]][coords[1]].remove(this);
            playerVector.remove(this);
            //issue: if the player isn't found the coords will be [0][0]?

        }

        protected int[] getEndPosition(int positions){
            //Grab the current coordinates
            int[] coords = returnIntPos();
            int rows = coords[0];
            int columns = coords[1];
//            System.out.printf("Starting coordinates: R%d C%d\n", rows, columns);
            int rowsTo = rows;
            int columnsTo;
            int moveBank = positions;
            int[] endPosition;
            //System.out.println("BARNEY " + positions/BOARD_SIZE);
            /*
             * Case 1: If the quantity of moves is greater or equal to 8
             *
             * First, shift the required amount of rows by dividing positions by board size
             * and subtracting the number of rows * 8 from the move bank.
             * example: 15/8 = 1, which means 1 row up
             *
             * Once you've shifted the required number of rows upwards, you must evaluate if
             * moving left by the remaining number of positions will place you at a negative index.
             *
             * If that is the case, move one row up and shift (columns - moveBank) amount left.
             * example: Moving 15 positions. You have moved 1 row up and now the moveBank is 7. But
             * your column position is at 5, so you will get a negative index. So you will move 1 row up again
             * to the last column (like in S+L) then move leftwards by 7 - 5 = 2 positions.
             *
             * If you can simply shift the remaining amount of moves leftwards without incurring
             * a negative index, do so.
             */
            if ((positions / BOARD_SIZE) >= 1) {
//                System.out.println("entered if 1");
                rowsTo = rowsTo - positions / BOARD_SIZE;
                moveBank = moveBank - positions / BOARD_SIZE * BOARD_SIZE;
                //System.out.println("MoveBank: " + moveBank);
                //System.out.println("POS/BSZ*BSZ: " + positions/BOARD_SIZE*BOARD_SIZE);
                if (columns - moveBank < 0) {
//                    System.out.println("entered if 1.5");
                    //System.out.printf("Columns:%d MoveBank:%d Pos/BO_SZ*BO_SZ:%d", columns ,moveBank, positions/BOARD_SIZE*BOARD_SIZE);
                    rowsTo--;
                    columnsTo = (BOARD_SIZE + (columns - moveBank));
                } else {
                    columnsTo = columns - moveBank;
                }
            }
            /*
             * Case 2: If the quantity of moves is NOT greater than or equal to 8,
             * but subtracting the number of positions would incur a negative index.
             *
             * Move up 1 row to the last column then move (columns - position) positions
             * left
             *
             * All a negative index means is that you should shift a row upwards to the last
             * column like in Snakes and Ladders.
             * This same logic is repeated in the first if statement because it is how you
             * should handle a situation where moving left would create an error.
             */
            else if (columns - (positions % BOARD_SIZE) < 0) {
//                System.out.println("entered if 2");
                rowsTo--;
                columnsTo = (BOARD_SIZE + (columns - positions));
            }

            /*
             * Case 3: If the quantity of moves would not cause any errors and is less than
             * 8.
             * My favorite case :)
             */
            else {
//                System.out.println("entered if 3");
                columnsTo = columns - moveBank;
            }
            endPosition = new int[]{rowsTo, columnsTo};
            return endPosition;
        }

        /**
         * Moves the player on the board according to the Snakes and Ladders
         * movement pattern.
         * @param positions number of tiles to move
         */
        protected void move(int positions, boolean ignorePortals, String invokingPlayer) {
            int[] endPositions = getEndPosition(positions);

            try {
                BoardTile endTile = board[endPositions[0]][endPositions[1]];
                addToBoard(this, endPositions[0], endPositions[1], invokingPlayer);
                if(endPositions[0] == 0 && endPositions[1] == 0){
                    JOptionPane.showMessageDialog(null, String.format("%s won!",this.getText()) );
                }
                if(!ignorePortals && (endTile.getPortalType().equals("Orange") || endTile.getPortalType().equals("Blue"))){
                    System.out.println("Landed on a " + endTile.getPortalType() + " portal!");
                    //Move to the previous orange portal if not the first portal
                    if(endTile.getPortalType().equals("Orange") && orangePortals.indexOf(endTile) > 0 ){
                        JOptionPane.showMessageDialog(null, "You landed on an orange Portal! Moving to the previous orange portal..");
                        BoardTile previousOrange = orangePortals.get(orangePortals.indexOf(endTile) - 1);
                        previousOrange.add(this);
                    }
                    //Move to the next blue portal if not the only portal
                    if (endTile.getPortalType().equals("Blue") && bluePortals.size() > 1) {
                        JOptionPane.showMessageDialog(null, "You landed on a blue Portal! Moving to the next blue portal..");
                        BoardTile nextBlue = bluePortals.get(bluePortals.indexOf(endTile) + 1);
                        nextBlue.add(this);
                    }
                }
            } catch(ArrayIndexOutOfBoundsException aioobe) {
                if (this.getText().equals(invokingPlayer)) {
                    JOptionPane.showMessageDialog(null, "You failed to land exactly on the last tile.");
                }
            }

        }

        /**
         * Moves a single tile x number of times.
         * Calls the move() method.
         * @param positions the number of tiles to move
         */
        protected void moveOneByOne(int positions, String invokingPlayer) {
            ActionListener alMv1 = new ActionListener() {
                int counter = positions;

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (counter != 0) {
                        //Move ignoring portals except for the last tile
                        if(counter == 1){
                            //System.out.println("Moving 1, not ignoring portals");
                            move(1, false, invokingPlayer);
                        } else {
                            //System.out.println("Moving 1, ignoring portals");
                            move(1, true, invokingPlayer);
                        }
                        counter--;
                    }
                }
            };
            Timer timer = new Timer(500, alMv1);
            timer.start();
        }

        /**
         * Adds this Player to the Player Vector in the GameLogic class.
         */
        protected void addToPlayersVector(){
            playerVector.add(this);
        }

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

    protected class BoardTile extends JPanel{
        private boolean isOrangePortal = false;
        private boolean isBluePortal = false;

        /**
         * Returns a textual representation of the type of portal.
         * @return a textual representation of the type of portal.
         */
        public String getPortalType(){
            String portalType = "";
            if(isOrangePortal){
                portalType = "Orange";
            } else if(isBluePortal) {
                portalType = "Blue";
            }
            return portalType;
        }

        /**
         * Makes this tile an orange or blue tile.
         * Sets the isOrangePortal/isBluePortal booleans depending on the parameters.
         * Changes the color of the tile depending on the parameters.
         * @param portalColor the type of tile you wish to change this tile to
         */
        public void makePortal(String portalColor){
            if(portalColor.equals("Orange") && !isOrangePortal){
                isOrangePortal = true;
                orangePortals.add(this);
                this.setBackground(Color.ORANGE);
//                JLabel orangePortalBG = new JLabel();
//                orangePortalBG.setIcon(orangePortalIcon);
//                add(orangePortalBG);
            } else if(portalColor.equals("Blue") && !isBluePortal) {
                isBluePortal = true;
                bluePortals.add(this);
                this.setBackground(Color.CYAN);
//                JLabel bluePortalBG = new JLabel();
//                bluePortalBG.setIcon(bluePortalIcon);
//                add(bluePortalBG);
            } else {
                System.out.println("That board is already a tile.");
            }
        }


    }
}


