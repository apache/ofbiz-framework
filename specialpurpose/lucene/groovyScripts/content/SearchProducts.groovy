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


import org.apache.lucene.analysis.core.WhitespaceAnalyzer
import org.ofbiz.content.search.SearchWorker

import org.apache.lucene.document.Document
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.Query
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.TopScoreDocCollector
import org.apache.lucene.store.FSDirectory

if (parameters.luceneQuery) {
    Query combQuery = new BooleanQuery();
    IndexSearcher searcher;
    WhitespaceAnalyzer analyzer;
    try {
        DirectoryReader reader = DirectoryReader.open(FSDirectory.open(new File(SearchWorker.getIndexPath("products")).toPath()));
        searcher = new IndexSearcher(reader);
        analyzer = new WhitespaceAnalyzer();
    } catch (FileNotFoundException e) {
        context.errorMessageList.add(e.getMessage());
        return;
    }

    QueryParser parser = new QueryParser("fullText", analyzer);
    parser.setLocale(locale);
    Query query;
    try {
        query = parser.parse(parameters.luceneQuery);
    } catch(ParseException pe) {
        context.errorMessageList.add(pe.getMessage());
        return;
    }
    combQuery.add(query, BooleanClause.Occur.MUST);

    TopScoreDocCollector collector = TopScoreDocCollector.create(100); // defaulting to 100 results
    searcher.search(combQuery, collector);
    ScoreDoc[] hits = collector.topDocs().scoreDocs;
    productList = []
    hits.each { hit ->
        Document doc = searcher.doc(hit.doc)
        productId = doc.productId
        product = from("Product").where("productId", productId).cache(true).queryOne();
        if (product) {
            productList.add(product)
        }
    }
    context.queryResults = productList;
}
