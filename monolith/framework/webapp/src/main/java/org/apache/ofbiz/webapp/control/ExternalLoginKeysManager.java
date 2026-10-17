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
package org.apache.ofbiz.webapp.control;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.DelegatorFactory;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.webapp.WebAppUtil;

/**
 * This class manages the single sign-on authentication through external login keys between OFBiz applications.
 */
public class ExternalLoginKeysManager {
    private static final String MODULE = ExternalLoginKeysManager.class.getName();
    private static final String EXTERNAL_LOGIN_KEY_ATTR = "externalLoginKey";
    // How long a minted key remains redeemable. Bounds the window during which a leaked key
    // (address bar, browser history, Referer header, proxy log) is worth anything to an attacker.
    private static final long EXTERNAL_LOGIN_KEY_TTL_MILLIS = 2 * 60 * 1000;
    // This Map is keyed by the randomly generated externalLoginKey and the value is the ticket
    // describing who it authenticates as and until when. One render mints one key and embeds it
    // in every cross-webapp link on that page (see getExternalLoginKey's request-attribute
    // cache), so a ticket is redeemable once per distinct destination webapp -- see
    // ExternalLoginTicket#redeemFor -- rather than once globally: that keeps the ordinary
    // app-switcher workflow (open two different apps from the same page) working while still
    // rejecting a second redemption against the same webapp.
    private static final Map<String, ExternalLoginTicket> EXTERNAL_LOGIN_KEYS = new ConcurrentHashMap<>();

    /**
     * A minted external login key, bound to the UserLogin it authenticates and to a deadline.
     * Also bound to the set of destination webapps it is actually attached to: a mint site does
     * not know in advance every webapp its render will link to (one render mints one key and
     * hands it to several cross-webapp links, possibly several different destinations), so
     * destinations are registered as they become known -- see #allowFor -- rather than fixed at
     * construction time. A ticket with no registered destinations yet authenticates nowhere.
     */
    private static final class ExternalLoginTicket {
        private final GenericValue userLogin;
        private final long expiresAtMillis;
        // Context paths this ticket is allowed to authenticate into. Grown by #allowFor as the
        // concrete destinations of this render's cross-webapp links become known; checked by
        // #isValidFor so a leaked key is only ever redeemable against a webapp this render
        // actually intended to reach, not any webapp on the server.
        private final Set<String> allowedContextPaths = ConcurrentHashMap.newKeySet();
        // Context paths this ticket has already been redeemed for. A single render shares one
        // key across every cross-webapp link it emits, so the same key legitimately needs to
        // authenticate into several different webapps; this set is what stops it authenticating
        // into the *same* webapp twice, which is the actual replay this ticket must prevent.
        private final Set<String> redeemedContextPaths = ConcurrentHashMap.newKeySet();

        ExternalLoginTicket(GenericValue userLogin) {
            this.userLogin = userLogin;
            this.expiresAtMillis = System.currentTimeMillis() + EXTERNAL_LOGIN_KEY_TTL_MILLIS;
        }

        GenericValue getUserLogin() {
            return userLogin;
        }

        /**
         * Allows this ticket to authenticate into the given destination webapp.
         * @param contextPath the destination webapp's context path, e.g. "/partymgr"
         */
        void allowFor(String contextPath) {
            allowedContextPaths.add(contextPath);
        }

        boolean isValidFor(HttpServletRequest request) {
            if (System.currentTimeMillis() > expiresAtMillis) {
                return false;
            }
            return allowedContextPaths.contains(request.getServletContext().getContextPath());
        }

        /**
         * Atomically marks this ticket as redeemed for the request's webapp.
         * @param request the request presenting this ticket's key
         * @return true the first time this webapp redeems this ticket, false on any repeat
         *     (replay) for the same webapp
         */
        boolean redeemFor(HttpServletRequest request) {
            return redeemedContextPaths.add(request.getServletContext().getContextPath());
        }
    }

