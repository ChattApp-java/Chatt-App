CREATE DATABASE IF NOT EXISTS wechat CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE wechat;

CREATE TABLE IF NOT EXISTS utilisateur (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    statut VARCHAR(30) DEFAULT 'NON_CONNECTE',
    derniereConnexion DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS contact (
    id INT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id INT NOT NULL,
    contact_id INT NOT NULL,
    dateAjout DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id) ON DELETE CASCADE,
    FOREIGN KEY (contact_id) REFERENCES utilisateur (id) ON DELETE CASCADE,
    UNIQUE KEY unique_contact (utilisateur_id, contact_id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS groupe (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    createur_id INT NOT NULL,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (createur_id) REFERENCES utilisateur (id) ON DELETE CASCADE,
    INDEX idx_groupe_nom (nom)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS groupe_membre (
    id INT AUTO_INCREMENT PRIMARY KEY,
    groupe_id INT NOT NULL,
    utilisateur_id INT NOT NULL,
    date_ajout TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    role VARCHAR(20) DEFAULT 'MEMBRE',
    FOREIGN KEY (groupe_id) REFERENCES groupe (id) ON DELETE CASCADE,
    FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id) ON DELETE CASCADE,
    UNIQUE KEY unique_groupe_membre (groupe_id, utilisateur_id),
    INDEX idx_groupe_membre_user (utilisateur_id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS conversation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(20) DEFAULT 'INDIVIDUEL',
    groupe_id INT NULL,
    dateCreation DATETIME DEFAULT CURRENT_TIMESTAMP,
    derniereModification DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (groupe_id) REFERENCES groupe (id) ON DELETE SET NULL,
    INDEX idx_conversation_groupe (groupe_id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS participant_conversation (
    conversation_id INT NOT NULL,
    utilisateur_id INT NOT NULL,
    PRIMARY KEY (conversation_id, utilisateur_id),
    FOREIGN KEY (conversation_id) REFERENCES conversation (id) ON DELETE CASCADE,
    FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS conversation_clear_state (
    utilisateur_id INT NOT NULL,
    conversation_id INT NOT NULL,
    cleared_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (utilisateur_id, conversation_id),
    FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id) ON DELETE CASCADE,
    FOREIGN KEY (conversation_id) REFERENCES conversation (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS groupe_clear_state (
    utilisateur_id INT NOT NULL,
    groupe_id INT NOT NULL,
    cleared_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (utilisateur_id, groupe_id),
    FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id) ON DELETE CASCADE,
    FOREIGN KEY (groupe_id) REFERENCES groupe (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS reunion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    groupe_id INT NOT NULL,
    initiateur_id INT NOT NULL,
    type VARCHAR(20) NOT NULL,
    statut VARCHAR(20) DEFAULT 'EN_COURS',
    date_debut TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_fin TIMESTAMP NULL,
    FOREIGN KEY (groupe_id) REFERENCES groupe (id) ON DELETE CASCADE,
    FOREIGN KEY (initiateur_id) REFERENCES utilisateur (id) ON DELETE CASCADE,
    INDEX idx_reunion_active (groupe_id, statut)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS reunion_participant (
    id INT AUTO_INCREMENT PRIMARY KEY,
    reunion_id INT NOT NULL,
    utilisateur_id INT NOT NULL,
    date_join TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_leave TIMESTAMP NULL,
    FOREIGN KEY (reunion_id) REFERENCES reunion (id) ON DELETE CASCADE,
    FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id) ON DELETE CASCADE,
    INDEX idx_reunion_participant_active (reunion_id, date_leave)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS message (
    id INT AUTO_INCREMENT PRIMARY KEY,
    contenu TEXT,
    dateEnvoi DATETIME DEFAULT CURRENT_TIMESTAMP,
    type VARCHAR(50) DEFAULT 'PRIVATE',
    expediteur_id INT NOT NULL,
    destinataire_id INT NULL,
    conversation_id INT NULL,
    groupe_id INT NULL,
    reunion_id INT NULL,
    estLu BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (expediteur_id) REFERENCES utilisateur (id) ON DELETE CASCADE,
    FOREIGN KEY (destinataire_id) REFERENCES utilisateur (id) ON DELETE SET NULL,
    FOREIGN KEY (conversation_id) REFERENCES conversation (id) ON DELETE CASCADE,
    FOREIGN KEY (groupe_id) REFERENCES groupe (id) ON DELETE CASCADE,
    FOREIGN KEY (reunion_id) REFERENCES reunion (id) ON DELETE SET NULL,
    INDEX idx_dateEnvoi (dateEnvoi),
    INDEX idx_message_groupe (groupe_id, dateEnvoi)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS message_clear_state (
    utilisateur_id INT NOT NULL,
    message_id INT NOT NULL,
    cleared_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (utilisateur_id, message_id),
    FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id) ON DELETE CASCADE,
    FOREIGN KEY (message_id) REFERENCES message (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS message_delete_for_me (
    utilisateur_id INT NOT NULL,
    message_id INT NOT NULL,
    deleted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (utilisateur_id, message_id),
    FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id) ON DELETE CASCADE,
    FOREIGN KEY (message_id) REFERENCES message (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS message_delete_for_everyone (
    id INT AUTO_INCREMENT PRIMARY KEY,
    message_id INT NULL,
    deleted_by_id INT NOT NULL,
    conversation_id INT NULL,
    groupe_id INT NULL,
    original_type VARCHAR(50),
    deleted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (deleted_by_id) REFERENCES utilisateur (id) ON DELETE CASCADE,
    FOREIGN KEY (conversation_id) REFERENCES conversation (id) ON DELETE SET NULL,
    FOREIGN KEY (groupe_id) REFERENCES groupe (id) ON DELETE SET NULL,
    INDEX idx_delete_everyone_message (message_id),
    INDEX idx_delete_everyone_conversation (conversation_id),
    INDEX idx_delete_everyone_groupe (groupe_id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS message_selection (
    id INT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id INT NOT NULL,
    message_id INT NOT NULL,
    selection_group VARCHAR(64) NOT NULL,
    selected_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id) ON DELETE CASCADE,
    FOREIGN KEY (message_id) REFERENCES message (id) ON DELETE CASCADE,
    INDEX idx_message_selection_user_group (utilisateur_id, selection_group),
    INDEX idx_message_selection_message (message_id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS fichier_media (
    id INT AUTO_INCREMENT PRIMARY KEY,
    message_id INT NOT NULL,
    nom_fichier VARCHAR(255),
    chemin_acces VARCHAR(500),
    taille BIGINT,
    type VARCHAR(50),
    date_ajout DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES message (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS vocal (
    id INT PRIMARY KEY,
    duree INT,
    FOREIGN KEY (id) REFERENCES fichier_media (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS video (
    id INT PRIMARY KEY,
    resolution VARCHAR(50),
    duree INT,
    FOREIGN KEY (id) REFERENCES fichier_media (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS appel (
    id INT AUTO_INCREMENT PRIMARY KEY,
    type_appel VARCHAR(50),
    date_heure DATETIME DEFAULT CURRENT_TIMESTAMP,
    duree INT DEFAULT 0,
    statut VARCHAR(30) NOT NULL,
    expediteur_id INT NOT NULL,
    destinataire_id INT NULL,
    reunion_id INT NULL,
    est_reunion BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (expediteur_id) REFERENCES utilisateur (id) ON DELETE CASCADE,
    FOREIGN KEY (destinataire_id) REFERENCES utilisateur (id) ON DELETE CASCADE,
    FOREIGN KEY (reunion_id) REFERENCES reunion (id) ON DELETE SET NULL
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS notification (
    id INT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id INT NOT NULL,
    contenu VARCHAR(255),
    type VARCHAR(50) NOT NULL,
    est_lue BOOLEAN DEFAULT FALSE,
    date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS connexion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id INT NOT NULL,
    socketId VARCHAR(255),
    estEnLigne BOOLEAN DEFAULT FALSE,
    dateConnexion DATETIME DEFAULT CURRENT_TIMESTAMP,
    dateDeconnexion DATETIME,
    FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id) ON DELETE CASCADE
) ENGINE = InnoDB;
