
package com.circumspectus.ChessApplication;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import com.circumspectus.Engine.*;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

/**
 * Runs a chess game in a JFrame.
 * @author Christopher Stieg
 */
public class ChessGame extends JFrame implements Runnable {
    private final ZobristBoard board;  // main chessboard for the game
    private final BoardRenderer boardRenderer;  // renderer to paint chessboard in JPanel
    private final ChessEngine engine;  // chess engine to supply computer's moves
    private final ZobristBoard searchBoard;  // search board to be used by search engine
    //private final BoardRenderer searchBoardRenderer; // rendered to paint search board
    private final SquareClicker squareClicker; // event listener for mouse clicks
    private IdleTimeEngineWorker idleTimeEngineWorker;
    private final static int enginePlayer = Chessboard.BLACK;

    /**
     * Constructor for ChessGame
    */
    public ChessGame() {
        board = new ZobristBoard(new ZobristHashing());
        searchBoard = board.getClone();
        boardRenderer = new BoardRenderer(board);
        //searchBoardRenderer = new BoardRenderer(searchBoard);
        engine = new ChessEngine(board, searchBoard);
        squareClicker = new SquareClicker();
        boardRenderer.addMouseListener(squareClicker);
        boardRenderer.addMouseMotionListener(squareClicker);
        //searchBoardRenderer.addMouseMotionListener(squareClicker);
        //searchBoardRenderer.setVisible(true);
        idleTimeEngineWorker = new IdleTimeEngineWorker(engine);
    }

