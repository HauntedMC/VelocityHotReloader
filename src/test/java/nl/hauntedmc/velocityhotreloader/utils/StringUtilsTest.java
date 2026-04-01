package nl.hauntedmc.velocityhotreloader.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StringUtilsTest {

    @Test
    void bytesToHexShouldEncodeLowercaseHex() {
        byte[] bytes = new byte[] {0x00, 0x0F, 0x10, (byte) 0xFF};
        assertEquals("000f10ff", StringUtils.bytesToHex(bytes));
    }

    @Test
    void bytesToHexShouldHandleEmptyArray() {
        assertEquals("", StringUtils.bytesToHex(new byte[0]));
    }
}
