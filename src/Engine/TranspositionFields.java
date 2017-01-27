
package com.circumspectus.Engine;

import com.circumspectus.ChessApplication.Move;

/**
 * A tuple to store transposition table data as the value of a hashmap
 * @author Christopher Stieg
 */
public class TranspositionFields {
    public int score;
    public int depth;
    public Move bestMove;
    public int serial;
    public enum TypeOfScore {EXACT, FAIL_HIGH, FAIL_LOW};
    public TypeOfScore typeOfScore;
    
    /**
     * Constructor for TranspositionFields
     * @param score     The score for a board position returned by search
     * @param depth     The depth to which the position was searched
     * @param bestMove  The best move found for this board position
     * @param serial    Serial entry number in transposition table
     * @param typeOfScore   Whether the score is exact value, fail high, or fail low
     */
    public TranspositionFields(int score, int depth, Move bestMove, int serial, TypeOfScore typeOfScore) {
        this.score = score;
        this.depth = depth;
        this.bestMove = bestMove;
        this.serial = serial;
        this.typeOfScore = typeOfScore;
    }
}