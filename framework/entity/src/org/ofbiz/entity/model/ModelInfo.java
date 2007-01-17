/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.ofbiz.entity.model;

import org.ofbiz.base.util.UtilXml;
import org.w3c.dom.Element;

/**
 * Generic Entity - Entity model class
 *
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
        public String getCopyright()    { return "Copyright 2001-2006 The Apache Software Foundation"; }
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
