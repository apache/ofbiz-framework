package org.apache.ofbiz.entity.util;

import org.apache.ofbiz.entity.Delegator;

/**
 * Interface for classes that hold a delegator that is meant to be used by other classes (for queries for example)
 */
public interface DelegatorProvider {

    /**
     * Gets the Delegator associated with current object instance
     * @return Delegator associated with current object instance
     */
    Delegator getDelegator();

}
