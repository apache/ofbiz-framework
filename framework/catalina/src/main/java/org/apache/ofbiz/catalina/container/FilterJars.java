package org.apache.ofbiz.catalina.container;

import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.tomcat.JarScanFilter;
import org.apache.tomcat.JarScanType;

final class FilterJars implements JarScanFilter {

    @Override
    public boolean check(final JarScanType jarScanType, final String jarName) {
        return UtilProperties.getPropertyAsBoolean("catalina", "webSocket", false) ? jarName.contains("ofbiz.jar") : false;
    } 
}
