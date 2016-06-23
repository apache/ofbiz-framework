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

import java.util.List;
import org.ofbiz.entity.*;
import org.ofbiz.entity.condition.*;
import org.ofbiz.entity.util.*;
import org.ofbiz.entity.util.EntityUtil;
import org.ofbiz.entity.transaction.*
import org.ofbiz.base.util.*;
import org.ofbiz.base.util.string.*;
import org.ofbiz.content.content.*;
import org.apache.commons.lang.StringEscapeUtils;
import org.ofbiz.entity.util.EntityUtilProperties;

int minFontSize = EntityUtilProperties.getPropertyAsInteger("ecommerce", "tagcloud.min.fontsize", 0).intValue();
int maxFontSize = EntityUtilProperties.getPropertyAsInteger("ecommerce", "tagcloud.max.fontsize", 0).intValue();
int limitTagCloud = EntityUtilProperties.getPropertyAsInteger("ecommerce", "tagcloud.limit", 0).intValue();

tagCloudList = [] as LinkedList;
tagList = [] as LinkedList;

productKeywords = select("keyword", "keywordTypeId", "statusId")
                    .from("ProductKeyword")
                    .where(keywordTypeId : "KWT_TAG", statusId : "KW_APPROVED")
                    .orderBy("keyword")
                    .distinct(true)
                    .queryList();

if (UtilValidate.isNotEmpty(productKeywords)) {
    productKeywords.each { productKeyword ->
        productTags = from("ProductKeyword").where("keyword", productKeyword.keyword, "keywordTypeId", "KWT_TAG", "statusId", "KW_APPROVED").queryList();
        searchResult = [:];
        searchResult.tag = productKeyword.keyword;
        searchResult.countTag = productTags.size();
        tagList.add(searchResult);
    }
}

if (tagList) {
    int tag = 0;
    tagList.sort{ a,b -> b.countTag <=> a.countTag };
    if (tagList.size() < limitTagCloud) {
        limitTagCloud = tagList.size();
    }
    int maxResult = tagList[0].countTag;
    int minResult = tagList[limitTagCloud - 1].countTag;
    tagList.each { tagCloud ->
        if (tag < limitTagCloud) {
            tagCloudOfbizInfo = [:];
            double weight = 0;
            if ((maxResult - minResult) > 0) {
                weight = (tagCloud.countTag - minResult) / (maxResult - minResult);
            }
            double fontSize = minFontSize + ((maxFontSize - minFontSize) * weight);
            tagCloudOfbizInfo.tag = tagCloud.tag;
            tagCloudOfbizInfo.fontSize = fontSize;
            tagCloudList.add(tagCloudOfbizInfo);
            tag++;
        }
    }
}

tagCloudList = tagCloudList.sort{it.tag}

context.tagCloudList = tagCloudList;
