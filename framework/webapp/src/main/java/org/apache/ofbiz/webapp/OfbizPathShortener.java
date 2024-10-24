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
package org.apache.ofbiz.webapp;

import java.util.Map;
import javax.transaction.Transaction;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.ofbiz.base.crypto.HashCrypt;
import org.apache.ofbiz.base.util.UtilDateTime;
import org.apache.ofbiz.base.util.UtilProperties;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.transaction.GenericTransactionException;
import org.apache.ofbiz.entity.transaction.TransactionUtil;
import org.apache.ofbiz.entity.util.EntityQuery;

public class OfbizPathShortener {
    public static final String SHORTENED_PATH = "s/";
    public static final String RESTORE_PATH = "../";

    /**
     * For an ofbiz path, return a shortened url that will be linked to the given path
     * example : orderview?orderId=HA1023 -> s/izapnreiis
     * @param delegator
     * @param path to shorten
     * @return a shortened key corresponding to the path
     * @throws GenericEntityException
     */
    public static String shortenPath(Delegator delegator, String path) throws GenericEntityException {
        return SHORTENED_PATH + resolveShortenedPath(delegator, path);
    }

    /**
     * For the given path, check if a shortened path already exists otherwise generate a new one
     * @param delegator
     * @param path
     * @return a shortened path corresponding to the given path
     * @throws GenericEntityException
     */
    public static String resolveShortenedPath(Delegator delegator, String path) throws GenericEntityException {
        String shortenedPath = resolveExistingShortenedPath(delegator, path);
        int nbLoop = 0;
        if (shortenedPath == null) {
            do {
                shortenedPath = generate();
                nbLoop++;
            } while (!recordPathMapping(delegator, path, shortenedPath) || nbLoop > 10);
        }
        return shortenedPath;
    }

    /**
     * Try to resolve the original path, if failed, return to the webapp root. Use views request for that define on common-controller
     * @param delegator
     * @param shortenedPath
     * @return the origin path corresponding to the given shortened path, webapp root otherwise
     * @throws GenericEntityException
     */
    public static String restoreOriginalPath(Delegator delegator, String shortenedPath) throws GenericEntityException {
        String originalPath = resolveOriginalPathFromShortened(delegator, shortenedPath);
        return RESTORE_PATH + (originalPath != null ? originalPath : "views");
    }

    /**
     * From a shortened path, resolve the origin path
     * @param delegator
     * @param shortenedPath path
     * @return the original path corresponding to the shortened path
     * @throws GenericEntityException
     */
    public static String resolveOriginalPathFromShortened(Delegator delegator, String shortenedPath) throws GenericEntityException {
        return readPathMapping(delegator, shortenedPath);
    }

    /**
     * For a path, function tried to resolve if it already presents in database
     * For performance issue the function use the hash to resolve it
     * @param delegator
     * @param path
     * @return the shortened path if found, null otherwise
     * @throws GenericEntityException
     */
    private static String resolveExistingShortenedPath(Delegator delegator, String path) throws GenericEntityException {
        GenericValue existingPath = EntityQuery.use(delegator)
                .from("ShortenedPath")
                .where("originalPathHash", generateHash(path))
                .cache()
                .queryFirst();
        return existingPath != null ? existingPath.getString("shortenedPath") : null;
    }

    /**
     * generate a random shortened path, the size can be set on property : security.
     * @return shortened path
     */
    private static String generate() {
        int shortenerSize = UtilProperties.getPropertyAsInteger("security", "path.shortener.size", 10);
        return RandomStringUtils.randomAlphabetic(shortenerSize);
    }

    /**
     * Create the mapping between an origin map and the shortened path
     * This will be executed on dedicate transaction to be sure to not rollback it after.
     * @param delegator
     * @param path
     * @param shortenedPath
     * @return true if it's create with success
     */
    private static boolean recordPathMapping(Delegator delegator, String path, String shortenedPath) {
        Transaction trans = null;
        try {
            try {
                trans = TransactionUtil.suspend();
                TransactionUtil.begin();
                delegator.create("ShortenedPath", Map.of("shortenedPath", shortenedPath,
                        "originalPath", path,
                        "originalPathHash", generateHash(path),
                        "createdDate", UtilDateTime.nowTimestamp(),
                        "createdByUserLogin", "system"));
                TransactionUtil.commit();
            } catch (GenericEntityException e) {
                TransactionUtil.rollback();
                return false;
            } finally {
                TransactionUtil.resume(trans);
            }
        } catch (GenericTransactionException e) {
            return false;
        }
        return true;
    }

    /**
     * @param path
     * @return a hash of the given path
     */
    private static String generateHash(String path) {
        return HashCrypt.digestHash("SHA", path.getBytes());
    }

    /**
     * Find the origin path corresponding to the shorter in database
     * @param delegator
     * @param shortenedPath
     * @return the original path corresponding to shortened path given, null if not found
     * @throws GenericEntityException
     */
    private static String readPathMapping(Delegator delegator, String shortenedPath) throws GenericEntityException {
        GenericValue existingPath = EntityQuery.use(delegator)
                .from("ShortenedPath")
                .where("shortenedPath", shortenedPath)
                .cache()
                .queryOne();
        return existingPath != null
                ? existingPath.getString("originalPath")
                : null;
    }

}
