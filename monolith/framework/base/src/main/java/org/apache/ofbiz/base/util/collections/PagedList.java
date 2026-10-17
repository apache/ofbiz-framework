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
package org.apache.ofbiz.base.util.collections;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Stores the result of a subset of data items from a source (such
 * as EntityListIterator).
 */
public class PagedList<E> implements Iterable<E> {

    private int startIndex;
    private int endIndex;
    private int size;
    private int viewIndex;
    private int viewSize;
    private List<E> data;

    /**
     * Default constructor - populates all fields in this class
     * @param startIndex
     * @param endIndex
     * @param size
     * @param viewIndex
     * @param viewSize
     * @param data
     */
    public PagedList(int startIndex, int endIndex, int size, int viewIndex, int viewSize, List<E> data) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.size = size;
        this.viewIndex = viewIndex;
        this.viewSize = viewSize;
        this.data = data;
    }

    /**
     * @param viewIndex
     * @param viewSize
     * @return an empty PagedList object
     */
    public static <E> PagedList<E> empty(int viewIndex, int viewSize) {
        List<E> emptyList = Collections.emptyList();
        return new PagedList<>(0, 0, 0, viewIndex, viewSize, emptyList);
    }

    /**
     * @return the start index (for paginator) or known as low index
     */
    public int getStartIndex() {
        return startIndex;
    }

    /**
     * @return the end index (for paginator) or known as high index
     */
    public int getEndIndex() {
        return endIndex;
    }

    /**
     * @return the size of the full list, this can be the
     * result of <code>EntityListIterator.getResultsSizeAfterPartialList()</code>
     */
    public int getSize() {
        return size;
    }

    /**
     * @return the paged data. Eg - the result from <code>EntityListIterator.getPartialList()</code>
     */
    public List<E> getData() {
        return data;
    }

    /**
     * @return the view index supplied by client
     */
    public int getViewIndex() {
        return viewIndex;
    }

    /**
     * @return the view size supplied by client
     */
    public int getViewSize() {
        return viewSize;
    }

    /**
     * @return an interator object over the data returned in getData() method
     *         of this class
     */
    @Override
    public Iterator<E> iterator() {
        return this.data.iterator();
    }

}
