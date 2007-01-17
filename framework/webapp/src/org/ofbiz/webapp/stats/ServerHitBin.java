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
package org.ofbiz.webapp.stats;

import java.net.InetAddress;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.model.ModelEntity;

/**
 * <p>Counts server hits and tracks statistics for request, events and views
 * <p>Handles total stats since the server started and binned 
 *  stats according to settings in the serverstats.properties file.
 */
public class ServerHitBin {
    // Debug module name
    public static final String module = ServerHitBin.class.getName();

    public static final int REQUEST = 1;
    public static final int EVENT = 2;
    public static final int VIEW = 3;
    public static final int ENTITY = 4;
    public static final int SERVICE = 5;
    public static final String[] typeNames = {"", "Request", "Event", "View", "Entity", "Service"};
    public static final String[] typeIds = {"", "REQUEST", "EVENT", "VIEW", "ENTITY", "SERVICE"};

    public static void countRequest(String id, HttpServletRequest request, long startTime, long runningTime, GenericValue userLogin,
        GenericDelegator delegator) {
        countHit(id, REQUEST, request, startTime, runningTime, userLogin, delegator);
    }

    public static void countEvent(String id, HttpServletRequest request, long startTime, long runningTime, GenericValue userLogin,
        GenericDelegator delegator) {
        countHit(id, EVENT, request, startTime, runningTime, userLogin, delegator);
    }

    public static void countView(String id, HttpServletRequest request, long startTime, long runningTime, GenericValue userLogin,
        GenericDelegator delegator) {
        countHit(id, VIEW, request, startTime, runningTime, userLogin, delegator);
    }

    public static void countEntity(String id, HttpServletRequest request, long startTime, long runningTime, GenericValue userLogin,
        GenericDelegator delegator) {
        countHit(id, ENTITY, request, startTime, runningTime, userLogin, delegator);
    }

    public static void countService(String id, HttpServletRequest request, long startTime, long runningTime, GenericValue userLogin,
        GenericDelegator delegator) {
        countHit(id, SERVICE, request, startTime, runningTime, userLogin, delegator);
    }

    public static void countHit(String id, int type, HttpServletRequest request, long startTime, long runningTime, GenericValue userLogin,
        GenericDelegator delegator) {
        // only count hits if enabled, if not specified defaults to false
        if (!"true".equals(UtilProperties.getPropertyValue("serverstats", "stats.enable." + typeIds[type]))) return;
        countHit(id, type, request, startTime, runningTime, userLogin, delegator, true);
    }

    public static void advanceAllBins(long toTime) {
        advanceAllBins(toTime, requestHistory);
        advanceAllBins(toTime, eventHistory);
        advanceAllBins(toTime, viewHistory);
        advanceAllBins(toTime, entityHistory);
        advanceAllBins(toTime, serviceHistory);
    }

