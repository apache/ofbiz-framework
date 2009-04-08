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

import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.content.search.SearchWorker;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.*;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.ofbiz.widget.html.HtmlFormWrapper;
import org.ofbiz.product.feature.ParametricSearch;

paramMap = UtilHttp.getParameterMap(request);
queryLine = paramMap.queryLine;
//Debug.logInfo("in search, queryLine:" + queryLine, "");

formDefFile = page.formDefFile;
queryFormName = page.queryFormName;
//Debug.logInfo("in search, queryFormName:" + queryFormName, "");
queryWrapper = new HtmlFormWrapper(formDefFile, queryFormName, request, response);
context.queryWrapper = queryWrapper;

listFormName = page.listFormName;
//Debug.logInfo("in search, listFormName:" + listFormName, "");
listWrapper = new HtmlFormWrapper(formDefFile, listFormName, request, response);
context.listWrapper = listWrapper;
siteId = paramMap.siteId ?: "WebStoreCONTENT";
//Debug.logInfo("in search, siteId:" + siteId, "");
featureIdByType = ParametricSearch.makeFeatureIdByTypeMap(paramMap);
//Debug.logInfo("in search, featureIdByType:" + featureIdByType, "");

combQuery = new BooleanQuery();
indexPath = null;
searcher = null;
analyzer = null;
try {
    indexPath = SearchWorker.getIndexPath(null);
    searcher = new IndexSearcher(indexPath);
    analyzer = new StandardAnalyzer();
} catch (java.io.FileNotFoundException e) {
    Debug.logError(e, "Search.groovy");
    request.setAttribute("errorMsgReq", "No index file exists.");
}
termQuery = new TermQuery(new Term("site", siteId.toLowerCase()));
combQuery.add(termQuery, BooleanClause.Occur.MUST);
//Debug.logInfo("in search, termQuery:" + termQuery.toString(), "");

//Debug.logInfo("in search, combQuery(1):" + combQuery, "");
if (queryLine && analyzer) {
    Query query = null;
    queryParser = new QueryParser("content", analyzer);
    query = queryParser.parse(queryLine);
    combQuery.add(query, true, false);
}

if (featureIdByType) {
    featureQuery = new BooleanQuery();
    anyOrAll = paramMap.any_or_all;
    featuresRequired = true;
    if ("any".equals(anyOrAll)) {
        featuresRequired = false;
    }

    if (featureIdByType) {
        featureIdByType.each { key, value ->
            termQuery = new TermQuery(new Term("feature", value));
            featureQuery.add(termQuery, featuresRequired, false);
            //Debug.logInfo("in search searchFeature3, termQuery:" + termQuery.toString(), "");
        }
    }
    combQuery.add(featureQuery, featuresRequired, false);
}

if (searcher) {
    Debug.logInfo("in search searchFeature3, combQuery:" + combQuery.toString(), "");
    hits = searcher.search(combQuery);
    Debug.logInfo("in search, hits:" + hits.length(), "");
    contentList = [];
    hitSet = new HashSet();
    for (start = 0; start < hits.length(); start++) {
        doc = hits.doc(start);
        contentId = doc.contentId;
        content = delegator.findByPrimaryKeyCache("Content", [contentId : contentId]);
        if (!hitSet.contains(contentId)) {
            contentList.add(content);
            hitSet.add(contentId);
        }
    }
    listWrapper.putInContext("queryResults", contentList);
}
