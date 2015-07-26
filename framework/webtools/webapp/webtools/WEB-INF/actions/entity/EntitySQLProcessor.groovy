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

import org.ofbiz.entity.jdbc.SQLProcessor;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import org.ofbiz.entity.*;
import org.ofbiz.entity.model.ModelGroupReader;

resultMessage = "";
rs = null;
columns = [];
records = [];
mgr = delegator.getModelGroupReader();
groups = [];
for (String group : mgr.getGroupNames(delegator.getDelegatorName())) groups.add(0,["group":group]); //use for list-option in widget drop-down

if (sqlCommand && selGroup) {
    du = new SQLProcessor(delegator, delegator.getGroupHelperInfo(selGroup));
    try {
        if (sqlCommand.toUpperCase().startsWith("SELECT")) {
            rs = du.executeQuery(sqlCommand);
            if (rs) {
                rsmd = rs.getMetaData();
                numberOfColumns = rsmd.getColumnCount();
                for (i = 1; i <= numberOfColumns; i++) {
                    columns.add(rsmd.getColumnName(i));
                }
                rowLimitReached = false;
                while (rs.next()) {
                    if (records.size() >= rowLimit) {
                        resultMessage = "Returned top $rowLimit rows.";
                        rowLimitReached = true;
                        break;
                    }
                    record = [];
                    for (i = 1; i <= numberOfColumns; i++) {
                        record.add(rs.getObject(i));
                    }
                    records.add(record);
                }
                resultMessage = "Returned " + (rowLimitReached? "top " + rowLimit : "" + records.size()) + " rows.";
                rs.close();
            }
        } else {
            du.prepareStatement(sqlCommand);
            numOfAffectedRows = du.executeUpdate();
            resultMessage = "Affected $numOfAffectedRows rows.";
        }
    } catch (Exception exc) {
        resultMessage = exc.getMessage();
    }
}
context.groups = groups;
context.resultMessage = resultMessage;
context.columns = columns;
context.records = records;