    /**
     * Opens a window and adds the chessboard
     */
    @Override
    public void run() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 500);
        setLayout(new GridLayout(1, 2));
        add(boardRenderer);
        //add(searchBoardRenderer);
        setVisible(true);
    }

    /**
     * Execute a move from the chess engine and represent it on the board
     */
    public void engineMove() {
        if (board.getCurrentPlayer() == enginePlayer) {            
            Move nextMove = engine.getNextMove();
            board.move(nextMove);
            boardRenderer.addHighlight(Square.toBitwise(nextMove.getOriginSerial()));
            boardRenderer.addHighlight(Square.toBitwise(nextMove.getDestinationSerial()));
            boardRenderer.removeAll();
            boardRenderer.updateUI();
            boardRenderer.repaint();
        }        
    }

    public static void main(String[] args) {
        ChessGame game = new ChessGame();
        SwingUtilities.invokeLater(game);
    }

    /**
     * Helper class to listen for and process mouse input
     */
    private class SquareClicker extends MouseAdapter {
        private Point mouseDownAt;  // point where mouse was clicked
        private Point mouseUpAt;    // point where mouse was released
        
        /**
         * Refreshes the board when mouse is moved
         * @param e MouseEvent object
         */
        @Override
        public void mouseMoved(MouseEvent e) {
            //searchBoardRenderer.repaint();
            boardRenderer.repaint();
        }

        /**
         * Highlights possible moves when a square is clicked
         * @param e MouseEvent object
         */
        @Override
        public void mouseClicked(MouseEvent e) {
            highlightPossibleMovesAt(e.getPoint());
            boardRenderer.repaint();
        }

        /**
         * Record where mouse is pressed for drag/drop operation
         * @param e MouseEvent object
         */
        @Override
        public void mousePressed(MouseEvent e) {
            mouseDownAt = e.getPoint();
            if (canMovePiece(mouseDownAt)) {
                highlightPossibleMovesAt(mouseDownAt);
                boardRenderer.setDragCurrentLocation(mouseDownAt);
                boardRenderer.setDraggingOriginSquare(boardRenderer.getSquareAtPoint(mouseDownAt));
                boardRenderer.repaint();
            }
        }

        /**
         * Complete drag/drop operation when mouse is released, moving piece and starting the engine
         * @param e MouseEvent object
         */
        @Override
        public void mouseReleased(MouseEvent e) {
            mouseUpAt = e.getPoint();
            if (isValidMove()) {
                idleTimeEngineWorker.cancel();
                movePiece();
                checkForCheckmate();
                boardRenderer.setDraggingOriginSquare(null);
                boardRenderer.clearHighlight();
                boardRenderer.revalidate();
                boardRenderer.repaint();
                engineMove();
                checkForCheckmate();
                idleTimeEngineWorker = new IdleTimeEngineWorker(engine);
                idleTimeEngineWorker.execute();
            }
        }

        /**
         * Repaint piece being dragged
         * @param e MouseEvent object
         */
        @Override
        public void mouseDragged(MouseEvent e) {
            boardRenderer.setDragCurrentLocation(e.getPoint());
            boardRenderer.repaint();
        }

        /**
         * Determine whether the piece in the square being clicked can move
         * @param clickAt   The point where the mouse was clicked
         * @return  True if the piece can move, false if it cannot
         */
        private boolean canMovePiece(Point clickAt) {
            Square square = boardRenderer.getSquareAtPoint(clickAt);
            if (square == null) {
                return false;
            }
            long clickedSquareBitwise = square.toBitwise();
            Chessboard board = boardRenderer.getChessboard();
            return (board.getPieceColorAt(clickedSquareBitwise) == board.getCurrentPlayer());
        }

        /**
         * Highlights possible moves of a piece when its square is clicked
         * @param clickAt   The point where the mouse is clicked
         */
        private void highlightPossibleMovesAt(Point clickAt) {
            if (canMovePiece(clickAt)) {
                int clickedSquareSerial = boardRenderer.getSquareAtPoint(clickAt).toSerial();
                Chessboard board = boardRenderer.getChessboard();
                long possibleMoves = Piece.getLegalMoves(board.getPieceTypeColorAt(Square.toBitwise(clickedSquareSerial)), clickedSquareSerial, board);
                boardRenderer.setHighlighted(possibleMoves);
            }
            else {
                boardRenderer.setHighlighted(0);
            }
        }

        /**
         * Determines whether a move is valid
         * @return  True if the move is valid, false if it is not
         */
        private boolean isValidMove() {
            Square originSquare = boardRenderer.getSquareAtPoint(mouseDownAt);
            Square destinationSquare = boardRenderer.getSquareAtPoint(mouseUpAt);
            Chessboard board = boardRenderer.getChessboard();
            if (originSquare == null || destinationSquare == null) {
                return false;
            }
            int originSerial = originSquare.toSerial();
            int destinationSerial = destinationSquare.toSerial();
            long origin = Square.toBitwise(originSerial);
            long destination = Square.toBitwise(destinationSerial);
            int pieceTypeColor = board.getPieceTypeColorAt(origin);
            return (board.getPieceColorAt(origin) == board.getCurrentPlayer() && (destination & Piece.getLegalMoves(pieceTypeColor, originSerial, board)) != 0);
        }
        
        /**
         * Executes move that has been dragged
         */
        private void movePiece() {
            Square originSquare = boardRenderer.getSquareAtPoint(mouseDownAt);
            Square destinationSquare = boardRenderer.getSquareAtPoint(mouseUpAt);
            Chessboard board = boardRenderer.getChessboard();
            int originSerial = originSquare.toSerial();
            int destinationSerial = destinationSquare.toSerial();
            long origin = Square.toBitwise(originSerial);
            long destination = Square.toBitwise(destinationSerial);
            int pieceTypeColor = board.getPieceTypeColorAt(origin);
            int pieceColor = Chessboard.getPieceColor(pieceTypeColor);
            int pieceType = Chessboard.getPieceType(pieceTypeColor);
            if (pieceType == Chessboard.PAWN && (destination & Chessboard.PROMOTION_RANK[pieceColor]) != 0) {
                PromotionTypePicker promotionTypePicker = new PromotionTypePicker();
                if (board.getPieceColorAt(origin) == board.getCurrentPlayer() && (destination & Piece.getLegalMoves(pieceTypeColor, originSerial, board)) != 0) {
                    board.move(new Move(board.getPly(), originSerial, destinationSerial, pieceTypeColor, board, promotionTypePicker.getPromotionType() + pieceColor));
                }
            } else if (board.getPieceColorAt(origin) == board.getCurrentPlayer() && (destination & Piece.getLegalMoves(pieceTypeColor, originSerial, board)) != 0) {
                board.move(new Move(board.getPly(), originSerial, destinationSerial, pieceTypeColor, board));
            } 
        }

        /**
         * Checks for checkmate, and displays a message and exits the game in the case of checkmate.
         */
        private void checkForCheckmate() {
            if (board.isCheckmated(0)) {
                JOptionPane.showMessageDialog(null, "You lose!");
                System.exit(0);
            }
            if (board.isCheckmated(1)) {
                JOptionPane.showMessageDialog(null, "You win!");
                System.exit(0);
            }
        }

    }

}




/**
 * Extends the SwingWorker class to run the engine while waiting for user to move,
 * thus filling up the transposition table with deeper search data and saving
 * time in regular search
 * @author Christopher Stieg
 */
class IdleTimeEngineWorker extends SwingWorker<Move, Move> {
    private final ChessEngine engine;
    
    /**
     * Constructor for IdleTimeEngineWorker
     * @param engine    Engine to utilize for search
     */
    public IdleTimeEngineWorker(ChessEngine engine) {
        this.engine = engine;
    }
    
    /**
     * Runs the engine in background
     * @return  The best move from the search (though this value is not used)
     */
    @Override
    public Move doInBackground() {
        while (engine.isRunning()) {
            System.out.println("Waiting for engine to return. (Idle time search)");
        }
        System.out.println("Thread started");
        return engine.getNextMove(10 * 60 * 1000);
    }

    /**
     * Cancels the engine running in background
     */
    public void cancel() {
        System.out.println("Thread interrupted");
        engine.interrupt();
        while (engine.isRunning()) {
            System.out.println("Waiting for engine to return. (Main search)");
        }
    }

}