package org.ofbiz.jcr.api.jackrabbit;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.jcr.access.jackrabbit.ConstantsJackrabbit;
import org.ofbiz.jcr.access.jackrabbit.JackrabbitRepositoryAccessor;
import org.ofbiz.jcr.api.JcrDataHelper;
import org.ofbiz.jcr.orm.OfbizRepositoryMapping;
import org.ofbiz.jcr.orm.jackrabbit.data.JackrabbitArticle;
import org.ofbiz.jcr.util.jackrabbit.JackrabbitUtils;

/**
 * This Helper class encapsulate the jcr article content bean. it provide all
 * attributes and operations which are necessary to work with the content
 * repository.
 *
 * The concrete implementations covers the different content use case related
 * workflows. I.E. Different behavior for File/Folder or Text content.
 *
 * The Helper classes should be build on top of the generic JCR implementation
 * in the Framework.
 *
 */
public class JackrabbitArticleHelper extends JackrabbitAbstractHelper implements JcrDataHelper {

    private final static String module = JackrabbitArticleHelper.class.getName();

    private JackrabbitArticle article = null;
    private static List<String> possibleLocales = null;

    static {
        if (UtilValidate.isEmpty(possibleLocales)) {
            possibleLocales = new ArrayList<String>();
            List<Locale> locales = org.ofbiz.base.util.UtilMisc.availableLocales();
            for (Locale locale : locales) {
                possibleLocales.add(locale.toString());
            }
        }
    }

