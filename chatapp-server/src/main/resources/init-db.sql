-- ============================================================
-- Script d'initialisation de la base de données ChatApp
-- À exécuter dans MySQL (phpMyAdmin, MySQL Workbench, ou CLI)
-- ============================================================

CREATE DATABASE IF NOT EXISTS chattapp
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE chattapp;

-- Table des utilisateurs
CREATE TABLE IF NOT EXISTS users (
    id_user     INT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(32) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,  -- hash BCrypt
    email       VARCHAR(255),
    status      BOOLEAN DEFAULT FALSE,   -- en ligne/hors ligne
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table des messages
CREATE TABLE IF NOT EXISTS messages (
    id_message   INT AUTO_INCREMENT PRIMARY KEY,
    id_sender    INT NOT NULL,
    id_receiver  INT NOT NULL,
    contenu      TEXT NOT NULL,
    date_envoi   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    statut       VARCHAR(20) DEFAULT 'non_lu',  -- non_lu, lu
    FOREIGN KEY (id_sender) REFERENCES users(id_user) ON DELETE CASCADE,
    FOREIGN KEY (id_receiver) REFERENCES users(id_user) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table des appels (audio/vidéo)
CREATE TABLE IF NOT EXISTS appels (
    id_appel     INT AUTO_INCREMENT PRIMARY KEY,
    id_appelant  INT NOT NULL,
    id_receveur  INT NOT NULL,
    type         VARCHAR(10) NOT NULL,         -- AUDIO ou VIDEO
    statut       VARCHAR(20) DEFAULT 'initie', -- initie, accepte, refuse, termine
    date_debut   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_fin     TIMESTAMP NULL,
    duree_sec    INT DEFAULT 0,
    FOREIGN KEY (id_appelant) REFERENCES users(id_user) ON DELETE CASCADE,
    FOREIGN KEY (id_receveur) REFERENCES users(id_user) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Index pour accélérer les requêtes d'historique
CREATE INDEX idx_messages_sender_receiver ON messages(id_sender, id_receiver);
CREATE INDEX idx_messages_receiver_statut ON messages(id_receiver, statut);
CREATE INDEX idx_appels_appelant ON appels(id_appelant);
CREATE INDEX idx_appels_receveur ON appels(id_receveur);

-- ============================================================
-- Instructions d'exécution :
-- 1. Ouvrez phpMyAdmin (http://localhost/phpmyadmin) ou MySQL Workbench
-- 2. Allez dans l'onglet "SQL" ou "Requête"
-- 3. Copiez-collez ce script entier
-- 4. Cliquez sur "Exécuter" / "Go"
-- 5. Vérifiez que les tables "users", "messages" et "appels" apparaissent dans la DB "chattapp"
-- ============================================================

