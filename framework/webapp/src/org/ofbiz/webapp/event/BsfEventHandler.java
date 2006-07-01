/*
 * $Id: BsfEventHandler.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 2003 The Open For Business Project - www.ofbiz.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT
 * OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package org.ofbiz.webapp.event;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.bsf.BSFException;
import com.ibm.bsf.BSFManager;
import com.ibm.bsf.util.IOUtils;

import org.ofbiz.base.util.cache.UtilCache;

/**
 * BsfEventHandler - BSF Event Handler
 *
 * @author     <a href="mailto:jaz@ofbiz.org">Andy Zeneski</a>
 * @version    $Rev$
 * @since      2.2
 */
public class BsfEventHandler implements EventHandler {
    
    public static final String module = BsfEventHandler.class.getName();    
    public static UtilCache eventCache = new UtilCache("webapp.BsfEvents");

    /**
     * @see org.ofbiz.webapp.event.EventHandler#init(javax.servlet.ServletContext)
     */
    public void init(ServletContext context) throws EventHandlerException {
    }

    /**
     * @see org.ofbiz.webapp.event.EventHandler#invoke(java.lang.String, java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    public String invoke(String eventPath, String eventMethod, HttpServletRequest request, HttpServletResponse response) throws EventHandlerException {
        ServletContext context = (ServletContext) request.getAttribute("servletContext");
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null)
            cl = this.getClass().getClassLoader();
        
        if (context == null)
            throw new EventHandlerException("Problem getting ServletContext");
                   
        try {      
            // create the BSF manager  
            BSFManager bsfManager = new BSFManager();
            bsfManager.setClassLoader(cl);
            
            // expose the event objects to the script
            bsfManager.declareBean("request", request, HttpServletRequest.class);
            bsfManager.declareBean("response", response, HttpServletResponse.class);            
            
            // get the script type
            String scriptType = BSFManager.getLangFromFilename(eventMethod);
            
            // load the script                        
            InputStream scriptStream = null; 
            String scriptString = null;   
            String cacheName = null;        
            if (eventPath == null || eventPath.length() == 0) {
                // we are a resource to be loaded off the classpath
                cacheName = eventMethod;
                scriptString = (String) eventCache.get(cacheName);
                if (scriptString == null) {
                    synchronized(this) {
                        if (scriptString == null) {
                            scriptStream = cl.getResourceAsStream(eventMethod);
                            scriptString = IOUtils.getStringFromReader(new InputStreamReader(scriptStream));
                            eventCache.put(cacheName, scriptString);
                        }
                    }
                }
                                
            } else {
                // we are a script in the webapp - load by resource
                cacheName = context.getServletContextName() + ":" + eventPath + eventMethod;
                scriptString = (String) eventCache.get(cacheName);
                if (scriptString == null) {
                    synchronized(this) {
                        if (scriptString == null) {                                  
                            scriptStream = context.getResourceAsStream(eventPath + eventMethod);
                            scriptString = IOUtils.getStringFromReader(new InputStreamReader(scriptStream));
                            eventCache.put(cacheName, scriptString);
                        }
                    }
                }                                                
            }                    
                                                                                     
            // execute the script
            Object result = bsfManager.eval(scriptType, cacheName, 0, 0, scriptString);
            
            // check the result
            if (result != null && !(result instanceof String)) {
                throw new EventHandlerException("Event did not return a String result, it returned a " + result.getClass().getName());           
            }
            
            return (String) result;
                                 
        } catch(BSFException e) {        
            throw new EventHandlerException("BSF Error", e);
        } catch (IOException e) {
            throw new EventHandlerException("Problems reading script", e);     
        }                
    }
}
