package nl.hauntedmc.velocityhotreloader.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class HashGraphTest {

    @Test
    void shouldStoreNodesAndEdges() {
        HashGraph<String> graph = new HashGraph<>();
        graph.addNode("A");
        graph.addNode("B");
        graph.putEdge("A", "B");

        assertEquals(Set.of("A", "B"), graph.nodes());
        assertEquals(Set.of("B"), graph.successors("A"));
        assertEquals(Set.of("A"), graph.predecessors("B"));
    }

    @Test
    void unknownNodeShouldReturnEmptyRelations() {
        HashGraph<String> graph = new HashGraph<>();

        assertTrue(graph.successors("unknown").isEmpty());
        assertTrue(graph.predecessors("unknown").isEmpty());
    }
}
