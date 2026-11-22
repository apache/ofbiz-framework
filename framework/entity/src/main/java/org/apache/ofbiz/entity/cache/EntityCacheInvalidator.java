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
package org.apache.ofbiz.entity.cache;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.cache.CacheInvalidation;
import org.apache.ofbiz.base.util.cache.InvalidationCodec;
import org.apache.ofbiz.base.util.cache.InvalidationHandler;
import org.apache.ofbiz.base.util.cache.InvalidationMessage;
import org.apache.ofbiz.base.util.cache.InvalidationMessage.ClearAll;
import org.apache.ofbiz.base.util.cache.InvalidationMessage.EntityClear;
import org.apache.ofbiz.base.util.cache.InvalidationMessage.EntityClearAll;
import org.apache.ofbiz.base.util.cache.InvalidationMessage.EntityKey;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntity;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericPK;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.model.ModelField;
import org.apache.ofbiz.entity.model.ModelFieldType;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionFactoryLoader;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.DistributedCacheClear;

import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;

/**
 * Tells the other servers of a cluster about entity changes through the installed cache invalidation bus, and applies
 * the changes they report. Select it on a delegator with {@code distributed-cache-clear-enabled="true"} and
 * {@code distributed-cache-clear-class-name="org.apache.ofbiz.entity.cache.EntityCacheInvalidator"}. The entity caches
 * are distributed this way whatever their {@code <cache>} policy.
 * <p>Without an installed bus it does nothing: the delegator has already cleared its local cache before calling it, and
 * there is no one to tell.</p>
 * <p>Inside a transaction the invalidations are queued. After commit the invalidation versions of the touched entities,
 * and of the view entities containing them, are moved on, then the local cache is cleared again and the queue is
 * published. Moving the versions keeps a find on this server that read the database before the commit from caching its
 * result afterwards; the second clear removes what such a find cached before the versions moved. A find running inside
 * an older REPEATABLE READ snapshot is not covered: it can still read and cache a row older than the change. On rollback
 * nothing is published or cleared again. Messages carry primary key values as strings, never entity values. A
 * transaction that changes many rows publishes clears of the touched entities instead of keys, see
 * {@link #MAX_KEYS_PER_ENTITY} and {@link #MAX_MESSAGES}.</p>
 */
public final class EntityCacheInvalidator implements DistributedCacheClear {

    private static final String MODULE = EntityCacheInvalidator.class.getName();
    /**
     * The most primary key invalidations, or local condition re-clears, of one entity a transaction keeps; past it they
     * are replaced by a single clear of the entity. A bulk change of thousands of rows would otherwise hold a message and
     * a local re-clear per row in memory until commit, run them all after commit, and hand the bus provider one very
     * large payload, when clearing the entity is cheaper and just as correct.
     */
    public static final int MAX_KEYS_PER_ENTITY = 100;
    /**
     * The most invalidations of any kind a transaction keeps; past it every touched entity is replaced by a single clear
     * of that entity, for the same reasons of memory, after-commit work and payload size.
     */
    public static final int MAX_MESSAGES = 1000;
    /** The delegators using this class, which received entity messages are applied to. */
    private static final Set<Delegator> DELEGATORS = new CopyOnWriteArraySet<>();
    private static final AtomicBoolean HANDLER_REGISTERED = new AtomicBoolean();
    private static final Map<PendingKey, PendingInvalidations> PENDING = new ConcurrentHashMap<>();
    /** The re-clear key of the delegator's clear-all. */
    private static final Object CLEAR_ALL = new Object();
    /** The re-clear key of the single re-clear left once an entity is collapsed. */
    private static final Object COLLAPSED = new Object();

    private Delegator delegator;

    public EntityCacheInvalidator() { }

    @Override
    public void setDelegator(Delegator delegator, String userLoginId) {
        this.delegator = delegator;
        DELEGATORS.add(delegator);
        if (HANDLER_REGISTERED.compareAndSet(false, true)) {
            CacheInvalidation.addHandler(new RemoteEntityInvalidations());
        }
    }

    @Override
    public void distributedClearCacheLine(GenericValue value) {
        InvalidationMessage message = messageFor(value);
        enqueue(message, reclearKey(message), () -> delegator.clearCacheLine(value, false));
    }

    @Override
    public void distributedClearCacheLineFlexible(GenericEntity dummyPK) {
        InvalidationMessage message = messageFor(dummyPK);
        Object key = message instanceof EntityKey ? message
                : new ReclearKey("flexible", new HashMap<>(dummyPK.getAllFields()));
        enqueue(message, key, () -> delegator.clearCacheLineFlexible(dummyPK, false));
    }

