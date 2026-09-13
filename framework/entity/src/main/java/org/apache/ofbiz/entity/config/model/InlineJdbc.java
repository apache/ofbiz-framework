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

import java.util.Map;

/**
 * An object that models the <code>&lt;inline-jdbc&gt;</code> element.
 *
 * @see <code>entity-config.xsd</code>
 */
@ThreadSafe
public final class InlineJdbc extends JdbcElement {

    private final EntityConfigGetter config = EntityConfigGetter.getInstance();
    public static final String ELEMENT_NAME = "inline-jdbc";

    private final String jdbcDriver;
    private final String jdbcUri;
    private final String jdbcUsername;
    private final String jdbcPassword;
    private final String jdbcPasswordLookup;
    private final int poolMaxsize;
    private final int poolMinsize;
    private final int idleMaxsize;
    private final int timeBetweenEvictionRunsMillis;
    private final int softMinEvictableIdleTimeMillis;
    private final int poolSleeptime;
    private final int poolLifetime;
    private final int poolDeadlockMaxwait;
    private final int poolDeadlockRetrywait;
    private final String poolJdbcTestStmt;
    private final boolean testOnCreate;
    private final boolean testOnBorrow;
    private final boolean testOnReturn;
    private final boolean testWhileIdle;
    private final String poolXaWrapperClass;

    InlineJdbc(Element element, String xPathParent) throws GenericEntityConfException {
        super(element, xPathParent.concat("/inline-jdbc"));
        String lineNumberText = EntityConfig.createConfigFileLineNumberText(element);
        String jdbcDriver = config.getValue(getXPath() + "/@jdbc-driver");
        if (jdbcDriver.isEmpty()) {
            throw new GenericEntityConfException("<inline-jdbc> element jdbc-driver attribute is empty" + lineNumberText);
        }
        this.jdbcDriver = jdbcDriver;
        String jdbcUri = config.getValue(getXPath() + "/@jdbc-uri");
        if (jdbcUri.isEmpty()) {
            throw new GenericEntityConfException("<inline-jdbc> element jdbc-uri attribute is empty" + lineNumberText);
        }
        this.jdbcUri = jdbcUri;
        String jdbcUsername = config.getValue(getXPath() + "/@jdbc-username");
        if (jdbcUsername.isEmpty()) {
            throw new GenericEntityConfException("<inline-jdbc> element jdbc-username attribute is empty" + lineNumberText);
        }
        this.jdbcUsername = jdbcUsername;
        jdbcPassword = config.getValue(getXPath() + "/@jdbc-password");
        jdbcPasswordLookup = config.getValue(getXPath() + "/@jdbc-password-lookup");

        poolMaxsize = config.getValue(getXPath() + "/@pool-maxsize", 50, Integer.class);
        poolMinsize = config.getValue(getXPath() + "/@pool-minsize", 2, Integer.class);
        idleMaxsize = config.getValue(getXPath() + "/@idle-maxsize", poolMaxsize / 2, Integer.class);

        timeBetweenEvictionRunsMillis = config.getValue(getXPath() + "/@time-between-eviction-runs-millis", 600000, Integer.class);
        softMinEvictableIdleTimeMillis = config.getValue(getXPath() + "/@soft-min-evictable-idle-time-millis", 600000, Integer.class);
        poolSleeptime = config.getValue(getXPath() + "/@pool-sleeptime", 300000, Integer.class);
        poolLifetime = config.getValue(getXPath() + "/@pool-lifetime", 600000, Integer.class);
        poolDeadlockMaxwait = config.getValue(getXPath() + "/@pool-deadlock-maxwait", 300000, Integer.class);
        poolDeadlockRetrywait = config.getValue(getXPath() + "/@pool-deadlock-retrywait", 10000, Integer.class);
        poolJdbcTestStmt = config.getValue(getXPath() + "/@pool-jdbc-test-stmt");
        testOnCreate = "true".equals(config.getValue(getXPath() + "/@test-on-create"));
        testOnBorrow = "true".equals(config.getValue(getXPath() + "/@test-on-borrow"));
        testOnReturn = "true".equals(config.getValue(getXPath() + "/@test-on-return"));
        testWhileIdle = "true".equals(config.getValue(getXPath() + "/@test-while-idle"));
        poolXaWrapperClass = config.getValue(getXPath() + "/@pool-xa-wrapper-class");
    }

