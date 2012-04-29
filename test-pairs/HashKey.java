package org.glassfish.enterprise.ha.store.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * An annotation that can be used to declare a String Attribute
 *  as a an attribute that must be used by (Consistent) hash functions.
 *  HashKey attribute is a special attribute of a StoreEntry.
 *
 * @author Mahesh.Kannan@Sun.Com
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface HashKey {
    public String name() default "";
}