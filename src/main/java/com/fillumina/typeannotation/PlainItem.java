package com.fillumina.typeannotation;

import jakarta.validation.constraints.NotBlank;

/** The item type of a collection when it is a top level class, which needs no special spelling. */
public class PlainItem {

    @NotBlank
    public String name;

    public PlainItem(String name) {
        this.name = name;
    }
}
