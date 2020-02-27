package org.meds.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MD5Hasher {

    private static Logger logger = LogManager.getLogger();

    public static String computeHash(String string) {
        StringBuilder sb = new StringBuilder();
        try {
            MessageDigest dm = MessageDigest.getInstance("MD5");
            dm.update(string.getBytes());
            byte[] digest = dm.digest();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
        } catch (NoSuchAlgorithmException ex) {
            logger.fatal("Acquiring hashing algorithm has been failed.", ex);
            return null;
        }

        return sb.toString();
    }

    public static String computePasswordHash(String password) {
        return computeHash(computeHash(password) + "dsdarkswords");
    }
}
