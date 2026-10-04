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
package org.apache.ofbiz.service.config.test

import org.apache.ofbiz.service.config.model.ServiceEngine
import org.junit.jupiter.api.Test

class ConfigReaderNotificationGroup extends BaseServiceConfigReaderTest {

    @Test
    void testNotificationGroupFromXml() {
        ServiceEngine engine = getConfig('''
        <notification-group name="test">
            <notification subject="Subject Test"
                          service="service-test"
                          screen="screen-test"/>
            <notify type="from">from@test.com</notify>
            <notify type="to">to@test.com</notify>
            <notify type="cc">cc@test.com</notify>
            <notify type="bcc">bcc@test.com</notify>
        </notification-group>''', '')
        List notificationGroups = engine.getNotificationGroups()
        assert notificationGroups?.size() == 1
        notificationGroups.first().with {
            assert getNotification()?.getSubject() == 'Subject Test'
            assert getNotification()?.getService() == 'service-test'
            assert getNotification()?.getScreen() == 'screen-test'
            List notifies = getNotifyList()
            assert notifies.size() == 4
            ['to', 'from', 'cc', 'bcc'].each { type ->
                assert notifies.find { it.getType() == type }?.getContent() == "$type@test.com"
            }
        }
    }

    @Test
    void testNotificationGroupFromOverride() {
        ServiceEngine engine = getConfig('''
        <notification-group name="test">
            <notification subject="Subject Test"
                          service="service-test"
                          screen="screen-test"/>
            <notify type="from">from@test.com</notify>
            <notify type="to">to@test.com</notify>
            <notify type="cc">cc@test.com</notify>
            <notify type="bcc">bcc@test.com</notify>
        </notification-group>''', '''
            "notification-group": {
                "test": {
                    "subject": "Subject Over"
                    "service": "service-over"
                    "screen": "screen-over"
                    "notify": {"type": "to"}
                     } }''')
        List notificationGroups = engine.getNotificationGroups()
        assert notificationGroups?.size() == 1
        /* FIXME
        notificationGroups.first().with {
            assert getNotification()?.getSubject() == 'Subject Over'
            assert getNotification()?.getService() == 'service-over'
            assert getNotification()?.getScreen() == 'screen-over'
        }
         */
    }

}
