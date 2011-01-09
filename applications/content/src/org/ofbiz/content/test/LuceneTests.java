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

package org.ofbiz.content.test;

import java.io.File;
import java.util.List;
import java.util.Map;

import javolution.util.FastMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.content.search.SearchWorker;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.testtools.OFBizTestCase;

public class LuceneTests extends OFBizTestCase {

    protected GenericValue userLogin = null;

    public LuceneTests(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        userLogin = delegator.findByPrimaryKey("UserLogin", UtilMisc.toMap("userLoginId", "system"));
    }

    @Override
    protected void tearDown() throws Exception {
    }

    public void testCreateIndex() throws Exception {
        Map<String, Object> ctx = FastMap.newInstance();
        ctx.put("contentId", "WebStoreCONTENT");
        ctx.put("userLogin", userLogin);
        Map<String, Object> resp = dispatcher.runSync("indexTree", ctx);

        assertEquals(7, resp.get("goodIndexCount"));

        List<String> badIndexList = UtilGenerics.checkList(resp.get("badIndexList"));
        assertEquals(8, badIndexList.size());
    }

    public void testSearchTermHand() throws Exception {
        BooleanQuery combQuery = new BooleanQuery();
        String queryLine = "hand";
        IndexReader reader = IndexReader.open(FSDirectory.open(new File(SearchWorker.getIndexPath(null))), true);

        Searcher searcher = new IndexSearcher(reader);
        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_30);

        QueryParser parser = new QueryParser(Version.LUCENE_30, "content", analyzer);
        Query query = parser.parse(queryLine);
        combQuery.add(query, BooleanClause.Occur.MUST);

        TopScoreDocCollector collector = TopScoreDocCollector.create(10, false);
        searcher.search(combQuery, collector);

        assertEquals("Only 1 result expected from the testdata", 1, collector.getTotalHits());
    }
}
