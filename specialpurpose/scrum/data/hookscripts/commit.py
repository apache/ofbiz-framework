#!/usr/bin/python
#
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
#

from xml.dom.minidom import Document
import httplib,sys,os,subprocess
import ConfigParser

#####################################
##### Subversion configurations #####
REPOS = sys.argv[1]
REV = sys.argv[2]

# if the repository and the version number of the repository not empty. 
if REPOS and REV:
    # The path of revision.properties
    CONFIG_PATH = "/home/ofbiz/ofbiz/hot-deploy/scrum/config/revision.properties"
    openfile = open(CONFIG_PATH)
    openfile.readline()
    revision = ConfigParser.ConfigParser()
    revision.readfp(openfile)
    REVISION_URL = revision.get("config", "revision.url")
    HOSTNAME = revision.get("config", "host.name")
    HOSTPORT = revision.get("config", "host.port")
    OFBIZ_WEBSERVICE_URL = revision.get("config", "ofbiz.webservice.url")
    SVN_USER = revision.get("config", "svn.user")
    SVN_PASSWORD = revision.get("config", "svn.password")
    
    print "HOSTNAME  == >>>" + HOSTNAME
    print "HOSTPORT  == >>>" + HOSTPORT
    print "OFBIZ_WEBSERVICE_URL  == >>>" + OFBIZ_WEBSERVICE_URL
    print "SVN_USER  == >>>" + SVN_USER
    print "SVN_PASSWORD  == >>>" + SVN_PASSWORD
    
    try:
        REPOS_INPUT = REPOS[ 0 : REPOS.rfind("svn/")]
        if len(REPOS_INPUT) > 15:
            REPOSITORY_ROOT = REPOS[ REPOS.rfind("svn/") + 4 : len(REPOS)]
            REPOS = REPOS
        else:
            REPOSITORY_ROOT = REPOS[ REPOS.rfind("svn/repositories/") + 17 : len(REPOS)]
            REPOS = REVISION_URL + REPOSITORY_ROOT
            
        SVN_INFO_URL = REPOSITORY_ROOT + "&revision=" + REV
        
        print "REPOS  == >>>" + REPOS
        print "SVN_INFO_URL  == >>>" + SVN_INFO_URL
        ##### Web service configurations #####
        #####################################
        ###### Find Task and User ######
        if SVN_USER and SVN_PASSWORD:
            l = subprocess.Popen('svn log -r %s %s --username %s --password %s  \n\n' % (REV, REPOS, SVN_USER, SVN_PASSWORD), shell=True, stdout=subprocess.PIPE, stdin=subprocess.PIPE, stderr=subprocess.STDOUT)
            l.stdin.write('no\n')
            l.stdin.write('t\n')
        else:
            l = subprocess.Popen('svn log -r %s %s \n\n' % (REV, REPOS), shell=True, stdout=subprocess.PIPE, stdin=subprocess.PIPE, stderr=subprocess.STDOUT)
        l.stdin.write('clear\n')
        log = l.communicate()[0]
        logListInput = log.split('\n');
        if len(logListInput) > 7:
            log = logListInput[17]
        else:
            logList = log.split('|');
            log = logList[3]
        ### check taskId ###
        count = 0
        taskId = ''
        for index in range(len(log)):
            letter = log[index]
            if letter.isdigit():
                count = count + 1
            else:
                count = 0
            if count == 5:
                taskId = log[(index - 4) : index + 1]
                break
        ### check hours ###
        hours ='';
        checkCond = False;
        hourData = log.lower();
        hourMes = hourData[ hourData.rfind("hrs:") + 4 : hourData.rfind("hrs:") + 8]
        countHrs =  len(hourMes);
        firstchar = hourMes[0];
        for hrsIndex in xrange(countHrs,0,-1):
            if firstchar.isdigit():
                hrsLetter = hourMes[hrsIndex-1];
                if hrsLetter.isdigit():
                    checkCond = True;
                else:
                    checkCond = False;
                if checkCond:
                    hours = hourMes[0 : (hrsIndex)]
                    break
            else:
                break
        ### check Description ###
        if len(logListInput) > 7:
            userlog = logListInput[15]
            userlogList = userlog.split('|');
            user = userlogList[1]
            logMes = logListInput[17]
            if len(logMes) > 255:
                logMes = logMes[0:255]
            revisionDescription = logMes
        else:
            user = logList[1].strip()
            removeIndex = log.find('----------')
            if removeIndex != -1:
                log = log[0 : removeIndex - 1]
            if len(log) > 255:
                log = log[0:255]
            revisionDescription = log[9 : len(log) - 1]
            
        print "user       == >>>" + user
        print "taskId     == >>>" + taskId
        print "hours      == >>>" + hours
        print "revisionDescription  == >>>" + revisionDescription
        ###### Create the minidom document ######
        doc = Document()
        soapenv = doc.createElement("soapenv:Envelope")
        doc.appendChild(soapenv)
        soapenv.setAttribute("xmlns:soapenv", "http://schemas.xmlsoap.org/soap/envelope/")
        soapenv.setAttribute("xmlns:ser", OFBIZ_WEBSERVICE_URL)
        
        header = doc.createElement("soapenv:Header")
        soapenv.appendChild(header)
        
        body = doc.createElement("soapenv:Body")
        soapenv.appendChild(body)
        
        updateScrumRevision = doc.createElement("ser:updateScrumRevision")
        body.appendChild(updateScrumRevision)
        
        mapMap = doc.createElement("map-Map")
        updateScrumRevision.appendChild(mapMap)
        
        mapEntry = doc.createElement("ser:map-Entry")
        mapKey = doc.createElement("ser:map-Key")
        stdString = doc.createElement("ser:std-String")
        stdString.setAttribute("value", "revisionNumber")
        mapKey.appendChild(stdString)
        mapEntry.appendChild(mapKey)
        mapValue = doc.createElement("ser:map-Value")
        stdString = doc.createElement("ser:std-String")
        stdString.setAttribute("value", REV)
        mapValue.appendChild(stdString)
        mapEntry.appendChild(mapValue)
        mapMap.appendChild(mapEntry)
        
        mapEntry = doc.createElement("ser:map-Entry")
        mapKey = doc.createElement("ser:map-Key")
        stdString = doc.createElement("ser:std-String")
        stdString.setAttribute("value", "revisionLink")
        mapKey.appendChild(stdString)
        mapEntry.appendChild(mapKey)
        mapValue = doc.createElement("ser:map-Value")
        stdString = doc.createElement("ser:std-String")
        stdString.setAttribute("value", SVN_INFO_URL)
        mapValue.appendChild(stdString)
        mapEntry.appendChild(mapValue)
        mapMap.appendChild(mapEntry)
        
        mapEntry = doc.createElement("ser:map-Entry")
        mapKey = doc.createElement("ser:map-Key")
        stdString = doc.createElement("ser:std-String")
        stdString.setAttribute("value", "revisionDescription")
        mapKey.appendChild(stdString)
        mapEntry.appendChild(mapKey)
        mapValue = doc.createElement("ser:map-Value")
        stdString = doc.createElement("ser:std-String")
        stdString.setAttribute("value", revisionDescription)
        mapValue.appendChild(stdString)
        mapEntry.appendChild(mapValue)
        mapMap.appendChild(mapEntry)
        
        mapEntry = doc.createElement("ser:map-Entry")
        mapKey = doc.createElement("ser:map-Key")
        stdString = doc.createElement("ser:std-String")
        stdString.setAttribute("value", "taskId")
        mapKey.appendChild(stdString)
        mapEntry.appendChild(mapKey)
        mapValue = doc.createElement("ser:map-Value")
        stdString = doc.createElement("ser:std-String")
        stdString.setAttribute("value", taskId)
        mapValue.appendChild(stdString)
        mapEntry.appendChild(mapValue)
        mapMap.appendChild(mapEntry)
        
        mapEntry = doc.createElement("ser:map-Entry")
        mapKey = doc.createElement("ser:map-Key")
        stdString = doc.createElement("ser:std-String")
        stdString.setAttribute("value", "hours")
        mapKey.appendChild(stdString)
        mapEntry.appendChild(mapKey)
        mapValue = doc.createElement("ser:map-Value")
        stdString = doc.createElement("ser:std-String")
        stdString.setAttribute("value", hours)
        mapValue.appendChild(stdString)
        mapEntry.appendChild(mapValue)
        mapMap.appendChild(mapEntry)
        
        mapEntry = doc.createElement("ser:map-Entry")
        mapKey = doc.createElement("ser:map-Key")
        stdString = doc.createElement("ser:std-String")
        stdString.setAttribute("value", "user")
        mapKey.appendChild(stdString)
        mapEntry.appendChild(mapKey)
        mapValue = doc.createElement("ser:map-Value")
        stdString = doc.createElement("ser:std-String")
        stdString.setAttribute("value", user)
        mapValue.appendChild(stdString)
        mapEntry.appendChild(mapValue)
        mapMap.appendChild(mapEntry)
        
        soapMessage = doc.toprettyxml(indent="  ");
        
        #######  Call Webservice #######
        # Send request
        webservice = httplib.HTTPConnection(HOSTNAME, HOSTPORT, timeout=10)
        webservice.putrequest("POST", OFBIZ_WEBSERVICE_URL)
        webservice.putheader("Host", HOSTNAME)
        webservice.putheader("User-Agent", "Python post")
        webservice.putheader("Content-type", "text/xml; charset=\"UTF-8\"")
        webservice.putheader("Content-length", "%d" % len(soapMessage))
        webservice.putheader("SOAPAction", "\"\"")
        webservice.endheaders()
        webservice.send(soapMessage)
        
        # Get response
        response = webservice.getresponse()
        #print "reason: ", response.reason
        webservice.close()
    except Exception:
        print >>sys.stderr, "File: {0}".format(sys.argv[0])
        error_info = sys.exc_info()[1]
        sys.stderr.write("Exception :%s" % error_info)
        sys.exit(1)
else:
    print >>sys.stderr, "File: {0}".format(sys.argv[0])
    sys.stderr.write("<<<:  Error: The repository and the version number of subversion can not be null  :>>\n")
    sys.exit(1)
    
sys.exit(0)
