package com.circumspectus.ChessApplication;

import static com.circumspectus.ChessApplication.Chessboard.ASSERTION_CHECKS;
import java.util.ArrayList;

/**
 * Represents and executes a move on the chessboard.
 * @author Christopher Stieg
 * @version 1.0
 */
public class Move implements Comparable<Move> {
    private int originSerial;   // squares for origin and destination are represented as a serial int, 0 being a1, 1 being a2, 63 being h8
    private int destinationSerial;
    private int pieceTypeColor;
    private int capturedPieceTypeColor;  // type and color of the captured piece
    private int originalBoardState;  // copy of the board state before the move
    private int evaluation;  // field to store value of move for sorting
    private long boardHashValue;  // hash value of board after move
    
    private int moveType;
        /*  0 = Quiet move
            1 = Double pawn push
            2           UNUSED
            3           UNUSED
            4 = Simple capture             Bit 3 (0b0100) indicates capture
            5 = En passant capture
            6           UNUSED
            7           UNUSED
            8 = Pawn promotion to queen    Bit 4 (0b1000) indicates pawn promotion
            9 = Pawn promotion to rook
           10 = Pawn promotion to bishop
           11 = Pawn promotion to knight
           12 = Capture + pawn promotion to queen
           13 = Capture + pawn promotion to rook
           14 = Capture + pawn promotion to bishop
           15 = Capture + pawn promotion to knight      */
    
    private static final int KING_AT_ORIGIN = 4;
    private static final long KINGS_ROOK_AT_ORIGIN = 7;
    private static final long QUEENS_ROOK_AT_ORIGIN = 0;
    
    private int ply;
    private Chessboard board;
    
    /**
     * Constructor for class Move
     * @param ply           Ply of move (half moves)
     * @param originSerial  Origin square of the move
     * @param destinationSerial Destination square of the move
     * @param pieceTypeColor     Enumeration specifying color and type of the piece (see Chessboard)
     * @param board         Chessboard object
     */
    public Move(int ply, int originSerial, int destinationSerial, int pieceTypeColor, Chessboard board) {
        this.ply = ply;
        this.originSerial = originSerial;
        this.destinationSerial = destinationSerial;
        this.pieceTypeColor = pieceTypeColor;
        this.board = board;
        this.originalBoardState = board.getBoardState();
        checkMoveType();
        evaluation = Integer.MIN_VALUE + 1;  // default value 
    }
    
    /**
     * Constructor for class Move which creates a move involving a pawn promotion
     * @param ply           Ply of move (half moves)
     * @param originSerial  Origin square of the move
     * @param destinationSerial Destination square of the move
     * @param pieceTypeColor     Enumeration specifying color and type of the piece (see Chessboard)
     * @param board         Chessboard object
     * @param promotionPieceTypeColor    Type and color of piece that pawn will become when promoted to final rank (see enumeration in Chessboard)
     */
    public Move(int ply, int originSerial, int destinationSerial, int pieceTypeColor, Chessboard board, int promotionPieceTypeColor) {
        this(ply, originSerial, destinationSerial, pieceTypeColor, board);
        setPromotionFlags(promotionPieceTypeColor);
    }
   
