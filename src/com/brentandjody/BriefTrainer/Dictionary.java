package com.brentandjody.BriefTrainer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by brent on 02/17/2014 (adapted from StenoTray && StenoDictionary.
 * implements a reverse-lookup steno dictionary, with strokes sorted by stroke-length
 */
public class Dictionary {

    private static final String[] DICTIONARY_TYPES = {".json"};

    private TST<Queue<String>> mDictionary = new TST<Queue<String>>();

    public Dictionary() {
    }

    public void load(String[] filenames) {
        String line, stroke, english;
        String[] fields;
        boolean simple= (filenames.length<=1);
        TST<String> forwardLookup = new TST<String>();
        for (String filename : filenames) {
            if (validateFilename(filename)) {
                try {
                    File file = new File(filename);
                    FileReader reader = new FileReader(file);
                    BufferedReader lines = new BufferedReader(reader);
                    while ((line = lines.readLine()) != null) {
                        fields = line.split("\"");
                        if ((fields.length > 3) && (fields[3].length() > 0)) {
                            stroke = fields[1];
                            english = fields[3];
                            if (simple) {
                                addToDictionary(stroke, english);
                            } else {
                                forwardLookup.put(stroke, english, false);
                            }
                        }
                    }
                    lines.close();
                    reader.close();
                } catch (IOException e) {
                    System.err.println("com.brentandjody.BriefTrainer.Dictionary File: " + filename + " could not be found");
                }
            }
        }
        if (!simple) {
            // Build reverse lookup
            for (String s : forwardLookup.keys()) {
                english = forwardLookup.get(s, false);
                addToDictionary(s, english);
            }
        }
    }

    public Queue<String> lookup(String english) {
        return mDictionary.get(english, true);
    }

    public Queue<String> possibilities(String partial_word, int limit) {
        if (partial_word.length()<1) return null;
        Queue<String> result = new LinkedList<String>();
        for (String possibility : mDictionary.prefixMatch(partial_word)) {
            result.add(possibility);
            if (result.size() >= limit) break;
        }
        if (result.size() < limit) {
            for (String possibility : mDictionary.prefixMatch("{"+partial_word)) {
                result.add(possibility);
                if (result.size() >= limit) break;
            }
        }
        return result;
    }

    public int size() { return mDictionary.size(); }

    public void unload() {
        mDictionary = null;
        mDictionary = new TST<Queue<String>>();
    }

    private boolean validateFilename(String filename) {
        if (filename.contains(".")) {
            String extension = filename.substring(filename.lastIndexOf("."));
            if (Arrays.asList(DICTIONARY_TYPES).contains(extension)) {
                File file = new File(filename);
                if (!file.exists()) {
                    System.err.println("com.brentandjody.BriefTrainer.Dictionary File: "+filename+" could not be found");
                    return false;
                }
            } else {
                System.err.println(extension + " is not an accepted dictionary format.");
                return false;
            }
        } else {
            System.err.println("com.brentandjody.BriefTrainer.Dictionary file does not have the correct extiension");
            return false;
        }
        return true;
    }

    private void addToDictionary(String stroke, String english) {
        StrokeComparator compareByStrokeLength = new StrokeComparator();
        Queue<String> strokes = mDictionary.get(english, true);
        if (strokes == null)
            strokes = new PriorityQueue<String>(3, compareByStrokeLength);
        strokes.add(stroke);
        mDictionary.put(english, strokes, true);
    }

    private class StrokeComparator implements Comparator<String> {

        @Override
        public int compare(String a, String b) {
            if (a==null || b==null) return 0;
            int aStrokes = countStrokes(a);
            int bStrokes = countStrokes(b);
            //first compare number of strokes
            if (aStrokes < bStrokes) return -1;
            if (aStrokes > bStrokes) return 1;
            //then compare complexity of strokes
            if (a.length() < b.length()) return -1;
            if (a.length() > b.length()) return 1;
            //otherwise consider them equal
            return 0;
        }

        private int countStrokes(String s) {
            return (s.length()-s.replace("/","").length());
        }
    }
}
