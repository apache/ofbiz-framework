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
package org.ofbiz.widget.xml;

import java.util.Collection;

import org.ofbiz.widget.AbstractModelCondition.And;
import org.ofbiz.widget.AbstractModelCondition.IfCompare;
import org.ofbiz.widget.AbstractModelCondition.IfCompareField;
import org.ofbiz.widget.AbstractModelCondition.IfEmpty;
import org.ofbiz.widget.AbstractModelCondition.IfEntityPermission;
import org.ofbiz.widget.AbstractModelCondition.IfHasPermission;
import org.ofbiz.widget.AbstractModelCondition.IfRegexp;
import org.ofbiz.widget.AbstractModelCondition.IfServicePermission;
import org.ofbiz.widget.AbstractModelCondition.IfValidateMethod;
import org.ofbiz.widget.AbstractModelCondition.Not;
import org.ofbiz.widget.AbstractModelCondition.Or;
import org.ofbiz.widget.AbstractModelCondition.Xor;
import org.ofbiz.widget.ModelCondition;
import org.ofbiz.widget.ModelConditionVisitor;
import org.ofbiz.widget.menu.ModelMenuCondition;
import org.ofbiz.widget.screen.ModelScreenCondition.IfEmptySection;
import org.ofbiz.widget.tree.ModelTreeCondition;

/**
 * An object that generates XML from widget models.
 * The generated XML is unformatted - if you want to
 * "pretty print" the XML, then use a transformer.
 *
 */
public class XmlWidgetConditionVisitor extends AbstractWidgetVisitor implements ModelConditionVisitor {

    public XmlWidgetConditionVisitor(Appendable writer) {
        super(writer);
    }

    @Override
    public void visit(And and) throws Exception {
        writer.append("<and>");
        visitSubConditions(and.getSubConditions());
        writer.append("</and>");
    }

    @Override
    public void visit(IfCompare ifCompare) throws Exception {
        writer.append("<if-compare");
        visitAttribute("field", ifCompare.getFieldAcsr());
        visitAttribute("operator", ifCompare.getOperator());
        visitAttribute("value", ifCompare.getValueExdr());
        visitAttribute("type", ifCompare.getType());
        visitAttribute("format", ifCompare.getFormatExdr());
        writer.append("/>");
    }

    @Override
    public void visit(IfCompareField ifCompareField) throws Exception {
        writer.append("<if-compare-field");
        visitAttribute("field", ifCompareField.getFieldAcsr());
        visitAttribute("operator", ifCompareField.getOperator());
        visitAttribute("to-field", ifCompareField.getToFieldAcsr());
        visitAttribute("type", ifCompareField.getType());
        visitAttribute("format", ifCompareField.getFormatExdr());
        writer.append("/>");
    }

    @Override
    public void visit(IfEmpty ifEmpty) throws Exception {
        writer.append("<if-empty");
        visitAttribute("field", ifEmpty.getFieldAcsr());
        writer.append("/>");
    }

    @Override
    public void visit(IfEmptySection ifEmptySection) throws Exception {
        writer.append("<if-empty-section");
        visitAttribute("section-name", ifEmptySection.getSectionExdr());
        writer.append("/>");
    }

    @Override
    public void visit(IfEntityPermission ifEntityPermission) throws Exception {
        writer.append("<if-entity-permission");
        // TODO: Create EntityPermissionChecker visitor
        writer.append("/>");
    }

    @Override
    public void visit(IfHasPermission ifHasPermission) throws Exception {
        writer.append("<if-has-permission");
        visitAttribute("permission", ifHasPermission.getPermissionExdr());
        visitAttribute("action", ifHasPermission.getActionExdr());
        writer.append("/>");
    }

    @Override
    public void visit(IfRegexp ifRegexp) throws Exception {
        writer.append("<if-regexp");
        visitAttribute("field", ifRegexp.getFieldAcsr());
        visitAttribute("expr", ifRegexp.getExprExdr());
        writer.append("/>");
    }

    @Override
    public void visit(IfServicePermission ifServicePermission) throws Exception {
        writer.append("<if-service-permission");
        visitAttribute("service-name", ifServicePermission.getServiceExdr());
        visitAttribute("main-action", ifServicePermission.getActionExdr());
        visitAttribute("context-map", ifServicePermission.getCtxMapExdr());
        visitAttribute("resource-description", ifServicePermission.getResExdr());
        writer.append("/>");
    }

    @Override
    public void visit(IfValidateMethod ifValidateMethod) throws Exception {
        writer.append("<if-validate-method");
        visitAttribute("field", ifValidateMethod.getFieldAcsr());
        visitAttribute("method", ifValidateMethod.getMethodExdr());
        visitAttribute("class", ifValidateMethod.getClassExdr());
        writer.append("/>");
    }

    @Override
    public void visit(ModelMenuCondition modelMenuCondition) throws Exception {
        writer.append("<condition");
        visitAttribute("pass-style", modelMenuCondition.getPassStyleExdr());
        visitAttribute("disabled-style", modelMenuCondition.getFailStyleExdr());
        writer.append("/>");
        modelMenuCondition.getCondition().accept(this);
        ;
        writer.append("</condition>");
    }

    @Override
    public void visit(ModelTreeCondition modelTreeCondition) throws Exception {
        writer.append("<condition>");
        modelTreeCondition.getCondition().accept(this);
        writer.append("</condition>");
    }

    @Override
    public void visit(Not not) throws Exception {
        writer.append("<not>");
        not.getSubCondition().accept(this);
        writer.append("</not>");
    }

    @Override
    public void visit(Or or) throws Exception {
        writer.append("<or>");
        visitSubConditions(or.getSubConditions());
        writer.append("</or>");
    }

    @Override
    public void visit(Xor xor) throws Exception {
        writer.append("<xor>");
        visitSubConditions(xor.getSubConditions());
        writer.append("</xor>");
    }

    private void visitSubConditions(Collection<ModelCondition> subConditions) throws Exception {
        for (ModelCondition subCondition : subConditions) {
            subCondition.accept(this);
        }
    }
}
