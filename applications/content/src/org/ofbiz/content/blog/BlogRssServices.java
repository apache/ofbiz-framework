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

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ServiceUtil;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;

/**
 * BlogRssServices
 */
public class BlogRssServices {

    public static final String module = BlogRssServices.class.getName();
    public static final String resource = "ContentUiLabels";
    public static final String mimeTypeId = "text/html";
    public static final String mapKey = "SUMMARY";

    public static Map<String, Object> generateBlogRssFeed(DispatchContext dctx, Map<String, ? extends Object> context) {
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        String contentId = (String) context.get("blogContentId");
        String entryLink = (String) context.get("entryLink");
        String feedType = (String) context.get("feedType");
        Locale locale = (Locale) context.get("locale");

        // create the main link
        String mainLink = (String) context.get("mainLink");
        mainLink = mainLink + "?blogContentId=" + contentId;

        LocalDispatcher dispatcher = dctx.getDispatcher();
        Delegator delegator = dctx.getDelegator();

        // get the main blog content
        GenericValue content = null;
        try {
            content = delegator.findOne("Content", UtilMisc.toMap("contentId", contentId), false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        if (content == null) {
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "ContentCannotGenerateBlogRssFeed", 
                    UtilMisc.toMap("contentId", contentId), locale));
        }

        // create the feed
        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType(feedType);
        feed.setLink(mainLink);

        feed.setTitle(content.getString("contentName"));
        feed.setDescription(content.getString("description"));
        feed.setEntries(generateEntryList(dispatcher, delegator, contentId, entryLink, locale, userLogin));

        Map<String, Object> resp = ServiceUtil.returnSuccess();
        resp.put("wireFeed", feed.createWireFeed());
        return resp;
    }

    public static List<SyndEntry> generateEntryList(LocalDispatcher dispatcher, Delegator delegator, String contentId, String entryLink, Locale locale, GenericValue userLogin) {
        List<SyndEntry> entries = FastList.newInstance();
        List<EntityCondition> exprs = FastList.newInstance();
        exprs.add(EntityCondition.makeCondition("contentIdStart", contentId));
        exprs.add(EntityCondition.makeCondition("caContentAssocTypeId", "PUBLISH_LINK"));
        exprs.add(EntityCondition.makeCondition("statusId", "CTNT_PUBLISHED"));

        List<GenericValue> contentRecs = null;
        try {
            contentRecs = delegator.findList("ContentAssocViewTo", EntityCondition.makeCondition(exprs), null, UtilMisc.toList("-caFromDate"), null, false);
        } catch (GenericEntityException e) {
            Debug.logError(e, module);
        }

        if (contentRecs != null) {
            for(GenericValue v : contentRecs) {
                String sub = null;
                try {
                    Map<String, Object> dummy = FastMap.newInstance();
                    sub = ContentWorker.renderSubContentAsText(dispatcher, delegator, v.getString("contentId"), mapKey, dummy, locale, mimeTypeId, true);
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
