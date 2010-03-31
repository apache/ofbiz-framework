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
package org.ofbiz.base.concurrent;

import org.ofbiz.base.lang.SourceMonitored;

@SourceMonitored
public abstract class TTLCachedObject<T> extends TTLObject<T> {
    public static final long NOT_EXISTANT_TIMESTAMP = Long.MIN_VALUE;
    public static final long FORCE_REGEN = Long.MIN_VALUE + 1;

    protected long lastModifiedTime = NOT_EXISTANT_TIMESTAMP;

    public long getTimestamp() throws ObjectException {
        getObject();
        return lastModifiedTime;
    }

    protected T load(T old, int serial) throws Exception {
        long timestamp = getTimestamp(old);
        if (lastModifiedTime != timestamp) {
            if (timestamp != NOT_EXISTANT_TIMESTAMP) {
                GeneratedResult<T> result = generate(old, serial);
                lastModifiedTime = result.lastModifiedTime;
                return result.object;
            } else {
                lastModifiedTime = NOT_EXISTANT_TIMESTAMP;
                return getInitial();
            }
        }
        return old;
    }

    protected abstract GeneratedResult<T> generate(T old, int serial) throws Exception;
    protected abstract long getTimestamp(T old) throws Exception;
}
