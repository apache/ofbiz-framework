package org.ofbiz.jcr.orm.jackrabbit.data;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;
import org.ofbiz.jcr.orm.OfbizRepositoryMapping;
import org.ofbiz.jcr.util.jackrabbit.JackrabbitUtils;

@Node(isAbstract = true, jcrMixinTypes = "mix:versionable")
public abstract class JackrabbitUnstructured implements OfbizRepositoryMapping {

    protected static String module = JackrabbitUnstructured.class.getName();

    @Field(path = true)
    private String path;
    @Field
    private String version;
    @Field(jcrName = "jcr:created")
    private Calendar creationDate;
    @Field
    private boolean localized;
    @Field
    private String partyThatCreatedTheContent;
    @Field
    private String lastUpdatedParty;

    protected JackrabbitUnstructured() {
        // create an empty object
    }

    protected JackrabbitUnstructured(String nodePath) {
        this.setPath(nodePath);
        this.creationDate = new GregorianCalendar();
        this.localized = false;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ofbiz.jcr.orm.OfbizRepositoryMapping#getPath()
     */
    @Override
    public String getPath() {
        return path;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ofbiz.jcr.orm.OfbizRepositoryMapping#setPath(java.lang.String)
     */
    @Override
    public void setPath(String nodePath) {
        nodePath = JackrabbitUtils.createAbsoluteNodePath(nodePath);
        this.path = nodePath;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ofbiz.jcr.orm.OfbizRepositoryMapping#getVersion()
     */
    @Override
    public String getVersion() {
        return version;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ofbiz.jcr.orm.OfbizRepositoryMapping#setVersion(java.lang.String)
     */
    @Override
    public void setVersion(String version) {
        this.version = version;
    }

    public Calendar getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Calendar creationDate) {
        this.creationDate = creationDate;
    }

    public boolean getLocalized() {
        return localized;
    }

    public void setLocalized(boolean isLocalized) {
        this.localized = isLocalized;
    }

    public void setPartyThatCreatedTheContent(String partyId) {
        this.partyThatCreatedTheContent = partyId;
    }

    public String getPartyThatCreatedTheContent() {
        return this.partyThatCreatedTheContent;
    }

    public void setLastUpdatedParty(String partyId) {
        this.lastUpdatedParty = partyId;
    }

    public String getLastUpdatedParty() {
        return this.lastUpdatedParty;
    }

}
