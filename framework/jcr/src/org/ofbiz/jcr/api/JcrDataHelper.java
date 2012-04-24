package org.ofbiz.jcr.api;

import java.util.Calendar;
import java.util.List;

import javax.jcr.ItemExistsException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.ofbiz.jcr.orm.jackrabbit.data.JackrabbitArticle;

public interface JcrDataHelper extends JcrHelper{

    /**
     * Read the article content object from the repository. Throws an Exception
     * when the read content type is not an article content type.
     *
     * @param contentPath
     * @return content article object
     * @throws PathNotFoundException
     * @throws ClassCastException
     */
    public abstract JackrabbitArticle readContentFromRepository(String contentPath) throws ClassCastException, PathNotFoundException;

    /**
     * @throws PathNotFoundException
     * Read the article content object, in the passed language, from the
     * repository. if the language is not available, the default language will
     * be choose. Throws an Exception when the read content type is not an
     * article content type.
     *
     * @param contentPath
     * @return content article object
     * @throws PathNotFoundException
     * @throws ClassCastException
     */
    public abstract JackrabbitArticle readContentFromRepository(String contentPath, String language) throws ClassCastException, PathNotFoundException;

    /**
     * @throws PathNotFoundException
     * Read the article content object, in the passed language and version, from
     * the repository. if the language is not available, the default language
     * will be choose. Throws an Exception when the read content type is not an
     * article content type.
     *
     * @param contentPath
     * @param language
     * @param version
     * @return
     * @throws PathNotFoundException
     * @throws ClassCastException
     */
    public abstract JackrabbitArticle readContentFromRepository(String contentPath, String language, String version) throws ClassCastException, PathNotFoundException;

    /**
     * Stores a new article content object in the repository.
     *
     * @param contentPath
     * @param language
     * @param title
     * @param content
     * @param publicationDate
     * @throws ObjectContentManagerException
     * @throws ItemExistsException
     */
    public abstract void storeContentInRepository(String contentPath, String language, String title, String content, Calendar publicationDate) throws ObjectContentManagerException, ItemExistsException;

    /**
     * Update an existing content article object in the repository.
     *
     * @param updatedArticle
     * @param partyThatChangedThisContent
     * @throws RepositoryException
     * @throws ObjectContentManagerException
     */
    public abstract void updateContentInRepository(JackrabbitArticle updatedArticle) throws RepositoryException, ObjectContentManagerException;

    /**
     * Returns a list of versions which are available for the current article.
     * If no article is loaded before, the list will be empty.
     *
     * @return
     */
    public abstract List<String> getVersionListForCurrentArticle();

    public abstract List<String> getAvailableLanguageList();

}