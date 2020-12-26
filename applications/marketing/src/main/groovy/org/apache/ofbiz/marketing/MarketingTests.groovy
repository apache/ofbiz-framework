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
package org.apache.ofbiz.marketing

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.service.ServiceUtil
import org.apache.ofbiz.service.testtools.OFBizTestCase

class MarketingTests extends OFBizTestCase {
    public MarketingTests(String name) {
        super(name)
    }

    void testCreateAndUpdateContactList() {
        /* Precondition:
        Create contact list
        1. Go to the Marketing Manager
        2. Select the Contact List menu item
        3. Click Create New Contact List
        4. Enter fields in contact list form

        Edit contact list
        1. Go to edit contact list screen
        2. Add / modify fields
        3. Click on save

        process tested by test case:

        1. This test the process for create and update contact list

        Post condition: Contact list should be created and updated successfully
        */

        //create contact list
        Map serviceCtx = [
                contactListTypeId: "ANNOUNCEMENT",
                contactListName: "Announcement List",
                contactMechTypeId: "EMAIL_ADDRESS",
                userLogin: userLogin
        ]
        Map serviceResult = dispatcher.runSync("createContactList", serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)
        String contactListId = serviceResult.contactListId

        GenericValue contactList = from("ContactList").where("contactListId", contactListId).queryOne()
        assert contactList != null
        assert contactList.contactMechTypeId == "EMAIL_ADDRESS"

        //update contact list
        serviceCtx = [
                contactListTypeId: "ANNOUNCEMENT",
                contactListName: "Announcement Records",
                contactMechTypeId: "POSTAL_ADDRESS",
                contactListId: contactListId,
                userLogin: userLogin
        ]
        serviceResult = dispatcher.runSync("updateContactList", serviceCtx)
        assert ServiceUtil.isSuccess(serviceResult)

        contactList.refresh()

        assert contactList != null
        assert contactList.contactMechTypeId == "POSTAL_ADDRESS"
    }

}