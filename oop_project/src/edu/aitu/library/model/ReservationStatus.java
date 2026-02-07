package edu.aitu.library.model;

public enum ReservationStatus {
    ACTIVE,
    CANCELLED,
    FULFILLED,
    EXPIRED;

    public static ReservationStatus fromString(String s) {
        return ReservationStatus.valueOf(s.trim().toUpperCase());
    }
}
