package sample;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {
    public final static String root = System.getenv("APPDATA") + "/cubetimer/";

    public static List<String> loadFile(String subpath) {
        List<String> lines = new ArrayList<String>();
        File f = new File(root + subpath);
        if (f.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(f));
                String line;
                while ((line = br.readLine()) != null) {
                    lines.add(line);
                }
                br.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                System.out.println("Error loading file: \"" + e.getMessage()
                        + "\"");
            }

        } else {
            System.out.println("File \"" + subpath + "\" could not be found.");
        }
        return lines;
    }

    public static boolean saveFile(String subpath, List<String> lines) {
        File f = new File(root + subpath);
        f.mkdirs();
        if (f.exists()) {
            f.delete();
        }
        try {
            FileWriter writer;
            writer = new FileWriter(f);
            for (String s : lines) {
                writer.write(s + "\n");
            }
            writer.close();
            return true;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            System.out.println("Error saving file: \"" + e.getMessage() + "\"");
            return false;
        }
    }

}