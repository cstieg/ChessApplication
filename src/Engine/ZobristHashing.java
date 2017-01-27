
package com.circumspectus.Engine;

import com.circumspectus.ChessApplication.Chessboard;
import com.circumspectus.ChessApplication.Square;
import java.util.Random;

/**
 * A Zobrist hash generator which creates a random long hash for each element of the chessboard
 * (each type and color of piece for each square, as well as board state and turn to move).
 * @author Christopher Stieg
 */
public class ZobristHashing {
    private final long hashTable[][];
    private final long turnToMoveHash;
    private final long boardStateHash[];
    
    /**
     * Constructor for ZobristHashing which initializes each of the various elements with a random hash value.
     */
    public ZobristHashing() {
        hashTable = new long [12][64];
        boardStateHash = new long [2048];
        Random random = new Random(42);
        
        for (int i = 0; i < 12; i++) {
            for (int j = 0; j < 64; j++) {
                hashTable[i][j] = random.nextLong();
            }
        }
        for (int i = 0; i < 2048; i++) {
            boardStateHash[i] = random.nextLong();
        }
        turnToMoveHash = random.nextLong();
    }
    
    /**
     * Gets the hash value for a particular piece type and color on a particular square
     * @param pieceTypeColor    The sum of the type and color addends of the piece to hash
     * @param squareSerial      The serial int of the square to hash
     * @return The hash value for the given piece type and color on the given square
     */
    public long getPieceSquareHash(int pieceTypeColor, int squareSerial) {
        return hashTable[pieceTypeColor][squareSerial];
    }
    
    /**
     * Gets the hash value to distinguish the turn to move.
     * It is XORed when it is black's turn to move
     * @return  The long hash value to distinguished the turn to move
     */
    public long getTurnToMoveHash() {
        return turnToMoveHash;
    }
    
    /**
     * Gets the hash value for a particular board state
     * @param boardState    The boardState int (see Chessboard for enumeration)
     * @return  The hash value for the given board state
     */
    public long getBoardStateHash(int boardState) {
        return boardStateHash[boardState];
    }
    
    /**
     * Gets the hash value for the entire board
     * @param board The board to be hashed
     * @return  The hash value for the board
     */
    public long getBoardHash(Chessboard board) {
        long boardHash = 0;
        for (int i = 0; i < 64; i++) {
            int pieceTypeColor = board.getPieceTypeColorAt(Square.toBitwise(i));
            if (pieceTypeColor != Chessboard.NO_PIECE) {
                if (pieceTypeColor == -2) {
                    int x = board.getPieceTypeColorAt(Square.toBitwise(i));
                }
                boardHash ^= hashTable[pieceTypeColor][i];
            }
        }
        if (board.getCurrentPlayer() == 1) {
            boardHash ^= turnToMoveHash;
        }
        boardHash ^= boardStateHash[board.getBoardState()];
        return boardHash;
    }
}