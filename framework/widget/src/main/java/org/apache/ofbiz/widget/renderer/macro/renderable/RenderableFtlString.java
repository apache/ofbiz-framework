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

import java.util.function.Consumer;

public final class RenderableFtlString implements RenderableFtl {
    private final String ftlString;

    public RenderableFtlString(final String ftlString) {
        this.ftlString = ftlString;
    }

    public String getFtlString() {
        return ftlString;
    }

    public static RenderableFtlStringBuilder builder() {
        return new RenderableFtlStringBuilder();
    }

    public static RenderableFtlString withStringBuilder(final Consumer<StringBuilder> callback) {
        final RenderableFtlStringBuilder builder = builder();
        callback.accept(builder.getStringBuilder());
        return builder.build();
    }

    @Override
    public String toString() {
        return "RenderableFtlString{"
                + "ftlString='" + ftlString + '\''
                + '}';
    }

    @Override
    public void accept(final RenderableFtlVisitor visitor) {
        visitor.visit(this);
    }

    public static final class RenderableFtlStringBuilder {
        private final StringBuilder stringBuilder = new StringBuilder();

        public StringBuilder getStringBuilder() {
            return stringBuilder;
        }

        public RenderableFtlString build() {
            return new RenderableFtlString(stringBuilder.toString());
        }
    }
}
