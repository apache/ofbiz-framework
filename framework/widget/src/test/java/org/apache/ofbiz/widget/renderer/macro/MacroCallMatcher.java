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

import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtl;
import org.apache.ofbiz.widget.renderer.macro.renderable.RenderableFtlMacroCall;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import java.util.ArrayList;
import java.util.List;

public final class MacroCallMatcher extends TypeSafeMatcher<RenderableFtl> {
    private final String macroName;
    private final MacroCallParameterMatcher[] parameterMatchers;

    private final List<MacroCallParameterMatcher> failedParameterMatchers = new ArrayList<>();

    public MacroCallMatcher(final String macroName, final MacroCallParameterMatcher... parameterMatchers) {
        super(RenderableFtlMacroCall.class);

        this.macroName = macroName;
        this.parameterMatchers = parameterMatchers;
    }

    @Override
    protected boolean matchesSafely(final RenderableFtl item) {
        final RenderableFtlMacroCall macroCall = (RenderableFtlMacroCall) item;
        boolean nameMatched = (macroName == null) || macroName.equals(macroCall.getName());

        for (final MacroCallParameterMatcher parameterMatcher : parameterMatchers) {
            boolean matchForParameterMatcher = macroCall.getParameters()
                    .entrySet()
                    .stream()
                    .anyMatch(parameterMatcher::matches);
            if (!matchForParameterMatcher) {
                failedParameterMatchers.add(parameterMatcher);
            }
        }

        return nameMatched && failedParameterMatchers.isEmpty();
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText("MacroCall has name '" + macroName + "' ");
        description.appendText("with Parameters[");
        for (final MacroCallParameterMatcher parameterMatcher : parameterMatchers) {
            parameterMatcher.describeTo(description);
        }
        description.appendText("]");
    }

    @Override
    protected void describeMismatchSafely(final RenderableFtl item, final Description mismatchDescription) {
        final RenderableFtlMacroCall macroCall = (RenderableFtlMacroCall) item;

        mismatchDescription.appendText("MacroCall has name '" + macroCall.getName() + "' ");

        if (!failedParameterMatchers.isEmpty()) {
            mismatchDescription.appendText("with Parameters[");
            for (final MacroCallParameterMatcher failedParameterMatcher : failedParameterMatchers) {
                macroCall.getParameters()
                        .entrySet()
                        .forEach(entry ->
                                failedParameterMatcher.describeMismatch(entry, mismatchDescription));
            }
            mismatchDescription.appendText("]");
        }
    }

    public static MacroCallMatcher hasName(final String macroName) {
        return new MacroCallMatcher(macroName);
    }

    public static MacroCallMatcher hasParameters(final MacroCallParameterMatcher... parameterMatchers) {
        return new MacroCallMatcher(null, parameterMatchers);
    }

    public static MacroCallMatcher hasNameAndParameters(final String macroName,
                                                        final MacroCallParameterMatcher... parameterMatchers) {
        return new MacroCallMatcher(macroName, parameterMatchers);
    }
}
