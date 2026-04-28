package org.example.tpchatjavafx.model;

/**
 * Représente un utilisateur de l'application.
 */
public class User {

    private final int    id;
    private final String username;
    private final String email;
    private final String passwordHash;

    public User(int id, String username, String email, String passwordHash) {
        this.id           = id;
        this.username     = username;
        this.email        = email;
        this.passwordHash = passwordHash;
    }

    public int    getId()           { return id; }
    public String getUsername()     { return username; }
    public String getEmail()        { return email; }
    public String getPasswordHash() { return passwordHash; }

    @Override public String toString() { return username; }
}
