
package com.company;

import javafx.util.Pair;

import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;

public class FinderServlet extends HttpServlet {

    public void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws IOException {
        httpServletResponse.getWriter().print(
                "<html>" +
                "<head>" +
                        "<title>Поиск слов в текстах</title>" +
                "</head>" +
                    "<body>" +
                        "<div align=\"center\">" +
                            "<h1>Поиск слов</h1>" +
                            "<form method=\"POST\" action=\"/search\">" +
                                "<input type=\"text\" name=\"search_text\">" +
                                "<input type=\"submit\" value=\"Поиск\" />" +
                            "</form>" +
                            "<a href=\"/index\">Повторная индексация</a>" +
                        "</div>" +
                    "</body>" +
                "</html>"
        );
    }

    public void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws IOException {

        Finder finder = new Finder();
        finder.setPathToDB("WordsDB.db");
        String userInput = httpServletRequest.getParameter("search_text");
        try {
            List<Pair<String, Integer>> results = finder.search(userInput);
            this.writeSearchResults(httpServletResponse.getWriter(), results);
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void writeSearchResults(PrintWriter responseWriter, List<Pair<String, Integer>> searchResults){
        responseWriter.print(
                "<div align=\"center\">" +
                    "<table rules=\"all\" frame=\"border\" style=\"width: 75%;\">" +
                    "<b><caption><h1>Результаты поиска</caption></h1></b>" +
                    "<tr><th>Файл</th><th>Количество совпадений</th></tr>"

        );
        for (Pair<String, Integer> oneResultPoint: searchResults){
            responseWriter.print(
                    "<tr><td>"+oneResultPoint.getKey()+"</td><td>"+oneResultPoint.getValue()+"</td></tr>"
            );
        }
        responseWriter.print(
                    "</table>" +
                    "<a href=\"/\">Назад</a>" +
                "</div>"
        );
    }
}