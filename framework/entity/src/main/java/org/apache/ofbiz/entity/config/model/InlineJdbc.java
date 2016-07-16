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
package org.apache.ofbiz.entity.config.model;

import org.apache.ofbiz.base.lang.ThreadSafe;
import org.apache.ofbiz.entity.GenericEntityConfException;
import org.w3c.dom.Element;

/**
 * An object that models the <code>&lt;inline-jdbc&gt;</code> element.
 *
 * @see <code>entity-config.xsd</code>
 */
@ThreadSafe
public final class InlineJdbc extends JdbcElement {

    private final String jdbcDriver; // type = xs:string
    private final String jdbcUri; // type = xs:string
    private final String jdbcUsername; // type = xs:string
    private final String jdbcPassword; // type = xs:string
    private final String jdbcPasswordLookup; // type = xs:string
    private final int poolMaxsize; // type = xs:nonNegativeInteger
    private final int poolMinsize; // type = xs:nonNegativeInteger
    private final int idleMaxsize; // type = xs:nonNegativeInteger
    private final int timeBetweenEvictionRunsMillis; // type = xs:nonNegativeInteger
    private final int softMinEvictableIdleTimeMillis; // type = xs:nonNegativeInteger
    private final int poolSleeptime; // type = xs:nonNegativeInteger
    private final int poolLifetime; // type = xs:nonNegativeInteger
    private final int poolDeadlockMaxwait; // type = xs:nonNegativeInteger
    private final int poolDeadlockRetrywait; // type = xs:nonNegativeInteger
    private final String poolJdbcTestStmt; // type = xs:string
    private final boolean testOnCreate; // type = xs:boolean
    private final boolean testOnBorrow; // type = xs:boolean
    private final boolean testOnReturn; // type = xs:boolean
    private final boolean testWhileIdle; // type = xs:boolean
    private final String poolXaWrapperClass; // type = xs:string

