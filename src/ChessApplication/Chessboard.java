package com.circumspectus.ChessApplication;

import java.util.ArrayList;

/**
 * Creates a chessboard.  Uses 64 bit bitboards to store pieces.
 * @author  Christopher Stieg
 * @version 1.0
 */
public class Chessboard {
    public static final boolean ASSERTION_CHECKS = false;
    
    protected ArrayList<Move> moves = new ArrayList<>();
    
    protected long[] piecesByType = new long[6]; // must halve the piece enumeration to access
    protected long[] piecesByColor = new long[2];
    protected int ply;  // half moves
    protected int boardState;
    /*  BoardState bitfield:
    Bits 1-3 (1,2,4's place bit) -  double pawn push file (0-7)
    Bit 4 (8's place bit) -         is double pawn push
    Bit 5 (16's place bit) -        white king has moved
    Bit 6 (32's place bit) -        black king has moved
    Bit 7 (64's place bit) -        white king's rook has moved
    Bit 8 (128's place bit) -       white queen's rook has moved
    Bit 9 (256's place bit) -       black king's rook has moved
    Bit 10 (1024's place bit) -     black queen's rook has moved
    Value ranges from 0 to 2047
    */
    

    
    // piece type and color can be added together to form a pieceTypeColor int
    // enumeration for piece types
    public static final int KING = 0;
    public static final int QUEEN = 2;
    public static final int ROOK = 4;
    public static final int BISHOP = 6;
    public static final int KNIGHT = 8;
    public static final int PAWN = 10;
    public static final int NUMBER_OF_TYPES = 6;
    
    // enumeration for colors
    public static final int WHITE = 0;
    public static final int BLACK = 1;  
    public static final int NUMBER_OF_COLORS = 2;
    
    public static final int NO_PIECE = -1;

    // usage: if ((CANT_CASTLE[side][color] & boardState) != 0) #can't castle
    private static final int CANT_CASTLE_KINGSIDE[] =   { 0b0001010000, 0b0100100000 };
    private static final int CANT_CASTLE_QUEENSIDE[] =  { 0b0010010000, 0b1000100000 };
    private static final int CANT_CASTLE[][] = new int[][] { CANT_CASTLE_KINGSIDE, CANT_CASTLE_QUEENSIDE };
    
    // usage: if ((CASTLESPACE[side][color] & board.getPieces) != 0) # can't castle  
    private static final long KINGSIDE_CASTLESPACE[] =  { 0x0000000000000060L, 0x6000000000000000L };
    private static final long QUEENSIDE_CASTLESPACE[] = { 0x000000000000000EL, 0x0E00000000000000L };
    private static final long CASTLESPACE[][] = new long[][] { KINGSIDE_CASTLESPACE, QUEENSIDE_CASTLESPACE };
    
    // usage: if ((CASTLESPACE_AND_PIECES[side][color] & board.getPieces) != 0) # can't castle  
    private static final long KINGSIDE_CASTLESPACE_AND_PIECES[] =  { 0x00000000000000F0L, 0xF000000000000000L };
    private static final long QUEENSIDE_CASTLESPACE_AND_PIECES[] = { 0x000000000000001FL, 0x1F00000000000000L };
    private static final long CASTLESPACE_AND_PIECES[][] = new long[][] { KINGSIDE_CASTLESPACE_AND_PIECES, QUEENSIDE_CASTLESPACE_AND_PIECES };
    
