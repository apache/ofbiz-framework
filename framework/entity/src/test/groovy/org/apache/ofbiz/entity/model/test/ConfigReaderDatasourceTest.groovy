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
package org.apache.ofbiz.entity.model.test

import org.apache.ofbiz.entity.config.model.Datasource
import org.apache.ofbiz.entity.config.model.EntityConfig
import org.junit.jupiter.api.Test

class ConfigReaderDatasourceTest extends BaseEntityConfigReaderTest {

    @Test
    void testLoadDatasourceFromXmlWithFalseValue() {
        EntityConfig config = mockXmlAndHoconCgf('''
    <datasource name="datasourcetest"
                add-missing-on-start="false"
                alias-view-columns="false"
                always-use-constraint-keyword="false"
                character-set="character-set-test"
                check-fks-indices-on-start="false"
                check-fks-on-start="false"
                check-indices-on-start="false"
                check-on-start="false"
                check-pks-on-start="false"
                collate="collate-test"
                constraint-name-clip-length="888"
                drop-fk-use-foreign-key-keyword="false"
                field-type-name="field-type-test"
                fk-style="fk-style-test"
                helper-class="org.apache.ofbiz.entity.datasource.GenericTest"
                join-style="join-test"
                max-worker-pool-size="999"
                offset-style="offset-style-test"
                proxy-cursor-name="proxy-test"
                result-fetch-size="99"
                schema-name="schema-test"
                table-type="table-test"
                use-binary-type-for-blob="false"
                use-fk-initially-deferred="false"
                use-foreign-key-indices="false"
                use-foreign-keys="false"
                use-indices="false"
                use-indices-unique="false"
                use-order-by-nulls="false"
                use-pk-constraint-names="false"
                use-proxy-cursor="false"
                use-schemas="false">
        <read-data reader-name="reader-test1"/>
        <read-data reader-name="reader-test2"/>
    </datasource>''', '')
        Map<String, Datasource> datasources = config.getDatasourceMap()
        Datasource datasource = datasources.datasourcetest

        assert datasource
        datasource.with {
            assert getName() == 'datasourcetest'
            assert !getAddMissingOnStart()
            assert !getAliasViewColumns()
            assert !getAlwaysUseConstraintKeyword()
            assert getCharacterSet() == 'character-set-test'
            assert !getCheckFkIndicesOnStart()
            assert !getCheckFksOnStart()
            assert !getCheckIndicesOnStart()
            assert !getCheckOnStart()
            assert !getCheckPksOnStart()
            assert getCollate() == 'collate-test'
            assert getConstraintNameClipLength() == 888
            assert !getDropFkUseForeignKeyKeyword()
            assert getFieldTypeName() == 'field-type-test'
            assert getFkStyle() == 'fk-style-test'
            assert getHelperClass() == 'org.apache.ofbiz.entity.datasource.GenericTest'
            assert getJoinStyle() == 'join-test'
            assert getMaxWorkerPoolSize() == 999
            assert getOffsetStyle() == 'offset-style-test'
            assert getProxyCursorName() == 'proxy-test'
            assert getResultFetchSize() == 99
            assert getSchemaName() == 'schema-test'
            // TODO assert getSqlLoadPathList()
            assert getTableType() == 'table-test'
            assert !getUseBinaryTypeForBlob()
            assert !getUseFkInitiallyDeferred()
            assert !getUseForeignKeyIndices()
            assert !getUseForeignKeys()
            assert !getUseIndices()
            assert !getUseIndicesUnique()
            assert !getUseOrderByNulls()
            assert !getUsePkConstraintNames()
            assert !getUseProxyCursor()
            assert !getUseSchemas()
            assert getReadDataList()?.size() == 2
        }
    }

