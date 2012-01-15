package org.ofbiz.jcr.access.jackrabbit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.jackrabbit.ocm.version.Version;
import org.apache.jackrabbit.ocm.version.VersionIterator;
import org.ofbiz.base.util.Debug;
import org.ofbiz.jcr.access.VersioningManager;

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
            // return an empty List
            return result;
        }

        // write the versions to the array list.
        while (versionIterator.hasNext()) {
            Version version = (Version) versionIterator.next();
            // filter the root version string, because it's not needed each node
            // starts with the version number 1.0
            if (!ConstantsJackrabbit.ROOTVERSION.equals(version.getName())) {
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
        if (ConstantsJackrabbit.ROOTPATH.equals(nodePath)) {
            return;
        }

        try {
            if (this.ocm.getSession().nodeExists(nodePath) && !this.ocm.getSession().getWorkspace().getVersionManager().isCheckedOut(nodePath) && !checkedOutNodeStore.contains(nodePath)) {
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

    public void checkOutContentObject(String nodePath, boolean recursiv) {
        if (recursiv) {
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
        if (ocm != null) {
            ocm.save();
        }

        try {
            for (String nodePath : checkedOutNodeStore) {
                // add the new resource content to the version history
                if (this.ocm.getSession().nodeExists(nodePath) && this.ocm.getSession().getWorkspace().getVersionManager().isCheckedOut(nodePath)) {
                    this.ocm.checkin(nodePath);
                }
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
    protected void checkOutRelatedNodes(String startNodePath) throws RepositoryException {
        List<String> nodesToCheckOut = new ArrayList<String>();
        nodesToCheckOut.add(startNodePath);
        nodesToCheckOut.add(ocm.getSession().getNode(startNodePath).getParent().getPath());
        if (ocm.getSession().getNode(startNodePath).hasNodes()) {
            nodesToCheckOut.addAll(getAllChildNodes(startNodePath));
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
    private List<String> getAllChildNodes(String startNode) throws RepositoryException {
        List<String> nodes = new ArrayList<String>();
        NodeIterator ni = ocm.getSession().getNode(startNode).getNodes();
        while (ni.hasNext()) {
            Node nextNode = ni.nextNode();
            if (nextNode.hasNodes()) {
                nodes.addAll(getAllChildNodes(nextNode.getPath()));
            }

            nodes.add(nextNode.getPath());
        }

        return nodes;
    }
}
