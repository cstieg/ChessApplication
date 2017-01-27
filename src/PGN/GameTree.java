
package com.circumspectus.PGN;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

/**
 * Stores a database of PGN games in a tree on disk, to be utilized in ChessEngine opening
 * @author Christopher Stieg
 */
public class GameTree {
    Node root;
    private Node currentNode;
    private int currentDepth;
    private int result;
    private int serial;
    private final static int WIN = 1;
    private final static int LOSS = -1;
    private final static int DRAW = 0;
    private final static int OTHER = -2;
    private final static int EMPTY = -4;
    private Writer writer;
    private final int INITIAL_PLY_SEARCH_DEPTH = 4;
    private final int MAX_DEPTH = 40;
    private final String basePath = new File("").getAbsolutePath() + "\\resources\\games";    
    private GameTree openingTree;  // tree to index files which are in various folders
    
    /**
     * Constructor for GameTree.
     */
    public GameTree() {
        root = new Node();
        currentNode = root;
        serial = 0;
    }
    
    /**
     * Gets the 
     * @return 
     */
    public GameTree getOpeningTree() {
        return openingTree;
    }
    
    /**
     * Nested class representing a single node of a tree, called recursively.
     */
    private class Node {
        ArrayList<Node> children;
        String movetext;
        int wins;
        int losses;
        int draws;
        int serial;
        
        /**
         * Constructor for Node nested class
         */
        public Node() {
            children = new ArrayList<>();
            movetext = "";
            wins = 0;
            losses = 0;
            draws = 0;
        }     
       
        /**
         * Provides a string representation of the number of wins, losses, and draws
         * @return  A string representation of the number of wins, losses, and draws
         */
        @Override
        public String toString() {
            return movetext + " [" + wins + "," + losses + "," + draws + "]";
        }

        /**
         * Gets a list of the serial numbers of all the children of the node, to be used in writing the tree to file
         * @return  A list of the serial numbers of all the children, separated and ended by a comma.
         */
        private String childrensSerials() {
            String serials = "";
            for (Node child : children) {
                serials = serials + child.serial + ",";
            }
            return serials;
        }
    }
    
    /**
     * Adds a game to the tree.  Does not add the game if the game is empty or a nonstandard result (unfinished, etc.).  Calls the addNewMove method to add each individual move.
     * @param game  A PGNGame object representing a game
     */
    public void addNewGame(PGNGame game) {
        currentNode = root;
        this.result = intResult(game.getResult());
        if (result == EMPTY) {
            System.out.println("No game");
            return;
        }
        if (result == OTHER) {
            System.out.println("Nonstandard game result!");
            return;
        }
        currentDepth = 0;
        for (String movetext : game.getMoves()) {
            addNewMove(movetext);    
        }
    }
    
    /**
     * Adds a move to the tree.  Called by addNewGame() in a loop.  Updates CurrentNode after adding child to the tree
     * @param movetext  A string representation of the PGN movetext to add.
     */
    public void addNewMove(String movetext) {
        currentDepth++;
        if (currentDepth > MAX_DEPTH) {  // only add the opening
            return;
        }
        
        if (movetext.equals("")) {
            System.out.println("Caution: Blank movetext!");
            return;
        }
        Node nodeToAdd = null;
        
        for (Node child : currentNode.children) {
            // search to see if this move has been added in another game
            if (movetext.equals(child.movetext)) {
                nodeToAdd = child;
                break;
            }
        }
        
        // if not, then add
        if (nodeToAdd == null) {
            nodeToAdd = new Node();
            nodeToAdd.movetext = movetext;
            currentNode.children.add(nodeToAdd);
        }
        
        // add one to the appropriate result variable stored in the node
        if (result == WIN) {
            nodeToAdd.wins++;
        }
        if (result == LOSS) {
            nodeToAdd.losses++;
        }
        if (result == DRAW) {
            nodeToAdd.draws++;
        }

        currentNode = nodeToAdd;
    }
    
    /**
     * Serializes the tree after the tree is finished.
     */
    private void writeSerials() {
        serial = 0;
        root.serial = serial++;
        writeNextSerial(root);
    }
    
