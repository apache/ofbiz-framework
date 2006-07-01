/*
 * $Id: DataResourceDocument.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 *  Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 *  CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 *  OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 *  THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
 * 
 * @author <a href="mailto:byersa@automationgroups.com">Al Byers</a>
 * @version $Rev$
 * @since 3.1
 * 
 *  
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
	  	    DataResourceWorker.writeDataResourceTextCache(dataResource, mimeTypeId, locale, context, delegator, outWriter);
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
