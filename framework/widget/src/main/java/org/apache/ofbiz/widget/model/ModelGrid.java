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
package org.apache.ofbiz.widget.model;

import java.util.List;

import java.util.Map;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.entity.model.ModelReader;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.widget.renderer.VisualTheme;
import org.w3c.dom.Element;

/**
 * Models the &lt;grid&gt; element.
 *
 * @see <code>widget-form.xsd</code>
 */
@SuppressWarnings("serial")
public class ModelGrid extends ModelForm {

    /*
     * ----------------------------------------------------------------------- *
     *                     DEVELOPERS PLEASE READ
     * ----------------------------------------------------------------------- *
     * This model is intended to be a read-only data structure that represents
     * an XML element. Outside of object construction, the class should not
     * have any behaviors. All behavior should be contained in model visitors.
     * Instances of this class will be shared by multiple threads - therefore
     * it is immutable. DO NOT CHANGE THE OBJECT'S STATE AT RUN TIME!
     */

    private static final String MODULE = ModelGrid.class.getName();

    /** XML Constructor */
    public ModelGrid(Element formElement, String formLocation, ModelReader entityModelReader,
                     VisualTheme visualTheme, DispatchContext dispatchContext) {
        super(formElement, formLocation, entityModelReader, visualTheme, dispatchContext, "list");
    }

    @Override
    public void accept(ModelWidgetVisitor visitor) throws Exception {
        visitor.visit(this);
    }

    @Override
    protected ModelForm getParentModel(Element gridElement, ModelReader entityModelReader,
                                       VisualTheme visualTheme, DispatchContext dispatchContext) {
        ModelForm parentModel = null;
        String parentResource = gridElement.getAttribute("extends-resource");
        String parentGrid = gridElement.getAttribute("extends");
        if (!parentGrid.isEmpty()) {
            // check if we have a resource name
            if (!parentResource.isEmpty()) {
                try {
                    FlexibleStringExpander parentResourceExp = FlexibleStringExpander.getInstance(parentResource);
                    Map<String, String> visualRessources = UtilMisc.toMap(
                            "commonFormLocations", visualTheme.getModelTheme().getModelCommonForms());
                    parentResource = parentResourceExp.expandString(visualRessources);
                    parentModel = GridFactory.getGridFromLocation(parentResource, parentGrid, entityModelReader,
                            visualTheme, dispatchContext);
                } catch (Exception e) {
                    Debug.logError(e, "Failed to load parent grid definition '" + parentGrid
                            + "' at resource '" + parentResource + "'", MODULE);
                }
            } else if (!parentGrid.equals(gridElement.getAttribute("name"))) {
                // try to find a grid definition in the same file
                Element rootElement = gridElement.getOwnerDocument().getDocumentElement();
                List<? extends Element> gridElements = UtilXml.childElementList(rootElement, "grid");
                if (gridElements.isEmpty()) {
                    // Backwards compatibility - look for form definitions
                    gridElements = UtilXml.childElementList(rootElement, "form");
                }
                for (Element parentElement : gridElements) {
                    if (parentElement.getAttribute("name").equals(parentGrid)) {
                        parentModel = GridFactory.createModelGrid(parentElement, entityModelReader, visualTheme,
                                dispatchContext, parentResource, parentGrid);
                        break;
                    }
                }
                if (parentModel == null) {
                    Debug.logError("Failed to find parent grid definition '" + parentGrid + "' in same document.", MODULE);
                }
            } else {
                Debug.logError("Recursive grid definition found for '" + gridElement.getAttribute("name") + ".'", MODULE);
            }
        }
        return parentModel;
    }
}
