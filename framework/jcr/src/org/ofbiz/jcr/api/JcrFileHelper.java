package org.ofbiz.jcr.api;

import java.io.InputStream;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.ofbiz.jcr.orm.jackrabbit.JackrabbitHierarchyNode;

public interface JcrFileHelper extends JcrHelper{

    /**
     * Returns a content file object from the repository. Throws an Exception
     * when the read content type is not an article content type.
     *
     * @param contentPath
     * @return
     * @throws ClassCastException
     * @throws PathNotFoundException
     */
    public abstract JackrabbitHierarchyNode getRepositoryContent(String contentPath) throws ClassCastException, PathNotFoundException;

    /**
     * Returns a content file object in the passed version from the repository.
     * Throws an Exception when the read content type is not an article content
     * type.
     *
     * @param contentPath
     * @return
     * @throws ClassCastException
     * @throws PathNotFoundException
     */
    public abstract JackrabbitHierarchyNode getRepositoryContent(String contentPath, String version) throws ClassCastException, PathNotFoundException;

    /**
     * Stores a new file content object in the repository.
     *
     * @param fileData
     * @param fileName
     * @param folderPath
     * @param mimeType
     * @throws ObjectContentManagerException
     * @throws RepositoryException
     */
    public abstract void storeContentInRepository(byte[] fileData, String fileName, String folderPath) throws ObjectContentManagerException, RepositoryException;

    /**
     * Stores a new file content object in the repository.
     *
     * @param fileData
     * @param fileName
     * @param folderPath
     * @param mimeType
     * @throws ObjectContentManagerException
     * @throws RepositoryException
     */
    public abstract void storeContentInRepository(InputStream fileData, String fileName, String folderPath) throws ObjectContentManagerException, RepositoryException;

    /**
     * Returns TRUE if the current content is a file content (Type:
     * OfbizRepositoryMappingJackrabbitFile)
     *
     * @return
     */
    public abstract boolean isFileContent();

    /**
     * Returns TRUE if the current content is a folder content (Type:
     * OfbizRepositoryMappingJackrabbitFolder)
     *
     * @return
     */
    public abstract boolean isFolderContent();

}