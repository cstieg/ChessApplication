
package com.circumspectus.ChessApplication;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

/**
 * Renders Chessboard on a Graphics object.
 * @author Christopher Stieg
 */
public class BoardRenderer extends JPanel {
    private Chessboard board;
    private Point topLeftCorner;
    private int squareWidth = 45;
    private long highlighted = 0L;
    private Square draggingOriginSquare = null; // square of a piece being dragged by the mouse
    private Point dragCurrentLocation = null; // current location of a piece being dragged by the mouse

    private final BufferedImage[] pieceImages = new BufferedImage[12]; // array of images for various chess pieces, numbered by piece type and color (see enumeration in Chessboard.java)
    private final static Color whiteSquareColor = Color.white;
    private final static Color blackSquareColor = Color.black;
    private final static Color highlightedWhiteSquareColor = Color.yellow;
    private final static Color highlightedBlackSquareColor = new Color(102, 102, 0); // dark yellow

    /**
     * Constructor for RenderBoard
     * @param board     The Chessboard object to be rendered
     */
    public BoardRenderer(Chessboard board) {
        super();
        setBackground(Color.WHITE);
        this.board = board;
        this.topLeftCorner = new Point(10, 10);
        getImages();
    }
    
    /**
     * Does the painting, called by JPanel graphics repainting
     * @param g     Graphics object from repaint
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);            // call superclass to make panel display correctly
        draw(g);
    }   
       
    /**
     * Draws the chessboard and pieces on it on a Graphics object.
     * @param g     Graphics object passed in by JPanel
     */
    public void draw(Graphics g) {
        drawSquares(g);
        drawAllPieces(g);
        if (draggingOriginSquare != null) {
            // clear square from which the piece is dragged
            drawSquare(g, draggingOriginSquare, false);
            // draw dragging piece at its current location
            dragPiece(g, board.getPieceTypeColorAt(draggingOriginSquare.toBitwise()), dragCurrentLocation);
        }
        drawBoard(g);
    }
    
    /**
     * Draws the border of the chessboard, along with letters and numbers of the grid
     * @param g a Graphics object passed in by the PaintComponent method of JPanel on which to draw the board
     */  
    public void drawBoard(Graphics g) {
        int widthOfBoard = squareWidth * 8;
        g.setColor(Color.black);
        g.drawRect(topLeftCorner.x, topLeftCorner.y, widthOfBoard, widthOfBoard);
        // draw letters and numbers of grid
        for (int i = 0; i < 8; i++) {
            String letter = String.valueOf(Square.getFileChar(i));
            String number = String.valueOf(Square.getRankChar((7-i)));
            g.drawString(letter, topLeftCorner.x + (i * squareWidth) + (int)(.4 * squareWidth), topLeftCorner.y + widthOfBoard + (int)(.4 * squareWidth));
            g.drawString(number, topLeftCorner.x + widthOfBoard + (int)(.1 * squareWidth), topLeftCorner.y + (i * squareWidth) + (int)(.6 * squareWidth));
        }
    }
    
    /**
     * Cycle through Square objects in squares array and draw each of them
     * @param g a Graphics object passed in by the PaintComponent method of JPanel on which to draw the board
     */    
    public void drawSquares(Graphics g) {
        for (int file = 0; file < 8; file++) {
            for (int rank = 0; rank < 8; rank++) {
                Square square = new Square(file, rank);
                drawSquare(g, square, square.isOccupied(highlighted));
            }
        }
    }
    
    /**
     * Draws a given square on the canvass to the correct color
     * @param g             a Graphics object passed in by the PaintComponent method of JPanel on which to draw the board
     * @param square        the Square object to draw
     * @param highlighted   whether the square should be highlighted
     */
    public void drawSquare(Graphics g, Square square, boolean highlighted) {
        Color squareColor; 
        // set color
        if (square.isBlack()) {
            if (highlighted) {
                squareColor = highlightedBlackSquareColor;
            }
            else {
                squareColor = blackSquareColor;
            }
        }
        else  {
            if (highlighted) {
                squareColor = highlightedWhiteSquareColor;
            }
            else {
                squareColor = whiteSquareColor;
            }
        }
        g.setColor(squareColor);
        int rankReversed = 7 - square.getRank(); // to display first rank at bottom rather than top
        int squareX = topLeftCorner.x + square.getFile() * squareWidth;
        int squareY = topLeftCorner.y + rankReversed * squareWidth;
        g.fillRect(squareX, squareY, squareWidth, squareWidth);
    }
    
    /**
     * Draws the pieces on the board.
     * @param g a Graphics object passed in by the PaintComponent method of JPanel on which to draw the board
     * @param bitboard  Bitboard containing pieces of a certain type
     * @param pieceImage    BufferedImage containing the image to draw (white bishop, black pawn, etc.)
     */
    public void drawPieces(Graphics g, long bitboard, BufferedImage pieceImage) {
        long bitboardOfRemainingPieces = bitboard;
        while (bitboardOfRemainingPieces != 0L) {
            Square square = new Square(bitboardOfRemainingPieces);

            // find position and draw image
            int rankReversed = 7 - square.getRank(); // to display first rank at bottom rather than top
            int squareX = topLeftCorner.x + square.getFile() * squareWidth;
            int squareY = topLeftCorner.y + rankReversed * squareWidth;
            g.drawImage(pieceImage, squareX, squareY, squareWidth, squareWidth, null);

            // remove drawn piece from bitboard, so that next piece, if any, can be found
            bitboardOfRemainingPieces -= square.toBitwise();
        }
    }
    
