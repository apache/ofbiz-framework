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

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an FTL macro call.
 */
public final class RenderableFtlMacroCall implements RenderableFtl {
    private final String name;
    private final Map<String, Object> parameters;

    private RenderableFtlMacroCall(String name, Map<String, Object> parameters) {
        if (name == null) {
            throw new NullPointerException("RenderableFtlMacroCall name cannot be null");
        }
        this.name = name;
        this.parameters = parameters;
    }

    @Override
    public void accept(final RenderableFtlVisitor visitor) {
        visitor.visit(this);
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public static RenderableFtlMacroCallBuilder builder() {
        return new RenderableFtlMacroCallBuilder();
    }

    public static final class RenderableFtlMacroCallBuilder {
        private String name;
        private Map<String, Object> parameters = new HashMap<>();

        private RenderableFtlMacroCallBuilder() {
        }

        public RenderableFtlMacroCallBuilder name(final String name) {
            this.name = name;
            return this;
        }

        public RenderableFtlMacroCallBuilder stringParameter(final String parameterName, final String parameterValue) {
            return parameter(parameterName, parameterValue);
        }

        public RenderableFtlMacroCallBuilder booleanParameter(final String parameterName, final boolean parameterValue) {
            return parameter(parameterName, parameterValue);
        }

        public RenderableFtlMacroCallBuilder mapParameter(final String parameterName, final Map<String, String> parameterValue) {
            return parameter(parameterName, parameterValue);
        }

        public RenderableFtlMacroCall build() {
            return new RenderableFtlMacroCall(name, parameters);
        }

        private RenderableFtlMacroCallBuilder parameter(final String name, final Object value) {
            parameters.put(name, value);
            return this;
        }
    }
}
