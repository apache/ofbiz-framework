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
package org.apache.ofbiz.widget.renderer.macro;

import org.apache.ofbiz.widget.renderer.macro.parameter.MacroCallParameterBooleanValue;
import org.apache.ofbiz.widget.renderer.macro.parameter.MacroCallParameterValue;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public final class MacroCallParameterBooleanValueMatcher extends TypeSafeMatcher<MacroCallParameterValue> {
    private final boolean value;

    public MacroCallParameterBooleanValueMatcher(final boolean value) {
        super(MacroCallParameterBooleanValue.class);
        this.value = value;
    }

    @Override
    protected boolean matchesSafely(final MacroCallParameterValue item) {
        final MacroCallParameterBooleanValue booleanValue = (MacroCallParameterBooleanValue) item;
        return value == booleanValue.isValue();
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText("with boolean value '" + value + "'");
    }

    @Override
    protected void describeMismatchSafely(final MacroCallParameterValue item, final Description mismatchDescription) {
        final MacroCallParameterBooleanValue booleanValue = (MacroCallParameterBooleanValue) item;
        mismatchDescription.appendText("with boolean value '" + booleanValue.isValue() + "'");
    }
}
