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
package org.ofbiz.webslinger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericPK;
import org.ofbiz.entity.GenericValue;
import org.webslinger.cache.ConcurrentCache;
import org.webslinger.concurrent.ExecutionPool;

public class StatsUpdater {
    private static final Updater UPDATER = new Updater();

    public static void updateStats(Delegator delegator, String entityName, Map<String, ? extends Object> keyFields, Map<String, ? extends Long> updateCountFields) throws GenericEntityException {
        GenericPK pk = delegator.makePK(entityName, keyFields);
        Map<String, Long> value = UPDATER.getValue(pk);
        synchronized (value) {
            for (Map.Entry<String, ? extends Long> entry: updateCountFields.entrySet()) {
                Long oldValue = value.get(entry.getKey());
                if (oldValue != null) {
                    value.put(entry.getKey(), Long.valueOf(oldValue.longValue() + entry.getValue()));
                } else {
                    value.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    private static final class Updater implements Callable<Void> {
        protected AtomicReference<EntityHolder> entities = new AtomicReference<EntityHolder>(new EntityHolder(Updater.class, "entities", null));
        protected ScheduledFuture<Void> future;

        protected Map<String, Long> getValue(GenericPK pk) throws GenericEntityException {
            synchronized (this) {
                if (future == null || future.isDone()) {
                    future = ExecutionPool.schedule(this, 1, TimeUnit.SECONDS);
                }
            }
            try {
                return entities.get().get(pk);
            } catch (RuntimeException e) {
                throw e;
            } catch (GenericEntityException e) {
                throw e;
            } catch (Exception e) {
                throw UtilMisc.initCause(new GenericEntityException(e.getMessage()), e);
            }
        }

        public Void call() {
            EntityHolder oldEntities;
            EntityHolder newEntities = new EntityHolder(Updater.class, "entities", null);
            do {
                oldEntities = entities.get();
            } while (!entities.compareAndSet(oldEntities, newEntities));
            synchronized (Updater.class) {
                for (GenericPK pk: oldEntities.keys()) {
                    try {
                        Map<String, Long> add = oldEntities.get(pk);
                        GenericValue existing = pk.getDelegator().findOne(pk.getEntityName(), pk, false);
                        if (existing == null) {
                            existing = pk.getDelegator().create(pk.getEntityName(), pk);
                        }
                        for (Map.Entry<String, Long> entry: add.entrySet()) {
                            Long value = entry.getValue();
                            Long oldValue = existing.getLong(entry.getKey());
                            if (oldValue != null) {
                                existing.put(entry.getKey(), Long.valueOf(value.longValue() + oldValue.longValue()));
                            } else {
                                existing.put(entry.getKey(), value);
                            }
                        }
                        existing.store();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return null;
        }
    }

    private static final class EntityHolder extends ConcurrentCache<GenericPK, Map<String, Long>> {
        protected EntityHolder(Class<?> owner, String field, String label) {
            super(owner, field, label, HARD);
        }

        @Override
        protected Map<String, Long> createValue(GenericPK pk) throws Exception {
            return new HashMap<String, Long>();
        }
    }
}
