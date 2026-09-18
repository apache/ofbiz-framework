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
 */
package org.apache.ofbiz.service.test.engine

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.engine.EntityUpdateBuilder
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.junit.jupiter.api.Test

/**
 * First time : ./gradlew 'ofbiz  -l readers=seed,seed-initial -l delegator=test'
 * ./gradlew 'ofbiz -t component=service -t suitename=servicetests -t case=engine-entity-update-builder-tests'
 */
@JunitJupiterTest
class EntityUpdateBuilderTest implements JupiterTestHelper {

    @Test
    void testFirstFiltersRawMapToEntityFieldsBeforeQuery() {
        // A fresh id per run, not a fixed literal: this test writes via delegator.create() directly
        // (not through a service), so it isn't reliably covered by the suite's transactional rollback
        // -- a fixed id would collide with a prior run's leftover row.
        String testingTypeId = 'EUB' + UUID.randomUUID().toString().replace('-', '').take(16)
        delegator.create('TestingType', [testingTypeId: testingTypeId, description: 'Original'])

        // A raw service parameters map -- like a Groovy service script's own `parameters` -- carries
        // keys (userLogin, locale) that are never real fields on TestingType, alongside the real
        // identifying field(s). queryOne() would narrow this to PK fields automatically; first()'s
        // queryFirst() must filter it to the entity's own field names itself, or building the query
        // condition on a nonexistent column blows up.
        Map rawParameters = [testingTypeId: testingTypeId, userLogin: 'system', locale: 'en_US']

        GenericValue updated = new EntityUpdateBuilder(delegator, 'TestingType')
                .where(rawParameters)
                .first()
                .set([description: 'Updated'])

        assert updated.description == 'Updated'
    }

}
