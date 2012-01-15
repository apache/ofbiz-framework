package org.ofbiz.jcr.access;

import java.util.List;

public interface VersioningManager {

    /**
     * Returns a List of all available versions for the passed node.
     *
     * @param nodePath
     * @return
     */
    public List<String> getVersionList(String nodePath);

    /**
     * Returns true if the passed node exist in the requested version.
     *
     * @param nodePath
     * @param version
     * @return
     */
    public boolean checkIfVersionExist(String nodePath, String version);

    /**
     * Returns the last checked in version.
     *
     * @param nodePath
     * @return
     */
    public String getBaseVersion(String nodePath);

    /**
     * Returns the first checked in version.
     *
     * @param nodePath
     * @return
     */
    public String getRootVersion(String nodePath);

    /**
     * Check Out a node from a repository
     *
     * @param nodePath
     */
    public void checkOutContentObject(String nodePath);

    /**
     * Check Out a node from a repository. If recursive is set to TRUE all
     * related sub nodes and the parent node will also be checked out.
     *
     * @param nodePath
     * @param recursiv
     */
    public void checkOutContentObject(String nodePath, boolean recursiv);

    /**
     * Saves the state of the session and checks in all checkout content nodes.
     */
    public void checkInContentAndSaveState();

    /**
     * Add a node path to a list of node path which will be checked in when the
     * state is saved. This is to add new created nodes which could not checked
     * out before.
     *
     * @param nodePath
     */
    public void addContentToCheckInList(String nodePath);

}
