/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

package org.ofbiz.webapp.event;

import org.ofbiz.webapp.control.RequestHandler;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.syndication.feed.WireFeed;
import com.sun.syndication.io.WireFeedOutput;
import com.sun.syndication.io.FeedException;

import java.io.IOException;

/**
 * RomeEventHandler
 */
public class RomeEventHandler implements EventHandler {

    public static final String module = RomeEventHandler.class.getName();
    public static final String mime = "application/xml; charset=UTF-8";
    public static final String defaultFeedType = "rss_2.0";

    protected RequestHandler handler;
    protected ServletContext context;
    protected EventHandler service;
    protected WireFeedOutput out;

    public void init(ServletContext context) throws EventHandlerException {
        this.context = context;
        this.handler = (RequestHandler) context.getAttribute("_REQUEST_HANDLER_");
        if (this.handler == null) {
            throw new EventHandlerException("No request handler found in servlet context!");
        }
        
        // get the service event handler
        this.service = new ServiceEventHandler();
        this.service.init(context);
        this.out = new WireFeedOutput();
    }

    public String invoke(String eventPath, String eventMethod, HttpServletRequest request, HttpServletResponse response) throws EventHandlerException {
        // generate the main and entry links
        String entryLinkReq = request.getParameter("entryLinkReq");
        String mainLinkReq = request.getParameter("mainLinkReq");

        // create the links; but the query string must be created by the service
        String entryLink = handler.makeLink(request, response, entryLinkReq, true, false, false);
        String mainLink = handler.makeLink(request, response, mainLinkReq, true, false, false);
        request.setAttribute("entryLink", entryLink);
        request.setAttribute("mainLink", mainLink);

        String feedType = request.getParameter("feedType");
        if (feedType == null) {
            request.setAttribute("feedType", defaultFeedType);
        }

        // invoke the feed generator service (implements rssFeedInterface)
        String respCode = service.invoke(eventPath, eventMethod, request, response);

        // pull out the RSS feed from the request attributes
        WireFeed wireFeed = (WireFeed) request.getAttribute("wireFeed");
        response.setContentType(mime);
        try {
            out.output(wireFeed, response.getWriter());
        } catch (IOException e) {
            throw new EventHandlerException("Unable to get response writer", e);
        } catch (FeedException e) {
            throw new EventHandlerException("Unable to write RSS feed", e);
        }
        
        return respCode;
    }
}
