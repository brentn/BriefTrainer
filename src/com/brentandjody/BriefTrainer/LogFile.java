package com.brentandjody.BriefTrainer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by brentn on 17/02/14.
 * Read and parse the log file
 */
public class LogFile {

    private Reader fileReader = null;
    private BufferedReader input = null;

    Pattern strokePattern = Pattern.compile("([STPHKWRAO\\*\\#\\-EUFLDBGZ]+)");

    private int strokes=0;
    private String translation="";

    public LogFile(String filename) {
        try {
            fileReader = new FileReader(filename);
            input = new BufferedReader(fileReader);
        } catch (IOException e) {
            System.err.println("Log file ("+filename+" not found.");
        }
    }

    public boolean isReady() {
        if (input==null) return false;
        try {
            return input.ready();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return false;
    }

    public void read() {
        strokes=0;
        translation="";
        if (input==null) return;
        try {
            String line;
            if (input.ready()) {
                if ((line = input.readLine()) != null) {
                    parseLog(line);
                    return;
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public String getTranslation() {return translation;}

    public int getStrokes() {return strokes;}

    public void close() {
        try {
            input.close();
            fileReader.close();
        } catch(Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private void parseLog(String line) {
        if (line==null || !line.contains("Translation(")) return;
        String[] parts;
        if (line.contains("Translation(")) {
            boolean undo = (line.contains("*Translation("));
            line = line.substring(line.indexOf("Translation(")+12,line.length()-1);
            parts = line.split(":", 2);
            Matcher matcher = strokePattern.matcher(parts[0]);
            strokes=0;
            while (matcher.find()) strokes++;
            if (undo) strokes = -strokes;
            translation = parts[1].trim();
        }
    }

}
