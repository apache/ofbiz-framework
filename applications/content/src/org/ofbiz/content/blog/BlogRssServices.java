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

package org.ofbiz.content.blog;

import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.Debug;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.condition.EntityConditionList;
import org.ofbiz.content.content.ContentWorker;
import com.sun.syndication.feed.synd.*;

import java.util.*;
import java.io.IOException;

import javolution.util.FastList;

/**
 * BlogRssServices
 */
public class BlogRssServices {

    public static final String module = BlogRssServices.class.getName();
    public static final String mimeTypeId = "text/html";
    public static final String mapKey = "SUMMARY";

    public static Map generateBlogRssFeed(DispatchContext dctx, Map context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String contentId = (String) context.get("blogContentId");
        String entryLink = (String) context.get("entryLink");
        String feedType = (String) context.get("feedType");
        Locale locale = (Locale) context.get("locale");

        // create the main link
        String mainLink = (String) context.get("mainLink");
        mainLink = mainLink + "?blogContentId=" + contentId;

        LocalDispatcher dispatcher = dctx.getDispatcher();
        GenericDelegator delegator = dctx.getDelegator();

        // get the main blog content
        GenericValue content = null;
        try {
            content = delegator.findByPrimaryKey("Content", UtilMisc.toMap("contentId", contentId));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        if (content == null) {
            return ServiceUtil.returnError("Not able to generate RSS feed for content: " + contentId);
        }

        // create the feed
        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType(feedType);
        feed.setLink(mainLink);

        feed.setTitle(content.getString("contentName"));
        feed.setDescription(content.getString("description"));
        feed.setEntries(generateEntryList(dispatcher, delegator, contentId, entryLink, locale, userLogin));

        Map resp = ServiceUtil.returnSuccess();
        resp.put("wireFeed", feed.createWireFeed());
        return resp;
    }

    public static List generateEntryList(LocalDispatcher dispatcher, GenericDelegator delegator, String contentId, String entryLink, Locale locale, GenericValue userLogin) {
        List entries = FastList.newInstance();
        List exprs = FastList.newInstance();
        exprs.add(new EntityExpr("contentIdStart", EntityOperator.EQUALS, contentId));
        exprs.add(new EntityExpr("caContentAssocTypeId", EntityOperator.EQUALS, "PUBLISH_LINK"));
        exprs.add(new EntityExpr("statusId", EntityOperator.EQUALS, "CTNT_PUBLISHED"));

        List contentRecs = null;
        try {
            contentRecs = delegator.findByCondition("ContentAssocViewTo", new EntityConditionList(exprs, EntityOperator.AND), null, UtilMisc.toList("-caFromDate"));
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        if (contentRecs != null) {
            Iterator i = contentRecs.iterator();
            while (i.hasNext()) {
                GenericValue v = (GenericValue) i.next();
                String sub = null;
                try {
                    sub = ContentWorker.renderSubContentAsText(dispatcher, delegator, v.getString("contentId"), mapKey, new HashMap(), locale, mimeTypeId, true);
                } catch (GeneralException e) {
                    Debug.logError(e, module);
                } catch (IOException e) {
                    Debug.logError(e, module);
                }
                if (sub != null) {
                    String thisLink = entryLink + "?articleContentId=" + v.getString("contentId") + "&blogContentId=" + contentId;
                    SyndContent desc = new SyndContentImpl();
                    desc.setType("text/plain");
                    desc.setValue(sub);

                    SyndEntry entry = new SyndEntryImpl();
                    entry.setTitle(v.getString("contentName"));
                    entry.setPublishedDate(v.getTimestamp("createdDate"));
                    entry.setDescription(desc);
                    entry.setLink(thisLink);
                    entry.setAuthor((v.getString("createdByUserLogin")));                    
                    entries.add(entry);
                }
            }
        }

        return entries;
    }
}