/*
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
public final class ModelGrid extends ModelForm {

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

    /** XML Constructor */
    public ModelGrid(Element formElement, String formLocation, ModelReader entityModelReader,
                     VisualTheme visualTheme, DispatchContext dispatchContext) {
        super(formElement, formLocation, entityModelReader, visualTheme, dispatchContext, "list",
                resolveParentModel(formElement, "grid", entityModelReader, visualTheme, dispatchContext));
    }

    @Override
    public void accept(ModelWidgetVisitor visitor) throws Exception {
        visitor.visit(this);
    }
}
