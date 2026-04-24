package com.skinsshowcase.trades.service;

import com.skinsshowcase.trades.config.SteamIdProtectionProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class SteamIdProtectionService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;

    private final byte[] keyBytes;
    private final SecureRandom secureRandom = new SecureRandom();

    public SteamIdProtectionService(SteamIdProtectionProperties properties) {
        var keyRaw = properties.getKey();
        if (keyRaw == null || keyRaw.isBlank()) {
            throw new IllegalStateException("app.steam-id-protection.key must be configured");
        }
        var decoded = Base64.getDecoder().decode(keyRaw.trim());
        if (decoded.length != 32) {
            throw new IllegalStateException("app.steam-id-protection.key must be a base64-encoded 32-byte key");
        }
        this.keyBytes = decoded;
    }

    public String hash(String steamId) {
        if (steamId == null || steamId.isBlank()) {
            return null;
        }
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var bytes = digest.digest(steamId.trim().getBytes(StandardCharsets.UTF_8));
            var sb = new StringBuilder(bytes.length * 2);
            for (var b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash steam id", e);
        }
    }

    public String encrypt(String steamId) {
        if (steamId == null || steamId.isBlank()) {
            return null;
        }
        try {
            var iv = new byte[GCM_IV_BYTES];
            secureRandom.nextBytes(iv);
            var cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            var encrypted = cipher.doFinal(steamId.trim().getBytes(StandardCharsets.UTF_8));
            var payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt steam id", e);
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isBlank()) {
            return null;
        }
        try {
            var payload = Base64.getDecoder().decode(ciphertext);
            var iv = new byte[GCM_IV_BYTES];
            var encrypted = new byte[payload.length - GCM_IV_BYTES];
            System.arraycopy(payload, 0, iv, 0, GCM_IV_BYTES);
            System.arraycopy(payload, GCM_IV_BYTES, encrypted, 0, encrypted.length);
            var cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt steam id", e);
        }
    }
}
