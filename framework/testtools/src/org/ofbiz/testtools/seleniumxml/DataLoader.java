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

import java.util.List;
import java.util.Map;

import javolution.util.FastMap;

import org.jdom.Element;
import org.python.core.PyArray;
import org.python.core.PyDictionary;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;
import org.ofbiz.testtools.seleniumxml.InitJython;
import org.ofbiz.testtools.seleniumxml.SeleniumXml;

public class DataLoader {

    private String file;
    private String iterations;
    private SeleniumXml parent;
    private SeleniumXml currentTest;
    private List<Element> children;
    
    private int currentRowIndx;
    
    
    //Objects initialized from csvreader script.
    private PyDictionary fieldNameMap;
    private PyList dataList;
    private PyList fieldNames;
    
    public DataLoader(String file, String iterations, SeleniumXml parent, List<Element> children) {
        super();
        this.file = file;
        this.iterations = iterations;
        this.parent = parent;
        this.children = children;
        initData();
    }

    private void initData() {
        // Run the python script
        // Read header and get record count
        PythonInterpreter interp = InitJython.getInterpreter();

        Map<String, Object> map = FastMap.newInstance();
        map.put("file", this.file);
        interp.set("params", map);
    
        interp.exec("from csvreader import CSVReader");
        String cmd = "reader = CSVReader('" + this.file + "')";
        interp.exec(cmd);
        this.dataList = (PyList) interp.eval("reader.dataList");
        this.fieldNames = (PyList) interp.eval("reader.fieldNames");
        this.fieldNameMap = (PyDictionary) interp.eval("reader.fieldNameMap");
        //interp.execfile("c:/dev/ag/seleniumxml/plugins/csvreader.py");
        //interp.execfile("c:/dev/ag/seleniumxml/plugins/TestCSVReader.py");
       
        //Now get output from script
        //this.dataList = (PyArray) map.get("dataList");
        //this.fieldNames = (PyDictionary) map.get("fieldNames");
        
    }
    
    private void next() {
        this.currentRowIndx = (this.currentRowIndx + 1) % this.dataList.__len__();
    }
    
    private void loadData() {

        int size = this.fieldNames.__len__();
        for(int i=0; i<size; i++ ) {
            PyObject name = this.fieldNames.__getitem__(i);
            PyObject valueList = this.dataList.__getitem__(this.currentRowIndx);
            PyObject columnIndx = this.fieldNameMap.__getitem__(name);
            Integer convIndx = (Integer) columnIndx.__tojava__(Integer.class);
            //int convIndx = Integer.parseInt((String) columnIndx.__tojava__(String.class));
            PyObject value = valueList.__getitem__(convIndx);
            this.currentTest.addParam((String) name.__tojava__(String.class), (String) value.__tojava__(String.class));
        }
        
    }
    
    public void runTest() throws TestCaseException {

        //Depending on the iteration instruction repeat the following until complete
        int iter = Integer.parseInt(this.iterations);

        //Iterate through entire list of data
        if(iter == -1) {
            iter = this.dataList.__len__();
        }
    
        this.currentTest = new SeleniumXml(this.parent);
        for( int i=0; i<iter; i++) {
            loadData();
            currentTest.runCommands(this.children);
            next();
        }
        
    }
}
