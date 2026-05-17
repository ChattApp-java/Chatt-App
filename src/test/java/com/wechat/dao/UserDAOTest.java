package com.wechat.dao;

import com.wechat.model.User;
import com.wechat.model.User.UserStatus;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserDAOTest {

    private static UserDAO userDAO;
    private static User testUser;

    @BeforeAll
    static void setUp() {
        userDAO = new UserDAO();
    }

    @Test
    @Order(1)
    @DisplayName("Création d'un utilisateur")
    void testSave() throws SQLException {
        User user = new User("junit_user", "junit@test.com", "hashed_pass", "JUnit User");
        testUser = userDAO.save(user);

        assertNotNull(testUser.getId(), "L'ID doit être généré");
        assertEquals("junit_user", testUser.getUsername());
        System.out.println("✅ testSave : User créé avec ID=" + testUser.getId());
    }

    @Test
    @Order(2)
    @DisplayName("Recherche par ID")
    void testFindById() throws SQLException {
        Optional<User> found = userDAO.findById(testUser.getId());

        assertTrue(found.isPresent(), "L'utilisateur doit être trouvé");
        assertEquals("junit_user", found.get().getUsername());
        System.out.println("✅ testFindById : User trouvé = " + found.get().getNickname());
    }

    @Test
    @Order(3)
    @DisplayName("Recherche par email")
    void testFindByEmail() throws SQLException {
        Optional<User> found = userDAO.findByEmail("junit@test.com");

        assertTrue(found.isPresent());
        assertEquals("JUnit User", found.get().getNickname());
        System.out.println("✅ testFindByEmail : Email trouvé");
    }

    @Test
    @Order(4)
    @DisplayName("Mise à jour du statut")
    void testUpdateStatus() throws SQLException {
        userDAO.updateStatus(testUser.getId(), UserStatus.ONLINE);

        Optional<User> updated = userDAO.findById(testUser.getId());
        assertTrue(updated.isPresent());
        assertEquals(UserStatus.ONLINE, updated.get().getStatus());
        System.out.println("✅ testUpdateStatus : Statut mis à jour = " + updated.get().getStatus());
    }

    @Test
    @Order(5)
    @DisplayName("Liste tous les utilisateurs")
    void testListAll() throws SQLException {
        List<User> users = userDAO.listAll();

        assertFalse(users.isEmpty(), "La liste ne doit pas être vide");
        assertTrue(users.stream().anyMatch(u -> u.getId().equals(testUser.getId())));
        System.out.println("✅ testListAll : " + users.size() + " utilisateurs en base");
    }

    @Test
    @Order(6)
    @DisplayName("Suppression")
    void testDelete() throws SQLException {
        userDAO.delete(testUser.getId());

        Optional<User> deleted = userDAO.findById(testUser.getId());
        assertFalse(deleted.isPresent(), "L'utilisateur doit être supprimé");
        System.out.println("✅ testDelete : User supprimé");
    }
}