    public static final long ROW_MULTIPLIER[] = {   0x0000000000000001L, 0x0000000000000100L, 0x0000000001000000L, 0x0000000100000000L,
                                                    0x0000000100000000L, 0x0000010000000000L, 0x0001000000000000L, 0x0100000000000000L };
    public static final long FILES[] = {            0x0101010101010101L, 0x0202020202020202L, 0x0404040404040404L, 0x0808080808080808L,
                                                    0x1010101010101010L, 0x2020202020202020L, 0x4040404040404040L, 0x8080808080808080L };
    public static final long RANKS[]  = {           0x00000000000000FFL, 0x000000000000FF00L, 0x0000000000FF0000L, 0x00000000FF000000L,
                                                    0x000000FF00000000L, 0x0000FF0000000000L, 0x00FF000000000000L, 0xFF00000000000000L };
    public static final long CENTER16 =             0x00003C3C3C3C0000L;
    public static final long CENTER4  =             0x0000001818000000L;
    public static final long WHITE_SQUARES =        0xAAAAAAAAAAAAAAAAL;
    public static final long BLACK_SQUARES =        0x5555555555555555L;
    public static final long EDGE_SQUARES =         0xFF818181818181FFL;
    public static final long BACK_RANK_MULTIPLIER[] = { ROW_MULTIPLIER[0], ROW_MULTIPLIER[7] };
    public static final long PAWN_RANK_MULTIPLIER[] = { ROW_MULTIPLIER[1], ROW_MULTIPLIER[6] };
    public static final long PROMOTION_RANK[] = { RANKS[7], RANKS[0] };
    
    // indexed by rank
    public static final long RANKS_AHEAD_WHITE[] = {0xFFFFFFFFFFFFFF00L, 0xFFFFFFFFFFFF0000L, 0xFFFFFFFFFF000000L, 0xFFFFFFFF00000000L,
                                                    0xFFFFFF0000000000L, 0xFFFF000000000000L, 0xFF00000000000000L, 0x0000000000000000L };
    public static final long RANKS_AHEAD_BLACK[] = {0x0000000000000000L, 0x00000000000000FFL, 0x000000000000FFFFL, 0x0000000000FFFFFFL,
                                                    0x00000000FFFFFFFFL, 0x000000FFFFFFFFFFL, 0x0000FFFFFFFFFFFFL, 0x00FFFFFFFFFFFFFFL };
    public static final long RANKS_AHEAD[][] = { RANKS_AHEAD_WHITE, RANKS_AHEAD_BLACK };
    
        
    public static final int DISTANCE_FROM_CENTER [] = { 7, 6, 5, 4, 4, 5, 6, 7, 
                                                        6, 5, 3, 2, 2, 3, 5, 6,
                                                        5, 3, 2, 1, 1, 2, 3, 5,
                                                        4, 2, 1, 0, 0, 1, 2, 4,
                                                        4, 2, 1, 0, 0, 1, 2, 4,
                                                        5, 3, 2, 1, 1, 2, 3, 5,
                                                        6, 5, 3, 2, 2, 3, 5, 6,
                                                        7, 6, 5, 4, 4, 5, 6, 7 };
    
    public static final long ADJACENT_SQUARE_BLOCK[] = {    0x0000000000000303L, 0x0000000000000707L, 0x0000000000000E0EL, 0x0000000000001C1CL, 
                                                            0x0000000000003838L, 0x0000000000007070L, 0x000000000000E0E0L, 0x000000000000C0C0L,
                                                            0x0000000000030303L, 0x0000000000070707L, 0x00000000000E0E0EL, 0x00000000001C1C1CL,
                                                            0x0000000000383838L, 0x0000000000707070L, 0x0000000000E0E0E0L, 0x0000000000C0C0C0L,
                                                            0x0000000003030300L, 0x0000000007070700L, 0x000000000E0E0E00L, 0x000000001C1C1C00L,
                                                            0x0000000038383800L, 0x0000000070707000L, 0x00000000E0E0E000L, 0x00000000C0C0C000L,
                                                            0x0000000303030000L, 0x0000000707070000L, 0x0000000E0E0E0000L, 0x0000001C1C1C0000L,
                                                            0x0000003838380000L, 0x0000007070700000L, 0x000000E0E0E00000L, 0x000000C0C0C00000L,
                                                            0x0000030303000000L, 0x0000070707000000L, 0x00000E0E0E000000L, 0x00001C1C1C000000L,
                                                            0x0000383838000000L, 0x0000707070000000L, 0x0000E0E0E0000000L, 0x0000C0C0C0000000L,
                                                            0x0003030300000000L, 0x0007070700000000L, 0x000E0E0E00000000L, 0x001C1C1C00000000L,
                                                            0x0038383800000000L, 0x0070707000000000L, 0x00E0E0E000000000L, 0x00C0C0C000000000L,
                                                            0x0303030000000000L, 0x0707070000000000L, 0x0E0E0E0000000000L, 0x1C1C1C0000000000L,
                                                            0x3838380000000000L, 0x7070700000000000L, 0xE0E0E00000000000L, 0xC0C0C00000000000L,
                                                            0x3030000000000000L, 0x0707000000000000L, 0x0E0E000000000000L, 0x1C1C000000000000L,
                                                            0x3838000000000000L, 0x7070000000000000L, 0xE0E0000000000000L, 0xC0C0000000000000L };
    
    
    /**
     * Constructor for class Chessboard.  Initializes board to beginning position and state.
     */
    public Chessboard() {
        initializeBoard();
    }
    
