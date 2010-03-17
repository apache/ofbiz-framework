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

SELECT
    a.*,
    b.* EXCLUDE (partyId, partyId),
    c.* EXCLUDE (partyId),
    d.roleTypeId,
    d.description AS roleDescription,
    SUM(a.partyId),
    FOO(a.partyId, 1) AS baz,
    (a.partyId || '-' || a.partyTypeId) AS one
FROM
    Party a LEFT JOIN Person b USING partyId
    LEFT JOIN PartyGroup c ON b.partyId = c.partyId
    JOIN PartyRole d ON c.partyId = d.partyId AND c.partyId = d.partyId
RELATION TYPE one TITLE MainA Person USING partyId
RELATION TITLE MainB Person USING partyId
RELATION TYPE one Person USING partyId
RELATION PartyGroup USING partyId
WHERE
    a.partyTypeId = 'PERSON'
    AND
    b.lastName LIKE ?lastName
    AND
    b.birthDate BETWEEN '1974-12-01' AND '1974-12-31'
    OR
    (
        b.partyId IN ('1', '2', '3', '4')
        AND
        b.gender = 'M'
    )
    
HAVING
    b.firstName LIKE '%foo%'
ORDER BY
    LOWER(lastName), firstName, birthDate DESC
OFFSET 5
LIMIT 10
;

INSERT INTO Party (partyId, partyTypeId, statusId) VALUES
    ('a', 'PERSON', 'PARTY_DISABLED'),
    (5, 'PARTY_GROUP', ?name);
INSERT INTO Person (partyId, firstName) SELECT partyId, (partyId || '-auto') AS firstName FROM Party WHERE partyId IN ('a', 'b');
UPDATE Person SET (lastName) = (('auto-' || partyId)) WHERE partyId IN ('a', 'b');
UPDATE Person SET (lastName, height, width) = (('auto-' || partyId), 5, 7) WHERE partyId IN ('a', 'b');
UPDATE Person SET lastName = ('auto-' || partyId), height = 6, width = 5, nickname = 'a' WHERE partyId IN ('a', 'b');
DELETE FROM Person WHERE partyId IN ('a', 'b');
DELETE FROM Party WHERE partyId IN ('a', 'b');
CREATE VIEW viewOne AS SELECT a.* FROM Party a;
/*
UPDATE Person SET firstName = partyId || '-auto' WHERE partyId IN ('a', 'b');
*/
