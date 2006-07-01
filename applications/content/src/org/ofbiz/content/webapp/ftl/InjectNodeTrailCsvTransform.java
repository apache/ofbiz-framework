/*
 * $Id: InjectNodeTrailCsvTransform.java 5462 2005-08-05 18:35:48Z jonesde $
 * 
 * Copyright (c) 2001-2005 The Open For Business Project - www.ofbiz.org
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *  
 */
package org.ofbiz.content.webapp.ftl;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.base.util.template.FreeMarkerWorker;
import org.ofbiz.content.content.ContentWorker;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.webapp.ftl.LoopWriter;

import freemarker.core.Environment;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateTransformModel;
import freemarker.template.TransformControl;

/**
 * InjectNodeTrailCsvTransform - Freemarker Transform for URLs (links)
 * 
 * @author <a href="mailto:byersa@automationgroups.com">Al Byers</a>
 * @version $Rev$
 * @since 3.0
 */
public class InjectNodeTrailCsvTransform implements TemplateTransformModel {

    public static final String module = InjectNodeTrailCsvTransform.class.getName();

    public static final String [] saveKeyNames = {"nodeTrailCsv","globalNodeTrail", "nodeTrail"};
    public static final String [] removeKeyNames = {"nodeTrailCsv"};

    /**
     * A wrapper for the FreeMarkerWorker version.
     */
    public static Object getWrappedObject(String varName, Environment env) {
        return FreeMarkerWorker.getWrappedObject(varName, env);
    }

    public static String getArg(Map args, String key, Environment env) {
        return FreeMarkerWorker.getArg(args, key, env);
    }

    public static String getArg(Map args, String key, Map ctx) {
        return FreeMarkerWorker.getArg(args, key, ctx);
    }


    public Writer getWriter(final Writer out, Map args) {
        final StringBuffer buf = new StringBuffer();
        final Environment env = Environment.getCurrentEnvironment();
        final Map templateCtx = (Map) FreeMarkerWorker.getWrappedObject("context", env);
        //FreeMarkerWorker.convertContext(templateCtx);
        final GenericDelegator delegator = (GenericDelegator) FreeMarkerWorker.getWrappedObject("delegator", env);
        final HttpServletRequest request = (HttpServletRequest) FreeMarkerWorker.getWrappedObject("request", env);
        final GenericValue userLogin = (GenericValue) FreeMarkerWorker.getWrappedObject("userLogin", env);
        FreeMarkerWorker.getSiteParameters(request, templateCtx);
        FreeMarkerWorker.overrideWithArgs(templateCtx, args);

        return new LoopWriter(out) {

            final String passedCsv = (String)templateCtx.get("nodeTrailCsv");

            public void write(char cbuf[], int off, int len) {
                buf.append(cbuf, off, len);
            }

            public void flush() throws IOException {
                out.flush();
            }

            public int onStart() throws TemplateModelException, IOException {
                String csvTrail = null;

                List trail = (List)templateCtx.get("globalNodeTrail");

                if (Debug.infoOn()) Debug.logInfo("in InjectNodeTrailCsv(0), trail:"+trail,module);
                // This will build a nodeTrail if none exists
                // Maybe only contentId or subContentId are passed in
                //GenericValue currentValue = getCurrentContent( delegator, trail,  userLogin, templateCtx, nullThruDatesOnly, contentAssocPredicateId);
                String redo = (String)templateCtx.get("redo");

                if (trail == null || trail.size() == 0 || (redo != null && redo.equalsIgnoreCase("true"))) {
                    String thisContentId = null;
                    String subContentId = (String)templateCtx.get("subContentId");
                    if (Debug.infoOn()) Debug.logInfo("in InjectNodeTrailCsv(0), subContentId:"+subContentId,module);
                    String contentId = (String)templateCtx.get("contentId");
                    if (Debug.infoOn()) Debug.logInfo("in InjectNodeTrailCsv(0), contentId:"+contentId,module);
                    String contentAssocTypeId = (String)templateCtx.get("contentAssocTypeId");
                    if (Debug.infoOn()) Debug.logInfo("in InjectNodeTrailCsv(0), contentAssocTypeId:"+contentAssocTypeId,module);
                    try {
                        if (UtilValidate.isNotEmpty(subContentId)) {
                            csvTrail = ContentWorker.getContentAncestryNodeTrailCsv(delegator, subContentId, contentAssocTypeId, "to");                     
                            if (UtilValidate.isNotEmpty(csvTrail))
                                csvTrail += ",";
                            csvTrail += subContentId;
                        } else if (UtilValidate.isNotEmpty(contentId)) {
                            csvTrail = ContentWorker.getContentAncestryNodeTrailCsv(delegator, contentId, contentAssocTypeId, "to");                     
                            if (UtilValidate.isNotEmpty(csvTrail))
                                csvTrail += ",";
                            csvTrail += contentId;
                        }
                    } catch (GenericEntityException e) {
                        throw new RuntimeException("Error getting current content. " + e.toString());
                    }
                    if (Debug.infoOn()) Debug.logInfo("in InjectNodeTrailCsv(0), csvTrail:"+csvTrail,module);
                } else {
                    // Build nodeTrail if one does not exist
                    //if (Debug.infoOn()) Debug.logInfo("in InjectNodeTrailCsv, trail:"+trail,module);
                    //if (Debug.infoOn()) Debug.logInfo("in InjectNodeTrailCsv, passedCsv:"+passedCsv,module);
                    if (UtilValidate.isNotEmpty(passedCsv)) {
                        csvTrail = passedCsv;
                        int lastComma = passedCsv.lastIndexOf(",");
                        String lastPassedContentId = null;
                        if (lastComma >= 0) { 
                            lastPassedContentId = passedCsv.substring(lastComma + 1);
                        } else {
                            lastPassedContentId = passedCsv;
                        }
    
                        if (UtilValidate.isNotEmpty(lastPassedContentId)) {
                            if (trail != null && trail.size() > 0) {
                                Map nd = (Map)trail.get(0);
                                String firstTrailContentId = (String)nd.get("contentId");
                                if (UtilValidate.isNotEmpty(firstTrailContentId)
                                    && UtilValidate.isNotEmpty(lastPassedContentId)
                                    && firstTrailContentId.equals(lastPassedContentId) ) {
                                    csvTrail += "," + ContentWorker.nodeTrailToCsv(trail.subList(1, trail.size()));
                                } else {
                                    csvTrail += "," + ContentWorker.nodeTrailToCsv(trail);
                                }
                            }
                        }
                    } else {
                        csvTrail = ContentWorker.nodeTrailToCsv(trail);
                    }
                }
                //if (Debug.infoOn()) Debug.logInfo("in InjectNodeTrailCsv, csvTrail:"+csvTrail,module);
                templateCtx.put("nodeTrailCsv", csvTrail);
                return TransformControl.EVALUATE_BODY;
            }


            public void close() throws IOException {
                templateCtx.put("nodeTrailCsv", passedCsv);
                String wrappedContent = buf.toString();
                out.write(wrappedContent);
            }
        };
    }
}