    /**
     * Returns a bitboard containing a certain type of piece and color.
     * @param pieceTypeColor The enumeration of piece and color (WHITE_KING = 0, etc.)
     * @return A 64 bit long containing a representation of the piecesByType of the specified type and color
     */
    public long getPieceSet(int pieceTypeColor) {
        if (ASSERTION_CHECKS) {
            assert (pieceTypeColor >= 0 && pieceTypeColor < 12);
        }
        // get intersection of piece type board and color board
        return piecesByType[pieceTypeColor / NUMBER_OF_COLORS] & piecesByColor[pieceTypeColor % NUMBER_OF_COLORS];
    }
    
    /**
     * Returns a bitboard containing all the piecesByType of a certain color.
     * @param color Either the constant WHITE or BLACK
     * @return      A 64 bit long containing a representation of all the piecesByType of the given color
     */
    public long getPiecesOfColor(int color) {
        if (ASSERTION_CHECKS) {
            assert (color == BLACK || color == WHITE);
        }
        return piecesByColor[color];
    }
    
    /**
     * Returns a bitboard containing 1s representing all the occupied squares.
     * @return  A 64 bit long containing a representation of all the pieces on the board
     */
    public long getAllPieces() {
        return piecesByColor[WHITE] | piecesByColor[BLACK];
    }
    
    /**
     * Adds a piece (or pieces) at a given position (in both type array and color array).  Does not check whether multiple bits are true within the position added.
     * @param position          Binary bitboard with a 1(s) at the position of the piece(s) to added
     * @param pieceTypeColor    Type and color of the piece to add (see enumeration)
     */
    public void addPiece(long position, int pieceTypeColor) {
        if (ASSERTION_CHECKS) {
            // make sure square on the board is not already occupied
            assert ((position & getAllPieces()) == 0);  
        }
        piecesByType[pieceTypeColor / NUMBER_OF_COLORS] |= position;
        piecesByColor[pieceTypeColor % NUMBER_OF_COLORS] |= position;
    }
    
    /**
     * Removes a piece (or pieces) at a given position (in both type array and color array)
     * @param position          Binary bitboard with a 1(s) at the position of piece(s) to be removed
     * @param pieceTypeColor    Type and color of the piece to remove (see enumeration)
     */
    public void removePiece(long position, int pieceTypeColor) {
        if (ASSERTION_CHECKS) {
            // make sure this type of piece exists on the board first
            assert ((position & getPieceSet(pieceTypeColor)) != 0);
        }
        piecesByType[pieceTypeColor / NUMBER_OF_COLORS] &= ~position;
        piecesByColor[pieceTypeColor % NUMBER_OF_COLORS] &= ~position;
    }
    
    /**
     * Gets the color of the piece at a given position on the board
     * @param position  Binary bitboard with a 1 at the position of the piece to check
     * @return          The color of the piece (see enumeration) or NO_PIECE if there is no piece
     */
    public int getPieceColorAt(long position) {
        for (int i = 0; i < NUMBER_OF_COLORS; i++) {
            if ((piecesByColor[i] & position) != 0) {
                return i;
            }
        }
        return NO_PIECE;
    }
    
