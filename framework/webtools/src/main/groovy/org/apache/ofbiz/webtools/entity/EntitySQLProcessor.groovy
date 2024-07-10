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
package org.apache.ofbiz.webtools.entity

import java.sql.ResultSet
import java.sql.ResultSetMetaData

import org.apache.ofbiz.entity.jdbc.SQLProcessor
import org.apache.ofbiz.entity.model.ModelGroupReader

if (!security.hasPermission('ENTITY_MAINT', userLogin)) {
    return
}

String sqlCommand = context.request.getParameter('sqlCommand') // (see OFBIZ-6567)

String resultMessage = ''
List<String> columns = []
List<List<Object>> records = []
ModelGroupReader mgr = delegator.getModelGroupReader()
List<Map<String,String>> groups = []
mgr.getGroupNames(delegator.getDelegatorName()).each { String group ->
    groups.add(0, ['group': group]) //use for list-option in widget drop-down
}

if (sqlCommand && selGroup) {
    try (SQLProcessor du = new SQLProcessor(delegator, delegator.getGroupHelperInfo(selGroup))) {
        if (sqlCommand.toUpperCase().startsWith('SELECT')) {
            try (ResultSet rs = du.executeQuery(sqlCommand)) {
                ResultSetMetaData rsmd = rs.getMetaData()

                int numberOfColumns = rsmd.getColumnCount()
                for (int i = 1; i <= numberOfColumns; i++) {
                    columns.add(rsmd.getColumnLabel(i))
                }

                boolean rowLimitReached = false
                while (rs.next()) {
                    if (records.size() >= rowLimit) {
                        rowLimitReached = true
                        break
                    }

                    List<Object> record = []
                    for (int i = 1; i <= numberOfColumns; i++) {
                        record.add(rs.getObject(i))
                    }
                    records.add(record)
                }

                resultMessage = "Returned ${rowLimitReached ? '' : 'top'} ${records.size() as String} rows."
            }
        } else {
            if (sqlCommand.toUpperCase().contains('SYSCS_UTIL.SYSCS_EXPORT_TABLE')
                    || sqlCommand.toUpperCase().contains('JSP')) {
                context.resultMessage = 'Not executed for security reason'
                context.groups = groups
                context.columns = columns
                context.records = records
                context.sqlCommand = sqlCommand
                return
            }

            du.prepareStatement(sqlCommand)
            numOfAffectedRows = du.executeUpdate()
            resultMessage = "Affected $numOfAffectedRows rows."
        }
    } catch (Exception exc) {
        resultMessage = exc.getMessage()
    }
}

context.groups = groups
context.resultMessage = resultMessage
context.columns = columns
context.records = records
context.sqlCommand = sqlCommand // (see OFBIZ-6567)
