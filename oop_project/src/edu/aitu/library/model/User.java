package edu.aitu.library.model;

public abstract class User {
    private final int id;
    private String name;
    private final Role role;

    protected User(int id, String name, Role role) {
        this.id = id;
        this.name = name;
        this.role = role;
    }

    public final int getId() { return id; }
    public final String getName() { return name; }
    public final void setName(String name) { this.name = name; }
    public final Role getRole() { return role; }

    public abstract int getBorrowLimit();
    public abstract int getLoanPeriodDays();

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', role=" + role +
                ", limit=" + getBorrowLimit() + ", loanDays=" + getLoanPeriodDays() + "}";
    }
}


