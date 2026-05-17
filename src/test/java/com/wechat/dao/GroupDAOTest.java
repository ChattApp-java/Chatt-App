package com.wechat.dao;

import com.wechat.model.Group;
import com.wechat.model.User;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GroupDAOTest {

    private static GroupDAO groupDAO;
    private static UserDAO userDAO;
    private static User admin, m1, m2, m3;
    private static Group testGroup;

    @BeforeAll
    static void setUp() throws SQLException {
        groupDAO = new GroupDAO();
        userDAO = new UserDAO();

        admin = userDAO.save(new User("grp_admin_test", "admin_t@test.com", "pass", "Admin"));
        m1 = userDAO.save(new User("grp_m1_test", "m1_t@test.com", "pass", "M1"));
        m2 = userDAO.save(new User("grp_m2_test", "m2_t@test.com", "pass", "M2"));
        m3 = userDAO.save(new User("grp_m3_test", "m3_t@test.com", "pass", "M3"));
    }

    @Test
    @Order(1)
    @DisplayName("Créer un groupe")
    void testCreate() throws SQLException {
        Group group = new Group("JUnit Group", "Desc test", admin.getId(),
                Arrays.asList(admin.getId(), m1.getId(), m2.getId()));
        testGroup = groupDAO.create(group);

        assertNotNull(testGroup.getId());
        assertEquals("JUnit Group", testGroup.getName());
        assertEquals(3, testGroup.getMemberIds().size());
        System.out.println("✅ testCreate : Groupe ID=" + testGroup.getId());
    }

    @Test
    @Order(2)
    @DisplayName("Trouver membres")
    void testFindMembers() throws SQLException {
        List<Long> members = groupDAO.findMembers(testGroup.getId());

        assertEquals(3, members.size());
        assertTrue(members.contains(admin.getId()));
        System.out.println("✅ testFindMembers : " + members.size() + " membres");
    }

    @Test
    @Order(3)
    @DisplayName("Ajouter membre")
    void testAddMember() throws SQLException {
        groupDAO.addMember(testGroup.getId(), m3.getId());

        List<Long> members = groupDAO.findMembers(testGroup.getId());
        assertEquals(4, members.size());
        assertTrue(members.contains(m3.getId()));
        System.out.println("✅ testAddMember : Membre ajouté, total=" + members.size());
    }

    @Test
    @Order(4)
    @DisplayName("Retirer membre")
    void testRemoveMember() throws SQLException {
        groupDAO.removeMember(testGroup.getId(), m2.getId());

        List<Long> members = groupDAO.findMembers(testGroup.getId());
        assertEquals(3, members.size());
        assertFalse(members.contains(m2.getId()));
        System.out.println("✅ testRemoveMember : Membre retiré, total=" + members.size());
    }

    @Test
    @Order(5)
    @DisplayName("Trouver groupes par membre")
    void testFindByMember() throws SQLException {
        List<Group> groups = groupDAO.findByMember(m1.getId());

        assertFalse(groups.isEmpty());
        assertTrue(groups.stream().anyMatch(g -> g.getId().equals(testGroup.getId())));
        System.out.println("✅ testFindByMember : " + groups.size() + " groupe(s) pour M1");
    }

    @AfterAll
    static void tearDown() throws SQLException {
        if (testGroup != null) groupDAO.delete(testGroup.getId());
        userDAO.delete(admin.getId());
        userDAO.delete(m1.getId());
        userDAO.delete(m2.getId());
        userDAO.delete(m3.getId());
        System.out.println("✅ tearDown : Nettoyage terminé");
    }
}