    static void advanceAllBins(long toTime, Map binMap) {
        Iterator entries = binMap.entrySet().iterator();

        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();

            if (entry.getValue() != null) {
                ServerHitBin bin = (ServerHitBin) entry.getValue();

                bin.advanceBin(toTime);
            }
        }
    }

    protected static void countHit(String id, int type, HttpServletRequest request, long startTime, long runningTime, GenericValue userLogin,
        GenericDelegator delegator, boolean isOriginal) {
        if (delegator == null) {
            throw new IllegalArgumentException("The delgator passed to countHit cannot be null");
        }

        ServerHitBin bin = null;
        LinkedList binList = null;

        switch (type) {
        case REQUEST:
            binList = (LinkedList) requestHistory.get(id);
            break;

        case EVENT:
            binList = (LinkedList) eventHistory.get(id);
            break;

        case VIEW:
            binList = (LinkedList) viewHistory.get(id);
            break;

        case ENTITY:
            binList = (LinkedList) entityHistory.get(id);
            break;

        case SERVICE:
            binList = (LinkedList) serviceHistory.get(id);
            break;
        }

        if (binList == null) {
            synchronized (ServerHitBin.class) {
                switch (type) {
                case REQUEST:
                    binList = (LinkedList) requestHistory.get(id);
                    break;

                case EVENT:
                    binList = (LinkedList) eventHistory.get(id);
                    break;

                case VIEW:
                    binList = (LinkedList) viewHistory.get(id);
                    break;

                case ENTITY:
                    binList = (LinkedList) entityHistory.get(id);
                    break;

                case SERVICE:
                    binList = (LinkedList) serviceHistory.get(id);
                    break;
                }
                if (binList == null) {
                    binList = new LinkedList();
                    switch (type) {
                    case REQUEST:
                        requestHistory.put(id, binList);
                        break;

                    case EVENT:
                        eventHistory.put(id, binList);
                        break;

                    case VIEW:
                        viewHistory.put(id, binList);
                        break;

                    case ENTITY:
                        entityHistory.put(id, binList);
                        break;

                    case SERVICE:
                        serviceHistory.put(id, binList);
                        break;
                    }
                }
            }
        }

        if (binList.size() > 0) {
            bin = (ServerHitBin) binList.getFirst();
        }
        if (bin == null) {
            synchronized (ServerHitBin.class) {
                if (binList.size() > 0) {
                    bin = (ServerHitBin) binList.getFirst();
                }
                if (bin == null) {
                    bin = new ServerHitBin(id, type, true, delegator);
                    binList.addFirst(bin);
                }
            }
        }

        bin.addHit(startTime, runningTime);
        if (isOriginal && !"GLOBAL".equals(id)) {
            bin.saveHit(request, startTime, runningTime, userLogin);
        }

        // count since start global and per id hits
        if (!"GLOBAL".equals(id))
            countHitSinceStart(id, type, startTime, runningTime, isOriginal, delegator);

        // also count hits up the hierarchy if the id contains a '.'
        if (id.indexOf('.') > 0) {
            countHit(id.substring(0, id.lastIndexOf('.')), type, request, startTime, runningTime, userLogin, delegator, false);
        }

        if (isOriginal && !"GLOBAL".equals(id))
            countHit("GLOBAL", type, request, startTime, runningTime, userLogin, delegator, true);
    }

    static void countHitSinceStart(String id, int type, long startTime, long runningTime, boolean isOriginal,
        GenericDelegator delegator) {
        if (delegator == null) {
            throw new IllegalArgumentException("The delgator passed to countHitSinceStart cannot be null");
        }

        ServerHitBin bin = null;

        // save in global, and try to get bin by id
        switch (type) {
        case REQUEST:
            bin = (ServerHitBin) requestSinceStarted.get(id);
            break;

        case EVENT:
            bin = (ServerHitBin) eventSinceStarted.get(id);
            break;

        case VIEW:
            bin = (ServerHitBin) viewSinceStarted.get(id);
            break;

        case ENTITY:
            bin = (ServerHitBin) entitySinceStarted.get(id);
            break;

        case SERVICE:
            bin = (ServerHitBin) serviceSinceStarted.get(id);
            break;
        }

        if (bin == null) {
            synchronized (ServerHitBin.class) {
                switch (type) {
                case REQUEST:
                    bin = (ServerHitBin) requestSinceStarted.get(id);
                    break;

                case EVENT:
                    bin = (ServerHitBin) eventSinceStarted.get(id);
                    break;

                case VIEW:
                    bin = (ServerHitBin) viewSinceStarted.get(id);
                    break;

                case ENTITY:
                    bin = (ServerHitBin) entitySinceStarted.get(id);
                    break;

                case SERVICE:
                    bin = (ServerHitBin) serviceSinceStarted.get(id);
                    break;
                }

                if (bin == null) {
                    bin = new ServerHitBin(id, type, false, delegator);
                    switch (type) {
                    case REQUEST:
                        requestSinceStarted.put(id, bin);
                        break;

                    case EVENT:
                        eventSinceStarted.put(id, bin);
                        break;

                    case VIEW:
                        viewSinceStarted.put(id, bin);
                        break;

                    case ENTITY:
                        entitySinceStarted.put(id, bin);
                        break;

                    case SERVICE:
                        serviceSinceStarted.put(id, bin);
                        break;
                    }
                }
            }
        }

        bin.addHit(startTime, runningTime);

        if (isOriginal)
            countHitSinceStart("GLOBAL", type, startTime, runningTime, false, delegator);
    }

    // these Maps contain Lists of ServerHitBin objects by id, the most recent is first in the list
    public static Map requestHistory = new HashMap();
    public static Map eventHistory = new HashMap();
    public static Map viewHistory = new HashMap();
    public static Map entityHistory = new HashMap();
    public static Map serviceHistory = new HashMap();

    // these Maps contain ServerHitBin objects by id
    public static Map requestSinceStarted = new HashMap();
    public static Map eventSinceStarted = new HashMap();
    public static Map viewSinceStarted = new HashMap();
    public static Map entitySinceStarted = new HashMap();
    public static Map serviceSinceStarted = new HashMap();

    GenericDelegator delegator;
    String delegatorName;
    String id;
    int type;
    boolean limitLength;
    long startTime;
    long endTime;
    long numberHits;
    long totalRunningTime;
    long minTime;
    long maxTime;

    public ServerHitBin(String id, int type, boolean limitLength, GenericDelegator delegator) {
        super();
        if (delegator == null) {
            throw new IllegalArgumentException("The delgator passed to countHitSinceStart cannot be null");
        }

        this.id = id;
        this.type = type;
        this.limitLength = limitLength;
        this.delegator = delegator;
        this.delegatorName = delegator.getDelegatorName();
        reset(getEvenStartingTime());
    }

    public GenericDelegator getDelegator() {
        if (this.delegator == null) {
            this.delegator = GenericDelegator.getGenericDelegator(this.delegatorName);
        }
        // if still null, then we have a problem
        if (this.delegator == null) {
            throw new IllegalArgumentException("Could not perform stats operation: could not find delegator with name: " + this.delegatorName);
        }
        return this.delegator;
    }

    long getEvenStartingTime() {
        // binLengths should be a divisable evenly into 1 hour
        long curTime = System.currentTimeMillis();
        long binLength = getNewBinLength();

        // find the first previous millis that are even on the hour
        Calendar cal = Calendar.getInstance();

        cal.setTime(new Date(curTime));
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        while (cal.getTime().getTime() < (curTime - binLength)) {
            cal.add(Calendar.MILLISECOND, (int) binLength);
        }

        return cal.getTime().getTime();
    }

    static long getNewBinLength() {
        long binLength = (long) UtilProperties.getPropertyNumber("serverstats", "stats.bin.length.millis");

        // if no or 0 binLength specified, set to 30 minutes
        if (binLength <= 0) binLength = 1800000;
        // if binLength is more than an hour, set it to one hour
        if (binLength > 3600000) binLength = 3600000;
        return binLength;
    }

    void reset(long startTime) {
        this.startTime = startTime;
        if (limitLength) {
            long binLength = getNewBinLength();

            // subtract 1 millisecond to keep bin starting times even
            this.endTime = startTime + binLength - 1;
        } else {
            this.endTime = 0;
        }
        this.numberHits = 0;
        this.totalRunningTime = 0;
        this.minTime = Long.MAX_VALUE;
        this.maxTime = 0;
    }

    ServerHitBin(ServerHitBin oldBin) {
        super();

        this.id = oldBin.id;
        this.type = oldBin.type;
        this.limitLength = oldBin.limitLength;
        this.delegator = oldBin.delegator;
        this.delegatorName = oldBin.delegatorName;
        this.startTime = oldBin.startTime;
        this.endTime = oldBin.endTime;
        this.numberHits = oldBin.numberHits;
        this.totalRunningTime = oldBin.totalRunningTime;
        this.minTime = oldBin.minTime;
        this.maxTime = oldBin.maxTime;
    }

    public String getId() {
        return this.id;
    }

    public int getType() {
        return this.type;
    }

    public String getTypeString() {
        return typeNames[this.type];
    }

    /** returns the startTime of the bin */
    public long getStartTime() {
        return this.startTime;
    }

    /** Returns the end time if the length of the bin is limited, otherwise returns the current system time */
    public long getEndTime() {
        return limitLength ? this.endTime : System.currentTimeMillis();
    }

    /** returns the startTime of the bin */
    public String getStartTimeString() {
        // using Timestamp toString because I like the way it formats it
        return new java.sql.Timestamp(this.getStartTime()).toString();
    }

    /** Returns the end time if the length of the bin is limited, otherwise returns the current system time */
    public String getEndTimeString() {
        return new java.sql.Timestamp(this.getEndTime()).toString();
    }

    /** returns endTime - startTime */
    public long getBinLength() {
        return this.getEndTime() - this.getStartTime();
    }

    /** returns (endTime - startTime)/60000 */
    public double getBinLengthMinutes() {
        return ((double) this.getBinLength()) / 60000.0;
    }

    public long getNumberHits() {
        return this.numberHits;
    }

    public long getTotalRunningTime() {
        return this.totalRunningTime;
    }

    public long getMinTime() {
        return this.minTime;
    }

    public double getMinTimeSeconds() {
        return ((double) this.minTime) / 1000.0;
    }

    public long getMaxTime() {
        return this.maxTime;
    }

    public double getMaxTimeSeconds() {
        return ((double) this.maxTime) / 1000.0;
    }

    public double getAvgTime() {
        return ((double) this.totalRunningTime) / ((double) this.numberHits);
    }

    public double getAvgTimeSeconds() {
        return this.getAvgTime() / 1000.0;
    }

    /** return the hits per minute using the entire length of the bin as returned by getBinLengthMinutes() */
    public double getHitsPerMinute() {
        return ((double) this.numberHits) / this.getBinLengthMinutes();
    }

    synchronized void addHit(long startTime, long runningTime) {
        advanceBin(startTime + runningTime);

        this.numberHits++;
        this.totalRunningTime += runningTime;
        if (runningTime < this.minTime)
            this.minTime = runningTime;
        if (runningTime > this.maxTime)
            this.maxTime = runningTime;
    }

    synchronized void advanceBin(long toTime) {
        // first check to see if this bin has expired, if so save and recycle it
        while (limitLength && toTime > this.endTime) {
            LinkedList binList = null;

            switch (type) {
            case REQUEST:
                binList = (LinkedList) requestHistory.get(id);
                break;

            case EVENT:
                binList = (LinkedList) eventHistory.get(id);
                break;

            case VIEW:
                binList = (LinkedList) viewHistory.get(id);
                break;

            case ENTITY:
                binList = (LinkedList) entityHistory.get(id);
                break;

            case SERVICE:
                binList = (LinkedList) serviceHistory.get(id);
                break;
            }

            // the first in the list will be this object, remove and copy it, 
            // put the copy at the first of the list, then put this object back on
            binList.removeFirst();
            if (this.numberHits > 0) {
                binList.addFirst(new ServerHitBin(this));

                // persist each bin when time ends if option turned on
                if (UtilProperties.propertyValueEqualsIgnoreCase("serverstats", "stats.persist." + ServerHitBin.typeIds[type] + ".bin", "true")) {
                    GenericValue serverHitBin = delegator.makeValue("ServerHitBin", null);
                    serverHitBin.set("serverHitBinId", getDelegator().getNextSeqId("ServerHitBin"));
                    serverHitBin.set("contentId", this.id);
                    serverHitBin.set("hitTypeId", ServerHitBin.typeIds[this.type]);
                    serverHitBin.set("binStartDateTime", new java.sql.Timestamp(this.startTime));
                    serverHitBin.set("binEndDateTime", new java.sql.Timestamp(this.endTime));
                    serverHitBin.set("numberHits", new Long(this.numberHits));
                    serverHitBin.set("totalTimeMillis", new Long(this.totalRunningTime));
                    serverHitBin.set("minTimeMillis", new Long(this.minTime));
                    serverHitBin.set("maxTimeMillis", new Long(this.maxTime));
                    // get localhost ip address and hostname to store
                    try {
                        InetAddress address = InetAddress.getLocalHost();

                        if (address != null) {
                            serverHitBin.set("serverIpAddress", address.getHostAddress());
                            serverHitBin.set("serverHostName", address.getHostName());
                        } else {
                            Debug.logError("Unable to get localhost internet address, was null", module);
                        }
                    } catch (java.net.UnknownHostException e) {
                        Debug.logError("Unable to get localhost internet address: " + e.toString(), module);
                    }
                    try {
                        serverHitBin.create();
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Could not save ServerHitBin:", module);
                    }
                }
            }
            this.reset(this.endTime + 1);
            binList.addFirst(this);
        }
    }

    void saveHit(HttpServletRequest request, long startTime, long runningTime, GenericValue userLogin) {
        // persist record of hit in ServerHit entity if option turned on
        if (UtilProperties.propertyValueEqualsIgnoreCase("serverstats", "stats.persist." + ServerHitBin.typeIds[type] + ".hit", "true")) {
            // if the hit type is ENTITY and the name contains "ServerHit" don't 
            // persist; avoids the infinite loop and a bunch of annoying data
            if (this.type == ENTITY && this.id.indexOf("ServerHit") > 0) {
                return;
            }

            // check for type data before running.
            GenericValue serverHitType = null;

            try {
                serverHitType = delegator.findByPrimaryKeyCache("ServerHitType", UtilMisc.toMap("hitTypeId", ServerHitBin.typeIds[this.type]));
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
            }
            if (serverHitType == null) {
                // datamodel data not loaded; not storing hit.
                Debug.logWarning("The datamodel data has not been loaded; cannot find hitTypeId '" + ServerHitBin.typeIds[this.type] + " not storing ServerHit.", module);
                return;
            }

            String visitId = VisitHandler.getVisitId(request.getSession());

            if (visitId == null || visitId.length() == 0) {
                // no visit info stored, so don't store the ServerHit
                Debug.logWarning("Could not find a visitId, so not storing ServerHit. This is probably a configuration error. If you turn of persistance of visits you should also turn off persistence of hits.", module);
                return;
            }

            GenericValue serverHit = delegator.makeValue("ServerHit", null);

            serverHit.set("visitId", visitId);
            serverHit.set("hitStartDateTime", new java.sql.Timestamp(startTime));
            serverHit.set("hitTypeId", ServerHitBin.typeIds[this.type]);
            if (userLogin != null) {
                serverHit.set("userLoginId", userLogin.get("userLoginId"));
                ModelEntity modelUserLogin = userLogin.getModelEntity();
                if (modelUserLogin.isField("partyId")) {
                    serverHit.set("partyId", userLogin.get("partyId"));
                }
            }
            serverHit.set("contentId", this.id);
            serverHit.set("runningTimeMillis", new Long(runningTime));

            String fullRequestUrl = UtilHttp.getFullRequestUrl(request).toString();

            serverHit.set("requestUrl", fullRequestUrl.length() > 250 ? fullRequestUrl.substring(0, 250) : fullRequestUrl);
            String referrerUrl = request.getHeader("Referer") != null ? request.getHeader("Referer") : "";

            serverHit.set("referrerUrl", referrerUrl.length() > 250 ? referrerUrl.substring(0, 250) : referrerUrl);

            // get localhost ip address and hostname to store
            try {
                InetAddress address = InetAddress.getLocalHost();

                if (address != null) {
                    serverHit.set("serverIpAddress", address.getHostAddress());
                    serverHit.set("serverHostName", address.getHostName());
                } else {
                    Debug.logError("Unable to get localhost internet address, was null", module);
                }
            } catch (java.net.UnknownHostException e) {
                Debug.logError("Unable to get localhost internet address: " + e.toString(), module);
            }

            try {
                serverHit.create();
            } catch (GenericEntityException e) {
                Debug.logError(e, "Could not save ServerHit:", module);
            }
        }
    }
}
