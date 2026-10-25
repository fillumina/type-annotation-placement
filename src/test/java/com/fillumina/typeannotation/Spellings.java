package com.fillumina.typeannotation;

import jakarta.validation.Valid;
import java.util.List;

/**
 * The five spellings of a {@code @Valid} on the items of a collection, in one file: the four that
 * compile, each with the bean that declares it, and the one javac rejects.
 *
 * <p>This is the same code as the beans in {@code src/main/java}, gathered here so that it can be read
 * in one place.
 */
public class Spellings {

    /** {@code TopLevelItems}: the annotation is on the type argument, which is the item. */
    private List<@Valid PlainItem> topLevelItems;

    /** {@code InnerItemsLastSegment}: the annotation is on the inner class, which is the item. */
    private List<Enclosing.@Valid Inner> innerItemsLastSegment;

    /** {@code InnerItemsWholeName}: the annotation is on {@code Enclosing}, so nothing is validated. */
    private List<@Valid Enclosing.Inner> innerItemsWholeName;

    /** {@code StaticNestedItems}: the annotation is between the enclosing class and the item. */
    private List<Enclosing.@Valid Nested> staticNestedItems;

    // A fifth spelling, List<@Valid Enclosing.Nested>, does not compile: Nested is static, so Enclosing
    // in that name is only a name and cannot be annotated.
}
