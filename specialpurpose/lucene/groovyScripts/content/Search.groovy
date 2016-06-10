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

import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.Document
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.store.FSDirectory
import org.ofbiz.base.util.UtilHttp
import org.ofbiz.content.search.SearchWorker
import org.ofbiz.product.feature.ParametricSearch
import org.apache.lucene.search.*
import org.apache.lucene.index.DirectoryReader
import org.ofbiz.base.util.UtilProperties;

queryLine = parameters.queryLine;

siteId = parameters.lcSiteId;

searchFeature1 = (String) parameters.SEARCH_FEAT;
searchFeature2 = (String) parameters.SEARCH_FEAT2;
searchFeature3 = (String) parameters.SEARCH_FEAT3;

featureIdByType = ParametricSearch.makeFeatureIdByTypeMap(UtilHttp.getParameterMap(request));

combQuery = new BooleanQuery();

try {
    DirectoryReader reader = DirectoryReader.open(FSDirectory.open(new File(SearchWorker.getIndexPath("content")).toPath()));
    searcher = new IndexSearcher(reader);
    analyzer = new StandardAnalyzer();
} catch (java.io.FileNotFoundException e) {
    context.errorMessageList.add(UtilProperties.getMessage("ContentErrorUiLabels", "ContentSearchNotIndexed", locale));
    return;
}

if (queryLine || siteId) {
    Query query = null;
    if (queryLine) {
        QueryParser parser = new QueryParser("content", analyzer);
        query = parser.parse(queryLine);
        combQuery.add(query, BooleanClause.Occur.MUST);
    }
    if (siteId) {
        termQuery = new TermQuery(new Term("site", siteId.toString()));
        combQuery.add(termQuery, BooleanClause.Occur.MUST);
    }
}

if (searchFeature1 || searchFeature2 || searchFeature3 || !featureIdByType.isEmpty()) {
    featureQuery = new BooleanQuery();
    featuresRequired = BooleanClause.Occur.MUST;
    if ("any".equals(parameters.any_or_all)) {
        featuresRequired = BooleanClause.Occur.SHOULD;
    }

    if (searchFeature1) {
        termQuery = new TermQuery(new Term("feature", searchFeature1));
        featureQuery.add(termQuery, featuresRequired);
    }

    if (searchFeature2) {
        termQuery = new TermQuery(new Term("feature", searchFeature2));
        featureQuery.add(termQuery, featuresRequired);
    }

    if (searchFeature3) {
        termQuery = new TermQuery(new Term("feature", searchFeature3));
        featureQuery.add(termQuery, featuresRequired);
    }

  if (featureIdByType) {
    featureIdByType.each { key, value ->
            termQuery = new TermQuery(new Term("feature", value));
            featureQuery.add(termQuery, featuresRequired);
        }
    combQuery.add(featureQuery, featuresRequired);
    }
}
if (searcher) {
    TopScoreDocCollector collector = TopScoreDocCollector.create(100); //defaulting to 100 results
    searcher.search(combQuery, collector);
    ScoreDoc[] hits = collector.topDocs().scoreDocs;

    contentList = [] as ArrayList;
    hitSet = [:] as HashSet;
    for (int start = 0; start < collector.getTotalHits(); start++) {
        Document doc = searcher.doc(hits[start].doc)
        contentId = doc.get("contentId");
        content = from("Content").where("contentId", contentId).cache(true).queryOne();
        if (!hitSet.contains(contentId)) {
            contentList.add(content);
            hitSet.add(contentId);
        }
    }
    context.queryResults = contentList;
}
