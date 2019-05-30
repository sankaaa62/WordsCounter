package com.company;

import javafx.util.Pair;
import java.sql.*;
import java.util.*;


@SuppressWarnings("WeakerAccess")
public class Finder {
    private String pathToDB;

    public Finder() {
    }

    public void setPathToDB(String pathToDB) {
        this.pathToDB = pathToDB;
    }

    public List<Pair<String, Integer>> search(String userInput) throws SQLException, ClassNotFoundException {
        List<String> words = Arrays.asList(userInput.toLowerCase().split(" "));

        Connection dbConn;
        Class.forName("org.sqlite.JDBC");
        dbConn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", this.pathToDB));
        dbConn.setAutoCommit(false);

        StringJoiner joiner = new StringJoiner(",", "(", ")");
        for (int i=0; i < words.size(); i+=1)
            joiner.add("?");
        PreparedStatement statement = dbConn.prepareStatement(
                "SELECT FILE_PATH, COUNT(WORD) AS MATCHES_COUNT " +
                        "FROM WORDS_COUNTS WHERE WORD IN " + joiner.toString() +
                        "GROUP BY FILE_PATH;");
        for (int i = 1; i <= words.size(); i+=1) {
            statement.setObject(i, words.get(i-1));
        }

        ResultSet result = statement.executeQuery();
        List<Pair<String, Integer>> wordsAndCounts = new ArrayList<>();
        while (result.next()) {
            wordsAndCounts.add(new Pair<>(
                    result.getString("FILE_PATH"),
                    result.getInt("MATCHES_COUNT")
            ));
        }
        statement.close();

        dbConn.close();
        return  wordsAndCounts;
    }

}
