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
import java.lang.Object;
import java.lang.String;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.ofbiz.content.search.SearchWorker;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.util.EntityQuery;
import org.ofbiz.service.ServiceUtil;
import org.ofbiz.service.testtools.OFBizTestCase;

public class LuceneTests extends OFBizTestCase {

    protected GenericValue userLogin = null;

    public LuceneTests(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        userLogin = EntityQuery.use(delegator).from("UserLogin").where("userLoginId", "system").queryOne();
    }

    @Override
    protected void tearDown() throws Exception {
    }

    public void testSearchTermHand() throws Exception {
        Map<String, Object> ctx = new HashMap<String, Object>();
        ctx.put("contentId", "WebStoreCONTENT");
        ctx.put("userLogin", userLogin);
        Map<String, Object> resp = dispatcher.runSync("indexContentTree", ctx);
        assertTrue("Could not init search index", ServiceUtil.isSuccess(resp));
        try {
            Thread.sleep(3000); // sleep 3 seconds to give enough time to the indexer to process the entries
        } catch(Exception e) {}
        Directory directory = FSDirectory.open(new File(SearchWorker.getIndexPath("content")).toPath());
        DirectoryReader r = null;
        try {
            r = DirectoryReader.open(directory);
        } catch (Exception e) {
            fail("Could not open search index: " + directory);
        }

        BooleanQuery.Builder combQueryBuilder = new BooleanQuery.Builder();
        String queryLine = "hand";

        IndexSearcher searcher = new IndexSearcher(r);
        Analyzer analyzer = new StandardAnalyzer();
        analyzer.setVersion(SearchWorker.getLuceneVersion());

        QueryParser parser = new QueryParser("content", analyzer);
        Query query = parser.parse(queryLine);
        combQueryBuilder.add(query, BooleanClause.Occur.MUST);
        BooleanQuery combQuery = combQueryBuilder.build();

        TopScoreDocCollector collector = TopScoreDocCollector.create(10);
        searcher.search(combQuery, collector);

        assertEquals("Only 1 result expected from the testdata", 1, collector.getTotalHits());
    }
}
