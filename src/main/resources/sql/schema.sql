-- Schema MySQL pour l'application de Chat JavaFX

CREATE DATABASE IF NOT EXISTS wechat CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE wechat;

-- 1. Table Utilisateur
CREATE TABLE IF NOT EXISTS utilisateur (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    statut ENUM('EN_LIGNE', 'NON_CONNECTE') DEFAULT 'NON_CONNECTE',
    derniereConnexion DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB;

-- 2. Table Contact (Many-to-Many Utilisateur-Utilisateur)
CREATE TABLE IF NOT EXISTS contact (
    id INT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id INT NOT NULL,
    contact_id INT NOT NULL,
    dateAjout DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id) ON DELETE CASCADE,
    FOREIGN KEY (contact_id) REFERENCES utilisateur(id) ON DELETE CASCADE,
    UNIQUE KEY unique_contact (utilisateur_id, contact_id)
) ENGINE=InnoDB;

-- 3. Table Conversation
CREATE TABLE IF NOT EXISTS conversation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    dateCreation DATETIME DEFAULT CURRENT_TIMESTAMP,
    derniereModification DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 4. Table Participant_Conversation
CREATE TABLE IF NOT EXISTS participant_conversation (
    conversation_id INT NOT NULL,
    utilisateur_id INT NOT NULL,
    PRIMARY KEY (conversation_id, utilisateur_id),
    FOREIGN KEY (conversation_id) REFERENCES conversation(id) ON DELETE CASCADE,
    FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 5. Table Message
CREATE TABLE IF NOT EXISTS message (
    id INT AUTO_INCREMENT PRIMARY KEY,
    contenu TEXT,
    dateEnvoi DATETIME DEFAULT CURRENT_TIMESTAMP,
    type ENUM('TEXTE', 'AUDIO', 'VIDEO', 'SYSTEME') DEFAULT 'TEXTE',
    expediteur_id INT NOT NULL,
    destinataire_id INT, -- Optionnel si c'est pour un groupe, mais selon les contraintes : message privé
    conversation_id INT NOT NULL,
    estLu BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (expediteur_id) REFERENCES utilisateur(id) ON DELETE CASCADE,
    FOREIGN KEY (destinataire_id) REFERENCES utilisateur(id) ON DELETE SET NULL,
    FOREIGN KEY (conversation_id) REFERENCES conversation(id) ON DELETE CASCADE,
    INDEX idx_dateEnvoi (dateEnvoi)
) ENGINE=InnoDB;

-- 6. Table Fichier_Media (Héritage Vocal et Video via type)
CREATE TABLE IF NOT EXISTS fichier_media (
    id INT AUTO_INCREMENT PRIMARY KEY,
    type ENUM('VOCAL', 'VIDEO', 'IMAGE', 'AUTRE') NOT NULL,
    cheminFichier VARCHAR(255) NOT NULL,
    message_id INT NOT NULL,
    dateCreation DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES message(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 7. Table Appel
CREATE TABLE IF NOT EXISTS appel (
    id INT AUTO_INCREMENT PRIMARY KEY,
    statut ENUM('EN_COURS', 'TERMINE', 'MANQUE', 'REJETE') NOT NULL,
    dateDebut DATETIME DEFAULT CURRENT_TIMESTAMP,
    dateFin DATETIME,
    type ENUM('AUDIO', 'VIDEO') NOT NULL,
    utilisateur_id INT NOT NULL, -- initiateur
    FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 8. Table Notification
CREATE TABLE IF NOT EXISTS notification (
    id INT AUTO_INCREMENT PRIMARY KEY,
    dateEnvoi DATETIME DEFAULT CURRENT_TIMESTAMP,
    type ENUM('NOUVEAU_MESSAGE', 'APPEL_MANQUE', 'DEMANDE_CONTACT') NOT NULL,
    expediteur_id INT,
    destinataire_id INT NOT NULL,
    estLu BOOLEAN DEFAULT FALSE,
    contenu VARCHAR(255),
    FOREIGN KEY (expediteur_id) REFERENCES utilisateur(id) ON DELETE SET NULL,
    FOREIGN KEY (destinataire_id) REFERENCES utilisateur(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 9. Table Connexion
CREATE TABLE IF NOT EXISTS connexion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id INT NOT NULL,
    socketId VARCHAR(255),
    estEnLigne BOOLEAN DEFAULT FALSE,
    dateConnexion DATETIME DEFAULT CURRENT_TIMESTAMP,
    dateDeconnexion DATETIME,
    FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id) ON DELETE CASCADE
) ENGINE=InnoDB;
