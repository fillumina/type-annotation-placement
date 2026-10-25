package com.fillumina.typeannotation;

import jakarta.validation.Valid;
import java.util.List;

/**
 * {@code List<@Valid Enclosing.ReachableInner>}: the annotation is on {@code Enclosing}, so the item is
 * not marked, even though it hands back the instance it belongs to.
 */
public class ReachableInnerItemsWholeName {

    private final List<@Valid Enclosing.ReachableInner> items;

    public ReachableInnerItemsWholeName(List<Enclosing.ReachableInner> items) {
        this.items = items;
    }

    public List<Enclosing.ReachableInner> getItems() {
        return items;
    }
}
