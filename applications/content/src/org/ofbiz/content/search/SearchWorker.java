/*******************************************************************************
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
 *******************************************************************************/
package org.ofbiz.content.search;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.LocalDispatcher;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;



/**
 * SearchWorker Class
 */
public class SearchWorker {

    public static final String module = SearchWorker.class.getName();

    public static Map<String, Object> indexTree(LocalDispatcher dispatcher, Delegator delegator, String siteId, Map<String, Object> context, String path) throws Exception {

        Map<String, Object> results = FastMap.newInstance();
        GenericValue content = delegator.makeValue("Content", UtilMisc.toMap("contentId", siteId));
        if (Debug.infoOn()) Debug.logInfo("in indexTree, siteId:" + siteId + " content:" + content, module);
        List<GenericValue> siteList = ContentWorker.getAssociatedContent(content, "To", UtilMisc.toList("SUBSITE", "PUBLISH_LINK", "SUB_CONTENT"), null, UtilDateTime.nowTimestamp().toString(), null);

        if (siteList != null) {
            Iterator<GenericValue> iter = siteList.iterator();
            while (iter.hasNext()) {
                GenericValue siteContent = iter.next();
                String siteContentId = siteContent.getString("contentId");
                List<GenericValue> subContentList = ContentWorker.getAssociatedContent(siteContent, "To", UtilMisc.toList("SUBSITE", "PUBLISH_LINK", "SUB_CONTENT"), null, UtilDateTime.nowTimestamp().toString(), null);

                if (subContentList != null) {
                    List<String> contentIdList = FastList.newInstance();
                    Iterator<GenericValue> iter2 = subContentList.iterator();
                    while (iter2.hasNext()) {
                        GenericValue subContent = iter2.next();
                        contentIdList.add(subContent.getString("contentId"));
                    }

                    indexContentList(contentIdList, delegator, dispatcher, context);

                    String subSiteId = siteContent.getString("contentId");
                    indexTree(dispatcher, delegator, subSiteId, context, path);
                } else {
                    List<String> badIndexList = UtilGenerics.checkList(context.get("badIndexList"));
                    badIndexList.add(siteContentId + " had no sub-entities.");
                }
            }
        } else {
            List<String> badIndexList = UtilGenerics.checkList(context.get("badIndexList"));
            badIndexList.add(siteId + " had no sub-entities.");
        }
        results.put("badIndexList", context.get("badIndexList"));
        results.put("goodIndexCount", context.get("goodIndexCount"));

        return results;
    }

    public static void indexContentList(List<String> idList, Delegator delegator, LocalDispatcher dispatcher, Map<String, Object> context) throws Exception {
        String path = null;
        indexContentList(dispatcher, delegator, context, idList, path);
    }

    public static void indexContentList(LocalDispatcher dispatcher, Delegator delegator, Map<String, Object> context,List<String> idList, String path) throws Exception {
        Directory directory = FSDirectory.open(new File(getIndexPath(path)));
        if (Debug.infoOn()) Debug.logInfo("in indexContent, indexAllPath: " + directory.toString(), module);
        GenericValue content = null;
        // Delete existing documents
        Iterator<String> iter = null;
        List<GenericValue> contentList = null;
        IndexReader reader = null;
        try {
            reader = IndexReader.open(directory, false);
        } catch (Exception e) {
            // ignore
        }

        contentList = FastList.newInstance();
        iter = idList.iterator();
        while (iter.hasNext()) {
            String id = iter.next();
            if (Debug.infoOn()) Debug.logInfo("in indexContent, id:" + id, module);
            try {
                content = delegator.findByPrimaryKeyCache("Content", UtilMisc .toMap("contentId", id));
                if (content != null) {
                    if (reader != null) {
                        deleteContentDocument(content, reader);
                    }
                    contentList.add(content);
                }
            } catch (GenericEntityException e) {
                Debug.logError(e, module);
                return;
            }
        }
        if (reader != null) {
            reader.close();
        }
        // Now create
        IndexWriter writer = null;
        try {
            writer = new IndexWriter(directory, new StandardAnalyzer(Version.LUCENE_30), false, IndexWriter.MaxFieldLength.UNLIMITED);
        } catch (Exception e) {
            writer = new IndexWriter(directory, new StandardAnalyzer(Version.LUCENE_30), true, IndexWriter.MaxFieldLength.UNLIMITED);
        }

        Iterator<GenericValue> contentListIter = contentList.iterator();
        while (contentListIter.hasNext()) {
            content = contentListIter.next();
            indexContent(dispatcher, delegator, context, content, writer);
        }
        writer.optimize();
        writer.close();
    }

