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

import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class IteratorWrapper<DEST, SRC> implements Iterator<DEST> {
    private final Iterator<? extends SRC> it;
    private boolean nextCalled;
    private DEST lastDest;
    private SRC lastSrc;

    protected IteratorWrapper(Iterator<? extends SRC> it) {
        this.it = it;
    }

    @Override
    public boolean hasNext() {
        if (nextCalled) {
            return true;
        }
        if (!it.hasNext()) {
            return false;
        }
        do {
            lastSrc = it.next();
            DEST nextDest = convert(lastSrc);
            if (isValid(lastSrc, nextDest)) {
                nextCalled = true;
                lastDest = nextDest;
                return true;
            }
        } while (it.hasNext());
        return false;
    }

    @Override
    public DEST next() {
        if (!nextCalled) {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
        }
        nextCalled = false;
        return lastDest;
    }

    @Override
    public void remove() {
        if (lastSrc != null) {
            noteRemoval(lastDest, lastSrc);
            it.remove();
            lastDest = null;
            lastSrc = null;
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Is valid boolean.
     * @param src the src
     * @param dest the dest
     * @return the boolean
     */
    protected boolean isValid(SRC src, DEST dest) {
        return true;
    }

    protected abstract void noteRemoval(DEST dest, SRC src);
    protected abstract DEST convert(SRC src);
}

