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
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.store.FSDirectory
import org.apache.ofbiz.base.util.Debug
import org.apache.ofbiz.base.util.UtilHttp
import org.apache.ofbiz.content.search.SearchWorker
import org.apache.ofbiz.product.feature.ParametricSearch
import org.apache.lucene.search.*
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.store.Directory
import org.apache.ofbiz.base.util.UtilProperties

paramMap = UtilHttp.getParameterMap(request)
queryLine = paramMap.queryLine.toString()
//Debug.logInfo("in search, queryLine:" + queryLine, "")

siteId = paramMap.siteId ?: "WebStoreCONTENT"
//Debug.logInfo("in search, siteId:" + siteId, "")
featureIdByType = ParametricSearch.makeFeatureIdByTypeMap(paramMap)
//Debug.logInfo("in search, featureIdByType:" + featureIdByType, "")

combQuery = new BooleanQuery.Builder()
IndexSearcher searcher = null
Analyzer analyzer = null

try {
    Directory directory = FSDirectory.open(new File(SearchWorker.getIndexPath("content")).toPath())
    DirectoryReader reader = DirectoryReader.open(directory)
    searcher = new IndexSearcher(reader)
    analyzer = new StandardAnalyzer()
} catch (java.io.FileNotFoundException e) {
    context.errorMessageList.add(UtilProperties.getMessage("ContentErrorUiLabels", "ContentSearchNotIndexed", locale))
    return
}

termQuery = new TermQuery(new Term("site", siteId.toString()))
combQuery.add(termQuery, BooleanClause.Occur.MUST)
//Debug.logInfo("in search, termQuery:" + termQuery.toString(), "")

//Debug.logInfo("in search, combQuery(1):" + combQuery, "")
if (queryLine && analyzer) {
    Query query = null
    QueryParser parser = new QueryParser("content", analyzer)
    query = parser.parse(queryLine)
    combQuery.add(query, BooleanClause.Occur.MUST)
}

if (featureIdByType) {
    featureQuery = new BooleanQuery.Builder()
    featuresRequired = BooleanClause.Occur.MUST
    if ("any".equals(paramMap.anyOrAll)) {
        featuresRequired = BooleanClause.Occur.SHOULD
    }

    if (featureIdByType) {
        featureIdByType.each { key, value ->
            termQuery = new TermQuery(new Term("feature", value))
            featureQuery.add(termQuery, featuresRequired)
            //Debug.logInfo("in search searchFeature3, termQuery:" + termQuery.toString(), "")
        }
    }
    combQuery.add(featureQuery.build(), featuresRequired)
}

if (searcher) {
    Debug.logInfo("in search searchFeature3, combQuery:" + combQuery.toString(), "")
    TopScoreDocCollector collector = TopScoreDocCollector.create(100) //defaulting to 100 results
    searcher.search(combQuery.build(), collector)
    ScoreDoc[] hits = collector.topDocs().scoreDocs
    Debug.logInfo("in search, hits:" + collector.getTotalHits(), "")

    contentList = [] as ArrayList
    hitSet = [:] as HashSet
    for (int start = 0; start < collector.getTotalHits(); start++) {
        Document doc = searcher.doc(hits[start].doc)
        contentId = doc.get("contentId")
        content = from("Content").where("contentId", contentId).cache(true).queryOne()
        if (!hitSet.contains(contentId)) {
            contentList.add(content)
            hitSet.add(contentId)
        }
    }
    context.queryResults = contentList
}