    public static void deleteContentDocument(GenericValue content, String path) throws Exception {
        Directory directory = FSDirectory.open(new File(getIndexPath(path)));
        IndexReader reader = IndexReader.open(directory);
        deleteContentDocument(content, reader);
        reader.close();
    }

    public static void deleteContentDocument(GenericValue content, IndexReader reader) throws Exception {
        String contentId = content.getString("contentId");
        Term term = new Term("contentId", contentId);
        if (Debug.infoOn()) Debug.logInfo("in indexContent, term:" + term, module);
        int qtyDeleted = reader.deleteDocuments(term);
        if (Debug.infoOn()) Debug.logInfo("in indexContent, qtyDeleted:" + qtyDeleted, module);
        String dataResourceId = content.getString("dataResourceId");
        if (dataResourceId != null) {
            deleteDataResourceDocument(dataResourceId, reader);
        }
    }

    public static void deleteDataResourceDocument(String dataResourceId, IndexReader reader) throws Exception {
        Term term = new Term("dataResourceId", dataResourceId);
        if (Debug.infoOn()) Debug.logInfo("in indexContent, term:" + term, module);
        int qtyDeleted = reader.deleteDocuments(term);
        if (Debug.infoOn()) Debug.logInfo("in indexContent, qtyDeleted:" + qtyDeleted, module);
    }

    public static void indexContent(LocalDispatcher dispatcher, Delegator delegator, Map<String, Object> context, GenericValue content, String path) throws Exception {
        Directory directory = FSDirectory.open(new File(getIndexPath(path)));
        IndexWriter writer = null;
        try {
            writer = new IndexWriter(directory, new StandardAnalyzer(Version.LUCENE_30), false, IndexWriter.MaxFieldLength.UNLIMITED);
            if (Debug.infoOn()) Debug.logInfo("Used old directory:" + directory.toString(), module);
        } catch (FileNotFoundException e) {
            writer = new IndexWriter(directory, new StandardAnalyzer(Version.LUCENE_30), true, IndexWriter.MaxFieldLength.UNLIMITED);
            if (Debug.infoOn()) Debug.logInfo("Created new directory:" + directory.toString(), module);
        }

        indexContent(dispatcher, delegator, context, content, writer);
        writer.optimize();
        writer.close();
    }

    public static void indexContent(LocalDispatcher dispatcher, Delegator delegator, Map<String, Object> context, GenericValue content, IndexWriter writer) throws Exception {
        Document doc = ContentDocument.Document(content, context, dispatcher);

        if (doc != null) {
            writer.addDocument(doc);
            Integer goodIndexCount = (Integer)context.get("goodIndexCount");
            Integer newIndexCount = goodIndexCount + 1;
            context.put("goodIndexCount", newIndexCount);
        }
        /*
            String dataResourceId = content.getString("dataResourceId");
            if (UtilValidate.isNotEmpty(dataResourceId)) {
                indexDataResource(delegator, context, dataResourceId, writer);
            }
         */
    }

    public static void indexDataResource(Delegator delegator, Map<String, Object> context, String id) throws Exception {
        String path = null;
        indexDataResource(delegator, context, id, path);
    }

    public static void indexDataResource(Delegator delegator, Map<String, Object> context, String id, String path) throws Exception {
        Directory directory = FSDirectory.open(new File(getIndexPath(path)));
        IndexWriter writer = null;
        try {
            writer = new IndexWriter(directory, new StandardAnalyzer(Version.LUCENE_30), false, IndexWriter.MaxFieldLength.UNLIMITED);
        } catch (FileNotFoundException e) {
            writer = new IndexWriter(directory, new StandardAnalyzer(Version.LUCENE_30), true, IndexWriter.MaxFieldLength.UNLIMITED);
        }
        indexDataResource(delegator, context, id, writer);
        writer.optimize();
        writer.close();
    }

    public static void indexDataResource(Delegator delegator, Map<String, Object> context, String id, IndexWriter writer) throws Exception {
        Document doc = DataResourceDocument.Document(id, delegator, context);
        writer.addDocument(doc);
    }

    public static String getIndexPath(String path) {
        String indexAllPath = path;
        if (UtilValidate.isEmpty(indexAllPath)) {
            indexAllPath = UtilProperties.getPropertyValue("search", "defaultIndex", "index");
        }
        return indexAllPath;
    }
}