    @Test
    void testLoadDatasourceFromXmlWithTrueValue() {
        EntityConfig config = mockXmlAndHoconCgf('''
    <datasource name="datasourcetest"
                add-missing-on-start="true"
                alias-view-columns="true"
                always-use-constraint-keyword="true"
                character-set="character-set-test"
                check-fks-indices-on-start="true"
                check-fks-on-start="true"
                check-indices-on-start="true"
                check-on-start="true"
                check-pks-on-start="true"
                collate="collate-test"
                constraint-name-clip-length="888"
                drop-fk-use-foreign-key-keyword="true"
                field-type-name="field-type-test"
                fk-style="fk-style-test"
                helper-class="org.apache.ofbiz.entity.datasource.GenericTest"
                join-style="join-test"
                max-worker-pool-size="999"
                offset-style="offset-style-test"
                proxy-cursor-name="proxy-test"
                result-fetch-size="99"
                schema-name="schema-test"
                table-type="table-test"
                use-binary-type-for-blob="true"
                use-fk-initially-deferred="true"
                use-foreign-key-indices="true"
                use-foreign-keys="true"
                use-indices="true"
                use-indices-unique="true"
                use-order-by-nulls="true"
                use-pk-constraint-names="true"
                use-proxy-cursor="true"
                use-schemas="true">
        <read-data reader-name="reader-test1"/>
        <read-data reader-name="reader-test2"/>
    </datasource>''', '')
        Map<String, Datasource> datasources = config.getDatasourceMap()
        Datasource datasource = datasources.datasourcetest

        assert datasource
        datasource.with {
            assert getName() == 'datasourcetest'
            assert getAddMissingOnStart()
            assert getAliasViewColumns()
            assert getAlwaysUseConstraintKeyword()
            assert getCharacterSet() == 'character-set-test'
            assert getCheckFkIndicesOnStart()
            assert getCheckFksOnStart()
            assert getCheckIndicesOnStart()
            assert getCheckOnStart()
            assert getCheckPksOnStart()
            assert getCollate() == 'collate-test'
            assert getConstraintNameClipLength() == 888
            assert getDropFkUseForeignKeyKeyword()
            assert getFieldTypeName() == 'field-type-test'
            assert getFkStyle() == 'fk-style-test'
            assert getHelperClass() == 'org.apache.ofbiz.entity.datasource.GenericTest'
            assert getJoinStyle() == 'join-test'
            assert getMaxWorkerPoolSize() == 999
            assert getOffsetStyle() == 'offset-style-test'
            assert getProxyCursorName() == 'proxy-test'
            assert getResultFetchSize() == 99
            assert getSchemaName() == 'schema-test'
            // TODO assert getSqlLoadPathList()
            assert getTableType() == 'table-test'
            assert getUseBinaryTypeForBlob()
            assert getUseFkInitiallyDeferred()
            assert getUseForeignKeyIndices()
            assert getUseForeignKeys()
            assert getUseIndices()
            assert getUseIndicesUnique()
            assert getUseOrderByNulls()
            assert getUsePkConstraintNames()
            assert getUseProxyCursor()
            assert getUseSchemas()
            assert getReadDataList()?.size() == 2
        }
    }

