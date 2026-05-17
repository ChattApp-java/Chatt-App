package com.wechat.dao;

import com.wechat.model.Conversation;
import com.wechat.model.User;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ConversationDAOTest {

    private static ConversationDAO conversationDAO;
    private static UserDAO userDAO;
    private static User u1, u2, u3;
    private static Conversation privateConv;
    private static Conversation groupConv;

    @BeforeAll
    static void setUp() throws SQLException {
        conversationDAO = new ConversationDAO();
        userDAO = new UserDAO();

        u1 = userDAO.save(new User("conv_u1_test", "u1_t@test.com", "pass", "User1"));
        u2 = userDAO.save(new User("conv_u2_test", "u2_t@test.com", "pass", "User2"));
        u3 = userDAO.save(new User("conv_u3_test", "u3_t@test.com", "pass", "User3"));
    }

    @Test
    @Order(1)
    @DisplayName("Créer conversation privée")
    void testCreatePrivate() throws SQLException {
        privateConv = conversationDAO.createPrivate(u1.getId(), u2.getId());

        assertNotNull(privateConv.getId());
        assertEquals(2, privateConv.getParticipantIds().size());
        System.out.println("✅ testCreatePrivate : Conv privée ID=" + privateConv.getId());
    }

    @Test
    @Order(2)
    @DisplayName("Créer groupe")
    void testCreateGroup() throws SQLException {
        groupConv = conversationDAO.createGroup("Test Group", Arrays.asList(u1.getId(), u2.getId(), u3.getId()));

        assertNotNull(groupConv.getId());
        assertEquals("Test Group", groupConv.getName());
        assertEquals(3, groupConv.getParticipantIds().size());
        System.out.println("✅ testCreateGroup : Groupe ID=" + groupConv.getId());
    }

    @Test
    @Order(3)
    @DisplayName("Recherche par utilisateur")
    void testFindByUser() throws SQLException {
        List<Conversation> convs = conversationDAO.findByUser(u1.getId());

        assertTrue(convs.size() >= 2, "User1 doit être dans au moins 2 conversations");
        System.out.println("✅ testFindByUser : " + convs.size() + " conversation(s) pour User1");
    }

    @Test
    @Order(4)
    @DisplayName("Trouver conversation privée entre 2 users")
    void testFindPrivateBetweenUsers() throws SQLException {
        Optional<Conversation> found = conversationDAO.findPrivateBetweenUsers(u1.getId(), u2.getId());

        assertTrue(found.isPresent());
        assertEquals(privateConv.getId(), found.get().getId());
        System.out.println("✅ testFindPrivateBetweenUsers : Conv trouvée ID=" + found.get().getId());
    }

    @Test
    @Order(5)
    @DisplayName("Éviter duplicate conversation privée")
    void testNoDuplicatePrivate() throws SQLException {
        Conversation duplicate = conversationDAO.createPrivate(u1.getId(), u2.getId());

        assertEquals(privateConv.getId(), duplicate.getId(), "Doit retourner la même conversation");
        System.out.println("✅ testNoDuplicatePrivate : Pas de duplicate");
    }

    @AfterAll
    static void tearDown() throws SQLException {
        if (privateConv != null) conversationDAO.delete(privateConv.getId());
        if (groupConv != null) conversationDAO.delete(groupConv.getId());
        userDAO.delete(u1.getId());
        userDAO.delete(u2.getId());
        userDAO.delete(u3.getId());
        System.out.println("✅ tearDown : Nettoyage terminé");
    }
}