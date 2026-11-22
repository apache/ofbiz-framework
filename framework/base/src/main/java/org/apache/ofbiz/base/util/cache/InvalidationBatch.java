/*
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

package org.apache.ofbiz.base.util.cache;

import java.util.List;
import java.util.Objects;

/**
 * The messages of one publish, with the id of the server that sent them so it can ignore its own.
 * @param nodeId the sending server's id
 * @param messages the invalidations, in order
 */
public record InvalidationBatch(String nodeId, List<InvalidationMessage> messages) {
    public InvalidationBatch {
        Objects.requireNonNull(nodeId, "nodeId");
        messages = List.copyOf(messages);
    }
}