    /**
     * Gets (and creates if necessary) an authentication token to be used for an external login parameter.
     * When a new token is created, it is persisted in the web session and in the web request and map entry keyed by the
     * token and valued by a userLogin object is added to a map that is looked up for subsequent requests.
     * @param request - the http request in which the authentication token is searched and stored
     * @return the authentication token as persisted in the session and request objects
     */
    public static String getExternalLoginKey(HttpServletRequest request) {
        String externalKey = (String) request.getAttribute(EXTERNAL_LOGIN_KEY_ATTR);
        if (externalKey != null) return externalKey;

        HttpSession session = request.getSession();
        synchronized (session) {
            // if the session has a previous key in place, remove it from the master list
            String sesExtKey = (String) session.getAttribute(EXTERNAL_LOGIN_KEY_ATTR);

            if (sesExtKey != null) {
                if (isAjax(request)) return sesExtKey;

                EXTERNAL_LOGIN_KEYS.remove(sesExtKey);
            }

            GenericValue userLogin = (GenericValue) request.getAttribute("userLogin");
            //check the userLogin here, after the old session setting is set so that it will always be cleared
            if (userLogin == null) return "";

            //no key made yet for this request, create one
            while (externalKey == null || EXTERNAL_LOGIN_KEYS.containsKey(externalKey)) {
                UUID uuid = UUID.randomUUID();
                externalKey = "EL" + uuid.toString();
            }

            request.setAttribute(EXTERNAL_LOGIN_KEY_ATTR, externalKey);
            session.setAttribute(EXTERNAL_LOGIN_KEY_ATTR, externalKey);
            EXTERNAL_LOGIN_KEYS.put(externalKey, new ExternalLoginTicket(userLogin));
            return externalKey;
        }
    }

    /**
     * Removes the authentication token, if any, from the session.
     * @param session - the http session from which the authentication token is removed
     */
    static void cleanupExternalLoginKey(HttpSession session) {
        String sesExtKey = (String) session.getAttribute(EXTERNAL_LOGIN_KEY_ATTR);
        if (sesExtKey != null) {
            EXTERNAL_LOGIN_KEYS.remove(sesExtKey);
        }
    }

    /**
     * Allows an already-minted key to authenticate into a destination webapp, given that
     * webapp's context path directly. Used at the point a concrete destination is already
     * known, e.g. the set of webapps a rendered app-switcher menu can link to.
     * @param externalLoginKey the minted key, as returned by {@link #getExternalLoginKey}
     * @param contextPath the destination webapp's context path, e.g. "/partymgr"
     */
    public static void registerDestination(String externalLoginKey, String contextPath) {
        if (UtilValidate.isEmpty(externalLoginKey) || UtilValidate.isEmpty(contextPath)) {
            return;
        }
        ExternalLoginTicket ticket = EXTERNAL_LOGIN_KEYS.get(externalLoginKey);
        if (ticket != null) {
            ticket.allowFor(contextPath);
        }
    }

    /**
     * Allows an already-minted key to authenticate into the destination webapp targeted by a
     * relative inter-app link, e.g. {@code "/partymgr/control/viewprofile?partyId=10000"} or
     * {@code "/partymgr/control"}. The destination webapp's context path is parsed out of the
     * target; a target that isn't a routed control-servlet request registers nothing, since
     * nothing would ever redeem a ticket against it anyway.
     * @param externalLoginKey the minted key, as returned by {@link #getExternalLoginKey}
     * @param interAppTarget the relative target of an inter-app link
     */
    public static void registerInterAppDestination(String externalLoginKey, String interAppTarget) {
        registerDestination(externalLoginKey, contextRootOf(interAppTarget));
    }

    /**
     * Parses the destination webapp's context path out of a relative inter-app target, by
     * taking everything before the "/control" path segment.
     * @param target the relative target, e.g. "/partymgr/control/viewprofile?partyId=10000"
     * @return the context path, e.g. "/partymgr" (possibly empty, for a root-mounted webapp),
     *     or null if the target has no "/control" path segment to anchor on
     */
    private static String contextRootOf(String target) {
        if (target == null) {
            return null;
        }
        int idx = target.indexOf("/control");
        while (idx >= 0) {
            int afterIdx = idx + "/control".length();
            if (afterIdx == target.length() || target.charAt(afterIdx) == '/' || target.charAt(afterIdx) == '?') {
                return target.substring(0, idx);
            }
            idx = target.indexOf("/control", idx + 1);
        }
        return null;
    }

