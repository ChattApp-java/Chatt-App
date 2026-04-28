CREATE DATABASE IF NOT EXISTS chattapp_db;
USE chattapp_db;

CREATE TABLE IF NOT EXISTS utilisateur (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    statut VARCHAR(50) DEFAULT 'HORS_LIGNE',
    derniere_connexion DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS connexion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id INT NOT NULL,
    socket_id VARCHAR(255),
    est_en_ligne BOOLEAN DEFAULT FALSE,
    date_connexion DATETIME DEFAULT CURRENT_TIMESTAMP,
    date_deconnexion DATETIME,
    FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS contact (
    utilisateur_id INT,
    contact_id INT,
    PRIMARY KEY (utilisateur_id, contact_id),
    FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id) ON DELETE CASCADE,
    FOREIGN KEY (contact_id) REFERENCES utilisateur(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS conversation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(255),
    type VARCHAR(50) DEFAULT 'INDIVIDUEL',
    date_creation DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS participant_conversation (
    conversation_id INT,
    utilisateur_id INT,
    PRIMARY KEY (conversation_id, utilisateur_id),
    FOREIGN KEY (conversation_id) REFERENCES conversation(id) ON DELETE CASCADE,
    FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS message (
    id INT AUTO_INCREMENT PRIMARY KEY,
    contenu TEXT,
    type VARCHAR(50) DEFAULT 'TEXTE',
    expediteur_id INT NOT NULL,
    destinataire_id INT,
    conversation_id INT NOT NULL,
    est_lu BOOLEAN DEFAULT FALSE,
    date_envoi DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (expediteur_id) REFERENCES utilisateur(id) ON DELETE CASCADE,
    FOREIGN KEY (destinataire_id) REFERENCES utilisateur(id) ON DELETE SET NULL,
    FOREIGN KEY (conversation_id) REFERENCES conversation(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS fichier_media (
    id INT AUTO_INCREMENT PRIMARY KEY,
    message_id INT NOT NULL,
    nom_fichier VARCHAR(255),
    chemin_acces VARCHAR(500),
    taille BIGINT,
    type VARCHAR(50),
    date_ajout DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES message(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS vocal (
    id INT PRIMARY KEY,
    duree INT,
    FOREIGN KEY (id) REFERENCES fichier_media(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS video (
    id INT PRIMARY KEY,
    resolution VARCHAR(50),
    duree INT,
    FOREIGN KEY (id) REFERENCES fichier_media(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS appel (
    id INT AUTO_INCREMENT PRIMARY KEY,
    type_appel VARCHAR(50),
    date_heure DATETIME DEFAULT CURRENT_TIMESTAMP,
    duree INT DEFAULT 0,
    statut VARCHAR(50),
    expediteur_id INT NOT NULL,
    destinataire_id INT NOT NULL,
    FOREIGN KEY (expediteur_id) REFERENCES utilisateur(id) ON DELETE CASCADE,
    FOREIGN KEY (destinataire_id) REFERENCES utilisateur(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS notification (
    id INT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id INT NOT NULL,
    contenu TEXT,
    type VARCHAR(50),
    est_lue BOOLEAN DEFAULT FALSE,
    date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id) ON DELETE CASCADE
);
