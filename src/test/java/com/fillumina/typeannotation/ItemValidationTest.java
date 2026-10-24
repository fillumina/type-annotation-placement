package com.fillumina.typeannotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * What Hibernate Validator makes of a {@code @Valid} on the items of a collection, in each of the
 * shapes a nested item type allows.
 *
 * <p>The interesting case is the one that compiles and does nothing: an annotation written in front
 * of a qualified name belongs to the enclosing class, so the items are never visited. Every
 * assertion here is about a violation that has to appear, except that one, which asserts that the
 * invalid item passes unnoticed.
 */
class ItemValidationTest {

    private static final String ITEM_NAME = "items[0].name";

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * The ordinary case: the item type is a simple name, the annotation in the type argument is on the
     * item, and a blank item is reported.
     */
    @Test
    void theItemsOfATopLevelTypeAreValidated() {
        assertEquals(List.of(ITEM_NAME),
                pathsOf(validator.validate(new TopLevelItems(List.of(new PlainItem(""))))));
    }

    /**
     * A static nested item type, the shape XJC generates for an anonymous complex type: the
     * annotation stands between the enclosing class and the name of the item, and the item is
     * reported.
     */
    @Test
    void theItemsOfANestedTypeAreValidatedByTheAnnotationBeforeTheItemName() {
        assertEquals(List.of(ITEM_NAME),
                pathsOf(validator.validate(new StaticNestedItems(
                        List.of(new Enclosing.Nested(""))))));
    }

    /**
     * An inner item type with the annotation in the same place: the item is reported, and the
     * enclosing instance it belongs to says nothing, see
     * {@link #theAnnotationBeforeTheItemNameValidatesTheItemAndNotTheEnclosingClass()}.
     */
    @Test
    void theItemsOfAnInnerTypeAreValidatedByTheAnnotationBeforeTheItemName() {
        Enclosing enclosing = new Enclosing("");
        assertEquals(List.of(ITEM_NAME),
                pathsOf(validator.validate(new MemberItemsLastSegment(
                        List.of(enclosing.new Member(""))))));
    }

    /**
     * The spelling that compiles and does nothing: the annotation is closest to {@code Enclosing}, so
     * it marks the enclosing class, and the invalid item passes unnoticed.
     */
    @Test
    void theItemsOfAnInnerTypeAreNotValidatedByTheAnnotationBeforeTheEnclosingName() {
        Enclosing enclosing = new Enclosing("");
        assertEquals(List.of(),
                pathsOf(validator.validate(new MemberItemsWholeName(
                        List.of(enclosing.new Member(""))))));
    }

    /**
     * The same spelling with the enclosing instance holding a blank {@code label} of its own: neither
     * the item nor the enclosing class produces a violation. Both values are invalid, so a cascade in
     * either direction would have shown here.
     */
    @Test
    void theAnnotationBeforeTheEnclosingNameValidatesNeitherTheItemNorTheEnclosingClass() {
        Enclosing enclosing = new Enclosing("");

        assertEquals(List.of(),
                pathsOf(validator.validate(new MemberItemsWholeName(
                        List.of(enclosing.new Member(""))))),
                "the annotation marks the enclosing class and reaches nothing");
    }

    /**
     * The same two invalid objects with the annotation on the item: only the item is reported, because
     * cascading follows the object graph of the bean and the enclosing instance is not on it.
     */
    @Test
    void theAnnotationBeforeTheItemNameValidatesTheItemAndNotTheEnclosingClass() {
        Enclosing enclosing = new Enclosing("");

        assertEquals(List.of(ITEM_NAME),
                pathsOf(validator.validate(new MemberItemsLastSegment(
                        List.of(enclosing.new Member(""))))),
                "cascading follows the object graph, and the enclosing instance is not on it");
    }

    /**
     * The constraint of the enclosing class is live when that class is what is validated, so its
     * silence in the tests above is about the cascade and not about a constraint that never worked.
     */
    @Test
    void theEnclosingClassIsValidatedWhenItIsTheObjectBeingValidated() {
        assertEquals(List.of("label"), pathsOf(validator.validate(new Enclosing(""))));
    }

    /**
     * The other half of every case above: a valid item produces no violation in any of the shapes, so
     * what those tests assert is about the items and not about the annotations alone.
     */
    @Test
    void aValidItemPassesInEveryShape() {
        Enclosing enclosing = new Enclosing("ok");
        assertEquals(List.of(),
                pathsOf(validator.validate(new TopLevelItems(List.of(new PlainItem("ok"))))));
        assertEquals(List.of(),
                pathsOf(validator.validate(new StaticNestedItems(
                        List.of(new Enclosing.Nested("ok"))))));
        assertEquals(List.of(),
                pathsOf(validator.validate(new MemberItemsLastSegment(
                        List.of(enclosing.new Member("ok"))))));
        assertEquals(List.of(),
                pathsOf(validator.validate(new MemberItemsWholeName(
                        List.of(enclosing.new Member("ok"))))));
    }

    /**
     * The same difference read through reflection instead of through the violations: the type argument
     * of the field carries {@code @Valid} in every shape except the whole-name one, where it sits on
     * {@code getAnnotatedOwnerType()}, the enclosing class.
     */
    @Test
    void theAnnotationIsOnTheItemUnlessItIsWrittenInFrontOfAQualifiedName() throws Exception {
        AnnotatedType topLevel = itemTypeOf(TopLevelItems.class);
        assertValid(topLevel);
        assertNull(topLevel.getAnnotatedOwnerType());

        AnnotatedType nested = itemTypeOf(StaticNestedItems.class);
        assertValid(nested);

        AnnotatedType innerLastSegment = itemTypeOf(MemberItemsLastSegment.class);
        assertValid(innerLastSegment);

        AnnotatedType innerWholeName = itemTypeOf(MemberItemsWholeName.class);
        assertEquals(List.of(), List.of(innerWholeName.getAnnotations()),
                "the item itself carries no annotation");
        assertValid(innerWholeName.getAnnotatedOwnerType());
    }

    private static AnnotatedType itemTypeOf(Class<?> bean) throws Exception {
        AnnotatedParameterizedType items =
                (AnnotatedParameterizedType) bean.getDeclaredField("items").getAnnotatedType();
        return items.getAnnotatedActualTypeArguments()[0];
    }

    private static void assertValid(AnnotatedType type) {
        assertTrue(List.of(type.getAnnotations()).stream()
                .anyMatch(annotation -> annotation.annotationType() == Valid.class),
                () -> "@Valid is not on " + type);
    }

    private static List<String> pathsOf(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .sorted()
                .toList();
    }
}
