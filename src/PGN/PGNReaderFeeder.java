package com.circumspectus.PGN;

import java.io.BufferedReader;
import java.io.File;

/**
 * Feeds PGN readers (files containing PGN game texts) from a folder
 * @author Christopher Stieg
 */
public class PGNReaderFeeder {
    File dir; 
    File[] directoryListing;
    BufferedReader in;
    int currentIndex;        

    /**
     * Constructor for PGNReaderFeeder
     * @param folderName Folder path containing the PGN game text files
     */
    public PGNReaderFeeder(String folderName) {
        dir = new File(folderName);
        directoryListing = dir.listFiles();
        currentIndex = 0;
    }

    /**
     * Checks whether there is another file yet to read in the folder
     * @return True if there is another file, false if there is not
     */
    public boolean hasNext() {
        return currentIndex < directoryListing.length;
    }
            
    /**
     * Gets the next file in the folder
     * @return  A PGN Reader reading the next file in the folder
     */
    public PGNReader getNext() {
        File file = directoryListing[currentIndex++];
        return new PGNReader(file);
    }

}