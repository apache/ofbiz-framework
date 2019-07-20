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

package org.apache.ofbiz.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.config.GenericConfigException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.StringUtil;
import org.apache.ofbiz.base.util.UtilGenerics;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.service.config.ServiceConfigUtil;
import org.apache.ofbiz.service.config.model.NotificationGroup;
import org.apache.ofbiz.service.config.model.Notify;

/**
 * ModelNotification
 */
public class ModelNotification {

    public static final String module = ModelNotification.class.getName();

    public String notificationGroupName;
    public String notificationEvent;
    public String notificationMode;

    public void callNotify(DispatchContext dctx, ModelService model, Map<String, ? extends Object> context, Map<String, Object> result) {
        String thisEvent = (String) result.get(ModelService.RESPONSE_MESSAGE);
        if (notificationEvent.equals(thisEvent)) {
            String notificationService = this.getService();
            if (notificationService != null) {
                try {
                    Map<String, Object> notifyContext = this.buildContext(context, result, model);
                    dctx.getDispatcher().runSync(getService(), notifyContext, 90, true);
                } catch (GenericServiceException e) {
                    Debug.logError(e, module);
                }
            }
        }
    }

    public Map<String, Object> buildContext(Map<String, ? extends Object> context, Map<String, Object> result, ModelService model) throws GenericServiceException {
        Map<String, Object> userLogin = UtilGenerics.cast(context.get("userLogin"));
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
        Map<String, Object> notifyContext = new HashMap<>();
        Map<String, Object> bodyParams = new HashMap<>();
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
        return getCommaSeparatedAddressList("to");
    }

    public String buildCc() {
        return getCommaSeparatedAddressList("cc");
    }

    public String buildBcc() {
        return getCommaSeparatedAddressList("bcc");
    }

    public String buildFrom() {
        return getCommaSeparatedAddressList("from");
    }

    private String getCommaSeparatedAddressList(String notifyType) {
        try {
            NotificationGroup group = getNotificationGroup(notificationGroupName);
            return getCommaSeparatedAddressList(group, notifyType);
        } catch (GenericConfigException e) {
            Debug.logWarning(e, "Exception thrown while getting service configuration: ", module);
            return null;
        }
    }

    private static String getCommaSeparatedAddressList(NotificationGroup notificationGroup, String notifyType) {
        if (notificationGroup != null) {
            List<String> addr = getAddressesByType(notificationGroup, notifyType);
            if (UtilValidate.isNotEmpty(addr)) {
                return StringUtil.join(addr, ",");
            }
        }
        return null;
    }

    private static List<String> getAddressesByType(NotificationGroup group, String type) {
        List<String> l = new ArrayList<>();
        for (Notify n : group.getNotifyList()) {
            if (n.getType().equals(type)) {
                l.add(n.getContent());
            }
        }
        return l;
    }

    public String getSubject() {
        try {
            NotificationGroup group = getNotificationGroup(notificationGroupName);
            if (group != null) {
                return group.getNotification().getSubject();
            }
        } catch (GenericConfigException e) {
            Debug.logWarning(e, "Exception thrown while getting service configuration: ", module);
        }
        return null;
    }

    public String getScreen() {
        try {
            NotificationGroup group = getNotificationGroup(notificationGroupName);
            if (group != null) {
                return group.getNotification().getScreen();
            }
        } catch (GenericConfigException e) {
            Debug.logWarning(e, "Exception thrown while getting service configuration: ", module);
        }
        return null;
    }

    public String getService() {
        try {
            NotificationGroup group = getNotificationGroup(notificationGroupName);
            if (group != null) {
                // only service supported at this time
                return "sendMailFromScreen";
            }
        } catch (GenericConfigException e) {
            Debug.logWarning(e, "Exception thrown while getting service configuration: ", module);
        }
        return null;
    }

    public static NotificationGroup getNotificationGroup(String group) throws GenericConfigException {
        List<NotificationGroup> notificationGroups;
        notificationGroups = ServiceConfigUtil.getServiceEngine().getNotificationGroups();
        for (NotificationGroup notificationGroup : notificationGroups) {
            if (notificationGroup.getName().equals(group)) {
                return notificationGroup;
            }
        }
        return null;
    }
}
