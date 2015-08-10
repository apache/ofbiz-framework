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
package org.ofbiz.widget.model;

import java.util.Collection;

import org.ofbiz.base.util.Assert;
import org.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.ofbiz.base.util.string.FlexibleStringExpander;
import org.ofbiz.widget.model.CommonWidgetModels.AutoEntityParameters;
import org.ofbiz.widget.model.CommonWidgetModels.AutoServiceParameters;
import org.ofbiz.widget.model.CommonWidgetModels.Image;
import org.ofbiz.widget.model.CommonWidgetModels.Link;
import org.ofbiz.widget.model.CommonWidgetModels.Parameter;

/**
 * Abstract XML widget visitor.
 *
 */
public abstract class XmlAbstractWidgetVisitor {

    protected final Appendable writer;

    public XmlAbstractWidgetVisitor(Appendable writer) {
        Assert.notNull("writer", writer);
        this.writer = writer;
    }

    protected void visitAttribute(String attributeName, Boolean attributeValue) throws Exception {
        if (attributeValue != null) {
            writer.append(" ").append(attributeName).append("=\"");
            writer.append(attributeValue.toString());
            writer.append("\"");
        }
    }

    protected void visitAttribute(String attributeName, FlexibleMapAccessor<?> attributeValue) throws Exception {
        if (attributeValue != null && !attributeValue.isEmpty()) {
            writer.append(" ").append(attributeName).append("=\"");
            writer.append(attributeValue.getOriginalName());
            writer.append("\"");
        }
    }

    protected void visitAttribute(String attributeName, FlexibleStringExpander attributeValue) throws Exception {
        if (attributeValue != null && !attributeValue.isEmpty()) {
            writer.append(" ").append(attributeName).append("=\"");
            writer.append(attributeValue.getOriginal());
            writer.append("\"");
        }
    }

    protected void visitAttribute(String attributeName, Integer attributeValue) throws Exception {
        if (attributeValue != null) {
            writer.append(" ").append(attributeName).append("=\"");
            writer.append(attributeValue.toString());
            writer.append("\"");
        }
    }

    protected void visitAttribute(String attributeName, String attributeValue) throws Exception {
        if (attributeValue != null && !attributeValue.isEmpty()) {
            writer.append(" ").append(attributeName).append("=\"");
            writer.append(attributeValue);
            writer.append("\"");
        }
    }

    protected void visitAutoEntityParameters(AutoEntityParameters autoEntityParameters) throws Exception {

    }

    protected void visitAutoServiceParameters(AutoServiceParameters autoServiceParameters) throws Exception {

    }

    protected void visitImage(Image image) throws Exception {
        if (image != null) {
            writer.append("<image");
            visitAttribute("name", image.getName());
            visitAttribute("alt", image.getAlt());
            visitAttribute("border", image.getBorderExdr());
            visitAttribute("height", image.getHeightExdr());
            visitAttribute("id", image.getIdExdr());
            visitAttribute("src", image.getSrcExdr());
            visitAttribute("style", image.getStyleExdr());
            visitAttribute("title", image.getTitleExdr());
            visitAttribute("url-mode", image.getUrlMode());
            visitAttribute("width", image.getWidthExdr());
            writer.append("/>");
        }
    }

    protected void visitLink(Link link) throws Exception {
        if (link != null) {
            writer.append("<link");
            visitLinkAttributes(link);
            if (link.getImage() != null || link.getAutoEntityParameters() != null || link.getAutoServiceParameters() != null) {
                writer.append(">");
                visitImage(link.getImage());
                visitAutoEntityParameters(link.getAutoEntityParameters());
                visitAutoServiceParameters(link.getAutoServiceParameters());
                writer.append("</link>");
            } else {
                writer.append("/>");
            }
        }
    }

    protected void visitLinkAttributes(Link link) throws Exception {
        if (link != null) {
            visitAttribute("name", link.getName());
            visitAttribute("encode", link.getEncode());
            visitAttribute("full-path", link.getFullPath());
            visitAttribute("id", link.getIdExdr());
            visitAttribute("height", link.getHeight());
            visitAttribute("link-type", link.getLinkType());
            visitAttribute("prefix", link.getPrefixExdr());
            visitAttribute("secure", link.getSecure());
            visitAttribute("style", link.getStyleExdr());
            visitAttribute("target", link.getTargetExdr());
            visitAttribute("target-window", link.getTargetWindowExdr());
            visitAttribute("text", link.getTextExdr());
            visitAttribute("size", link.getSize());
            visitAttribute("url-mode", link.getUrlMode());
            visitAttribute("width", link.getWidth());
        }
    }

    protected void visitModelWidget(ModelWidget widget) throws Exception {
        if (widget.getName() != null && !widget.getName().isEmpty()) {
            writer.append(" name=\"");
            writer.append(widget.getName());
            writer.append("\"");
        }
    }

    protected void visitParameters(Collection<Parameter> parameters) throws Exception {
        if (parameters != null) {
            for (Parameter parameter : parameters) {
                writer.append("<parameter");
                visitAttribute("param-name", parameter.getName());
                visitAttribute("from-field", parameter.getFromField());
                visitAttribute("value", parameter.getValue());
                writer.append("/>");
            }
        }
    }
}
