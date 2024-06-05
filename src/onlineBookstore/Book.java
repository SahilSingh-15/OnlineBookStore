package onlineBookstore;

public class Book {
    int id;
    String title;
    String author;
    double price;
    int stock;

    public Book(int id, String title, String author, double price, int stock) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.price = price;
        this.stock = stock;
    }

    // Method to decrease stock when a book is purchased
    public void decreaseStock(int quantity) {
        if (stock >= quantity) {
            stock -= quantity;
        } else {
            System.out.println("Sorry, there are not enough copies of this book in stock.");
        }
    }
}
