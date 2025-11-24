package org.apache.ofbiz.entity.util;

import org.apache.ofbiz.entity.Delegator;

/**
 * Interface used for the <code>EntityQuery</code> to set the delegator to yse for the query.
 * Allows easyer and shorter query instructions, like calling an <code>EntityQuery</code> with a dispatcher.
 */
public interface Queryable {

    /**
     * Gets the GenericEntityDelegator associated with this dispatcher
     * @return GenericEntityDelegator associated with this dispatcher
     */
    Delegator getDelegator();

}
