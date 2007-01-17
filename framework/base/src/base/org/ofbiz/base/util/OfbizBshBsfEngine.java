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
package org.ofbiz.base.util;

/*
 * Based on code that started with this header/license:
        This file is associated with the BeanShell Java Scripting language
        distribution (http://www.beanshell.org/).
 
        This file is hereby placed into the public domain...  You may copy,
        modify, and redistribute it without restriction.
 
 */

import java.io.StringReader;
import java.util.Vector;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.InterpreterError;
import bsh.TargetError;

import org.apache.bsf.BSFDeclaredBean;
import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;
import org.apache.bsf.util.BSFEngineImpl;

import org.ofbiz.base.util.cache.UtilCache;

/**
 * This is the BeanShell adapter for IBM's Bean Scripting Famework.
 * It is an implementation of the BSFEngine class, allowing BSF aware
 * applications to use BeanShell as a scripting language.
 * <p>
 *
 * I believe this implementation is complete (with some hesitation about the
 * the usefullness of the compileXXX() style methods - provided by the base
 * utility class).
 *
 */
public class OfbizBshBsfEngine extends BSFEngineImpl {
    
    public static final String module = OfbizBshBsfEngine.class.getName();
    
    protected Interpreter interpreter;
    protected boolean installedApplyMethod;
    
    public static UtilCache parsedScripts = new UtilCache("script.BshBsfParsedCache", 0, 0, false);
    
    public void initialize(BSFManager mgr, String lang, Vector declaredBeans) throws BSFException {
        super.initialize(mgr, lang, declaredBeans);
        
        interpreter = BshUtil.getMasterInterpreter(null);
        
        // declare the bsf manager for callbacks, etc.
        try {
            interpreter.set("bsf", mgr);
        } catch (EvalError e) {
            throw new BSFException("bsh internal error: "+e.toString());
        }
        
        for(int i=0; i<declaredBeans.size(); i++) {
            BSFDeclaredBean bean = (BSFDeclaredBean)declaredBeans.get(i);
            declareBean(bean);
        }
    }
    
    public void setDebug(boolean debug) {
        Interpreter.DEBUG=debug;
    }
    
    /**
     * Invoke method name on the specified bsh scripted object.
     * The object may be null to indicate the global namespace of the
     * interpreter.
     * @param object may be null for the global namespace.
     */
    public Object call(Object object, String name, Object[] args) throws BSFException {
        if (object == null) {
            try {
                object = interpreter.get("global");
            } catch (EvalError e) {
                throw new BSFException("bsh internal error: "+e.toString());
            }
        }
        
        if (object instanceof bsh.This) {
            try {
                return ((bsh.This)object).invokeMethod(name, args);
            } catch (InterpreterError e) {
                throw new BSFException("BeanShell interpreter internal error: "+e);
            } catch (TargetError e2) {
                throw new BSFException("The application script threw an exception: " + e2.getTarget());
            } catch (EvalError e3) {
                throw new BSFException("BeanShell script error: "+e3);
            }
        } else {
            throw new BSFException("Cannot invoke method: " + name +
            ". Object: "+object +" is not a BeanShell scripted object.");
        }
    }
    
    
    /**
     * A helper BeanShell method that implements the anonymous method apply
     * proposed by BSF.  Note that the script below could use the standard
     * bsh eval() method to set the variables and apply the text, however
     * then I'd have to escape quotes, etc.
     */
    final static String bsfApplyMethod =
    "_bsfApply(_bsfNames, _bsfArgs, _bsfText) {"
    +"for(i=0;i<_bsfNames.length;i++)"
    +"this.namespace.setVariable(_bsfNames[i], _bsfArgs[i]);"
    +"return this.interpreter.eval(_bsfText, this.namespace);"
    +"}";
    
    /**
     * This is an implementation of the BSF apply() method.
     * It exectutes the funcBody text in an "anonymous" method call with
     * arguments.
     */
    public Object apply(String source, int lineNo, int columnNo, Object funcBody, Vector namesVec, Vector argsVec) throws BSFException {
        if (namesVec.size() != argsVec.size()) throw new BSFException("number of params/names mismatch");
        if (!(funcBody instanceof String)) throw new BSFException("apply: function body must be a string");
        
        String [] names = new String [ namesVec.size() ];
        namesVec.copyInto(names);
        Object [] args = new String [ argsVec.size() ];
        argsVec.copyInto(args);
        
        try {
            if (!installedApplyMethod) {
                interpreter.eval(bsfApplyMethod);
                installedApplyMethod = true;
            }
            
            bsh.This global = (bsh.This)interpreter.get("global");
            return global.invokeMethod("_bsfApply", new Object [] { names, args, (String)funcBody });
            
        } catch (InterpreterError e) {
            throw new BSFException("BeanShell interpreter internal error: " + e + sourceInfo(source,lineNo,columnNo));
        } catch (TargetError e2) {
            throw new BSFException("The application script threw an exception: " + e2.getTarget() + sourceInfo(source,lineNo,columnNo));
        } catch (EvalError e3) {
            throw new BSFException("BeanShell script error: " + e3 + sourceInfo(source,lineNo,columnNo));
        }
    }
    
    public Object eval(String source, int lineNo, int columnNo, Object expr) throws BSFException {
        if (!(expr instanceof String)) throw new BSFException("BeanShell expression must be a string");
        
        try {
            //return interpreter.eval(((String) expr));
            
            Interpreter.ParsedScript script = null;
            
            if (source != null && source.length() > 0) {
                script = (Interpreter.ParsedScript) parsedScripts.get(source);
                if (script == null) {
                    synchronized (OfbizBshBsfEngine.class) {
                        script = (Interpreter.ParsedScript) parsedScripts.get(source);
                        if (script == null) {
                            script = interpreter.parseScript(source, new StringReader((String) expr));
                            Debug.logVerbose("Caching BSH script at: " + source, module);
                            parsedScripts.put(source, script);
                        }
                    }
                }
            } else {
                script = interpreter.parseScript(source, new StringReader((String) expr));
            }
            
            return interpreter.evalParsedScript(script);
        } catch (InterpreterError e) {
            throw new BSFException("BeanShell interpreter internal error: "+ e + sourceInfo(source,lineNo,columnNo));
        } catch (TargetError e2) {
            Debug.logError(e2, "Error thrown in BeanShell script called through BSF at: " + sourceInfo(source,lineNo,columnNo), module);
            //Debug.logError(e2.getTarget(), module);
            throw new BSFException("The application script threw an exception: " + e2 + " " + sourceInfo(source,lineNo,columnNo));
        } catch (EvalError e3) {
            throw new BSFException("BeanShell script error: " + e3 + sourceInfo(source,lineNo,columnNo));
        }
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
        try {
            interpreter.set(bean.name, bean.bean);
        } catch (EvalError e) {
            throw new BSFException("error declaring bean: " + bean.name + " : " + e.toString());
        }
    }
    
    public void undeclareBean(BSFDeclaredBean bean) throws BSFException {
        try {
            interpreter.unset(bean.name);
        } catch (EvalError e) {
            throw new BSFException("bsh internal error: " + e.toString());
        }
    }
    
    public void terminate() { }
    
    private String sourceInfo(String source, int lineNo, int columnNo) {
        return "BSF info: " + source + " at line: " + lineNo +" column: " + columnNo;
    }
}
