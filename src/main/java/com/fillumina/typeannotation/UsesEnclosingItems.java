package com.fillumina.typeannotation;

import jakarta.validation.Valid;
import java.util.List;

/**
 * {@code List<Enclosing.@Valid UsesEnclosing>}: the annotation is on the item, and the item keeps a
 * reference to its enclosing instance without exposing it as a property.
 */
public class UsesEnclosingItems {

    private final List<Enclosing.@Valid UsesEnclosing> items;

    public UsesEnclosingItems(List<Enclosing.@Valid UsesEnclosing> items) {
        this.items = items;
    }

    public List<Enclosing.UsesEnclosing> getItems() {
        return items;
    }
}
