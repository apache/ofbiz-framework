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
package org.apache.ofbiz.base.util;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

public final class ReferenceCleaner {
    public static final String module = ReferenceCleaner.class.getName();

    private static final class CleanerThread extends Thread {
        private boolean keepRunning = true;

        protected CleanerThread() {
            setDaemon(true);
            setName("ReferenceCleaner");
        }

        protected void stopRunning() {
            keepRunning = false;
        }

        @Override
        public void run() {
            while (keepRunning) {
                try {
                    ((Removable) QUEUE.remove()).remove();
                } catch (Throwable t) {
                    // ignore
                }
                if (interrupted()) {
                    stopRunning();
                    cleanerThread = new CleanerThread();
                    cleanerThread.start();
                }
            }
        }
    }
    private static CleanerThread cleanerThread = new CleanerThread();

    static {
        cleanerThread.start();
    }

    private ReferenceCleaner() {
    }

    private static final ReferenceQueue<Object> QUEUE = new ReferenceQueue<>();

    public interface Removable {
        void remove() throws Exception;
    }

    public abstract static class Soft<V> extends SoftReference<V> implements Removable {
        public Soft(V value) {
            super(value, QUEUE);
        }
    }

    public abstract static class Phantom<V> extends PhantomReference<V> implements Removable {
        public Phantom(V value) {
            super(value, QUEUE);
        }
    }

    public abstract static class Weak<V> extends WeakReference<V> implements Removable {
        public Weak(V value) {
            super(value, QUEUE);
        }
    }
}