    /**
     * Draws all types of pieces on the board
     * @param g a Graphics object passed in by the PaintComponent method of JPanel on which to draw the board
     */
    public void drawAllPieces(Graphics g) {
        for (int i = 0; i < 12; i++) {
            drawPieces(g, board.getPieceSet(i), pieceImages[i]);
        }
    }
    
    /**
     * Draws a piece being dragged in the correct location on the canvass
     * @param g                 a Graphics object passed in by the PaintComponent method of JPanel on which to draw the board
     * @param pieceTypeColor    the type and color of the piece being dragged (see Chessboard for enumeration)
     * @param position          the current location of the piece being dragged
     */
    public void dragPiece(Graphics g, int pieceTypeColor, Point position) {
        if (pieceTypeColor >= 0 && position != null && position.x > 0 && position.y > 0 && position.x < squareWidth * 8 && position.y < squareWidth * 8) {
            g.drawImage(pieceImages[pieceTypeColor], position.x - (squareWidth / 2), position.y - (squareWidth / 2), squareWidth, squareWidth, null);
        }
    }
    
    /**
     * Sets the origin Square of the piece to be dragged
     * @param draggingOriginSquare origin square of the piece to be dragged
     */
    public void setDraggingOriginSquare(Square draggingOriginSquare) {
        this.draggingOriginSquare = draggingOriginSquare;
    }
    
    /**
     * Gets the origin square of the piece to be dragged
     * @param dragCurrentLocation origin square of the piece to be dragged
     */
    public void setDragCurrentLocation(Point dragCurrentLocation) {
        this.dragCurrentLocation = dragCurrentLocation;
    }
    
    /**
     * Gets the Chessboard being rendered
     * @return Chessboard being rendered
     */
    public Chessboard getChessboard() {
        return board;
    }
    
    /**
     * Sets the Chessboard being rendered
     * @param board Chessboard being rendered
     */
    public void setChessboard(Chessboard board) {
        this.board = board;
    }
    
    /**
     * Adds a bit(s) to the highlight bitwise mask
     * @param highlight 64 bit binary mask containing 1s in the squares to be highlighted
     */
    public void addHighlight(long highlight) {
        this.highlighted |= highlight;
    }
    
    /**
     * Removes a bit(s) from the highlight bitwise mask by flipping the unhighlight mask and then taking the intersection of the negative with the current mask
     * @param unHighlight 64 bit binary mask containing 1s in the square to be unhighlighted
     */
    public void removeHighlight(long unHighlight) {
        this.highlighted &= ~unHighlight;
    }
    
    /**
     * Clears highlight from all squares
     */
    public void clearHighlight() {
        this.highlighted = 0;
    }
    
    /**
     * Gets the highlight status for the board
     * @return  a bitboard containing 1s for the squares to be highlighted
     */
    public long getHighlighted() {
        return highlighted;
    }
    
    /**
     * Sets the highlight status for the board
     * @param highlighted a bitboard containing 1s for the squares to be highlighted
     */
    public void setHighlighted(long highlighted) {
        this.highlighted = highlighted;
    }
    
    /**
     * Sets the location of the board on the canvas.
     * @param topLeft   A Pixel object containing the coordinates for the top left corner of the board.
     */
    public void setTopLeft(Point topLeft) {
        this.topLeftCorner = topLeft;
    }
    
    /**
     * Returns the location of the board on the canvas.
     * @return  A Pixel object containing the coordinates for the top left corner of the board.
     */
    public Point getTopLeft() {
        return topLeftCorner;
    }
    
    /**
     * Sets the square width of the board
     * @param squareWidth Square width in pixels
     */
    public void setSquareWidth(int squareWidth) {
        this.squareWidth = squareWidth;
    }
    
    /**
     * Gets the square width of the board
     * @return Square width in pixels
     */
    public int getSquareWidth() {
        return squareWidth;
    }
    
    /**
     * Gets the square at a given point
     * @param point the point on the canvass
     * @return      the Square at the given point, null if not within the chessboard
     */
    public Square getSquareAtPoint(Point point) {
        int x = point.x - topLeftCorner.x;
        int y = point.y - topLeftCorner.y;
        if (x < 0 || y < 0 || x > squareWidth * 8 || y > squareWidth * 8) {
            return null;
        }
        return new Square(x / squareWidth, 7 - (y / squareWidth));
    }
    
     /**
     * Initializes image variables with images for each of the chess pieces
     */   
    private void getImages() {
        try  {
            pieceImages[0] = ImageIO.read(new File("resources/King-white.png"));
            pieceImages[1] = ImageIO.read(new File("resources/King-black.png"));
            pieceImages[2] = ImageIO.read(new File("resources/Queen-white.png"));
            pieceImages[3] = ImageIO.read(new File("resources/Queen-black.png"));
            pieceImages[4] = ImageIO.read(new File("resources/Rook-white.png"));
            pieceImages[5] = ImageIO.read(new File("resources/Rook-black.png"));
            pieceImages[6] = ImageIO.read(new File("resources/Bishop-white.png"));
            pieceImages[7] = ImageIO.read(new File("resources/Bishop-black.png"));
            pieceImages[8] = ImageIO.read(new File("resources/Knight-white.png"));
            pieceImages[9] = ImageIO.read(new File("resources/Knight-black.png"));
            pieceImages[10] = ImageIO.read(new File("resources/Pawn-white.png"));
            pieceImages[11] = ImageIO.read(new File("resources/Pawn-black.png"));
        } 
        catch (IOException e) {
            System.out.printf("Missing image file");
        }
    }
}