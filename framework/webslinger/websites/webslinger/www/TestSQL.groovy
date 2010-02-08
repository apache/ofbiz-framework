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
import java.util.concurrent.Callable
import org.ofbiz.entity.sql.SQLUtil
import org.ofbiz.entity.transaction.TransactionUtil
response.contentType = 'text/html'
def delegator = request.delegator

/*
def ec1 = SQLUtil.parseCondition("partyId = 'foo' AND partyTypeId = 'PARTY_GROUP' OR sequenceNum IN (1, 2) or foo BETWEEN 'a' and 'b'")
println("ec1=$ec1")
response.writer.println("ec1=$ec1<br />")
def ec2 = SQLUtil.parseCondition(ec1.toString())
println("ec2=$ec2")
response.writer.println("ec2=$ec2<br />")
//return
*/

def sql = """
SELECT
    a.partyId,
    a.partyTypeId AS type,
    COALESCE(b.firstName, '') AS firstName,
    COALESCE(b.lastName, '') AS lastName,
    COALESCE(c.groupName, '') AS groupName
FROM
	Party a LEFT JOIN Person b USING partyId LEFT JOIN PartyGroup c USING partyId
RELATION TYPE one Party USING partyId
WHERE
    partyId = ?partyId
;
"""
def sqlSelect = SQLUtil.parseSelect(sql)

TransactionUtil.doNewTransaction("Test", [call: {
    def eli
    try {
        eli = sqlSelect.getEntityListIterator(delegator, [partyId: 'admin'])
        def gv;
        while ((gv = eli.next()) != null) {
            response.writer.println("gv=$gv<br />")
            def party = gv.getRelatedOneCache('Party'); response.writer.println("\tparty=$party<br />")
            //def person = gv.getRelatedOneCache('Person'); response.writer.println("\tperson=$person<br />")
        }
    } finally {
        if (eli != null) eli.close()
    }
}] as Callable)
