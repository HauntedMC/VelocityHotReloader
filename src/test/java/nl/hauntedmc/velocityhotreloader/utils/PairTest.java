package nl.hauntedmc.velocityhotreloader.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PairTest {

    @Test
    void recordAccessorsShouldExposeValues() {
        Pair<String, Integer> pair = new Pair<>("left", 7);

        assertEquals("left", pair.first());
        assertEquals(7, pair.second());
    }
}
