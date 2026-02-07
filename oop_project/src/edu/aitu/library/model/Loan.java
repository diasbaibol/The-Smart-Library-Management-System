package edu.aitu.library.model;

import java.time.LocalDate;

public class Loan {
    private final int id;
    private final int userId;
    private final int bookId;
    private final LocalDate loanDate;
    private final LocalDate dueDate;
    private final LocalDate returnDate;
    private final int fineCents;

    public Loan(int id, int userId, int bookId, LocalDate loanDate, LocalDate dueDate, LocalDate returnDate, int fineCents) {
        this.id = id;
        this.userId = userId;
        this.bookId = bookId;
        this.loanDate = loanDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.fineCents = fineCents;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public int getBookId() { return bookId; }
    public LocalDate getLoanDate() { return loanDate; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public int getFineCents() { return fineCents; }
    public boolean isOpen() { return returnDate == null; }

    @Override
    public String toString() {
        return "Loan{id=" + id + ", userId=" + userId + ", bookId=" + bookId +
                ", loanDate=" + loanDate + ", dueDate=" + dueDate +
                ", returnDate=" + returnDate + ", fineCents=" + fineCents + "}";
    }
}

