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

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.search.Searcher
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.index.IndexReader
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.index.Term
import org.apache.lucene.search.Hits
import org.apache.lucene.queryParser.QueryParser
import org.ofbiz.base.util.UtilHttp
import org.ofbiz.base.util.Debug
import org.ofbiz.product.feature.ParametricSearch
import org.ofbiz.content.search.SearchWorker

paramMap = UtilHttp.getParameterMap(request);
queryLine = paramMap.queryLine;
Debug.logInfo("in search, queryLine:" + queryLine, "");

siteId = paramMap.lcSiteId;
Debug.logInfo("in search, siteId:" + siteId, "");

searchFeature1 = paramMap.SEARCH_FEAT;
searchFeature2 = paramMap.SEARCH_FEAT2;
searchFeature3 = paramMap.SEARCH_FEAT3;

featureIdByType = ParametricSearch.makeFeatureIdByTypeMap(paramMap);
Debug.logInfo("in search, featureIdByType:" + featureIdByType, "");


combQuery = new BooleanQuery();
indexPath = null;
Searcher searcher = null;
Analyzer analyzer = null;

try {
    indexPath = SearchWorker.getIndexPath(null);
    Debug.logInfo("in search, indexPath:" + indexPath, "");
    searcher = new IndexSearcher(indexPath);
    Debug.logInfo("in search, searcher:" + searcher, "");
    analyzer = new StandardAnalyzer();
} catch (java.io.FileNotFoundException e) {
    request.setAttribute("errorMsgReq", "No index file exists.");
    Debug.logError("in search, error:" + e.getMessage(), "");
}

if (queryLine || siteId) {
    Query query = null;
    if (queryLine) {
        queryParser = new QueryParser("content", analyzer);
        query = queryParser.parse(queryLine);
        combQuery.add(query, true, false);
    }
    Debug.logInfo("in search, combQuery(0):" + combQuery, "");

    if (siteId) {
        termQuery = new TermQuery(new Term("site", siteId));
        combQuery.add(termQuery, true, false);
        Debug.logInfo("in search, termQuery:" + termQuery.toString(), "");
    }
    Debug.logInfo("in search, combQuery(1):" + combQuery, "");
}

if (searchFeature1 || searchFeature2 || searchFeature3 || !featureIdByType.isEmpty()) {
    featureQuery = new BooleanQuery();
    anyOrAll = paramMap.any_or_all;
    featuresRequired = true;

    if (anyOrAll && "any".equals(anyOrAll)) {
        featuresRequired = false;
    }

    if (searchFeature1) {
        termQuery = new TermQuery(new Term("feature", searchFeature1));
        featureQuery.add(termQuery, featuresRequired, false);
        Debug.logInfo("in search searchFeature1, termQuery:" + termQuery.toString(), "");
    }

    if (searchFeature2) {
        termQuery = new TermQuery(new Term("feature", searchFeature2));
        featureQuery.add(termQuery, featuresRequired, false);
        Debug.logInfo("in search searchFeature2, termQuery:" + termQuery.toString(), "");
    }

    if (searchFeature3) {
        termQuery = new TermQuery(new Term("feature", searchFeature3));
        featureQuery.add(termQuery, featuresRequired, false);
        Debug.logInfo("in search searchFeature3, termQuery:" + termQuery.toString(), "");
    }

    if (!featureIdByType.isEmpty()) {
        values = featureIdByType.values();
        values.each { val ->
            termQuery = new TermQuery(new Term("feature", val));
            featureQuery.add(termQuery, featuresRequired, false);
            Debug.logInfo("in search searchFeature3, termQuery:" + termQuery.toString(), "");
        }
        combQuery.add(featureQuery, featuresRequired, false);
    }

    if (searcher) {
        Debug.logInfo("in search searchFeature3, combQuery:" + combQuery.toString(), "");
        Hits hits = searcher.search(combQuery);
        Debug.logInfo("in search, hits:" + hits.length(), "");

        contentList = [] as ArrayList;
        hitSet = [:] as HashSet;
        for (int start = 0; start < hits.length(); start++) {
             doc = hits.doc(start);
             contentId = doc.contentId;
             content = delegator.findOne("Content", [contentId : contentId], true);
             if (!hitSet.contains(contentId)) {
                 contentList.add(content);
                 hitSet.add(contentId);
             }
        }
        context.queryResults = contentList;
    }
}