package onlineBookstore;



import java.util.Scanner;

public class BookStore {
    public static void main(String[] args) {
        BookDatabaseManager bookDatabaseManager = new BookDatabaseManager();
        bookDatabaseManager.insertFromFile("book_list.txt");

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\nMenu:");
            System.out.println("1. Add a book");
            System.out.println("2. Search for a book");
            System.out.println("3. View all books");
            System.out.println("4. Purchase a book");
            System.out.println("5. View earnings");
            System.out.println("6. View most sold book");
            System.out.println("7. View book with highest earnings");
            System.out.println("8. Get recommendation of books");
            System.out.println("9. Exit");
            System.out.print("Enter your choice: ");
            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    System.out.print("Enter book ID: ");
                    int bookId = scanner.nextInt();
                    scanner.nextLine();
                    System.out.print("Enter book title: ");
                    String bookTitle = scanner.nextLine();
                    System.out.print("Enter book author: ");
                    String bookAuthor = scanner.nextLine();
                    System.out.print("Enter book price: ");
                    double bookPrice = scanner.nextDouble();
                    System.out.print("Enter book stock: ");
                    int bookStock = scanner.nextInt();
                    scanner.nextLine();
                    bookDatabaseManager.insert(new Book(bookId, bookTitle, bookAuthor, bookPrice, bookStock));
                    break;
                case 2:
                    System.out.print("Enter book ID to search: ");
                    int searchId = scanner.nextInt();
                    scanner.nextLine();
                    Book result = bookDatabaseManager.search(searchId);
                    if (result != null) {
                        System.out.println("Book found: ID: " + result.id + ", Title: " + result.title + ", Author: " + result.author + ", Price: $" + result.price + ", Stock: " + result.stock);
                    } else {
                        System.out.println("Book not found.");
                    }
                    break;
                case 3:
                    System.out.println("Books available in the store:");
                    bookDatabaseManager.inorder();
                    break;
                case 4:
                    System.out.print("Enter book ID to purchase: ");
                    int purchaseId = scanner.nextInt();
                    System.out.print("Enter the number of copies you want to purchase: ");
                    int copies = scanner.nextInt();
                    scanner.nextLine();
                    bookDatabaseManager.purchaseBook(purchaseId, copies);
                    break;
                case 5:
                    bookDatabaseManager.viewEarnings();
                    break;
                case 6:
                    bookDatabaseManager.viewMostSoldBook();
                    break;
                case 7:
                    bookDatabaseManager.viewHighestEarningBook();
                    break;
                case 8:
                    System.out.print("Enter the title of the book for which you want recommendations: ");
                    String bookTitleForRecommendation = scanner.nextLine();
                    String recommendations = bookDatabaseManager.getRecommendations(bookTitleForRecommendation);
                    System.out.println("Recommended books for " + bookTitleForRecommendation + ":");
                    System.out.println(recommendations);
                    break;
                case 9:
                    System.out.println("Exiting...");
                    scanner.close();
                    bookDatabaseManager.closeConnection();
                    System.exit(0);
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }
}
