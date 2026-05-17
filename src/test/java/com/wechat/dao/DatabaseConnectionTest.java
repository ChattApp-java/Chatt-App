package com.wechat.dao;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DatabaseConnectionTest {

    @Test
    public void testSingletonInstance() {
        DatabaseConnection instance1 = DatabaseConnection.getInstance();
        DatabaseConnection instance2 = DatabaseConnection.getInstance();

        assertNotNull(instance1, "L'instance ne doit pas être null");
        assertSame(instance1, instance2, "Les deux instances doivent être identiques (Singleton)");
    }

    @Test
    public void testConnectionNotNull() {
        DatabaseConnection instance = DatabaseConnection.getInstance();
        assertNotNull(instance.getConnection(), "La connexion ne doit pas être null");
    }

    @Test
    public void testConnectionIsOpen() {
        DatabaseConnection instance = DatabaseConnection.getInstance();
        assertTrue(instance.testConnection(), "La connexion doit être ouverte et fonctionnelle");
    }

    @Test
    public void testMultipleCallsSameConnection() {
        DatabaseConnection instance = DatabaseConnection.getInstance();
        var conn1 = instance.getConnection();
        var conn2 = instance.getConnection();

        assertNotNull(conn1);
        assertNotNull(conn2);
    }
}