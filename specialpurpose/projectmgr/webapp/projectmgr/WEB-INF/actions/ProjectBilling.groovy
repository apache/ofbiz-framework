/*
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
  
import org.ofbiz.entity.*;
import org.ofbiz.base.util.*;

projectId = parameters.projectId;
List entries = delegator.findByAnd("ProjectAndPhaseAndTask", ["projectId" : projectId ], ["lastModifiedDate DESC"]);
for(ind = 0; ind < entries.size(); ind++) {
	entryItems = entries[ind].getRelated("TimeEntry");
	if (entryItems && entryItems[0].invoiceId != null) {
		invoice = delegator.findByPrimaryKey("Invoice", ["invoiceId" : entryItems[0].invoiceId]);
		if (invoice.getString("statusId").equals("INVOICE_IN_PROCESS")) {
			context.partyIdFrom = invoice.partyIdFrom;
			context.partyId = invoice.partyId;
			context.invoiceId = entryItems[0].invoiceId; 
		}
		break;
	}
}

//start of this month
context.thruDate = UtilDateTime.getMonthStart(UtilDateTime.nowTimestamp()); 