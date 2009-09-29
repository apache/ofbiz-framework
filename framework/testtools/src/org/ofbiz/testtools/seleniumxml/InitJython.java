/*
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
 */
package org.ofbiz.testtools.seleniumxml;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


import org.apache.log4j.Logger;

import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;
import org.ofbiz.testtools.seleniumxml.InitJython;

/**
 *
 */
public class InitJython {

//     public Logger  logger = Logger.getLogger(InitJython.class.getName());
     
     private static PythonInterpreter SINGLETON;
             
     /** Only allow creation through the factory method */
     protected InitJython() {
//        logger.setLevel(Level.DEBUG);
     } 
    
     /**
      * getInterpreter initializes the Python environment the first time.  It then issues a 
      * new Interpreter for each request.
      * @return PythonInterpreter
     */
    public static PythonInterpreter getInterpreter() {
         
         if (SINGLETON == null) {
             synchronized (InitJython.class) {
                 Properties props = System.getProperties();
                 //String ofbizHome = props.getProperty("ofbiz.home");
                    
                 Properties pyProps = new Properties();
                 
                 if( props.getProperty("python.home") == null) {
                     //pyProps.setProperty("python.home", "c:/devtools/jython2.2rc2");
                     pyProps.setProperty("python.home", "c:/devtools/Python24");
                 } 
                   
                 //Debug.logInfo(props.toString(), module);
                 ClassLoader loader = Thread.currentThread().getContextClassLoader();
                 PySystemState.initialize(props, pyProps, new String[0], loader);
                 
                 SINGLETON  =  new PythonInterpreter();
                 
                 SINGLETON.exec("import sys");  
                 SINGLETON.exec("sys.path.append(\"c:/dev/ag/seleniumXml/plugins\")");  
             }
         }
         
         return SINGLETON;
     }
}
