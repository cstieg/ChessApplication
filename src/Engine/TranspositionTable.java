
package com.circumspectus.Engine;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Stores the search results for a given board position, which can be reused
 * for the same position derived from different moves (or the same moves in different order).
 * Also stores the best move, which aids in move ordering at deeper levels of the iterative deepening framework,
 * causing more cutoffs and speeding up search.
 * @author Christopher Stieg
 */
public class TranspositionTable {
    private final HashMap<Long, TranspositionFields> hashMap;
    private int serial;
    private static final int MAX_ENTRIES = 20000000;
    private static final int NO_OF_QUANTILES = 9;
    private static final int CLEAR_QUANTILES = 7; // must be more than search depth, less than NO_OF_QUANTILES
    private static final int LEVELS_TO_SAVE = 2; // save the deepest searches
    private final int quantiles[];

    public TranspositionTable() {
        hashMap = new HashMap<>(MAX_ENTRIES + 1, 1);
        serial = 0;
        quantiles = new int[NO_OF_QUANTILES];
        for (int i = 0; i < NO_OF_QUANTILES; i++) {
            quantiles[i] = i * MAX_ENTRIES / NO_OF_QUANTILES;
        }
    }

    public TranspositionFields get(long hashValue) {
        TranspositionFields returnValue = hashMap.get(hashValue);
        if (returnValue != null) {
            returnValue.serial = serial++;
        }
        return returnValue;
    }

    public void put(long hashValue, TranspositionFields transpositionFields) {
        serial++;
        hashMap.put(hashValue, transpositionFields);
        if (hashMap.size() >= MAX_ENTRIES) {
            clearOldEntries();
        }
    }

    public int getSerial() {
        return serial;
    }

    public int getSize() {
        return hashMap.size();
    }

    private void clearOldEntries() {
        System.out.println("Clearing Old Entries ************************************************************************************");
        Iterator<Map.Entry<Long, TranspositionFields>> it = hashMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, TranspositionFields> pair = it.next();
            // remove most distant entries, more             
            int boardDepth = pair.getValue().depth;
            int boardSerial = pair.getValue().serial;
            if (boardSerial < quantiles[CLEAR_QUANTILES] && boardDepth < ChessEngine.DEPTH - LEVELS_TO_SAVE) {
                it.remove();
            }
        }
        for (int i = 0; i < NO_OF_QUANTILES - 1; i++) {
            quantiles[i] = quantiles[i + 1];
        }
        quantiles[NO_OF_QUANTILES - 1] = serial;
    }

}
