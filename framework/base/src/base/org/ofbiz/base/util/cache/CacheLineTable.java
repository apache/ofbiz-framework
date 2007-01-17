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
package org.ofbiz.base.util.cache;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.commons.collections.map.LRUMap;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.ObjectType;


public class CacheLineTable implements Serializable {

    public static final String module = CacheLineTable.class.getName();
    protected static transient jdbm.RecordManager jdbmMgr = null;

    protected transient jdbm.htree.HTree fileTable = null;
    protected Map memoryTable = null;
    protected String fileStore = null;
    protected String cacheName = null;
    protected int maxInMemory = 0;

    public CacheLineTable(String fileStore, String cacheName, boolean useFileSystemStore, int maxInMemory) {
        this.fileStore = fileStore;
        this.cacheName = cacheName;
        this.maxInMemory = maxInMemory;
        if (useFileSystemStore) {
            // create the manager the first time it is needed
            if (CacheLineTable.jdbmMgr == null) {
                synchronized (this) {
                    if (CacheLineTable.jdbmMgr == null) {
                        try {
                            Debug.logImportant("Creating file system cache store for cache with name: " + cacheName, module);
                            CacheLineTable.jdbmMgr = new JdbmRecordManager(fileStore);
                        } catch (IOException e) {
                            Debug.logError(e, "Error creating file system cache store for cache with name: " + cacheName, module);
                        }
                    }
                }
            }
            if (CacheLineTable.jdbmMgr != null) {
                try {
                    long recno = CacheLineTable.jdbmMgr.getNamedObject(cacheName);
                    if (recno != 0) {
                        this.fileTable = jdbm.htree.HTree.load(CacheLineTable.jdbmMgr, recno);
                    } else {
                        this.fileTable = jdbm.htree.HTree.createInstance(CacheLineTable.jdbmMgr);
                        CacheLineTable.jdbmMgr.setNamedObject(cacheName, this.fileTable.getRecid());
                        CacheLineTable.jdbmMgr.commit();
                    }
                } catch (IOException e) {
                    Debug.logError(e, module);
                }
            }
        }
        this.setLru(maxInMemory);
    }

    public synchronized Object put(Object key, Object value) {
        if (key == null) {
            if (Debug.verboseOn()) Debug.logVerbose("In CacheLineTable tried to put with null key, using NullObject" + this.cacheName, module);
            key = ObjectType.NULL;
        }
        memoryTable.put(key, value);
        if (fileTable != null) {
            try {
                fileTable.put(key, value);                
                CacheLineTable.jdbmMgr.commit();
            } catch (IOException e) {
                Debug.logError(e, module);
            }
        }
        return value;
    }

    public Object get(Object key) {
        if (key == null) {
            if (Debug.verboseOn()) Debug.logVerbose("In CacheLineTable tried to get with null key, using NullObject" + this.cacheName, module);
            key = ObjectType.NULL;
        }
        Object value = memoryTable.get(key);
        if (value == null) {
            if (fileTable != null) {
                try {
                    value = fileTable.get(key);
                } catch (IOException e) {
                    Debug.logError(e, module);
                }
            }
        }
        return value;
    }

    public synchronized Object remove(Object key) {
        if (key == null) {
            if (Debug.verboseOn()) Debug.logVerbose("In CacheLineTable tried to remove with null key, using NullObject" + this.cacheName, module);
            key = ObjectType.NULL;
        }
        Object value = this.get(key);
        if (fileTable != null) {
            try {
                fileTable.remove(key);
            } catch (IOException e) {
                Debug.logError(e, module);
            }
        }
        memoryTable.remove(key);
        return value;
    }

    public synchronized Collection values() {
        List values = FastList.newInstance();

        if (fileTable != null) {
            try {
                jdbm.helper.FastIterator iter = fileTable.values();
                Object value = iter.next();
                while (value != null) {
                    values.add(value);
                    value = iter.next();
                }
            } catch (IOException e) {
                Debug.logError(e, module);
            }
        } else {
            values.addAll(memoryTable.values());
        }

        return values;
    }

    /**
     * 
     * @return An unmodifiable Set for the keys for this cache; to remove while iterating call the remove method on this class.
     */
    public synchronized Set keySet() {
        // note that this must be a HashSet and not a FastSet in order to have a null value
        Set keys = new HashSet();

        if (fileTable != null) {
            try {
                jdbm.helper.FastIterator iter = fileTable.keys();
                Object key = null;
                while ((key = iter.next()) != null) {
                    if (key instanceof ObjectType.NullObject) {
                        keys.add(null);
                    } else {
                        keys.add(key);
                    }
                }
            } catch (IOException e) {
                Debug.logError(e, module);
            }
        } else {
            keys.addAll(memoryTable.keySet());
            if (keys.contains(ObjectType.NULL)) {
                keys.remove(ObjectType.NULL);
                keys.add(null);
            }
        }

        return Collections.unmodifiableSet(keys);
    }

    public synchronized void clear() {
        if (fileTable != null && this.size() > 0) {
            try {
                // remove this table
                long recid = fileTable.getRecid();
                CacheLineTable.jdbmMgr.delete(recid);
                CacheLineTable.jdbmMgr.commit();
                this.fileTable = null;                

                // create a new table
                this.fileTable = jdbm.htree.HTree.createInstance(CacheLineTable.jdbmMgr);
                CacheLineTable.jdbmMgr.setNamedObject(cacheName, this.fileTable.getRecid());
                CacheLineTable.jdbmMgr.commit();
            } catch (IOException e) {
                Debug.logError(e, module);
            }
        }
        memoryTable.clear();
    }

    public int size() {
        if (fileTable != null) {
            return this.keySet().size();
        } else {
            return memoryTable.size();
        }
    }

    public synchronized void setLru(int newSize) {
        this.maxInMemory = newSize;

        Map oldmap = null;
        if (this.memoryTable != null) {
            // using linked map to preserve the order when using LRU (FastMap is a linked map)
            oldmap = FastMap.newInstance();
            oldmap.putAll(this.memoryTable);
        }

        if (newSize > 0) {
            this.memoryTable = new LRUMap(newSize);
        } else {
            this.memoryTable = FastMap.newInstance();
        }

        if (oldmap != null) {
            this.memoryTable.putAll(oldmap);
        }
    }

    public synchronized Object getKeyFromMemory(int index) {
        Iterator i = null;
        if (memoryTable instanceof LRUMap) {
            i = ((LRUMap) memoryTable).orderedMapIterator();
        } else {
            i = memoryTable.keySet().iterator();
        }

        int currentIdx = 0;
        while (i.hasNext()) {
            Object key = i.next();
            if (currentIdx == index) {
                return key;
            }
        }
        return null;
    }
}

