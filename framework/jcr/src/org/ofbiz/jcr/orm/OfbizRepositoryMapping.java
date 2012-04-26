package org.ofbiz.jcr.orm;

public interface OfbizRepositoryMapping {
    /**
     * Return the Node Path.
     *
     * @return
     */
    String getPath();

    /**
     * Set the Node Path.
     *
     * @param path
     */
    void setPath(String path);

    /**
     * Return the current Version of the content object.
     *
     * @return
     */
    public String getVersion();

    /**
     * Set the node version.
     *
     * @param version
     */
    public void setVersion(String version);
}