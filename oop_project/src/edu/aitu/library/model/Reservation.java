package edu.aitu.library.model;

import java.time.LocalDate;

public class Reservation {
    private final int id;
    private final int userId;
    private final int bookId;
    private final ReservationStatus status;
    private final LocalDate createdAt;
    private final LocalDate expiresAt;
    private final Integer fulfilledLoanId;

    public Reservation(int id, int userId, int bookId, ReservationStatus status,
                       LocalDate createdAt, LocalDate expiresAt, Integer fulfilledLoanId) {
        this.id = id;
        this.userId = userId;
        this.bookId = bookId;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.fulfilledLoanId = fulfilledLoanId;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public int getBookId() { return bookId; }
    public ReservationStatus getStatus() { return status; }
    public LocalDate getCreatedAt() { return createdAt; }
    public LocalDate getExpiresAt() { return expiresAt; }
    public Integer getFulfilledLoanId() { return fulfilledLoanId; }
    public boolean isActive() { return status == ReservationStatus.ACTIVE; }

    @Override
    public String toString() {
        return "Reservation{id=" + id +
                ", userId=" + userId +
                ", bookId=" + bookId +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                ", fulfilledLoanId=" + fulfilledLoanId + "}";
    }
}
