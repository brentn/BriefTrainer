import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by brentn on 17/02/14.
 *
 */
public class BriefTrainer {

    private static boolean DEBUG = true;

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
        Dictionary dictionary = new Dictionary();
        try {
            readConfig();
        } catch (FileNotFoundException e) {
            System.err.println("Error: could not locate Plover config file.");
        }
        dictionary.load(dictionaryFiles.toArray(new String[dictionaryFiles.size()]));
    }


}

