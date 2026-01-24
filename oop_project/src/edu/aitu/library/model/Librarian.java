package edu.aitu.library.model;

public class Librarian extends User {
    public Librarian(int id, String name) {
        super(id, name, Role.LIBRARIAN);
    }

    @Override
    public int getBorrowLimit() {
        return 10;
    }
}

