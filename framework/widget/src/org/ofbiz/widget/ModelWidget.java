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
package org.ofbiz.widget;

import java.io.Serializable;
import java.util.Map;

import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilProperties;
import org.w3c.dom.Element;

/**
 * Widget Library - Widget model class. ModelWidget is a base class that is
 * extended by other widget model classes.
 */
@SuppressWarnings("serial")
public class ModelWidget implements Serializable {

    /**
     * The parameter name used to control widget boundary comments. Currently
     * set to "widgetVerbose". Set the parameter to "true" to enable widget
     * boundary comments.<br/><br/>
     * <code>WEB-INF/web.xml</code> example:<br/><br/>
     * <code>
     * &lt;context-param&gt;<br/>
     * &nbsp;&nbsp;&lt;param-name&gt;widgetVerbose&lt;/param-name&gt;<br/>
     * &nbsp;&nbsp;&lt;param-value&gt;true&lt;/param-value&gt;<br/>
     * &lt;/context-param&gt;
     * </code><br/><br/>
     * Screen widget example:<br/><br/>
     * <code>
     * &lt;actions&gt;<br/>
     * &nbsp;&nbsp;&lt;set field="parameters.widgetVerbose" value="true" global="true"/&gt;<br/>
     * &lt;/actions&gt;
     * </code>
     */
    public static final String enableBoundaryCommentsParam = "widgetVerbose";
    protected String name;
    protected boolean enableWidgetBoundaryComments = false;
    private String systemId;
    private int startColumn;
    private int startLine;

    protected ModelWidget() {}

    /**
     * Derived classes must call this constructor.
     * @param widgetElement The XML Element for the widget
     */
    public ModelWidget(Element widgetElement) {
        this.name = widgetElement.getAttribute("name");
        this.systemId = (String) widgetElement.getUserData("systemId");
        this.startColumn = ((Integer) widgetElement.getUserData("startColumn")).intValue();
        this.startLine = ((Integer) widgetElement.getUserData("startLine")).intValue();
    }

    /**
     * Returns the widget's name.
     * @return Widget's name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the url as a string, from where this widget was defined.
     * @return url
     */
    public String getSystemId() {
        return systemId;
    }

    /**
     * Returns the column where this widget was defined, in it's containing xml file.
     * @return start column
     */
    public int getStartColumn() {
        return startColumn;
    }

    /**
     * Returns the line where this widget was defined, in it's containing xml file.
     * @return start line
     */
    public int getStartLine() {
        return startLine;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getSystemId() + "#" + getName() + "@" + getStartColumn() + "," + getStartLine() + "]";
    }

    /**
     * Returns the widget's name to be used in boundary comments. The default action
     * is to return the widget's name. Derived classes can override this method to
     * return a customized name.
     * @return Name to be used in boundary comments
     */
    public String getBoundaryCommentName() {
        return name;
    }

    /**
     * Returns true if boundary comments are enabled for this widget, otherwise
     * returns false.
     * @return True if boundary comments are enabled for this widget
     * @deprecated Use the static <code>widgetBoundaryCommentsEnabled</code> method instead
     */
    @Deprecated
    public boolean boundaryCommentsEnabled() {
        return enableWidgetBoundaryComments;
    }

    /**
     * Enables/disables boundary comments for this widget.
     * @param context The screen rendering context
     * @deprecated Do not use this - it is not thread-safe
     */
    @Deprecated
    public void setWidgetBoundaryComments(Map<String, ? extends Object> context) {
        enableWidgetBoundaryComments = widgetBoundaryCommentsEnabled(context);
    }

    /**
     * Returns true if widget boundary comments are enabled. Widget boundary comments are
     * enabled by setting widgetVerbose true in the context Map, or by setting
     * widget.verbose=true in widget.properties.
     * @param context Optional context Map
     */
    public static boolean widgetBoundaryCommentsEnabled(Map<String, ? extends Object> context) {
        boolean result = "true".equals(UtilProperties.getPropertyValue("widget", "widget.verbose"));
        if (result == false && context != null) {
            String str = (String) context.get(enableBoundaryCommentsParam);
            if (str != null) {
                result = "true".equals(str);
            } else{
                Map<String, ? extends Object> parameters = UtilGenerics.checkMap(context.get("parameters"));
                if (parameters != null) {
                    str = (String) parameters.get(enableBoundaryCommentsParam);
                    if (str != null) {
                        result = "true".equals(str);
                    }
                }
            }
        }
        return result;
    }
}
