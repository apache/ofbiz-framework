package org.ofbiz.jcr.access.jackrabbit;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.ofbiz.base.util.Debug;
import org.ofbiz.jcr.access.ContentReader;
import org.ofbiz.jcr.access.VersioningManager;
import org.ofbiz.jcr.orm.OfbizRepositoryMapping;
import org.ofbiz.jcr.util.jackrabbit.JcrUtilJackrabbit;

public class ContentReaderJackrabbit implements ContentReader {

    private final static String module = ContentReaderJackrabbit.class.getName();

    private final ObjectContentManager ocm;

    public ContentReaderJackrabbit(ObjectContentManager ocm) {
        this.ocm = ocm;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ofbiz.jcr.access.ContentReader#getContentObject(java.lang.String)
     */
    @Override
    public OfbizRepositoryMapping getContentObject(String nodePath) throws PathNotFoundException{
        nodePath = JcrUtilJackrabbit.createAbsoluteNodePath(nodePath);
        OfbizRepositoryMapping orm = (OfbizRepositoryMapping) ocm.getObject(nodePath);
        try {
            if (orm != null) {
                orm.setVersion(ocm.getBaseVersion(nodePath).getName());
            }
        } catch (VersionException e) {
            // -0.0 means we have no version information
            orm.setVersion("-0.0");
            Debug.logError(e, module);
        }
        return orm;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ofbiz.jcr.access.ContentReader#getContentObject(java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @Override
    public OfbizRepositoryMapping getContentObject(String nodePath, String version) throws PathNotFoundException {
        nodePath = JcrUtilJackrabbit.createAbsoluteNodePath(nodePath);
        VersioningManager vm = new VersioningManagerJackrabbit(ocm);
        if (!vm.checkIfVersionExist(nodePath, version)) {
            Debug.logWarning("The version: " + version + " for content object: " + nodePath + " does not exist, the latest version for this object will be returned.", module);
            return getContentObject(nodePath);
        }

        OfbizRepositoryMapping orm = (OfbizRepositoryMapping) ocm.getObject(nodePath, version);
        orm.setVersion(version);
        return orm;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ofbiz.jcr.orm.ContentReader#getJsonDataTree()
     */
    @Override
    public JSONArray getJsonDataTree() throws RepositoryException {
        return getJsonDataChildNodes(ocm.getSession().getRootNode());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ofbiz.jcr.access.ContentReader#getJsonFileTree()
     */
    @Override
    public JSONArray getJsonFileTree() throws RepositoryException {
        return getJsonFileChildNodes(ocm.getSession().getRootNode());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ofbiz.jcr.access.ContentReader#queryRepositoryData(java.lang.String)
     */
    @Override
    public QueryResult queryRepositoryData(String query) throws RepositoryException {
        return executeQuery(query);
    }

    /**
     * Returns a JSON Array with the repository folder structure. The JSON array
     * is directly build for the jsTree jQuery plugin.
     *
     * @param startNode
     * @return
     * @throws RepositoryException
     */
    private JSONArray getJsonFileChildNodes(Node startNode) throws RepositoryException {
        NodeIterator nodeIterator = startNode.getNodes();

        JSONArray folderStrucutre = new JSONArray();
        JSONObject attr = new JSONObject();

        while (nodeIterator.hasNext()) {
            JSONObject folder = new JSONObject();
            Node node = nodeIterator.nextNode();

            if (node.getPrimaryNodeType().isNodeType(ConstantsJackrabbit.FOLDER)) {
                attr.element("title", node.getName());
                folder.element("data", attr);

                attr = new JSONObject();
                attr.element("NodePath", node.getPath());
                attr.element("NodeType", node.getPrimaryNodeType().getName());
                folder.element("attr", attr);

                folder.element("children", getJsonFileChildNodes(node).toString());

                folderStrucutre.element(folder);
            } else if (node.getPrimaryNodeType().isNodeType(ConstantsJackrabbit.FILE)) {
                attr = new JSONObject();
                attr.element("title", node.getName());
                folder.element("data", attr);

                attr = new JSONObject();
                attr.element("NodePath", node.getPath());
                attr.element("NodeType", node.getPrimaryNodeType().getName());
                folder.element("attr", attr);

                folderStrucutre.element(folder);
            }

        }

        return folderStrucutre;
    }

    /**
     * Returns a JSON Array with the repository text data structure. The JSON
     * array is directly build for the jsTree jQuery plugin.
     *
     * @param startNode
     * @return
     * @throws RepositoryException
     */
    private JSONArray getJsonDataChildNodes(Node startNode) throws RepositoryException {
        NodeIterator nodeIterator = startNode.getNodes();

        JSONArray folderStrucutre = new JSONArray();
        JSONObject attr = new JSONObject();

        while (nodeIterator.hasNext()) {
            JSONObject folder = new JSONObject();
            Node node = nodeIterator.nextNode();

            //
            if (node.getPrimaryNodeType().isNodeType(ConstantsJackrabbit.UNSTRUCTURED) && !node.hasProperty(ConstantsJackrabbit.MIXIN_LANGUAGE)) {
                attr.element("title", node.getName());
                folder.element("data", attr);

                attr = new JSONObject();
                attr.element("NodePath", node.getPath());
                attr.element("NodeType", node.getPrimaryNodeType().getName());
                folder.element("attr", attr);

                folder.element("children", getJsonDataChildNodes(node).toString());

                folderStrucutre.element(folder);
            }
        }

        return folderStrucutre;
    }

    /**
     * Executes the query specified by <code>statement</code> and returns the
     * query result.
     *
     * @param statement
     *            either a SQL2 or JQOM statement.
     * @return the query result.
     * @throws RepositoryException
     *             if an error occurs.
     */
    protected QueryResult executeQuery(String statement) throws RepositoryException {
        // TODO create a query manager which uses the OCM Layer.
        QueryManager qm = ocm.getSession().getWorkspace().getQueryManager();

        if (statement.trim().toLowerCase().startsWith("select")) {
            return qm.createQuery(statement, Query.JCR_SQL2).execute();
        } else {
            return qm.createQuery(statement, Query.JCR_JQOM).execute();
        }
    }

}
