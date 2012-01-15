package org.ofbiz.jcr.util.jackrabbit;

import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.jcr.access.jackrabbit.ConstantsJackrabbit;
import org.ofbiz.jcr.loader.JCRFactoryUtil;

public class JcrUtilJackrabbit {

    public static final String module = JcrUtilJackrabbit.class.getName();

    /**
     * A method to list all nodes in the repository. The result List contains
     * the node path and the node type.
     *
     * @param startNodePath
     * @return
     * @throws RepositoryException
     */
    public static List<Map<String, String>> getRepositoryNodes(GenericValue userLogin, String startNodePath) throws RepositoryException {
        List<Map<String, String>> returnList = null;
        Session session = JCRFactoryUtil.getSession();

        try {
            returnList = getRepositoryNodes(session, startNodePath);
        } catch (RepositoryException e) {
            throw new RepositoryException(e);
        } finally {
            session.logout();
        }

        return returnList;
    }

    /**
     * Just a dummy method to list all nodes in the repository.
     *
     * @param startNodePath
     * @return
     * @throws RepositoryException
     */
    private static List<Map<String, String>> getRepositoryNodes(Session session, String startNodePath) throws RepositoryException {
        Node node = null;

        List<Map<String, String>> nodeList = FastList.newInstance();
        if (UtilValidate.isEmpty(startNodePath)) {
            node = session.getRootNode();
        } else {
            node = session.getNode(startNodePath);
        }

        NodeIterator nodeIterator = node.getNodes();
        Map<String, String> nodeEntry = null;
        while (nodeIterator.hasNext()) {
            Node n = nodeIterator.nextNode();

            // recursion - get all subnodes and add the results to our nodeList
            if (n.getNodes().hasNext()) {
                nodeList.addAll(getRepositoryNodes(session, n.getPath()));
            }

            nodeEntry = FastMap.newInstance();

            // if the node path is a jcr:system node than ignore this
            // entry
            if (n.getPath().startsWith("/jcr:system")) {
                continue;
            }

            nodeEntry.put("path", n.getPath());

            nodeEntry.put("primaryNodeType", n.getPrimaryNodeType().getName());

            nodeList.add(nodeEntry);
        }

        return nodeList;
    }

    /**
     * If the node path is not absolute (means starts with <code>/</code>), an
     * absolute path will be created.
     *
     * @param nodePath
     * @return
     */
    public static String createAbsoluteNodePath(String nodePath) {
        if (UtilValidate.isEmpty(nodePath)) {
            nodePath = ConstantsJackrabbit.ROOTPATH;
        } else if (!checkIfNodePathIsAbsolute(nodePath)) {
            nodePath = ConstantsJackrabbit.ROOTPATH + nodePath;
        }

        return nodePath;
    }

    /**
     * Returns true if the passed node path is an absolute path (starts with a
     * <code>/<code>).
     *
     * @param nodePath
     * @return
     */
    public static boolean checkIfNodePathIsAbsolute(String nodePath) {
        return nodePath.startsWith(ConstantsJackrabbit.ROOTPATH);
    }

    /**
     * Return default language from property file.
     *
     * @return
     */
    public static String determindeTheDefaultLanguage() {
        return UtilProperties.getPropertyValue("general", "locale.properties.fallback");
    }
}
