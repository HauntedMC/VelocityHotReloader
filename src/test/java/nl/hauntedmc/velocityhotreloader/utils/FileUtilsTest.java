package nl.hauntedmc.velocityhotreloader.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void getHashShouldReturnMd5ForFileContents() throws IOException {
        Path file = tempDir.resolve("example.txt");
        Files.writeString(file, "abc");

        assertEquals("900150983cd24fb0d6963f7d28e17f72", FileUtils.getHash(file));
    }

    @Test
    void getHashShouldReturnNullForMissingFile() {
        assertNull(FileUtils.getHash(tempDir.resolve("missing.txt")));
    }
}
