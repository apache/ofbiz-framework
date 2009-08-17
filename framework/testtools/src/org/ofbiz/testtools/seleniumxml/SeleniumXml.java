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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.reflect.*;

import junit.framework.Assert;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import org.ofbiz.testtools.seleniumxml.util.TestUtils;

import com.thoughtworks.selenium.DefaultSelenium;
//import com.thoughtworks.selenium.SeleniumException;


public class SeleniumXml {
	
	public static final String PROPS_NAME = "selenium.config";
	Logger  logger = Logger.getLogger(SeleniumXml.class.getName());

	public static final int MAX_STR_LENGTH = 15;
	static String testPath;
	private Map <String, Object> map;
	private Document doc;
	private DefaultSelenium sel;
	private static Properties props;
    private String testSourcePath;
    private String username;
    private String password;
	
	public static void main(String[] args) throws JDOMException, IOException{
		if (args.length == 0) {
			System.out.println("Please include a path for the selenium XML test file.");
		} else {
            SeleniumXml sel = new SeleniumXml();
            for (String arg: args) {
                if (arg.startsWith("-username")) {
                    sel.username = arg.substring(10);
                } else if (arg.startsWith("-password")) {
                    sel.password = arg.substring(10);
                } else {
                    sel.testSourcePath = arg;
                }
            }
			sel.runTest(args[0]);
		}
	}
	
	public SeleniumXml() throws IOException {
		this.map = new HashMap<String, Object>();
		if (props == null) {
			props = new Properties();
			initConfig();
		}
		logger.setLevel(Level.DEBUG);
	}

