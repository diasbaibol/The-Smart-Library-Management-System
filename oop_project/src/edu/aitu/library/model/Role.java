package edu.aitu.library.model;

public enum Role {
    MEMBER,
    LIBRARIAN;

    public static Role fromString(String value) {
        if (value == null) return MEMBER;
        return Role.valueOf(value.trim().toUpperCase());
    }
}
