
package com.circumspectus.PGN;

import com.circumspectus.ChessApplication.*;
import java.util.ArrayList;

/**
 * Represents a chess game with PGN movetexts
 * @author Christopher Stieg
 */
public class PGNGame {
    private String gameText;
    private int i;
    private int ply;
    private ArrayList<String> tags;
    ArrayList<String> moves;
    private String result;

    /**
     * Constructor for PGNGame taking as input a game text such as "1. e4 e5 2. Nf3 Nf6 ..... etc." into an array of moves indexed by ply
     * @param PGNtext   Game text including move numbers
     */
    public PGNGame(String PGNtext) {
        this.gameText = PGNtext;
        tags = new ArrayList<>();
        moves = new ArrayList<>();
        i = 0;
        ply = 0;
        parseTokens();
    }

    /**
     * Constructor for PGNGame taking as input an ArrayList of Moves
     * @param moves Move object (containing serial ints for origin, destination, etc.)
     */
    public PGNGame(ArrayList<Move> moves) {
        this.moves = new ArrayList<>();
        for (Move move : moves) {
            this.moves.add(move.getMovetext());
        }
        createGameText();
        ply = 0;
    }

    /**
     * Gets the moves ArrayList
     * @return  The moves ArrayList
     */
    public ArrayList<String> getMoves() {
        return moves;
    }

    /**
     * Reads the next sequential move from the move list
     * @return  The next sequential move from the move list
     */
    public String getNextMove() {
        if (ply < moves.size()) {
            ply++;
            return moves.get(ply - 1);
        }
        return "";
    }

    /**
     * Gets the result of the game (win, lose, draw)
     * @return  A string representation of the result of the game ("1-0" for white win, "0-1" for black win, "1/2-1/2" for draw)
     */
    public String getResult() {
        return result;
    }

    /**
     * Returns the original game text that was passed into the constructor
     * or created by the createGameText method
     * @return  The game text in PGN format
     */
    public String getText() {
        return gameText;
    }

    /**
     * Creates a PGN game text from the list of moves.
     */
    void createGameText() {
        int ply = 1;
        for (String move : moves) {
            if (ply % 2 == 1) {
                gameText = gameText + Integer.toString(ply / 2) + ".";
            }
            gameText = gameText + move + " ";
        }
        gameText = gameText + "*";
    }

    /**
     * Parses tokens and combines them into moves or results.
     */
    private void parseTokens() {
        String token = getNextToken();
        while (!token.equals("")) {
            if (token.equals("[")) {
                String tag = "[";
                while (!token.equals("") && !token.equals("]")) {
                    token = getNextToken();
                    tag = tag + token;
                }
                tags.add(tag);
            } else if (inRange("0", "9", token.substring(0, 1))) {
                if (token.contains("-")) {
                    result = token;
                    return;
                } else {
                    token = getNextToken();
                    if (!token.equals(".")) {
                        return;
                    }
                }
            } else {
                moves.add(token.trim());
            }
            token = getNextToken();
        }
    }

    /**
     * Identifies, gets and removes tokens from the game text.
     * @return  The next token from the game text
     */
    private String getNextToken() {
        while (gameText.length() > i) {
            // find quotes and return enclosed
            if (gameText.substring(i, i + 1).equals("\"")) {
                int j = i + 1;
                while (gameText.length() > j && !gameText.substring(j, j + 1).equals("\"")) {
                    j++;
                    if (j - i > 257) {
                        throw new RuntimeException("Quotes limited to 255 characters!  Probably data corruption");
                    }
                }
                int oldi = i;
                i = j + 1;
                return gameText.substring(oldi, j + 1);
            }

            // return integer token
            if (inRange("0", "9", gameText.substring(i, i + 1))) {
                int j = i + 1;
                while (gameText.length() > j && (inRange("0", "9", gameText.substring(j, j + 1))
                        || gameText.substring(j, j + 1).equals("-")
                        || gameText.substring(j, j + 1).equals("/"))) {
                    j++;
                }
                int oldi = i;
                i = j;
                return gameText.substring(oldi, j);
            }

            // return single character tokens
            if (".*[]()<>".contains(gameText.substring(i, i + 1))) {
                int oldi = i;
                i++;
                return gameText.substring(oldi, oldi + 1);
            }

            // return symbol (text) token
            if ("abcdefghijklmnopqrstuvwxyz".contains(gameText.substring(i, i + 1).toLowerCase())) {
                int j = i + 1;
                while (gameText.length() > j && "abcdefghijklmnopqrstuvwxyz0123456789_+#=:-".contains(gameText.substring(j, j + 1).toLowerCase())) {
                    j++;
                }
                int oldi = i;
                i = j;
                return gameText.substring(oldi, j);

            }

            i++;
        }

        return "";
    }

    /**
     * Checks whether a string is within a given range
     * @param lowerBound    the lower bound of the range
     * @param upperBound    the upper bound of the range
     * @param input         the string to check
     * @return              True if input string is within the range; false if it is not
     */
    private boolean inRange(String lowerBound, String upperBound, String input) {
        // (First, be sure to check for null values)
        return input.compareToIgnoreCase(lowerBound) >= 0 && input.compareToIgnoreCase(upperBound) <= 0;
    }

}