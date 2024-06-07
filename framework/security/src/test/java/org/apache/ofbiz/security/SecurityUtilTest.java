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
package org.apache.ofbiz.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class SecurityUtilTest {
    @Test
    public void basicAdminPermissionTesting() {
        List<String> adminPermissions = Arrays.asList("PARTYMGR", "EXAMPLE", "ACCTG_PREF");
        assertTrue(SecurityUtil.checkMultiLevelAdminPermissionValidity(adminPermissions, "PARTYMGR_CREATE"));
        assertTrue(SecurityUtil.checkMultiLevelAdminPermissionValidity(adminPermissions, "EXAMPLE_CREATE "));
        assertTrue(SecurityUtil.checkMultiLevelAdminPermissionValidity(adminPermissions, "EXAMPLE_ADMIN"));
        assertFalse(SecurityUtil.checkMultiLevelAdminPermissionValidity(adminPermissions, "ACCTG_ADMIN"));
    }

    @Test
    public void multiLevelAdminPermissionTesting() {
        List<String> adminPermissions = Arrays.asList("PARTYMGR", "EXAMPLE", "ACCTG_PREF");
        assertTrue(SecurityUtil.checkMultiLevelAdminPermissionValidity(adminPermissions, "PARTYMGR_CME_CREATE"));
        assertTrue(SecurityUtil.checkMultiLevelAdminPermissionValidity(
                    adminPermissions, "EXAMPLE_WITH_MULTI_LEVEL_ADMIN"));
        assertFalse(SecurityUtil.checkMultiLevelAdminPermissionValidity(adminPermissions, "ACCTG_ADMIN"));
    }

    @Test
    public void multiLevelBadHierarchyPermissionTesting() {
        List<String> adminPermissions = Arrays.asList("PARTYMGR", "EXAMPLE", "ACCTG_PREF");
        assertFalse(SecurityUtil.checkMultiLevelAdminPermissionValidity(
                    adminPermissions, "SPECIFIC_MULTI_LEVEL_EXAMPLE_VIEW"));
        assertFalse(SecurityUtil.checkMultiLevelAdminPermissionValidity(adminPermissions, "HOTDEP_PARTYMGR_ADMIN"));
    }

    @Test
    public void webShellTokensTesting() {
        /* Currently used
                java.,beans,freemarker,<script,javascript,<body,body ,<form,<jsp:,<c:out,taglib,<prefix,<%@ page,<?php,exec(,alert(,\
                %eval,@eval,eval(,runtime,import,passthru,shell_exec,assert,str_rot13,system,decode,include,page ,\
                chmod,mkdir,fopen,fclose,new file,upload,getfilename,download,getoutputstring,readfile,iframe,object,embed,onload,build,\
                python,perl ,/perl,ruby ,/ruby,process,function,class,InputStream,to_server,wget ,static,assign,webappPath,\
                ifconfig,route,crontab,netstat,uname ,hostname,iptables,whoami,"cmd",*cmd|,+cmd|,=cmd|,localhost,thread,require,gzdeflate,\
                execute,println,calc,calculate,touch,curl
         */
        try {
            List<String> allowed = new ArrayList<>();
            allowed.add("getfilename");
            assertTrue(SecuredUpload.isValidText("hack.getFileName", allowed));
            allowed = new ArrayList<>();
            assertFalse(SecuredUpload.isValidText("hack.getFileName", allowed));

            assertFalse(SecuredUpload.isValidText("java.", allowed));
            assertFalse(SecuredUpload.isValidText("beans", allowed));
            assertFalse(SecuredUpload.isValidText("freemarker", allowed));
            assertFalse(SecuredUpload.isValidText("<script", allowed));
            assertFalse(SecuredUpload.isValidText("javascript", allowed));
            assertFalse(SecuredUpload.isValidText("<body", allowed));
            assertFalse(SecuredUpload.isValidText("body ", allowed));
            assertFalse(SecuredUpload.isValidText("<form", allowed));
            assertFalse(SecuredUpload.isValidText("<jsp:", allowed));
            assertFalse(SecuredUpload.isValidText("<c:out", allowed));
            assertFalse(SecuredUpload.isValidText("taglib", allowed));
            assertFalse(SecuredUpload.isValidText("<prefix", allowed));
            assertFalse(SecuredUpload.isValidText("<%@ page", allowed));
            assertFalse(SecuredUpload.isValidText("<?php", allowed));
            assertFalse(SecuredUpload.isValidText("exec(", allowed));

            assertFalse(SecuredUpload.isValidText("%eval", allowed));
            assertFalse(SecuredUpload.isValidText("@eval", allowed));
            assertFalse(SecuredUpload.isValidText("runtime", allowed));
            assertFalse(SecuredUpload.isValidText("import", allowed));
            assertFalse(SecuredUpload.isValidText("passthru", allowed));
            assertFalse(SecuredUpload.isValidText("shell_exec", allowed));
            assertFalse(SecuredUpload.isValidText("assert", allowed));
            assertFalse(SecuredUpload.isValidText("str_rot13", allowed));
            assertFalse(SecuredUpload.isValidText("system", allowed));
            assertFalse(SecuredUpload.isValidText("decode", allowed));
            assertFalse(SecuredUpload.isValidText("include", allowed));

            assertFalse(SecuredUpload.isValidText("chmod", allowed));
            assertFalse(SecuredUpload.isValidText("mkdir", allowed));
            assertFalse(SecuredUpload.isValidText("fopen", allowed));
            assertFalse(SecuredUpload.isValidText("fclose", allowed));
            assertFalse(SecuredUpload.isValidText("new file", allowed));
            assertFalse(SecuredUpload.isValidText("upload", allowed));
            assertFalse(SecuredUpload.isValidText("getfilename", allowed));
            assertFalse(SecuredUpload.isValidText("download", allowed));
            assertFalse(SecuredUpload.isValidText("getoutputstring", allowed));
            assertFalse(SecuredUpload.isValidText("readfile", allowed));
            assertFalse(SecuredUpload.isValidText("iframe", allowed));
            assertFalse(SecuredUpload.isValidText("object", allowed));
            assertFalse(SecuredUpload.isValidText("embed", allowed));
            assertFalse(SecuredUpload.isValidText("onload", allowed));
            assertFalse(SecuredUpload.isValidText("build", allowed));

            assertFalse(SecuredUpload.isValidText("python", allowed));
            assertFalse(SecuredUpload.isValidText("perl ", allowed));
            assertFalse(SecuredUpload.isValidText("/perl", allowed));
            assertFalse(SecuredUpload.isValidText("ruby ", allowed));
            assertFalse(SecuredUpload.isValidText("/ruby", allowed));
            assertFalse(SecuredUpload.isValidText("process", allowed));
            assertFalse(SecuredUpload.isValidText("function", allowed));
            assertFalse(SecuredUpload.isValidText("class", allowed));
            assertFalse(SecuredUpload.isValidText("wget ", allowed));
            assertFalse(SecuredUpload.isValidText("static", allowed));
            assertFalse(SecuredUpload.isValidText("assign", allowed));
            assertFalse(SecuredUpload.isValidText("webappPath", allowed));

            assertFalse(SecuredUpload.isValidText("ifconfig", allowed));
            assertFalse(SecuredUpload.isValidText("route", allowed));
            assertFalse(SecuredUpload.isValidText("crontab", allowed));
            assertFalse(SecuredUpload.isValidText("netstat", allowed));
            assertFalse(SecuredUpload.isValidText("uname ", allowed));
            assertFalse(SecuredUpload.isValidText("hostname", allowed));
            assertFalse(SecuredUpload.isValidText("iptables", allowed));
            assertFalse(SecuredUpload.isValidText("whoami", allowed));
            // ip, ls, nc, ip, cat and pwd can't be used, too short
            assertFalse(SecuredUpload.isValidText("\"cmd\"", allowed));
            assertFalse(SecuredUpload.isValidText("*cmd|", allowed));
            assertFalse(SecuredUpload.isValidText("+cmd|", allowed));
            assertFalse(SecuredUpload.isValidText("=cmd|", allowed));
            assertFalse(SecuredUpload.isValidText("localhost", allowed));
            assertFalse(SecuredUpload.isValidText("thread", allowed));
            assertFalse(SecuredUpload.isValidText("require", allowed));
            assertFalse(SecuredUpload.isValidText("gzdeflate", allowed));
            assertFalse(SecuredUpload.isValidText("execute", allowed));
            assertFalse(SecuredUpload.isValidText("println", allowed));
            assertFalse(SecuredUpload.isValidText("calc", allowed));
            assertFalse(SecuredUpload.isValidText("calculate", allowed));
            assertFalse(SecuredUpload.isValidText("curl", allowed));
            assertFalse(SecuredUpload.isValidText("touch", allowed));
        } catch (IOException e) {
            fail(String.format("IOException occured : %s", e.getMessage()));
        }
    }
}
