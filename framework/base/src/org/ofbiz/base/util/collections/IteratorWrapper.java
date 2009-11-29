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
package org.ofbiz.base.util.collections;

import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class IteratorWrapper<DEST, SRC> implements Iterator<DEST> {
    private final Iterator<? extends SRC> it;
    private DEST lastDest;
    private SRC lastSrc;

    protected IteratorWrapper(Iterator<? extends SRC> it) {
        this.it = it;
    }

    public boolean hasNext() {
        return it.hasNext();
    }

    public DEST next() {
        try {
            lastSrc = it.next();
            return lastDest = convert(lastSrc);
        } catch (NoSuchElementException e) {
            lastDest = null;
            lastSrc = null;
            throw e;
        }
    }

    public void remove() {
        if (lastSrc != null) {
            it.remove();
            noteRemoval(lastDest, lastSrc);
            lastDest = null;
            lastSrc = null;
        } else {
            throw new IllegalStateException();
        }
    }

    protected abstract void noteRemoval(DEST dest, SRC src);
    protected abstract DEST convert(SRC src);
}

