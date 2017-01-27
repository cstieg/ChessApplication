/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.circumspectus.ChessApplication;

/**
 * Thrown when movetext contains a reference to an invalid square code
 * @author Christopher
 */
public class InvalidSquareException extends InvalidMoveException {
    public InvalidSquareException(String message) {
        super(message);
    }
    
    public InvalidSquareException(String message, Throwable throwable) {
        super(message, throwable);
    }
}