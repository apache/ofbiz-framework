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

import java.util.ArrayList;
import java.util.List;

/**
 * Class for composing multiple RenderableFtl objects in a sequence.
 */
public final class RenderableFtlSequence implements RenderableFtl {
    private final List<RenderableFtl> renderableFtls;

    private RenderableFtlSequence(List<RenderableFtl> renderableFtls) {
        this.renderableFtls = renderableFtls;
    }

    @Override
    public void accept(final RenderableFtlVisitor visitor) {
        renderableFtls.forEach(renderableFtl -> renderableFtl.accept(visitor));
    }

    public static RenderableFtlSequenceBuilder builder() {
        return new RenderableFtlSequenceBuilder();
    }

    public static final class RenderableFtlSequenceBuilder {
        private List<RenderableFtl> renderableFtls = new ArrayList<>();

        public RenderableFtlSequenceBuilder renderableFtl(RenderableFtl renderableFtl) {
            renderableFtls.add(renderableFtl);
            return this;
        }

        public RenderableFtlSequence build() {
            return new RenderableFtlSequence(renderableFtls);
        }
    }
}
