package edu.aitu.library.service;

import edu.aitu.library.exception.*;
import edu.aitu.library.model.*;
import edu.aitu.library.repository.*;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class LibraryService {
    private final Connection conn;
    private final UserRepository userRepo;
    private final BookRepository bookRepo;
    private final LoanRepository loanRepo;
    private final ReservationRepository reservationRepo;

    private static final int FINE_CENTS_PER_DAY = 200;
    private static final int RESERVATION_EXPIRES_DAYS = 7;

    public LibraryService(Connection conn) {
        this.conn = conn;
        this.userRepo = new UserRepository(conn);
        this.bookRepo = new BookRepository(conn);
        this.loanRepo = new LoanRepository(conn);
        this.reservationRepo = new ReservationRepository(conn);
    }

    public int registerUser(String name, Role role) throws LibraryException {
        try {
            return userRepo.createUser(name, role);
        } catch (Exception e) {
            throw new LibraryException("DB error while creating user.", e);
        }
    }

    public int addBook(String title, String author) throws LibraryException {
        try {
            return bookRepo.addBook(title, author);
        } catch (Exception e) {
            throw new LibraryException("DB error while adding book.", e);
        }
    }

    public List<User> listUsers() throws LibraryException {
        try {
            return userRepo.findAll();
        } catch (Exception e) {
            throw new LibraryException("DB error while listing users.", e);
        }
    }

    public List<Book> listBooks() throws LibraryException {
        try {
            return bookRepo.findAll();
        } catch (Exception e) {
            throw new LibraryException("DB error while listing books.", e);
        }
    }

    public List<Loan> listLoans() throws LibraryException {
        try {
            return loanRepo.listAllLoans();
        } catch (Exception e) {
            throw new LibraryException("DB error while listing loans.", e);
        }
    }

    public List<Reservation> listReservations() throws LibraryException {
        try {
            reservationRepo.expireOldReservations(LocalDate.now());
            return reservationRepo.listAll();
        } catch (Exception e) {
            throw new LibraryException("DB error while listing reservations.", e);
        }
    }

    public int reserveBook(int userId, int bookId) throws LibraryException {
        try {
            reservationRepo.expireOldReservations(LocalDate.now());

            User user = userRepo.findById(userId);
            if (user == null) throw new NotFoundException("User not found: id=" + userId);

            Book book = bookRepo.findById(bookId);
            if (book == null) throw new NotFoundException("Book not found: id=" + bookId);

            if (reservationRepo.hasActiveReservation(userId, bookId)) {
                throw new ReservationExistsException("You already have an active reservation for this book.");
            }

            boolean freeToTake = book.isAvailable() && !loanRepo.hasOpenLoanForBook(bookId);

            if (freeToTake) {
                try {
                    int loanId = borrowBook(userId, bookId);
                    return -loanId;
                } catch (BorrowLimitExceededException | ReservationNotAllowedException | BookUnavailableException e) {
                    // fall through to reservation creation
                }
            }

            LocalDate expiresAt = LocalDate.now().plusDays(RESERVATION_EXPIRES_DAYS);
            return reservationRepo.createReservation(userId, bookId, expiresAt);

        } catch (LibraryException e) {
            throw e;
        } catch (Exception e) {
            throw new LibraryException("DB error while reserving book.", e);
        }
    }

    public int borrowBook(int userId, int bookId) throws LibraryException {
        boolean oldAutoCommit = true;
        try {
            oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            reservationRepo.expireOldReservations(LocalDate.now());

            User user = userRepo.findById(userId);
            if (user == null) throw new NotFoundException("User not found: id=" + userId);

            Book book = bookRepo.findById(bookId);
            if (book == null) throw new NotFoundException("Book not found: id=" + bookId);

            if (!book.isAvailable() || loanRepo.hasOpenLoanForBook(bookId)) {
                throw new BookUnavailableException("Book is not available now: id=" + bookId);
            }

            Reservation oldest = reservationRepo.findOldestActiveReservationForBook(bookId);
            if (oldest != null && oldest.getUserId() != userId) {
                throw new ReservationNotAllowedException(
                        "This book is reserved for another user (FIFO). Only the earliest reserver can borrow it now."
                );
            }

            int openLoans = loanRepo.countOpenLoansByUser(userId);
            if (openLoans >= user.getBorrowLimit()) {
                throw new BorrowLimitExceededException(
                        "Borrow limit exceeded. Current open loans=" + openLoans +
                                ", limit=" + user.getBorrowLimit() + " for role=" + user.getRole()
                );
            }

            LocalDate dueDate = LocalDate.now().plusDays(user.getLoanPeriodDays());

            bookRepo.setAvailability(bookId, false);
            int loanId = loanRepo.createLoan(userId, bookId, dueDate);

            if (oldest != null && oldest.getUserId() == userId) {
                reservationRepo.fulfillReservation(oldest.getId(), loanId);
            }

            conn.commit();
            return loanId;

        } catch (LibraryException e) {
            rollbackQuietly();
            throw e;
        } catch (Exception e) {
            rollbackQuietly();
            throw new LibraryException("Unexpected error while borrowing book.", e);
        } finally {
            restoreAutoCommitQuietly(oldAutoCommit);
        }
    }

    public void returnBook(int bookId) throws LibraryException {
        boolean oldAutoCommit = true;
        try {
            oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            reservationRepo.expireOldReservations(LocalDate.now());

            Book book = bookRepo.findById(bookId);
            if (book == null) throw new NotFoundException("Book not found: id=" + bookId);

            Loan openLoan = loanRepo.findOpenLoanByBook(bookId);
            if (openLoan == null) throw new NotFoundException("No open loan found for book: id=" + bookId);

            LocalDate today = LocalDate.now();
            long overdueDays = ChronoUnit.DAYS.between(openLoan.getDueDate(), today);
            int fine = overdueDays > 0 ? (int) overdueDays * FINE_CENTS_PER_DAY : 0;

            loanRepo.closeLoanWithFine(openLoan.getId(), fine);
            bookRepo.setAvailability(bookId, true);

            Reservation next = reservationRepo.findOldestActiveReservationForBook(bookId);
            if (next != null) {
                User reservedUser = userRepo.findById(next.getUserId());
                if (reservedUser != null) {
                    int openLoans = loanRepo.countOpenLoansByUser(reservedUser.getId());
                    if (openLoans < reservedUser.getBorrowLimit()) {
                        LocalDate due = LocalDate.now().plusDays(reservedUser.getLoanPeriodDays());
                        bookRepo.setAvailability(bookId, false);
                        int newLoanId = loanRepo.createLoan(reservedUser.getId(), bookId, due);
                        reservationRepo.fulfillReservation(next.getId(), newLoanId);
                    }
                }
            }

            conn.commit();
        } catch (LibraryException e) {
            rollbackQuietly();
            throw e;
        } catch (Exception e) {
            rollbackQuietly();
            throw new LibraryException("Unexpected error while returning book.", e);
        } finally {
            restoreAutoCommitQuietly(oldAutoCommit);
        }
    }

    private void rollbackQuietly() {
        try { conn.rollback(); } catch (Exception ignored) {}
    }

    private void restoreAutoCommitQuietly(boolean oldAutoCommit) {
        try { conn.setAutoCommit(oldAutoCommit); } catch (Exception ignored) {}
    }
}
