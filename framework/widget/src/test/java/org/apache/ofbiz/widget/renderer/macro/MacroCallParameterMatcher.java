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

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.Map;

public final class MacroCallParameterMatcher extends TypeSafeMatcher<Map.Entry<String, Object>> {
    private final String name;
    private final Matcher<Object> valueMatcher;

    private boolean nameMatches = true;
    private boolean valueMatches = true;

    public MacroCallParameterMatcher(final String name, final Matcher<Object> valueMatcher) {
        this.name = name;
        this.valueMatcher = valueMatcher;
    }

    @Override
    protected boolean matchesSafely(final Map.Entry<String, Object> item) {
        if (name != null) {
            nameMatches = name.equals(item.getKey());
        }

        if (valueMatcher != null) {
            valueMatches = valueMatcher.matches(item.getValue());
        }

        return nameMatches && valueMatches;
    }

    @Override
    public void describeTo(final Description description) {
        if (name != null) {
            description.appendText("has name '" + name + "' ");
        }

        if (valueMatcher != null) {
            valueMatcher.describeTo(description);
        }
    }

    @Override
    protected void describeMismatchSafely(final Map.Entry<String, Object> item,
                                          final Description mismatchDescription) {
        if (name != null) {
            mismatchDescription.appendText("has name '" + item.getKey() + "' ");
        }

        if (valueMatcher != null) {
            valueMatcher.describeMismatch(item.getValue(), mismatchDescription);
        }

        mismatchDescription.appendText(", ");
    }

    public static MacroCallParameterMatcher hasName(final String name) {
        return new MacroCallParameterMatcher(name, null);
    }

    public static MacroCallParameterMatcher hasNameAndStringValue(final String name, final String value) {
        return new MacroCallParameterMatcher(name, new MacroCallParameterStringValueMatcher(value));
    }

    public static MacroCallParameterMatcher hasNameAndBooleanValue(final String name, final boolean value) {
        return new MacroCallParameterMatcher(name, new MacroCallParameterBooleanValueMatcher(value));
    }

    public static MacroCallParameterMatcher hasNameAndMapValue(final String name,
                                                               final Matcher<Map<String, String>> matcher) {
        return new MacroCallParameterMatcher(name, new MacroCallParameterMapValueMatcher(matcher));
    }
}
