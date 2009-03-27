package org.ofbiz.catalina.container;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.apache.catalina.Globals;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.http.MimeHeaders;
import org.apache.tomcat.util.http.ServerCookie;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;

public class CrossSubdomainSessionValve extends ValveBase {

    public static final String module = CrossSubdomainSessionValve.class.getName();

    public CrossSubdomainSessionValve() {
        super();
    }

    public @Override void invoke(Request request, Response response) throws IOException, ServletException {

        // this will cause Request.doGetSession to create the session cookie if necessary
        request.getSession(true);

        // replace any Tomcat-generated session cookies with our own
        Cookie[] cookies = response.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                Cookie cookie = cookies[i];
                if (Globals.SESSION_COOKIE_NAME.equals(cookie.getName())) {
                    replaceCookie(request, response, cookie);
                }
            }
        }

        // process the next valve
        getNext().invoke(request, response);
    }

    protected void replaceCookie(Request request, Response response, Cookie cookie) {

        // copy the existing session cookie, but use a different domain (only if domain is valid)
        String cookieDomain = null;
        cookieDomain = UtilProperties.getPropertyValue("url", "cookie.domain", "");

        if (UtilValidate.isEmpty(cookieDomain)) {
            String serverName = request.getServerName();
            String[] domainArray = serverName.split("\\.");
            // check that the domain isn't an IP address
            if (domainArray.length == 4) {
                boolean isIpAddress = true;
                for (String domainSection : domainArray) {
                    if (!UtilValidate.isIntegerInRange(domainSection, 0, 255)) {
                        isIpAddress = false;
                        break;
                    }
                }
                if (isIpAddress) return;
            }
            if (domainArray.length > 2) {
                cookieDomain = "." + domainArray[domainArray.length - 2] + "." + domainArray[domainArray.length - 1];
            }
        }


        if (UtilValidate.isNotEmpty(cookieDomain)) {
            Cookie newCookie = new Cookie(cookie.getName(), cookie.getValue());
            if (cookie.getPath() != null) {
                newCookie.setPath(cookie.getPath());
            }
            newCookie.setDomain(cookieDomain);
            newCookie.setMaxAge(cookie.getMaxAge());
            newCookie.setVersion(cookie.getVersion());
            if (cookie.getComment() != null) {
                newCookie.setComment(cookie.getComment());
            }
            newCookie.setSecure(cookie.getSecure());

            // if the response has already been committed, our replacement strategy will have no effect
            if (response.isCommitted()) {
                Debug.logError("CrossSubdomainSessionValve: response was already committed!", module);
            }

            // find the Set-Cookie header for the existing cookie and replace its value with new cookie
            MimeHeaders mimeHeaders = response.getCoyoteResponse().getMimeHeaders();
            for (int i = 0, size = mimeHeaders.size(); i < size; i++) {
                if (mimeHeaders.getName(i).equals("Set-Cookie")) {
                    MessageBytes value = mimeHeaders.getValue(i);
                    if (value.indexOf(cookie.getName()) >= 0) {
                        StringBuffer buffer = new StringBuffer();
                        ServerCookie.appendCookieValue(buffer, newCookie.getVersion(), newCookie.getName(), newCookie.getValue(), newCookie.getPath(),
                                newCookie.getDomain(), newCookie.getComment(), newCookie.getMaxAge(), newCookie.getSecure());
                        Debug.logVerbose("CrossSubdomainSessionValve: old Set-Cookie value: " + value.toString(), module);
                        Debug.logVerbose("CrossSubdomainSessionValve: new Set-Cookie value: " + buffer, module);
                        value.setString(buffer.toString());
                    }
                }
            }
        }
    }
}
