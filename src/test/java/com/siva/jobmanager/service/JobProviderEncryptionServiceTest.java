package com.siva.jobmanager.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JobProviderEncryptionServiceTest {

    @Test
    void testEncryptionDecryptionWithKey() {
        JobProviderEncryptionService service = new JobProviderEncryptionService("my-super-secret-master-key-12345");

        String secret = "secret_api_key_value_987654";
        String encrypted = service.encrypt(secret);

        assertNotNull(encrypted);
        assertNotEquals(secret, encrypted);

        String decrypted = service.decrypt(encrypted);
        assertEquals(secret, decrypted);
    }

    @Test
    void testRandomIvProducesDifferentCiphertext() {
        JobProviderEncryptionService service = new JobProviderEncryptionService("my-super-secret-master-key-12345");

        String secret = "adzuna_app_key_abc123";
        String cipher1 = service.encrypt(secret);
        String cipher2 = service.encrypt(secret);

        assertNotEquals(cipher1, cipher2, "AES-GCM with fresh IV must produce different ciphertexts for the same plaintext");
        assertEquals(secret, service.decrypt(cipher1));
        assertEquals(secret, service.decrypt(cipher2));
    }

    @Test
    void testCredentialMasking() {
        assertEquals("", JobProviderEncryptionService.mask(""));
        assertEquals("••••", JobProviderEncryptionService.mask("1234"));
        assertEquals("••••78", JobProviderEncryptionService.mask("12345678"));
        assertEquals("••••••••9012", JobProviderEncryptionService.mask("secret-token-3456789012"));
    }
}

