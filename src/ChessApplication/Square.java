
package com.circumspectus.ChessApplication;

import static com.circumspectus.ChessApplication.Chessboard.ASSERTION_CHECKS;

/**
 * Holds a grid coordinate corresponding to a square on a chessboard.
 * @author Christopher Stieg
 */
public class Square {
    private int file; // 0-7 corresponding to a-h on the chessboard
    private int rank; // 0-7 corresponding to 1-8 on the chessboard
   
    /**
     * Constructor for square accepting integer coordinates
     * @param file  File number 0-7 corresponding to a-h on the chessboard
     * @param rank  Rank number 0-7 corresponding to 1-8 on the chessboard
     */
    public Square(int file, int rank) {
        if (ASSERTION_CHECKS) {
            assert (file >= 0 && file < 8);
            assert (rank >= 0 && rank < 8);
        }
        this.file = file;
        this.rank = rank;
    }
    
    /**
     * Constructor for square accepting long bitwise containing 1 in the position representing the square
     * @param position  Long bitwise containing 1 in the position representing the square
     */
    public Square(long position) {
        if (ASSERTION_CHECKS) {
            assert (position > 0);
        }       
        this.file = getFile(position);
        this.rank = getRank(position);
    }
    
    /**
     * Constructor for square accepting int serial representing the square
     * @param serial    Int serial representing the square
     */
    public Square(int serial) {
        if (ASSERTION_CHECKS) {
            assert (serial >= 0 && serial < 64);
        }
        this.file = getFile(serial);
        this.rank = getRank(serial);
    }
    
    /**
     * Constructor for Square accepting movetext square name (user input)
     * @param squareName    Movetext square name consisting of one lowercase letter (a-h) and one number (1-8) inclusive
     * @throws InvalidSquareException   Square movetext does not meet these parameters
     */
    public Square(String squareName) throws InvalidSquareException {
        if (squareName.length() != 2 ||
            squareName.charAt(0) < 'a' || squareName.charAt(0) > 'h' ||
            squareName.charAt(1) < '1' || squareName.charAt(1) > '8') {
            throw new InvalidSquareException("Valid square name must consist of one lowercase letter between a and h and one number between 1 and 8 inclusive!");
        }
        this.file = fileCharToInt(squareName.charAt(0));
        this.rank = rankCharToInt(squareName.charAt(1));
    }
    
    /**
     * Gets file of the square
     * @return  File of the square (0 = a, 7 = h)
     */
    public int getFile() {
        return file;
    }
    
    /**
     * Gets file of the square
     * @param bitwise   64 bit bitfield containing a 1 in the location representing the square
     * @return  File of the square (0 = a, 7 = h)
     */
    public static int getFile(long bitwise) {
        if (ASSERTION_CHECKS) {
            assert (bitwise != 0);
        }
        return toSerial(bitwise) % 8; 
    }
    
    /**
     * Gets file of the square
     * @param serial    Int serial representing the square
     * @return  File of the square (0 = a, 7 = h)
     */
    public static int getFile(int serial) {
        if (ASSERTION_CHECKS) {
            assert (serial >= 0 && serial < 64);
        }
        return serial % 8;      
    }
    /**
     * Gets the rank of the square
     * @return  Rank of the square (0 = rank 1, 7 = rank 8)
     */
    public int getRank() {
        return rank;
    }
    
    /**
     * Gets the rank of a bitwise square
     * @param bitwise   64 bit bitfield containing a 1 in the location representing the square
     * @return  Rank of the square (0 = rank 1, 7 = rank 8)
     */
    public static int getRank(long bitwise) {
        if (ASSERTION_CHECKS) {
            assert (bitwise != 0);
        }
        return toSerial(bitwise) / 8;  
    }
    
     /**
     * Gets the rank of a serial square
     * @param serial    Serial int representing the square
     * @return  Rank of the square (0 = rank 1, 7 = rank 8)
     */
    public static int getRank(int serial) {
        if (ASSERTION_CHECKS) {
            assert (serial >= 0 && serial < 64);
        }
        return serial / 8;     
    }
    
     /**
     * Gets the file number in char format 
     * @return  File number (a-h) L to R
     */
    public char getFileChar() {
        return getFileChar(this.file);
    }
    
    /**
     * Converts a given file number to char
     * @param file  File number (a-h)
     * @return  File number (0-7)
     */
    public static char getFileChar(int file) {
        if (ASSERTION_CHECKS) {
            assert (file >= 0 && file < 8);
        }
        return (char)(file + 97);
    }
    
    /**
     * Gets the rank number in char format
     * @return  Rank number (1-8) bottom to top
     */
    public char getRankChar() {
        return getRankChar(this.rank);
    }
    
    /**
     * Converts a given rank number to char
     * @param rank  Rank number (0-7)
     * @return  Rank number (1-8)
     */
    public static char getRankChar(int rank) {
        if (ASSERTION_CHECKS) {
            assert (rank >= 0 && rank < 8);
        }
        return (char) (rank + 49);
    }
    
    /**
     * Converts a char file letter to int (0-7)
     * @param file  char representation of file letter (a-h)
     * @return  int representation of file letter (0-7)
     */
    public static int fileCharToInt(char file) {
        if (ASSERTION_CHECKS) {
            assert (file >= 'a' && file <= 'h');
        }
        return (int)(file - 97);
    }
    
