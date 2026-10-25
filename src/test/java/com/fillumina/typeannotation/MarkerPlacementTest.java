package com.fillumina.typeannotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Where a type annotation on a qualified item type lands, with an annotation that means nothing:
 * {@code @Marker} is read by nobody, so nothing here depends on a validation library.
 *
 * <p>Both beans have the same item type, {@code Enclosing.Member}. Only the position of the annotation
 * changes, and with it the part of the type the annotation is attached to.
 */
class MarkerPlacementTest {

    static class AfterTheDot {

        List<Enclosing.@Marker Member> items;
    }

    static class InFrontOfTheWholeName {

        List<@Marker Enclosing.Member> items;
    }

    static class Assigned {

        List<@Marker String> first = List.of("a");
        List<String> second = first;
    }

    @Test
    void afterTheDotTheMarkerIsOnTheItem() throws Exception {
        AnnotatedType item = itemTypeOf(AfterTheDot.class);

        assertEquals(Enclosing.Member.class, item.getType());
        assertEquals(List.of(Marker.class), annotationTypes(item));
        assertEquals(List.of(), annotationTypes(item.getAnnotatedOwnerType()));
    }

    @Test
    void inFrontOfTheWholeNameTheMarkerIsOnTheEnclosingClass() throws Exception {
        AnnotatedType item = itemTypeOf(InFrontOfTheWholeName.class);

        assertEquals(Enclosing.Member.class, item.getType());
        assertEquals(List.of(), annotationTypes(item), "the item carries nothing");
        assertEquals(List.of(Marker.class), annotationTypes(item.getAnnotatedOwnerType()));
    }

    /**
     * The annotation belongs to the declaration it is written on: the two fields hold the same list,
     * and only the first declares the marker.
     */
    @Test
    void anAssignedValueDoesNotCarryTheAnnotationsOfTheDeclarationItCameFrom() throws Exception {
        assertEquals(List.of(Marker.class), annotationTypes(elementTypeOf(Assigned.class, "first")));
        assertEquals(List.of(), annotationTypes(elementTypeOf(Assigned.class, "second")));

        Assigned assigned = new Assigned();
        assertSame(assigned.first, assigned.second, "one list, two declarations");
    }

    private static AnnotatedType itemTypeOf(Class<?> bean) throws Exception {
        return elementTypeOf(bean, "items");
    }

    private static AnnotatedType elementTypeOf(Class<?> bean, String field) throws Exception {
        AnnotatedParameterizedType items =
                (AnnotatedParameterizedType) bean.getDeclaredField(field).getAnnotatedType();
        return items.getAnnotatedActualTypeArguments()[0];
    }

    private static List<Class<? extends Annotation>> annotationTypes(AnnotatedType type) {
        return Arrays.stream(type.getAnnotations())
                .map(Annotation::annotationType)
                .toList();
    }
}
