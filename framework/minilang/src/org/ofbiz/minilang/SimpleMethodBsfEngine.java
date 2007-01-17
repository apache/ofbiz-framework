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
package org.ofbiz.minilang;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.ofbiz.base.util.Debug;
import org.ofbiz.minilang.method.MethodContext;

import org.apache.bsf.BSFDeclaredBean;
import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;
import org.apache.bsf.util.BSFEngineImpl;

/**
 * <P>This is the OFBiz MiniLang SimpleMethod adapter for IBM's Bean Scripting Famework.
 * It is an implementation of the BSFEngine class, allowing BSF aware
 * applications to use SimpleMethod as a scripting language.
 * 
 * <P>There should only be ONE simple-method in the XML file and it will be run as an event.
 */
public class SimpleMethodBsfEngine extends BSFEngineImpl {
    
    public static final String module = SimpleMethodBsfEngine.class.getName();
    
    protected Map context = new HashMap();
    
    public void initialize(BSFManager mgr, String lang, Vector declaredBeans) throws BSFException {
        super.initialize(mgr, lang, declaredBeans);
        
        // declare the bsf manager for callbacks, etc.
        context.put("bsf", mgr);
        
        for(int i=0; i<declaredBeans.size(); i++) {
            BSFDeclaredBean bean = (BSFDeclaredBean)declaredBeans.get(i);
            declareBean(bean);
        }
    }
    
    public void setDebug(boolean debug) {
        //interpreter.DEBUG=debug;
    }
    
    /**
     * Invoke method name on the specified scripted object.
     * The object may be null to indicate the global namespace of the
     * interpreter.
     * @param object may be null for the global namespace.
     */
    public Object call(Object object, String name, Object[] args) throws BSFException {
        throw new BSFException("The call method is not yet supported for SimpleMethods");
    }
    
    
    /**
     * This is an implementation of the apply() method.
     * It exectutes the funcBody text in an "anonymous" method call with
     * arguments.
     */
    public Object apply(String source, int lineNo, int columnNo, Object funcBody, Vector namesVec, Vector argsVec) throws BSFException {
        //if (namesVec.size() != argsVec.size()) throw new BSFException("number of params/names mismatch");
        //if (!(funcBody instanceof String)) throw new BSFException("apply: function body must be a string");
        
        throw new BSFException("The apply method is not yet supported for simple-methods");
    }
    
    public Object eval(String source, int lineNo, int columnNo, Object expr) throws BSFException {
        if (!(expr instanceof String)) throw new BSFException("simple-method expression must be a string");

        //right now only supports one method per file, so get all methods and just run the first...
        Map simpleMethods = null;
        try {
            simpleMethods = SimpleMethod.getDirectSimpleMethods(source, (String) expr, "<bsf source>");
        } catch (MiniLangException e) {
            throw new BSFException("Error loading/parsing simple-method XML source: " + e.getMessage());
        }
        Set smNames = simpleMethods.keySet();
        if (smNames.size() == 0) throw new BSFException("Did not find any simple-methods in the file");

        String methodName = (String) smNames.iterator().next();
        if (smNames.size() > 1) Debug.logWarning("Found more than one simple-method in the file, running the [" + methodName + "] method, you should remove all but one method from this file", module);

        SimpleMethod simpleMethod = (SimpleMethod) simpleMethods.get(methodName);
        MethodContext methodContext = new MethodContext(context, null, MethodContext.EVENT);
        return simpleMethod.exec(methodContext);
        //methodContext.getResults();
    }
    
    
    public void exec(String source, int lineNo, int columnNo, Object script) throws BSFException {
        eval(source, lineNo, columnNo, script);
    }
    
    
/*
        public void compileApply (String source, int lineNo, int columnNo,
                Object funcBody, Vector paramNames, Vector arguments, CodeBuffer cb)
                throws BSFException;
 
        public void compileExpr (String source, int lineNo, int columnNo,
                Object expr, CodeBuffer cb) throws BSFException;
 
        public void compileScript (String source, int	lineNo,	int columnNo,
                Object script, CodeBuffer cb) throws BSFException;
 */
    
    public void declareBean(BSFDeclaredBean bean) throws BSFException {
        context.put(bean.name, bean.bean);
    }
    
    public void undeclareBean(BSFDeclaredBean bean) throws BSFException {
        context.remove(bean.name);
    }
    
    public void terminate() { }
    
    private String sourceInfo(String source, int lineNo, int columnNo) {
        return "SimpleMethod: " + source + " at line: " + lineNo +" column: " + columnNo;
    }
}
