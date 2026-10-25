package com.fillumina.typeannotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A type annotation that means nothing to anybody: it is read by no library, only by the compiler and
 * by reflection, so that where it lands can be seen without a validator deciding anything.
 */
@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Marker {
}
