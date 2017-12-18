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
package org.apache.ofbiz.common.preferences;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.ofbiz.base.util.GeneralException;
import org.apache.ofbiz.base.util.ObjectType;
import org.apache.ofbiz.base.util.UtilMisc;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.security.Security;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.ServiceUtil;

/**
 * User preference worker methods.
 */
public final class PreferenceWorker {
    public static final String module = PreferenceWorker.class.getName();
    /**
     * User preference administrator permission. Currently set to "USERPREF_ADMIN".
     */
    private static final String ADMIN_PERMISSION = "USERPREF_ADMIN";
    /** User login ID parameter name. Currently set to "userPrefLoginId". This
     * parameter name is used in preference service definitions to specify a user login ID
     * that is different than the currently logged in user.
     */
    private static final String LOGINID_PARAMETER_NAME = "userPrefLoginId";

    /** Default userLoginId. Currently set to "_NA_". This userLoginId is used to
     * retrieve default preferences when the user is not logged in.
     */
    private static final String DEFAULT_UID = "_NA_";

    private PreferenceWorker () {}

    /**
     * Add a UserPreference GenericValue to a Map.
     * @param rec GenericValue to convert
     * @param userPrefMap user preference Map
     * @throws GeneralException
     * @return user preference map
     */
    public static Map<String, Object> addPrefToMap(GenericValue rec, Map<String, Object> userPrefMap) throws GeneralException {
        String prefDataType = rec.getString("userPrefDataType");
        if (UtilValidate.isEmpty(prefDataType)) {
            // default to String
            userPrefMap.put(rec.getString("userPrefTypeId"), rec.getString("userPrefValue"));
        } else {
            userPrefMap.put(rec.getString("userPrefTypeId"), ObjectType.simpleTypeConvert(rec.get("userPrefValue"), prefDataType, null, null, false));
        }
        return userPrefMap;
    }

    /**
     * Checks preference copy permissions. Returns hasPermission=true if permission
     * is granted.
     * <p>Users can copy from any set of preferences to their own preferences.
     * Copying to another user's preferences requires <a href="#ADMIN_PERMISSION">ADMIN_PERMISSION</a>
     * permission.</p>
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input arguments.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> checkCopyPermission(DispatchContext ctx, Map<String, ?> context) {
        boolean hasPermission = false;
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (userLogin != null) {
            String userLoginId = userLogin.getString("userLoginId");
            String userLoginIdArg = (String) context.get(LOGINID_PARAMETER_NAME); // is an optional parameters which defaults to the logged on user
            if (userLoginIdArg == null || userLoginId.equals(userLoginIdArg)) {
                hasPermission = true; // users can copy to their own preferences
            } else {
                Security security = ctx.getSecurity();
                hasPermission = security.hasPermission(ADMIN_PERMISSION, userLogin);
            }
        }
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("hasPermission", hasPermission);
        return result;
    }

    /**
     * Checks preference get/set permissions. Returns hasPermission=true if
     * permission is granted.
     * <p>This method is a simple wrapper around the isValidxxxId methods.</p>
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input arguments.
     * @return Map with the result of the service, the output parameters.
     */
    public static Map<String, Object> checkPermission(DispatchContext ctx, Map<String, ?> context) {
        boolean hasPermission = false;
        String mainAction = (String) context.get("mainAction");
        if ("VIEW".equals(mainAction)) {
            if (DEFAULT_UID.equals(context.get(LOGINID_PARAMETER_NAME))) {
                hasPermission = true;
            } else {
                hasPermission = isValidGetId(ctx, context);
            }
        } else if ("CREATE~UPDATE~DELETE".contains(mainAction)) {
            hasPermission = isValidSetId(ctx, context);
        } else {
            hasPermission = false;
        }
        Map<String, Object> result = ServiceUtil.returnSuccess();
        result.put("hasPermission", hasPermission);
        return result;
    }

    /**
     * Convert a UserPreference GenericValue to a userPrefMap.
     * @param rec GenericValue to convert
     * @throws GeneralException
     * @return user preference map
     */
    public static Map<String, Object> createUserPrefMap(GenericValue rec) throws GeneralException {
        return addPrefToMap(rec, new LinkedHashMap<>());
    }

    /**
     * Convert a List of UserPreference GenericValues to a userPrefMap.
     * @param recList List of GenericValues to convert
     * @throws GeneralException
     * @return user preference map
     */
    public static Map<String, Object> createUserPrefMap(List<GenericValue> recList) throws GeneralException {
        Map<String, Object> userPrefMap =  new LinkedHashMap<>();
        if (recList != null) {
            for (GenericValue value: recList) {
                addPrefToMap(value, userPrefMap);
            }
        }
        return userPrefMap;
    }

