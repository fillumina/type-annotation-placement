package com.fillumina.typeannotation;

import jakarta.validation.Valid;
import java.util.List;

/** {@code List<@Valid PlainItem>}: the annotation is on the type argument, which is the item. */
public class TopLevelItems {

    private final List<@Valid PlainItem> items;

    public TopLevelItems(List<PlainItem> items) {
        this.items = items;
    }

    public List<PlainItem> getItems() {
        return items;
    }
}
