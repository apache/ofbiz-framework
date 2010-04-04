package org.ofbiz.base.lang;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** This annotation is meant to be used to describe how some variable
 * or method is protected by some other locking mechanism.  Its value
 * is free-form, and can contain anything.  It will generally contain
 * the name of an instance variable, including <code>this</code>.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.METHOD})
public @interface LockedBy {
    String value();
}