    /**
     * Constructor for class Move which accepts a PGN movetext
     * @param ply           Ply of move (half moves)
     * @param movetext      PGN movetext following standard form
     * @param board         Chessboard object
     * @throws InvalidMoveException     When move is not valid, as the movetext will often be user generated
     */
    public Move(int ply, String movetext, Chessboard board) throws InvalidMoveException {
        this.ply = ply;
        this.board = board;
        this.originalBoardState = board.getBoardState();
        
        if (movetext.isEmpty())  {
            throw new InvalidMoveException("Move is empty!!");
        }
    
        // castle kingside
        if (movetext.equals("O-O")) {
            this.originSerial = 4 + getCastleRankAddend();
            this.destinationSerial = 6 + getCastleRankAddend();
            this.capturedPieceTypeColor = Chessboard.NO_PIECE;
            this.pieceTypeColor = Chessboard.KING + board.getCurrentPlayer();
 
            // must set king has moved
            doErrorChecks();
            return;
        }
        
        // castle queenside
        if (movetext.equals("O-O-O")) {
            this.originSerial = 4 + getCastleRankAddend();
            this.destinationSerial = 2 + getCastleRankAddend();
            this.capturedPieceTypeColor = Chessboard.NO_PIECE;
            this.pieceTypeColor = Chessboard.KING + board.getCurrentPlayer();
            // must set king has moved
            doErrorChecks();
            return;
        }
        
        // decode PGN
        String pieceCode;
        String disambiguation = "";
        boolean capture = false;
        this.capturedPieceTypeColor = Chessboard.NO_PIECE;
        boolean check = false;
        boolean checkmate = false;
        
        // extract and remove piece code from movetext
        if (Character.isUpperCase(movetext.charAt(0))) {
            pieceCode = movetext.substring(0,1);
            movetext = movetext.substring(1);
        }
        else {
            pieceCode = "P";
        }
        pieceTypeColor = Chessboard.getPieceTypeEnum(pieceCode) + board.getCurrentPlayer();       
        
        // process and remove other symbols 
        if (movetext.contains("+")) {
            check = true;
            movetext = movetext.replace("+", "");
            // todo: verify that the piece putting king in check
        } 
        else if (movetext.contains("#")) {  // can't have both check and checkmate at the same time
            checkmate = true;
            movetext = movetext.replace("#", "");
            // todo: verify that the move is checkmate
        }
                
        if (movetext.contains("=")) {
            int i = movetext.indexOf("=");
            int promotionPieceType = Chessboard.getPieceTypeEnum(movetext.substring(i + 1, i + 2).toUpperCase()) + board.getCurrentPlayer();
            if (promotionPieceType < Chessboard.QUEEN || promotionPieceType >= Chessboard.PAWN) {
                throw new InvalidMoveException("Can only promote to queen, rook, bishop, or knight.");
            }
            setPromotionFlags(promotionPieceType);
            movetext = movetext.substring(0, i); // should be nothing after =X
        }
        
        if (movetext.contains("x")) {
            capture = true;
            movetext = movetext.replace("x", "");
            destinationSerial = new Square(movetext.substring(movetext.length() - 2)).toSerial();
            setCapturedPiece();

            // todo: verify that piece is being captured
        }
        
        // all that should be left now is any disambiguation and the target square coordinates
        if (movetext.length() > 2) { // then there is disambiguation
            disambiguation = movetext.substring(0, movetext.length() - 2);
            movetext = movetext.substring(movetext.length() - 2);
            
            if (disambiguation.length() > 2) {
                throw new InvalidMoveException("Superfluous characters in movetext!");
            }
        }
        
        destinationSerial = new Square(movetext).toSerial();
        
        // origin will change to an actual legitimate single square bitboard provided any appropriate disambiguation is found
        originSerial = Chessboard.NO_PIECE;

        ArrayList<Piece> possiblyMoveable = Piece.bitboardToPieces(board.getPieceSet(pieceTypeColor), board, pieceTypeColor);
        if (possiblyMoveable.isEmpty()) {
            throw new InvalidMoveException("No pieces of this type to move!");
        }
        
        ArrayList<Piece> moveable = new ArrayList<>();
 
        for (Piece piece : possiblyMoveable) {
            if ((piece.getPossibleMoves() & Square.toBitwise(destinationSerial)) != 0) {
                moveable.add(piece);
            }
        }
        
        if (moveable.isEmpty()) {
            throw new InvalidMoveException("No pieces of this type can move to the specified square!");
        }
        
        // disambiguate between possible piecesByType to move
        if (moveable.size() > 1) { 
            // there is full disambiguation square name
            if (disambiguation.length() > 1) { 
                try {
                    originSerial = new Square(disambiguation).toSerial();
                }
                catch (InvalidSquareException ise) {
                    throw new InvalidMoveException("Disambiguation text incorrect!");
                }
            }
            else {
                // only file letter is given as disambiguation
                if (Character.isLetter(disambiguation.charAt(0))) {
                    for (Piece piece : moveable) {
                        if (new Square(piece.getCurrentPositionSerial()).getFileChar() == disambiguation.charAt(0)) {
                            originSerial = piece.getCurrentPositionSerial();
                        }
                    }
                    if (originSerial == Chessboard.NO_PIECE) {
                        throw new InvalidMoveException("No piece to move in disambiguation file!");
                    }
                }
                // only rank number is given as disambiguation
                else {
                    for (Piece piece : moveable) {
                        if (new Square(piece.getCurrentPositionSerial()).getRankChar() == disambiguation.charAt(0)) {
                            originSerial = piece.getCurrentPositionSerial();
                        }
                    }
                    if (originSerial == Chessboard.NO_PIECE) {
                        throw new InvalidMoveException("No piece to move in disambiguation rank!");
                    }
                }
            }
        }
        // only one possible piece to move
        else {
            originSerial = moveable.get(0).getCurrentPositionSerial();
        }
        checkMoveType();
        doErrorChecks();

        Piece piece = new Piece(originSerial, pieceTypeColor, board);
        if (ply > 2 && !piece.isLegalDestination(destinationSerial)) {
            throw new InvalidMoveException("Not legal move!  King is in check!");
        }
    }
    