    /**
     * Converts a char rank number to int (0-7)
     * @param rank  char representation of rank number (1-8)
     * @return  int representation of rank number (0-7)
     */
    public static int rankCharToInt(char rank) {
        if (ASSERTION_CHECKS) {
            assert (rank >= '1' && rank <= '8');
        }
        return (int)(rank - 49);
    }
    
    /**
     * Gets the name of the square in algebraic notation
     * @return  The name of the square (ex. a1)
     */
    public String getName() {
        return Character.toString(getFileChar()) + Character.toString(getRankChar());
    }
    
    public static String getName(int squareSerial) {
        return Character.toString(getFileChar(getFile(squareSerial))) + Character.toString(getRankChar(getRank(squareSerial)));
    }

    /**
     * Returns a string representation of the Square
     * @return The name of the square (ex. a1)
     */
    @Override
    public String toString()  {
        return getName();
    }   
    
    /**
     * Determines color of a given file and rank on the chessboard
     * @return      true for black, false for white
     */
    boolean isBlack() {   
        return (rank + file) % 2 == 0;
    }

    /**
     * Checks whether this square is the same rank and file of another square object
     * @param square    The square object which to compare
     * @return          True if two squares have the same rank, false if they do not
     */
    public boolean equals(Square square) {
        return square.getFile() == this.file && square.getRank() == this.rank;
    }
    
    /**
     * Returns a bitfield representation of the Square
     * @return  64 bit bitfield representation of this Square
     */
    public long toBitwise() {
        return toBitwise(this);
    }
    
    /**
     * Returns a bitfield representation of the Square
     * @param serial    Serial int representing the square
     * @return  64 bit bitfield representation of this Square
     */
    public static long toBitwise(int serial) {
        return 1L << serial;
    }
    
    /**
     * Converts a Square (file, rank) to bitfield
     * @param square    Square containing a file and a rank
     * @return          64 bit bitfield
     */
    public static long toBitwise (Square square) {
        if (ASSERTION_CHECKS) {
            assert (square != null);
        }
        return (long)(1L << Square.toSerial(square)); 
    }
    
    /**
     * Converts a Square (file, rank) to a serial int
     * @param square    Square containing a file and rank
     * @return  Serial int representing the square
     */
    public static int toSerial(Square square) {
        if (ASSERTION_CHECKS) {
            assert (square != null);
        }
        return square.getFile() + square.getRank() * 8;
    }
      
    /**
     * Converts this Square to a serial int
     * @return  Serial int representing the square
     */
    public int toSerial() {
        return Square.toSerial(this);
    }
        
    /**
     * Extracts the (last/highest) Square from a bitfield
     * @param bitwise   64 bit bitfield
     * @return          Square serial from 0 to 63 (a1 - h8).  If bitfield is empty, returns -1.
     */
    public static int toSerial(long bitwise) {
        if (bitwise == 0) {
            return Chessboard.NO_PIECE;
        }
        return Long.numberOfTrailingZeros(bitwise);
    }

    /**
     * Checks whether the coordinates in a square are occupied in a binary bitboard.
     * @param square    A Square containing coordinates of the chessboard
     * @param bitboard  A binary bitboard to be checked as to whether the Square is occupied
     * @return True if the square is occupied, false if it is not
     */
    public static boolean isOccupied(Square square, long bitboard) {
        return ((toBitwise(square) & bitboard) != 0);
    }
    
    /**
     * Checks whether the square is occupied in a given binary bitboard.
     * @param bitboard  A binary bitboard to be checked as to whether the Square is occupied.
     * @return  True if the square is occupied, false if it is not
     */
    public boolean isOccupied(long bitboard) {
        return isOccupied(this, bitboard);
    }
    
    /**
     * Checks whether a square is attacked by a piece of the opposite color of a given type.  It assumes square contains a piece of the same kind, 
     * but opposite color of the potential attacking piece.  Because the attacks of two pieces of the same kind but opposite color are always mutual.
     * @param squareSerial  Serial int representing the square to be checked
     * @param board         Reference to parent Chessboard
     * @param pieceTypeColor    Sum of piece type and color enumerations representing the type and color of piece(s) that may potentially be attacking
     * @return  True if square is attacked by a piece of the specified type and color, false otherwise
     */
    public static boolean isAttackedByPieceTypeColor(int squareSerial, Chessboard board, int pieceTypeColor) {
        int attackedSquareTypeColor = Chessboard.getPieceType(pieceTypeColor) + 1 - Chessboard.getPieceColor(pieceTypeColor);
        return ((Piece.getPossibleMovesExcludingCastle(attackedSquareTypeColor, squareSerial, board) & board.getPieceSet(pieceTypeColor)) != 0);
    }
    
    /**
     * Checks whether a square is being attacked by the opposite color.
     * @param squareSerial  Serial int representing the square to be checked
     * @param board         Reference to parent Chessboard
     * @param pieceColor    Color of player potentially being attacked
     * @return  True if square is attacked
     */
    public static boolean isAttacked(int squareSerial, Chessboard board, int pieceColor) {
        for (int pieceType = Chessboard.KING; pieceType <= Chessboard.PAWN; pieceType += 2) {
            if (isAttackedByPieceTypeColor(squareSerial, board, pieceType + 1 - pieceColor)) {
                return true;
            }
        }
        return false;
    }
    
   }