    InlineJdbc(Map<String, Object> configObject, String xPath) throws GenericEntityConfException {
        super(configObject, xPath);
        String jdbcDriver = config.getValue(configObject, "/@jdbc-driver");
        if (jdbcDriver.isEmpty()) {
            throw new GenericEntityConfException("<inline-jdbc> element jdbc-driver attribute is empty");
        }
        this.jdbcDriver = jdbcDriver;
        String jdbcUri = config.getValue(configObject, "/@jdbc-uri");
        if (jdbcUri.isEmpty()) {
            throw new GenericEntityConfException("<inline-jdbc> element jdbc-uri attribute is empty");
        }
        this.jdbcUri = jdbcUri;
        String jdbcUsername = config.getValue(configObject, "/@jdbc-username");
        if (jdbcUsername.isEmpty()) {
            throw new GenericEntityConfException("<inline-jdbc> element jdbc-username attribute is empty");
        }
        this.jdbcUsername = jdbcUsername;
        jdbcPassword = config.getValue(configObject, "/@jdbc-password");
        jdbcPasswordLookup = config.getValue(configObject, "/@jdbc-password-lookup");

        poolMaxsize = config.getValue(configObject, "/@pool-maxsize", 50, Integer.class);
        poolMinsize = config.getValue(configObject, "/@pool-minsize", 2, Integer.class);
        idleMaxsize = config.getValue(configObject, "/@idle-maxsize", poolMaxsize / 2, Integer.class);

        timeBetweenEvictionRunsMillis = config.getValue(configObject, "/@time-between-eviction-runs-millis", 600000, Integer.class);
        softMinEvictableIdleTimeMillis = config.getValue(configObject, "/@soft-min-evictable-idle-time-millis", 600000, Integer.class);
        poolSleeptime = config.getValue(configObject, "/@pool-sleeptime", 300000, Integer.class);
        poolLifetime = config.getValue(configObject, "/@pool-lifetime", 600000, Integer.class);
        poolDeadlockMaxwait = config.getValue(configObject, "/@pool-deadlock-maxwait", 300000, Integer.class);
        poolDeadlockRetrywait = config.getValue(configObject, "/@pool-deadlock-retrywait", 10000, Integer.class);
        poolJdbcTestStmt = config.getValue(configObject, "/@pool-jdbc-test-stmt");
        testOnCreate = "true".equals(config.getValue(configObject, "/@test-on-create"));
        testOnBorrow = "true".equals(config.getValue(configObject, "/@test-on-borrow"));
        testOnReturn = "true".equals(config.getValue(configObject, "/@test-on-return"));
        testWhileIdle = "true".equals(config.getValue(configObject, "/@test-while-idle"));
        poolXaWrapperClass = config.getValue(configObject, "/@pool-xa-wrapper-class");
    }

    public String getJdbcDriver() {
        return jdbcDriver;
    }

    public String getJdbcUri() {
        return jdbcUri;
    }

    public String getJdbcUsername() {
        return jdbcUsername;
    }

    public String getJdbcPassword() {
        return jdbcPassword;
    }

    public String getJdbcPasswordLookup() {
        return jdbcPasswordLookup;
    }

    public int getPoolMaxsize() {
        return poolMaxsize;
    }

    public int getPoolMinsize() {
        return poolMinsize;
    }

    public int getIdleMaxsize() {
        return idleMaxsize;
    }

    public int getTimeBetweenEvictionRunsMillis() {
        return timeBetweenEvictionRunsMillis;
    }

    public int getSoftMinEvictableIdleTimeMillis() {
        return softMinEvictableIdleTimeMillis;
    }

    public int getPoolSleeptime() {
        return poolSleeptime;
    }

    public int getPoolLifetime() {
        return poolLifetime;
    }

    public int getPoolDeadlockMaxwait() {
        return poolDeadlockMaxwait;
    }

    public int getPoolDeadlockRetrywait() {
        return poolDeadlockRetrywait;
    }

    public String getPoolJdbcTestStmt() {
        return poolJdbcTestStmt;
    }

    public boolean getTestOnCreate() {
        return testOnCreate;
    }

    public boolean getTestOnBorrow() {
        return testOnBorrow;
    }

    public boolean getTestOnReturn() {
        return testOnReturn;
    }

    public boolean getTestWhileIdle() {
        return testWhileIdle;
    }

    public String getPoolXaWrapperClass() {
        return poolXaWrapperClass;
    }

    public static InlineJdbc loadFromXml(Element element, String xPathParent) throws GenericEntityConfException {
        return new InlineJdbc(element, xPathParent);
    }

    public static InlineJdbc loadFromConfig(Map<String, Object> configMap, String xPath) throws GenericEntityConfException {
        return new InlineJdbc(configMap, xPath);
    }

    @Override
    public String getName() {
        return "inline-jdbc";
    }
}
