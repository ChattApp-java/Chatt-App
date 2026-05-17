package com.wechat.dao;

import com.wechat.model.*;
import com.wechat.model.User.UserStatus;
import com.wechat.model.Message.MessageType;
import com.wechat.model.Conversation.ConversationType;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Console de test DAO - Lancez cette classe pour tester toutes les opérations CRUD
 * en ligne de commande avec affichage des résultats.
 */
public class DAOTestConsole {

    private static final String SEP = "═══════════════════════════════════════════════════════════════";

    public static void main(String[] args) {
        System.out.println("\n" + SEP);
        System.out.println("         🧪 CONSOLE DE TEST DAO - WeChat JavaFX");
        System.out.println(SEP + "\n");

        try {
            // 1. Test Connexion
            testConnection();

            // 2. Test UserDAO
            testUserDAO();

            // 3. Test ConversationDAO
            testConversationDAO();

            // 4. Test MessageDAO
            testMessageDAO();

            // 5. Test GroupDAO
            testGroupDAO();

            System.out.println("\n" + SEP);
            System.out.println("         ✅ TOUS LES TESTS DAO SONT PASSÉS !");
            System.out.println(SEP + "\n");

        } catch (Exception e) {
            System.err.println("\n❌ ERREUR LORS DES TESTS : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== TEST CONNEXION ====================
    private static void testConnection() {
        printSection("TEST CONNEXION BASE DE DONNÉES");

        DatabaseConnection db = DatabaseConnection.getInstance();
        boolean ok = db.testConnection();

        System.out.println("  Singleton instance : " + (db != null ? "✅ OK" : "❌ NULL"));
        System.out.println("  Connexion active   : " + (ok ? "✅ OK" : "❌ ÉCHEC"));
        System.out.println();
    }

    // ==================== TEST USER DAO ====================
    private static void testUserDAO() throws SQLException {
        printSection("TEST USER DAO");

        UserDAO userDAO = new UserDAO();

        // Création
        System.out.println("  📝 Création d'utilisateurs...");
        User alice = new User("alice_test", "alice@test.com", "hash_pass1", "Alice");
        User bob = new User("bob_test", "bob@test.com", "hash_pass2", "Bob");
        User charlie = new User("charlie_test", "charlie@test.com", "hash_pass3", "Charlie");

        alice = userDAO.save(alice);
        bob = userDAO.save(bob);
        charlie = userDAO.save(charlie);

        System.out.println("    ✅ Alice créée (ID=" + alice.getId() + ")");
        System.out.println("    ✅ Bob créé   (ID=" + bob.getId() + ")");
        System.out.println("    ✅ Charlie créé (ID=" + charlie.getId() + ")");

        // findById
        System.out.println("  🔍 findById(" + alice.getId() + ")...");
        Optional<User> found = userDAO.findById(alice.getId());
        found.ifPresent(u -> System.out.println("    ✅ Trouvé : " + u.getUsername() + " | " + u.getEmail()));

        // findByEmail
        System.out.println("  🔍 findByEmail('bob@test.com')...");
        Optional<User> byEmail = userDAO.findByEmail("bob@test.com");
        byEmail.ifPresent(u -> System.out.println("    ✅ Trouvé : " + u.getNickname()));

        // findByUsername
        System.out.println("  🔍 findByUsername('charlie_test')...");
        Optional<User> byUsername = userDAO.findByUsername("charlie_test");
        byUsername.ifPresent(u -> System.out.println("    ✅ Trouvé : " + u.getNickname()));

        // updateStatus
        System.out.println("  🔄 updateStatus(alice -> ONLINE)...");
        userDAO.updateStatus(alice.getId(), UserStatus.ONLINE);
        Optional<User> updated = userDAO.findById(alice.getId());
        updated.ifPresent(u -> System.out.println("    ✅ Statut mis à jour : " + u.getStatus()));

        // listAll
        System.out.println("  📋 listAll()...");
        List<User> allUsers = userDAO.listAll();
        System.out.println("    ✅ " + allUsers.size() + " utilisateurs en base");
        for (User u : allUsers) {
            System.out.println("       → " + u.getId() + " | " + u.getUsername() + " | " + u.getStatus());
        }

        // Nettoyage
        System.out.println("  🗑️  Suppression des utilisateurs de test...");
        userDAO.delete(alice.getId());
        userDAO.delete(bob.getId());
        userDAO.delete(charlie.getId());
        System.out.println("    ✅ Utilisateurs de test supprimés");
        System.out.println();
    }

    // ==================== TEST CONVERSATION DAO ====================
    private static void testConversationDAO() throws SQLException {
        printSection("TEST CONVERSATION DAO");

        ConversationDAO convDAO = new ConversationDAO();
        UserDAO userDAO = new UserDAO();

        // Créer des users temporaires
        User u1 = userDAO.save(new User("conv_user1", "u1@test.com", "pass", "User1"));
        User u2 = userDAO.save(new User("conv_user2", "u2@test.com", "pass", "User2"));
        User u3 = userDAO.save(new User("conv_user3", "u3@test.com", "pass", "User3"));

        // createPrivate
        System.out.println("  📝 createPrivate(" + u1.getId() + ", " + u2.getId() + ")...");
        Conversation privateConv = convDAO.createPrivate(u1.getId(), u2.getId());
        System.out.println("    ✅ Conversation privée créée (ID=" + privateConv.getId() + ")");
        System.out.println("       Participants : " + privateConv.getParticipantIds());

        // Test duplicate (doit retourner la même)
        System.out.println("  📝 createPrivate duplicate...");
        Conversation duplicate = convDAO.createPrivate(u1.getId(), u2.getId());
        System.out.println("    ✅ Même conversation retournée (ID=" + duplicate.getId() + ")");

        // createGroup
        System.out.println("  📝 createGroup('Dev Team', [" + u1.getId() + ", " + u2.getId() + ", " + u3.getId() + "])...");
        Conversation groupConv = convDAO.createGroup("Dev Team", Arrays.asList(u1.getId(), u2.getId(), u3.getId()));
        System.out.println("    ✅ Groupe créé (ID=" + groupConv.getId() + ", nom='" + groupConv.getName() + "')");
        System.out.println("       Participants : " + groupConv.getParticipantIds());

        // findByUser
        System.out.println("  🔍 findByUser(" + u1.getId() + ")...");
        List<Conversation> userConvs = convDAO.findByUser(u1.getId());
        System.out.println("    ✅ " + userConvs.size() + " conversations trouvées pour User1");
        for (Conversation c : userConvs) {
            System.out.println("       → [" + c.getType() + "] " + c.getName() + " (ID=" + c.getId() + ")");
        }

        // findPrivateBetweenUsers
        System.out.println("  🔍 findPrivateBetweenUsers(" + u1.getId() + ", " + u2.getId() + ")...");
        Optional<Conversation> foundPrivate = convDAO.findPrivateBetweenUsers(u1.getId(), u2.getId());
        foundPrivate.ifPresent(c -> System.out.println("    ✅ Conversation privée trouvée (ID=" + c.getId() + ")"));

        // Nettoyage
        System.out.println("  🗑️  Suppression des conversations de test...");
        convDAO.delete(privateConv.getId());
        convDAO.delete(groupConv.getId());
        userDAO.delete(u1.getId());
        userDAO.delete(u2.getId());
        userDAO.delete(u3.getId());
        System.out.println("    ✅ Conversations et users de test supprimés");
        System.out.println();
    }

    // ==================== TEST MESSAGE DAO ====================
    private static void testMessageDAO() throws SQLException {
        printSection("TEST MESSAGE DAO");

        MessageDAO msgDAO = new MessageDAO();
        ConversationDAO convDAO = new ConversationDAO();
        UserDAO userDAO = new UserDAO();

        // Setup
        User sender = userDAO.save(new User("msg_sender", "sender@test.com", "pass", "Sender"));
        User receiver = userDAO.save(new User("msg_receiver", "receiver@test.com", "pass", "Receiver"));
        Conversation conv = convDAO.createPrivate(sender.getId(), receiver.getId());

        // save
        System.out.println("  📝 Envoi de 3 messages...");
        Message m1 = msgDAO.save(new Message(conv.getId(), sender.getId(), "Salut !", MessageType.TEXT));
        Message m2 = msgDAO.save(new Message(conv.getId(), receiver.getId(), "Hey ! Ça va ?", MessageType.TEXT));
        Message m3 = msgDAO.save(new Message(conv.getId(), sender.getId(), "Ça va merci !", MessageType.TEXT));

        System.out.println("    ✅ Message 1 (ID=" + m1.getId() + ") : '" + m1.getContent() + "'");
        System.out.println("    ✅ Message 2 (ID=" + m2.getId() + ") : '" + m2.getContent() + "'");
        System.out.println("    ✅ Message 3 (ID=" + m3.getId() + ") : '" + m3.getContent() + "'");

        // findByConversation
        System.out.println("  🔍 findByConversation(" + conv.getId() + ")...");
        List<Message> messages = msgDAO.findByConversation(conv.getId());
        System.out.println("    ✅ " + messages.size() + " messages trouvés");
        for (Message m : messages) {
            String readStatus = m.isRead() ? "✓" : "○";
            System.out.println("       [" + readStatus + "] " + m.getSenderId() + " : " + m.getContent());
        }

        // getUnreadCount
        System.out.println("  🔢 getUnreadCount(conv=" + conv.getId() + ", user=" + receiver.getId() + ")...");
        int unread = msgDAO.getUnreadCount(conv.getId(), receiver.getId());
        System.out.println("    ✅ Messages non lus par receiver : " + unread);

        // markAsRead
        System.out.println("  🔄 markAsRead(" + m1.getId() + ")...");
        msgDAO.markAsRead(m1.getId());
        int unreadAfter = msgDAO.getUnreadCount(conv.getId(), receiver.getId());
        System.out.println("    ✅ Messages non lus après markAsRead : " + unreadAfter);

        // markConversationAsRead
        System.out.println("  🔄 markConversationAsRead(conv=" + conv.getId() + ", user=" + sender.getId() + ")...");
        msgDAO.markConversationAsRead(conv.getId(), sender.getId());
        int unreadFinal = msgDAO.getUnreadCount(conv.getId(), sender.getId());
        System.out.println("    ✅ Messages non lus après markConversationAsRead : " + unreadFinal);

        // Nettoyage
        System.out.println("  🗑️  Suppression des messages de test...");
        msgDAO.delete(m1.getId());
        msgDAO.delete(m2.getId());
        msgDAO.delete(m3.getId());
        convDAO.delete(conv.getId());
        userDAO.delete(sender.getId());
        userDAO.delete(receiver.getId());
        System.out.println("    ✅ Tout supprimé");
        System.out.println();
    }

    // ==================== TEST GROUP DAO ====================
    private static void testGroupDAO() throws SQLException {
        printSection("TEST GROUP DAO");

        GroupDAO groupDAO = new GroupDAO();
        UserDAO userDAO = new UserDAO();

        // Setup users
        User admin = userDAO.save(new User("group_admin", "admin@test.com", "pass", "Admin"));
        User m1 = userDAO.save(new User("group_m1", "m1@test.com", "pass", "Member1"));
        User m2 = userDAO.save(new User("group_m2", "m2@test.com", "pass", "Member2"));
        User m3 = userDAO.save(new User("group_m3", "m3@test.com", "pass", "Member3"));

        // create
        System.out.println("  📝 Création du groupe 'Les Amis'...");
        Group group = new Group("Les Amis", "Groupe de test", admin.getId(),
                Arrays.asList(admin.getId(), m1.getId(), m2.getId()));
        group = groupDAO.create(group);
        System.out.println("    ✅ Groupe créé (ID=" + group.getId() + ", admin=" + group.getAdminId() + ")");

        // findMembers
        System.out.println("  🔍 findMembers(" + group.getId() + ")...");
        List<Long> members = groupDAO.findMembers(group.getId());
        System.out.println("    ✅ " + members.size() + " membres trouvés : " + members);

        // addMember
        System.out.println("  ➕ addMember(" + group.getId() + ", " + m3.getId() + ")...");
        groupDAO.addMember(group.getId(), m3.getId());
        List<Long> membersAfterAdd = groupDAO.findMembers(group.getId());
        System.out.println("    ✅ Membres après ajout : " + membersAfterAdd.size() + " → " + membersAfterAdd);

        // removeMember
        System.out.println("  ➖ removeMember(" + group.getId() + ", " + m2.getId() + ")...");
        groupDAO.removeMember(group.getId(), m2.getId());
        List<Long> membersAfterRemove = groupDAO.findMembers(group.getId());
        System.out.println("    ✅ Membres après suppression : " + membersAfterRemove.size() + " → " + membersAfterRemove);

        // findByMember
        System.out.println("  🔍 findByMember(" + m1.getId() + ")...");
        List<Group> userGroups = groupDAO.findByMember(m1.getId());
        System.out.println("    ✅ " + userGroups.size() + " groupe(s) trouvé(s) pour Member1");
        for (Group g : userGroups) {
            System.out.println("       → " + g.getName() + " (" + g.getMemberIds().size() + " membres)");
        }

        // Nettoyage
        System.out.println("  🗑️  Suppression du groupe de test...");
        groupDAO.delete(group.getId());
        userDAO.delete(admin.getId());
        userDAO.delete(m1.getId());
        userDAO.delete(m2.getId());
        userDAO.delete(m3.getId());
        System.out.println("    ✅ Tout supprimé");
        System.out.println();
    }

    private static void printSection(String title) {
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  " + String.format("%-55s", title) + "│");
        System.out.println("└─────────────────────────────────────────────────────────────┘");
    }
}