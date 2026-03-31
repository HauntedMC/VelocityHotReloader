package nl.hauntedmc.velocityhotreloader.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileUtils {

    private FileUtils() {}

    /**
     * Get the Hash of a file at given path.
     *
     * @param path The path
     * @return The file's hash
     */
    public static String getHash(Path path) {
        byte[] digest;
        try {
            digest = MessageDigest.getInstance("MD5").digest(Files.readAllBytes(path));
        } catch (IOException | NoSuchAlgorithmException ex) {
            return null;
        }
        return StringUtils.bytesToHex(digest);
    }
}
