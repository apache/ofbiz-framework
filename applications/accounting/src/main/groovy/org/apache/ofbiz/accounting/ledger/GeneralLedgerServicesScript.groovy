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
package org.apache.ofbiz.accounting.ledger

import org.apache.ofbiz.entity.GenericValue

/**
 * Create a new Accounting Transaction (AcctgTrans) with isPosted set to 'N'.
 *
 * @param parameters Contains all attributes for service interface, interfaceAcctgTrans.
 * @return Success response containing the AcctgTrans primary key, error response otherwise.
 */
Map createAcctgTrans() {
    GenericValue newAcctgTrans = makeValue('AcctgTrans', [*:parameters, isPosted: 'N'])
    newAcctgTrans.acctgTransId = delegator.getNextSeqId('AcctgTrans')
    newAcctgTrans.create()

    return success([acctgTransId: newAcctgTrans.acctgTransId])
}
