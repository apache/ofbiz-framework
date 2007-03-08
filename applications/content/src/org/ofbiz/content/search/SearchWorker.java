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

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.LocalDispatcher;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;



/**
 * SearchWorker Class
 */
public class SearchWorker {

	public static final String module = SearchWorker.class.getName();

        public static Map indexTree(LocalDispatcher dispatcher, GenericDelegator delegator, String siteId, Map context, String path) throws Exception {

            Map results = new HashMap();
            GenericValue content = delegator.makeValue("Content", UtilMisc.toMap("contentId", siteId));
                if (Debug.infoOn()) Debug.logInfo("in indexTree, siteId:" + siteId + " content:" + content, module);
            List siteList = ContentWorker.getAssociatedContent(content, "From", UtilMisc.toList("SUBSITE", "PUBLISH_LINK"), null, UtilDateTime.nowTimestamp().toString(), null);
	    //if (Debug.infoOn()) Debug.logInfo("in indexTree, siteList:" + siteList, module);
            if (siteList != null) {
                Iterator iter = siteList.iterator();
                while (iter.hasNext()) {
                    GenericValue siteContent = (GenericValue)iter.next();
                    String siteContentId = siteContent.getString("contentId");
                    List subContentList = ContentWorker.getAssociatedContent(siteContent, "From", UtilMisc.toList("SUBSITE", "PUBLISH_LINK", "SUB_CONTENT"), null, UtilDateTime.nowTimestamp().toString(), null);
    	  	//if (Debug.infoOn()) Debug.logInfo("in indexTree, subContentList:" + subContentList, module);
                    if (subContentList != null) {
                        List contentIdList = new ArrayList();
                        Iterator iter2 = subContentList.iterator();
                        while (iter2.hasNext()) {
                            GenericValue subContent = (GenericValue)iter2.next();
                            contentIdList.add(subContent.getString("contentId")); 
                        }
        	  	//if (Debug.infoOn()) Debug.logInfo("in indexTree, contentIdList:" + contentIdList, module);
                        indexContentList(contentIdList, delegator, dispatcher, context);
        
                        String subSiteId = siteContent.getString("contentId");
                        indexTree(dispatcher, delegator, subSiteId, context, path);
                    } else {
                        List badIndexList = (List)context.get("badIndexList");
                        badIndexList.add(siteContentId + " had no sub-entities.");
                    }
                }
            } else {
                List badIndexList = (List)context.get("badIndexList");
                badIndexList.add(siteId + " had no sub-entities.");
            }
            results.put("badIndexList", context.get("badIndexList"));
            results.put("goodIndexCount", context.get("goodIndexCount"));
            //if (Debug.infoOn()) Debug.logInfo("in indexTree, results:" + results, module);
            return results;
        }
	
	public static void indexContentList(List idList, GenericDelegator delegator, LocalDispatcher dispatcher, Map context) throws Exception {
		String path = null;
		indexContentList(dispatcher, delegator, context, idList, path);
	}
	
	public static void indexContentList(LocalDispatcher dispatcher, GenericDelegator delegator, Map context, List idList, String path) throws Exception {
		String indexAllPath = getIndexPath(path);
		if (Debug.infoOn())
			Debug.logInfo("in indexContent, indexAllPath:" + indexAllPath, module);
		GenericValue content = null;
		// Delete existing documents
		Iterator iter = null;
		List contentList = null;
		IndexReader reader = null;
		try {
			reader = IndexReader.open(indexAllPath);
		} catch (Exception e) {
			// ignore
		}
		//if (Debug.infoOn()) Debug.logInfo("in indexContent, reader:" +
		// reader, module);
		contentList = new ArrayList();
		iter = idList.iterator();
		while (iter.hasNext()) {
			String id = (String) iter.next();
			if (Debug.infoOn())
				Debug.logInfo("in indexContent, id:" + id, module);
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
			writer = new IndexWriter(indexAllPath, new StandardAnalyzer(), false);
		} catch (Exception e) {
			writer = new IndexWriter(indexAllPath, new StandardAnalyzer(), true);
		}
		//if (Debug.infoOn()) Debug.logInfo("in indexContent, writer:" +
		// writer, module);
		iter = contentList.iterator();
		while (iter.hasNext()) {
			content = (GenericValue) iter.next();
			indexContent(dispatcher, delegator, context, content, writer);
		}
		writer.optimize();
		writer.close();
	}
	
	
	public static void deleteContentDocument(GenericValue content, String path) throws Exception {
	    String indexAllPath = null;
	    indexAllPath = getIndexPath(path);
	    IndexReader reader = IndexReader.open(indexAllPath);
            deleteContentDocument(content, reader);
            reader.close();
	}
	
