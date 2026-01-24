package edu.aitu.library.service;

import edu.aitu.library.exception.*;
import edu.aitu.library.model.Book;
import edu.aitu.library.model.Role;
import edu.aitu.library.model.User;
import edu.aitu.library.repository.BookRepository;
import edu.aitu.library.repository.LoanRepository;
import edu.aitu.library.repository.UserRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class LibraryService {
    private final Connection conn;
    private final UserRepository userRepo;
    private final BookRepository bookRepo;
    private final LoanRepository loanRepo;

    public LibraryService(Connection conn) {
        this.conn = conn;
        this.userRepo = new UserRepository(conn);
        this.bookRepo = new BookRepository(conn);
        this.loanRepo = new LoanRepository(conn);
    }

    public int registerUser(String name, Role role) throws LibraryException {
        try {
            return userRepo.createUser(name, role);
        } catch (SQLException e) {
            throw new LibraryException("DB error while creating user.", e);
        }
    }

    public int addBook(String title, String author) throws LibraryException {
        try {
            return bookRepo.addBook(title, author);
        } catch (SQLException e) {
            throw new LibraryException("DB error while adding book.", e);
        }
    }

    public List<User> listUsers() throws LibraryException {
        try {
            return userRepo.findAll();
        } catch (SQLException e) {
            throw new LibraryException("DB error while listing users.", e);
        }
    }

    public List<Book> listBooks() throws LibraryException {
        try {
            return bookRepo.findAll();
        } catch (SQLException e) {
            throw new LibraryException("DB error while listing books.", e);
        }
    }

    public void borrowBook(int userId, int bookId) throws LibraryException {
        boolean oldAutoCommit;
        try {
            oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            User user = userRepo.findById(userId);
            if (user == null) throw new NotFoundException("User not found: id=" + userId);

            Book book = bookRepo.findById(bookId);
            if (book == null) throw new NotFoundException("Book not found: id=" + bookId);

            if (!book.isAvailable() || loanRepo.hasOpenLoanForBook(bookId)) {
                throw new BookUnavailableException("Book is not available now: id=" + bookId);
            }

            int openLoans = loanRepo.countOpenLoansByUser(userId);
            if (openLoans >= user.getBorrowLimit()) {
                throw new BorrowLimitExceededException(
                        "Borrow limit exceeded. Current open loans=" + openLoans +
                                ", limit=" + user.getBorrowLimit() + " for role=" + user.getRole()
                );
            }

            // Update availability + create loan (same transaction)
            bookRepo.setAvailability(bookId, false);
            loanRepo.createLoan(userId, bookId);

            conn.commit();
            conn.setAutoCommit(oldAutoCommit);

        } catch (LibraryException e) {
            rollbackQuietly();
            restoreAutoCommitQuietly();
            throw e;
        } catch (Exception e) {
            rollbackQuietly();
            restoreAutoCommitQuietly();
            throw new LibraryException("Unexpected error while borrowing book.", e);
        }
    }

    public void returnBook(int bookId) throws LibraryException {
        boolean oldAutoCommit;
        try {
            oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            Book book = bookRepo.findById(bookId);
            if (book == null) throw new NotFoundException("Book not found: id=" + bookId);

            var openLoan = loanRepo.findOpenLoanByBook(bookId);
            if (openLoan == null) throw new NotFoundException("No open loan found for book: id=" + bookId);

            loanRepo.closeLoan(openLoan.getId());
            bookRepo.setAvailability(bookId, true);

            conn.commit();
            conn.setAutoCommit(oldAutoCommit);

        } catch (LibraryException e) {
            rollbackQuietly();
            restoreAutoCommitQuietly();
            throw e;
        } catch (Exception e) {
            rollbackQuietly();
            restoreAutoCommitQuietly();
            throw new LibraryException("Unexpected error while returning book.", e);
        }
    }

    public List<edu.aitu.library.model.Loan> listLoans() throws LibraryException {
        try {
            return loanRepo.listAllLoans();
        } catch (SQLException e) {
            throw new LibraryException("DB error while listing loans.", e);
        }
    }

    private void rollbackQuietly() {
        try { conn.rollback(); } catch (Exception ignored) {}
    }

    private void restoreAutoCommitQuietly() {
        try { conn.setAutoCommit(true); } catch (Exception ignored) {}
    }
}
