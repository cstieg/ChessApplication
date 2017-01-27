/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.circumspectus.PGN;

/**
 * Tuple class used to store the number of wins, losses, or draws resulting from a particular move in a database of games
 * @author Christopher Stieg
 */
public class MoveResults {
    public String movetext;
    public int wins;
    public int losses;
    public int draws;
    
    /**
     * Constructor for MoveResults
     * @param movetext  The move in PGN form
     * @param wins      Number of wins resulting from this move
     * @param losses    Number of losses resulting from this move
     * @param draws     Number of draws resulting from this move
     */
    public MoveResults (String movetext, int wins, int losses, int draws) {
        this.movetext = movetext;
        this.wins = wins;
        this.losses = losses;
        this.draws = draws;
    }
}