    InlineJdbc(Element element) throws GenericEntityConfException {
        super(element);
        String lineNumberText = EntityConfig.createConfigFileLineNumberText(element);
        String jdbcDriver = element.getAttribute("jdbc-driver").intern();
        if (jdbcDriver.isEmpty()) {
            throw new GenericEntityConfException("<inline-jdbc> element jdbc-driver attribute is empty" + lineNumberText);
        }
        this.jdbcDriver = jdbcDriver;
        String jdbcUri = element.getAttribute("jdbc-uri").intern();
        if (jdbcUri.isEmpty()) {
            throw new GenericEntityConfException("<inline-jdbc> element jdbc-uri attribute is empty" + lineNumberText);
        }
        this.jdbcUri = jdbcUri;
        String jdbcUsername = element.getAttribute("jdbc-username").intern();
        if (jdbcUsername.isEmpty()) {
            throw new GenericEntityConfException("<inline-jdbc> element jdbc-username attribute is empty" + lineNumberText);
        }
        this.jdbcUsername = jdbcUsername;
        this.jdbcPassword = element.getAttribute("jdbc-password").intern();
        this.jdbcPasswordLookup = element.getAttribute("jdbc-password-lookup").intern();
        String poolMaxsize = element.getAttribute("pool-maxsize");
        if (poolMaxsize.isEmpty()) {
            this.poolMaxsize = 50;
        } else {
            try {
                this.poolMaxsize = Integer.parseInt(poolMaxsize);
            } catch (Exception e) {
                throw new GenericEntityConfException("<inline-jdbc> element pool-maxsize attribute is invalid" + lineNumberText);
            }
        }
        String poolMinsize = element.getAttribute("pool-minsize");
        if (poolMinsize.isEmpty()) {
            this.poolMinsize = 2;
        } else {
            try {
                this.poolMinsize = Integer.parseInt(poolMinsize);
            } catch (Exception e) {
                throw new GenericEntityConfException("<inline-jdbc> element pool-minsize attribute is invalid" + lineNumberText);
            }
        }
        String idleMaxsize = element.getAttribute("idle-maxsize");
        if (idleMaxsize.isEmpty()) {
            this.idleMaxsize = this.poolMaxsize / 2;
        } else {
            try {
                this.idleMaxsize = Integer.parseInt(idleMaxsize);
            } catch (Exception e) {
                throw new GenericEntityConfException("<inline-jdbc> element idle-maxsize attribute is invalid" + lineNumberText);
            }
        }
        String timeBetweenEvictionRunsMillis = element.getAttribute("time-between-eviction-runs-millis");
        if (timeBetweenEvictionRunsMillis.isEmpty()) {
            this.timeBetweenEvictionRunsMillis = 600000;
        } else {
            try {
                this.timeBetweenEvictionRunsMillis = Integer.parseInt(timeBetweenEvictionRunsMillis);
            } catch (Exception e) {
                throw new GenericEntityConfException("<inline-jdbc> element time-between-eviction-runs-millis attribute is invalid" + lineNumberText);
            }
        }
        String softMinEvictableIdleTimeMillis = element.getAttribute("soft-min-evictable-idle-time-millis");
        if (softMinEvictableIdleTimeMillis.isEmpty()) {
            this.softMinEvictableIdleTimeMillis = 600000;
        } else {
            try {
                this.softMinEvictableIdleTimeMillis = Integer.parseInt(softMinEvictableIdleTimeMillis);
            } catch (Exception e) {
                throw new GenericEntityConfException("<inline-jdbc> element soft-min-evictable-idle-time-millis attribute is invalid" + lineNumberText);
            }
        }
        String poolSleeptime = element.getAttribute("pool-sleeptime");
        if (poolSleeptime.isEmpty()) {
            this.poolSleeptime = 300000;
        } else {
            try {
                this.poolSleeptime = Integer.parseInt(poolSleeptime);
            } catch (Exception e) {
                throw new GenericEntityConfException("<inline-jdbc> element pool-sleeptime attribute is invalid" + lineNumberText);
            }
        }
        String poolLifetime = element.getAttribute("pool-lifetime");
        if (poolLifetime.isEmpty()) {
            this.poolLifetime = 600000;
        } else {
            try {
                this.poolLifetime = Integer.parseInt(poolLifetime);
            } catch (Exception e) {
                throw new GenericEntityConfException("<inline-jdbc> element pool-lifetime attribute is invalid" + lineNumberText);
            }
        }
        String poolDeadlockMaxwait = element.getAttribute("pool-deadlock-maxwait");
        if (poolDeadlockMaxwait.isEmpty()) {
            this.poolDeadlockMaxwait = 300000;
        } else {
            try {
                this.poolDeadlockMaxwait = Integer.parseInt(poolDeadlockMaxwait);
            } catch (Exception e) {
                throw new GenericEntityConfException("<inline-jdbc> element pool-deadlock-maxwait attribute is invalid" + lineNumberText);
            }
        }
        String poolDeadlockRetrywait = element.getAttribute("pool-deadlock-retrywait");
        if (poolDeadlockRetrywait.isEmpty()) {
            this.poolDeadlockRetrywait = 10000;
        } else {
            try {
                this.poolDeadlockRetrywait = Integer.parseInt(poolDeadlockRetrywait);
            } catch (Exception e) {
                throw new GenericEntityConfException("<inline-jdbc> element pool-deadlock-retrywait attribute is invalid" + lineNumberText);
            }
        }
        this.poolJdbcTestStmt = element.getAttribute("pool-jdbc-test-stmt").intern();
        this.testOnCreate = "true".equals(element.getAttribute("test-on-create"));
        this.testOnBorrow = "true".equals(element.getAttribute("test-on-borrow"));
        this.testOnReturn = "true".equals(element.getAttribute("test-on-return"));
        this.testWhileIdle = "true".equals(element.getAttribute("test-while-idle"));
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

    /** Returns the value of the <code>pool-maxsize</code> attribute. */
    public int getPoolMaxsize() {
        return this.poolMaxsize;
    }

    /** Returns the value of the <code>pool-minsize</code> attribute. */
    public int getPoolMinsize() {
        return this.poolMinsize;
    }

    /** Returns the value of the <code>idle-maxsize</code> attribute. */
    public int getIdleMaxsize() {
        return this.idleMaxsize;
    }

    /** Returns the value of the <code>time-between-eviction-runs-millis</code> attribute. */
    public int getTimeBetweenEvictionRunsMillis() {
        return this.timeBetweenEvictionRunsMillis;
    }

    /** Returns the value of the <code>time-between-eviction-runs-millis</code> attribute. */
    public int getSoftMinEvictableIdleTimeMillis() {
        return this.softMinEvictableIdleTimeMillis;
    }

    /** Returns the value of the <code>pool-sleeptime</code> attribute. */
    public int getPoolSleeptime() {
        return this.poolSleeptime;
    }

    /** Returns the value of the <code>pool-lifetime</code> attribute. */
    public int getPoolLifetime() {
        return this.poolLifetime;
    }

    /** Returns the value of the <code>pool-deadlock-maxwait</code> attribute. */
    public int getPoolDeadlockMaxwait() {
        return this.poolDeadlockMaxwait;
    }

    /** Returns the value of the <code>pool-deadlock-retrywait</code> attribute. */
    public int getPoolDeadlockRetrywait() {
        return this.poolDeadlockRetrywait;
    }

    /** Returns the value of the <code>pool-jdbc-test-stmt</code> attribute. */
    public String getPoolJdbcTestStmt() {
        return this.poolJdbcTestStmt;
    }

    /** Returns the value of the <code>test-on-create</code> attribute. */
    public boolean getTestOnCreate() {
        return this.testOnCreate;
    }

    /** Returns the value of the <code>test-on-create</code> attribute. */
    public boolean getTestOnBorrow() {
        return this.testOnBorrow;
    }

    /** Returns the value of the <code>test-on-create</code> attribute. */
    public boolean getTestOnReturn() {
        return this.testOnReturn;
    }

    /** Returns the value of the <code>test-on-create</code> attribute. */
    public boolean getTestWhileIdle() {
        return this.testWhileIdle;
    }

    /** Returns the value of the <code>pool-xa-wrapper-class</code> attribute. */
    public String getPoolXaWrapperClass() {
        return this.poolXaWrapperClass;
    }
}