    @Test
    void testLoadDatasourceWithOverrideValue() {
        EntityConfig config = mockXmlAndHoconCgf('''
    <datasource name="datasourcetestover"
                add-missing-on-start="true"
                alias-view-columns="true"
                always-use-constraint-keyword="true"
                character-set="character-set-test"
                check-fks-indices-on-start="true"
                check-fks-on-start="true"
                check-indices-on-start="true"
                check-on-start="true"
                check-pks-on-start="true"
                collate="collate-test"
                constraint-name-clip-length="888"
                drop-fk-use-foreign-key-keyword="true"
                field-type-name="field-type-test"
                fk-style="fk-style-test"
                helper-class="org.apache.ofbiz.entity.datasource.GenericTest"
                join-style="join-test"
                max-worker-pool-size="999"
                offset-style="offset-style-test"
                proxy-cursor-name="proxy-test"
                result-fetch-size="99"
                schema-name="schema-test"
                table-type="table-test"
                use-binary-type-for-blob="true"
                use-fk-initially-deferred="true"
                use-foreign-key-indices="true"
                use-foreign-keys="true"
                use-indices="true"
                use-indices-unique="true"
                use-order-by-nulls="true"
                use-pk-constraint-names="true"
                use-proxy-cursor="true"
                use-schemas="true">
    </datasource>''',
                '''"datasource" : {
        "datasourcetestover": {
                "add-missing-on-start": "false",
                "alias-view-columns": "false",
                "always-use-constraint-keyword": "false",
                "character-set": "character-set-overridetest",
                "check-fks-indices-on-start": "false",
                "check-fks-on-start": "false",
                "check-indices-on-start": "false",
                "check-on-start": "false",
                "check-pks-on-start": "false",
                "collate": "collate-overridetest",
                "constraint-name-clip-length": "777",
                "drop-fk-use-foreign-key-keyword": "false",
                "field-type-name": "field-type-overridetest",
                "fk-style": "fk-style-overridetest",
                "helper-class": "org.apache.ofbiz.entity.datasource.GenericTest",
                "join-style": "join-overridetest",
                "max-worker-pool-size": "777",
                "offset-style": "offset-style-overridetest",
                "proxy-cursor-name": "proxy-overridetest",
                "result-fetch-size": "77",
                "schema-name": "schema-overridetest",
                "table-type": "table-overridetest",
                "use-binary-type-for-blob": "false",
                "use-fk-initially-deferred": "false",
                "use-foreign-key-indices": "false",
                "use-foreign-keys": "false",
                "use-indices": "false",
                "use-indices-unique": "false",
                "use-order-by-nulls": "false",
                "use-pk-constraint-names": "false",
                "use-proxy-cursor": "false",
                "use-schemas": "false"
                }}
''')
        Map<String, Datasource> datasources = config.getDatasourceMap()
        Datasource datasource = datasources.datasourcetestover

        assert datasource
        datasource.with {
            assert getName() == 'datasourcetestover'
            assert !getAddMissingOnStart()
            assert !getAliasViewColumns()
            assert !getAlwaysUseConstraintKeyword()
            assert getCharacterSet() == 'character-set-overridetest'
            assert !getCheckFkIndicesOnStart()
            assert !getCheckFksOnStart()
            assert !getCheckIndicesOnStart()
            assert !getCheckOnStart()
            assert !getCheckPksOnStart()
            assert getCollate() == 'collate-overridetest'
            assert getConstraintNameClipLength() == 777
            assert !getDropFkUseForeignKeyKeyword()
            assert getFieldTypeName() == 'field-type-overridetest'
            assert getFkStyle() == 'fk-style-overridetest'
            assert getHelperClass() == 'org.apache.ofbiz.entity.datasource.GenericTest'
            assert getJoinStyle() == 'join-overridetest'
            assert getMaxWorkerPoolSize() == 777
            assert getOffsetStyle() == 'offset-style-overridetest'
            assert getProxyCursorName() == 'proxy-overridetest'
            assert getResultFetchSize() == 77
            assert getSchemaName() == 'schema-overridetest'
            // TODO assert getSqlLoadPathList()
            assert getTableType() == 'table-overridetest'
            assert !getUseBinaryTypeForBlob()
            assert !getUseFkInitiallyDeferred()
            assert !getUseForeignKeyIndices()
            assert !getUseForeignKeys()
            assert !getUseIndices()
            assert !getUseIndicesUnique()
            assert !getUseOrderByNulls()
            assert !getUsePkConstraintNames()
            assert !getUseProxyCursor()
            assert !getUseSchemas()
        }
    }

    @Test
    void testLoadingLineJdbcFromXmlWithFalseValue() {
        EntityConfig config = mockXmlAndHoconCgf('''
    <datasource name="testinlinejdbc1">
            <inline-jdbc
                idle-maxsize="11"
                jdbc-driver="jdbc-driver-test"
                jdbc-password="jdbc-password-test"
                jdbc-password-lookup="jdbc-password-lookup-test"
                jdbc-uri="jdbc-uri-test"
                jdbc-username="jdbc-username-test"
                pool-deadlock-maxwait="16"
                pool-deadlock-retrywait="17"
                pool-jdbc-test-stmt="pool-test"
                pool-lifetime="15"
                pool-maxsize="10"
                pool-minsize="1"
                pool-sleeptime="14"
                soft-min-evictable-idle-time-millis="13"
                test-on-borrow="false"
                test-on-create="false"
                test-on-return="false"
                test-while-idle="false"
                time-between-eviction-runs-millis="12"
        />
    </datasource>''', '')
        Datasource datasource = config.getDatasourceMap()?.testinlinejdbc1
        assert datasource
        datasource.getInlineJdbc().with {
            assert getIdleMaxsize() == 11
            assert getJdbcDriver() == 'jdbc-driver-test'
            assert getJdbcPassword() == 'jdbc-password-test'
            assert getJdbcPasswordLookup() == 'jdbc-password-lookup-test'
            assert getJdbcUri() == 'jdbc-uri-test'
            assert getJdbcUsername() == 'jdbc-username-test'
            assert getPoolDeadlockMaxwait() == 16
            assert getPoolDeadlockRetrywait() == 17
            assert getPoolJdbcTestStmt() == 'pool-test'
            assert getPoolLifetime() == 15
            assert getPoolMaxsize() == 10
            assert getPoolMinsize() == 1
            assert getPoolSleeptime() == 14
            assert getSoftMinEvictableIdleTimeMillis() == 13
            assert !getTestOnBorrow()
            assert !getTestOnCreate()
            assert !getTestOnReturn()
            assert !getTestWhileIdle()
            assert getTimeBetweenEvictionRunsMillis() == 12
        }
    }