    /**
     * A recursive helper method to help writeSerials() serialize the tree
     * @param node  The current node in the recursive process
     */
    private void writeNextSerial(Node node) {
        node.serial = this.serial++;
        for (Node nextNode : node.children) {
            writeNextSerial(nextNode);
        }          
    }
    
    /**
     * Saves a finished tree to disk.  Each node is represented by the serial in the format ####:, followed by the movetext, followed by the game results in the format
     *      [wins, losses, draws], followed by the children's serial numbers in a list contained by braces - {serial1, serial2, serial3,}, followed by a newline character.
     * @param pathName  the path of the file to which the tree will be written
     * @param fileName  the name of the file to which the tree will be written
     */
    public void saveToDisk(String pathName, String fileName) {
        writeSerials(); // first, serialize the tree
        try {
            writer = new FileWriter(fileName);
        }
        catch (IOException ioe) {
            System.err.println("Error in creating file");
        }
        saveNextNode(root);

        try {
            writer.close();
        }
        catch (IOException ioe) {
            System.err.println("Can't close writer");
        }
    }
    
    /**
     * A recursive helper method to help saveToDisk()
     * @param node  The current node in the recursive process
     */
    private void saveNextNode(Node node) {        
        String nodeLine = node.serial + ": " + node.toString() + " {" + node.childrensSerials() + "}\n";
        try {
            writer.write(nodeLine);
        }
        catch (IOException ioe) {
            System.err.println("Can't write to writer");
        }

        for (Node nextNode : node.children) {
            saveNextNode(nextNode);
        }  
    }

    /**
     * Converts a string representation of a game result to an int
     * @param result    String representation of game result, most commonly "1-0", "0-1" or "1/2-1/2"
     * @return          EMPTY if null, WIN, LOSS, DRAW, or OTHER
     */
    private int intResult(String result) {
        if (result == null || result.isEmpty()) {
            return EMPTY;
        }
        result = result.trim();
        if (result.equals("1-0")) {
            return WIN;
        } 
        else if (result.equals("0-1")) {
            return LOSS;
        }
        else if (result.equals("1/2-1/2")) {
            return DRAW;
        }
        else {
            return OTHER;
        }
    }
    
    /**
     * Adds the initial moves of a tree to an opening index tree, which is saved after all the trees have been completed.
     * @param depth     Ply depth to store in the opening tree, which corresponds to the folder depth in the game trees
     */
    public void addToOpening(int depth) {
        Node currentNode = this.root;
        Node openingTreeCurrentNode = openingTree.root;
        for (int i = 0; i < depth; i++) {
            // get next node in original GameTree
            if (currentNode.children.isEmpty()) {
                System.out.println("Debug");
            }
            currentNode = currentNode.children.get(0);

            // get next node in OpeningTree, add if not found
            boolean found = false;
            for (Node node : openingTreeCurrentNode.children) {
                if (node.movetext.equals(currentNode.movetext)) {
                    openingTreeCurrentNode = node;
                    found = true;
                    break;
                }
            }
            
            // add wins, losses, draws from original GameTree
            if (!found) {
                Node newNode = new Node();
                newNode.wins = currentNode.wins;
                newNode.losses = currentNode.losses;
                newNode.draws = currentNode.draws;
                newNode.movetext = currentNode.movetext;
                openingTreeCurrentNode.children.add(newNode);
                openingTreeCurrentNode = newNode;
            }
            else {
                openingTreeCurrentNode.wins = openingTreeCurrentNode.wins + currentNode.wins;
                openingTreeCurrentNode.losses = openingTreeCurrentNode.losses + currentNode.losses;
                openingTreeCurrentNode.draws = openingTreeCurrentNode.draws + currentNode.draws;
            }
        }
    }
    
