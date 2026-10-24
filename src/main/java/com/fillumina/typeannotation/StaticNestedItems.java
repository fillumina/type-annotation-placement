package com.fillumina.typeannotation;

import jakarta.validation.Valid;
import java.util.List;

/**
 * {@code List<Enclosing.@Valid Nested>}: the annotation sits between the enclosing class and the name
 * of the item, which is the only legal spelling for a nested class.
 *
 * <p>Written in front of the whole name the same annotation does not compile, so this is the form a
 * generator has to emit.
 */
public class StaticNestedItems {

    private final List<Enclosing.@Valid Nested> items;

    public StaticNestedItems(List<Enclosing.@Valid Nested> items) {
        this.items = items;
    }

    public List<Enclosing.Nested> getItems() {
        return items;
    }
}
