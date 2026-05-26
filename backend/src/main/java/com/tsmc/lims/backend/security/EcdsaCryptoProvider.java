package com.tsmc.lims.backend.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;

@Component
public class EcdsaCryptoProvider {

    private static final String ALGORITHM = "EC";
    private static final String CURVE_NAME = "secp256r1";
    private static final String SIGNATURE_ALGORITHM = "SHA256withECDSA";
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BIT = 128;
    private static final int GCM_IV_LENGTH_BYTE = 12;

    private final SecretKey masterKey;

    public EcdsaCryptoProvider(@Value("${lims.security.aes.master-key}") String base64MasterKey) {
        byte[] decodedKey = Base64.getDecoder().decode(base64MasterKey);
        this.masterKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    }

    public KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(ALGORITHM);
            keyGen.initialize(new ECGenParameterSpec(CURVE_NAME), new SecureRandom());
            return keyGen.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate ECDSA key pair", e);
        }
    }

    public String signData(String payload, PrivateKey privateKey) {
        try {
            Signature ecdsaSign = Signature.getInstance(SIGNATURE_ALGORITHM);
            ecdsaSign.initSign(privateKey);
            ecdsaSign.update(payload.getBytes(StandardCharsets.UTF_8));
            byte[] signature = ecdsaSign.sign();
            return Base64.getEncoder().encodeToString(signature);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign data", e);
        }
    }

    public boolean verifySignature(String payload, String signatureBase64, PublicKey publicKey) {
        try {
            Signature ecdsaVerify = Signature.getInstance(SIGNATURE_ALGORITHM);
            ecdsaVerify.initVerify(publicKey);
            ecdsaVerify.update(payload.getBytes(StandardCharsets.UTF_8));
            byte[] signature = Base64.getDecoder().decode(signatureBase64);
            return ecdsaVerify.verify(signature);
        } catch (Exception e) {
            return false;
        }
    }

    public String encryptPrivateKey(PrivateKey privateKey) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTE];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, gcmParameterSpec);

            byte[] cipherText = cipher.doFinal(privateKey.getEncoded());

            byte[] encryptedMessage = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, encryptedMessage, 0, iv.length);
            System.arraycopy(cipherText, 0, encryptedMessage, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(encryptedMessage);
        } catch (Exception e) {
            throw new RuntimeException("CRITICAL: Failed to encrypt private key with AES-GCM", e);
        }
    }

    public PrivateKey decryptPrivateKey(String base64EncryptedMessage) throws Exception {
        byte[] encryptedMessage = Base64.getDecoder().decode(base64EncryptedMessage);

        byte[] iv = new byte[GCM_IV_LENGTH_BYTE];
        System.arraycopy(encryptedMessage, 0, iv, 0, iv.length);

        byte[] cipherText = new byte[encryptedMessage.length - iv.length];
        System.arraycopy(encryptedMessage, iv.length, cipherText, 0, cipherText.length);

        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BIT, iv);
        cipher.init(Cipher.DECRYPT_MODE, masterKey, gcmParameterSpec);

        byte[] plainTextBytes = cipher.doFinal(cipherText);

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(plainTextBytes);
        KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
        return kf.generatePrivate(spec);
    }

    public String encodeKeyToBase64(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public PrivateKey decodePrivateKey(String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
        return kf.generatePrivate(spec);
    }

    public PublicKey decodePublicKey(String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance(ALGORITHM);
        return kf.generatePublic(spec);
    }
}
