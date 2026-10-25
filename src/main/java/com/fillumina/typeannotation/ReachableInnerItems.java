package com.fillumina.typeannotation;

import jakarta.validation.Valid;
import java.util.List;

/**
 * {@code List<Enclosing.@Valid ReachableInner>}: the annotation is on the item, and the item hands back
 * the instance it belongs to, so the enclosing instance is on the object graph.
 */
public class ReachableInnerItems {

    private final List<Enclosing.@Valid ReachableInner> items;

    public ReachableInnerItems(List<Enclosing.ReachableInner> items) {
        this.items = items;
    }

    public List<Enclosing.ReachableInner> getItems() {
        return items;
    }
}
