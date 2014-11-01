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
package org.ofbiz.widget.form;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.widget.ModelActionVisitor;
import org.ofbiz.widget.ModelWidgetAction;
import org.w3c.dom.Element;

/**
 * Widget Library - Screen model class
 */
public class ModelFormAction {

    public static final String module = ModelFormAction.class.getName();

    public static List<ModelWidgetAction> readSubActions(ModelForm modelForm, Element parentElement) {
        List<? extends Element> elementList = UtilXml.childElementList(parentElement);
        if (elementList.isEmpty()) {
            return Collections.emptyList();
        }
        List<ModelWidgetAction> actions = new ArrayList<ModelWidgetAction>(elementList.size());
        for (Element actionElement: UtilXml.childElementList(parentElement)) {
            if ("call-parent-actions".equals(actionElement.getNodeName())) {
                actions.add(new CallParentActions(modelForm, actionElement));
            } else {
                actions.add(ModelWidgetAction.toModelWidgetAction(modelForm, actionElement));
            }
        }
        return Collections.unmodifiableList(actions);
    }

    public static void runSubActions(List<ModelWidgetAction> actions, Map<String, Object> context) {
        if (actions == null) return;
        for (ModelWidgetAction action: actions) {
            if (Debug.verboseOn()) Debug.logVerbose("Running form action " + action.getClass().getName(), module);
            try {
                action.runAction(context);
            } catch (GeneralException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @SuppressWarnings("serial")
    public static class CallParentActions extends ModelWidgetAction {
        protected static enum ActionsKind {
            ACTIONS,
            ROW_ACTIONS
        };

        protected ActionsKind kind;
        private final ModelForm modelForm;

        public CallParentActions(ModelForm modelForm, Element callParentActionsElement) {
            super(modelForm, callParentActionsElement);
            String parentName = callParentActionsElement.getParentNode().getNodeName();
            if ("actions".equals(parentName)) {
                kind = ActionsKind.ACTIONS;
            } else if ("row-actions".equals(parentName)) {
                kind = ActionsKind.ROW_ACTIONS;
            } else {
                throw new IllegalArgumentException("Action element not supported for call-parent-actions : " + parentName);
            }
            ModelForm parentModel = modelForm.getParentModelForm();
            if (parentModel == null) {
                throw new IllegalArgumentException("call-parent-actions can only be used with form extending another form");
            }
            this.modelForm = modelForm;
        }

        @Override
        public void runAction(Map<String, Object> context) {
            ModelForm parentModel = modelForm.getParentModelForm();
            switch (kind) {
                case ACTIONS:
                    parentModel.runFormActions(context);
                    break;
                case ROW_ACTIONS:
                    ModelFormAction.runSubActions(parentModel.rowActions, context);
                    break;
            }
        }

        @Override
        public void accept(ModelActionVisitor visitor) {
            visitor.visit(this);
        }
    }
}
