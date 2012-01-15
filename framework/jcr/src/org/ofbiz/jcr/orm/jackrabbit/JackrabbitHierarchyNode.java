package org.ofbiz.jcr.orm.jackrabbit;

import java.util.Calendar;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;
import org.ofbiz.jcr.orm.OfbizRepositoryMapping;
import org.ofbiz.jcr.util.jackrabbit.JcrUtilJackrabbit;

@Node(jcrType = "nt:hierarchyNode", jcrMixinTypes="mix:versionable")
public class JackrabbitHierarchyNode implements OfbizRepositoryMapping {
    @Field(path = true, id = true, jcrProtected = true)
    protected String path;
    private String version;
    @Field(jcrName = "jcr:created")
    private Calendar creationDate;

    public String getPath() {
        return path;
    }

    public void setPath(String nodePath) {
        // check if the node path is an absolute path
        this.path = JcrUtilJackrabbit.createAbsoluteNodePath(nodePath);
    }

    public Calendar getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Calendar creationDate) {
        this.creationDate = creationDate;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public void setVersion(String version) {
        this.version = version;
    }

}