    /**
     * Executes the move represented by the current Move object.  Updates the locations of the pieces on the bitboards as well as the board state if necessary.
     */
    public void make() {
        setHasMoved();

        //also move rooks if castling
        if (isCastleShort()) {
            castleRookShort();
        }
        if (isCastleLong())  {
            castleRookLong();
        }
        
        // move piece to destination
        board.removePiece(Square.toBitwise(originSerial), pieceTypeColor);
        if (isEnPassantCapture()) {
            board.removePiece(board.getEnPassantCaptureSquare(), capturedPieceTypeColor);
        }
        else if (isCapture()) {        
            board.removePiece(Square.toBitwise(destinationSerial), capturedPieceTypeColor);
        }
        
        board.addPiece(Square.toBitwise(destinationSerial), pieceTypeColor);
        
        // if pawn promotion, change piece type
        if (isPromotion()) {
            promotePawn();
        }
    }
    
    /**
     * If king or rook are moving for the first time, set the board state to indicate that the pieces have moved.
     */
    private void setHasMoved() {
        int pieceType = Chessboard.getPieceType(pieceTypeColor);
        int castleRankAddend = getCastleRankAddend();
        if (pieceType == Chessboard.KING && originSerial == KING_AT_ORIGIN + castleRankAddend) {
            board.setCastlePiecesMoved(pieceTypeColor, 0, true);
        }
        else if (pieceType == Chessboard.ROOK && originSerial == KINGS_ROOK_AT_ORIGIN + castleRankAddend) {
            board.setCastlePiecesMoved(pieceTypeColor, 0, true);
        }
        else if (pieceType == Chessboard.ROOK && originSerial == QUEENS_ROOK_AT_ORIGIN + castleRankAddend) {
            board.setCastlePiecesMoved(pieceTypeColor, 1, true);
        }
    }
    
    /**
     * Restores a piece that was captured.  Used in undo move.
     */
    private void restoreCapturedPiece() {
        if (isEnPassantCapture()) {
            // restore captured pawn at the file of the destination (which double push has passed), and the rank of the origin (because en passant capture allows a pawn
            // to capture the double push pawn which is directly beside it.
            board.addPiece(Chessboard.getEnPassantCaptureSquare(destinationSerial), capturedPieceTypeColor);
        }
        else {
            board.addPiece(Square.toBitwise(destinationSerial), capturedPieceTypeColor);
        }
    }
    
    /**
     * Sets the moveType variable.  Checks for double pawn push, en passant capture, capture.
     */
    private void checkMoveType() {
        if (Chessboard.getPieceType(pieceTypeColor) == Chessboard.PAWN) {
            // check whether is double pawn push
            if (Math.abs(Square.getRank(destinationSerial) - Square.getRank(originSerial)) == 2) {
                moveType = 1;
                return;
            }
            
            // check whether is en passant capture
            if (board.lastMoveIsDoublePawnPush() && destinationSerial == Square.toSerial(board.getEnPassantDestinationSquare())) {
                moveType = 5;
                setCapturedPiece();
                return;
            }
        }
        
        // check whether is capture
        if ((Square.toBitwise(destinationSerial) & board.getPiecesOfColor(board.getOpposingPlayer())) != 0) {
            moveType = 4;
            setCapturedPiece();
        }
        
        // check whether is pawn promotion
        if (Chessboard.getPieceType(pieceTypeColor) == Chessboard.PAWN && (Square.toBitwise(destinationSerial) & (Chessboard.RANKS[0] | Chessboard.RANKS[7])) != 0) {
            setPromotionFlags(Chessboard.QUEEN + Chessboard.getPieceColor(pieceTypeColor));
        }
    }
    
    /**
     * Checks whether the move is a quiet move (not capture or promotion)
     * @return  True if not capture or promotion; false if it is
     */
    public boolean isQuietMove() {
        return (moveType < 2);
    }
    
    /**
     * Checks whether the move is a double pawn push
     * @return True if double pawn push, false otherwise
     */
    public boolean isDoublePawnPush() {
        return (moveType == 1);
    }

