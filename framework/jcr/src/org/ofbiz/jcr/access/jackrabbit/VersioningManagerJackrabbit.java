package org.ofbiz.jcr.access.jackrabbit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.version.Version;
import org.apache.jackrabbit.ocm.version.VersionIterator;
import org.ofbiz.base.util.Debug;
import org.ofbiz.jcr.access.VersioningManager;
import org.ofbiz.jcr.util.jackrabbit.JackrabbitUtils;

public class VersioningManagerJackrabbit implements VersioningManager {

    private final static String module = VersioningManagerJackrabbit.class.getName();

    private final ObjectContentManager ocm;

    private final Set<String> checkedOutNodeStore = Collections.synchronizedSet(new HashSet<String>());
    private final static String NOVERSION = "-0.0";

    VersioningManagerJackrabbit(ObjectContentManager ocm) {
        this.ocm = ocm;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ofbiz.jcr.access.VersioningManager#getVersionList(java.lang.String)
     */
    @Override
    public List<String> getVersionList(String nodePath) {
        List<String> result = new ArrayList<String>();
        VersionIterator versionIterator = null;
        try {
            versionIterator = this.ocm.getAllVersions(nodePath);
        } catch (VersionException e) {
            Debug.logError(e, module);
            return result;
        }

        // write the versions to the array list.
        while (versionIterator.hasNext()) {
            Version version = (Version) versionIterator.next();
            // filter the root version string, because it's not needed each node
            // starts with the version number 1.0
            if (isNotRootVersion(version)) {
                result.add(version.getName());
            }
        }

        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ofbiz.jcr.access.VersioningManager#checkIfVersionExist(java.lang.
     * String, java.lang.String)
     */
    @Override
    public boolean checkIfVersionExist(String nodePath, String version) {
        return getVersionList(nodePath).contains(version);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ofbiz.jcr.access.VersioningManager#checkOutContentObject(java.lang
     * .String)
     */
    @Override
    public void checkOutContentObject(String nodePath) {
        // check if the parent node is the root node, because the
        // root node can not be checked out.
        if (JackrabbitUtils.isARootNode(nodePath)) {
            return;
        }

        try {
            if (doNodeExist(nodePath) && isNodeNotCheckedOut(nodePath)) {
                this.ocm.checkout(nodePath);
                this.addContentToCheckInList(nodePath);
            }
        } catch (VersionException e) {
            Debug.logError(e, module);
        } catch (UnsupportedRepositoryOperationException e) {
            Debug.logError(e, module);
        } catch (RepositoryException e) {
            Debug.logError(e, module);
        }
    }

    public void checkOutContentObject(String nodePath, boolean checkOutAllSubnodes) {
        if (checkOutAllSubnodes) {
            try {
                checkOutRelatedNodes(nodePath);
            } catch (RepositoryException e) {
                Debug.logError(e, module);
            }
        } else {
            checkOutContentObject(nodePath);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ofbiz.jcr.access.VersioningManager#addContentToCheckInList(java.lang
     * .String)
     */
    @Override
    public void addContentToCheckInList(String nodePath) {
        checkedOutNodeStore.add(nodePath);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ofbiz.jcr.access.VersioningManager#checkInContentAndSaveState()
     */
    @Override
    public void checkInContentAndSaveState() {
        ocm.save();

        try {
            for (String nodePath : checkedOutNodeStore) {
                checkinNode(nodePath);
            }

            // reset the node store after everything is checked in
            checkedOutNodeStore.clear();
        } catch (VersionException e) {
            Debug.logError(e, module);
        } catch (UnsupportedRepositoryOperationException e) {
            Debug.logError(e, module);
        } catch (RepositoryException e) {
            Debug.logError(e, module);
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ofbiz.jcr.access.VersioningManager#getBaseVersion(java.lang.String)
     */
    @Override
    public String getBaseVersion(String nodePath) {
        try {
            return ocm.getBaseVersion(nodePath).getName();
        } catch (VersionException e) {
            Debug.logError(e, module);
            return NOVERSION;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ofbiz.jcr.access.VersioningManager#getRootVersion(java.lang.String)
     */
    @Override
    public String getRootVersion(String nodePath) {
        try {
            return ocm.getRootVersion(nodePath).getName();
        } catch (VersionException e) {
            Debug.logError(e, module);
            return NOVERSION;
        }
    }

    /**
     * Checks out recursively all related nodes (parent, all child's (if exists)
     * and the node itself)
     *
     * @param startNode
     * @throws RepositoryException
     */
    private void checkOutRelatedNodes(String startNodePath) throws RepositoryException {
        Node startNode = getNode(startNodePath);
        List<String> nodesToCheckOut = new ArrayList<String>();

        nodesToCheckOut.add(startNodePath);
        nodesToCheckOut.add(startNode.getParent().getPath());

        if (startNode.hasNodes()) {
            List<String> allChildNodes = getAllChildNodes(startNode);
            nodesToCheckOut.addAll(allChildNodes);
        }

        for (String node : nodesToCheckOut) {
            this.checkOutContentObject(node);
        }

    }

    /**
     * Return recursively all child nodes
     *
     * @param startNode
     * @return
     * @throws RepositoryException
     */
    private List<String> getAllChildNodes(Node startNode) throws RepositoryException {
        List<String> nodes = new ArrayList<String>();

        NodeIterator subNodeIterator = startNode.getNodes();
        while (subNodeIterator.hasNext()) {
            Node subNode = subNodeIterator.nextNode();

            if (subNode.hasNodes()) {
                nodes.addAll(getAllChildNodes(subNode));
            }

            nodes.add(subNode.getPath());
        }

        return nodes;
    }

    private void checkinNode(String nodePath) throws RepositoryException, UnsupportedRepositoryOperationException, VersionException {
        if (doNodeExist(nodePath) && this.isNodeCheckedOut(nodePath)) {
            this.ocm.checkin(nodePath);
        }
    }

    private Node getNode(String startNodePath) throws PathNotFoundException, RepositoryException {
        return ocm.getSession().getNode(startNodePath);
    }

    private boolean isNotRootVersion(Version version) {
        return !ConstantsJackrabbit.ROOTVERSION.equals(version.getName());
    }

    private boolean doNodeExist(String nodePath) throws RepositoryException {
        return this.ocm.getSession().nodeExists(nodePath);
    }

    private boolean isNodeNotCheckedOut(String nodePath) throws RepositoryException, UnsupportedRepositoryOperationException {
        return !isNodeCheckedOut(nodePath);
    }

    private boolean isNodeCheckedOut(String nodePath) throws UnsupportedRepositoryOperationException, RepositoryException {
        boolean isNodeMarkedAsCheckedOut = this.ocm.getSession().getWorkspace().getVersionManager().isCheckedOut(nodePath);
        boolean isNodeInCheckedOutNodeStore = checkedOutNodeStore.contains(nodePath);

        return (isNodeMarkedAsCheckedOut && isNodeInCheckedOutNodeStore);
    }
}
