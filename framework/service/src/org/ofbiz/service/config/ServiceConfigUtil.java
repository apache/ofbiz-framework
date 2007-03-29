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
package org.ofbiz.service.config;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.io.Serializable;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.config.GenericConfigException;
import org.ofbiz.base.config.ResourceLoader;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilXml;
import org.ofbiz.base.util.cache.UtilCache;
import org.w3c.dom.Element;

/**
 * Misc. utility method for dealing with the serviceengine.xml file
 */
public class ServiceConfigUtil implements Serializable {
    
    public static final String module = ServiceConfigUtil.class.getName();
    public static final String engine = "default";
    public static final String SERVICE_ENGINE_XML_FILENAME = "serviceengine.xml";
    protected static UtilCache notificationGroupCache = new UtilCache("service.NotificationGroups", 0, 0, false);

    public static Element getXmlRootElement() throws GenericConfigException {
        Element root = ResourceLoader.getXmlRootElement(ServiceConfigUtil.SERVICE_ENGINE_XML_FILENAME);
        return UtilXml.firstChildElement(root, "service-engine"); // only look at the first one for now
    }

    public static Element getElement(String elementName) {
        Element rootElement = null;

        try {
            rootElement = ServiceConfigUtil.getXmlRootElement();
        } catch (GenericConfigException e) {
            Debug.logError(e, "Error getting Service Engine XML root element", module);
        }
        return  UtilXml.firstChildElement(rootElement, elementName);       
    }
    
    public static String getElementAttr(String elementName, String attrName) {        
        Element element = getElement(elementName);

        if (element == null) return null;
        return element.getAttribute(attrName);
    }

    public static String getSendPool() {
        return getElementAttr("thread-pool", "send-to-pool");        
    }
    
    public static List getRunPools() {
        List readPools = null;
        
        Element threadPool = getElement("thread-pool");
        List readPoolElements = UtilXml.childElementList(threadPool, "run-from-pool");
        if (readPoolElements != null) {
            readPools = FastList.newInstance();        
            Iterator i = readPoolElements.iterator();
        
            while (i.hasNext()) {                
                Element e = (Element) i.next();
                readPools.add(e.getAttribute("name"));
            }
        }
        return readPools;
    }
    
    public static int getPurgeJobDays() {
        String days = getElementAttr("thread-pool", "purge-job-days");
        int purgeDays;
        try {
            purgeDays = Integer.parseInt(days);
        } catch (NumberFormatException e) {
            Debug.logError(e, "Cannot read the number of days to keep jobs; not purging", module);
            purgeDays = 0;
        }
        return purgeDays;
    }

    public static int getFailedRetryMin() {
        String minString = getElementAttr("thread-pool", "failed-retry-min");
        int retryMin;
        try {
            retryMin = Integer.parseInt(minString);
        } catch (NumberFormatException e) {
            Debug.logError(e, "Unable to parse retry minutes; using default of 30", module);
            retryMin = 30;
        }
        return retryMin;
    }

    public static void readNotificationGroups() {
        Element rootElement = null;

        try {
            rootElement = ServiceConfigUtil.getXmlRootElement();
        } catch (GenericConfigException e) {
            Debug.logError(e, "Error getting Service Engine XML root element", module);
        }

        FastMap engineNotifyMap = FastMap.newInstance();

        List nGroups = UtilXml.childElementList(rootElement, "notification-group");
        Iterator i = nGroups.iterator();
        while (i.hasNext()) {
            Element e = (Element) i.next();
            NotificationGroup ng = new NotificationGroup(e);
            engineNotifyMap.put(ng.getName(), ng);
        }

        notificationGroupCache.put(engine, engineNotifyMap);
    }

    public static NotificationGroup getNotificationGroup(String group) {
        Map engineNotifyMap = (Map) notificationGroupCache.get(engine);
        if (engineNotifyMap == null) {
            synchronized(ServiceConfigUtil.class) {
                engineNotifyMap = (Map) notificationGroupCache.get(engine);
                if (engineNotifyMap == null) {
                    readNotificationGroups();
                }
            }
            engineNotifyMap = (Map) notificationGroupCache.get(engine);
        }
        if (engineNotifyMap != null) {
           return (NotificationGroup) engineNotifyMap.get(group);
        }

        return null;
    }
        
    public static class NotificationGroup implements Serializable {
        protected Notification notification;
        protected List notify;
        protected String name;

        protected NotificationGroup(Element e) {
            name = e.getAttribute("name");
            notify = FastList.newInstance();
            notification = new Notification(UtilXml.firstChildElement(e, "notification"));

            List n = UtilXml.childElementList(e, "notify");
            Iterator i = n.iterator();
            while (i.hasNext()) {
                Element e2 = (Element) i.next();
                notify.add(new Notify(e2));
            }
        }

        public String getName() {
            return name;
        }

        public Notification getNotification() {
            return notification;
        }

        public List getNotify() {
            return notify;
        }

        public String getService() {
            return notification.getService();
        }

        public String getSubject() {
            return notification.getSubject();
        }

        public String getScreen() {
            return notification.getScreen();
        }

        public List getAddress(String type) {
            List l = FastList.newInstance();
            Iterator i = notify.iterator();
            while (i.hasNext()) {
                Notify n = (Notify) i.next();
                if (n.getType().equals(type)) {
                    l.add(n.getValue());
                }
            }

            return l;
        }

        class Notification implements Serializable {
            protected String subject = null;
            protected String screen = null;
            protected String service = null;

            public Notification(Element e) {
                service = e.getAttribute("service");
                subject = e.getAttribute("subject");
                screen = e.getAttribute("screen");
            }

            public String getScreen() {
                return screen;
            }

            public String getSubject() {
                return subject;
            }

            public String getService() {
                return service;
            }
        }

        class Notify implements Serializable {
            protected String type;
            protected String value;

            public Notify(Element e) {
                type = e.getAttribute("type");
                value = UtilXml.elementValue(e);
            }

            public String getType() {
                return type;
            }
            public String getValue() {
                return value;
            }
        }
    }
}
