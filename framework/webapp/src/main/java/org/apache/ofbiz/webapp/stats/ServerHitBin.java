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
package org.apache.ofbiz.webapp.stats;

import java.util.Date;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilHttp;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;

import com.ibm.icu.util.Calendar;

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

    private static final String[] typeIds = {"", "REQUEST", "EVENT", "VIEW", "ENTITY", "SERVICE"};

    // these Maps contain Lists of ServerHitBin objects by id, the most recent is first in the list
    public static final ConcurrentMap<String, Deque<ServerHitBin>> requestHistory = new ConcurrentHashMap<String, Deque<ServerHitBin>>();
    public static final ConcurrentMap<String, Deque<ServerHitBin>> eventHistory = new ConcurrentHashMap<String, Deque<ServerHitBin>>();
    public static final ConcurrentMap<String, Deque<ServerHitBin>> viewHistory = new ConcurrentHashMap<String, Deque<ServerHitBin>>();
    public static final ConcurrentMap<String, Deque<ServerHitBin>> entityHistory = new ConcurrentHashMap<String, Deque<ServerHitBin>>();
    public static final ConcurrentMap<String, Deque<ServerHitBin>> serviceHistory = new ConcurrentHashMap<String, Deque<ServerHitBin>>();

    // these Maps contain ServerHitBin objects by id
    public static final ConcurrentMap<String, ServerHitBin> requestSinceStarted = new ConcurrentHashMap<String, ServerHitBin>();
    public static final ConcurrentMap<String, ServerHitBin> eventSinceStarted = new ConcurrentHashMap<String, ServerHitBin>();
    public static final ConcurrentMap<String, ServerHitBin> viewSinceStarted = new ConcurrentHashMap<String, ServerHitBin>();
    public static final ConcurrentMap<String, ServerHitBin> entitySinceStarted = new ConcurrentHashMap<String, ServerHitBin>();
    public static final ConcurrentMap<String, ServerHitBin> serviceSinceStarted = new ConcurrentHashMap<String, ServerHitBin>();

    public static void countRequest(String id, HttpServletRequest request, long startTime, long runningTime, GenericValue userLogin) {
        countHit(id, REQUEST, request, startTime, runningTime, userLogin);
    }

    public static void countEvent(String id, HttpServletRequest request, long startTime, long runningTime, GenericValue userLogin) {
        countHit(id, EVENT, request, startTime, runningTime, userLogin);
    }

    public static void countView(String id, HttpServletRequest request, long startTime, long runningTime, GenericValue userLogin) {
        countHit(id, VIEW, request, startTime, runningTime, userLogin);
    }

    public static void countEntity(String id, HttpServletRequest request, long startTime, long runningTime, GenericValue userLogin) {
        countHit(id, ENTITY, request, startTime, runningTime, userLogin);
    }

    public static void countService(String id, HttpServletRequest request, long startTime, long runningTime, GenericValue userLogin) {
        countHit(id, SERVICE, request, startTime, runningTime, userLogin);
    }

    private static void countHit(String id, int type, HttpServletRequest request, long startTime, long runningTime, GenericValue userLogin) {
        // only count hits if enabled, if not specified defaults to false
        if (!"true".equals(UtilProperties.getPropertyValue("serverstats", "stats.enable." + typeIds[type]))) return;
        countHit(id, type, request, startTime, runningTime, userLogin, true);
    }

    private static String makeIdTenantAware(String id, Delegator delegator) {
        if (UtilValidate.isNotEmpty(delegator.getDelegatorTenantId())) {
            return id + "#" + delegator.getDelegatorTenantId();
        } else {
            return id;
        }
    }

    private static long getNewBinLength() {
        long binLength = (long) UtilProperties.getPropertyNumber("serverstats", "stats.bin.length.millis");

        // if no or 0 binLength specified, set to 30 minutes
        if (binLength <= 0) binLength = 1800000;
        // if binLength is more than an hour, set it to one hour
        if (binLength > 3600000) binLength = 3600000;
        return binLength;
    }

    private static long getEvenStartingTime(long binLength) {
        // binLengths should be a divisable evenly into 1 hour
        long curTime = System.currentTimeMillis();

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

    private static void countHit(String baseId, int type, HttpServletRequest request, long startTime, long runningTime, GenericValue userLogin, boolean isOriginal) {
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        if (delegator == null) {
            String delegatorName = (String) request.getSession().getAttribute("delegatorName");
            delegator = DelegatorFactory.getDelegator(delegatorName);
        }
        if (delegator == null) {
            throw new IllegalArgumentException("In countHit could not find a delegator or delegatorName to work from");
        }
        
        String id = makeIdTenantAware(baseId, delegator);

        ServerHitBin bin = null;
        Deque<ServerHitBin> binList = null;

        switch (type) {
        case REQUEST:
            binList = requestHistory.get(id);
            break;

        case EVENT:
            binList = eventHistory.get(id);
            break;

        case VIEW:
            binList = viewHistory.get(id);
            break;

        case ENTITY:
            binList = entityHistory.get(id);
            break;

        case SERVICE:
            binList = serviceHistory.get(id);
            break;
        }

        if (binList == null) {
            binList = new ConcurrentLinkedDeque<ServerHitBin>();
            Deque<ServerHitBin> listFromMap = null;
            switch (type) {
            case REQUEST:
                listFromMap = requestHistory.putIfAbsent(id, binList);
                break;

            case EVENT:
                listFromMap = eventHistory.putIfAbsent(id, binList);
                break;

            case VIEW:
                listFromMap = viewHistory.putIfAbsent(id, binList);
                break;

            case ENTITY:
                listFromMap = entityHistory.putIfAbsent(id, binList);
                break;

            case SERVICE:
                listFromMap = serviceHistory.putIfAbsent(id, binList);
                break;
            }
            binList = listFromMap != null ? listFromMap : binList;
        }

        do {
            bin = binList.peek();
            if (bin == null) {
                binList.addFirst(new ServerHitBin(id, type, true, delegator));
            }
        } while (bin == null);

        long toTime = startTime + runningTime;
        // advance the bin
        // first check to see if the bin has expired, if so save and recycle it
        while (bin.limitLength && toTime > bin.endTime) {
            // the first in the list will be this object, remove and copy it,
            // put the copy at the first of the list, then put this object back on
            if (bin.getNumberHits() > 0) {
                // persist each bin when time ends if option turned on
                if (EntityUtilProperties.propertyValueEqualsIgnoreCase("serverstats", "stats.persist." + ServerHitBin.typeIds[type] + ".bin", "true", delegator)) {
                    GenericValue serverHitBin = delegator.makeValue("ServerHitBin");
                    serverHitBin.set("contentId", bin.id);
                    serverHitBin.set("hitTypeId", ServerHitBin.typeIds[bin.type]);
                    serverHitBin.set("binStartDateTime", new java.sql.Timestamp(bin.startTime));
                    serverHitBin.set("binEndDateTime", new java.sql.Timestamp(bin.endTime));
                    serverHitBin.set("numberHits", Long.valueOf(bin.getNumberHits()));
                    serverHitBin.set("totalTimeMillis", Long.valueOf(bin.getTotalRunningTime()));
                    serverHitBin.set("minTimeMillis", Long.valueOf(bin.getMinTime()));
                    serverHitBin.set("maxTimeMillis", Long.valueOf(bin.getMaxTime()));
                    // get localhost ip address and hostname to store
                    if (VisitHandler.address != null) {
                        serverHitBin.set("serverIpAddress", VisitHandler.address.getHostAddress());
                        serverHitBin.set("serverHostName", VisitHandler.address.getHostName());
                    }
                    try {
                        delegator.createSetNextSeqId(serverHitBin);
                    } catch (GenericEntityException e) {
                        Debug.logError(e, "Could not save ServerHitBin:", module);
                    }
                }
            } else {
                binList.pollFirst();
            }
            bin = new ServerHitBin(bin, bin.endTime + 1);
            binList.addFirst(bin);
        }

        bin.addHit(runningTime);
        if (isOriginal) {
            try {
                bin.saveHit(request, startTime, runningTime, userLogin);
            } catch (GenericEntityException e) {
                Debug.logWarning("Error saving ServerHit: " + e.toString(), module);
            }
        }

        // count since start global and per id hits
        if (!id.startsWith("GLOBAL")) {
            countHitSinceStart(id, type, runningTime, delegator);
            if (isOriginal) {
                countHitSinceStart(makeIdTenantAware("GLOBAL", delegator), type, runningTime, delegator);
            }
        }

        // also count hits up the hierarchy if the id contains a '.'
        if (id.indexOf('.') > 0) {
            countHit(id.substring(0, id.lastIndexOf('.')), type, request, startTime, runningTime, userLogin, false);
        }

        if (isOriginal) {
            countHit("GLOBAL", type, request, startTime, runningTime, userLogin, false);
        }
    }

    private static void countHitSinceStart(String id, int type, long runningTime, Delegator delegator) {
        ServerHitBin bin = null;

        switch (type) {
        case REQUEST:
            bin = requestSinceStarted.get(id);
            break;

        case EVENT:
            bin = eventSinceStarted.get(id);
            break;

        case VIEW:
            bin = viewSinceStarted.get(id);
            break;

        case ENTITY:
            bin = entitySinceStarted.get(id);
            break;

        case SERVICE:
            bin = serviceSinceStarted.get(id);
            break;
        }

        if (bin == null) {
            bin = new ServerHitBin(id, type, false, delegator);
            ServerHitBin binFromMap = null;
            switch (type) {
            case REQUEST:
                binFromMap = requestSinceStarted.putIfAbsent(id, bin);
                break;

            case EVENT:
                binFromMap = eventSinceStarted.putIfAbsent(id, bin);
                break;

            case VIEW:
                binFromMap = viewSinceStarted.putIfAbsent(id, bin);
                break;

            case ENTITY:
                binFromMap = entitySinceStarted.putIfAbsent(id, bin);
                break;

            case SERVICE:
                binFromMap = serviceSinceStarted.putIfAbsent(id, bin);
                break;
            }
            bin = binFromMap != null ? binFromMap : bin;
        }
        bin.addHit(runningTime);
    }

    private final Delegator delegator;
    private final String id;
    private final int type;
    private final boolean limitLength;
    private final long binLength;
    private final long startTime;
    private final long endTime;

    private long numberHits;
    private long totalRunningTime;
    private long minTime;
    private long maxTime;

    private ServerHitBin(String id, int type, boolean limitLength, Delegator delegator) {
        this.id = id;
        this.type = type;
        this.limitLength = limitLength;
        this.delegator = delegator;
        this.binLength = getNewBinLength();
        this.startTime = getEvenStartingTime(this.binLength);
        if (this.limitLength) {
            // subtract 1 millisecond to keep bin starting times even
            this.endTime = this.startTime + this.binLength - 1;
        } else {
            this.endTime = 0;
        }
        this.numberHits = 0;
        this.totalRunningTime = 0;
        this.minTime = Long.MAX_VALUE;
        this.maxTime = 0;
    }

    private ServerHitBin(ServerHitBin oldBin, long startTime) {
        this.id = oldBin.id;
        this.type = oldBin.type;
        this.limitLength = oldBin.limitLength;
        this.delegator = oldBin.delegator;
        this.binLength = oldBin.binLength;

        this.startTime = startTime;
        if (limitLength) {
            // subtract 1 millisecond to keep bin starting times even
            this.endTime = this.startTime + this.binLength - 1;
        } else {
            this.endTime = 0;
        }
        this.numberHits = 0;
        this.totalRunningTime = 0;
        this.minTime = Long.MAX_VALUE;
        this.maxTime = 0;
    }

    public Delegator getDelegator() {
        return this.delegator;
    }

    public String getId() {
        return this.id;
    }

    public int getType() {
        return this.type;
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
        return (this.getBinLength()) / 60000.0;
    }

    public synchronized long getNumberHits() {
        return this.numberHits;
    }

    public synchronized long getMinTime() {
        return this.minTime;
    }

    public synchronized long getMaxTime() {
        return this.maxTime;
    }

    public synchronized long getTotalRunningTime() {
        return this.totalRunningTime;
    }

    public double getMinTimeSeconds() {
        return (this.getMinTime()) / 1000.0;
    }

    public double getMaxTimeSeconds() {
        return (this.getMaxTime()) / 1000.0;
    }

    public synchronized double getAvgTime() {
        return ((double) this.getTotalRunningTime()) / ((double) this.getNumberHits());
    }

    public double getAvgTimeSeconds() {
        return this.getAvgTime() / 1000.0;
    }

    /** return the hits per minute using the entire length of the bin as returned by getBinLengthMinutes() */
    public double getHitsPerMinute() {
        return this.getNumberHits() / this.getBinLengthMinutes();
    }

    private synchronized void addHit(long runningTime) {
        this.numberHits++;
        this.totalRunningTime += runningTime;
        if (runningTime < this.minTime)
            this.minTime = runningTime;
        if (runningTime > this.maxTime)
            this.maxTime = runningTime;
    }

    private void saveHit(HttpServletRequest request, long startTime, long runningTime, GenericValue userLogin) throws GenericEntityException {
        // persist record of hit in ServerHit entity if option turned on
    	Delegator delegator = (Delegator) request.getAttribute("delegator");
        if (EntityUtilProperties.propertyValueEqualsIgnoreCase("serverstats", "stats.persist." + ServerHitBin.typeIds[type] + ".hit", "true", delegator)) {
            // if the hit type is ENTITY and the name contains "ServerHit" don't
            // persist; avoids the infinite loop and a bunch of annoying data
            if (this.type == ENTITY && this.id.indexOf("ServerHit") > 0) {
                return;
            }

            // check for type data before running.
            GenericValue serverHitType = null;

            serverHitType = EntityQuery.use(delegator).from("ServerHitType").where("hitTypeId", ServerHitBin.typeIds[this.type]).cache().queryOne();
            if (serverHitType == null) {
                // datamodel data not loaded; not storing hit.
                Debug.logWarning("The datamodel data has not been loaded; cannot find hitTypeId '" + ServerHitBin.typeIds[this.type] + " not storing ServerHit.", module);
                return;
            }

            GenericValue visit = VisitHandler.getVisit(request.getSession());
            if (visit == null) {
                // no visit info stored, so don't store the ServerHit
                Debug.logWarning("Could not find a visitId, so not storing ServerHit. This is probably a configuration error. If you turn off persistance of visits you should also turn off persistence of hits.", module);
                return;
            }
            String visitId = visit.getString("visitId");
            visit = EntityQuery.use(delegator).from("Visit").where("visitId", visitId).queryOne();
            if (visit == null) {
                // GenericValue stored in client session does not exist in database.
                Debug.logInfo("The Visit GenericValue stored in the client session does not exist in the database, not storing server hit.", module);
                return;
            }
            
            Debug.logInfo("Visit delegatorName=" + visit.getDelegator().getDelegatorName() + ", ServerHitBin delegatorName=" + this.delegator.getDelegatorName(), module);
            
            GenericValue serverHit = delegator.makeValue("ServerHit");

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
            serverHit.set("runningTimeMillis", Long.valueOf(runningTime));

            String fullRequestUrl = UtilHttp.getFullRequestUrl(request);

            serverHit.set("requestUrl", fullRequestUrl);
            String referrerUrl = request.getHeader("Referer") != null ? request.getHeader("Referer") : "";

            serverHit.set("referrerUrl", referrerUrl);

            // get localhost ip address and hostname to store
            if (VisitHandler.address != null) {
                serverHit.set("serverIpAddress", VisitHandler.address.getHostAddress());
                serverHit.set("serverHostName", VisitHandler.address.getHostName());
            }

            // The problem with
            //
            //     serverHit.create();
            //
            // is that if there are two requests with the same startTime (this should only happen with MySQL see https://issues.apache.org/jira/browse/OFBIZ-2208)
            // then this will go wrong and abort the actual
            // transaction we are interested in.
            // Another way instead of using create is to store or update,
            // that is overwrite in case there already was an entry, thus
            // avoiding the transaction being aborted which is not
            // less desirable than having multiple requests with the
            // same startTime overwriting each other.
            // This may not satisfy those who want to record each and
            // every server hit even with equal startTimes but that could be
            // solved adding a counter to the ServerHit's PK (a counter
            // counting multiple hits at the same startTime).
            serverHit.create();
        }
    }
}
