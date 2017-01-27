
package com.circumspectus.PGN;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

/**
 * Converts a folder of PGN game text files to a new folder system of game text files sorted by opening moves up to a certain depth.
 * @author Christopher Stieg
 */
public class PGNSort {
    private final PGNGameFeeder gameFeeder;
    private final int DEPTH = 4;
    
    /**
     * Constructor for PGNSort
     */
    public PGNSort() {
        gameFeeder = new PGNGameFeeder("C:\\Users\\user\\Java\\Chess\\pgn_lite");
    }
    
    /**
     * Main method which sorts the games by opening moves
     */
    public void sort() {
        String basePath = new File("").getAbsolutePath() + "\\resources\\sortedgames";    
        PGNGame game = gameFeeder.getNextInFolder();
        final String eol = System.getProperty("line.separator");
        while (game != null) {
            if (game.getMoves().size() < 2) {
                game = gameFeeder.getNextInFolder();
                continue;
            }
            String filePath = basePath + "\\";
            for (int i = 0; i < DEPTH; i++) {
                filePath = filePath + game.getNextMove() + " ";
            }
            filePath = filePath.trim() + ".txt";

            try {
                File file = new File(filePath);
                if (!file.exists()) {
                    file.createNewFile();
                }
                FileWriter fileWriter = new FileWriter(file, true);  // append
                String gameText = game.getText();
                Scanner scanner = new Scanner(gameText);
                while (scanner.hasNextLine()) {
                    fileWriter.write(scanner.nextLine() + eol);                   
                }

                fileWriter.close();
            }
            catch (IOException ioe) {
                System.err.println("Error writing file");
            }

            game = gameFeeder.getNextInFolder();
        }
    }
    
    
    public static void main(String[] args) {
        PGNSort sorter = new PGNSort();
        sorter.sort();
        System.out.println("All finished!");
    }
}