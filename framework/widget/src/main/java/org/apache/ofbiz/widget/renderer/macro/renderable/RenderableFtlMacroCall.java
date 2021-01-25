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

import org.apache.ofbiz.widget.renderer.macro.parameter.MacroCallParameterBooleanValue;
import org.apache.ofbiz.widget.renderer.macro.parameter.MacroCallParameterMapValue;
import org.apache.ofbiz.widget.renderer.macro.parameter.MacroCallParameterStringValue;
import org.apache.ofbiz.widget.renderer.macro.parameter.MacroCallParameterValue;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Represents an FTL macro call.
 */
public final class RenderableFtlMacroCall implements RenderableFtl {
    private final String name;
    private final Map<String, MacroCallParameterValue> parameters;

    private RenderableFtlMacroCall(String name, Map<String, MacroCallParameterValue> parameters) {
        if (name == null) {
            throw new NullPointerException("RenderableFtlMacroCall name cannot be null");
        }
        this.name = name;
        this.parameters = parameters;
    }

    @Override
    public String toFtlString() {
        return parameters.entrySet()
                .stream()
                .map((entry) -> createFtlMacroParameter(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(" ", "<@" + name + " ", " />"));
    }

    public String getName() {
        return name;
    }

    public Map<String, MacroCallParameterValue> getParameters() {
        return parameters;
    }

    private String createFtlMacroParameter(final String parameterName, final MacroCallParameterValue parameterValue) {
        return parameterName + "=" + parameterValue.toFtlString();
    }

    public static RenderableFtlMacroCallBuilder builder() {
        return new RenderableFtlMacroCallBuilder();
    }

    public static final class RenderableFtlMacroCallBuilder {
        private String name;
        private Map<String, MacroCallParameterValue> parameters = new HashMap<>();

        private RenderableFtlMacroCallBuilder() {
        }

        public RenderableFtlMacroCallBuilder name(final String name) {
            this.name = name;
            return this;
        }

        public RenderableFtlMacroCallBuilder stringParameter(final String parameterName, final String parameterValue) {
            return parameter(parameterName, new MacroCallParameterStringValue(parameterValue));
        }

        public RenderableFtlMacroCallBuilder booleanParameter(final String parameterName, final boolean parameterValue) {
            return parameter(parameterName, new MacroCallParameterBooleanValue(parameterValue));
        }

        public RenderableFtlMacroCallBuilder mapParameter(final String parameterName, final Map<String, String> parameterValue) {
            return parameter(parameterName, new MacroCallParameterMapValue(parameterValue));
        }

        public RenderableFtlMacroCall build() {
            return new RenderableFtlMacroCall(name, parameters);
        }

        private RenderableFtlMacroCallBuilder parameter(final String name, final MacroCallParameterValue value) {
            parameters.put(name, value);
            return this;
        }
    }
}
