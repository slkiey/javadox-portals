import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
/**
 * A class implementing the game logic we will use
 * in our Portals game.
 * @author Alan Chu
 * @version 20191207
 */
public class GameLogic extends JPanel {
    private JPanel[][] board;
    private final int BOARD_SIZE;
    private JPanel jpBoard;
    private ArrayList<Player> playerArrayList = new ArrayList<>();

    /**
     * Construct the GUI and make a board with the specified parameters.
     * @param boardSize the side length of the board
     */
    public GameLogic(int boardSize) {
        BOARD_SIZE = boardSize;

        //Center Panel: The Game Board
        jpBoard = new JPanel();
        jpBoard.setMinimumSize(new Dimension(800, 800));
        jpBoard.setLayout(new GridLayout(BOARD_SIZE, BOARD_SIZE));
        //Create a 2D array of JPanels and fill it up. We do this so we can reference them later in the code.
        board = new JPanel[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                JPanel jpIJ = new JPanel();
                jpIJ.setPreferredSize(new Dimension(100, 100));
                jpIJ.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                board[i][j] = jpIJ;
                jpBoard.add(board[i][j]);
            }
        } //end of for loop
        add(jpBoard, BorderLayout.CENTER);

        //Some testing with a swing Timer
        ActionListener alRefresh = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                repaint();
                revalidate();
            }
        };
        //Repaint and re-validate every 30 milliseconds (could also do this in the addToBoard method)
        Timer timer = new Timer(30, alRefresh);
        timer.start();

        //Panel initialization
        setVisible(true);
    }

    /**
     * Adds a player's JLabel to the specified JPanel position.
     * Informs the player if they won or overshot the last tile.
     * @param jlPlayer a player's JLabel
     * @param rows     the row position, from top to bottom
     * @param columns  the column position, from left to right
     */
    public void addToBoard(JLabel jlPlayer, int rows, int columns) {
        try {
            board[rows][columns].add(jlPlayer);
            if (rows == 0 && columns == 0) {
                JOptionPane.showMessageDialog(null, String.format("%s won!",jlPlayer.getText()) );
            }
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            JOptionPane.showMessageDialog(null, "You failed to land exactly on the last tile.");
        }
    }

    /**
     * Clears the board by calling each player's remove method.
     */
    public void clearBoard() {
        for(Player player: playerArrayList) {
            player.remove();
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
     * Returns the ArrayList of Players.
     * @return the ArrayList of Players
     */
    public ArrayList<Player> getPlayerArrayList() {
        return playerArrayList;
    }

    protected class Player extends JLabel {
        private final String name;

        /**
         * Constructs a Player with a name sets the JLabel text.
         * @param _name    the player's name
         */
        public Player(String _name) {
            setText(_name);
            name = _name;
            playerArrayList.add(this);
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
        public void remove() {
            try {
                /*
                 * Get the coordinates from returnIntPos() then use them to access
                 * that position in the board[][] array and call the remove() method.
                 */
                int[] coords = returnIntPos();
                //issue: if the player isn't found the coords will be [0][0]?
                board[coords[0]][coords[1]].remove(this);
            } catch (NullPointerException npe) {
                npe.printStackTrace();
            }
        }

        /**
         * Moves the player on the board according to the Snakes and Ladders
         * movement pattern.
         * @param positions number of tiles to move
         */
        public void move(int positions) {
            //Grab the current coordinates
            int[] coords = returnIntPos();
            int rows = coords[0];
            int columns = coords[1];
//            System.out.printf("Starting coordinates: R%d C%d\n", rows, columns);
            int rowsTo = rows;
            int columnsTo;
            int moveBank = positions;
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
            addToBoard(this, rowsTo, columnsTo);
        }

        /**
         * Moves a single tile x number of times.
         * Calls the move() method.
         * @param positions the number of tiles to move
         */
        public void moveOneByOne(int positions) {
            ActionListener alMv1 = new ActionListener() {
                int counter = positions;

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (counter != 0) {
                        move(1);
                        counter--;
                    }
                }
            };
            Timer timer = new Timer(500, alMv1);
            timer.start();
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
}


