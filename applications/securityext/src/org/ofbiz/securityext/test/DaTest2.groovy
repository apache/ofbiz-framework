package org.ofbiz.securityext.test;

import org.ofbiz.base.util.Debug;

String recordNumber = permission.substring(permission.lastIndexOf(":") + 1)
if ("system".equals(userId) && "2000".equals(recordNumber)) {
    Debug.log("Matched approval requirements {system} - {2000}; returning true");
    return true;
}

Debug.logInfo("Did not match expected requirements; returning false", "groovy");
return false;