    /**
     * Checks whether the move is a capture (including en passant capture and pawn promotion capture)
     * @return True if capture, false otherwise
     */
    public boolean isCapture() {
        return ((moveType & 0b0100) != 0);
    }
    
    /**
     * Checks whether the move is an en passant capture
     * @return True if en passant capture
     */
    public boolean isEnPassantCapture() {
        return (moveType == 5);
    }
    
    /**
     * Gets the ply count of the Move
     * @return  ply count of the Move
     */
    public int getPly() {
        return ply;
    }
    
    /**
     * Gets the move number of the Move
     * @return move number of the Move
     */
    public int getMoveNo() {
        return (ply + 1) / 2;  // int division will change (1 + 1) / 2 = 1; (2 + 1) / 2 = 1.5 = 1; (3 + 1) / 2 = 2, etc.
    }
    
    /**
     * Returns the player making the move
     * @param ply   Ply count of the move
     * @return      Int representation of the player (see enumeration in Chessboard)
     */
    public static int getTurn(int ply) {
        return 1 - (ply % 2);
    }
    
    /**
     * Stores the hash value of the board resulting from the move.
     * @param hashValue Hash value of the board resulting from the move
     */
    public void setBoardHashValue(long hashValue) {
        this.boardHashValue = hashValue;
    }
    
    /**
     * Retrieves the hash value of the board resulting from the move.
     * @return  Hash value of the board resulting from the move
     */
    public long getBoardHashValue() {
        return boardHashValue;
    }
    
    /**
     * Sets the flag for pawn promotion.  Fails assertion if not pawn promotion.
     * @param promotionPieceTypeColor   Sum of the enumerations for type and color of the piece into which the pawn will be promoted (queen, etc.)
     */
    private void setPromotionFlags(int promotionPieceTypeColor) {
        if (ASSERTION_CHECKS) {
            assert(board.getPieceTypeAt(Square.toBitwise(originSerial)) == Chessboard.PAWN && (Square.getRank(destinationSerial) == 0  || Square.getRank(destinationSerial) == 7));
        }
        moveType |= 0b1000;
        moveType &= 0b1100;  // remove any prior piece type bits before adding them again
        moveType += (Chessboard.getPieceType(promotionPieceTypeColor) - 2) / 2;
    }
    
    /**
     * Checks whether the move is a pawn promotion.
     * @return True if pawn promotion, false otherwise
     */
    boolean isPromotion() {
        return ((moveType & 0b1000) != 0);
    }
    
    /**
     * Gets the origin of the move
     * @return  An int representing the origin square of the move
     */
    public int getOriginSerial() {
        return originSerial;
    }
    
    /**
     * Sets the origin of the move
     * @param originSerial An int representing the origin square of the move
     */
    public void setOriginSerial(int originSerial) {
        this.originSerial = originSerial;
    }
    
    /**
     * Gets the destination of the move
     * @return  An int representing the destination square of the move
     */
    public int getDestinationSerial() {
        return destinationSerial;
    }
    
    /**
     * Sets the destination of the move
     * @param destinationSerial An int representing the destination square of the move
     */
    public void setDestination(int destinationSerial) {
        this.destinationSerial = destinationSerial;
    }
    
    /**
     * Sets the piece type and color which is being moved
     * @param pieceTypeColor The sum of the type and color enumerations for the type and color of the piece being moved
     */
    public void setPieceTypeColor(int pieceTypeColor) {
        this.pieceTypeColor = pieceTypeColor;
    }
    
    /**
     * Sets the piece type and color which is being captured
     * @param capturedPieceTypeColor The sum of the type and color enumerations for the type and color of the piece being captured
     */
    public void setCapturedPieceTypeColor(int capturedPieceTypeColor) {
        this.capturedPieceTypeColor = capturedPieceTypeColor;
    }
    
    /**
     * Gets the piece type and color of the moving piece
     * @return The sum of the piece type and color addends of the moving piece
     */
    public int getPieceTypeColor() {
        return pieceTypeColor;
    }
    
    /**
     * Gets the piece type and color of the captured piece (if any)
     * @return The sum of the piece type and color addends of the captured piece
     */
    public int getCapturedPieceTypeColor() {
        return capturedPieceTypeColor;
    }
    
    
    /**
     * Gets the type and color of a promoted pawn from the movetype field
     * @return  The piece type and color of the promoted pawn
     */
    private int getPromotionPieceTypeColor() {
        /*  Queen = 0
            Rook = 1
            Bishop = 2
            Knight = 3
        */
        int pieceBits = moveType & 0b0011;
        int pieceType = (pieceBits * 2) + 2;
        return pieceType + Chessboard.getPieceColor(pieceTypeColor);
    }
 
    
    /**
     * Sets the board on which the move is to take place
     * @param board The board on which the move is to take place
     */
    public void setBoard(Chessboard board) {
        this.board = board;
    }
    