	private static void initConfig() throws IOException {
		try {
			String configFile = System.getProperty(PROPS_NAME);
			if (configFile == null) {
				String errMsg = "The Java environment (-Dxxx=yyy) variable with name " + PROPS_NAME + " is not set, cannot resolve location.";
				throw new MalformedURLException(errMsg);
			}
			BasicConfigurator.configure();
			InputStream in = new FileInputStream(configFile);
			props.load(in);
			in.close();


		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	/**
	 * Constructor to preset with an existing Map of parameters.  Intended to be used 
	 * for nested Selenium tests.
	 * @param map
	 */
	public SeleniumXml(SeleniumXml selenium) {
		this.sel = selenium.getSelenium();
		this.map = selenium.getParameterMap();
	}

	public DefaultSelenium getSelenium() {
		return this.sel;
	}
	public Map <String, Object> getParameterMap() {
		return this.map;
	}
	public void runTest(String fileName) throws JDOMException, IOException {
		readFile(fileName);
		setupSelenium();
		runCommands();
	}

	public void runCommands() {
		Element root = this.doc.getRootElement();
		List<Element> nodes = root.getChildren();
		runCommands(nodes);
	}
	
	public void runCommands(List<Element> nodes) {
		
		for(Element elem: nodes) {
			if ("type" == elem.getName()) {
				typeCmd(elem);
				
			} else if ("clickAt" == elem.getName()) {
				clickAt(elem);
			} else if ("waitForValue" == elem.getName()) {
				waitForValue(elem);
			} else if ("waitForCondition" == elem.getName()) {
				waitForCondition(elem);
			} else if ("loadData" == elem.getName()) {
				loadData(elem);
            } else if ("loadData" == elem.getName()) {
                loadData(elem);
            } else if ("jythonRunner" == elem.getName()) {
            	jythonRunner(elem);
            } else if ("groovyRunner" == elem.getName()) {
            	groovyRunner(elem);
            } else if ("dataLoop" == elem.getName()) {
                dataLoop(elem);
            } else if ("remoteRequest" == elem.getName()) {
                remoteRequest(elem);
			} else if ("selectPopup" == elem.getName()) {
				selectPopup(elem);
			} else if ("getAllWindowIds" == elem.getName()) {
				getAllWindowIds(elem);
			} else if ("captureTextInPage" == elem.getName()) {
				captureTextInPageCmd(elem);
			} else if ("getSelectedLabel" == elem.getName()) {
				getSelectedLabel(elem);
			} else if ("getSelectedValue" == elem.getName()) {
				getSelectedValue(elem);
			} else if ("getSelectedId" == elem.getName()) {
				getSelectedId(elem);
			} else if ("testcase" == elem.getName()) {
				testcase(elem);
			} else if ("assertContains" == elem.getName()) {
				assertContains(elem);
			} else if ("getHtmlSource" == elem.getName()) {
				getHtmlSource(elem);
			} else if ("getBodyText" == elem.getName()) {
				getBodyText(elem);
			} else if ("setup" == elem.getName()) {
				continue; //setup is handled previously
			} else if ("print" == elem.getName()) {
				printCmd(elem);
			} else if ("waitForPageToLoad" == elem.getName()) {
				waitForPageToLoadCmd(elem);
			} else if ("getSelectedIds" == elem.getName()) {
				getSelectedIdsCmd(elem);
			} else if ("copy" == elem.getName()) {
				copyCmd(elem);
			} else if ("append" == elem.getName()) {
				appendCmd(elem);
			} else if ("open" == elem.getName()) {
				openCmd(elem);
			} else if ("click" == elem.getName()) {
				clickCmd(elem);
			} else if ("select" == elem.getName()) {
				selectCmd(elem);
			} else if ("uniqueId" == elem.getName()) {
				uniqueIdCmd(elem);
			} else if ("randomAlphaString" == elem.getName()) {
				randomAlphaStringCmd(elem);
			} else if ("randomString" == elem.getName()) {
				randomStringCmd(elem);
            } else if ("setSpeed" == elem.getName()) {
                setSpeed(elem);
 			} else {
				//logger.error("Unknown SeleniumXml command found:"+elem.getName());
				//Use reflection with parameters using the naming convention param1, param2, and any return results stored 
				//in map using "out"
				logger.info("Undefined command calling by reflection for command: " + elem.getName());
				callByReflection(elem);
			}
		}
		
	}
	
	private void callByReflection(Element elem) {
		
		String methodName = elem.getName();
		//Support two parameters for all selenium RC calls
		String param1 = elem.getAttributeValue("param1");  
		String param2 = elem.getAttributeValue("param2");  
	
		Class[] paramTypes = null;
		Object[] args = null;
		if ((param1 != null)  && (param2 != null)) {
			paramTypes = new Class[] {String.class, String.class};
			args = new Object[] {param1, param2};
		} else if (param1 != null) {
            paramTypes = new Class[] {String.class};
            args = new Object[] {param1};
        } else {
            paramTypes = new Class[] {};
            args = new Object[] {};
        }

		//Capture the output name for "get" methods
		String out = elem.getAttributeValue("out");  
		
		Method m;
		try {
			m = (Method) this.sel.getClass().getDeclaredMethod(methodName, paramTypes);
			Object results = m.invoke(this.sel, args);
			
			//Add output parameter to common map
			if ((out != null) && (results != null)) {
				addParam(out, results);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error("Exception occurred when Unknown SeleniumXml command found:"+elem.getName());
			e.printStackTrace();
		}
		
		
	}
	
	public void waitForValue(Element elem) {
		
		String locator = elem.getAttributeValue("locator");
		String timeout = elem.getAttributeValue("timeout");
		String outParam = elem.getAttributeValue("out");
		
		int maxTime = Integer.parseInt(timeout);
		int maxSeconds = maxTime/1000;
		logger.debug("waitForValue: locator=" + locator + " timeout=" + timeout);
		//this.sel.waitForCondition(script, timeout);
		String foundValue = null;
		for(int second=0;; second++) {
			if (second >= maxSeconds) {
//				throw new SeleniumException("waitForValue exceeded timeout: " + maxTime);
			}
			try { 
				//getValue throws an exception if it can't find locator
				// - sleep for 1 sec and try again
				// - otherwise break as we found the value
				foundValue = sel.getValue(locator); 
				if (outParam != null) {
					this.addParam(outParam, foundValue);
				}
				break; //
			} catch (Exception e) { 
				//wait for 1 second and then resume
				try {
					Thread.sleep(1000);
				} catch (InterruptedException threadE) {
					// TODO Auto-generated catch block
					threadE.printStackTrace();
					
				}
			}
		}
	}	
	
	public void waitForCondition(Element elem) {
		
		String script = elem.getAttributeValue("script");
		String timeout = elem.getAttributeValue("timeout");
		
		logger.debug("waitForCondition: script=" + script + " timeout=" + timeout);
		this.sel.waitForCondition(script, timeout);
	}
	
    public void loadData(Element elem) {
            
            String file = elem.getAttributeValue("file");
            String iterations = elem.getAttributeValue("iterations");
            List children = elem.getChildren();
            
            DataLoader loader = new DataLoader(file, iterations, this, children);
            loader.runTest();
    }

    public void groovyRunner(Element elem) {
        
        String urlName = elem.getAttributeValue("srcUrl");
        GroovyRunner runner = new GroovyRunner(urlName, this);
        runner.runTest();
    }

    public void jythonRunner(Element elem) {
        
        String urlName = elem.getAttributeValue("srcUrl");
        JythonRunner runner = new JythonRunner(urlName, this);
        runner.runTest();
    }

    public void dataLoop(Element elem) {
        
        String dataListName = elem.getAttributeValue("dataListName");
        List children = elem.getChildren();
        
        DataLoop looper = new DataLoop(dataListName, this, children);
        looper.runTest();
    }

    public void remoteRequest(Element elem) {

        String requestUrl = elem.getAttributeValue("url");
        String host = elem.getAttributeValue("host");
        if (host == null || host.length() == 0) {
            host = props.getProperty("startUrl");
        }
        String responseHandlerMode = elem.getAttributeValue("responseHandlerMode");
        List <Element> children = elem.getChildren();
        
        RemoteRequest loader = new RemoteRequest(this, children, requestUrl, host, responseHandlerMode);
        loader.runTest();
    }

    public String getParamValue(String key) {
        return (String) this.map.get(key);
    }
    
    public Object getParamValue(Object key) {
        return this.map.get(key);
    }
    
    public void addParam(String name, String value) {
        //logger.info("addParam: name=" + name + " value="+value);
        this.map.put(name, value);
    }

    public void addParam(String name, Object value) {
        //logger.info("addParam: name=" + name + " value="+value);
        this.map.put(name, value);
    }

	private void assertContains(Element elem) {
		String src = replaceParam(elem.getAttributeValue("src"));  
		String test = replaceParam(elem.getAttributeValue("test"));  
		int indxSearch = src.indexOf(test);
		if (indxSearch == -1) {
			logger.info("assertContains didn't find " + test + " in the src");
		} else {
			logger.info("assertContains found " + test + " in the src");
		}
		Assert.assertTrue(indxSearch != -1);
		//String text = this.sel.getHtmlSource();  
	}
	
	private void selectPopup(Element elem) {
		String locator = elem.getAttributeValue("locator");  
//		String winId = elem.getAttributeValue("windowId");  
		String timeout = elem.getAttributeValue("timeout");  
		
		//this.sel.waitForPopUp(winId, timeout);
		this.sel.click(locator); 
		
		String[] winNames = this.sel.getAllWindowNames();
		this.sel.selectWindow("name=" + winNames[1]);
	}
	
	private void getAllWindowIds(Element elem) {
		String[] winIds = this.sel.getAllWindowIds();
		for(int i=0; i<winIds.length; i++) {
			logger.info("WindowId: " + winIds[i]);
		}
		String[] winNames = this.sel.getAllWindowNames();
		for(int i=0; i<winIds.length; i++) {
			logger.info("WindowName: " + winNames[i]);
		}
		
		//this.sel.selectWindow("name=" + winNames[1]);
		//System.out.println("Did we select WindowName: " + winNames[1]);
	}

	private void getWindowPopup(Element elem) {
		
	}
	/**
	 * Gets the hidden value of a list box
	 * @param elem
	 */
	private void getSelectedValue(Element elem) {
		String locator = elem.getAttributeValue("locator");  
		String out = elem.getAttributeValue("out");  
		String text = this.sel.getSelectedValue(locator);  
		logger.info("getSelectedValue: locator=" + locator + " text="+text);
		addParam(out, text);
	}
	
	/**
	 * Gets the visible (displayed) value of a list box
	 * @param elem
	 */
	private void getSelectedLabel(Element elem) {
		String locator = elem.getAttributeValue("locator");  
		String out = elem.getAttributeValue("out");  
		String text = this.sel.getSelectedLabel(locator);  
		logger.info("getSelectedValue: locator=" + locator + " text="+text);
		addParam(out, text);
	}
	private void getSelectedId(Element elem) {
		String locator = elem.getAttributeValue("locator");  
		String out = elem.getAttributeValue("out");  
		String text = this.sel.getSelectedId(locator);  
		addParam(out, text);
	}
	private void getHtmlSource(Element elem) {
		String paramName = elem.getAttributeValue("out");  
		String text = this.sel.getHtmlSource();  
		//logger.info("getHtmlsource: paramName=" + paramName + " text=" + text);
		addParam(paramName, text);
	}
	
	private void getBodyText(Element elem) {
		String paramName = elem.getAttributeValue("out");  
		String text = this.sel.getBodyText();  
		//logger.info("getBodyText: paramName=" + paramName + " text=" + text);
		addParam(paramName, text);
	}
	private void testcase(Element elem) {
		System.err.println("New testcase: " + elem.getAttributeValue("file"));
		String testFile = elem.getAttributeValue("file");
		SeleniumXml newTest = new SeleniumXml(this);
		try {
			newTest.runTest(testFile);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Testcase error for file: " + testFile);
		}
	}
	private void clickAt(Element elem) {
		logger.debug("clickAt: " + replaceParam(elem.getAttributeValue("locator")));
		String locator = elem.getAttributeValue("locator");
		String coordString = elem.getAttributeValue("coordString");
		this.sel.clickAt(locator, coordString);
	}
	/**
	 * @param elem takes a Selenium String locator.  See Javadocs for more information.  Here are some
	 * example locators:
	 * id="buttonId"  - the easiest
	 * css=input[type='submit'] - uses css selector notation
	 * xpath= <TBD>
	 * dom= <TBD>
	 */
	private void clickCmd(Element elem) {
		String locator = this.replaceParam(elem.getAttributeValue("locator"));
		logger.info("clickCmd: " + locator);
		this.sel.click(locator);
	}
	private void typeCmd(Element elem) {
		String name = elem.getAttributeValue("name");
		String value = replaceParam(elem.getAttributeValue("value"));  
		logger.info("typeCmd: id=" + name + " value=" + value);
		this.sel.type(name, value);  
	}
    /*
     * setSpeed delays the time for the next selenium command to execute
     */
    private void setSpeed(Element elem) {
        logger.info("setSpeed: " + elem.getAttributeValue("value"));
        this.sel.setSpeed(elem.getAttributeValue("value"));  
    }
	/*
	 * waitForPageToLoadCmd is the max timeout selenium will wait for a page to load.  
	 * Commands are executed immediately after the page loads therefore if the pages are 
	 * fast the test will go through the pages very quickly.  Use setSpeed if you want to 
	 * see the pages executed slower.
	 */
	private void waitForPageToLoadCmd(Element elem) {
		logger.info("waitForPageToLoadCmd: " + elem.getAttributeValue("value"));
		this.sel.waitForPageToLoad(elem.getAttributeValue("value"));  
	}
	private void openCmd(Element elem) {
		String cmd = replaceParam(elem.getAttributeValue("value"));  
		logger.info("openCmd: " + cmd);
		this.sel.open(cmd);  
        //this.sel.windowMaximize();
	}
	private void uniqueIdCmd(Element elem) {
		String paramName = elem.getAttributeValue("out");  
        String paramValue = RandomStringUtils.randomAlphanumeric(MAX_STR_LENGTH).toUpperCase();
        logger.info("uniqueIdCmd: parameter=" + paramName + " value=" + paramValue);
		addParam(paramName, paramValue);
    }
	
	/*
	 * captureText command captures the current HTML page and runs a regex to 
	 * get the specified string.  
	 *
	 * For example:  if the following string existed in a web page
	 * 
	 *   "xx <tag a=b> yy </tag> zz"
	 * 
	 *  
	 * And you wanted to capture the value in the of the XML (yy).  You would do the following;
	 * Use regxp: "<(\\S+?).*?>(.*?)</\\1>";  //The \\1 reuses group 1
	 * 
	 * <captureText regex="<(\\S+?).*?>(.*?)</\\1>" group="2" results="xmlValue" /> 
	 * 
	 * The command will find the <tag>.. and group 2 contains the 'yy' value.
	 * 
	 * Note: if 'group' is null it will default to the entire regex group.
	 */
	private void captureTextInPageCmd(Element elem) {
		
		String regex = elem.getAttributeValue("regex");  
		String group = elem.getAttributeValue("group");  
		String results = elem.getAttributeValue("results");  
		Pattern pattern = Pattern.compile(regex);
	
		String targetString = this.sel.getHtmlSource();  
		
		// Create the 'target' string we wish to interrogate.
		// Get a Matcher based on the target string.
		Matcher matcher = pattern.matcher(targetString);

		// Find all the matches.
		if (matcher.find()) {
			String resultsValue = null;
			if (group != null) {
				resultsValue = matcher.group(Integer.parseInt(group));
			} else {
				resultsValue = matcher.group();
			}
			logger.info("Found match for " + resultsValue);
			logger.debug("Using regex " + regex);
			logger.debug("Copy results to " + results);
			addParam(results, resultsValue);
		} else {
			logger.info("Didn't find results with regex: " + regex);
			
			//TODO: temporary to capture the missed string
			/*try {
			      FileWriter out = new FileWriter("c:/dev/erep/output/failure.txt");
			      BufferedWriter buffWriter = new BufferedWriter(out);
			      buffWriter.write(targetString);
			      out.flush();
			      out.close();
			} catch (IOException e) {
			      System.err.println(e);
			} */
		}
	}
	
	private void randomAlphaStringCmd(Element elem) {
		int nSize = 0;
		int nPrefixSize = 0;
		String paramName = elem.getAttributeValue("out");  
		String size = elem.getAttributeValue("size");  
		if (size != null) {
			nSize = Integer.parseInt(size);
		}
		String prefix = elem.getAttributeValue("prefix");  
		if (prefix != null) {
			nPrefixSize = prefix.length();
		}

		String paramValue = null;
		if (prefix != null) {
			paramValue = prefix + RandomStringUtils.randomAlphabetic(nSize - nPrefixSize);
		} else {
			paramValue = RandomStringUtils.randomAlphabetic(nSize);
		}
		//String paramValue = TestUtils.createRandomString(prefix, Integer.parseInt(size));
		logger.info("randomStringAlphaCmd: paramName=" + paramName + " paramValue=" + paramValue);
		addParam(paramName, paramValue);
	}
	private void randomStringCmd(Element elem) {
		String paramName = elem.getAttributeValue("out");  
		String size = elem.getAttributeValue("size");  
		String prefix = elem.getAttributeValue("prefix");  
		String paramValue = TestUtils.createRandomString(prefix, Integer.parseInt(size));
		logger.info("randomStringCmd: paramName=" + paramName + " paramValue=" + paramValue);
		addParam(paramName, paramValue);
	}
	private void getSelectedIdsCmd(Element elem) {
		logger.info("getSelectdIdsCmd: " + elem.getAttributeValue("value"));
		this.sel.getSelectedIds(elem.getAttributeValue("value"));  
	}
	private void selectCmd(Element elem) {
		String selectLocator = elem.getAttributeValue("locator");
		String optionLocator = elem.getAttributeValue("option");
		logger.info("selectCmd: selectLocator=" + selectLocator + " optionLocator=" + optionLocator);
		this.sel.select(selectLocator, optionLocator);  
	}
	private void printCmd(Element elem) {
		String value = replaceParam(elem.getAttributeValue("value"));
		logger.info("Print: " + value);
	}
	private void copyCmd(Element elem) {
		String toStr = replaceParam(elem.getAttributeValue("to")); 
		String fromStr = replaceParam(elem.getAttributeValue("from"));  
		logger.info("copyCmd: to=" + toStr + " from=" + fromStr);
		addParam(toStr, fromStr);
	}
	private void appendCmd(Element elem) {
		logger.info("appendCmd: src1=" + elem.getAttributeValue("src1") + " src2=" + elem.getAttributeValue("src2"));
		String newStr = replaceParam(elem.getAttributeValue("src1")) + replaceParam(elem.getAttributeValue("src2"));
		addParam(elem.getAttributeValue("out"), newStr);
	}

	public String replaceParam(String value) {
        StringBuffer buf = new StringBuffer();
        int end = 0;
        int start = 0;
        String replacedVal = null;
        String remainingStr = value;
		while (isParam(remainingStr)) {
            start = remainingStr.indexOf("${");
            buf.append(remainingStr.substring(end, start));
			end = remainingStr.indexOf("}");
			String paramName = remainingStr.substring(start + 2, end);
			replacedVal = getParamValue(paramName);
            if (replacedVal == null) { 
                replacedVal = "";
            }
            buf.append(replacedVal);
            remainingStr = remainingStr.substring(end + 1);
            end = 0;
		} 
        buf.append(remainingStr.substring(end));
		return buf.toString();
	}

	private boolean isParam(String value) {

		if ((value.indexOf("${") != -1) &&
			(value.indexOf("}", 1) != -1)) {
			return true;
		} 
		return false;
	}
	
	//TODO read properties file to setup selenium
	private void setupSelenium() {
		
		//return if Selenium has already been setup
		//e.g. nested selenium test cases.
		if (this.sel != null) return;
		
		String serverHost = null;
		String serverPort = null;
		String browser = null;
		String startUrl = null;
		
		//First initialize with property values
		if (props != null) { //Get setup params from property value
			
			serverHost = props.getProperty("serverHost", "localhost");
			serverPort = props.getProperty("proxyPort", "4444");
			browser = props.getProperty("browser", "*firefox");
			startUrl = props.getProperty("startUrl", "http://localhost:8080");
		}
		
		//Second over ride properties if defined in the "setup" element
		Element elem = this.doc.getRootElement().getChild("setup");
		if (elem != null) { 
		
			//Override properties if specified
			if (elem.getAttributeValue("serverHost") != null) {
				serverHost = elem.getAttributeValue("serverHost");
			}
			if (elem.getAttributeValue("serverPort") != null) {
				serverPort = elem.getAttributeValue("serverPort");
			}
			if (elem.getAttributeValue("browser") != null) {
				browser = elem.getAttributeValue("browser");
			}
			if (elem.getAttributeValue("startUrl") != null) {
				startUrl = elem.getAttributeValue("startUrl");
			}
		}
		logger.info("setup: serverHost=" + serverHost);
		logger.info("setup: serverPort=" + serverPort);
		logger.info("setup: browser=" + browser);
		logger.info("setup: startUrl=" + startUrl);
		this.sel = new DefaultSelenium(serverHost, Integer.parseInt(serverPort), browser, startUrl);
		this.sel.start();
	}
	private void readFile(String fileName) throws JDOMException, IOException {
		File xmlFile = new File(fileName);
		SAXBuilder builder = new SAXBuilder();
		this.doc = builder.build(xmlFile);
	}
    
    public String getUserName() {
        return this.username;
    }
    
    public String getPassword() {
        return this.password;
    }
    
    public Map <String, ? extends Object> getMap() {
        return this.map;
    }
}

