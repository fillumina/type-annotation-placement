package com.fillumina.typeannotation;

import jakarta.validation.Valid;
import java.util.List;

/** {@code List<Enclosing.@Valid Member>}: the annotation is on the inner class, which is the item. */
public class MemberItemsLastSegment {

    private final List<Enclosing.@Valid Member> items;

    public MemberItemsLastSegment(List<Enclosing.@Valid Member> items) {
        this.items = items;
    }

    public List<Enclosing.Member> getItems() {
        return items;
    }
}
