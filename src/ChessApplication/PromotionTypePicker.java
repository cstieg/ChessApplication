/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.circumspectus.ChessApplication;

/**
 * Unfinished class to determine the type of piece a pawn becomes when it is promoted to the final rank.
 * @author Christopher Stieg
 */
public class PromotionTypePicker {
    
    /**
     * Gets the type of piece the pawn becomes having reached the final rank.
     * At the present only a queen is allowed.
     * @return  The type of piece the promoted pawn becomes
     */
    public int getPromotionType() {
        return Chessboard.QUEEN;
    }
    
}
