package edu.aitu.library.model;

import java.time.LocalDate;

public class Loan {
    private final int id;
    private final int userId;
    private final int bookId;
    private final LocalDate loanDate;
    private final LocalDate returnDate; // null if not returned

    public Loan(int id, int userId, int bookId, LocalDate loanDate, LocalDate returnDate) {
        this.id = id;
        this.userId = userId;
        this.bookId = bookId;
        this.loanDate = loanDate;
        this.returnDate = returnDate;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public int getBookId() { return bookId; }
    public LocalDate getLoanDate() { return loanDate; }
    public LocalDate getReturnDate() { return returnDate; }

    public boolean isOpen() { return returnDate == null; }

    @Override
    public String toString() {
        return "Loan{id=" + id + ", userId=" + userId + ", bookId=" + bookId +
                ", loanDate=" + loanDate + ", returnDate=" + returnDate + "}";
    }
}
