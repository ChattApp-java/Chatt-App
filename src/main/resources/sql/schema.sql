-- ============================================================
-- WhatsApp JavaFX — Schéma de base de données
-- MySQL port 3306
-- Exécuter : mysql -u root < schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS chattapp
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE chattapp;

-- ── Table des utilisateurs ──────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    last_login    TIMESTAMP    NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Table des messages ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS messages (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    sender_id   INT          NOT NULL,
    receiver_id INT          NOT NULL,
    content     TEXT         NOT NULL,
    sent_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id)   REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_conversation (sender_id, receiver_id),
    INDEX idx_sent_at (sent_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Table des appels ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS call_logs (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    caller_id  INT  NOT NULL,
    callee_id  INT  NOT NULL,
    call_type  ENUM('audio','video') NOT NULL,
    status     ENUM('missed','accepted','rejected') NOT NULL DEFAULT 'missed',
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    duration_s INT DEFAULT 0,
    FOREIGN KEY (caller_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (callee_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
