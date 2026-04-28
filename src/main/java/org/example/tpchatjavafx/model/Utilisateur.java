package org.example.tpchatjavafx.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Utilisateur {
    private int id;
    private String username;
    private String email;
    private String password;
    private String statut;
    private LocalDateTime derniereConnexion;

    public Utilisateur() {}

    public Utilisateur(int id, String username, String email, String password, String statut, LocalDateTime derniereConnexion) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.statut = statut;
        this.derniereConnexion = derniereConnexion;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public LocalDateTime getDerniereConnexion() { return derniereConnexion; }
    public void setDerniereConnexion(LocalDateTime derniereConnexion) { this.derniereConnexion = derniereConnexion; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Utilisateur that = (Utilisateur) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return username; // Important for JavaFX ComboBox/ListView if needed
    }
}
