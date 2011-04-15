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

import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.SeleniumException;
import javolution.util.FastMap;
import junit.framework.Assert;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.ofbiz.base.util.*;
import org.ofbiz.testtools.seleniumxml.util.TestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SeleniumXml {
    public static String PROPS_NAME = "selenium.config";
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
    private String testCaseDirectory;
    private String imagePath = null;
    
    public static void main(String[] args) throws JDOMException, IOException, TestCaseException{
        if(args.length == 0) {
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

            File testFile = new File(args[0]);
            if (testFile.exists()) {
                System.err.println(" Argument : "+ args[0] );
                System.err.println(" Full absolute path of file : "+ testFile.getAbsolutePath()  );
                System.err.println(" Full canonical path of file : "+ testFile.getCanonicalPath() );

                sel.testCaseDirectory =  sel.getFileDirectory(testFile.getAbsolutePath());
                System.err.println(" testCaseDirectory: "+ sel.testCaseDirectory );
                 sel.runTest( testFile.getAbsolutePath() );
            } else {
                System.err.println("Test File is not exist :"+args[0]);
            }
        }
    }

    /* call run test suite from webtool selenium */
    public static String runTestSuite(HttpServletRequest request, HttpServletResponse response){
        Map parameters = UtilHttp.getParameterMap(request);
        String para = (String)parameters.get("testSuitePath");
        if(para == null){
            System.out.println("Error message : Test suite Path  is null");
            return "success";
        }
        if(para.length()==0){
            System.out.println("Error message : Test suite Path  is null");
            return "success";
        }
         try{
             URL url = UtilURL.fromResource("seleniumXml.properties");
             if (props == null) {
                props = new Properties();
                initConfig(url);
             }
             SeleniumXml sel = new SeleniumXml();
             File testFile = new File(para.trim());
             if (testFile.exists()) {
                System.err.println(" Argument : "+ para.trim() );
                System.err.println(" Full absolute path of file : "+ testFile.getAbsolutePath()  );
                System.err.println(" Full canonical path of file : "+ testFile.getCanonicalPath() );

                sel.testCaseDirectory =  sel.getFileDirectory(testFile.getAbsolutePath());
                System.err.println(" testCaseDirectory: "+ sel.testCaseDirectory );
                sel.runTest( testFile.getAbsolutePath() );
            } else {
                System.err.println("Test File is not exist :"+para.trim());
            }
        }catch(JDOMException jdome){
             System.out.println(jdome.getMessage());
        }catch(IOException ioe){
             System.out.println("Error message : "+ioe.getMessage());
        }finally{
             return "success";
         }
    }

    private String getFileDirectory(String filePath){
        String directory = null;
        if (filePath.indexOf(File.separatorChar) != -1   ) {
            int lastIndexOf = filePath.lastIndexOf(File.separatorChar);
            directory = filePath.substring(0, (lastIndexOf+1));
        }
        return directory;
    }

    public SeleniumXml() throws IOException {
        this.map = FastMap.newInstance();
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

    private static void initConfig(URL url) throws IOException {
        try {
            if (url == null) {
                String errMsg = "The Java environment (-Dxxx=yyy) variable with name " + url.toString() + " is not set, cannot resolve location.";
                throw new MalformedURLException(errMsg);
            }
            props = UtilProperties.getProperties(url);
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
        this.testCaseDirectory = selenium.testCaseDirectory;
    }

    public DefaultSelenium getSelenium() {
        return this.sel;
    }
    public Map <String, Object> getParameterMap() {
        return this.map;
    }
    public void runTest(String fileName) throws JDOMException, IOException, TestCaseException {
        readFile(fileName);
        setupSelenium();
        runCommands();
    }

    public void runCommands() throws TestCaseException {
        Element root = this.doc.getRootElement();
        List<Element> nodes = UtilGenerics.cast(root.getChildren());
        runCommands(nodes);
    }

    public void runCommands(List<Element> nodes) throws TestCaseException{
        for(Element elem: nodes) {
            String thisName = elem.getName();
            if("type" == elem.getName()) {
                typeCmd(elem);

            } else if("setParam" == thisName) {
                setParam(elem);
            } else if("clickAt" == thisName) {
                clickAt(elem);
            } else if("waitForValue" == thisName) {
                waitForValue(elem);
            } else if("waitForCondition" == thisName) {
                waitForCondition(elem);
            } else if("loadData" == thisName) {
                loadData(elem);
            } else if("jythonRunner" == thisName) {
                jythonRunner(elem);
            } else if("groovyRunner" == thisName) {
                groovyRunner(elem);
            } else if("dataLoop" == thisName) {
                dataLoop(elem);
            } else if("remoteRequest" == thisName) {
                remoteRequest(elem);
            } else if("selectPopup" == thisName) {
                selectPopup(elem);
            } else if("getAllWindowIds" == thisName) {
                getAllWindowIds(elem);
            } else if("captureTextInPage" == thisName) {
                captureTextInPageCmd(elem);
            } else if("getSelectedLabel" == thisName) {
                getSelectedLabel(elem);
            } else if("getSelectedValue" == thisName) {
                getSelectedValue(elem);
            } else if("getSelectedId" == thisName) {
                getSelectedId(elem);
            } else if("testcase" == thisName) {
                testcase(elem);
            } else if("assertContains" == thisName) {
                assertContains(elem);
            } else if("assertNotContains" == thisName) {
                assertNotContains(elem);
            } else if("getHtmlSource" == thisName) {
                getHtmlSource(elem);
            } else if("getBodyText" == thisName) {
                getBodyText(elem);
            } else if("setup" == thisName) {
                continue; //setup is handled previously
            } else if("print" == thisName) {
                printCmd(elem);
            } else if("waitForPageToLoad" == thisName) {
                waitForPageToLoadCmd(elem);
            } else if("getSelectedIds" == thisName) {
                getSelectedIdsCmd(elem);
            } else if("copy" == thisName) {
                copyCmd(elem);
            } else if("append" == thisName) {
                appendCmd(elem);
            } else if("loadParameter" == thisName) {
                loadParameter(elem);
            } else if("partialRunDependency" == thisName) {
                partialRunDependency(elem);
            } else if("if" == thisName) {
                ifCmd(elem);
            } else if("open" == thisName) {
                openCmd(elem);
            } else if("click" == thisName) {
                clickCmd(elem);
            } else if("check" == thisName) {
                checkCmd(elem);
            } else if("uncheck" == thisName) {
                uncheckCmd(elem);
            } else if("getValue" == thisName) {
                getValueCmd(elem);
            } else if("select" == thisName) {
                selectCmd(elem);
            } else if("uniqueId" == thisName) {
                uniqueIdCmd(elem);
            } else if("randomAlphaString" == thisName) {
                randomAlphaStringCmd(elem);
            } else if("randomString" == thisName) {
                randomStringCmd(elem);
            } else if("setSpeed" == thisName) {
                setSpeed(elem);
            } else if("openWindow" == thisName) {
                openWindow(elem);
            } else if("selectWindow" == thisName) {
                selectWindow(elem);
            }  else if("assertConfirmation" == thisName) {
                assertConfirmation(elem);
            }  else if("captureEntirePageScreenshot" == thisName) {
                captureEntirePageScreenshotCmd(elem);
            } else if("runScript" == thisName) {
                runScript(elem);
            } else if("closeBrowser" == thisName) {
                sel.stop();
            } else {
                logger.info("Undefined command calling by reflection for command: " + thisName);
                callByReflection(elem);
            }
        }
    }

    private void callByReflection(Element elem) {
        String methodName = elem.getName();
        String param1 = elem.getAttributeValue("param1");
        String param2 = elem.getAttributeValue("param2");

        Class[] paramTypes = null;
        Object[] args = null;
        if( (param1 != null)  && (param2 != null) ) {
            paramTypes = new Class[] {String.class, String.class};
            args = new Object[] {replaceParam(param1), replaceParam(param2)};
        } else if (param1 != null) {
            paramTypes = new Class[] {String.class};
            args = new Object[] {replaceParam(param1)};
        } else {
            paramTypes = new Class[] {};
            args = new Object[] {};
        }

        //Capture the output name for "get" methods
        String out = elem.getAttributeValue("out");

        Method m;
        try {
            m = this.sel.getClass().getDeclaredMethod(methodName, paramTypes);
            Object results = m.invoke(this.sel, args);

            //Add output parameter to common map
            if( (out != null) && (results != null)) {
                addParam(out, results);
            }
        } catch (Exception e) {
            logger.error("Exception occurred when Unknown SeleniumXml command found:"+elem.getName());
            e.printStackTrace();
        }
    }

    public void waitForValue(Element elem) {
        String locator = replaceParam(elem.getAttributeValue("locator"));
        String timeout = elem.getAttributeValue("timeout");
        String outParam = elem.getAttributeValue("out");

        int maxTime = Integer.parseInt(timeout);
        int maxSeconds = maxTime/1000;
        logger.debug("waitForValue: locator=" + locator + " timeout=" + timeout);
        String foundValue = null;
        for(int second=0;; second++) {
            if(second >= maxSeconds) {
                throw new SeleniumException("waitForValue exceeded timeout: " + maxTime);
            }
            try{
                //getValue throws an exception if it can't find locator
                // - sleep for 1 sec and try again
                // - otherwise break as we found the value
                foundValue = sel.getValue(locator);
                if(outParam != null) {
                    this.addParam(outParam, foundValue);
                }
                break;
            } catch(Exception e) {
                //wait for 1 second and then resume
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException threadE) {
                    threadE.printStackTrace();
                }
            }
        }
    }

    public void setParam(Element elem) {
        String name = replaceParam(elem.getAttributeValue("name"));
        String value = replaceParam(elem.getAttributeValue("value"));

        if( (name != null) && (value != null)) {
            this.addParam(name, value);
        }
    }

    public void getValueCmd(Element elem) {
        String locator = replaceParam(elem.getAttributeValue("locator"));
        String outParam = elem.getAttributeValue("out");

        logger.debug("getValueCmd: locator=" + locator);
        String foundValue = sel.getValue(locator);
        if(outParam != null) {
            this.addParam(outParam, foundValue);
        }
    }

    public void waitForCondition(Element elem) {
        String script = elem.getAttributeValue("script");
        String timeout = elem.getAttributeValue("timeout");

        logger.debug("waitForCondition: script=" + script + " timeout=" + timeout);
        this.sel.waitForCondition(script, timeout);
    }

    public void openWindow(Element elem) {
        String url = elem.getAttributeValue("url");
        String windowId = replaceParam(elem.getAttributeValue("windowId"));

        logger.debug("openWindow: url=" + url + " windowId=" + windowId);
        this.sel.openWindow(url, windowId);
        return;
    }

    public void selectWindow(Element elem) {
        String windowId = replaceParam(elem.getAttributeValue("windowId"));

        logger.debug("selectWindow:  windowId=" + windowId);
        this.sel.selectWindow(windowId);
        return;
    }

    public void runScript(Element elem) {
        String script = replaceParam(elem.getAttributeValue("script"));

        logger.debug("runScript:  script=" + script);
        this.sel.runScript(script);
        return;
    }

    public void loadData(Element elem) throws TestCaseException {

            String file = elem.getAttributeValue("file");
            String iterations = elem.getAttributeValue("iterations");
            List<Element> children = UtilGenerics.cast(elem.getChildren());

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

    public void dataLoop(Element elem) throws TestCaseException {

        String dataListName = elem.getAttributeValue("dataListName");
        List<Element> children = UtilGenerics.cast(elem.getChildren());

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
        List <Element> children = UtilGenerics.cast(elem.getChildren());
        List <Element> loginAs = UtilGenerics.cast(elem.getChildren("login-as"));
        logger.info("remoteRequest: children=" + children + " loginAs="+loginAs);
        RemoteRequest loader = new RemoteRequest( this, children, loginAs, requestUrl, host, responseHandlerMode);
        loader.runTest();
    }

    public void ifCmd(Element elem) throws TestCaseException {
        String isRun = replaceParam(elem.getAttributeValue("condition"));
        if (isRun != null && !isRun.equals("") && Boolean.valueOf(isRun)) {
            List <Element> children = UtilGenerics.cast(elem.getChildren());
            this.runCommands(children);
        }else{
            Element child = elem.getChild("else");
            List <Element> children = UtilGenerics.cast(child.getChildren());
            this.runCommands(children);
        }
    }

    public void partialRunDependency(Element elem) throws TestCaseException {
        String isRun = replaceParam(elem.getAttributeValue("isRun"));
        if (isRun != null && Boolean.valueOf(isRun)) {
            List <Element> children = UtilGenerics.cast(elem.getChildren());
            this.runCommands(children);
        }
    }

    public String getParamValue(String key) {
        return (String) this.map.get(key);
    }

    public Object getParamValue(Object key) {
        return this.map.get(key);
    }

    public void addParam(String name, String value) {
        logger.info("addParam: name=" + name + " value="+value);
        this.map.put(name, value);
    }

    public void addParam(String name, Object value) {
        logger.info("addParam: name=" + name + " value="+value);
        this.map.put(name, value);
    }

    private void assertContains(Element elem) throws TestCaseException {
        String src = replaceParam(elem.getAttributeValue("src"));
        String test = replaceParam(elem.getAttributeValue("test"));
        int indxSearch = src.indexOf(test);
        if(indxSearch == -1) {
            logger.info("assertContains didn't find " + test + " in the src");
            throw new TestCaseException("assertContains didn't find: " + test);
        } else {
            logger.info("assertContains found " + test + " in the src");
        }
        //TODO: implement JUnit TestCase - Assert.assertTrue(indxSearch != -1);
    }

    private void assertNotContains(Element elem) throws TestCaseException {
        String src = replaceParam(elem.getAttributeValue("src"));
        String test = replaceParam(elem.getAttributeValue("test"));
        int indxSearch = src.indexOf(test);
        if(indxSearch != -1) {
            logger.info("assertNotContains found " + test + " in the src");
            throw new TestCaseException("assertContains didn't find: " + test);
        } else {
            logger.info("assertNotContains didn't find " + test + " in the src");
        }
    }

    private void assertTitle(Element elem) throws TestCaseException {
        String src = replaceParam(this.sel.getTitle());
        String test = replaceParam(elem.getAttributeValue("value"));
        int indxSearch = src.indexOf(test);
        if(indxSearch == -1) {
            logger.info("assertTitle value " + test + " doesn't match exact "+src);
            throw new TestCaseException("assertTitle value " + test + " doesn't match exact "+src);
        } else {
            logger.info("assertTitle matched title");
        }
    }

    private void selectPopup(Element elem) {
        String locator = elem.getAttributeValue("locator");
        String timeout = elem.getAttributeValue("timeout");

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
        logger.info("getHtmlsource: paramName=" + paramName + " text=" + text);
        addParam(paramName, text);
    }

    private void getBodyText(Element elem) {
        String paramName = elem.getAttributeValue("out");
        String text = this.sel.getBodyText();
        addParam(paramName, text);
    }

    private void testcase(Element elem) {
        System.err.println("New testcase: " + elem.getAttributeValue("file"));
        String testFile = elem.getAttributeValue("file");
        String isRun = replaceParam(elem.getAttributeValue("isRun"));

        String absolutePath = getAbsolutePath(testFile);
        String parentTestCase = new String(this.testCaseDirectory);
        SeleniumXml newTest = new SeleniumXml(this);
        newTest.testCaseDirectory = getFileDirectoryForRelativePath(absolutePath);
        try {
            if (isRun == null || isRun.equals("") || Boolean.valueOf(isRun)) {
                newTest.runTest(absolutePath);
            }else{
                System.err.println(" testFile :"+testFile+ "  isRun:"+isRun);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Testcase error for file: " + absolutePath);
        }
        newTest.testCaseDirectory  = parentTestCase;
    }

    private String getFileDirectoryForRelativePath(String filePath){
        String directory = null;
        if (filePath.indexOf("/") != -1   ) {
            int lastIndexOf = filePath.lastIndexOf("/");
            directory = filePath.substring(0, (lastIndexOf+1));
        }else  if(filePath.indexOf("\\") != -1   ) {
            int lastIndexOf = filePath.lastIndexOf("\\");
            directory = filePath.substring(0, (lastIndexOf+1));
        }
        return directory;
    }

    private void clickAt(Element elem) {
        logger.debug("clickAt: " + replaceParam(elem.getAttributeValue("locator")));
        String locator = replaceParam(elem.getAttributeValue("locator"));
        String coordString = elem.getAttributeValue("coordString");
        this.sel.clickAt(locator, coordString);
    }

    private void assertConfirmation(Element elem) {
        logger.debug("assertConfirmation: " + replaceParam(elem.getAttributeValue("value")));
        this.sel.waitForCondition("selenium.isConfirmationPresent();", "1000");
        this.sel.getConfirmation();
    }

    /**
     * @param elem
     * Will save a browser's screenshot in runtime/logs
     * Need to be called with captureEntirePageScreenshot and the name of the test case
     * example :
     *     <captureEntirePageScreenshot value="CommEventCreateOpportunity"/>
     */
    private void captureEntirePageScreenshotCmd(Element elem) {
        Long now = UtilDateTime.nowTimestamp().getTime();
        String imageName = replaceParam(elem.getAttributeValue("value")) + "-" + now.toString();
        logger.debug("captureEntirePageScreenshot: " + imageName);
        imagePath = "runtime/logs/" + imageName + ".png";
        try {
            String base64Screenshot = sel.captureEntirePageScreenshotToString(""); 
            byte[] decodedScreenshot = Base64.decodeBase64(base64Screenshot.getBytes());
            FileOutputStream fos = new FileOutputStream(new File(imagePath));
            fos.write(decodedScreenshot);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        logger.info("clickCmd: " +  replaceParam(elem.getAttributeValue("locator")));
        try {
            this.sel.click(replaceParam(elem.getAttributeValue("locator")));
        } catch (SeleniumException e) {
            logger.info("caught SeleniumException Name:"+elem.getName()+"  , Value: "+elem.getAttributeValue("locator"));
            e.printStackTrace();
        }
    }

    private void doubleClick(Element elem) {
        logger.info("clickCmd: " +  replaceParam(elem.getAttributeValue("locator")));
        this.sel.doubleClick((replaceParam(elem.getAttributeValue("locator"))));
    }

    private void checkCmd(Element elem) {
        logger.info("checkCmd: " +  replaceParam(elem.getAttributeValue("locator")));
        this.sel.check(replaceParam(elem.getAttributeValue("locator")));
    }

    private void uncheckCmd(Element elem) {
        logger.info("uncheckCmd: " +  replaceParam(elem.getAttributeValue("locator")));
        this.sel.uncheck(replaceParam(elem.getAttributeValue("locator")));
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
     *   "xx <tag a=b> yy </tag> zz"
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
            if(group != null) {
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
        if(size != null) {
            nSize = Integer.parseInt(size);
        }
        String prefix = elem.getAttributeValue("prefix");
        if(prefix != null) {
            nPrefixSize = prefix.length();
        }

        String paramValue = null;
        if(prefix != null) {
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

    private void loadParameter(Element elem) {
        logger.info("loadParameter: fileName=" + elem.getAttributeValue("file") );
        String parameterFile = elem.getAttributeValue("file");
        String absolutePath = getAbsolutePath(parameterFile);
        BasicConfigurator.configure();

        try {
            InputStream in = new FileInputStream(absolutePath);
            Properties parameter = new Properties();
            parameter.load(in);
            in.close();

            Set<Entry<Object, Object>> entrySet = parameter.entrySet();

            for(Map.Entry entry : entrySet) {
                String key = (String)entry.getKey();
                String value = (String)entry.getValue();
                System.out.println(key + " = " + value);
                addParam(key, value);
            }
        } catch (Exception e) {
            logger.error("Can not load parameter . ");
        }

        String newStr = replaceParam(elem.getAttributeValue("src1")) + replaceParam(elem.getAttributeValue("src2"));
        addParam(elem.getAttributeValue("out"), newStr);
    }

    public String replaceParam(String value) {
        if (value == null) { return value; }

        StringBuilder buf = new StringBuilder();
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

    private boolean isParam(String value ) {

        if( (value.indexOf("${") != -1) &&
            (value.indexOf("}", 1) != -1) ) {
            return true;
        }
        return false;
    }

    //TODO read properties file to setup selenium
    private void setupSelenium() {
        //return if Selenium has already been setup
        //e.g. nested selenium test cases.
        if(this.sel != null) return;

        String serverHost = null;
        String serverPort = null;
        String browser = null;
        String startUrl = null;
        String timeout = null;

        //First initialize with property values
        if(props != null ) { //Get setup params from property value
            serverHost = props.getProperty("serverHost", "localhost");
            serverPort = props.getProperty("proxyPort", "4444");
            browser = props.getProperty("browser", "*iexplore");
            startUrl = props.getProperty("startUrl", "http://localhost:8080");
            timeout = props.getProperty("timeout", "30000");
            imagePath = props.getProperty("imagePath", "runtime/logs/");
        }
        //Second over ride properties if defined in the "setup" element
        Element elem = this.doc.getRootElement().getChild("setup");
        if (elem != null) {
            //Override properties if specified
            if( elem.getAttributeValue("serverHost") != null ) {
                serverHost = elem.getAttributeValue("serverHost");
            }
            if( elem.getAttributeValue("serverPort") != null ) {
                serverPort = elem.getAttributeValue("serverPort");
            }
            if( elem.getAttributeValue("browser") != null ) {
                browser = elem.getAttributeValue("browser");
            }
            if( elem.getAttributeValue("startUrl") != null ) {
                startUrl = elem.getAttributeValue("startUrl");
            }
        }
        logger.info("setup: serverHost=" + serverHost);
        logger.info("setup: serverPort=" + serverPort);
        logger.info("setup: browser=" + browser);
        logger.info("setup: startUrl=" + startUrl);
        logger.info("setup: timeout=" + timeout);
        logger.info("setup: imagePath=" + imagePath);
        this.sel = new DefaultSelenium(serverHost, Integer.parseInt(serverPort), browser, startUrl);
        this.sel.start();
        this.sel.setTimeout(timeout);
    }

    private String getAbsolutePath(String fileName){
        logger.info("getAbsolutePath: fileName=" + fileName);
        String fileAbsolutePath = fileName;
        if (fileName.indexOf(File.separatorChar) == -1) {
            if(this.testCaseDirectory != null) {
                fileAbsolutePath = this.testCaseDirectory + fileName;
            }
        }
        logger.info("getAbsolutePath: returning fileName=" + fileName);
        return fileAbsolutePath;
    }

    private void readFile(String fileName) throws JDOMException, IOException {
        String absolutePath = getAbsolutePath(fileName);
        File xmlFile = new File(absolutePath);

        SAXBuilder builder = new SAXBuilder();
        this.doc = builder.build(xmlFile);
    }

    public String getUserName() {
        return this.username;
    }

    public void setUserName(String val) {
        this.username = val;
    }

    public void setPassword(String val) {
        this.password = val;
    }

    public String getPassword() {
        return this.password;
    }

    public Map <String, Object> getMap() {
        return this.map;
    }
}
