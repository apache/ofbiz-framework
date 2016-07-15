package org.ofbiz.catalina.container;

import org.apache.tomcat.JarScanType; 
import org.apache.tomcat.JarScanFilter;

final class FilterJars implements JarScanFilter {

    @Override
    public boolean check(final JarScanType jarScanType, final String jarName) {
        if (jarName.contains("discoverable")) {
            return true; 
        } else {
            return false;
        }
    } 
}
