package com.fillumina.typeannotation;

import jakarta.validation.Valid;
import java.util.List;

/**
 * {@code List<@Valid Enclosing.ReachableMember>}: the annotation is on {@code Enclosing}, so the item is
 * not marked, even though it hands back the instance it belongs to.
 */
public class ReachableMemberItemsWholeName {

    private final List<@Valid Enclosing.ReachableMember> items;

    public ReachableMemberItemsWholeName(List<Enclosing.ReachableMember> items) {
        this.items = items;
    }

    public List<Enclosing.ReachableMember> getItems() {
        return items;
    }
}