    /**
     * Gets a valid userLoginId parameter from the context Map.
     * <p>This method searches the context Map for a userPrefLoginId key. If none is
     * found, the method attempts to get the current user's userLoginId. If the user
     * isn't logged in, then the method returns <a href="#DEFAULT_UID">DEFAULT_UID</a>
     * if returnDefault is set to true, otherwise the method returns a null or empty string.</p>
     *
     * @param context Map containing the input arguments.
     * @param returnDefault return <a href="#DEFAULT_UID">DEFAULT_UID</a> if no userLoginId is found.
     * @return userLoginId String
     */
    public static String getUserLoginId(Map<String, ?> context, boolean returnDefault) {
        String userLoginId = (String) context.get(LOGINID_PARAMETER_NAME);
        if (UtilValidate.isEmpty(userLoginId)) {
            GenericValue userLogin = (GenericValue) context.get("userLogin");
            if (userLogin != null) {
                userLoginId = userLogin.getString("userLoginId");
            }
        }
        if (UtilValidate.isEmpty(userLoginId) && returnDefault) {
            userLoginId = DEFAULT_UID;
        }
        return userLoginId;
    }

    /**
     * Checks for valid userLoginId to get preferences. Returns true if valid.
     * <p>This method applies a small rule set to determine if user preferences
     * can be retrieved by the current user:</p>
     * <ul>
     * <li>If the user isn't logged in, then the method returns true</li>
     * <li>If the user is logged in and the userPrefLoginId specified in the context Map
     * matches the user's userLoginId, then the method returns true.</li>
     * <li>If the user is logged in and the userPrefLoginId specified in the context Map
     * is different than the user's userLoginId, then a security permission check is performed.
     * If the user has the <a href="#ADMIN_PERMISSION">ADMIN_PERMISSION</a> permission then the
     *  method returns true.</li>
     * </ul>
     *
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input arguments.
     * @return true if the userLoginId arguments are valid
     */
    public static boolean isValidGetId(DispatchContext ctx, Map<String, ?> context) {
        String currentUserLoginId = null;
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (userLogin == null) {
            currentUserLoginId = DEFAULT_UID;
        } else {
            currentUserLoginId = userLogin.getString("userLoginId");
        }
        String userLoginIdArg = (String) context.get(LOGINID_PARAMETER_NAME);
        if (!currentUserLoginId.equals(DEFAULT_UID) && !currentUserLoginId.equals(userLoginIdArg)
                && userLoginIdArg != null) {
            Security security = ctx.getSecurity();
            return security.hasPermission(ADMIN_PERMISSION, userLogin);
        }
        return true;
    }

    /**
     * Checks for valid userLoginId to set preferences. Returns true if valid.
     * <p>This method applies a small rule set to determine if user preferences
     * can be set by the current user:</p>
     * <ul>
     * <li>If the user isn't logged in, then the method returns false</li>
     * <li>If the user is logged in and the userPrefLoginId specified in the context Map
     * matches the user's userLoginId, then the method returns true.</li>
     * <li>If the user is logged in and the userPrefLoginId specified in the context Map
     * is different than the user's userLoginId, then a security permission check is performed.
     * If the user has the <a href="#ADMIN_PERMISSION">ADMIN_PERMISSION</a>
     * permission then the method returns true.</li>
     * </ul>
     * @param ctx The DispatchContext that this service is operating in.
     * @param context Map containing the input arguments.
     * @return true if arguments are valid
     */
    public static boolean isValidSetId(DispatchContext ctx, Map<String, ?> context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        if (userLogin == null) {
            return false;
        }
        String currentUserLoginId = userLogin.getString("userLoginId");
        String userLoginIdArg = (String) context.get(LOGINID_PARAMETER_NAME);
        if (!currentUserLoginId.equals(userLoginIdArg) && userLoginIdArg != null) {
            Security security = ctx.getSecurity();
            return security.hasPermission(ADMIN_PERMISSION, userLogin);
        }
        return true;
    }

    /**
     * Creates a field Map to be used in GenericValue create or store methods.
     * @param userLoginId The user's login ID
     * @param userPrefTypeId The preference ID
     * @param userPrefGroupTypeId The preference group ID (may be null or empty)
     * @param userPrefValue The preference value (will be converted to java.lang.String data type)
     * @throws GeneralException
     * @return field map
     */
    public static Map<String, Object> toFieldMap(String userLoginId, String userPrefTypeId, String userPrefGroupTypeId, Object userPrefValue) throws GeneralException {
        Map<String, Object> fieldMap = UtilMisc.toMap("userLoginId", userLoginId, "userPrefTypeId", userPrefTypeId, "userPrefValue", ObjectType.simpleTypeConvert(userPrefValue, "String", null, null, false));
        if (UtilValidate.isNotEmpty(userPrefGroupTypeId)) {
            fieldMap.put("userPrefGroupTypeId", userPrefGroupTypeId);
        }
        String valueDataType = userPrefValue.getClass().getName();
        if (!"java.lang.String".equals(valueDataType)) {
            fieldMap.put("userPrefDataType", valueDataType);
        }
        return fieldMap;
    }
}