    /**
     * Gets the type of the piece at a given position on the board
     * @param position  Binary bitboard with a 1 at the position of the piece to check
     * @return          The type of the piece (see enumeration) or NO_PIECE if there is no piece
     */
    public int getPieceTypeAt(long position) {
        for (int i = 0; i < NUMBER_OF_TYPES; i++) {
            if ((piecesByType[i] & position) != 0) {
                return i * NUMBER_OF_COLORS;
            }
        }
        return NO_PIECE;
    }

    /**
     * Gets the piece at a given position
     * @param position  A bitboard with a 1 in the position to check
     * @return  The type and color of the piece (see enumeration)
     */
    public int getPieceTypeColorAt(long position) {
        int pieceColor = getPieceColorAt(position);
        if (pieceColor == NO_PIECE) {
            return NO_PIECE;
        }
        return pieceColor + getPieceTypeAt(position);
    }
    
    /**
     * Gets a clone of the piecesByType array
     * @return  A clone of the piecesByType array
     */
    public long[] getPiecesByTypeClone() {
        return piecesByType.clone();
    }
    
    /**
     * Gets a clone of the piecesByColor array
     * @return  A clone of the piecesByColor array
     */
    public long[] getPiecesByColorClone() {
        return piecesByColor.clone();
    }
    
    /**
     * Gets the board state
     * @return  An int containing bits representing various details about the board state (see declaration)
     */
    public int getBoardState() {
        return boardState;
    }
    
    /**
     * Returns a clone of the current chessboard, complete with a cloned set of dependent objects
     * @return  A cloned copy of the board
     **/
    public Chessboard getClone() {
        Chessboard clonedBoard = new Chessboard();
        clonedBoard.piecesByType = this.piecesByType.clone();
        clonedBoard.piecesByColor = this.piecesByColor.clone();
        clonedBoard.boardState = this.boardState;
        clonedBoard.ply = this.ply;
        return clonedBoard;
    }
    
    /**
     * Sets the ply count
     * @param ply   ply (number of moves of each player) count beginning with 1.  
     */
    protected void setPly(int ply)  {
        if (ASSERTION_CHECKS) {
            assert (ply > 0);
        }
        this.ply = ply;
    }
    
    /**
     * Gets the ply count
     * @return  ply (number of moves of each player) count beginning with 1.
     */
    public int getPly()  {
        return ply;
    }
    
    /**
     * Gets the moves array
     * @return  An array containing the moves made so far on this board
     */
    public ArrayList<Move> getMoves() {
        return moves;
    }
    
    /**
     * Initialize the board to the starting position.  The board is represented by an array of 6 longs for the piece types and an array of 2 longs for the color.  
     */
    public void initializeBoard() {
        piecesByType[KING   / NUMBER_OF_COLORS] = 0x1000000000000010L;
        piecesByType[QUEEN  / NUMBER_OF_COLORS] = 0x0800000000000008L;
        piecesByType[ROOK   / NUMBER_OF_COLORS] = 0x8100000000000081L;
        piecesByType[BISHOP / NUMBER_OF_COLORS] = 0x2400000000000024L;
        piecesByType[KNIGHT / NUMBER_OF_COLORS] = 0x4200000000000042L;
        piecesByType[PAWN   / NUMBER_OF_COLORS] = 0x00FF00000000FF00L;
        piecesByColor[WHITE]                    = 0x000000000000FFFFL;
        piecesByColor[BLACK]                    = 0xFFFF000000000000L;
        boardState = 0;
        ply = 1; // start with first move (white)
    }
    
    /**
     * Converts a PGN piece type String to an enumerated int constant.
     * @param pieceType Single character String containing a capital letter representing a piece type according to the PGN standard.  ("K" = King, "P" = Pawn, "N" = Knight, etc)
     * @return  One of the constants enumerating piece type (0 = King, 2 = Queen, 4 = Rook, etc.)
     * @throws InvalidMoveException     Thrown if a non-valid piece type code is specified.
     */
    public static int getPieceTypeEnum(String pieceType) throws InvalidMoveException {
        switch (pieceType) {
            case "K":
                return KING;
            case "Q":
                return QUEEN;
            case "R":
                return ROOK;
            case "B":
                return BISHOP;
            case "N":
                return KNIGHT;
            case "P":
                return PAWN;
            default:
                throw new InvalidMoveException("Invalid Piece Type!");
        }
    }

