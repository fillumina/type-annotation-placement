package com.fillumina.typeannotation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * The two kinds of class a class can hold: a static nested class, which is what XJC generates for an
 * anonymous complex type, and an inner class, which no schema produces and which is here because the
 * two accept a different spelling of the annotation.
 *
 * <p>It carries a constraint of its own so that a cascade into it would leave a trace: an annotation
 * written in front of a qualified name marks this class, and the tests ask whether that marks
 * anything at all.
 */
public class Enclosing {

    @NotBlank
    public String label;

    public Enclosing(String label) {
        this.label = label;
    }

    /** A nested class, reached from outside as {@code Enclosing.Nested}. */
    public static class Nested {

        @NotBlank
        public String name;

        public Nested(String name) {
            this.name = name;
        }
    }

    /** An inner class: it needs an instance of {@code Enclosing} to exist. */
    public class Inner {

        @NotBlank
        public String name;

        public Inner(String name) {
            this.name = name;
        }
    }

    /**
     * An inner class that uses the instance it belongs to without exposing it: javac then keeps a
     * reference to that instance, and nothing more, because a reference is not a property.
     */
    public class UsesEnclosing {

        @NotBlank
        public String name;

        public UsesEnclosing(String name) {
            this.name = name;
        }

        public String enclosingLabel() {
            // reads the field of the class it is declared in, so javac keeps a reference to it
            return label;   // Enclosing.this.label
        }
    }

    /**
     * An inner class that hands back the instance it belongs to, which makes of the enclosing instance
     * a property of the item and puts it on the object graph.
     */
    public class ReachableInner {

        @NotBlank
        public String name;

        public ReachableInner(String name) {
            this.name = name;
        }

        @Valid
        public Enclosing getEnclosing() {
            return Enclosing.this;
        }
    }
}
