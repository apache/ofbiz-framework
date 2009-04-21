package org.ofbiz.common.authentication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ofbiz.base.util.Debug;
import org.ofbiz.common.authentication.api.Authenticator;
import org.ofbiz.common.authentication.api.AuthenticatorException;
import org.ofbiz.service.LocalDispatcher;

/**
 * AuthHelper
 */
public class AuthHelper {

    private static final String module = AuthHelper.class.getName();
    protected static List<Authenticator> authenticators = new ArrayList<Authenticator>();
    protected static boolean authenticatorsLoaded = false;


    public static boolean authenticate(String username, String password, boolean isServiceAuth) throws AuthenticatorException {
        if (!authenticatorsLoaded) throw new AuthenticatorException("Authenticators never loaded; be sure to call AuthHelper.loadAuthenticators()");
        for (Authenticator auth : authenticators) {
            boolean pass = auth.authenticate(username, password, isServiceAuth);
            if (pass) {
                return true;
            } else if (auth.isSingleAuthenticator()) {
                throw new AuthenticatorException();
            }
        }
        return false;
    }

    public static void logout(String username) throws AuthenticatorException {
        if (!authenticatorsLoaded) throw new AuthenticatorException("Authenticators never loaded; be sure to call AuthHelper.loadAuthenticators()");
        for (Authenticator auth : authenticators) {
            auth.logout(username);
        }
    }

    public static void syncUser(String username) throws AuthenticatorException {
        if (!authenticatorsLoaded) throw new AuthenticatorException("Authenticators never loaded; be sure to call AuthHelper.loadAuthenticators()");
        for (Authenticator auth : authenticators) {
            if (auth.isUserSynchronized()) {
                auth.syncUser(username);
            }
        }
    }

    public static void updatePassword(String username, String password, String newPassword) throws AuthenticatorException {
        if (!authenticatorsLoaded) throw new AuthenticatorException("Authenticators never loaded; be sure to call AuthHelper.loadAuthenticators()");
        for (Authenticator auth : authenticators) {
            auth.updatePassword(username, password, newPassword);            
        }
    }

    public static boolean authenticatorsLoaded() {
        return authenticatorsLoaded;
    }
    
    public static void loadAuthenticators(LocalDispatcher dispatcher) {
        if (!authenticatorsLoaded) {
            loadAuthenticators_internal(dispatcher);
        }
    }

    private synchronized static void loadAuthenticators_internal(LocalDispatcher dispatcher) {
        if (!authenticatorsLoaded) {
            AuthInterfaceResolver resolver = new AuthInterfaceResolver();
            List<Class> implementations = resolver.getImplementations();

            for (Class c : implementations) {
                try {
                    Authenticator auth = (Authenticator) c.newInstance();
                    if (auth.isEnabled()) {
                        auth.initialize(dispatcher);                    
                        authenticators.add(auth);
                    }
                } catch (InstantiationException e) {
                    Debug.logError(e, module);
                } catch (IllegalAccessException e) {
                    Debug.logError(e, module);
                } catch (ClassCastException e) {
                    Debug.logError(e, module);
                }
            }

            Collections.sort(authenticators, new AuthenticationComparator());
            authenticatorsLoaded = true;
        }
    }
}