    /**
     * Converts an enumerated int constant to a PGN piece type String
     * @param pieceTypeColor    An int corresponding to one of the constants enumerating piece type.  Optionally includes the color bit also.  Ex: 0 = KING, 3 = QUEEN, 4 = ROOK, etc.
     * @return  Single character String containing a capital letter representing a piece type according to the PGN standard.  ("K" = King, "P" = Pawn, "N" = Knight, etc)
     */
    public static String getPieceTypeCharacter(int pieceTypeColor) {
        switch(getPieceType(pieceTypeColor)) {
            case KING:
                return "K";
            case QUEEN:
                return "Q";
            case ROOK:
                return "R";
            case BISHOP:
                return "B";
            case KNIGHT:
                return "N";
            case PAWN:
                return "P";
            default:
                if (ASSERTION_CHECKS) {
                    assert false;
                }
                return "";
        }        
    }
    
    /**
     * Gets the piece type (see enumeration) from the code containing piece type and color
     * @param pieceTypeColor    Code with piece type including the color bit
     * @return                  Piece type with color bit removed
     */
    public static int getPieceType(int pieceTypeColor) {
        return pieceTypeColor & 0b11111110;
    }
    
    /**
     * Gets the piece color (see enumeration) from the code containing piece type and color
     * @param pieceTypeColor    Code with piece type and color
     * @return                  Color bit of the piece
     */
    public static int getPieceColor(int pieceTypeColor) {
        return pieceTypeColor & 0b00000001;
    }
    
    /**
     * Gets the player whose turn it currently is to play
     * @return  An int representing the player whose turn it currently is to play (see enumeration)
     */
    public int getCurrentPlayer() {
        return Move.getTurn(ply);
    }
    
    /**
     * Gets the player whose turn it currently is not to play
     * @return  An int representing the player whose turn it is currently not to play
     */
    public int getOpposingPlayer() {
        return 1 - Move.getTurn(ply);
    }
    
    /**
     * Identifies whether a player is in check
     * @param player        WHITE or BLACK (see enumeration)
     * @return              True indicates is in check; false indicates is not in check
     */
    public boolean isInCheck(int player) {
        return Square.isAttacked(Square.toSerial(getPieceSet(player + KING)), this, player);
    }
    
    /**
     * Identifies whether a player is checkmated.  Note: this method should not be used by engine
     * @param player        WHITE or BLACK (see enumeration)
     * @return              True indicates is checkmated; false indicates is not checkmated
     */
    public boolean isCheckmated(int player) {
        return (isInCheck(player) && getLegalMoves(player).isEmpty());
    }
    
    /**
     * Returns an ArrayList of all the possible(pseudo-legal) moves for a color, possibly including some moves that illegally leave king in check
     * @param player        WHITE or BLACK (see enumeration)
     * @return              ArrayList of possible(pseudo-legal) moves
     */
    public ArrayList<Move> getPossibleMoves(int player) {
        ArrayList<Move> possibleMoves = new ArrayList<>(32);
        ArrayList<Integer> piecePositions = Piece.extractSerialsFromBitboard(piecesByColor[player], 16);
        for (int piecePosition : piecePositions)  {
            int pieceTypeColor = getPieceTypeAt(Square.toBitwise(piecePosition)) + player;
            for (int destinationSerial : Piece.extractSerialsFromBitboard(Piece.getPossibleMoves(pieceTypeColor, piecePosition, this), 60)) {
                possibleMoves.add(new Move(ply, piecePosition, destinationSerial, pieceTypeColor, this));
            }
        }
        return possibleMoves;
        
        // todo: return a separate Move for each possible pawn promotion type
    }
    