    /**
     * Setup my content Object
     */
    public JackrabbitArticleHelper(GenericValue userLogin) {
        super(new JackrabbitRepositoryAccessor(userLogin));
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ofbiz.jcr.api.jackrabbit.DataHelper#readContentFromRepository(java
     * .lang.String)
     */
    @Override
    public JackrabbitArticle readContentFromRepository(String contentPath) throws ClassCastException, PathNotFoundException {
        return readContentFromRepository(contentPath, "");
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ofbiz.jcr.api.jackrabbit.DataHelper#readContentFromRepository(java
     * .lang.String, java.lang.String)
     */
    @Override
    public JackrabbitArticle readContentFromRepository(String contentPath, String language) throws ClassCastException, PathNotFoundException {
        contentPath = determineContentLanguagePath(contentPath, language);

        OfbizRepositoryMapping orm = super.access.getContentObject(contentPath);
        setArticleContentObject(orm);

        article.setVersion(super.access.getBaseVersion(contentPath));
        return article;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ofbiz.jcr.api.jackrabbit.DataHelper#readContentFromRepository(java
     * .lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public JackrabbitArticle readContentFromRepository(String contentPath, String language, String version) throws ClassCastException, PathNotFoundException {
        contentPath = determineContentLanguagePath(contentPath, language);

        OfbizRepositoryMapping orm = super.access.getContentObject(contentPath, version);
        setArticleContentObject(orm);

        // manipulating the content path, because the jackrabbit orm
        // returns a content path with version information.
        article.setPath(contentPath);
        return article;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ofbiz.jcr.api.jackrabbit.DataHelper#storeContentInRepository(java
     * .lang.String, java.lang.String, java.lang.String, java.lang.String,
     * java.util.Calendar)
     */
    @Override
    public void storeContentInRepository(String contentPath, String language, String title, String content, Calendar publicationDate) throws ObjectContentManagerException, ItemExistsException {
        if (UtilValidate.isEmpty(language)) {
            language = JackrabbitUtils.determindeTheDefaultLanguage();
        }

        article = new JackrabbitArticle(contentPath, language, title, content, publicationDate);
        super.access.storeContentObject(article);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ofbiz.jcr.api.jackrabbit.DataHelper#updateContentInRepository(org
     * .ofbiz.jcr.orm.jackrabbit.OfbizRepositoryMappingJackrabbitArticle)
     */
    @Override
    public void updateContentInRepository(JackrabbitArticle updatedArticle) throws RepositoryException, ObjectContentManagerException {
        // if the item not already exist create it.
        if (!super.access.checkIfNodeExist(updatedArticle.getPath())) {
            Debug.logWarning("This content object with the path: " + updatedArticle.getPath() + " doesn't exist in the repository. It will now created.", module);
            this.storeContentInRepository(updatedArticle.getPath(), updatedArticle.getLanguage(), updatedArticle.getTitle(), updatedArticle.getContent(), updatedArticle.getPubDate());
            return;
        }

        super.access.updateContentObject(updatedArticle);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.ofbiz.jcr.api.jackrabbit.DataHelper#getVersionListForCurrentArticle()
     */
    @Override
    public List<String> getVersionListForCurrentArticle() {
        List<String> versions = new ArrayList<String>();

        if (article != null) {
            versions = super.access.getVersionList(article.getPath());
        } else {
            Debug.logWarning("No Article is loaded from the repository, please load an article first before requesting the version list.", module);
        }

        return versions;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ofbiz.jcr.api.jackrabbit.DataHelper#getAvailableLanguageList()
     */
    @Override
    public List<String> getAvailableLanguageList() {
        List<String> availableLanguages = new ArrayList<String>();

        if (article == null || !article.getLocalized()) {
            Debug.logWarning("No Article is loaded from the repository, please load an article first before requesting the version list.", module);
            return availableLanguages;
        }

        Session session = super.access.getSession();

        try {
            Node node = session.getNode(article.getPath()).getParent();
            NodeIterator nodeList = node.getNodes();

            while (nodeList.hasNext()) {
                Node currentNodeToExtractLanguage = nodeList.nextNode();
                // only use nodes which have the language mix in
                if (checkIfNodeHaveValidLanguageMixIn(currentNodeToExtractLanguage)) {
                    String languageFlag = extractLanguageFlagFromNodePath(currentNodeToExtractLanguage.getPath());
                    availableLanguages.add(languageFlag);
                }
            }

        } catch (PathNotFoundException e) {
            Debug.logError(e, module);
        } catch (RepositoryException e) {
            Debug.logError(e, module);
        }

        return availableLanguages;
    }

    /**
     * This method should determine the correct language for the content. It
     * covers the case when the passed language is not available.
     *
     * A default (system) language will be taken, if the passed language, does
     * not exist, if no default language node is specified the first language
     * node which will be found will be choose.
     *
     * @param contentPath
     * @param contentLanguage
     * @return
     * @throws PathNotFoundException
     */
    private String determineContentLanguagePath(String contentPath, String contentLanguage) throws PathNotFoundException {
        // return if only the root node path is requested
        if (JackrabbitUtils.isARootNode(contentPath)) {
            return contentPath;
        }

        // contentLanaguage should never be null, because the concatenation
        // goes wrong if the String have a null value.
        if (contentLanguage == null) {
            contentLanguage = "";
        }

        String canonicalizedContentPath = canonicalizeContentPath(contentPath, contentLanguage);

        // Step 1.) Check if the requested node language combination exist and
        // if it have a valid localize flag
        // Step 2.) If the first condition is not true, check the combination
        // from node path and default language have a valid node result
        // Step 3.) If condition one and two are false, determine the first node
        // which have a valid language flag

        if (super.access.checkIfNodeExist(canonicalizedContentPath + contentLanguage) && checkIfNodeHaveValidLanguageMixIn(canonicalizedContentPath + contentLanguage)) {
            contentPath = canonicalizedContentPath + contentLanguage;

        } else if (super.access.checkIfNodeExist(canonicalizedContentPath + JackrabbitUtils.determindeTheDefaultLanguage())) {
            contentPath = canonicalizedContentPath + JackrabbitUtils.determindeTheDefaultLanguage();

        } else {
            contentPath = determineFirstAvailableLanguageNode(canonicalizedContentPath);
        }

        return contentPath;
    }

    /**
     * Iterate over all child nodes and returns the first with a valid language
     * flag.
     *
     * @param canonicalizedContentPath
     * @return
     * @throws PathNotFoundException
     */
    private String determineFirstAvailableLanguageNode(String canonicalizedContentPath) throws PathNotFoundException {
        String contentPath = "";

        try {
            // return the first available language
            NodeIterator childNodes = super.access.getSession().getNode(canonicalizedContentPath).getNodes();
            while (childNodes.hasNext()) {
                Node child = childNodes.nextNode();
                if (possibleLocales.contains(child.getName()) && checkIfNodeHaveValidLanguageMixIn(child)) {
                    contentPath = child.getPath();
                    break;
                }
            }
            childNodes = null;
        } catch (PathNotFoundException pnf) {
            throw new PathNotFoundException(pnf);
        } catch (RepositoryException e) {
            Debug.logError(e, module);
        }
        return contentPath;
    }

    /**
     * We check if the content path already contains a language. If a language
     * is found it will be removed from the path string.
     *
     * @param contentPath
     * @return
     */
    private String canonicalizeContentPath(String contentPath, String contentLanguage) {
        // we split the path string in chunks
        String[] splitContentPath = contentPath.split(ConstantsJackrabbit.NODEPATHDELIMITER);

        String canonicalizedCotnentPath = "";
        // check if the last chunk contains a language flags, which is part of
        // our locale list.
        // It's possible to request a node directly with the language in the
        // URL. If the node path contains a language and the language should not
        // be changed (contentLanaguage is empty), the language flag stay in the
        // content path. Otherwise it will be removed.
        if (UtilValidate.isNotEmpty(contentLanguage) && possibleLocales.contains(splitContentPath[splitContentPath.length - 1])) {
            canonicalizedCotnentPath = buildCanonicalizeContentPath(splitContentPath, splitContentPath.length - 1);
        } else {
            canonicalizedCotnentPath = buildCanonicalizeContentPath(splitContentPath, splitContentPath.length);
        }

        return canonicalizedCotnentPath;
    }

    /**
     * Build a new path string from the split content path. A loop iterates
     * throw the array until lastFieldWhichShouldBeAddedToTheNewPathString is
     * reached. That means if you want to avoid that the last field of the array
     * is added to the string lastFieldWhichShouldBeAddedToTheNewPathString
     * should be array.length -1.
     *
     * @param splitContentPath
     * @param lastFieldWhichShouldBeAddedToTheNewPathString
     * @return
     */
    private String buildCanonicalizeContentPath(String[] splitContentPath, int lastFieldWhichShouldBeAddedToTheNewPathString) {
        StringBuffer canonicalizedContentPath = new StringBuffer(ConstantsJackrabbit.ROOTPATH);

        for (int i = 0; i < lastFieldWhichShouldBeAddedToTheNewPathString; i++) {
            if (UtilValidate.isNotEmpty(splitContentPath[i])) {
                canonicalizedContentPath.append(splitContentPath[i]).append(ConstantsJackrabbit.NODEPATHDELIMITER);
            }
        }
        return canonicalizedContentPath.toString();
    }

    /**
     * This extract the language flag from the node path, normally the language
     * should be the substring after the last / (slash).
     *
     * @param nodePathWithLanguageFlag
     * @return
     */
    private String extractLanguageFlagFromNodePath(String nodePathWithLanguageFlag) {
        int startPointAfterLastSlash = nodePathWithLanguageFlag.lastIndexOf(ConstantsJackrabbit.NODEPATHDELIMITER) + 1;

        return nodePathWithLanguageFlag.substring(startPointAfterLastSlash);
    }

    /**
     * Checks if a node have the property <code>localized</code> and if this
     * property is be <code>true</code>. If an exception occurs false will be
     * returned.
     *
     * @param nodePathWhichShouldHaveAValidLanguageFlag
     * @return
     */
    private boolean checkIfNodeHaveValidLanguageMixIn(String nodePathWhichShouldHaveAValidLanguageFlag) {
        try {
            Node node = super.access.getSession().getNode(nodePathWhichShouldHaveAValidLanguageFlag);
            return checkIfNodeHaveValidLanguageMixIn(node);
        } catch (RepositoryException e) {
            Debug.logError(e, module);
            return false;
        }
    }

    /**
     * Checks if a node have the property <code>localized</code> and if this
     * property is <code>true</code>
     *
     * @param nodeWhichShouldHaveAValidLanguageFlag
     * @return
     * @throws RepositoryException
     * @throws ValueFormatException
     * @throws PathNotFoundException
     */
    private boolean checkIfNodeHaveValidLanguageMixIn(Node nodeWhichShouldHaveAValidLanguageFlag) throws RepositoryException, ValueFormatException, PathNotFoundException {
        boolean hasProperty = nodeWhichShouldHaveAValidLanguageFlag.hasProperty("localized");

        return hasProperty && nodeWhichShouldHaveAValidLanguageFlag.getProperty("localized").getBoolean();
    }

    /**
     * Set the article class variable if the <code>orm</code> Object is an
     * instance of <code>JackrabbitArticle</code>. Throws a ClassCastException
     * if the <code>orm</code> Object is from another type.
     *
     * @param orm
     * @throws ClassCastException
     * @return
     */
    private void setArticleContentObject(OfbizRepositoryMapping orm) throws ClassCastException {
        if (orm != null && orm instanceof JackrabbitArticle) {
            article = (JackrabbitArticle) orm;
            return;
        }

        throw new ClassCastException("The content object for the path: " + orm.getPath() + " is not an article content object. This Helper can only handle content objects with the type: " + JackrabbitArticle.class.getName());
    }
}
