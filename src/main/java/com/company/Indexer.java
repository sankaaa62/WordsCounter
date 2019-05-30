package com.company;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newFixedThreadPool;


@SuppressWarnings("WeakerAccess")
public class Indexer {

    private String workDirectory;
    private int threadsCount = 5;
    private List<String> wordsToExclude;
    private String pathToDB;
    private String splitSymbols = "\\!.,-?;:…«»–()  ";
    private Connection dbConn;
    private Map<String, HashMap<String, Integer>> wordsCounts;

    /****************************************************/

    Indexer(){
        this.wordsCounts = new ConcurrentHashMap<>();
        this.wordsToExclude = new ArrayList<>();
    }

    /****************************************************/

    public void indexToDB() throws InterruptedException, SQLException, ClassNotFoundException, IOException {
        System.out.println("Indexing");
        this.countWords();
        System.out.println("Indexed");

        System.out.println("Storing index to db");
        this.connectDB();
        this.clearDB();
        this.storeIndexToDB();
        this.disconnectDB();
        System.out.println("Index stored");
    }

    private void countWords() throws InterruptedException, IOException {
        this.wordsCounts.clear();
        this.countWordsInFiles(this.getFilesListInWorkDirectory());
    }

    private List<String> getFilesListInWorkDirectory() throws IOException {
        List<String> dirList = new ArrayList<>();
        List<String> filesList = new ArrayList<>();

        dirList.add(this.workDirectory);
        int i = 0;
        int dirsCount = dirList.size();
        while (i < dirsCount) {
            for (File obj : (new File(dirList.get(i))).listFiles()) {
                if (obj.isDirectory()) {
                    dirList.add(obj.getCanonicalPath());
                    dirsCount += 1;
                }

                if (obj.isFile()) {
                    filesList.add(obj.getCanonicalPath());
                }

            }
            i += 1;
        }

        return filesList;
    }

    private void countWordsInFiles(List<String> filesList) throws InterruptedException{
        ExecutorService executor = newFixedThreadPool(this.threadsCount);

        for (String filePath: filesList){
            executor.execute(()->{
                try {
                    this.countWordsInFile(filePath);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

    private void countWordsInFile(String path) throws FileNotFoundException{
        File file = new File(path);
        Scanner scanner = new Scanner(file, "utf-8");
        StringTokenizer tokenizer;

        String word;
        HashMap<String, Integer> wordsCountsInFile = new HashMap<>();
        while (scanner.hasNextLine()) {
            tokenizer = new StringTokenizer(scanner.nextLine().toLowerCase(), splitSymbols);
            while (tokenizer.hasMoreElements()){
                word = tokenizer.nextToken();
                if (!this.wordsToExclude.contains(word)){
                    if (!wordsCountsInFile.containsKey(word)){
                        wordsCountsInFile.put(word, 0);
                    }
                    wordsCountsInFile.put(word, wordsCountsInFile.get(word) + 1);
                }
            }
        }

        scanner.close();
        this.wordsCounts.put(path, wordsCountsInFile);
    }

    private void connectDB() throws ClassNotFoundException, SQLException{
        System.out.println("Connecting to DB");

        Class.forName("org.sqlite.JDBC");
        this.dbConn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", this.pathToDB));
        this.dbConn.setAutoCommit(false);

        System.out.println("DB Connected");
    }

    private void clearDB() throws SQLException{
        System.out.println("Removing data from db");

        Statement statmt = this.dbConn.createStatement();
        statmt.execute("DELETE FROM 'WORDS_COUNTS'");
        statmt.close();
        this.dbConn.commit();

        System.out.println("DB Clear");
    }

    private void storeIndexToDB() throws SQLException {
        System.out.println("Storing data to db");

        this.wordsCounts.forEach((filePath, wordsInFiles)->{
            wordsInFiles.forEach((word, count)->{
                try {
                    PreparedStatement statement = this.dbConn.prepareStatement(
                            "INSERT INTO 'WORDS_COUNTS' " +
                                    "('FILE_PATH', 'WORD', 'COUNT') VALUES (?, ?, ?)");
                    statement.setObject(1, filePath);
                    statement.setObject(2, word);
                    statement.setObject(3, count);

                    statement.execute();
                    statement.close();

                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
        });

        this.dbConn.commit();
        System.out.println("Data stored");
    }

    private void disconnectDB() throws SQLException {
        this.dbConn.close();
        this.dbConn = null;
    }

    /****************************************************/

    public void setPathToDB(String path){
        this.pathToDB = path;
    }

    public void setThreadsCount(int threadsCount) {
        this.threadsCount = threadsCount;
    }

    public void setWorkDirectory(String path) {
        this.workDirectory = path;
    }

    public void loadWordsToExcludeFromFile(String filePath) throws IOException {
        File file = new File(filePath);
        Scanner scanner = new Scanner(file, "utf-8");

        List<String> wordsToExclude = new ArrayList<String>();
        while (scanner.hasNextLine()) {
            wordsToExclude.add(scanner.nextLine());
        }

        this.setWordsToExclude(wordsToExclude);
    }

    public void setWordsToExclude(List<String> words) {
        this.wordsToExclude = words;
    }

}