    /**
     * Returns an ArrayList of all the legal moves for a color.  Note: This method should not be used by engine
     * @param player        WHITE or BLACK (see enumeration)
     * @return              ArrayList of all legal moves
     */
    public ArrayList<Move> getLegalMoves(int player) {
        ArrayList<Move> legalMoves = new ArrayList<>(32);
        ArrayList<Integer> piecePositions = Piece.extractSerialsFromBitboard(piecesByColor[player], 16);
        for (int piecePosition : piecePositions)  {
            int pieceTypeColor = getPieceTypeColorAt(Square.toBitwise(piecePosition));
            for (int destinationSerial : Piece.extractSerialsFromBitboard(Piece.getLegalMoves(pieceTypeColor, piecePosition, this), 60)) {
                legalMoves.add(new Move(ply, piecePosition, destinationSerial, pieceTypeColor, this));
            }
        }
        return legalMoves;    
    }
    
    
    /**
     * Identifies whether the last move on the board was a double pawn push
     * @return True if the last move was a double pawn push; false if not
     */
    public boolean lastMoveIsDoublePawnPush() {
        if (moves.size() <= 1) {
            return false;
        }
        return (getLastMove().isDoublePawnPush());
    }
    
    /**
     * Returns last move made
     * @return  Last move made
     */
    public Move getLastMove() {
        return moves.get(moves.size() - 1);
    }
    
    /**
     * Returns the square which is the destination when capturing en passant.  Note: Must verify that last move was double pawn push before calling this method.
     * @return  the square which is the destination when capturing en passant
     */
    public long getEnPassantDestinationSquare() {
        if (ASSERTION_CHECKS) {
            assert (lastMoveIsDoublePawnPush());
        }
        return new Square(getFileOfLastMove(), getEnPassantDestinationRank()).toBitwise();
    }
    
    /**
     * Returns the square containing the pawn that may be captured en passant.  Note: Must verify that last move was double pawn push before calling this method.
     * @return  the square containing the pawn that may be captured en passant. 
     */
    public long getEnPassantCaptureSquare() {
        if (ASSERTION_CHECKS) {
            assert (lastMoveIsDoublePawnPush());
        }
        return new Square(getFileOfLastMove(), getEnPassantCaptureRank()).toBitwise();
    }
    
    /**
     * Returns the square containing the pawn that may be captured en passant.  Note: Must verify that last move was double pawn push before calling this method.
     * @param destinationSerial The destination of the last move (double pawn push)
     * @return  the square containing the pawn that may be captured en passant. 
     */
    public static long getEnPassantCaptureSquare(int destinationSerial) {
        int rank;
        if (destinationSerial >= 32) {
            rank = 4;
        }
        else {
            rank = 3;
        }
        int file = Square.getFile(destinationSerial);
        return new Square(file, rank).toBitwise();
    }
    
    /**
     * Returns file of the last move.
     * @return  file of the last move
     */
    public int getFileOfLastMove() {
        return Square.getFile(getLastMove().getDestinationSerial());
    }
    
    /**
     * Returns the rank of the pawn that will be removed when capturing en passant.
     * @return  the rank of the pawn that will be removed when capturing en passant.
     */
    public int getEnPassantCaptureRank() {
        if (getOpposingPlayer() == BLACK) {
            return 4;
        }
        else {
            return 3;
        }
    }
    
    /**
     * Returns the rank of the pawn's destination when capturing en passant.
     * @return  the rank of the pawn's destination when capturing en passant.
     */
    public int getEnPassantDestinationRank() {
        if (getOpposingPlayer() == BLACK) {
            return 5;
        }
        else {
            return 2;
        }
    }
    
    /**
     * Gets whether the castle pieces (king and rook) have moved.
     * @param color The color of the side that is potentially castling, following the enumerated constants
     * @param side  The side of the castle - 0 for kingside, 1 for queenside
     * @return      Whether the castle pieces have moved for the given color and side
     */
    public boolean castlePiecesHaveMoved(int color, int side) {
        return ((CANT_CASTLE[side][color] & boardState) != 0);
    }
    
