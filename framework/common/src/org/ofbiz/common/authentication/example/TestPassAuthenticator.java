package org.ofbiz.common.authentication.example;

import org.ofbiz.common.authentication.api.AuthenticatorException;
import org.ofbiz.common.authentication.api.Authenticator;
import org.ofbiz.base.util.Debug;

/**
 * TestPassAuthenticator
 */
public class TestPassAuthenticator extends TestFailAuthenticator implements Authenticator {

    private static final String module = TestPassAuthenticator.class.getName();

    /**
     * Method to authenticate a user
     *
     * @param username      User's username
     * @param password      User's password
     * @param isServiceAuth true if authentication is for a service call
     * @return true if the user is authenticated
     * @throws org.ofbiz.common.authentication.api.AuthenticatorException
     *          when a fatal error occurs during authentication
     */
    @Override
    public boolean authenticate(String username, String password, boolean isServiceAuth) throws AuthenticatorException {
        Debug.logInfo(this.getClass().getName() + " Authenticator authenticate() -- returning false", module);
        return true;
    }

    /**
     * Flag to test if this Authenticator is enabled
     *
     * @return true if the Authenticator is enabled
     */
    @Override
    public boolean isEnabled() {
        return false;
    }
}