    /**
     * Gets the board on which the move is to take place
     * @return The board on which the move is to take place
     */
    public Chessboard getBoard() {
        return board;
    }
    
    /**
     * Checks whether move is a short castle (kingside)
     * @return True if move is short castle, false otherwise
     */
    public boolean isCastleShort() { // kingside 
        Long origin = Square.toBitwise(originSerial);
        Long destination = Square.toBitwise(destinationSerial);
        return (board.getPieceTypeAt(origin) == Chessboard.KING && Square.getFile(origin) == 4 && Square.getFile(destination) == 6);
    }
    
    /**
     * Checks whether move is a long castle (queenside)
     * @return True if move is long castle, false otherwise
     */
    public boolean isCastleLong() { // queenside
        Long origin = Square.toBitwise(originSerial);
        Long destination = Square.toBitwise(destinationSerial);
        return (board.getPieceTypeAt(origin) == Chessboard.KING && Square.getFile(origin) == 4 && Square.getFile(destination) == 2);
    }
    
    /**
     * Castles short (kingside)
     */
    private void castleRookShort() { 
        int rank = Square.getRank(originSerial);
        long rookOrigin = new Square(7,rank).toBitwise();
        long rookDestination = new Square(5, rank).toBitwise();
        board.addPiece(rookDestination, Chessboard.ROOK + Chessboard.getPieceColor(pieceTypeColor));
        board.removePiece(rookOrigin, Chessboard.ROOK + Chessboard.getPieceColor(pieceTypeColor));
    }
    
    /**
     * Castles short (queenside)
     */
    private void castleRookLong() {
        int rank = Square.getRank(originSerial);
        long rookOrigin = new Square(0,rank).toBitwise();
        long rookDestination = new Square(3, rank).toBitwise();
        board.addPiece(rookDestination, Chessboard.ROOK + Chessboard.getPieceColor(pieceTypeColor));
        board.removePiece(rookOrigin, Chessboard.ROOK + Chessboard.getPieceColor(pieceTypeColor));
    }
    
    /**
     * Undoes a short castle (kingside)
     */
    private void restoreCastledRookShort() {
        int rank = Square.getRank(originSerial);
        long rookOrigin = new Square(5,rank).toBitwise();
        long rookDestination = new Square(7, rank).toBitwise();
        board.addPiece(rookDestination, Chessboard.ROOK + Chessboard.getPieceColor(pieceTypeColor));
        board.removePiece(rookOrigin, Chessboard.ROOK + Chessboard.getPieceColor(pieceTypeColor));
    }
    
    /**
     * Undoes a long castle (queenside)
     */
    private void restoreCastledRookLong() {
        int rank = Square.getRank(originSerial);
        long rookOrigin = new Square(3,rank).toBitwise();
        long rookDestination = new Square(0, rank).toBitwise();
        board.addPiece(rookDestination, Chessboard.ROOK + Chessboard.getPieceColor(pieceTypeColor));
        board.removePiece(rookOrigin, Chessboard.ROOK + Chessboard.getPieceColor(pieceTypeColor));
    }
    
    /**
     * Executes a pawn promotion
     */
    private void promotePawn() {
        Long destination = Square.toBitwise(destinationSerial);
        board.removePiece(destination, pieceTypeColor);
        board.addPiece(destination, getPromotionPieceTypeColor());
    }
    
    /**
     * Undoes a pawn promotion
     */
    private void undoPawnPromotion() {
        Long destination = Square.toBitwise(destinationSerial);
        
        board.removePiece(destination, getPromotionPieceTypeColor());
        board.addPiece(destination, pieceTypeColor);

    }
    
    /**
     * Undoes a move
     */
    public void undo() {
        if (isPromotion()) {
            undoPawnPromotion();
        }
        // move piece back where it was originally
        Long origin = Square.toBitwise(originSerial);
        Long destination = Square.toBitwise(destinationSerial);
        board.addPiece(origin, pieceTypeColor);
        board.removePiece(destination, pieceTypeColor);
        //doErrorChecks();
        if (isCapture()) {
            restoreCapturedPiece();
        }
        //also move rooks if castling
        if (isCastleShort()) {
            restoreCastledRookShort();
        }
        if (isCastleLong())  {
            restoreCastledRookLong();
        }
        
        // restore original board state
        board.boardState = originalBoardState;
    }
    
