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

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.ofbiz.base.lang.ObjectWrapper;
import org.ofbiz.base.lang.SourceMonitored;
import org.ofbiz.base.util.UtilGenerics;

@SourceMonitored
public abstract class TTLObject<T> implements ObjectWrapper<T> {
    private static final ScheduledExecutorService updateExecutor = ExecutionPool.getNewOptimalExecutor("TTLObject(async-update)");

    private static final <T> T getConfigForClass(ConcurrentHashMap<String, T> config, Class<?> c) {
        Class<?> ptr = c;
        T value = null;
        while (value == null && ptr != null) {
            value = config.get(ptr.getName());
            ptr = ptr.getSuperclass();
        }
        return value;
    }

    private static final ConcurrentHashMap<String, Long> ttls = new ConcurrentHashMap<String, Long>();

    public static void setDefaultTTLForClass(Class<?> c, long ttl) {
        ttls.putIfAbsent(c.getName(), ttl);
    }

    public static void setTTLForClass(Class<?> c, long ttl) {
        ttls.put(c.getName(), ttl);
    }

    public static long getTTLForClass(Class<?> c) throws ConfigurationException {
        Long ttl = getConfigForClass(ttls, c);
        if (ttl != null) return ttl.longValue();
        throw new ConfigurationException("No TTL defined for " + c.getName());
    }

    private static final ConcurrentHashMap<String, Boolean> inForeground = new ConcurrentHashMap<String, Boolean>();

    public static void setDefaultForegroundForClass(Class<?> c, boolean foreground) {
        inForeground.putIfAbsent(c.getName(), foreground);
    }

    public static void setForegroundForClass(Class<?> c, boolean foreground) {
        inForeground.put(c.getName(), foreground);
    }

    public static boolean getForegroundForClass(Class<?> c) {
        Boolean foreground = getConfigForClass(inForeground, c);
        if (foreground != null) return foreground.booleanValue();
        return true;
    }

    public static void pulseAll() {
        ExecutionPool.pulseAll(Pulse.class);
    }

    public enum State { INVALID, REGEN, REGENERATING, GENERATE, GENERATING, GENERATING_INITIAL, VALID, ERROR, ERROR_INITIAL, SET }
    @SuppressWarnings("unchecked")
    private static final AtomicReferenceFieldUpdater<TTLObject<?>, ValueAndState> objectAccessor = UtilGenerics.cast(AtomicReferenceFieldUpdater.newUpdater(TTLObject.class, ValueAndState.class, "object"));
    private static final AtomicIntegerFieldUpdater<TTLObject<?>> serialAccessor = UtilGenerics.cast(AtomicIntegerFieldUpdater.newUpdater(TTLObject.class, "serial"));
    protected volatile int serial;

    protected static abstract class ValueAndState<T> {
        protected final TTLObject<T> ttlObject;
        protected final FutureTask<T> future;
        protected final State state;
        protected final int serial;
        protected final Throwable t;
        protected final Pulse pulse;

        protected ValueAndState(TTLObject<T> ttlObject, FutureTask<T> future, State state, int serial, Throwable t, Pulse pulse) {
            this.ttlObject = ttlObject;
            this.future = future;
            this.state = state;
            this.serial = serial;
            this.t = t;
            this.pulse = pulse;
        }

        protected abstract T getValue();

        protected ValueAndState<T> refresh(State nextState) {
            return ttlObject.newValueAndState(getValue(), future, nextState, serial, null, null);
        }

        protected ValueAndState<T> valid(T value) {
            return ttlObject.newValueAndState(value, null, State.VALID, serialAccessor.incrementAndGet(ttlObject), null, new Pulse(ttlObject));
        }

        protected ValueAndState<T> set(T value) {
            return ttlObject.newValueAndState(value, null, State.SET, serialAccessor.incrementAndGet(ttlObject), null, null);
        }

        protected ValueAndState<T> submit(final T oldValue, State state) {
            return ttlObject.newValueAndState(getValue(), createTask(oldValue), state, serial, null, null);
        }

        protected FutureTask<T> createTask(final T oldValue) {
            return new FutureTask<T>(new Callable<T>() {
                public T call() throws Exception {
                    return ttlObject.load(oldValue, serial);
                }
            });
        }

        protected ValueAndState<T> error(Throwable t) {
            return ttlObject.newValueAndState(null, null, state != State.GENERATING_INITIAL ? State.ERROR : State.ERROR_INITIAL, serialAccessor.incrementAndGet(ttlObject), t, new Pulse(ttlObject));
        }
    }

    protected ValueAndState<T> newValueAndState(T value, FutureTask<T> future, State state, int serial, Throwable t, Pulse pulse) {
        return new StandardValueAndState<T>(this, value, future, state, serial, t, pulse);
    }

