package org.ofbiz.jcr.api;

import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

public interface JcrHelper {

    /**
     * This will close the connection to the content repository and make sure
     * that all changes a stored successfully.
     */
    public abstract void closeContentSession();

    /**
     * Remove the passed node from the content repository.
     *
     * @param contentPath
     */
    public abstract void removeContentObject(String contentPath);

    public abstract List<Map<String, String>> queryData(String query) throws RepositoryException;

}