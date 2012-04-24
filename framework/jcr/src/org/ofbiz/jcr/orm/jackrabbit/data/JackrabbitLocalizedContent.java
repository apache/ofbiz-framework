package org.ofbiz.jcr.orm.jackrabbit.data;

import java.util.GregorianCalendar;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;
import org.ofbiz.jcr.access.jackrabbit.ConstantsJackrabbit;

@Node(isAbstract = true, extend = JackrabbitUnstructured.class)
public abstract class JackrabbitLocalizedContent extends JackrabbitUnstructured {

    @Field
    private String language;

    public JackrabbitLocalizedContent() {
        super();
        this.language = "";
        super.setLocalized(true);
        super.setCreationDate(new GregorianCalendar());
        // create an empty localized object
    }

    /**
     *
     * @param nodePath
     * @param language
     */
    public JackrabbitLocalizedContent(String nodePath, String language) {
        super(nodePath);
        this.language = language;

        super.setPath(createLanguagePath(nodePath));
        // define this node as a localized node
        super.setLocalized(true);
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    private String createLanguagePath(String contentPath) {
        // the content path should contain the language information
        // TODO this have to be a little bit more intelligent in the future
        if (contentPath.endsWith(ConstantsJackrabbit.NODEPATHDELIMITER)) {
            contentPath = contentPath + language;
        } else {
            contentPath = contentPath + ConstantsJackrabbit.NODEPATHDELIMITER + language;
        }

        return contentPath;
    }

}