	public static void deleteContentDocument(GenericValue content, IndexReader reader) throws Exception {
            String contentId = content.getString("contentId");
	    Term term = new Term("contentId", contentId);
	    if (Debug.infoOn()) Debug.logInfo("in indexContent, term:" + term, module);
	    int qtyDeleted = reader.delete(term);
	    if (Debug.infoOn()) Debug.logInfo("in indexContent, qtyDeleted:" + term, module);
	    String dataResourceId = content.getString("dataResourceId");
	    if (dataResourceId != null) {
	    	deleteDataResourceDocument(dataResourceId, reader);
	    }

	}
	

	public static void deleteDataResourceDocument(String dataResourceId, IndexReader reader) throws Exception {
	    Term term = new Term("dataResourceId", dataResourceId);
	    if (Debug.infoOn()) Debug.logInfo("in indexContent, term:" + term, module);
	    int qtyDeleted = reader.delete(term);
	    if (Debug.infoOn()) Debug.logInfo("in indexContent, qtyDeleted:" + term, module);

	}

	public static void indexContent(LocalDispatcher dispatcher, GenericDelegator delegator, Map context, GenericValue content, String path) throws Exception {
		String indexAllPath = getIndexPath(path);
		IndexWriter writer = null;
		try {
		   	writer = new IndexWriter(indexAllPath, new StandardAnalyzer(), false);
	                if (Debug.infoOn()) Debug.logInfo("Used old directory:" + indexAllPath, module);
		} catch(FileNotFoundException e) {
		   	writer = new IndexWriter(indexAllPath, new StandardAnalyzer(), true);
	                if (Debug.infoOn()) Debug.logInfo("Created new directory:" + indexAllPath, module);
		}
		
		indexContent(dispatcher, delegator, context, content, writer);
       	writer.optimize();
    	writer.close();
	}
	
	public static void indexContent(LocalDispatcher dispatcher, GenericDelegator delegator, Map context, GenericValue content, IndexWriter writer) throws Exception {
	    Document doc = ContentDocument.Document(content, context, dispatcher);
	    //if (Debug.infoOn()) Debug.logInfo("in indexContent, content:" + content, module);
            if (doc != null) {
                writer.addDocument(doc);
                Integer goodIndexCount = (Integer)context.get("goodIndexCount");
                int newCount = goodIndexCount.intValue() + 1;
                Integer newIndexCount = new Integer(newCount);
                context.put("goodIndexCount", newIndexCount);
            }
            /*
            String dataResourceId = content.getString("dataResourceId");
            if (UtilValidate.isNotEmpty(dataResourceId)) {
                indexDataResource(delegator, context, dataResourceId, writer);
            }
            */
        
	}
	
	public static void indexDataResource(GenericDelegator delegator, Map context, String id) throws Exception {
		String path = null;
		indexDataResource(delegator, context, id, path );
	}
	
	public static void indexDataResource(GenericDelegator delegator, Map context, String id, String path) throws Exception {
		String indexAllPath = getIndexPath(path);
		IndexWriter writer = null;
		try {
		    writer = new IndexWriter(indexAllPath, new StandardAnalyzer(), false);
		} catch(FileNotFoundException e) {
		    writer = new IndexWriter(indexAllPath, new StandardAnalyzer(), true);
		}	
		indexDataResource(delegator, context, id, writer);
	    writer.optimize();
            writer.close();

	}

	public static void indexDataResource(GenericDelegator delegator, Map context, String id, IndexWriter writer) throws Exception {
	    Document doc = DataResourceDocument.Document(id, delegator, context);
	    writer.addDocument(doc);
	}
	
	public static String getIndexPath(String path) {
		String indexAllPath = path;
		if (UtilValidate.isEmpty(indexAllPath))
			indexAllPath = UtilProperties.getPropertyValue("search", "defaultIndex");
		if (UtilValidate.isEmpty(indexAllPath))
			indexAllPath = "index";
		return indexAllPath;

	}
}
