package dev.reecemercer.logging;

import java.io.*;

/**
 * Log generated Wikipedia page URLs into a file
 */
public class ArticleLogger {
    private final File file;
    private final BufferedWriter writer;

    /**
     * Create a new ArticleLogger instance
     * @param name The name of the external file to log the generated URLs to
     * @throws IOException Any errors thrown when opening a file handle to the logging file
     */
    public ArticleLogger(String name) throws IOException {
        file = new File(name);
        // create the logging file if it doesn't exist already
        if(file.createNewFile()){
            System.out.println("Bot created new logging file");
        } else {
            System.out.println("Bot created new file handle to existing logging file");
        }

        // set up a BufferedWriter in append mode so it can add to the end of the log file
        writer = new BufferedWriter(new FileWriter(file.getAbsoluteFile(), true));
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

    /**
     * Flush and close the logging file's attached writer
     * @throws IOException Any errors thrown during the flushing/closing of the writer
     */
    public void closeFileHandle() throws IOException {
        writer.flush();
        writer.close();
    }

}