    @Override
    public void distributedClearCacheLineByCondition(String entityName, EntityCondition condition) {
        Runnable localReclear = () -> delegator.clearCacheLineByCondition(entityName, condition, false);
        enqueue(new EntityClear(delegator.getDelegatorName(), entityName), new ReclearKey("condition", condition),
                localReclear);
    }

    @Override
    public void distributedClearCacheLine(GenericPK primaryKey) {
        InvalidationMessage message = messageFor(primaryKey);
        enqueue(message, reclearKey(message), () -> delegator.clearCacheLine(primaryKey, false));
    }

    @Override
    public void clearAllCaches() {
        // entity caches only, as the JMS path did; other caches publish their own clears
        enqueue(new EntityClearAll(delegator.getDelegatorName()), CLEAR_ALL, () -> delegator.clearAllCaches(false));
    }

    /**
     * The key under which a primary key re-clear is deduplicated: every re-clear of the same primary key removes the
     * same cached value and the entity's whole condition caches, so one is enough. A re-clear of an incomplete key is
     * not deduplicated.
     */
    private static Object reclearKey(InvalidationMessage message) {
        return message instanceof EntityKey ? message : new Object();
    }

    /**
     * An entity key when every primary key field has a value, otherwise a clear of the whole entity. Timestamp values are
     * sent as an ISO-8601 instant, so servers in different time zones read the same value; a key with another date or
     * time field is sent as a clear, as its string form depends on the time zone.
     */
    private InvalidationMessage messageFor(GenericEntity entity) {
        String entityName = entity.getEntityName();
        ModelEntity model = entity.getModelEntity();
        Map<String, String> pkFields = new LinkedHashMap<>();
        for (String fieldName : model.getPkFieldNames()) {
            Object value = entity.get(fieldName);
            if (value == null || (value instanceof java.util.Date && !(value instanceof Timestamp))) {
                return new EntityClear(delegator.getDelegatorName(), entityName);
            }
            pkFields.put(fieldName, value instanceof Timestamp timestamp ? timestamp.toInstant().toString()
                    : entity.getString(fieldName));
        }
        return new EntityKey(delegator.getDelegatorName(), entityName, pkFields);
    }

    private void enqueue(InvalidationMessage message, Object reclearKey, Runnable localReclear) {
        if (!CacheInvalidation.isActive()) {
            // the delegator has already cleared the local cache, and there is no other server to tell
            return;
        }
        try {
            Transaction transaction = activeTransaction();
            if (transaction != null) {
                pendingFor(transaction, delegator).add(message, reclearKey, localReclear);
                return;
            }
            if (TransactionUtil.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
                // the change will be rolled back, so there is nothing to tell the other servers
                return;
            }
        } catch (GenericTransactionException e) {
            Debug.logWarning(e, "Could not check the current transaction; publishing the cache invalidation now", MODULE);
        }
        CacheInvalidation.publish(List.of(message));
    }

    private static Transaction activeTransaction() throws GenericTransactionException {
        if (TransactionUtil.getStatus() != Status.STATUS_ACTIVE) {
            return null;
        }
        try {
            return TransactionFactoryLoader.getInstance().getTransactionManager().getTransaction();
        } catch (SystemException e) {
            throw new GenericTransactionException("Could not get the current transaction", e);
        }
    }

    private static PendingInvalidations pendingFor(Transaction transaction, Delegator delegator)
            throws GenericTransactionException {
        PendingKey key = new PendingKey(transaction, delegator.getDelegatorName());
        PendingInvalidations pending = PENDING.get(key);
        if (pending == null) {
            pending = new PendingInvalidations(key, delegator);
            // registered before it is remembered, so a failed registration leaves nothing behind
            try {
                transaction.registerSynchronization(pending);
            } catch (RollbackException | SystemException e) {
                throw new GenericTransactionException("Could not register the cache invalidations with the transaction",
                        e);
            }
            PENDING.put(key, pending);
        }
        return pending;
    }

    /** One delegator's invalidations within one transaction. */
    private record PendingKey(Transaction transaction, String delegatorName) { }

    /** Identifies a local re-clear that does the same work as another of the same entity. */
    private record ReclearKey(String kind, Object identity) { }

    /**
     * The invalidations of one delegator in one transaction, run after it completes. Bounded: see
     * {@link #MAX_KEYS_PER_ENTITY} and {@link #MAX_MESSAGES}.
     */
    private static final class PendingInvalidations implements Synchronization {
        private final PendingKey key;
        private final Delegator delegator;
        /** The invalidations of each touched entity. */
        private final Map<String, EntityInvalidations> byEntity = new LinkedHashMap<>();
        /** Whether the delegator's clear-all was called, which replaces everything else. */
        private boolean clearAll;
        /** Whether the transaction went past {@link #MAX_MESSAGES}, so every touched entity is collapsed. */
        private boolean collapsedAll;
        private int messageCount;
        private int reclearCount;