    /**
     * Adds games which are similar up to a specified depth to the tree.  Calls saveToDisk() to write the tree to disk, and then looks for the next game pattern unique 
     * up to the specified depth.  When all game patterns contained in the file are written, returns true.
     * @param reader    The PGNReader which feeds PGNGames from a PGN file containing the same opening moves as sorted by PGNSort
     * @return  True if all the game patterns in the file have been processed, False if not.
     */
    public boolean makeNextTree(PGNReader reader) {
        // create the opening index tree if it does not exist
        if (openingTree == null) {
            openingTree = new GameTree();
        }
        root = new Node();  // start with new tree
        currentNode = root;
        boolean finished = true;
        String currentTreeMoveSequence = "";  // the move sequence currently being processed
        int searchPlyDepth = INITIAL_PLY_SEARCH_DEPTH;
       
        String path = basePath;
        String filePath = "";
        boolean isNewTree = true;
        String gameText = reader.getNextGame();
        PGNGame game;
        
        while (!gameText.isEmpty()) {  // when gametext is empty, it is because the end of the gameReader has been reached
            try {  // catch memory errors
                game = new PGNGame(gameText);
                
                // don't want short games, would cause out of bounds exception
                if (game.getMoves().size() <= searchPlyDepth) {
                    gameText = reader.getNextGame();
                    continue;
                }

                // reset search critera
                if (isNewTree) {
                    currentTreeMoveSequence = "";
                }
                String currentGameMoveSequence = "";
                boolean treeAlreadyProcessed = false;
                
                // iterate through game by ply, to check to see if the game matches the sequence currently being processed
                for (int i = 0; i < searchPlyDepth; i++) {
                    currentGameMoveSequence = currentGameMoveSequence + game.getMoves().get(i) + " ";  
                    
                    // on new tree, set the search criteria
                    if (isNewTree) {
                        currentTreeMoveSequence = currentGameMoveSequence;
                        path = path + "\\" + game.getMoves().get(i);

                        // at end of searchPlyDepth, check to see if tree file is already in that folder.  If so, move on.
                        if (i == searchPlyDepth - 1) {
                            filePath = path + "\\" + game.getMoves().get(searchPlyDepth - 1) + ".tree";
                            if (new File(filePath).exists()) {
                                treeAlreadyProcessed = true;
                                continue;
                            }    
                            // if there is no file in the folder (which encompasses all of the folder's range), but there is a folder, then should go to the next level
                            // extend ply depth if already determined the shallower, broader range is not possible
                            if (new File(path).exists()) {
                                searchPlyDepth++;
                            }
                        }
                        new File(path).mkdir();   
                    }
                }
                
                if (treeAlreadyProcessed) { // don't process a sequence alread processed, continue to next game
                    isNewTree = true;
                    searchPlyDepth = INITIAL_PLY_SEARCH_DEPTH;
                    path = basePath;
                    gameText = reader.getNextGame();
                    continue;
                }
                
                // add game if matches current sequence
                if (currentTreeMoveSequence.equals(currentGameMoveSequence)) {
                    addNewGame(game);
                    finished = false;  // only finished when this method iterates through entire file and finds no new sequences not yet sequenced
                }
                
                // get next game for next iteration
                isNewTree = false;              
                gameText = reader.getNextGame();
                
                // save tree when finished with file
                if (gameText.isEmpty()) { // finished with while 
                    addToOpening(searchPlyDepth);
                    saveToDisk(path, filePath);                                        
                }
            }
            catch (OutOfMemoryError oome) {
                // increase ply depth to decrease the number of results stored in a single tree at one time
                root = null;  // release memory
                isNewTree = true;  // need to reprocess game again one level deeper
                searchPlyDepth++;
                root = new Node();
                path = basePath;
                reader.reset();
                gameText = reader.getNextGame();
                continue;
            }
        }

        root = null; // release tree from memory
        return finished;
    }
    
    /**
     * Use GameTree to process the sorted PGN games and output them in a tree of folders as GameTrees saved to disk by the writeToDisk() method.
     * @param args 
     */
    public static void main(String[] args) {
        PGNReaderFeeder readerFeeder = new PGNReaderFeeder("C:\\Users\\user\\Java\\ChessApplication\\resources\\sortedgames");
        GameTree tree = new GameTree();
        boolean finished;
        
        while (readerFeeder.hasNext())
        {
            PGNReader reader = readerFeeder.getNext();
            finished = tree.makeNextTree(reader);
            while (!finished) {
                finished = tree.makeNextTree(reader);
            }
        }
        System.out.println("Finished!)");   
        String pathName = "C:\\Users\\user\\Java\\ChessApplication\\resources\\games";
        String fileName = pathName + "\\opening.tree";
        tree.getOpeningTree().saveToDisk(pathName, fileName);
    }
}