    /**
     * OFBiz controller event that performs the user authentication using the authentication token.
     * The method is designed to be used in a chain of controller preprocessor event: it always return "success"
     * even when the authentication token is missing or the authentication fails in order to move the processing to the
     * next event in the chain.
     * @param request - the http request object
     * @param response - the http response object
     * @return "success" in all the cases
     */
    public static String checkExternalLoginKey(HttpServletRequest request, HttpServletResponse response) {
        String externalKey = request.getParameter(EXTERNAL_LOGIN_KEY_ATTR);
        if (externalKey == null) return "success";

        if (!isExternalLoginKeyEnabled(request)) {
            // Feature disabled: don't even look up the ticket, so nothing about it is consumed.
            LoginWorker.autoLoginSet(request, response);
            return "success";
        }

        // Look up without removing: the same key is shared across every cross-webapp link one
        // render emits, so it must stay valid for whichever *other* destination webapps the
        // user still hasn't visited yet. redeemFor(), below, is what actually stops replay --
        // it rejects a second redemption against a webapp this exact ticket already logged into.
        ExternalLoginTicket ticket = EXTERNAL_LOGIN_KEYS.get(externalKey);
        if (ticket == null || !ticket.isValidFor(request) || !ticket.redeemFor(request)) {
            Debug.logWarning("Could not find a valid, not-yet-redeemed userLogin for external login key: " + externalKey, MODULE);
            // make sure the autoUserLogin is set to the same and that the client cookie has the correct userLoginId
            LoginWorker.autoLoginSet(request, response);
            return "success";
        }

        GenericValue userLogin = ticket.getUserLogin();
        //to check it's the right tenant
        //in case username and password are the same in different tenants
        Delegator delegator = (Delegator) request.getAttribute("delegator");
        String oldDelegatorName = delegator.getDelegatorName();
        if (!oldDelegatorName.equals(userLogin.getDelegator().getDelegatorName())) {
            delegator = DelegatorFactory.getDelegator(userLogin.getDelegator().getDelegatorName());
            LocalDispatcher dispatcher = WebAppUtil.makeWebappDispatcher(request.getServletContext(), delegator);
            LoginWorker.setWebContextObjects(request, response, delegator, dispatcher);
        }
        // found userLogin, do the external login...

        // if the user is already logged in and the login is different, logout the other user
        HttpSession session = request.getSession();
        GenericValue currentUserLogin = (GenericValue) session.getAttribute("userLogin");
        if (currentUserLogin != null) {
            if (currentUserLogin.getString("userLoginId").equals(userLogin.getString("userLoginId"))) {
                // same user, just make sure the autoUserLogin is set to the same and that the client cookie has the correct userLoginId
                LoginWorker.autoLoginSet(request, response);
                // Same for the SecuredLoginId cookie
                LoginWorker.createSecuredLoginIdCookie(request, response);
                return "success";
            }

            // logout the current user and login the new user...
            LoginWorker.logout(request, response);
            // ignore the return value; even if the operation failed we want to set the new UserLogin
        }

        // check userLogin base permission and if it is enabled
        request.getSession().setAttribute("userLogin", userLogin);
        userLogin = LoginWorker.checkLogout(request, response);
        if (userLogin == null) {
            // base permission check failed or the account is flagged logged-out; checkLogout
            // already tore the session login down, so stop here rather than dereference null.
            LoginWorker.autoLoginSet(request, response);
            return "success";
        }

        LoginWorker.doBasicLogin(userLogin, request, response);

        // Create a secured cookie with the correct userLoginId
        LoginWorker.createSecuredLoginIdCookie(request, response);

        // make sure the autoUserLogin is set to the same and that the client cookie has the correct userLoginId
        LoginWorker.autoLoginSet(request, response);
        return "success";
    }

    /**
     * Checks if the request is an Ajax request.
     * @param request the request to check
     * @return indicator
     */
    private static boolean isAjax(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
    }

    /**
     * Check if using externalLoginKey
     * @return
     */
    public static boolean isExternalLoginKeyEnabled(HttpServletRequest request) {
        return "true".equals(EntityUtilProperties.getPropertyValue("security",
                "security.login.externalLoginKey.enabled", "true",
                (Delegator) request.getAttribute("delegator")));
    }

}
