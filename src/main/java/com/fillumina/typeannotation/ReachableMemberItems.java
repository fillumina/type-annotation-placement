package com.fillumina.typeannotation;

import jakarta.validation.Valid;
import java.util.List;

/**
 * {@code List<Enclosing.@Valid ReachableMember>}: the annotation is on the item, and the item hands back
 * the instance it belongs to, so the enclosing instance is on the object graph.
 */
public class ReachableMemberItems {

    private final List<Enclosing.@Valid ReachableMember> items;

    public ReachableMemberItems(List<Enclosing.ReachableMember> items) {
        this.items = items;
    }

    public List<Enclosing.ReachableMember> getItems() {
        return items;
    }
}
