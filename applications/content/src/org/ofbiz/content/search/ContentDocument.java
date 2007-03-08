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

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.service.LocalDispatcher;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

/**
 * ContentDocument Class
 */

public class ContentDocument {

    static char dirSep = System.getProperty("file.separator").charAt(0);
    public static final String module = ContentDocument.class.getName();
	
	public static Document Document(String id, GenericDelegator delegator, LocalDispatcher dispatcher) throws InterruptedException  {
	  	
		Document doc = null;
		GenericValue content;
	  	try {
	  		content = delegator.findByPrimaryKeyCache("Content", UtilMisc.toMap("contentId",id));
	  	} catch(GenericEntityException e) {
	  		Debug.logError(e, module);
	  		return doc;
	  	}
	  	
        Map map = new HashMap();
	  	doc = Document(content, map, dispatcher);
        return doc;
	}
	
	public static Document Document(GenericValue content, Map context, LocalDispatcher dispatcher) throws InterruptedException {

		Document doc;
		// make a new, empty document
		doc = new Document();
		String contentId = content.getString("contentId");
		doc.add(Field.Keyword("contentId", contentId));
		// Add the last modified date of the file a field named "modified". Use
		// a
		// Keyword field, so that it's searchable, but so that no attempt is
		// made
		// to tokenize the field into words.
		Timestamp modDate = (Timestamp) content.get("lastModifiedDate");
		if (modDate == null) {
			modDate = (Timestamp) content.get("createdDate");
		}
		if (modDate != null) {
			doc.add(Field.Keyword("modified", modDate.toString()));
		}
		String contentName = content.getString("contentName");
		if (UtilValidate.isNotEmpty(contentName))
			doc.add(Field.Text("title", contentName));
		String description = content.getString("description");
		if (UtilValidate.isNotEmpty(description))
			doc.add(Field.Text("description", description));
		List ancestorList = new ArrayList();
		GenericDelegator delegator = content.getDelegator();
		ContentWorker.getContentAncestryAll(delegator, contentId, "WEB_SITE_PUB_PT", "TO", ancestorList);
		String ancestorString = StringUtil.join(ancestorList, " ");
		//Debug.logInfo("in ContentDocument, ancestorString:" + ancestorString,
		// module);
		if (UtilValidate.isNotEmpty(ancestorString)) {
			Field field = Field.UnStored("site", ancestorString);
			//Debug.logInfo("in ContentDocument, field:" + field.stringValue(),
			// module);
			doc.add(field);
		}
		boolean retVal = indexDataResource(content, doc, context, dispatcher);
		//Debug.logInfo("in DataResourceDocument, context.badIndexList:" +
		// context.get("badIndexList"), module);
		if (!retVal)
			doc = null;
		return doc;
	}

	public static boolean indexDataResource(GenericValue content, Document doc, Map context, LocalDispatcher dispatcher) {
        GenericDelegator delegator = content.getDelegator();
		String contentId = content.getString("contentId");
		//Debug.logInfo("in ContentDocument, contentId:" + contentId,
		// module);
		String dataResourceId = content.getString("dataResourceId");
		//Debug.logInfo("in ContentDocument, dataResourceId:" + dataResourceId, module);
		GenericValue dataResource;
		try {
			dataResource = delegator.findByPrimaryKeyCache("DataResource", UtilMisc.toMap("dataResourceId", dataResourceId));
		} catch (GenericEntityException e) {
			Debug.logError(e, module);
			List badIndexList = (List) context.get("badIndexList");
			badIndexList.add(contentId + " - " + e.getMessage());
			//Debug.logInfo("in DataResourceDocument, badIndexList:" + badIndexList, module);
			return false;
		}
		if (dataResource == null) {
			List badIndexList = (List) context.get("badIndexList");
			badIndexList.add(contentId + " - dataResource is null.");
			//Debug.logInfo("in DataResourceDocument, badIndexList:" + badIndexList, module);
			return false;
		}
		String mimeTypeId = dataResource.getString("mimeTypeId");
		if (UtilValidate.isEmpty(mimeTypeId)) {
			mimeTypeId = "text/html";
		}
		Locale locale = Locale.getDefault();
		String currentLocaleString = dataResource.getString("localeString");
		if (UtilValidate.isNotEmpty(currentLocaleString)) {
			locale = UtilMisc.parseLocale(currentLocaleString);
		}
		String text;
		try {
			text = ContentWorker.renderContentAsText(dispatcher, delegator, contentId, context, locale, mimeTypeId, true);
		} catch (GeneralException e) {
			Debug.logError(e, module);
			List badIndexList = (List) context.get("badIndexList");
			badIndexList.add(contentId + " - " + e.getMessage());
			//Debug.logInfo("in DataResourceDocument, badIndexList:" + badIndexList, module);
			return false;
		} catch (IOException e2) {
			Debug.logError(e2, module);
			List badIndexList = (List) context.get("badIndexList");
			badIndexList.add(contentId + " - " + e2.getMessage());
			//Debug.logInfo("in DataResourceDocument, badIndexList:" + badIndexList, module);
			return false;
		}
		//Debug.logInfo("in DataResourceDocument, text:" + text, module);
		if (UtilValidate.isNotEmpty(text)) {
			Field field = Field.UnStored("content", text);
			//Debug.logInfo("in ContentDocument, field:" + field.stringValue(), module);
			doc.add(field);
		}
		List featureDataResourceList;
		try {
			featureDataResourceList = content.getRelatedCache("ProductFeatureDataResource");
		} catch (GenericEntityException e) {
			Debug.logError(e, module);
			List badIndexList = (List) context.get("badIndexList");
			badIndexList.add(contentId + " - " + e.getMessage());
			return false;
		}
		List featureList = new ArrayList();
		Iterator iter = featureDataResourceList.iterator();
		while (iter.hasNext()) {
			GenericValue productFeatureDataResource = (GenericValue) iter .next();
			String feature = productFeatureDataResource.getString("productFeatureId");
			featureList.add(feature);
		}
		String featureString = StringUtil.join(featureList, " ");
		//Debug.logInfo("in ContentDocument, featureString:" + featureString, module);
		if (UtilValidate.isNotEmpty(featureString)) {
			Field field = Field.UnStored("feature", featureString);
			doc.add(field);
		}
		return true;
	}
}
