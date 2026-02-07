package edu.aitu.library.app;

import edu.aitu.library.data.DB;
import edu.aitu.library.data.SchemaInitializer;
import edu.aitu.library.exception.LibraryException;
import edu.aitu.library.model.Role;
import edu.aitu.library.service.LibraryService;

import java.sql.Connection;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        String url = "jdbc:sqlite:library.db";

        try (Connection conn = new DB(url).getConnection()) {
            SchemaInitializer.init(conn);
            LibraryService service = new LibraryService(conn);
            runMenu(service);
        } catch (Exception e) {
            System.out.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runMenu(LibraryService service) {
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("\n=== Smart Library Management System ===");
            System.out.println("1) Register user");
            System.out.println("2) Add book");
            System.out.println("3) List users");
            System.out.println("4) List books");
            System.out.println("5) Borrow book");
            System.out.println("6) Return book");
            System.out.println("7) List loans");
            System.out.println("8) Reserve book");
            System.out.println("9) List reservations");
            System.out.println("0) Exit");
            System.out.print("Choose: ");

            String choice = sc.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> registerUser(sc, service);
                    case "2" -> addBook(sc, service);
                    case "3" -> service.listUsers().forEach(System.out::println);
                    case "4" -> service.listBooks().forEach(System.out::println);
                    case "5" -> borrowBook(sc, service);
                    case "6" -> returnBook(sc, service);
                    case "7" -> service.listLoans().forEach(System.out::println);
                    case "8" -> reserveBook(sc, service);
                    case "9" -> service.listReservations().forEach(System.out::println);
                    case "0" -> {
                        System.out.println("Bye.");
                        return;
                    }
                    default -> System.out.println("Invalid option.");
                }
            } catch (LibraryException e) {
                System.out.println("Operation failed: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Unexpected error: " + e.getMessage());
            }
        }
    }

    private static void registerUser(Scanner sc, LibraryService service) throws LibraryException {
        System.out.print("Name: ");
        String name = sc.nextLine().trim();

        System.out.print("Role (MEMBER / LIBRARIAN): ");
        Role role = Role.fromString(sc.nextLine());

        int id = service.registerUser(name, role);
        System.out.println("Created user with id=" + id);
    }

    private static void addBook(Scanner sc, LibraryService service) throws LibraryException {
        System.out.print("Title: ");
        String title = sc.nextLine().trim();

        System.out.print("Author: ");
        String author = sc.nextLine().trim();

        int id = service.addBook(title, author);
        System.out.println("Added book with id=" + id);
    }

    private static void borrowBook(Scanner sc, LibraryService service) throws LibraryException {
        System.out.print("User id: ");
        int userId = Integer.parseInt(sc.nextLine().trim());

        System.out.print("Book id: ");
        int bookId = Integer.parseInt(sc.nextLine().trim());

        service.borrowBook(userId, bookId);
        System.out.println("Book borrowed successfully.");
    }

    private static void returnBook(Scanner sc, LibraryService service) throws LibraryException {
        System.out.print("Book id: ");
        int bookId = Integer.parseInt(sc.nextLine().trim());

        service.returnBook(bookId);
        System.out.println("Book returned successfully.");
    }

    private static void reserveBook(Scanner sc, LibraryService service) throws LibraryException {
        System.out.print("User id: ");
        int userId = Integer.parseInt(sc.nextLine().trim());

        System.out.print("Book id: ");
        int bookId = Integer.parseInt(sc.nextLine().trim());

        int resId = service.reserveBook(userId, bookId);
        System.out.println("Reservation created with id=" + resId);
    }
}
