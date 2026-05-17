package com.wechat.dao;

import com.wechat.model.*;
import com.wechat.model.Message.MessageType;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MessageDAOTest {

    private static MessageDAO messageDAO;
    private static ConversationDAO conversationDAO;
    private static UserDAO userDAO;
    private static User sender;
    private static User receiver;
    private static Conversation conversation;
    private static Message testMessage;

    @BeforeAll
    static void setUp() throws SQLException {
        messageDAO = new MessageDAO();
        conversationDAO = new ConversationDAO();
        userDAO = new UserDAO();

        sender = userDAO.save(new User("msg_sender_test", "sender_t@test.com", "pass", "Sender"));
        receiver = userDAO.save(new User("msg_receiver_test", "receiver_t@test.com", "pass", "Receiver"));
        conversation = conversationDAO.createPrivate(sender.getId(), receiver.getId());
    }

    @Test
    @Order(1)
    @DisplayName("Sauvegarde d'un message")
    void testSave() throws SQLException {
        Message msg = new Message(conversation.getId(), sender.getId(), "Hello JUnit!", MessageType.TEXT);
        testMessage = messageDAO.save(msg);

        assertNotNull(testMessage.getId());
        assertEquals("Hello JUnit!", testMessage.getContent());
        System.out.println("✅ testSave : Message créé ID=" + testMessage.getId());
    }

    @Test
    @Order(2)
    @DisplayName("Recherche par conversation")
    void testFindByConversation() throws SQLException {
        List<Message> messages = messageDAO.findByConversation(conversation.getId());

        assertFalse(messages.isEmpty());
        assertEquals(1, messages.size());
        System.out.println("✅ testFindByConversation : " + messages.size() + " message(s)");
    }

    @Test
    @Order(3)
    @DisplayName("Compteur de messages non lus")
    void testGetUnreadCount() throws SQLException {
        int unread = messageDAO.getUnreadCount(conversation.getId(), receiver.getId());

        assertTrue(unread >= 1, "Doit avoir au moins 1 message non lu");
        System.out.println("✅ testGetUnreadCount : " + unread + " messages non lus");
    }

    @Test
    @Order(4)
    @DisplayName("Marquer comme lu")
    void testMarkAsRead() throws SQLException {
        messageDAO.markAsRead(testMessage.getId());

        int unreadAfter = messageDAO.getUnreadCount(conversation.getId(), receiver.getId());
        assertEquals(0, unreadAfter, "Le message doit être marqué comme lu");
        System.out.println("✅ testMarkAsRead : Message marqué comme lu");
    }

    @AfterAll
    static void tearDown() throws SQLException {
        messageDAO.delete(testMessage.getId());
        conversationDAO.delete(conversation.getId());
        userDAO.delete(sender.getId());
        userDAO.delete(receiver.getId());
        System.out.println("✅ tearDown : Nettoyage terminé");
    }
}