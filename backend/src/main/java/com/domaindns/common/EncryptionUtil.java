package com.domaindns.common;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class EncryptionUtil {
    private static final String ALGORITHM = "AES";
    // This key should be moved to configuration/environment variable in production
    private static final String KEY = "DomainDNS_Secret_Key_1234567890"; // 32 chars? No, 16, 24, 32 bytes.
    // "DomainDNS_Secret_Key_1234567890" is 29 chars. Let's make it 16 bytes (128 bit) for simplicity.
    private static final String SECRET_KEY = "DomainDNS_Secret"; // 16 chars

    public static String encrypt(String data) throws Exception {
        if (data == null) return null;
        SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public static String decrypt(String encryptedData) throws Exception {
        if (encryptedData == null) return null;
        SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        byte[] original = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(original, StandardCharsets.UTF_8);
    }
}
