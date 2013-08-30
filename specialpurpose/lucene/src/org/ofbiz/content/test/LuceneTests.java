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
import java.util.List;
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
        userLogin = delegator.findOne("UserLogin", UtilMisc.toMap("userLoginId", "system"), false);
    }

    @Override
    protected void tearDown() throws Exception {
    }

    public void testCreateIndex() throws Exception {
        Map<String, Object> ctx = new HashMap<String, Object>();
        ctx.put("contentId", "WebStoreCONTENT");
        ctx.put("userLogin", userLogin);
        Map<String, Object> resp = dispatcher.runSync("indexTree", ctx);

        assertEquals(7, resp.get("goodIndexCount"));

        List<String> badIndexList = UtilGenerics.checkList(resp.get("badIndexList"));
        assertEquals(8, badIndexList.size());
    }

    public void testSearchTermHand() throws Exception {
        Directory directory = FSDirectory.open(new File(SearchWorker.getIndexPath(null)));
        DirectoryReader r = null;
        try {
            r = DirectoryReader.open(directory);
        } catch (Exception e) {
            // ignore
        }

        BooleanQuery combQuery = new BooleanQuery();
        String queryLine = "hand";

        IndexSearcher searcher = new IndexSearcher(r);
        Analyzer analyzer = new StandardAnalyzer(SearchWorker.LUCENE_VERSION);

        QueryParser parser = new QueryParser(SearchWorker.LUCENE_VERSION, "content", analyzer);
        Query query = parser.parse(queryLine);
        combQuery.add(query, BooleanClause.Occur.MUST);

        TopScoreDocCollector collector = TopScoreDocCollector.create(10, false);
        searcher.search(combQuery, collector);

        assertEquals("Only 1 result expected from the testdata", 1, collector.getTotalHits());
    }
}
