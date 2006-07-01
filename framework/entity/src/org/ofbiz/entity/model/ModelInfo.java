/*
 * $Id: ModelInfo.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ofbiz.entity.model;

import org.ofbiz.base.util.UtilXml;
import org.w3c.dom.Element;

/**
 * Generic Entity - Entity model class
 *
 * @author     <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.0
 */
public class ModelInfo {
    
    public static final String module = ModelInfo.class.getName();

    protected ModelInfo def;
    /** The title for documentation purposes */
    protected String title = "";

    /** The description for documentation purposes */
    protected String description = "";

    /** The copyright for documentation purposes */
    protected String copyright = "";

    /** The author for documentation purposes */
    protected String author = "";

    /** The version for documentation purposes */
    protected String version = "";

    // ===== CONSTRUCTORS =====

    public ModelInfo() {
        this(DEFAULT);
    }

    public ModelInfo(ModelInfo def) {
        this.def = def;
    }

    public static final ModelInfo DEFAULT = new ModelInfo() {
        public String getTitle()        { return "None"; }
        public String getAuthor()       { return "None"; }
        public String getCopyright()    { return "Copyright (c) 2001 The Open For Business Project - www.ofbiz.org"; }
        public String getVersion()      { return "1.0"; }
        public String getDescription()  { return "None"; }
    };

    public void populateFromAttributes(Element element) {
        author = element.getAttribute("author");
        copyright = element.getAttribute("copyright");
        description = UtilXml.childElementValue(element, "description");
        title = element.getAttribute("title");
        version = element.getAttribute("version");
    }

    public void populateFromElements(Element element) {
        author = UtilXml.childElementValue(element, "author");
        copyright = UtilXml.childElementValue(element, "copyright");
        description = UtilXml.childElementValue(element, "description");
        title = UtilXml.childElementValue(element, "title");
        version = UtilXml.childElementValue(element, "version");
    }

    // Strings to go in the comment header.
    /** The title for documentation purposes */
    public String getTitle() {
        return this.title != null ? this.title : def.getTitle();
    }

    public void setTitle(String title) {
        this.title = title;
    }

    /** The description for documentation purposes */
    public String getDescription() {
        return this.description != null ? this.description : def.getDescription();
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /** The copyright for documentation purposes */
    public String getCopyright() {
        return this.copyright != null ? this.copyright : def.getCopyright();
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    /** The author for documentation purposes */
    public String getAuthor() {
        return this.author != null ? this.author : def.getAuthor();
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    /** The version for documentation purposes */
    public String getVersion() {
        return this.version != null ? this.version : def.getVersion();
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
