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
import org.w3c.dom.Element;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;

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

    protected ModelWidget() {}

    /**
     * Derived classes must call this constructor.
     * @param widgetElement The XML Element for the widget
     */
    public ModelWidget(Element widgetElement) {
        this.name = widgetElement.getAttribute("name");
    }

    /**
     * Returns the widget's name.
     * @return Widget's name
     */
    public String getName() {
        return name;
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
     */
    public boolean boundaryCommentsEnabled() {
        return enableWidgetBoundaryComments;
    }

    /**
     * Enables/disables boundary comments for this widget.
     * @param context The screen rendering context
     */
    public void setWidgetBoundaryComments(Map<String, ? extends Object> context) {
        Map<String, ? extends Object> parameters = UtilGenerics.checkMap(context.get("parameters"));
        enableWidgetBoundaryComments = widgetBoundaryCommentsEnabled(parameters);
    }

    /**
     * Returns true if widget boundary comments are enabled. Widget boundary comments are
     * enabled by setting widgetVerbose true in the parameters Map, or by setting
     * widget.verbose=true in widget.properties.
     * @param parameters Optional parameters Map
     */
    public static boolean widgetBoundaryCommentsEnabled(Map<String, ? extends Object> parameters) {
        boolean result = "true".equals(UtilProperties.getPropertyValue("widget", "widget.verbose"));
        if (!result && parameters != null) {
            result = "true".equals(parameters.get(enableBoundaryCommentsParam));
        }
        return result;
    }

    public int getPaginatorNumber(Map<String, Object> context) {
        int paginator_number = 0;
        Map<String, Object> globalCtx = UtilGenerics.checkMap(context.get("globalContext"));
        if (globalCtx != null) {
            Integer paginateNumberInt= (Integer)globalCtx.get("PAGINATOR_NUMBER");
            if (paginateNumberInt == null) {
                paginateNumberInt = Integer.valueOf(0);
                globalCtx.put("PAGINATOR_NUMBER", paginateNumberInt);
            }
            paginator_number = paginateNumberInt.intValue();
        }
        return paginator_number;
    }

    public void incrementPaginatorNumber(Map<String, Object> context) {
        Map<String, Object> globalCtx = UtilGenerics.checkMap(context.get("globalContext"));
        if (globalCtx != null) {
            Boolean NO_PAGINATOR = (Boolean) globalCtx.get("NO_PAGINATOR");
            if (UtilValidate.isNotEmpty(NO_PAGINATOR)) {
                globalCtx.remove("NO_PAGINATOR");
            } else {
                Integer paginateNumberInt = Integer.valueOf(getPaginatorNumber(context) + 1);
                globalCtx.put("PAGINATOR_NUMBER", paginateNumberInt);
            }
        }
    }

}
