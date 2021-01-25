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

import org.apache.ofbiz.widget.renderer.macro.parameter.MacroCallParameterMapValue;
import org.apache.ofbiz.widget.renderer.macro.parameter.MacroCallParameterValue;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.Map;

public final class MacroCallParameterMapValueMatcher extends TypeSafeMatcher<MacroCallParameterValue> {
    private final Matcher<Map<String, String>> matcher;

    public MacroCallParameterMapValueMatcher(final Matcher<Map<String, String>> matcher) {
        super(MacroCallParameterMapValue.class);
        this.matcher = matcher;
    }

    @Override
    protected boolean matchesSafely(final MacroCallParameterValue item) {
        final MacroCallParameterMapValue mapValue = (MacroCallParameterMapValue) item;
        return matcher.matches(mapValue.getValue());
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText("with map value '");
        matcher.describeTo(description);
        description.appendText("' ");
    }

    @Override
    protected void describeMismatchSafely(final MacroCallParameterValue item, final Description mismatchDescription) {
        final MacroCallParameterMapValue mapValue = (MacroCallParameterMapValue) item;
        mismatchDescription.appendText("with map value '");
        matcher.describeMismatch(mapValue, mismatchDescription);
        mismatchDescription.appendText("' ");
    }
}
