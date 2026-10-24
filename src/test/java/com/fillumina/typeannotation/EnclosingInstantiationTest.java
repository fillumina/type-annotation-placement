package com.fillumina.typeannotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

/**
 * The two creations the README shows: a static nested class is created from the name of the class that
 * holds it, an inner class from an instance of that class.
 *
 * <p>The second one is the only spelling that exists. {@code new Member("item")} does not compile
 * outside an {@code Enclosing}, which is why an inner class is written {@code enclosing.new
 * Member("item")}.
 */
class EnclosingInstantiationTest {

    @Test
    void aStaticNestedClassIsCreatedFromItsName() {
        Enclosing.Nested nested = new Enclosing.Nested("item");

        assertTrue(Modifier.isStatic(Enclosing.Nested.class.getModifiers()),
                "a static nested class is created without an enclosing instance");
        assertEquals("item", nested.name);
    }

    @Test
    void anInnerClassIsCreatedFromAnInstanceOfTheEnclosingClass() {
        Enclosing enclosing = new Enclosing("label");

        Enclosing.Member member = enclosing.new Member("item");

        assertFalse(Modifier.isStatic(Enclosing.Member.class.getModifiers()),
                "an inner class is created through an instance of its enclosing class");
        assertEquals("item", member.name);
    }
}
