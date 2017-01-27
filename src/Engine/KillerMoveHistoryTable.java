
package com.circumspectus.Engine;

import com.circumspectus.ChessApplication.Move;

/**
 * A table to store killer moves history (moves that cause cutoffs in alpha beta pruning).  
 * It is assumed that moves that cause a cutoff because they are too poor for one of the players
 * will be more likely to cause a cutoff again, and thus moves that cause cutoffs more frequently
 * should be ordered toward the beginning of the possible move list.  
 * The table consists of a multi-dimensional array with dimensions for origin and destination,
 * and increments the count for that entry whenever a move with the given origin and destination is added.
 * This table also indexes the moves by ply, and sequentially deletes ply which the game has moved past.
 * @author Christopher Stieg
 */
public class KillerMoveHistoryTable {
    private final int historyMoves[][][];
    private int currentPly;

    /**
     * Constructor for KillerMoveHistoryTable
     */
    public KillerMoveHistoryTable() {
        historyMoves = new int[ChessEngine.DEPTH + 2][64][64];  // first 2 elements of the array will be previous 2 ply
        currentPly = 1;
    }

    /**
     * Adds a killer move to the table count
     * @param move  The Move which has caused a cutoff in search
     */
    public void addMove(Move move) {
        historyMoves[move.getPly() - currentPly + 2 - 1][move.getOriginSerial()][move.getDestinationSerial()]++;
    }

    /**
     * Increments the current ply and shifts the arrays to the next ply accordingly.
     */
    public void incrementPly() {
        currentPly++;
        for (int i = 0; i < ChessEngine.DEPTH; i++) {
            historyMoves[i] = historyMoves[i + 1];
            historyMoves[i + 1] = new int[64][64];
        }
    }

    /**
     * Get a relative score corresponding to the number of cutoffs this move has caused at the same ply,
     * and to a lesser extent, the number of cutoffs this move has caused in previous and subsequent moves of the same player
     * @param move  The move whose score to return
     * @return  Score indicating the relative frequency the move has caused cutoffs;
     * the higher the score, the more frequent this move has caused cutoffs.
     */
    public int getResults(Move move) {
        int lastMove = move.getPly() - currentPly;
        int currentMove = move.getPly() - currentPly + 2;
        int nextMove = move.getPly() - currentPly + 4;
        int results = 0;
        if (currentMove - 1 < ChessEngine.DEPTH + 2 && currentMove > 0) {
            results = historyMoves[currentMove - 1][move.getOriginSerial()][move.getDestinationSerial()] * 4;
        }
        if (lastMove - 1 < ChessEngine.DEPTH + 2 && lastMove > 0) {
            results += historyMoves[lastMove - 1][move.getOriginSerial()][move.getDestinationSerial()];
        }
        if (nextMove - 1 < ChessEngine.DEPTH + 2 && nextMove > 0    ) {
            results += historyMoves[nextMove - 1][move.getOriginSerial()][move.getDestinationSerial()];
        }
        return results;
    }
    
    /**
     * Gets the current ply of the table.
     * @return  The current ply of the table
     */
    public int getCurrentPly() {
        return currentPly;
    }
}
