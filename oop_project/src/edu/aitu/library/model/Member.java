package edu.aitu.library.model;

public class Member extends User {
    public Member(int id, String name) {
        super(id, name, Role.MEMBER);
    }

    @Override
    public int getBorrowLimit() {
        return 3;
    }
}