    /**
     * Sets the captured piece
     * Assumes capture is true.
     */
    private void setCapturedPiece() {
        if (ASSERTION_CHECKS) {
            assert (isCapture());
        }
        Long destination = Square.toBitwise(destinationSerial);
        capturedPieceTypeColor = board.getPieceTypeAt(destination) + board.getPieceColorAt(destination);
        if (board.lastMoveIsDoublePawnPush() && pieceTypeColor == Chessboard.PAWN + board.getCurrentPlayer() && destination == board.getEnPassantDestinationSquare()) {
            capturedPieceTypeColor = Chessboard.PAWN + board.getOpposingPlayer();
        }
    }
    
    /**
     * Returns a clone of the Move object
     * @return a clone of the Move object
     */
    public Move getClone() {
        Move clonedMove = new Move(ply, originSerial, destinationSerial, pieceTypeColor, board);
        clonedMove.moveType = this.moveType;
        return clonedMove;
    }
    
    /**
     * Gets a multiplier allowing translating a long position (row) on a bitboard to the correct rank (white or black)
     * @return 
     */
    private int getCastleRankAddend() {
        if (board.getCurrentPlayer() == Chessboard.BLACK) {
            return 7 * 8;
        }
        else {
            return 0;
        }
    }
    
    /**
     * Checks to make sure the move is legal
     * @throws InvalidMoveException 
     */
    private void doErrorChecks() throws InvalidMoveException {
        Long origin = Square.toBitwise(originSerial);
        if (board.getPieceTypeAt(origin) == Chessboard.NO_PIECE) {
            throw new InvalidMoveException("No piece at origin!");
        }
        if (new Piece(originSerial, pieceTypeColor, board).isLegalDestination(destinationSerial) == false) {
            throw new InvalidMoveException("Not legal move!");
        }
    }
    
    /**
     * Sets the evaluation field of the move
     * @param evaluation    The evaluation score of the move
     */
    public void setEvaluation(int evaluation) {
        this.evaluation = evaluation;
    }
    
    /**
     * Gets the evaluation field of the move
     * @return  The evaluation score of the move
     */
    public int getEvaluation() {
        return evaluation;
    }
    
    /**
     * Checks whether a move's origin and destination are the same as another move's.
     * @param move  The move to be compared
     * @return  True if the other move have the same origin and destination, false if not
     */
    public boolean equals(Move move) {
        return (this.originSerial == move.originSerial && this.destinationSerial == move.destinationSerial);
    }
    
    /**
     * The comparator ranking the move by evaluation
     * @param move  The move which to compare
     * @return  A positive integer if the compared move has a higher score than this move;
     * a negative integer if the compared move has a lower score than this move;
     * 0 if the two moves have the same score
     */
    @Override
    public int compareTo(Move move) {
        // null moves come first
        if (move == null) {
            return 1;
        }
        if (move.evaluation > this.evaluation) {
            return 1;
        }
        if (this.evaluation < move.evaluation) {
            return -1;
        }
        return 0;
    }
    
    /**
     * Returns a string representation of the Move
     * @return a string representation of the Move
     */
    @Override
    public String toString()
    {
        String string = Chessboard.getPieceTypeCharacter(pieceTypeColor) + " " + new Square(originSerial).getName() + " ";
        if (isCapture()) {
            string = string + "x" + Chessboard.getPieceTypeCharacter(capturedPieceTypeColor);
        }
        else {
            string = string + "  ";
        }
        string = string + " " + new Square(destinationSerial).getName();
        return string;
    }
    
    /**
     * Represents the move as a PGN movetext
     * @return  A string movetext representing the move
     */
    public String getMovetext() {
        String movetext = "";
        if (!Chessboard.getPieceTypeCharacter(pieceTypeColor).equals("P")) {
            movetext = Chessboard.getPieceTypeCharacter(pieceTypeColor);
        }
        if (isCapture()) {
            if (Chessboard.getPieceTypeCharacter(pieceTypeColor).equals("P")) {
                movetext = Character.toString(Square.getFileChar(Square.getFile(originSerial)));
            }
            movetext = movetext + "x";
     
        }
        movetext = movetext + Square.getName(destinationSerial);
        return movetext;
        
        // todo: provide disambiguation, castling, pawn promotion, check, checkmate symbols
    }
    
}
