package com.fillumina.typeannotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Whether the enclosing instance can be reached once it becomes a property of the item: it can, when the
 * item is validated, and the whole-name spelling still never gets that far.
 */
class ReachableEnclosingTest {

    private static final List<String> ITEM_AND_ENCLOSING =
            List.of("items[0].enclosing.label", "items[0].name");

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * The enclosing instance is reachable, the item is marked, and both constraints are reported.
     */
    @Test
    void theItemIsValidatedAndSoIsTheEnclosingItHandsBack() {
        Enclosing enclosing = new Enclosing("");

        assertEquals(ITEM_AND_ENCLOSING, pathsOf(validator.validate(new ReachableInnerItems(
                List.of(enclosing.new ReachableInner(""))))));
    }

    /**
     * The same objects with the annotation in front of the whole name: the item is not marked, so nothing
     * is visited, and the reachable enclosing instance is not reached either.
     */
    @Test
    void theWholeNameSpellingDoesNotReachTheEnclosingInstanceEvenAsAProperty() {
        Enclosing enclosing = new Enclosing("");

        assertEquals(List.of(), pathsOf(validator.validate(new ReachableInnerItemsWholeName(
                List.of(enclosing.new ReachableInner(""))))));
    }

    /**
     * A kept reference is not a property: the item is validated, its enclosing instance is not.
     */
    @Test
    void aKeptReferenceIsNotAPropertyAndSoIsNotWalked() {
        Enclosing enclosing = new Enclosing("");

        assertEquals(List.of("items[0].name"), pathsOf(validator.validate(new UsesEnclosingItems(
                List.of(enclosing.new UsesEnclosing(""))))));
    }

    /**
     * javac keeps a reference to the enclosing instance only for a class that uses it.
     */
    @Test
    void onlyAClassThatUsesItsEnclosingInstanceKeepsAReferenceToIt() {
        assertFalse(keepsAnEnclosingReference(Enclosing.Inner.class));
        assertTrue(keepsAnEnclosingReference(Enclosing.UsesEnclosing.class));
        assertTrue(keepsAnEnclosingReference(Enclosing.ReachableInner.class));
    }

    private static boolean keepsAnEnclosingReference(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .anyMatch(field -> field.isSynthetic() && field.getType() == Enclosing.class);
    }

    private static List<String> pathsOf(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .sorted()
                .toList();
    }
}
