package onlineBookstore;

import java.sql.*;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class BookDatabaseManager {
    Connection connection;
    Connection earningsConnection;

    public BookDatabaseManager() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:books.db");
            Statement statement = connection.createStatement();
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS books (id INTEGER PRIMARY KEY, title TEXT, author TEXT, price REAL, stock INTEGER DEFAULT 0)");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            earningsConnection = DriverManager.getConnection("jdbc:sqlite:Earnings.db");
            Statement statement = earningsConnection.createStatement();
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS earnings (id INTEGER PRIMARY KEY, book_id INTEGER, copies INTEGER, amount_earned REAL, total_credits REAL DEFAULT 0)");
            statement.executeUpdate("INSERT OR IGNORE INTO earnings (id, book_id, copies, amount_earned) VALUES (1, 0, 0, 0)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    void insert(Book book) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO books (id, title, author, price, stock) VALUES (?, ?, ?, ?, ?)");
            preparedStatement.setInt(1, book.id);
            preparedStatement.setString(2, book.title);
            preparedStatement.setString(3, book.author);
            preparedStatement.setDouble(4, book.price);
            preparedStatement.setInt(5, book.stock);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    void insertFromFile(String filename) {
        try (Scanner scanner = new Scanner(new File(filename))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(",", 5);
                if (parts.length == 5) {
                    int id = Integer.parseInt(parts[0].trim());
                    if (search(id) != null) {
                        System.err.println("Book with ID " + id + " already exists. Skipping insertion.");
                        continue;
                    }
                    String title = parts[1].trim();
                    String author = parts[2].trim();
                    double price = Double.parseDouble(parts[3].trim());
                    int stock = Integer.parseInt(parts[4].trim());
                    insert(new Book(id, title, author, price, stock));
                } else {
                    System.err.println("Invalid line format: " + line);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    Book search(int id) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM books WHERE id = ?");
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return new Book(resultSet.getInt("id"), resultSet.getString("title"), resultSet.getString("author"), resultSet.getDouble("price"), resultSet.getInt("stock"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    void purchaseBook(int id, int copies) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM books WHERE id = ?");
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                int stock = resultSet.getInt("stock");
                double price = resultSet.getDouble("price");
                String title = resultSet.getString("title");
                if (stock >= copies) {
                    stock -= copies;
                    double amount = price * copies;
                    int retries = 3;
                    while (retries > 0) {
                        try {
                            connection.setAutoCommit(false);
                            preparedStatement = connection.prepareStatement("UPDATE books SET stock = ? WHERE id = ?");
                            preparedStatement.setInt(1, stock);
                            preparedStatement.setInt(2, id);
                            preparedStatement.executeUpdate();
                            connection.commit();
                            System.out.println("Book '" + title + "' (ID: " + id + ") purchased successfully.");
                            updateEarnings(id, copies, amount);
                            return;
                        } catch (SQLException e) {
                            connection.rollback();
                            retries--;
                            Thread.sleep(100);
                        } finally {
                            connection.setAutoCommit(true);
                        }
                    }
                    System.out.println("Failed to purchase book '" + title + "' (ID: " + id + ") after multiple attempts.");
                } else {
                    System.out.println("Sorry, there are not enough copies of the book '" + title + "' (ID: " + id + ") in stock.");
                }
            } else {
                System.out.println("Book with ID " + id + " not found.");
            }
        } catch (SQLException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    void updateEarnings(int bookId, int copies, double amountEarned) {
        try {
            PreparedStatement insertStatement = earningsConnection.prepareStatement("INSERT INTO earnings (book_id, copies, amount_earned) VALUES (?, ?, ?)");
            insertStatement.setInt(1, bookId);
            insertStatement.setInt(2, copies);
            insertStatement.setDouble(3, amountEarned);
            insertStatement.executeUpdate();

            PreparedStatement updateTotalCreditsStatement = earningsConnection.prepareStatement("UPDATE earnings SET total_credits = total_credits + ? WHERE id = 1");
            updateTotalCreditsStatement.setDouble(1, amountEarned);
            updateTotalCreditsStatement.executeUpdate();

            System.out.println("Earnings updated successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    void inorder() {
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM books");
            while (resultSet.next()) {
                System.out.println("ID: " + resultSet.getInt("id") + ", Title: " + resultSet.getString("title") + ", Author: " + resultSet.getString("author") + ", Price: $" + resultSet.getDouble("price") + ", Stock: " + resultSet.getInt("stock"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    void viewEarnings() {
        try {
            Statement statement = earningsConnection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM earnings");
            if (resultSet.next()) {
                double totalAmount = resultSet.getDouble("total_credits");
                System.out.println("Total earnings: $" + totalAmount);
            } else {
                System.out.println("No earnings recorded yet.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    void viewMostSoldBook() {
        try {
            Statement statement = earningsConnection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT book_id, SUM(copies) AS total_sold FROM earnings GROUP BY book_id ORDER BY total_sold DESC LIMIT 1");
            if (resultSet.next()) {
                int mostSoldBookId = resultSet.getInt("book_id");
                int totalSold = resultSet.getInt("total_sold");
                Book mostSoldBook = search(mostSoldBookId);
                if (mostSoldBook != null) {
                    System.out.println("Most sold book:");
                    System.out.println("ID: " + mostSoldBook.id + ", Title: " + mostSoldBook.title + ", Author: " + mostSoldBook.author + ", Total Sold: " + totalSold);
                }
            } else {
                System.out.println("No books sold yet.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    void viewHighestEarningBook() {
        try {
            Statement statement = earningsConnection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT book_id, SUM(amount_earned) AS total_earned FROM earnings GROUP BY book_id ORDER BY total_earned DESC LIMIT 1");
            if (resultSet.next()) {
                int highestEarningBookId = resultSet.getInt("book_id");
                double totalEarned = resultSet.getDouble("total_earned");
                Book highestEarningBook = search(highestEarningBookId);
                if (highestEarningBook != null) {
                    System.out.println("Book with highest earnings:");
                    System.out.println("ID: " + highestEarningBook.id + ", Title: " + highestEarningBook.title + ", Author: " + highestEarningBook.author + ", Total Earned: $" + totalEarned);
                }
            } else {
                System.out.println("No earnings recorded yet.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getRecommendations(String bookTitle) {
        try {
            URL url = new URL("http://127.0.0.1:5000/recommend");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String jsonInputString = "{\"book_title\": \"" + bookTitle + "\"}";

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                return response.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    void closeConnection() {
        try {
            if (connection != null)
                connection.close();
            if (earningsConnection != null)
                earningsConnection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