    /**
     * Sets whether the castle pieces (king or rook) have moved
     * @param pieceTypeColor    Sum of the enumerations for the type and color of the piece
     * @param side              The side of the rook - 0 for king's rook, 1 for queen's rook.  If piece is king, can be 0.
     * @param haveMoved         The status of whether the pieces have moved (true when moving, false when undoing the move)
     */
    public void setCastlePiecesMoved(int pieceTypeColor, int side, boolean haveMoved) {
        int bitMask = 0;
        if (pieceTypeColor == WHITE + KING) {
            bitMask = 16;
        }
        else if (pieceTypeColor == BLACK + KING) {
            bitMask = 32;
        }
        else if (pieceTypeColor == WHITE + ROOK) {
            bitMask = 64 + (64 * side);  // 128's place bit for queen's rook
        }
        else if (pieceTypeColor == BLACK + ROOK) {
            bitMask = 256 + (256 * side); // 1024's place bit for queen's rook
        }
        if (haveMoved) {
            boardState |= bitMask;
        }
        else {
            boardState &= ~bitMask;
        }
    }
    
    /**
     * Checks whether the space between the king and rook is empty, making it possible to castle.
     * @param color Color potentially castling
     * @param side  0 for Kingside, 1 for Queenside
     * @return      true if the space between the king and given rook is empty, false if occupied by any piece(s)
     */
    public boolean castleSpaceIsEmpty(int color, int side) {
        return ((CASTLESPACE[side][color] & (piecesByColor[WHITE] | piecesByColor[BLACK])) == 0);
    }
    
    /**
     * Checks whether the king, rook, and spaces in between are attacked, preventing castle.
     * @param color Color potentially castling
     * @param side  0 for Kingside, 1 for Queenside
     * @return      true if the king, rook, or space in between is attacked, false otherwise
     */
    public boolean castleSpaceOrPiecesAreAttacked(int color, int side) {
        for (int castlespaceSquareSerial: Piece.extractSerialsFromBitboard(CASTLESPACE_AND_PIECES[side][color])) {
            if (Square.isAttacked(castlespaceSquareSerial, this, color)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Moves a piece from one square to another, adds the move to the move array, and increments the ply count.
     * @param move  Move object specifying the move to be made
     */
    public void move(Move move) {
        moves.add(move);
        move.make();
        
        if ((piecesByColor[WHITE] | piecesByColor[BLACK]) != (piecesByType[KING] | piecesByType[QUEEN / 2] | piecesByType[ROOK / 2] | piecesByType[BISHOP / 2] | piecesByType[KNIGHT / 2] | piecesByType[PAWN / 2])) {
            int i = 0;
        }
        
        ply++;
    }
    
    /**
     * Moves a piece from one square to another, adds the move to the move array, and increments the ply count.
     * @param movetext  PGN notation specifying the move to be made
     * @throws InvalidMoveException     If movetext in invalid
     */
    public void move(String movetext) throws InvalidMoveException {
        Move newMove = new Move (ply, movetext, this);
        move(newMove);
       
    }
    
    /**
     * Undoes last move made
     */
    public void undoLastMove() {
        ply--;
        getLastMove().undo();
        moves.remove(moves.size() - 1);
    }
    
    public boolean sufficientMaterialToCheckmate() {
        if (piecesByType[QUEEN / 2] > 0 || piecesByType[PAWN / 2] > 0 || piecesByType[ROOK / 2] > 0) {
            return true;
        }
        for (int color = WHITE; color <= BLACK; color++) {
            if (Long.bitCount(piecesByType[BISHOP / 2] & piecesByColor[color]) >= 2) {
                return true;
            }
        }
        for (int color = WHITE; color <= BLACK; color++) {
            if ((piecesByType[BISHOP / 2] & piecesByColor[color]) > 0 && (piecesByType[KNIGHT / 2] & piecesByColor[color]) > 0) {
                return true;
            }
        }
        return false;
    }
}