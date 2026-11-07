package edu.campussignal.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** AES-GCM envelope used for OAuth tokens and short-lived OAuth transaction secrets. */
@Service
public class TokenCipher {
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private final SecureRandom random = new SecureRandom();
    private final SecretKeySpec key;

    public TokenCipher(@Value("${campussignal.token-encryption-key:}") String encodedKey) {
        try {
            byte[] bytes = Base64.getDecoder().decode(encodedKey == null ? "" : encodedKey);
            key = bytes.length == 32 ? new SecretKeySpec(bytes, "AES") : null;
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("CAMPUSSIGNAL_TOKEN_ENCRYPTION_KEY must be base64 when configured");
        }
    }

    public String encrypt(String value) {
        configured();
        try {
            byte[] iv = new byte[IV_LENGTH]; random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] payload = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    ByteBuffer.allocate(iv.length + payload.length).put(iv).put(payload).array());
        } catch (GeneralSecurityException exception) { throw new IllegalStateException("Token encryption failed", exception); }
    }

    public String decrypt(String encoded) {
        configured();
        try {
            byte[] packed = Base64.getUrlDecoder().decode(encoded);
            if (packed.length <= IV_LENGTH) throw new IllegalArgumentException("invalid token envelope");
            ByteBuffer buffer = ByteBuffer.wrap(packed); byte[] iv = new byte[IV_LENGTH]; buffer.get(iv);
            byte[] payload = new byte[buffer.remaining()]; buffer.get(payload);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(payload), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Stored token cannot be decrypted", exception);
        }
    }
    private void configured() { if (key == null) throw new IllegalStateException("CAMPUSSIGNAL_TOKEN_ENCRYPTION_KEY must be a base64 32-byte key before Google sign-in is enabled"); }
}
