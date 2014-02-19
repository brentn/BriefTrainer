package com.brentandjody.BriefTrainer;

import java.io.*;
import java.util.*;

/**
 * Created by brentn on 17/02/14.
 *
 */
public class BriefTrainer {

    private static boolean DEBUG = true;
    private static List<Candidate> candidates = new ArrayList<Candidate>();
    private static List<Recommendation> recommendations = new ArrayList<Recommendation>();

    // find the default location of the plover config directory
    private static final String PSEP       = System.getProperty("file.separator");
    private static final String UHOME      = System.getProperty("user.home");
    private static final String PLOVER_DIR;
    static {
        String[] innerDirsWin = {"AppData", "Local", "plover", "plover"};
        String[] innerDirsOther = {".config", "plover"};
        String[] innerDirs;
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Windows")) {
            PLOVER_DIR = mkPath(UHOME, "AppData", "Local", "plover", "plover");
        } else if (osName.startsWith("Mac")) {
            PLOVER_DIR = mkPath(UHOME, "Library", "Application Support", "plover");
        } else {
            PLOVER_DIR = mkPath(UHOME, ".config", "plover");
        }
    }

    private static final String CONFIG_DIR = mkPath(PLOVER_DIR, "brieftrainer.cfg");

    // global variables
    private static List<String> dictionaryFiles = new ArrayList<String>();
    private static String logFile = mkPath(PLOVER_DIR, "plover.log");
    private static Dictionary dictionary; // the main dictionary

    private static String mkPath(String path1, String... paths)
    {
        StringBuilder sb = new StringBuilder(path1);
        for (String p : paths)
        {
            sb.append(PSEP + p);
        }
        return sb.toString();
    }

    private static void readConfig() throws java.io.FileNotFoundException {
        String ploverConfig = mkPath(PLOVER_DIR, "plover.cfg");
        String line;
        String[] fields;
        if (new File(CONFIG_DIR).isFile()) {
            if (DEBUG) System.out.println("Loading config file ("+CONFIG_DIR+")...");
            try {
                BufferedReader stConfig = new BufferedReader(new FileReader(CONFIG_DIR));
                while ((line = stConfig.readLine()) != null) {
                    if (line.contains("=")) {
                        fields = line.split("=");
                        if (fields[0].trim().equals("PLOVER_CONFIG"))
                            ploverConfig = fields[1].trim();
                        else if (fields[0].trim().equals("DEBUG"))
                            DEBUG = (fields[1].trim().equals("true"));
                    }
                }
                stConfig.close();
            } catch (IOException e) {
                System.err.println("Error reading config file: "+CONFIG_DIR);
            }
        }
        if (new File(ploverConfig).isFile()) {
            if (DEBUG) System.out.println("reading Plover config ("+ploverConfig+")...");
            try {
                BufferedReader pConfig = new BufferedReader(new FileReader(ploverConfig));
                while (((line = pConfig.readLine()) != null)) {
                    fields = line.split("=");
                    if (fields.length >= 2) {
                        if (fields[0].trim().length() > 15)
                            if (fields[0].trim().substring(0,15).equals("dictionary_file"))
                                dictionaryFiles.add(fields[1].trim());
                        if (fields[0].trim().equals("log_file"))
                            logFile = fields[1].trim();
                    }
                }
                pConfig.close();
            } catch (IOException e) {
                System.err.println("Error reading Plover configuration file");
            }
            if (dictionaryFiles == null)
                throw new java.lang.IllegalArgumentException("Unable to locate Plover dictionary file(s)");
            if (logFile == null)
                throw new java.lang.IllegalArgumentException("Unable to locate Plover Log file");
        } else {
            throw new java.io.FileNotFoundException("Cannot locate plover config file: " + ploverConfig.toString());
        }
    }

    public static void main(String[] arguments) {
        dictionary = new Dictionary();
        try {
            readConfig();
            dictionary.load(dictionaryFiles.toArray(new String[dictionaryFiles.size()]));
            assert(dictionary.size()>0);
        } catch (FileNotFoundException e) {
            System.err.println("Error: could not locate Plover config file.");
        }
        LogFile log = new LogFile(logFile);
        while (log.isReady()) {
            log.read();
            if (log.getStrokes()!=0) {
                process(log.getStrokes(), log.getTranslation());
            }
        }
        log.close();
        Collections.sort(recommendations);
        for (Recommendation r : recommendations) {
            System.out.println(r.improvement()+" -> "+r.stroke()+" : "+r.translation());
        }
    }

    private static void process(int strokes, String translation) {
        //add or remove strokes from candidates
        //System.out.println(candidates.size()+" : "+strokes+" : "+translation);
        Candidate candidate;
        for (Iterator<Candidate> iterator = candidates.iterator(); iterator.hasNext();) {
            candidate = iterator.next();
            candidate.addWord(strokes, translation);
            Queue<String> lookup = null;
            if (!candidate.translation().isEmpty())
                lookup = dictionary.lookup(candidate.translation());
            if (lookup==null || lookup.isEmpty()) {
                iterator.remove();
            } else {
                if (numberOfStrokes(lookup.peek()) < candidate.strokes()) {
                    Recommendation recommendation = findRecommendation(lookup.peek());
                    if (recommendation == null) {
                        int savings = candidate.strokes()-numberOfStrokes(lookup.peek());
                        recommendations.add(new Recommendation(1, savings, lookup.peek(), candidate.translation));
                    } else {
                        recommendation.addOccurrence();
                    }
                }
            }
        }
        candidates.add(new Candidate(strokes, translation));
    }

    private static int numberOfStrokes(String seriesOfStrokes) {
        return seriesOfStrokes.split("/").length;
    }

    private static Recommendation findRecommendation(String stroke) {
        for (Recommendation r : recommendations) {
            if (r.equals(stroke)) return r;
        }
        return null;
    }

    static class Candidate {
        private int strokes=0;
        private String translation="";

        public Candidate(int strokes, String translation) {
            this.strokes = strokes;
            this.translation = translation;
        }

        public int strokes() {return strokes;}
        public String translation() {return translation;}

        public void addWord(int strokes, String word) {
            this.strokes+= strokes;
            if (strokes < 1) {
                int end = this.translation.lastIndexOf(word);
                if (end >= 0)
                    this.translation = this.translation.substring(0, end).trim();
            } else {
                if (!this.translation.isEmpty())
                    this.translation+=" ";
                this.translation+=word;
            }
        }
    }

    static class Recommendation implements Comparable<Recommendation>{
        private int occurrences=0;
        private int savings=0;
        private String stroke="";
        private String translation="";

        public Recommendation(int occurrences, int savings, String stroke, String translation) {
            this.occurrences = occurrences;
            this.savings = savings;
            this.stroke = stroke;
            this.translation = translation;
        }

        public boolean equals(String stroke) {
            if (stroke == null) return false;
            return (this.stroke().equals(stroke));
        }

        public void addOccurrence() {
            this.occurrences++;
        }

        public String improvement() {
            String s = " stroke";
            if (savings > 1) s=" strokes";
            return "This brief will save "+savings+s+", and could have been used "+occurrences+" times.";}
        public int score() {return occurrences * savings;}
        public String stroke() {return stroke;}
        public String translation() {return translation;}

        @Override
        public int compareTo(Recommendation that) {
            if (that==null) return -1;
            if (this.score()>that.score()) return -1;
            if (this.score()<that.score()) return 1;
            return 0;
        }
    }

}

