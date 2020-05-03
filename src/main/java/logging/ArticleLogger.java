package logging;

import java.io.*;
import java.util.ArrayList;

public class ArticleLogger {
    private File file;
    private BufferedWriter writer;
    private BufferedReader reader;

    public ArticleLogger(String name) throws IOException {
        file = new File(name);
        // create the logging file if it doesn't exist already
        if(file.createNewFile()){
            System.out.println("Bot created new logging file");
        } else {
            System.out.println("Bot created new file handle to existing logging file");
        }

        // set up readers and writers
        writer = new BufferedWriter(new FileWriter(file.getAbsoluteFile(), true));
        reader = new BufferedReader(new FileReader(file.getAbsoluteFile()));
    }

    /**
     * Write a single URL to the log file
     * @param url The URL string to add to the file
     * @throws IOException An error writing
     */
    public void log(String url) throws IOException {
        System.out.println("Logging a URL into " + file.getName());
        writer.write(url + "\n");
        writer.flush();
    }

}