        private PendingInvalidations(PendingKey key, Delegator delegator) {
            this.key = key;
            this.delegator = delegator;
        }

        private void add(InvalidationMessage message, Object reclearKey, Runnable localReclear) {
            if (clearAll) {
                return;
            }
            if (message instanceof EntityClearAll) {
                clearAll = true;
                byEntity.clear();
                return;
            }
            String entityName = switch (message) {
            case EntityKey m -> m.entityName();
            case EntityClear m -> m.entityName();
            default -> throw new IllegalArgumentException("Not an entity invalidation: " + message);
            };
            EntityInvalidations entity = byEntity.computeIfAbsent(entityName, EntityInvalidations::new);
            int messagesBefore = entity.messageCount();
            int reclearsBefore = entity.localReclears.size();
            if (collapsedAll) {
                entity.collapse(delegator);
            } else {
                entity.add(message, reclearKey, localReclear, delegator);
            }
            messageCount += entity.messageCount() - messagesBefore;
            reclearCount += entity.localReclears.size() - reclearsBefore;
            if (!collapsedAll && (messageCount > MAX_MESSAGES || reclearCount > MAX_MESSAGES)) {
                collapsedAll = true;
                for (EntityInvalidations touched : byEntity.values()) {
                    touched.collapse(delegator);
                }
                messageCount = byEntity.size();
                reclearCount = byEntity.size();
            }
        }

        @Override
        public void beforeCompletion() {
        }

        @Override
        public void afterCompletion(int status) {
            PENDING.remove(key);
            // publish on commit and on an unknown or heuristic outcome: over-invalidating is harmless
            if (status == Status.STATUS_ROLLEDBACK) {
                return;
            }
            Cache cache = delegator.getCache();
            List<Runnable> reclears = new ArrayList<>();
            List<InvalidationMessage> toPublish = new ArrayList<>();
            // the versions move before the re-clears, so a find that read the old row before the commit cannot cache it
            // once the re-clear has run
            if (clearAll) {
                cache.invalidateAllVersions();
                reclears.add(() -> delegator.clearAllCaches(false));
                toPublish.add(new EntityClearAll(delegator.getDelegatorName()));
            } else {
                for (EntityInvalidations entity : byEntity.values()) {
                    try {
                        cache.invalidateVersion(entity.entityName);
                    } catch (RuntimeException e) {
                        Debug.logWarning(e, "Could not move the invalidation version of entity [" + entity.entityName
                                + "] after commit", MODULE);
                    }
                    reclears.addAll(entity.localReclears.values());
                    if (entity.clearEntity) {
                        toPublish.add(new EntityClear(delegator.getDelegatorName(), entity.entityName));
                    } else {
                        toPublish.addAll(entity.keys);
                    }
                }
            }
            for (Runnable localReclear : reclears) {
                try {
                    localReclear.run();
                } catch (RuntimeException e) {
                    Debug.logWarning(e, "Could not clear the local entity cache after commit", MODULE);
                }
            }
            CacheInvalidation.publish(toPublish);
        }
    }

    /** The invalidations of one entity in one transaction, with their deduplicated local re-clears. */
    private static final class EntityInvalidations {
        private final String entityName;
        /** Whether the entity is published as one clear; its primary keys are then not published. */
        private boolean clearEntity;
        /** Whether the local re-clears were replaced by a single removal of the entity. */
        private boolean collapsed;
        private final Set<EntityKey> keys = new LinkedHashSet<>();
        private final Map<Object, Runnable> localReclears = new LinkedHashMap<>();

        private EntityInvalidations(String entityName) {
            this.entityName = entityName;
        }

        private int messageCount() {
            return clearEntity ? 1 : keys.size();
        }

        private void add(InvalidationMessage message, Object reclearKey, Runnable localReclear, Delegator delegator) {
            if (collapsed) {
                return;
            }
            if (message instanceof EntityKey entityKey) {
                if (!clearEntity) {
                    keys.add(entityKey);
                }
            } else {
                // a clear of the entity covers its keys on the other servers
                clearEntity = true;
                keys.clear();
            }
            localReclears.putIfAbsent(reclearKey, localReclear);
            if (keys.size() > MAX_KEYS_PER_ENTITY || localReclears.size() > MAX_KEYS_PER_ENTITY) {
                collapse(delegator);
            }
        }

        /** Replaces the entity's invalidations by one clear, and its local re-clears by one removal of the entity. */
        private void collapse(Delegator delegator) {
            if (collapsed) {
                return;
            }
            collapsed = true;
            clearEntity = true;
            keys.clear();
            localReclears.clear();
            // clears the cache lines locally without going through the distributed cache clear again
            localReclears.put(COLLAPSED, () -> delegator.getCache().removeEntityAndViews(entityName));
        }
    }

