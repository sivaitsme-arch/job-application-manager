package com.siva.jobmanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class JobProviderEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(JobProviderEncryptionService.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12; // 96-bit IV recommended for GCM
    private static final int GCM_TAG_LENGTH_BITS = 128; // 128-bit auth tag

    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKey secretKey;

    public JobProviderEncryptionService(
            @Value("${APP_ENCRYPTION_KEY:${JOB_MANAGER_ENCRYPTION_KEY:}}") String configuredKey) {
        this.secretKey = resolveSecretKey(configuredKey);
    }

    private SecretKey resolveSecretKey(String configuredKey) {
        try {
            byte[] keyBytes;
            if (configuredKey != null && !configuredKey.isBlank()) {
                // Key provided via environment variable
                log.info("JobProviderEncryptionService initialized with external environment key");
                keyBytes = sha256(configuredKey.getBytes(StandardCharsets.UTF_8));
            } else {
                // Obtain or generate a persistent local master key file in data/.master_key
                Path keyFile = Paths.get("data", ".master_key");
                if (Files.exists(keyFile)) {
                    log.info("JobProviderEncryptionService initialized with persistent master key");
                    byte[] fileBytes = Files.readAllBytes(keyFile);
                    keyBytes = sha256(fileBytes);
                } else {
                    log.info("Generating new persistent AES-256 master key in data/.master_key");
                    Path parent = keyFile.getParent();
                    if (parent != null && !Files.exists(parent)) {
                        Files.createDirectories(parent);
                    }
                    byte[] randomKey = new byte[32]; // 256 bits
                    secureRandom.nextBytes(randomKey);
                    Files.write(keyFile, Base64.getEncoder().encode(randomKey));
                    keyBytes = randomKey;
                }
            }
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize AES-256 encryption key", e);
        }
    }

    private static byte[] sha256(byte[] input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.digest(input);
    }

    /**
     * Encrypts plaintext string using AES-256-GCM with a fresh random 12-byte IV.
     * Output format: Base64(IV + CiphertextAndTag)
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("Encryption failed for credential");
            throw new RuntimeException("Encryption operation failed", e);
        }
    }

    /**
     * Decrypts Base64(IV + CiphertextAndTag) using AES-256-GCM.
     */
    public String decrypt(String encryptedPayload) {
        if (encryptedPayload == null || encryptedPayload.isEmpty()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedPayload);
            if (decoded.length < GCM_IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("Invalid encrypted payload length");
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] decrypted = cipher.doFinal(cipherText);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Decryption failed for credential");
            throw new RuntimeException("Decryption operation failed", e);
        }
    }

    /**
     * Safely masks a credential for UI display, e.g. "••••••••" or "••••••••a1b2".
     */
    public static String mask(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (value.length() <= 4) {
            return "••••";
        }
        if (value.length() <= 8) {
            return "••••" + value.substring(value.length() - 2);
        }
        return "••••••••" + value.substring(value.length() - 4);
    }
}