    @Test
    void testLoadInLineJdbcFromXmlWithTrueValue() {
        EntityConfig config = mockXmlAndHoconCgf('''
    <datasource name="testinlinejdbc2">
            <inline-jdbc
                jdbc-driver="empty"
                jdbc-uri="empty"
                jdbc-username="empty"
                test-on-borrow="true"
                test-on-create="true"
                test-on-return="true"
                test-while-idle="true"
        />
    </datasource>''', '')
        Datasource datasource = config.getDatasourceMap()?.testinlinejdbc2
        assert datasource
        datasource.getInlineJdbc().with {
            assert getTestOnBorrow()
            assert getTestOnCreate()
            assert getTestOnReturn()
            assert getTestWhileIdle()
        }
    }

    @Test
    void testLoadInLineJdbcFromXmlWithOverrideValue() {
        EntityConfig config = mockXmlAndHoconCgf('''
    <datasource name="testinlinejdbcoveride">
            <inline-jdbc
                idle-maxsize="999"
                jdbc-driver="empty"
                jdbc-password="empty"
                jdbc-password-lookup="empty"
                jdbc-uri="empty"
                jdbc-username="empty"
                pool-deadlock-maxwait="999"
                pool-deadlock-retrywait="999"
                pool-jdbc-test-stmt="empty"
                pool-lifetime="999"
                pool-maxsize="999"
                pool-minsize="999"
                pool-sleeptime="999"
                soft-min-evictable-idle-time-millis="999"
                test-on-borrow="false"
                test-on-create="false"
                test-on-return="false"
                test-while-idle="false"
                time-between-eviction-runs-millis="999"
        />
    </datasource>''', '''"datasource" : {
        "testinlinejdbcoveride": {
            "inline-jdbc": {
                "idle-maxsize": "91"
                "jdbc-driver": "jdbc-driver-over"
                "jdbc-password": "jdbc-password-over"
                "jdbc-password-lookup": "jdbc-password-lookup-over"
                "jdbc-uri": "jdbc-uri-over"
                "jdbc-username": "jdbc-username-over"
                "pool-deadlock-maxwait": "92"
                "pool-deadlock-retrywait": "93"
                "pool-jdbc-test-stmt": "pool-jdbc-test-stmt-over"
                "pool-lifetime": "94"
                "pool-maxsize": "95"
                "pool-minsize": "96"
                "pool-sleeptime": "97"
                "soft-min-evictable-idle-time-millis": "98"
                "test-on-borrow": "true"
                "test-on-create": "true"
                "test-on-return": "true"
                "test-while-idle": "true"
                "time-between-eviction-runs-millis": "99"
            }
        }
    }''')
        Datasource datasource = config.getDatasourceMap()?.testinlinejdbcoveride
        assert datasource
        datasource.getInlineJdbc().with {
            assert getIdleMaxsize() == 91
            assert getJdbcDriver() == 'jdbc-driver-over'
            assert getJdbcPassword() == 'jdbc-password-over'
            assert getJdbcPasswordLookup() == 'jdbc-password-lookup-over'
            assert getJdbcUri() == 'jdbc-uri-over'
            assert getJdbcUsername() == 'jdbc-username-over'
            assert getPoolDeadlockMaxwait() == 92
            assert getPoolDeadlockRetrywait() == 93
            assert getPoolJdbcTestStmt() == 'pool-jdbc-test-stmt-over'
            assert getPoolLifetime() == 94
            assert getPoolMaxsize() == 95
            assert getPoolMinsize() == 96
            assert getPoolSleeptime() == 97
            assert getSoftMinEvictableIdleTimeMillis() == 98
            assert getTestOnBorrow()
            assert getTestOnCreate()
            assert getTestOnReturn()
            assert getTestWhileIdle()
            assert getTimeBetweenEvictionRunsMillis() == 99
        }
    }