    /** Applies entity invalidations received from other servers to the delegators using this class. */
    private static final class RemoteEntityInvalidations implements InvalidationHandler {
        @Override
        public void onMessages(List<InvalidationMessage> messages) {
            for (InvalidationMessage message : messages) {
                try {
                    apply(message);
                } catch (RuntimeException e) {
                    Debug.logWarning(e, "Could not apply a received cache invalidation ["
                            + InvalidationCodec.sanitizeForLog(message.toString()) + "]", MODULE);
                }
            }
        }

        private static void apply(InvalidationMessage message) {
            switch (message) {
            case EntityKey m -> applyKey(m);
            case EntityClear m -> {
                Delegator target = find(m.delegatorName());
                if (target != null && cachedEntity(target, m.entityName()) != null) {
                    target.getCache().invalidateFromRemote(m.entityName());
                }
            }
            case EntityClearAll m -> {
                Delegator target = find(m.delegatorName());
                if (target != null) {
                    target.getCache().invalidateAllVersions();
                    target.getCache().clear();
                }
            }
            // UtilCache skips local caches, which the entity caches are unless declared otherwise, so they are cleared here
            case ClearAll m -> DELEGATORS.forEach(RemoteEntityInvalidations::clearEntityCaches);
            default -> {
                // cache messages are applied by UtilCache
            }
            }
        }

        @Override
        public void onGap() {
            DELEGATORS.forEach(RemoteEntityInvalidations::clearEntityCaches);
        }

        /** Moves every invalidation version on first, so loads in flight do not cache what they read, then clears. */
        private static void clearEntityCaches(Delegator target) {
            target.getCache().invalidateAllVersions();
            target.getCache().clear();
        }

        private static void applyKey(EntityKey message) {
            Delegator target = find(message.delegatorName());
            if (target == null) {
                return;
            }
            ModelEntity model = cachedEntity(target, message.entityName());
            if (model == null) {
                return;
            }
            try {
                GenericPK pk = target.makePK(message.entityName());
                for (Map.Entry<String, String> field : message.pkFields().entrySet()) {
                    if (!model.isField(field.getKey())) {
                        throw new IllegalArgumentException("Unknown field ["
                                + InvalidationCodec.sanitizeForLog(field.getKey()) + "]");
                    }
                    setField(target, model, pk, field.getKey(), field.getValue());
                }
                if (pk.isPrimaryKey()) {
                    target.getCache().invalidateFromRemote(pk);
                    return;
                }
            } catch (RuntimeException e) {
                Debug.logWarning("Invalid primary key in a cache invalidation for entity ["
                        + InvalidationCodec.sanitizeForLog(message.entityName()) + "], clearing the whole entity: "
                        + InvalidationCodec.sanitizeForLog(e.getMessage()), MODULE);
            }
            target.getCache().invalidateFromRemote(message.entityName());
        }

        /** Sets a received primary key field; a timestamp field is read as the ISO-8601 instant {@code messageFor} sends. */
        private static void setField(Delegator target, ModelEntity model, GenericPK pk, String name, String value) {
            if (isTimestamp(target, model, name)) {
                try {
                    pk.set(name, Timestamp.from(Instant.parse(value)));
                    return;
                } catch (DateTimeParseException e) {
                    // not an instant, perhaps from a server running an older version: fall back to the string form
                }
            }
            pk.setString(name, value);
        }

        private static boolean isTimestamp(Delegator target, ModelEntity model, String name) {
            ModelField field = model.getField(name);
            if (field == null) {
                return false;
            }
            try {
                ModelFieldType type = target.getEntityFieldType(model, field.getType());
                return type != null && "java.sql.Timestamp".equals(type.getJavaType());
            } catch (GenericEntityException e) {
                return false;
            }
        }

        private static Delegator find(String delegatorName) {
            for (Delegator candidate : DELEGATORS) {
                if (candidate.getDelegatorName().equals(delegatorName)) {
                    return candidate;
                }
            }
            return null;
        }

        /** The entity's model when it exists and may be cached, otherwise {@code null} (logged when unknown). */
        private static ModelEntity cachedEntity(Delegator target, String entityName) {
            ModelEntity model = target.getModelReader().getModelEntityNoCheck(entityName);
            if (model == null) {
                Debug.logWarning("Ignoring a cache invalidation for the unknown entity ["
                        + InvalidationCodec.sanitizeForLog(entityName) + "]", MODULE);
                return null;
            }
            return model.getNeverCache() ? null : model;
        }
    }
}
