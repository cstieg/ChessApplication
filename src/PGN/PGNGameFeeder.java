
package com.circumspectus.PGN;

/**
 * A game feeder to read PGN game texts from a text file database chess games
 * @author Christopher Stieg
 */
public class PGNGameFeeder {
    private PGNReader reader;
    private PGNReaderFeeder readerFeeder;
    private final String dir;
    
    /**
     * Constructor for PGNGameFeeder
     * @param folderName    Name of folder in which to find PGN games
     */
    public PGNGameFeeder(String folderName) {
        dir = folderName;
        readerFeeder = new PGNReaderFeeder(dir);
        reader = readerFeeder.getNext();
    }
    
    /**
     * Gets the next PGN game in the file
     * @return  The next PGNGame in the file 
     */
    public PGNGame getNext() {
        return new PGNGame(reader.getNextGame());
    }        
            
    /**
     * Gets the next PGN game in the folder
     * @return  The next PGNGame in the folder
     */
    public PGNGame getNextInFolder() {
        String nextGameText = reader.getNextGame();
        if (nextGameText.isEmpty()) {
            if (readerFeeder != null && readerFeeder.hasNext()) {
                reader = readerFeeder.getNext();
                nextGameText = reader.getNextGame();
            }
            else {
                return null;
            }
        }
        PGNGame newGame = new PGNGame(nextGameText);
        return newGame;
    }
    
    /**
     * Begins at the beginning of the file again.
     */
    public void reset()  {
        readerFeeder = new PGNReaderFeeder(dir);
        reader = readerFeeder.getNext();           
    }
    
    public static void main(String args[]) {
        PGNGameFeeder gameFeeder = new PGNGameFeeder("C:\\Users\\user\\Java\\Chess\\pgn_lite");
        PGNGame game = gameFeeder.getNext();
        while (game != null) {
            System.out.println(game.getResult() + ": " + game.moves.get(0) + " " + game.moves.get(1) + " " + game.moves.get(2));
            game = gameFeeder.getNext();
        }
    }
}