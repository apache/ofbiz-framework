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
import java.io.StringWriter;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.content.data.DataResourceWorker;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

/**
 * DataResourceDocument Class
 */

public class DataResourceDocument {
	static char dirSep = System.getProperty("file.separator").charAt(0);
    public static final String module = ContentDocument.class.getName();
	
	public static Document Document(String id, GenericDelegator delegator, Map context) throws InterruptedException  {
	  	
		Document doc = null;
		GenericValue dataResource = null;
	  	try {
	  		dataResource = delegator.findByPrimaryKeyCache("DataResource", UtilMisc.toMap("dataResourceId",id));
	  	} catch(GenericEntityException e) {
	  		Debug.logError(e, module);
	  		return doc;
	  	}
	  	// make a new, empty document
	  	doc = new Document();
	  	
	  	doc.add(Field.Keyword("dataResourceId", id));
	  	
	  	String mimeTypeId = dataResource.getString("mimeTypeId");
	    if (UtilValidate.isEmpty(mimeTypeId)) {
            mimeTypeId = "text/html";
        }

	    Locale locale = Locale.getDefault();
        String currentLocaleString = dataResource.getString("localeString");
        if (UtilValidate.isNotEmpty(currentLocaleString)) {
            locale = UtilMisc.parseLocale(currentLocaleString);
        }
        
        StringWriter outWriter = new StringWriter();
	  	try {
	  	    DataResourceWorker.writeDataResourceText(dataResource, mimeTypeId, locale, context, delegator, outWriter, true);
	  	} catch(GeneralException e) {
	  		Debug.logError(e, module);
	  	} catch(IOException e) {
	  		Debug.logError(e, module);
	  	}
	  	String text = outWriter.toString();
	  	Debug.logInfo("in DataResourceDocument, text:" + text, module);
                if (UtilValidate.isNotEmpty(text)) 
	  	    doc.add(Field.UnStored("content", text));
	    
	    return doc;
	}

}
