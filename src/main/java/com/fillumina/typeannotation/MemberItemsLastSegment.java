package com.fillumina.typeannotation;

import jakarta.validation.Valid;
import java.util.List;

/** {@code List<Enclosing.@Valid Member>}: the annotation is on the inner class, which is the item. */
public class MemberItemsLastSegment {

    // the copy the validator reads: the property is declared here
    private final List<Enclosing.@Valid Member> items;

    // no annotation here: only a validation of the constructor call would read it
    public MemberItemsLastSegment(List<Enclosing.Member> items) {
        this.items = items;
    }

    // the same type: the annotation is optional wherever the type is written
    public List<Enclosing.Member> getItems() {
        return items;
    }
}
