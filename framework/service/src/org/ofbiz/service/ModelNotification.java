/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

package org.ofbiz.service;

import org.ofbiz.service.config.ServiceConfigUtil;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.Debug;

import java.util.List;
import java.util.Map;

import javolution.util.FastMap;

/**
 * ModelNotification
 */
public class ModelNotification {

    public static final String module = ModelNotification.class.getName();

    public String notificationGroupName;
    public String notificationEvent;
    public String notificationMode;

    public void callNotify(DispatchContext dctx, ModelService model, Map context, Map result) {
        String thisEvent = (String) result.get(ModelService.RESPONSE_MESSAGE);
        if (notificationEvent.equals(thisEvent)) {
            String notificationService = this.getService();
            if (notificationService != null) {
                try {
                    Map notifyContext = this.buildContext(context, result, model);
                    dctx.getDispatcher().runSync(getService(), notifyContext, 90, true);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                }
            }
        }
    }

    public Map buildContext(Map context, Map result, ModelService model) throws GenericServiceException {
        Map userLogin = (Map) context.get("userLogin");
        String partyId = null;
        if (userLogin != null) {
            partyId = (String) userLogin.get("partyId");
        }

        String screen = getScreen();
        if (screen == null) {
            throw new GenericServiceException("SCREEN is a required attribute; check serviceengine.xml group definition; cannot generate notification");
        }

        String subject = getSubject();
        String from = buildFrom();
        String bcc = buildBcc();
        String cc = buildCc();
        String to = buildTo();
        if (subject == null || from == null || to == null) {
            throw new GenericServiceException("TO, FROM and SUBJECT are required for notifications; check serviceengine.xml group definition");
        }

        // template context
        Map notifyContext = FastMap.newInstance();
        Map bodyParams = FastMap.newInstance();
        bodyParams.put("serviceContext", context);
        bodyParams.put("serviceResult", result);
        bodyParams.put("service", model);

        // notification context
        notifyContext.put("bodyParameters", bodyParams);

        notifyContext.put("sendFrom", from);
        notifyContext.put("sendBcc", bcc);
        notifyContext.put("sendCc", cc);
        notifyContext.put("sendTo", to);
        notifyContext.put("subject", subject);
        notifyContext.put("partyId", partyId);

        notifyContext.put("bodyScreenUri", screen);
        
        return notifyContext;
    }

    public String buildTo() {
        ServiceConfigUtil.NotificationGroup group = ServiceConfigUtil.getNotificationGroup(notificationGroupName);
        if (group != null) {
            List addr = group.getAddress("to");
            if (addr != null && addr.size() > 0) {
                return StringUtil.join(addr, ",");
            }
        }
        return null;
    }

    public String buildCc() {
        ServiceConfigUtil.NotificationGroup group = ServiceConfigUtil.getNotificationGroup(notificationGroupName);
        if (group != null) {
            List addr = group.getAddress("cc");
            if (addr != null) {
                return StringUtil.join(addr, ",");
            }
        }
        return null;
    }

    public String buildBcc() {
        ServiceConfigUtil.NotificationGroup group = ServiceConfigUtil.getNotificationGroup(notificationGroupName);
        if (group != null) {
            List addr = group.getAddress("bcc");
            if (addr != null && addr.size() > 0) {
                return StringUtil.join(addr, ",");
            }
        }
        return null;
    }

    public String buildFrom() {
        ServiceConfigUtil.NotificationGroup group = ServiceConfigUtil.getNotificationGroup(notificationGroupName);
        if (group != null) {
            List addr = group.getAddress("from");
            if (addr != null && addr.size() > 0) {
                return (String) addr.get(0);
            }
        }
        return null;
    }

    public String getSubject() {
        ServiceConfigUtil.NotificationGroup group = ServiceConfigUtil.getNotificationGroup(notificationGroupName);
        if (group != null) {
            return group.getSubject();
        }
        return null;
    }

    public String getScreen() {
        ServiceConfigUtil.NotificationGroup group = ServiceConfigUtil.getNotificationGroup(notificationGroupName);
        if (group != null) {
            return group.getScreen();
        }
        return null;
    }

    public String getService() {
        ServiceConfigUtil.NotificationGroup group = ServiceConfigUtil.getNotificationGroup(notificationGroupName);
        if (group != null) {
            // only service supported at this time
            return "sendMailFromScreen";
        }
        return null;
    }
}
