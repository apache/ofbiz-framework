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
package org.apache.ofbiz.widget.renderer.macro.renderable;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import org.apache.ofbiz.widget.renderer.macro.parameter.MacroCallParameterBooleanValue;
import org.apache.ofbiz.widget.renderer.macro.parameter.MacroCallParameterMapValue;
import org.apache.ofbiz.widget.renderer.macro.parameter.MacroCallParameterStringValue;
import org.apache.ofbiz.widget.renderer.macro.parameter.MacroCallParameterValue;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents an FTL macro call.
 */
@Builder
public final class RenderableFtlMacroCall implements RenderableFtl {
    @NonNull
    @Getter
    private final String name;
    @Singular
    @Getter
    private final Map<String, MacroCallParameterValue> parameters;

    @Override
    public String toFtlString() {
        return parameters.entrySet()
                .stream()
                .map((entry) -> createFtlMacroParameter(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(" ", "<@" + name + " ", " />"));
    }

    private String createFtlMacroParameter(final String parameterName, final MacroCallParameterValue parameterValue) {
        return parameterName + "=" + parameterValue.toFtlString();
    }

    public static final class RenderableFtlMacroCallBuilder {
        public RenderableFtlMacroCallBuilder stringParameter(final String parameterName, final String parameterValue) {
            return parameter(parameterName, new MacroCallParameterStringValue(parameterValue));
        }

        public RenderableFtlMacroCallBuilder booleanParameter(final String parameterName, final boolean parameterValue) {
            return parameter(parameterName, new MacroCallParameterBooleanValue(parameterValue));
        }

        public RenderableFtlMacroCallBuilder mapParameter(final String parameterName, final Map<String, String> parameterValue) {
            return parameter(parameterName, new MacroCallParameterMapValue(parameterValue));
        }
    }
}
