package com.fillumina.typeannotation;

import jakarta.validation.Valid;
import java.util.List;

/**
 * {@code List<@Valid Enclosing.Inner>}: the annotation comes before the whole qualified name, and
 * the simple name it is closest to is {@code Enclosing}, so it applies to the enclosing class and
 * not to the item.
 *
 * <p>This compiles only because {@code Inner} is an inner class. Hibernate Validator then finds no
 * {@code @Valid} on the item and never validates it, which is why a generator must not write this
 * form: it is silently useless rather than wrong.
 */
public class InnerItemsWholeName {

    private final List<@Valid Enclosing.Inner> items;

    public InnerItemsWholeName(List<Enclosing.Inner> items) {
        this.items = items;
    }

    public List<Enclosing.Inner> getItems() {
        return items;
    }
}
