
package com.circumspectus.Engine;

import com.circumspectus.ChessApplication.Chessboard;
import com.circumspectus.ChessApplication.Move;
import com.circumspectus.ChessApplication.Square;

/**
 * Extends the Chessboard to have a Zobrist hash value field, and to keep
 * the hash value updated by XORing new moves
 * @author Christopher Stieg
 */
public class ZobristBoard extends Chessboard {
    long hashValue;
    ZobristHashing hasher;
    
    /**
     * Constructor for ZobristBoard
     * @param hasher    Hash value generator object
     */
    public ZobristBoard(ZobristHashing hasher) {
        super();
        this.hasher = hasher;
        hashValue = hasher.getBoardHash(new Chessboard());  // any chessboard in the initial state will do for initializing a new board
    }
    
    /**
     * Constructor for ZobristBoard which calculates the hash value of a given board
     * @param board     The board whose hash value to calculate
     * @param hasher    Hash value generator object
     */
    public ZobristBoard(Chessboard board, ZobristHashing hasher) {
        this.hasher = hasher;
        this.piecesByType = board.getPiecesByTypeClone();
        this.piecesByColor = board.getPiecesByColorClone();
        this.boardState = board.getBoardState();
        this.ply = board.getPly();
        this.hashValue = hasher.getBoardHash(board);
    }
    
    /**
     * Adds a piece (or pieces) at a given position (in both type array and color array).  Does not check whether multiple bits are true within the position added.
     * @param position          Binary bitboard with a 1(s) at the position of the piece(s) to added
     * @param pieceTypeColor    Type and color of the piece to add (see enumeration)
     */
    @Override
    public void addPiece(long position, int pieceTypeColor) {
        super.addPiece(position, pieceTypeColor);
        hashValue ^= hasher.getPieceSquareHash(pieceTypeColor, Square.toSerial(position));
    }
    

    /**
     * Removes a piece (or pieces) at a given position (in both type array and color array)
     * @param position          Binary bitboard with a 1(s) at the position of piece(s) to be removed
     * @param pieceTypeColor    Type and color of the piece to remove (see enumeration)
     */
    @Override
    public void removePiece(long position, int pieceTypeColor) {
        super.removePiece(position, pieceTypeColor);
        hashValue ^= hasher.getPieceSquareHash(pieceTypeColor, Square.toSerial(position));
    }
    
    /**
     * Returns a clone of the current chessboard, complete with a cloned set of dependent objects
     * @return A clone of the current chessboard
     **/
    @Override
    public ZobristBoard getClone() {
        ZobristBoard clonedBoard = new ZobristBoard(hasher);
        clonedBoard.piecesByType = this.piecesByType.clone();
        clonedBoard.piecesByColor = this.piecesByColor.clone();
        clonedBoard.boardState = this.boardState;
        clonedBoard.ply = this.ply;
        clonedBoard.hashValue = this.hashValue;
        return clonedBoard;
    }
    
    /**
     * Sets the ply count
     * @param ply   ply (number of moves of each player) count beginning with 1.  
     */
    @Override
    protected void setPly(int ply)  {
        int previousPly = this.ply;
        super.setPly(ply);
        if ((previousPly - ply) % 2 != 0) {
            hashValue ^= hasher.getTurnToMoveHash();
        }
    }
    
    /**
     * Sets whether the castle pieces (king or rook) have moved
     * @param pieceTypeColor    Sum of the enumerations for the type and color of the piece
     * @param side              The side of the rook - 0 for king's rook, 1 for queen's rook.  If piece is king, can be 0.
     * @param haveMoved         The status of whether the pieces have moved (true when moving, false when undoing the move)
     */
    @Override
    public void setCastlePiecesMoved(int pieceTypeColor, int side, boolean haveMoved) {
        int oldBoardState = super.boardState;
        super.setCastlePiecesMoved(pieceTypeColor, side, haveMoved);
        hashValue ^= hasher.getBoardStateHash(oldBoardState);  // remove old board state hash value
        hashValue ^= hasher.getBoardStateHash(super.boardState); // add in new board state hash value
    }
    
    /**
     * Moves a piece from one square to another, adds the move to the move array, and increments the ply count.
     * @param move  Move object specifying the move to be made
     */
    @Override
    public void move(Move move) {
        super.move(move);
        hashValue ^= hasher.getTurnToMoveHash();
        move.setBoardHashValue(hashValue);   
    }
    
    /**
     * Undoes last move made
     */
    @Override
    public void undoLastMove() {
        super.undoLastMove();
        hashValue ^= hasher.getTurnToMoveHash();
    }
    
    /**
     * Checks whether this board position has been repeated before a specified number of times
     * @param repetitionLimit   The target number of repetitions to check
     * @return  True if the board position has been repeated the specified number of times or more; false if not
     */
    public boolean isRepetition(int repetitionLimit) {
        int repetitionNumber = 0;
        for (Move move : moves) {
            if (move.getBoardHashValue() == hashValue) {
                repetitionNumber++;
                if (repetitionNumber >= repetitionLimit) {
                    return true;
                }
            }
        }
        return false;
    }
}