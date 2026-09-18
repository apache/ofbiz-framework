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

import static org.junit.jupiter.api.Assertions.assertThrows

import org.apache.ofbiz.service.ServiceErrorException
import org.apache.ofbiz.service.engine.EntityDeleteBuilder
import org.apache.ofbiz.testtools.JunitJupiterTest
import org.apache.ofbiz.testtools.JupiterTestHelper
import org.junit.jupiter.api.Test

/**
 * First time : ./gradlew 'ofbiz  -l readers=seed,seed-initial -l delegator=test'
 * ./gradlew 'ofbiz -t component=service -t suitename=servicetests -t case=engine-entity-delete-builder-tests'
 */
@JunitJupiterTest
class EntityDeleteBuilderTest implements JupiterTestHelper {

    @Test
    void testWhereFiltersRawMapToEntityFieldsBeforeDelete() {
        String keepId = 'EDB' + UUID.randomUUID().toString().replace('-', '').take(16)
        String deleteId = 'EDB' + UUID.randomUUID().toString().replace('-', '').take(16)
        delegator.create('TestingType', [testingTypeId: keepId, description: 'Keep'])
        delegator.create('TestingType', [testingTypeId: deleteId, description: 'Delete me'])

        // A raw service parameters map: the PK field, a real non-PK field (its actual stored value,
        // since non-PK fields legitimately participate in the delete condition -- unlike update()'s
        // set(), a delete()'s where() is a bulk-match, not a find-then-modify), and non-entity keys
        // (userLogin, locale) that must be silently ignored rather than blowing up on a nonexistent
        // column.
        Map rawParameters = [testingTypeId: deleteId, description: 'Delete me', userLogin: 'system', locale: 'en_US']

        int rowsDeleted = new EntityDeleteBuilder(delegator, 'TestingType').where(rawParameters)

        assert rowsDeleted == 1
        assert from('TestingType').where('testingTypeId', deleteId).queryOne() == null
        assert from('TestingType').where('testingTypeId', keepId).queryOne() != null
    }

    @Test
    void testWhereDeletesByNonPrimaryKeyFieldAlone() {
        // The whole point of filtering to "all entity fields" rather than "PK fields only": bulk
        // delete by a real, non-PK field (e.g. a foreign key shared by several rows) is a legitimate,
        // already-shipped pattern elsewhere in this codebase (e.g. QuoteAdjustment deleted by
        // quoteId+quoteItemSeqId, neither of which is its actual PK, quoteAdjustmentId). PK-only
        // filtering would make this always throw instead.
        String sharedDescription = 'EDB' + UUID.randomUUID().toString().replace('-', '').take(16)
        String matchIdA = 'EDB' + UUID.randomUUID().toString().replace('-', '').take(16)
        String matchIdB = 'EDB' + UUID.randomUUID().toString().replace('-', '').take(16)
        String otherId = 'EDB' + UUID.randomUUID().toString().replace('-', '').take(16)
        delegator.create('TestingType', [testingTypeId: matchIdA, description: sharedDescription])
        delegator.create('TestingType', [testingTypeId: matchIdB, description: sharedDescription])
        delegator.create('TestingType', [testingTypeId: otherId, description: 'Unrelated'])

        int rowsDeleted = new EntityDeleteBuilder(delegator, 'TestingType').where([description: sharedDescription])

        assert rowsDeleted == 2
        assert from('TestingType').where('testingTypeId', matchIdA).queryOne() == null
        assert from('TestingType').where('testingTypeId', matchIdB).queryOne() == null
        assert from('TestingType').where('testingTypeId', otherId).queryOne() != null
    }

    @Test
    void testWhereThrowsWhenMapHasNoRealEntityFields() {
        String testingTypeId = 'EDB' + UUID.randomUUID().toString().replace('-', '').take(16)
        delegator.create('TestingType', [testingTypeId: testingTypeId, description: 'Should survive'])

        // None of these keys are real fields on TestingType -- like WorkEffortServicesScript.groovy's
        // real, pre-existing bug of reusing a map keyed by workEffortId against WorkEffortAssoc, which
        // has no such field. Must fail loudly rather than silently building a condition-less delete
        // that would remove every TestingType row.
        Map noRealFields = [userLogin: 'system', locale: 'en_US']

        ServiceErrorException exception = assertThrows(ServiceErrorException) {
            new EntityDeleteBuilder(delegator, 'TestingType').where(noRealFields)
        }
        assert exception.message.contains('TestingType')

        assert from('TestingType').where('testingTypeId', testingTypeId).queryOne() != null
    }

}
