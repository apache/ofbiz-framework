/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License") throws Exception ; you may not use this file except in compliance
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

import org.ofbiz.widget.model.ModelFormField.CheckField;
import org.ofbiz.widget.model.ModelFormField.ContainerField;
import org.ofbiz.widget.model.ModelFormField.DateFindField;
import org.ofbiz.widget.model.ModelFormField.DateTimeField;
import org.ofbiz.widget.model.ModelFormField.DisplayEntityField;
import org.ofbiz.widget.model.ModelFormField.DisplayField;
import org.ofbiz.widget.model.ModelFormField.DropDownField;
import org.ofbiz.widget.model.ModelFormField.FileField;
import org.ofbiz.widget.model.ModelFormField.FormField;
import org.ofbiz.widget.model.ModelFormField.GridField;
import org.ofbiz.widget.model.ModelFormField.HiddenField;
import org.ofbiz.widget.model.ModelFormField.HyperlinkField;
import org.ofbiz.widget.model.ModelFormField.IgnoredField;
import org.ofbiz.widget.model.ModelFormField.ImageField;
import org.ofbiz.widget.model.ModelFormField.LookupField;
import org.ofbiz.widget.model.ModelFormField.MenuField;
import org.ofbiz.widget.model.ModelFormField.PasswordField;
import org.ofbiz.widget.model.ModelFormField.RadioField;
import org.ofbiz.widget.model.ModelFormField.RangeFindField;
import org.ofbiz.widget.model.ModelFormField.ResetField;
import org.ofbiz.widget.model.ModelFormField.ScreenField;
import org.ofbiz.widget.model.ModelFormField.SubmitField;
import org.ofbiz.widget.model.ModelFormField.TextField;
import org.ofbiz.widget.model.ModelFormField.TextFindField;
import org.ofbiz.widget.model.ModelFormField.TextareaField;

/**
 *  A <code>ModelFormField</code> visitor.
 */
public interface ModelFieldVisitor {

    void visit(CheckField checkField) throws Exception ;

    void visit(ContainerField containerField) throws Exception ;

    void visit(DateFindField dateFindField) throws Exception ;

    void visit(DateTimeField dateTimeField) throws Exception ;

    void visit(DisplayEntityField displayEntityField) throws Exception ;

    void visit(DisplayField displayField) throws Exception ;

    void visit(DropDownField dropDownField) throws Exception ;

    void visit(FileField fileField) throws Exception ;
    
    void visit(FormField formField) throws Exception ;

    void visit(GridField gridField) throws Exception ;

    void visit(HiddenField hiddenField) throws Exception ;

    void visit(HyperlinkField hyperlinkField) throws Exception ;

    void visit(IgnoredField ignoredField) throws Exception ;

    void visit(ImageField imageField) throws Exception ;

    void visit(LookupField lookupField) throws Exception ;

    void visit(MenuField menuField) throws Exception ;

    void visit(PasswordField passwordField) throws Exception ;

    void visit(RadioField radioField) throws Exception ;

    void visit(RangeFindField rangeFindField) throws Exception ;

    void visit(ResetField resetField) throws Exception ;

    void visit(ScreenField screenField) throws Exception ;

    void visit(SubmitField submitField) throws Exception ;

    void visit(TextareaField textareaField) throws Exception ;

    void visit(TextField textField) throws Exception ;

    void visit(TextFindField textFindField) throws Exception ;
}