    @SuppressWarnings("hiding")
    private class StandardValueAndState<T> extends ValueAndState<T> {
        protected final T value;

        protected StandardValueAndState(TTLObject<T> ttlObject, T value, FutureTask<T> future, State state, int serial, Throwable t, Pulse pulse) {
            super(ttlObject, future, state, serial, t, pulse);
            this.value = value;
        }

        @Override
        protected T getValue() {
            return value;
        }
    }

    protected final static class Pulse extends ExecutionPool.Pulse {
        protected final TTLObject<?> ttlObject;

        protected Pulse(TTLObject<?> ttlObject) {
            super(TimeUnit.NANOSECONDS.convert(ttlObject.getTTL(), TimeUnit.MILLISECONDS));
            this.ttlObject = ttlObject;
        }

        public void run() {
            ttlObject.refresh();
        }
    }

    public State getState() {
        return getContainer().state;
    }

    @SuppressWarnings("unchecked")
    private final ValueAndState<T> getContainer() {
        return objectAccessor.get(this);
    }

    public void refresh() {
        ValueAndState<T> container;
        ValueAndState<T> nextContainer = null;
        do {
            container = getContainer();
            if (container.state == State.INVALID) {
                nextContainer = container.refresh(State.GENERATE);
            } else if (container.state == State.REGENERATING) {
                nextContainer = container.refresh(State.REGEN);
            } else if (container.state == State.GENERATING) {
                nextContainer = container.refresh(State.GENERATE);
            } else if (container.state == State.ERROR_INITIAL) {
                nextContainer = container.refresh(State.INVALID);
            } else if (container.state == State.ERROR || container.state == State.VALID) {
                nextContainer = container.refresh(getForeground() ? State.GENERATE : State.REGEN);
            } else if (container.state == State.SET) {
                nextContainer = container.refresh(getForeground() ? State.GENERATE : State.REGEN);
            } else {
                return;
            }
            objectAccessor.compareAndSet(this, container, nextContainer);
            cancelFuture(container);
        } while (true);
    }

    public final int getSerial() {
        return getContainer().serial;
    }

    public final boolean checkSerial(int serial) {
        return getContainer().serial != serial;
    }

    protected final void setObject(T newObject) {
        ValueAndState<T> container = getContainer();
        ValueAndState<T> nextContainer = container.set(newObject);
        objectAccessor.compareAndSet(this, container, nextContainer);
        cancelFuture(container);
    }

    private void cancelFuture(ValueAndState<T> container) {
        ExecutionPool.removePulse(container.pulse);
        if (container.state == State.REGENERATING || container.state == State.GENERATING) {
            container.future.cancel(false);
        }
    }

    public final T getObject() throws ObjectException {
        try {
            ValueAndState<T> container;
            ValueAndState<T> nextContainer = null;
            do {
                do {
                    container = getContainer();
                    if (container.state == State.ERROR || container.state == State.ERROR_INITIAL) {
                        throw container.t;
                    } else if (container.state == State.VALID) {
                        return container.getValue();
                    } else if (container.state == State.INVALID) {
                        nextContainer = container.submit(getInitial(), State.GENERATING_INITIAL);
                    } else if (container.state == State.SET) {
                        nextContainer = container.valid(container.getValue());
                    } else if (container.state == State.REGENERATING || container.state == State.GENERATING || container.state == State.GENERATING_INITIAL) {
                        if (!container.future.isDone()) {
                            if (container.state == State.GENERATING || container.state == State.GENERATING_INITIAL) {
                                container.future.run();
                            } else {
                                return container.getValue();
                            }
                        }
                        try {
                            try {
                                nextContainer = container.valid(container.future.get());
                            } catch (ExecutionException e) {
                                throw e.getCause();
                            }
                        } catch (Throwable t) {
                            nextContainer = container.error(t);
                        }
                    } else if (container.state == State.REGEN) {
                        nextContainer = container.submit(container.getValue(), State.REGENERATING);
                    } else {
                        nextContainer = container.submit(container.getValue(), State.GENERATING);
                    }
                } while (!objectAccessor.compareAndSet(this, container, nextContainer));
                if (nextContainer.state == State.REGENERATING) {
                    updateExecutor.submit(nextContainer.future);
                } else if (nextContainer.pulse != null) {
                    ExecutionPool.removePulse(container.pulse);
                    ExecutionPool.addPulse(nextContainer.pulse);
                }
            } while (true);
        } catch (Throwable e) {
            return ObjectException.<T>checkException(e);
        }
    }

    protected T getInitial() throws Exception {
        return null;
    }

    protected abstract T load(T old, int serial) throws Exception;

    protected boolean getForeground() {
        return getForegroundForClass(getClass());
    }

    protected long getTTL() throws ConfigurationException {
        return getTTLForClass(getClass());
    }
}
