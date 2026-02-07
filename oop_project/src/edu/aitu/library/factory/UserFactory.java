package edu.aitu.library.factory;

import edu.aitu.library.model.*;

public class UserFactory {
    private UserFactory() {}

    public static User create(int id, String name, Role role) {
        return switch (role) {
            case LIBRARIAN -> new Librarian(id, name);
            case MEMBER -> new Member(id, name);
        };
    }
}