    @Test
    void testCreateDatasourceFromConfigWithoutOverride() {
        EntityConfig config = mockXmlAndHoconCgf(''' <!-- empty xml datasource--> ''',
                '''"datasource" : {
        "datasourcewithnoxmlequivalent": {
            // base params
            "add-missing-on-start": "false",
            "alias-view-columns": "false",
            "always-use-constraint-keyword": "false",
            "character-set": "character-set-overridetest",
            "check-fks-indices-on-start": "false",
            "check-fks-on-start": "false",
            "check-indices-on-start": "false",
            "check-on-start": "false",
            "check-pks-on-start": "false",
            "collate": "collate-overridetest",
            "constraint-name-clip-length": "777",
            "drop-fk-use-foreign-key-keyword": "false",
            "field-type-name": "field-type-overridetest",
            "fk-style": "fk-style-overridetest",
            "helper-class": "org.apache.ofbiz.entity.datasource.GenericTest",
            "join-style": "join-overridetest",
            "max-worker-pool-size": "777",
            "offset-style": "offset-style-overridetest",
            "proxy-cursor-name": "proxy-overridetest",
            "result-fetch-size": "77",
            "schema-name": "schema-overridetest",
            "table-type": "table-overridetest",
            "use-binary-type-for-blob": "false",
            "use-fk-initially-deferred": "false",
            "use-foreign-key-indices": "false",
            "use-foreign-keys": "false",
            "use-indices": "false",
            "use-indices-unique": "false",
            "use-order-by-nulls": "false",
            "use-pk-constraint-names": "false",
            "use-proxy-cursor": "false",
            "use-schemas": "false",
            // inline jdbc
            "inline-jdbc": {
                "jdbc-driver": "postgresnew-driver",
                "jdbc-password": "postgresnew-jdbc-password",
                "jdbc-uri": "postgresnew-jdbc-uri",
                "jdbc-username": "postgresnew-jdbc-username",
                "jdbc-password-lookup": "postgresnew-jdbc-password-lookup",
                "pool-jdbc-test-stmt": "postgresnew-pool-jdbc-test-stmt",
                "idle-maxsize": "91",
                "pool-deadlock-maxwait": "92",
                "pool-deadlock-retrywait": "93",
                "pool-lifetime": "94",
                "pool-maxsize": "95",
                "pool-minsize": "96",
                "pool-sleeptime": "97",
                "soft-min-evictable-idle-time-millis": "98",
                "time-between-eviction-runs-millis": "99",
                "test-on-borrow": "true",
                "test-on-create": "true",
                "test-on-return": "true",
                "test-while-idle": "true"
            }
        }
    }''')
        Datasource datasource = config.getDatasourceMap()?.datasourcewithnoxmlequivalent
        assert datasource
        datasource.with {
            assert getName() == 'datasourcewithnoxmlequivalent'
            assert !getAddMissingOnStart()
            assert !getAliasViewColumns()
            assert !getAlwaysUseConstraintKeyword()
            assert getCharacterSet() == 'character-set-overridetest'
            assert !getCheckFkIndicesOnStart()
            assert !getCheckFksOnStart()
            assert !getCheckIndicesOnStart()
            assert !getCheckOnStart()
            assert !getCheckPksOnStart()
            assert getCollate() == 'collate-overridetest'
            assert getConstraintNameClipLength() == 777
            assert !getDropFkUseForeignKeyKeyword()
            assert getFieldTypeName() == 'field-type-overridetest'
            assert getFkStyle() == 'fk-style-overridetest'
            assert getHelperClass() == 'org.apache.ofbiz.entity.datasource.GenericTest'
            assert getJoinStyle() == 'join-overridetest'
            assert getMaxWorkerPoolSize() == 777
            assert getOffsetStyle() == 'offset-style-overridetest'
            assert getProxyCursorName() == 'proxy-overridetest'
            assert getResultFetchSize() == 77
            assert getSchemaName() == 'schema-overridetest'
            assert getTableType() == 'table-overridetest'
            assert !getUseBinaryTypeForBlob()
            assert !getUseFkInitiallyDeferred()
            assert !getUseForeignKeyIndices()
            assert !getUseForeignKeys()
            assert !getUseIndices()
            assert !getUseIndicesUnique()
            assert !getUseOrderByNulls()
            assert !getUsePkConstraintNames()
            assert !getUseProxyCursor()
            assert !getUseSchemas()
        }
        datasource.getInlineJdbc().with {
            assert getJdbcDriver() == 'postgresnew-driver'
            assert getJdbcPassword() == 'postgresnew-jdbc-password'
            assert getJdbcPasswordLookup() == 'postgresnew-jdbc-password-lookup'
            assert getJdbcUri() == 'postgresnew-jdbc-uri'
            assert getJdbcUsername() == 'postgresnew-jdbc-username'
            assert getPoolJdbcTestStmt() == 'postgresnew-pool-jdbc-test-stmt'
            assert getIdleMaxsize() == 91
            assert getPoolDeadlockMaxwait() == 92
            assert getPoolDeadlockRetrywait() == 93
            assert getPoolLifetime() == 94
            assert getPoolMaxsize() == 95
            assert getPoolMinsize() == 96
            assert getPoolSleeptime() == 97
            assert getSoftMinEvictableIdleTimeMillis() == 98
            assert getTimeBetweenEvictionRunsMillis() == 99
            assert getTestOnBorrow()
            assert getTestOnCreate()
            assert getTestOnReturn()
            assert getTestWhileIdle()
        }
    }

