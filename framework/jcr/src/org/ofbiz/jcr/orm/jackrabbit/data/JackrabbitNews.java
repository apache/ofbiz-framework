package org.ofbiz.jcr.orm.jackrabbit.data;

import java.util.Calendar;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;

@Node(extend = JackrabbitLocalizedContent.class)
public class JackrabbitNews extends JackrabbitLocalizedContent {

    @Field(id = true)
    String title = null;
    @Field(jcrType = "Date")
    Calendar pubDate = null;
    @Field
    String content = null;

    public JackrabbitNews() {
        super();
        // create an empty object
    }

    public JackrabbitNews(String nodePath, String language, String title, Calendar pubDate, String content) {
        super(nodePath, language);
        this.title = title;
        this.pubDate = pubDate;
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Calendar getPubDate() {
        return pubDate;
    }

    public void setPubDate(Calendar pubDate) {
        this.pubDate = pubDate;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

}
