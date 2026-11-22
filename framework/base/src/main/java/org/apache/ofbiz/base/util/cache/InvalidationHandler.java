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

/**
 * Applies the received invalidations another layer owns, such as the entity engine's. The cache messages themselves
 * are applied by {@link UtilCache}. Handlers run on the bus's delivery thread and must be quick.
 */
public interface InvalidationHandler {

    /** Invalidations received from another server, in the order they were published. */
    void onMessages(List<InvalidationMessage> messages);

    /** Messages may have been missed; clear whatever cannot be trusted any more. */
    void onGap();
}
