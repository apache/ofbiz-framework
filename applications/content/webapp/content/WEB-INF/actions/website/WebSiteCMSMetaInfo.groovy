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

import org.ofbiz.entity.*
import org.ofbiz.entity.condition.*
import org.ofbiz.entity.util.*

if (content) {
    // lookup assoc content
    titles = delegator.findList("ContentAssoc", EntityCondition.makeCondition([contentId : contentId, mapKey : 'title']), null, ['-fromDate'], null, false);
    titles = EntityUtil.filterByDate(titles);
    title = EntityUtil.getFirst(titles);
    if (title) {
        tc = title.getRelatedOne("ToContent", false);
        tcdr = tc.getRelatedOne("DataResource", false);
        context.title = tcdr;
    }

    titleProps = delegator.findList("ContentAssoc", EntityCondition.makeCondition([contentId : contentId, mapKey : 'titleProperty']), null, ['-fromDate'], null, false);
    titleProps = EntityUtil.filterByDate(titleProps);
    titleProp = EntityUtil.getFirst(titleProps);
    if (titleProp) {
        tpc = titleProp.getRelatedOne("ToContent", false);
        tpcdr = tpc.getRelatedOne("DataResource", false);
        context.titleProperty = tpcdr;
    }

    metaDescs = delegator.findList("ContentAssoc", EntityCondition.makeCondition([contentId : contentId, mapKey : 'metaDescription']), null, ['-fromDate'], null, false);
    metaDescs = EntityUtil.filterByDate(metaDescs);
    metaDesc = EntityUtil.getFirst(metaDescs);
    if (metaDesc) {
        mdc = metaDesc.getRelatedOne("ToContent", false);
        mdcdr = mdc.getRelatedOne("DataResource", false);
        context.metaDescription = mdcdr;
    }

    metaKeys = delegator.findList("ContentAssoc", EntityCondition.makeCondition([contentId : contentId, mapKey : 'metaKeywords']), null, ['-fromDate'], null, false);
    metaKeys = EntityUtil.filterByDate(metaKeys);
    metaKey = EntityUtil.getFirst(metaKeys);
    if (metaKey) {
        mkc = metaKey.getRelatedOne("ToContent", false);
        mkcdr = mkc.getRelatedOne("DataResource", false);
        context.metaKeywords = mkcdr;
    }
}
