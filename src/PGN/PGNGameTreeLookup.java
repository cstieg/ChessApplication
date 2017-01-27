
package com.circumspectus.PGN;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Looks up a PGNGame in a tree saved to disk by GameTree
 * @author Christopher Stieg
 */
public class PGNGameTreeLookup {
    private final String gameFolder;
    private boolean gameFound = false;
    private String foundNode;
    private File file;
    private BufferedReader reader;
    
    /**
     * Convenience constructor for PGNGameTreeLookup, using default folder setting.
     */
    public PGNGameTreeLookup() {
        gameFolder = new File("").getAbsolutePath() + "\\resources\\games";  
    }
    
    /**
     * Constructor for PGNGameTreeLookup
     * @param gameFolder    The root folder in which the tree files are saved
     */
    public PGNGameTreeLookup(String gameFolder) {
        this.gameFolder = gameFolder;
    }
        
    /**
     * Finds a partial game represented by a PGNGame object on the tree saved to disk
     * @param game  The game to find
     * @return  True if the game is found; false if it is not
     */
    public boolean lookup(PGNGame game) {
        if (game.getMoves().size() >= 4) { // if under 4 ply, then no standalone gametree will exist - can check opening.tree
            // check to see if standalone gametree file for this game exists
            String searchFolder = gameFolder;
            String filePath = "";
            boolean foundFile = false;
            for (int i = 0; i < 4; i++) {
                searchFolder = searchFolder + "\\" + game.getMoves().get(i);
            }
            int i = 3;
            while (i < game.getMoves().size() && new File(searchFolder).exists()) {
                filePath = searchFolder + "\\" + game.getMoves().get(i) + ".tree";
                if (new File(filePath).exists()) {
                    foundFile = true;
                    break;
                }
                i++;
                searchFolder = searchFolder + "\\" + game.getMoves().get(i);
            }
            
            // try to find place in gametree file
            if (foundFile) {
                try {
                    this.file = new File(filePath);
                    reader = new BufferedReader(new FileReader(file));
                }
                catch (IOException ioe) {
                    System.err.println("Can't open file!");
                    return false;
                }
                foundNode = findPlaceInTree(reader, game);
                gameFound =  !foundNode.isEmpty();
                return gameFound;
            }
        }
        // if in the first 4 (or so moves), then should check opening.tree
        try {
            String filePath = gameFolder + "\\opening.tree";
            File file = new File(filePath);
            reader = new BufferedReader(new FileReader(file));
        }
        catch (IOException ioe) {
            System.err.println("Can't open file!");
            return false;
        }
        foundNode = findPlaceInTree(reader, game);
        gameFound = !foundNode.isEmpty();
        return gameFound;          
    }
    
    /**
     * Checks whether the given game is found in the game tree
     * @return  True if the game is found, false if it is not
     */
    public boolean isFound() {
        return gameFound;
    }
    
    /**
     * Gets the win-lose-draw record for each of the possible moves following a sequence of moves
     * @return  An ArrayList of MoveResults containing the win-lose-draw record for all the possible next moves
     */
    public ArrayList<MoveResults> getChildrensResults() {
        ArrayList<MoveResults> childrensResults = new ArrayList<>();
        if (!gameFound) {
            System.err.println("Can't find game!");
            return childrensResults;
        }
        for (String child : findChildren(foundNode))  {
            String childNodeText = findInReader(reader, child);
            if (childNodeText.isEmpty()) {
                reader = refreshedReader();
                childNodeText = findInReader(reader, child);
            }
            String movetext = findMovetextInLine(childNodeText);
            int wins = findRecord(childNodeText, 0);
            int losses = findRecord(childNodeText, 1);
            int draws = findRecord(childNodeText, 2);
            childrensResults.add(new MoveResults(movetext, wins, losses, draws));
        }
        return childrensResults;
    }
    
    /**
     * Gets a new PGN reader of the same file (returns current PGN documents to the beginning of file)
     * @return  New PGN reader positioned at the beginning of file
     */
    private BufferedReader refreshedReader() {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
        }
        catch (IOException ioe) {
            System.err.println("Can't open file!");
        }
        return reader;
    }

    /**
     * Reads the text of a game text file up to the progress of specified PGN game
     * @param reader    The reader containing the game text file
     * @param game      The game whose place to find
     * @return          A String game text of the game so far
     */
    private String findPlaceInTree(BufferedReader reader, PGNGame game) {
        String currentLine = findInReader(reader, "1:");
        if (currentLine.isEmpty()) {
            System.err.println("Invalid file");
            return "";
        }
        int ply = 1;
        boolean found = false;
        while (!currentLine.isEmpty() && ply <= game.getMoves().size()) {
            found = false;
            ArrayList<String> children = findChildren(currentLine);
            if (children == null) {
                return "";
            }
            for (String child : children) {
                currentLine = findInReader(reader, child + ":");
                String movetext = findMovetextInLine(currentLine);
                if (movetext.equals(game.getMoves().get(ply - 1))) {
                    ply++;
                    found = true;
                    break;
                }
            }            
        }
        if (found) {
            return currentLine;
        }
        return "";
    }
    
    /**
     * Finds a specified String in a reader
     * @param reader    The reader in which to search
     * @param searchText    The text to search for
     * @return  The line containing the searchText  
     */
    private static String findInReader(BufferedReader reader, String searchText) {
        String currentLine = "";
        while (currentLine != null && !currentLine.contains(searchText)) {
            try {
                currentLine = reader.readLine();
            }
            catch (IOException ioe) {
                System.err.println("Can't read line");
                return "";
            }
        }
        if (currentLine != null) {
            return currentLine;
        }
        else {
            return "";
        }
    }
    
    /**
     * Finds the children nodes of a game tree written to file
     * @param treeLine  The text of the node including the movetext, the win-loss-draw record, and the node numbers for the children moves in curly braces {};
     * @return  An ArrayList of the node numbres for the children moves
     */
    private ArrayList<String> findChildren(String treeLine) {
        ArrayList<String> children = new ArrayList<>();
        int firstBraceLocation = treeLine.indexOf("{");
        int lastBraceLocation = treeLine.indexOf("}");
        if (lastBraceLocation == firstBraceLocation + 1) {
            return null;
        }
        int currentLocation = firstBraceLocation + 1;
        int nextCommaLocation = treeLine.substring(currentLocation).indexOf(",");
        while (nextCommaLocation >= 0) {
            children.add(treeLine.substring(currentLocation, currentLocation + nextCommaLocation));
            currentLocation = currentLocation + nextCommaLocation + 1;  // skip comma
            nextCommaLocation = treeLine.substring(currentLocation).indexOf(",");
        }
        return children;
    }
    
    /**
     * Finds the move text in the game tree text file
     * @param currentLine   String of the current line (node) in the file
     * @return  PGN movetext of the node
     */
    private static String findMovetextInLine(String currentLine) {
        return currentLine.substring(currentLine.indexOf(":") + 2, currentLine.indexOf("[") - 1);
    }
    
    /**
     * Finds wins, losses or draws in a string node
     * @param currentLine   The string node in which to find the record
     * @param component     0 for wins, 1 for losses, 2 for draws
     * @return              The number of wins, losses, or draws
     */
    private static int findRecord(String currentLine, int component) {
        String record = currentLine.substring(currentLine.indexOf("[") + 1, currentLine.indexOf("]"));
        record = record + ",";
        for (int i = 0; i < component; i++) {
            record = record.substring(record.indexOf(",") + 1);
        }
        return Integer.parseInt(record.substring(0, record.indexOf(",")));
    }
}