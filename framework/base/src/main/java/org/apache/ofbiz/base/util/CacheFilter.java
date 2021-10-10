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
package org.apache.ofbiz.base.util;

import java.io.IOException;
import java.util.stream.Collectors;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

public class CacheFilter implements Filter {

    private FilterConfig filterConfig = null;

    /**
     * The <code>doFilter</code> method of the Filter is called by the container each time a request/response pair is passed through the chain due to
     * a client request for a resource at the end of the chain. The FilterChain passed in to this method allows the Filter to pass on the request and
     * response to the next entity in the chain.
     * <p>
     * A typical implementation of this method would follow the following pattern:- <br>
     * 1. Examine the request<br>
     * 2. Optionally wrap the request object with a custom implementation to filter content or headers for input filtering <br>
     * 3. Optionally wrap the response object with a custom implementation to filter content or headers for output filtering <br>
     * 4. a) <strong>Either</strong> invoke the next entity in the chain using the FilterChain object (<code>chain.doFilter()</code>), <br>
     * 4. b) <strong>or</strong> not pass on the request/response pair to the next entity in the filter chain to block the request processing<br>
     * 5. Directly set headers on the response after invocation of the next entity in the filter chain. <br>
     * <br>
     * ----------------------------------------------------------------------------------------------------<br>
     * Actually its goal in OFBiz is simply to prevent a post-auth security issue described in OFBIZ-12332 <br>
     * ----------------------------------------------------------------------------------------------------<br>
     * <br>
     * @param request The request to process
     * @param response The response associated with the request
     * @param chain Provides access to the next filter in the chain for this filter to pass the request and response to for further processing
     * @throws IOException if an I/O error occurs during this filter's processing of the request
     * @throws ServletException if the processing fails for any other reason
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        // Get the request URI without the webapp mount point.
        String context = ((HttpServletRequest) request).getContextPath();
        String uriWithContext = ((HttpServletRequest) request).getRequestURI();
        String uri = uriWithContext.substring(context.length());

        if ("/control/xmlrpc".equals(uri.toLowerCase())) {
            // Read request.getReader() as many time you need
            request = new RequestWrapper((HttpServletRequest) request);
            String body = request.getReader().lines().collect(Collectors.joining());
            if (body.contains("</serializable")) {
                Debug.logError("Content not authorised for security reason", "CacheFilter"); // Cf. OFBIZ-12332
                return;
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * Called by the web container to indicate to a filter that it is being placed into service. The servlet container calls the init method exactly
     * once after instantiating the filter. The init method must complete successfully before the filter is asked to do any filtering work.
     * <p>
     * The web container cannot place the filter into service if the init method either:
     * <ul>
     * <li>Throws a ServletException</li>
     * <li>Does not return within a time period defined by the web container</li>
     * </ul>
     * The default implementation is a NO-OP.
     * @param filterConfiguration The configuration information associated with the filter instance being initialised
     * @throws ServletException if the initialisation fails
     */
    public void init(FilterConfig filterConfiguration) throws ServletException {
        setFilterConfig(filterConfiguration);
    }

    /**
     * Called by the web container to indicate to a filter that it is being taken out of service. This method is only called once all threads within
     * the filter's doFilter method have exited or after a timeout period has passed. After the web container calls this method, it will not call the
     * doFilter method again on this instance of the filter. <br>
     * <br>
     * This method gives the filter an opportunity to clean up any resources that are being held (for example, memory, file handles, threads) and make
     * sure that any persistent state is synchronized with the filter's current state in memory. The default implementation is a NO-OP.
     */
    public void destroy() {
        setFilterConfig(null);
    }

    /**
     * @return the filterConfig
     */
    public FilterConfig getFilterConfig() {
        return filterConfig;
    }

    /**
     * @param filterConfig the filterConfig to set
     */
    public void setFilterConfig(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

}
