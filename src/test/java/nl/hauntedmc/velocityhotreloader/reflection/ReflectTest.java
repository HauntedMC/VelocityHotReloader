package nl.hauntedmc.velocityhotreloader.reflection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class ReflectTest {

    @Test
    void classForNameShouldResolveAndCacheClasses() {
        Class<?> first = Reflect.classForName(TestChild.class.getName());
        Class<?> second = Reflect.classForName(TestChild.class.getName());
        assertSame(first, second);
    }

    @Test
    void classForNameShouldThrowForMissingClass() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> Reflect.classForName("missing.ClassName")
        );
        assertNotNull(ex.getCause());
    }

    @Test
    void shouldAccessFieldsAcrossClassHierarchy() {
        Field field = Reflect.getAccessibleField(TestChild.class, "value");
        TestChild child = new TestChild("initial");

        assertEquals("initial", Reflect.getFieldValue(field, child));
        Reflect.setFieldValue(field, child, "changed");
        assertEquals("changed", Reflect.getFieldValue(field, child));
    }

    @Test
    void shouldAccessAndInvokePrivateMethodAcrossHierarchy() {
        Method method = Reflect.getAccessibleMethod(TestChild.class, "secret", String.class);
        TestChild child = new TestChild("x");

        String result = Reflect.invoke(method, child, "pre-");
        assertEquals("pre-x", result);
    }

    @Test
    void invokeShouldWrapTargetExceptions() {
        Method method = Reflect.getAccessibleMethod(TestChild.class, "explode");
        TestChild child = new TestChild("x");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> Reflect.invoke(method, child));
        assertTrue(ex.getMessage().contains("explode"));
        assertNotNull(ex.getCause());
    }

    @Test
    void shouldConstructWithPrivateConstructor() {
        Constructor<?> constructor = Reflect.getAccessibleConstructor(TestChild.class, String.class);
        TestChild child = Reflect.newInstance(constructor, "created");
        assertEquals("created", child.read());
    }

    @Test
    void putObjectFieldUnsafeShouldUpdateFinalField() {
        FinalHolder holder = new FinalHolder("before");
        Field field = Reflect.getAccessibleField(FinalHolder.class, "value");

        Reflect.putObjectFieldUnsafe(holder, field, "after");
        assertEquals("after", holder.read());
    }

    private static class TestBase {

        private final String value;

        private TestBase(String value) {
            this.value = value;
        }

        private String secret(String prefix) {
            return prefix + value;
        }

        protected String readValue() {
            return value;
        }
    }

    private static final class TestChild extends TestBase {

        private TestChild(String value) {
            super(value);
        }

        private String explode() {
            throw new IllegalArgumentException("boom");
        }

        private String read() {
            return readValue();
        }
    }

    private static final class FinalHolder {

        private final Object value;

        private FinalHolder(Object value) {
            this.value = value;
        }

        private Object read() {
            return value;
        }
    }
}
