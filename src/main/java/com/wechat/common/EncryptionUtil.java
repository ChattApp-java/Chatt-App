package com.wechat.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilitaire de chiffrement pour WeChat.
 * Hashage des mots de passe avec SHA-256 + salt (BCrypt non disponible sans dépendance externe).
 */
public class EncryptionUtil {

    private static final Logger LOGGER = Logger.getLogger(EncryptionUtil.class.getName());
    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16; // 16 bytes = 128 bits
    private static final int ITERATIONS = 10000; // Nombre d'itérations PBKDF2-like

    /**
     * Hash un mot de passe avec SHA-256 + salt aléatoire.
     * Format du résultat : salt:hash (Base64)
     */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe ne peut pas être vide");
        }

        try {
            // Générer un salt aléatoire
            byte[] salt = generateSalt();

            // Hasher le mot de passe avec le salt
            byte[] hash = hashWithSalt(password, salt);

            // Combiner salt + hash et encoder en Base64
            String saltBase64 = Base64.getEncoder().encodeToString(salt);
            String hashBase64 = Base64.getEncoder().encodeToString(hash);

            return saltBase64 + ":" + hashBase64;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur hashage mot de passe", e);
            throw new RuntimeException("Erreur hashage mot de passe", e);
        }
    }

    /**
     * Vérifie un mot de passe contre un hash stocké.
     *
     * @param password     Mot de passe en clair
     * @param storedHash   Hash stocké (format salt:hash)
     * @return true si le mot de passe correspond
     */
    public static boolean verifyPassword(String password, String storedHash) {
        if (password == null || storedHash == null || !storedHash.contains(":")) {
            return false;
        }

        try {
            // Extraire le salt du hash stocké
            String[] parts = storedHash.split(":");
            if (parts.length != 2) {
                LOGGER.warning("Format de hash invalide");
                return false;
            }

            byte[] salt = Base64.getDecoder().decode(parts[0]);

            // Hasher le mot de passe fourni avec le même salt
            byte[] hash = hashWithSalt(password, salt);
            String hashBase64 = Base64.getEncoder().encodeToString(hash);

            // Comparer les hashes
            return hashBase64.equals(parts[1]);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Erreur vérification mot de passe", e);
            return false;
        }
    }

    /**
     * Chiffre une chaîne simple (pour données sensibles non-MDP).
     */
    public static String encrypt(String data) {
        // TODO: Implémenter chiffrement AES si nécessaire
        return data;
    }

    /**
     * Déchiffre une chaîne.
     */
    public static String decrypt(String encryptedData) {
        // TODO: Implémenter déchiffrement AES si nécessaire
        return encryptedData;
    }

    // ─── PRIVÉ ───

    private static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }

    private static byte[] hashWithSalt(String password, byte[] salt)
            throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(ALGORITHM);

        // Combiner password + salt
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        byte[] combined = new byte[passwordBytes.length + salt.length];
        System.arraycopy(passwordBytes, 0, combined, 0, passwordBytes.length);
        System.arraycopy(salt, 0, combined, passwordBytes.length, salt.length);

        // Itérations multiples (PBKDF2-like avec SHA-256)
        byte[] hash = combined;
        for (int i = 0; i < ITERATIONS; i++) {
            hash = digest.digest(hash);
            digest.reset();
        }

        return hash;
    }

    /**
     * Test rapide du hashage.
     */
    public static void main(String[] args) {
        String password = "monSuperMotDePasse123";

        String hash1 = hashPassword(password);
        System.out.println("Hash 1 : " + hash1);

        String hash2 = hashPassword(password);
        System.out.println("Hash 2 : " + hash2);

        System.out.println("Hash 1 != Hash 2 (salt différent) : " + !hash1.equals(hash2));

        boolean valid = verifyPassword(password, hash1);
        System.out.println("Vérification correcte : " + valid);

        boolean invalid = verifyPassword("mauvaisMDP", hash1);
        System.out.println("Vérification incorrecte : " + !invalid);
    }
}