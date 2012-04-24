package org.ofbiz.jcr.orm.jackrabbit.file;

import java.io.InputStream;
import java.util.Calendar;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.apache.jackrabbit.ocm.mapper.impl.annotation.Node;

@Node(jcrType = "nt:resource")
public class JackrabbitResource {

    @Field(jcrName = "jcr:mimeType")
    private String mimeType;
    @Field(jcrName = "jcr:data")
    private InputStream data;
    @Field(jcrName = "jcr:lastModified")
    private Calendar lastModified;

    public InputStream getData() {
        return data;
    }

    public void setData(InputStream data) {
        this.data = data;
    }

    public Calendar getLastModified() {
        return lastModified;
    }

    public void setLastModified(Calendar lastModified) {
        this.lastModified = lastModified;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

}
