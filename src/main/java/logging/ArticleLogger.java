package logging;

import java.io.File;

public class ArticleLogger {
    private File file;

    public ArticleLogger(String name){
        file = new File(name);

    }
}
