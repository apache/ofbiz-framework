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

import org.ofbiz.widget.form.ModelFormField.CheckField;
import org.ofbiz.widget.form.ModelFormField.ContainerField;
import org.ofbiz.widget.form.ModelFormField.DateFindField;
import org.ofbiz.widget.form.ModelFormField.DateTimeField;
import org.ofbiz.widget.form.ModelFormField.DisplayEntityField;
import org.ofbiz.widget.form.ModelFormField.DisplayField;
import org.ofbiz.widget.form.ModelFormField.DropDownField;
import org.ofbiz.widget.form.ModelFormField.FileField;
import org.ofbiz.widget.form.ModelFormField.HiddenField;
import org.ofbiz.widget.form.ModelFormField.HyperlinkField;
import org.ofbiz.widget.form.ModelFormField.IgnoredField;
import org.ofbiz.widget.form.ModelFormField.ImageField;
import org.ofbiz.widget.form.ModelFormField.LookupField;
import org.ofbiz.widget.form.ModelFormField.PasswordField;
import org.ofbiz.widget.form.ModelFormField.RadioField;
import org.ofbiz.widget.form.ModelFormField.RangeFindField;
import org.ofbiz.widget.form.ModelFormField.ResetField;
import org.ofbiz.widget.form.ModelFormField.SubmitField;
import org.ofbiz.widget.form.ModelFormField.TextField;
import org.ofbiz.widget.form.ModelFormField.TextFindField;
import org.ofbiz.widget.form.ModelFormField.TextareaField;

/**
 *  A <code>ModelFormField</code> visitor.
 */
public interface ModelFieldVisitor {

    void visit(CheckField checkField);

    void visit(ContainerField containerField);

    void visit(DateFindField dateTimeField);

    void visit(DateTimeField dateTimeField);

    void visit(DisplayEntityField displayField);

    void visit(DisplayField displayField);

    void visit(DropDownField dropDownField);

    void visit(FileField textField);

    void visit(HiddenField hiddenField);

    void visit(HyperlinkField hyperlinkField);

    void visit(IgnoredField ignoredField);

    void visit(ImageField imageField);

    void visit(LookupField textField);

    void visit(PasswordField textField);

    void visit(RadioField radioField);

    void visit(RangeFindField textField);

    void visit(ResetField resetField);

    void visit(SubmitField submitField);

    void visit(TextareaField textareaField);

    void visit(TextField textField);

    void visit(TextFindField textField);
}
