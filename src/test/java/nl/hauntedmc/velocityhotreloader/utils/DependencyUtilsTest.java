package nl.hauntedmc.velocityhotreloader.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DependencyUtilsTest {

    @Test
    void determineOrderShouldProduceTopologicalOrdering() {
        Map<String, Set<String>> dependencyMap = new LinkedHashMap<>();
        dependencyMap.put("A", new LinkedHashSet<>(Set.of("B", "C")));
        dependencyMap.put("B", new LinkedHashSet<>(Set.of("D")));
        dependencyMap.put("C", new LinkedHashSet<>(Set.of("D")));
        dependencyMap.put("D", Set.of());

        List<String> order = DependencyUtils.determineOrder(dependencyMap);

        for (Map.Entry<String, Set<String>> entry : dependencyMap.entrySet()) {
            int nodeIndex = order.indexOf(entry.getKey());
            for (String dependency : entry.getValue()) {
                int dependencyIndex = order.indexOf(dependency);
                assertTrue(dependencyIndex < nodeIndex, () ->
                        "Dependency '" + dependency + "' should appear before '" + entry.getKey() + "'");
            }
        }
    }

    @Test
    void determineOrderShouldThrowOnCircularDependency() {
        Map<String, Set<String>> dependencyMap = new LinkedHashMap<>();
        dependencyMap.put("A", Set.of("B"));
        dependencyMap.put("B", Set.of("C"));
        dependencyMap.put("C", Set.of("A"));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> DependencyUtils.determineOrder(dependencyMap)
        );
        assertTrue(ex.getMessage().contains("Circular dependency detected"));
    }
}
