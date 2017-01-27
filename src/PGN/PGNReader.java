
package com.circumspectus.PGN;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
/**
 * Reader to read PGN game text file
 * @author Christopher Stieg
 */
public class PGNReader {
    String PGNtext;
    BufferedReader in;
    File file;
    
    /**
     * Constructor for PGNReader taking a File object
     * @param file  File object containing a reference to the PGN game text to be read
     */
    public PGNReader(File file) {
        this.file = file;
        setBufferedReader(file);
    }
    
    /**
     * Constructor for PGNReader allowing the user to select a file from a file chooser
     */
    public PGNReader() {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("PGN files", "pgn");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            file = chooser.getSelectedFile();
            setBufferedReader(file);
        }
    }

    /**
     * Sets the BufferedReader to read a File object
     * @param file  The File object to be read
     */
    private void setBufferedReader(File file) {
        try {
            in = new BufferedReader(new FileReader(file));
        }
        catch (FileNotFoundException fnfe) {
            System.err.println("Can't find file");
        }
    }

    /**
     * Finds and returns the next game in the game text file
     * @return  A String containing a single PGN game text
     */
    public String getNextGame() {
        PGNtext = "";
        String lineText = "";
        while (lineText != null) {
            try {
                lineText = in.readLine();
            }
            catch (IOException ioe) {
                System.out.println("Error reading PGN");
                return "";
            }
            if (lineText == null) {
                return PGNtext;
            }
            PGNtext = PGNtext + lineText + " \r\n"; // final space to prevent running lines together
            lineText = lineText.trim();
            if (lineText.endsWith("1-0") || lineText.endsWith("0-1") || lineText.endsWith("1/2-1/2") || lineText.endsWith("*")) {
                return PGNtext;
            }
        }
        return "";
    }
    
    /**
     * Resets the reader to the beginning of the file by reloading the file into the BufferedReader
     */
    public void reset() {
        try {
            in = new BufferedReader(new FileReader(file));               
        }
        catch (FileNotFoundException fnf) {
            System.out.println("Can't find file!");
        } 
    }
    
    public static void main(String[] args)
    {
        System.out.println("Hello");
        BufferedReader in = null;
        File file = null;
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("PGN files", "pgn");
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION)
        {
            file = chooser.getSelectedFile();
            PGNReader PGNtext = new PGNReader(file);
            PGNGame game = new PGNGame(PGNtext.getNextGame());
            game = new PGNGame(PGNtext.getNextGame());
            System.out.println(game.toString());
        }
    }
}