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
package org.apache.ofbiz.entity.model;

import java.util.Locale;
import java.util.TimeZone;

import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilXml;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;entitymodel&gt;</code> child elements that provide default values.
 *
 */
@ThreadSafe
public final class ModelInfo {

    public static final ModelInfo DEFAULT = new ModelInfo("None", "None", getCopyrightString(), "None", "1.0", "");

    /**
     * Returns a new <code>ModelInfo</code> instance initialized to the values found in <code>element</code> attributes.
     * 
     * @param defaultInfo A <code>ModelInfo</code> instance that will provide default values for missing attributes.
     * @param element
     */
    public static ModelInfo createFromAttributes(ModelInfo defaultInfo, Element element) {
        String title = element.getAttribute("title").intern();
        if (title.isEmpty()) {
            title = defaultInfo.getTitle();
        }
        String description = StringUtil.internString(UtilXml.childElementValue(element, "description"));
        if (description == null || description.isEmpty()) {
            description = defaultInfo.getDescription();
        }
        String copyright = element.getAttribute("copyright").intern();
        if (copyright.isEmpty()) {
            copyright = defaultInfo.getCopyright();
        }
        String author = element.getAttribute("author").intern();
        if (author.isEmpty()) {
            author = defaultInfo.getAuthor();
        }
        String version = element.getAttribute("version").intern();
        if (version.isEmpty()) {
            version = defaultInfo.getVersion();
        }
        String defaultResourceName = StringUtil.internString(element.getAttribute("default-resource-name"));
        if (defaultResourceName.isEmpty()) {
            defaultResourceName = defaultInfo.getDefaultResourceName();
        }
        return new ModelInfo(title, description, copyright, author, version, defaultResourceName);
    }

    /**
     * Returns a new <code>ModelInfo</code> instance initialized to the values found in <code>element</code> child elements.
     * 
     * @param defaultInfo A <code>ModelInfo</code> instance that will provide default values for missing child elements.
     * @param element
     */
    public static ModelInfo createFromElements(ModelInfo defaultInfo, Element element) {
        String title = StringUtil.internString(UtilXml.childElementValue(element, "title"));
        if (title == null || title.isEmpty()) {
            title = defaultInfo.getTitle();
        }
        String description = StringUtil.internString(UtilXml.childElementValue(element, "description"));
        if (description == null || description.isEmpty()) {
            description = defaultInfo.getDescription();
        }
        String copyright = StringUtil.internString(UtilXml.childElementValue(element, "copyright"));
        if (copyright == null || copyright.isEmpty()) {
            copyright = defaultInfo.getCopyright();
        }
        String author = StringUtil.internString(UtilXml.childElementValue(element, "author"));
        if (author == null ||author.isEmpty()) {
            author = defaultInfo.getAuthor();
        }
        String version = StringUtil.internString(UtilXml.childElementValue(element, "version"));
        if (version == null || version.isEmpty()) {
            version = defaultInfo.getVersion();
        }
        String defaultResourceName = StringUtil.internString(UtilXml.childElementValue(element, "default-resource-name"));
        if (defaultResourceName == null || defaultResourceName.isEmpty()) {
            defaultResourceName = defaultInfo.getDefaultResourceName();
        }
        return new ModelInfo(title, description, copyright, author, version, defaultResourceName);
    }

    private static String getCopyrightString() {
        int year = UtilDateTime.getYear(UtilDateTime.nowTimestamp(), TimeZone.getDefault(), Locale.getDefault());
        return "Copyright 2001-" + year + " The Apache Software Foundation";
    }

    /*
     * Developers - this is an immutable class. Once constructed, the object should not change state.
     * Therefore, 'setter' methods are not allowed. If client code needs to modify the object's
     * state, then it can create a new copy with the changed values.
     */

    /** The title for documentation purposes */
    private final String title;

    /** The description for documentation purposes */
    private final String description;

    /** The copyright for documentation purposes */
    private final String copyright;

    /** The author for documentation purposes */
    private final String author;

    /** The version for documentation purposes */
    private final String version;

    /** The default-resource-name of the Entity, used with the getResource call to check for a value in a resource bundle */
    private final String defaultResourceName;

    ModelInfo(String title, String description, String copyright, String author, String version, String defaultResourceName) {
        this.title = title;
        this.description = description;
        this.copyright = copyright;
        this.author = author;
        this.version = version;
        this.defaultResourceName = defaultResourceName;
    }

    /** Returns the author. */
    public String getAuthor() {
        return this.author;
    }

    /** Returns the copyright. */
    public String getCopyright() {
        return this.copyright;
    }

    /** Returns the default resource name. */
    public String getDefaultResourceName() {
        return this.defaultResourceName;
    }

    /** Returns the description. */
    public String getDescription() {
        return this.description;
    }

    /** Returns the title. */
    public String getTitle() {
        return this.title;
    }

    /** Returns the version. */
    public String getVersion() {
        return this.version;
    }
}
