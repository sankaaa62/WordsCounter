package com.company;

import sun.awt.Mutex;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;


public class IndexServlet extends HttpServlet {

    @SuppressWarnings("WeakerAccess")
    class IndexThread extends Thread {

        private Mutex indexMutex;
        private Indexer indexer;

        @Override
        public void run() {
            this.indexMutex.lock();
            try {
                this.indexer.indexToDB();
            } catch (InterruptedException | SQLException | ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
            this.indexMutex.unlock();
        }

        public void setIndexMutex(Mutex indexMutex) {
            this.indexMutex = indexMutex;
        }

        public void setIndexer(Indexer indexer) {
            this.indexer = indexer;
        }

    }

    private Indexer indexer;
    private Mutex indexMutex;

    public IndexServlet() throws IOException {
        this.indexer = new Indexer();
        this.indexer.setThreadsCount(5);
        this.indexer.loadWordsToExcludeFromFile("words_to_exclude.txt");
        this.indexer.setPathToDB("WordsDB.db");
        this.indexer.setWorkDirectory("Texts\\");

        this.indexMutex = new Mutex();
    }

    public void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws IOException {

        httpServletResponse.getWriter().print(
                "<head>" +
                    "<meta http-equiv=\"refresh\" content=\"3;URL=/search\" />" +
                "</head>" +
                "<div align=\"center\">" +
                    "<h2>Ожидайте, идет индексирование.</h2>" +
                "</div>"
        );

        this.startIndexing();
    }

    private void startIndexing(){
        IndexThread indexThread = new IndexThread();
        indexThread.setIndexer(this.indexer);
        indexThread.setIndexMutex(this.indexMutex);
        indexThread.start();
    }
}
