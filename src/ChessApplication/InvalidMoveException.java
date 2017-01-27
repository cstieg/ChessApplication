
package com.circumspectus.ChessApplication;

/**
 * Exception thrown when user attempts to make move that is not a valid move
 * @author Christopher Stieg
 */
public class InvalidMoveException extends Exception {
    public InvalidMoveException(String message)  {
        super(message);
    }
    
    public InvalidMoveException(String message, Throwable throwable)  {
        super(message, throwable);
    }
}
