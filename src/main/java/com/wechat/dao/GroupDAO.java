package com.wechat.dao;

import com.wechat.model.Group;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GroupDAO {

    private final Connection connection;

    public GroupDAO() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    public Group create(Group group) throws SQLException {
        String sql = "INSERT INTO groups (name, description, admin_id, member_ids, avatar, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, group.getName());
            stmt.setString(2, group.getDescription());
            stmt.setLong(3, group.getAdminId());
            stmt.setString(4, group.getMemberIds().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
            stmt.setString(5, group.getAvatar());
            stmt.setTimestamp(6, Timestamp.valueOf(group.getCreatedAt()));

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    group.setId(rs.getLong(1));
                }
            }
        }
        return group;
    }

    public Optional<Group> findById(Long id) throws SQLException {
        String sql = "SELECT * FROM groups WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Group> findByMember(Long userId) throws SQLException {
        List<Group> groups = new ArrayList<>();
        String sql = "SELECT * FROM groups WHERE FIND_IN_SET(?, member_ids) ORDER BY name";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    groups.add(mapResultSet(rs));
                }
            }
        }
        return groups;
    }

    public List<Group> findAll() throws SQLException {
        List<Group> groups = new ArrayList<>();
        String sql = "SELECT * FROM groups ORDER BY name";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                groups.add(mapResultSet(rs));
            }
        }
        return groups;
    }

    public void addMember(Long groupId, Long userId) throws SQLException {
        Optional<Group> opt = findById(groupId);
        if (opt.isPresent()) {
            Group group = opt.get();
            group.addMember(userId);
            updateMembers(groupId, group.getMemberIds());
        }
    }

    public void removeMember(Long groupId, Long userId) throws SQLException {
        Optional<Group> opt = findById(groupId);
        if (opt.isPresent()) {
            Group group = opt.get();
            group.removeMember(userId);
            updateMembers(groupId, group.getMemberIds());
        }
    }

    public List<Long> findMembers(Long groupId) throws SQLException {
        Optional<Group> opt = findById(groupId);
        return opt.map(Group::getMemberIds).orElse(new ArrayList<>());
    }

    public void updateMembers(Long groupId, List<Long> memberIds) throws SQLException {
        String sql = "UPDATE groups SET member_ids = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, memberIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
            stmt.setLong(2, groupId);
            stmt.executeUpdate();
        }
    }

    public void updateAdmin(Long groupId, Long adminId) throws SQLException {
        String sql = "UPDATE groups SET admin_id = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, adminId);
            stmt.setLong(2, groupId);
            stmt.executeUpdate();
        }
    }

    public void delete(Long id) throws SQLException {
        String sql = "DELETE FROM groups WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }

    private Group mapResultSet(ResultSet rs) throws SQLException {
        Group group = new Group();
        group.setId(rs.getLong("id"));
        group.setName(rs.getString("name"));
        group.setDescription(rs.getString("description"));
        group.setAdminId(rs.getLong("admin_id"));

        String memberStr = rs.getString("member_ids");
        if (memberStr != null && !memberStr.isEmpty()) {
            List<Long> ids = Arrays.stream(memberStr.split(","))
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            group.setMemberIds(ids);
        }

        group.setAvatar(rs.getString("avatar"));
        group.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return group;
    }
}