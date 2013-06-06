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
package org.ofbiz.entity.config.model;

import org.ofbiz.base.lang.ThreadSafe;
import org.ofbiz.entity.GenericEntityConfException;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;inline-jdbc&gt;</code> element.
 *
 * @see <code>entity-config.xsd</code>
 */
@ThreadSafe
public final class InlineJdbc {

    private final String jdbcDriver; // type = xs:string
    private final String jdbcUri; // type = xs:string
    private final String jdbcUsername; // type = xs:string
    private final String jdbcPassword; // type = xs:string
    private final String jdbcPasswordLookup; // type = xs:string
    private final String isolationLevel;
    private final String poolMaxsize; // type = xs:nonNegativeInteger
    private final String poolMinsize; // type = xs:nonNegativeInteger
    private final String idleMaxsize; // type = xs:nonNegativeInteger
    private final String timeBetweenEvictionRunsMillis; // type = xs:nonNegativeInteger
    private final String poolSleeptime; // type = xs:nonNegativeInteger
    private final String poolLifetime; // type = xs:nonNegativeInteger
    private final String poolDeadlockMaxwait; // type = xs:nonNegativeInteger
    private final String poolDeadlockRetrywait; // type = xs:nonNegativeInteger
    private final String poolJdbcTestStmt; // type = xs:string
    private final String poolXaWrapperClass; // type = xs:string

    public InlineJdbc(Element element) throws GenericEntityConfException {
        String jdbcDriver = element.getAttribute("jdbc-driver").intern();
        if (jdbcDriver.isEmpty()) {
            throw new GenericEntityConfException("<" + element.getNodeName() + "> element jdbc-driver attribute is empty");
        }
        this.jdbcDriver = jdbcDriver;
        String jdbcUri = element.getAttribute("jdbc-uri").intern();
        if (jdbcUri.isEmpty()) {
            throw new GenericEntityConfException("<" + element.getNodeName() + "> element jdbc-uri attribute is empty");
        }
        this.jdbcUri = jdbcUri;
        String jdbcUsername = element.getAttribute("jdbc-username").intern();
        if (jdbcUsername.isEmpty()) {
            throw new GenericEntityConfException("<" + element.getNodeName() + "> element jdbc-username attribute is empty");
        }
        this.jdbcUsername = jdbcUsername;
        this.jdbcPassword = element.getAttribute("jdbc-password").intern();
        this.jdbcPasswordLookup = element.getAttribute("jdbc-password-lookup").intern();
        this.isolationLevel = element.getAttribute("isolation-level").intern();
        String poolMaxsize = element.getAttribute("pool-maxsize").intern();
        if (poolMaxsize.isEmpty()) {
            poolMaxsize = "50";
        }
        this.poolMaxsize = poolMaxsize;
        String poolMinsize = element.getAttribute("pool-minsize").intern();
        if (poolMinsize.isEmpty()) {
            poolMinsize = "2";
        }
        this.poolMinsize = poolMinsize;
        this.idleMaxsize = element.getAttribute("idle-maxsize").intern();
        String timeBetweenEvictionRunsMillis = element.getAttribute("time-between-eviction-runs-millis").intern();
        if (timeBetweenEvictionRunsMillis.isEmpty()) {
            timeBetweenEvictionRunsMillis = "600000";
        }
        this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
        String poolSleeptime = element.getAttribute("pool-sleeptime").intern();
        if (poolSleeptime.isEmpty()) {
            poolSleeptime = "300000";
        }
        this.poolSleeptime = poolSleeptime;
        String poolLifetime = element.getAttribute("pool-lifetime").intern();
        if (poolLifetime.isEmpty()) {
            poolLifetime = "600000";
        }
        this.poolLifetime = poolLifetime;
        String poolDeadlockMaxwait = element.getAttribute("pool-deadlock-maxwait").intern();
        if (poolDeadlockMaxwait.isEmpty()) {
            poolDeadlockMaxwait = "300000";
        }
        this.poolDeadlockMaxwait = poolDeadlockMaxwait;
        String poolDeadlockRetrywait = element.getAttribute("pool-deadlock-retrywait").intern();
        if (poolDeadlockRetrywait.isEmpty()) {
            poolDeadlockRetrywait = "10000";
        }
        this.poolDeadlockRetrywait = poolDeadlockRetrywait;
        this.poolJdbcTestStmt = element.getAttribute("pool-jdbc-test-stmt").intern();
        this.poolXaWrapperClass = element.getAttribute("pool-xa-wrapper-class").intern();
    }

    /** Returns the value of the <code>jdbc-driver</code> attribute. */
    public String getJdbcDriver() {
        return this.jdbcDriver;
    }

    /** Returns the value of the <code>jdbc-uri</code> attribute. */
    public String getJdbcUri() {
        return this.jdbcUri;
    }

    /** Returns the value of the <code>jdbc-username</code> attribute. */
    public String getJdbcUsername() {
        return this.jdbcUsername;
    }

    /** Returns the value of the <code>jdbc-password</code> attribute. */
    public String getJdbcPassword() {
        return this.jdbcPassword;
    }

    /** Returns the value of the <code>jdbc-password-lookup</code> attribute. */
    public String getJdbcPasswordLookup() {
        return this.jdbcPasswordLookup;
    }

    /** Returns the value of the <code>isolation-level</code> attribute. */
    public String getIsolationLevel() {
        return this.isolationLevel;
    }

    /** Returns the value of the <code>pool-maxsize</code> attribute. */
    public String getPoolMaxsize() {
        return this.poolMaxsize;
    }

    /** Returns the value of the <code>pool-minsize</code> attribute. */
    public String getPoolMinsize() {
        return this.poolMinsize;
    }

    /** Returns the value of the <code>idle-maxsize</code> attribute. */
    public String getIdleMaxsize() {
        return this.idleMaxsize;
    }

    /** Returns the value of the <code>time-between-eviction-runs-millis</code> attribute. */
    public String getTimeBetweenEvictionRunsMillis() {
        return this.timeBetweenEvictionRunsMillis;
    }

    /** Returns the value of the <code>pool-sleeptime</code> attribute. */
    public String getPoolSleeptime() {
        return this.poolSleeptime;
    }

    /** Returns the value of the <code>pool-lifetime</code> attribute. */
    public String getPoolLifetime() {
        return this.poolLifetime;
    }

    /** Returns the value of the <code>pool-deadlock-maxwait</code> attribute. */
    public String getPoolDeadlockMaxwait() {
        return this.poolDeadlockMaxwait;
    }

    /** Returns the value of the <code>pool-deadlock-retrywait</code> attribute. */
    public String getPoolDeadlockRetrywait() {
        return this.poolDeadlockRetrywait;
    }

    /** Returns the value of the <code>pool-jdbc-test-stmt</code> attribute. */
    public String getPoolJdbcTestStmt() {
        return this.poolJdbcTestStmt;
    }

    /** Returns the value of the <code>pool-xa-wrapper-class</code> attribute. */
    public String getPoolXaWrapperClass() {
        return this.poolXaWrapperClass;
    }
}
