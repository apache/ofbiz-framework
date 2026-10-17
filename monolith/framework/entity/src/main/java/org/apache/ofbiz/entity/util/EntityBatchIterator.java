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
package org.apache.ofbiz.entity.util;

import java.util.Iterator;
import java.util.List;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;

public class EntityBatchIterator implements Iterator<GenericValue> {
    private static final String MODULE = EntityBatchIterator.class.getName();

    private EntityQuery query;
    private List<GenericValue> currentResultSet;
    private int currentIndex = 0;
    private int currentOffset = 0;

    public EntityBatchIterator(EntityQuery query) {
        this.query = query;
        if (query.getLimit() == null) {
            query.limit(500);
        }
        if (query.getOffset() == null) {
            query.offset(0);
        }
        // Just in case the query already has a non-zero offset, we need to continue from that point
        currentOffset = query.getOffset();
    }

    @Override
    public boolean hasNext() {
        try {
            return hasNextWithException();
        } catch (GenericEntityException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Has next with exception boolean.
     * @return the boolean
     * @throws GenericEntityException the generic entity exception
     */
    public boolean hasNextWithException() throws GenericEntityException {
        if (needNextBatch()) {
            getNextBatch();
        }
        return !currentResultSet.isEmpty() && currentResultSet.size() > currentIndex;
    }

    @Override
    public GenericValue next() {
        try {
            return nextWithException();
        } catch (GenericEntityException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Next with exception generic value.
     * @return the generic value
     * @throws GenericEntityException the generic entity exception
     */
    public GenericValue nextWithException() throws GenericEntityException {
        return hasNextWithException() ? currentResultSet.get(currentIndex++) : null;
    }

    private void getNextBatch() throws GenericEntityException {
        Debug.logInfo("Getting next batch with offset: " + currentOffset, MODULE);
        currentResultSet = this.query.offset(currentOffset).queryList();
        Debug.logInfo("Retreived row count: " + currentResultSet.size(), MODULE);
        currentOffset += query.getLimit();
        currentIndex = 0;
    }

    private boolean needNextBatch() {
        // Return true if we haven't fetched anything yet, or
        // if we're about to go out of bounds on the current batch
        return currentResultSet == null || currentIndex >= currentResultSet.size();
    }

}