    @Test
    void testCreateDatasourceFromConfigWithoutOverrideWithReadDataList() {
        EntityConfig config = mockXmlAndHoconCgf(''' <!-- empty xml datasource--> ''',
                '''"datasource" : {
        "datasourcewithreaddata": {
            "read-data": [
                { "reader-name": "tenant" },
                { "reader-name": "seed" },
                { "reader-name": "seed-initial" },
            ]
            "inline-jdbc": {
                "jdbc-driver": "postgresnew-driver",
                "jdbc-password": "postgresnew-jdbc-password",
                "jdbc-uri": "postgresnew-jdbc-uri",
                "jdbc-username": "postgresnew-jdbc-username",
            }
        }
    }''')
        Datasource datasource = config.getDatasourceMap()?.datasourcewithreaddata
        assert datasource
        datasource.with {
            assert ['tenant', 'seed', 'seed-initial'] == getReadDataList()*.readerName
            assert getReadDataList()?.size() == 3
        }
        datasource.getInlineJdbc().with {
            assert getJdbcDriver() == 'postgresnew-driver'
            assert getJdbcPassword() == 'postgresnew-jdbc-password'
            assert getJdbcUri() == 'postgresnew-jdbc-uri'
            assert getJdbcUsername() == 'postgresnew-jdbc-username'
        }
    }

    @Test
    void testLoadDatatasourceWithReadDataWithXmlAndConfigs() {
        EntityConfig config = mockXmlAndHoconCgf('''
    <datasource name="datasourcetestldwrdwxac">
        <read-data reader-name="reader-test-xml1"/>
        <read-data reader-name="reader-test-xml2"/>
    </datasource>''',
                    '''"datasource" : {
        "datasourcetestldwrdwxac": {
            "read-data" : [
                {"reader-name":"over-reader1"},
                {"reader-name":"over-reader2"}
            ]
        }
    }
''')
        Map<String, Datasource> datasources = config.getDatasourceMap()
        Datasource datasource = datasources.datasourcetestldwrdwxac

        assert datasource
        datasource.with {
            assert ['over-reader1', 'over-reader2'] == getReadDataList()*.readerName
            assert getReadDataList().size() == 2
        }
    }

}
