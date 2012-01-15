package org.ofbiz.jcr.orm.jackrabbit;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Bean;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;
import org.ofbiz.jcr.access.jackrabbit.ConstantsJackrabbit;

@Node(jcrType = "nt:file", extend = JackrabbitHierarchyNode.class)
public class JackrabbitFile extends JackrabbitHierarchyNode {

    @Bean(jcrName = "jcr:content")
    private JackrabbitResource resource;

    public JackrabbitResource getResource() {
        return resource;
    }

    public void setResource(JackrabbitResource resource) {
        this.resource = resource;
    }

    public void setPath(String nodePath) {
        // check that the path don't end with a /
        if (nodePath.endsWith(ConstantsJackrabbit.ROOTPATH)) {
            nodePath = nodePath.substring(0, nodePath.indexOf(ConstantsJackrabbit.NODEPATHDELIMITER));
        }

        // check that it is a relative path
        if (nodePath.indexOf(ConstantsJackrabbit.NODEPATHDELIMITER) != -1) {
            nodePath = nodePath.substring(nodePath.lastIndexOf(ConstantsJackrabbit.NODEPATHDELIMITER) + 1);
        }

        super.path = nodePath;
    }
}
