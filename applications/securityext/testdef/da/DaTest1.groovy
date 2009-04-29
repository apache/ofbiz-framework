import org.ofbiz.base.util.Debug;

String recordNumber = permission.substring(permission.lastIndexOf(":") + 1)
if ("system".equals(userId) && "1000".equals(recordNumber)) {
    Debug.log("Matched approval requirements {system} - {1000}; returning true");
    return true;
}

Debug.logInfo("Did not match expected requirements; returning false", "groovy");
return false;