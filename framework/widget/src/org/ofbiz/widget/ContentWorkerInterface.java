/*
 * Copyright 2001-2006 The Apache Software Foundation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.ofbiz.widget;

import java.io.IOException;
import java.io.Writer;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ofbiz.base.util.GeneralException;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;

/**
 * ContentWorkerInterface
 * 
 * @author <a href="mailto:jonesde@ofbiz.org">David E. Jones</a>
 */
public interface ContentWorkerInterface {

    public GenericValue getCurrentContentExt(GenericDelegator delegator, List trail, GenericValue userLogin, Map ctx, Boolean nullThruDatesOnly, String contentAssocPredicateId)  throws GeneralException;

    public Map renderSubContentAsTextExt(GenericDelegator delegator, String contentId, Writer out, String mapKey, String subContentId, GenericValue subContentDataResourceView, 
            Map templateContext, Locale locale, String mimeTypeId, GenericValue userLogin, Timestamp fromDate) throws GeneralException, IOException;

    public String renderSubContentAsTextCacheExt(GenericDelegator delegator, String contentId,  String mapKey,  GenericValue subContentDataResourceView, 
            Map templateRoot, Locale locale, String mimeTypeId, GenericValue userLogin, Timestamp fromDate) throws GeneralException, IOException;

    public Map renderSubContentAsTextCacheExt(GenericDelegator delegator, String contentId, Writer out, String mapKey,  GenericValue subContentDataResourceView, 
            Map templateRoot, Locale locale, String mimeTypeId, GenericValue userLogin, Timestamp fromDate) throws GeneralException, IOException;

    public Map renderSubContentAsTextCacheExt(GenericDelegator delegator, String contentId, Writer out, String mapKey,  GenericValue subContentDataResourceView, 
            Map templateRoot, Locale locale, String mimeTypeId, GenericValue userLogin, Timestamp fromDate, Boolean nullThruDatesOnly) throws GeneralException, IOException;

    public Map renderContentAsTextExt(GenericDelegator delegator, String contentId, Writer out, Map templateContext, GenericValue view, Locale locale, String mimeTypeId) throws GeneralException, IOException;

    public String renderContentAsTextCacheExt(GenericDelegator delegator, String contentId,  Map templateContext, GenericValue view, Locale locale, String mimeTypeId) throws GeneralException, IOException;

    public Map renderContentAsTextCacheExt(GenericDelegator delegator, String contentId, Writer out, Map templateContext, GenericValue view, Locale locale, String mimeTypeId) throws GeneralException, IOException;

    public String getMimeTypeIdExt(GenericDelegator delegator, GenericValue view, Map ctx);

    public GenericValue getWebSitePublishPointExt(GenericDelegator delegator, String contentId, boolean ignoreCache) throws GenericEntityException;
}
