package org.ofbiz.jcr.access.jackrabbit;

import java.util.List;

import javax.jcr.ItemExistsException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryResult;

import net.sf.json.JSONArray;

import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.jackrabbit.ocm.manager.impl.ObjectContentManagerImpl;
import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.jcr.access.ContentReader;
import org.ofbiz.jcr.access.ContentWriter;
import org.ofbiz.jcr.access.JcrRepositoryAccessor;
import org.ofbiz.jcr.access.VersioningManager;
import org.ofbiz.jcr.loader.JCRFactoryUtil;
import org.ofbiz.jcr.loader.jackrabbit.JCRFactoryImpl;
import org.ofbiz.jcr.orm.OfbizRepositoryMapping;

public class JackrabbitRepositoryAccessor implements JcrRepositoryAccessor {

    private final static String module = JackrabbitRepositoryAccessor.class.getName();

    private final Session session;
    private final ObjectContentManagerImpl ocm;

    /**
     * Create a repository Access object based on the userLogin.
     *
     * @param userLogin
     */
    public JackrabbitRepositoryAccessor(GenericValue userLogin) {
        // TODO pass the userLogin to the getSession() method and perform some
        this(JCRFactoryUtil.getSession());
    }

    /**
     * Create a repository Access object based on a JCR Session.
     *
     * @param userLogin
     */
    public JackrabbitRepositoryAccessor(Session session) {
        this.session = session;
        this.ocm = new ObjectContentManagerImpl(session, JCRFactoryImpl.getMapper());

        return;
    }

    /**
     * Returns the Jackrabbit session object.
     *
     * @return
     */
    public Session getSession() {
        return this.session;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ofbiz.jcr.orm.RepositoryAccess#closeAccess()
     */
    @Override
    public void closeAccess() {
        if (this.ocm != null && this.ocm.getSession().isLive()) {
            this.ocm.logout();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ofbiz.jcr.orm.RepositoryAccess#getContentObject(java.lang.String)
     */
    @Override
    public OfbizRepositoryMapping getContentObject(String nodePath) throws PathNotFoundException {
        ContentReader contentReader = new ContentReaderJackrabbit(this.ocm);
        return contentReader.getContentObject(nodePath);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ofbiz.jcr.access.RepositoryAccess#getContentObject(java.lang.String,
     * java.lang.String)
     */
    @Override
    public OfbizRepositoryMapping getContentObject(String nodePath, String version) throws PathNotFoundException {
        ContentReader contentReader = new ContentReaderJackrabbit(this.ocm);
        return contentReader.getContentObject(nodePath, version);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ofbiz.jcr.orm.RepositoryAccess#storeContentObject(org.ofbiz.jcr.orm
     * .OfbizRepositoryMapping)
     */
    @Override
    public void storeContentObject(OfbizRepositoryMapping orm) throws ObjectContentManagerException, ItemExistsException {
        ContentWriter contentWriter = new ContentWriterJackrabbit(this.ocm);
        contentWriter.storeContentObject(orm);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ofbiz.jcr.orm.RepositoryAccess#updateContentObject(org.ofbiz.jcr.
     * orm.OfbizRepositoryMapping)
     */
    @Override
    public void updateContentObject(OfbizRepositoryMapping orm) throws ObjectContentManagerException {
        ContentWriter contentWriter = new ContentWriterJackrabbit(this.ocm);
        contentWriter.updateContentObject(orm);

        return;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ofbiz.jcr.orm.RepositoryAccess#removeContentObject(java.lang.String)
     */
    @Override
    public void removeContentObject(String nodePath) throws ObjectContentManagerException {
        ContentWriter contentWriter = new ContentWriterJackrabbit(this.ocm);
        contentWriter.removeContentObject(nodePath);

        return;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ofbiz.jcr.orm.RepositoryAccess#removeContentObject(org.ofbiz.jcr.
     * orm.OfbizRepositoryMapping)
     */
    @Override
    public void removeContentObject(OfbizRepositoryMapping orm) throws ObjectContentManagerException {
        removeContentObject(orm.getPath());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ofbiz.jcr.access.RepositoryAccess#getVersionList(java.lang.String)
     */
    @Override
    public List<String> getVersionList(String nodePath) {
        VersioningManager versioningnManager = new VersioningManagerJackrabbit(this.ocm);
        return versioningnManager.getVersionList(nodePath);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ofbiz.jcr.access.RepositoryAccess#getBaseVersion(java.lang.String)
     */
    @Override
    public String getBaseVersion(String nodePath) {
        VersioningManager versioningnManager = new VersioningManagerJackrabbit(this.ocm);
        return versioningnManager.getBaseVersion(nodePath);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ofbiz.jcr.access.RepositoryAccess#getRootVersion(java.lang.String)
     */
    @Override
    public String getRootVersion(String nodePath) {
        VersioningManager versioningnManager = new VersioningManagerJackrabbit(this.ocm);
        return versioningnManager.getRootVersion(nodePath);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ofbiz.jcr.orm.RepositoryAccess#getJsonFileTree()
     */
    @Override
    public JSONArray getJsonDataTree() throws RepositoryException {
        ContentReader contentReader = new ContentReaderJackrabbit(this.ocm);
        return contentReader.getJsonDataTree();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ofbiz.jcr.access.RepositoryAccess#getJsonFileTree()
     */
    @Override
    public JSONArray getJsonFileTree() throws RepositoryException {
        ContentReader contentReader = new ContentReaderJackrabbit(this.ocm);
        return contentReader.getJsonFileTree();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ofbiz.jcr.access.RepositoryAccess#queryForRepositoryData(java.lang
     * .String)
     */
    @Override
    public QueryResult queryForRepositoryData(String query) throws RepositoryException {
        ContentReader contentReader = new ContentReaderJackrabbit(this.ocm);
        return contentReader.queryRepositoryData(query);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ofbiz.jcr.access.JcrRepositoryAccessor#checkIfNodeExist(java.lang
     * .String)
     */
    @Override
    public boolean checkIfNodeExist(String nodePathToCheck) {
        try {
            return getSession().itemExists(nodePathToCheck);
        } catch (RepositoryException e) {
            Debug.logError(e, module);
            return false;
        }
    }
}
