package org.ofbiz.jcr.orm.jackrabbit.data;

import java.util.Calendar;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;

@Node(extend = JackrabbitLocalizedContent.class)
public class JackrabbitArticle extends JackrabbitLocalizedContent {

    @Field(id = true)
    String title = null;
    @Field
    String content = null;
    @Field
    Calendar pubDate = null;

    /**
     *
     * @param nodePath
     * @param language
     * @param title
     * @param content
     * @param pubDate
     */
    public JackrabbitArticle(String nodePath, String language, String title, String content, Calendar pubDate) {
        super(nodePath, language);

        this.title = title;
        this.content = content;
        this.pubDate = pubDate;
    }

    /**
     *
     */
    public JackrabbitArticle() {
        super();
        // create empty instance
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Calendar getPubDate() {
        return pubDate;
    }

    public void setPubDate(Calendar pubDate) {
        this.pubDate = pubDate;
    }
}
