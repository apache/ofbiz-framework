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

package org.apache.ofbiz.base.start;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThrows;

import java.util.List;

import org.junit.Test;

public class OfbizStartupUnitTests {

    @Test
    public void commandParserDoesNotAcceptMoreThanOneCommand() {
        Exception e = assertThrows(Exception.class, () ->
                StartupCommandUtil.parseOfbizCommands(new String[]{"--help", "--status"}));
        assertThat(e.getMessage(), containsString("an option from this group has already been selected"));
    }

    @Test
    public void commandParserDoesNotAcceptPortoffsetWithoutArgument() {
        Exception e = assertThrows(Exception.class, () ->
                StartupCommandUtil.parseOfbizCommands(new String[]{"--portoffset"}));
        assertThat(e.getMessage(), containsString("Missing argument for option"));
    }

    @Test
    public void commandParserDoesNotAcceptPortoffsetWithoutPositiveInteger() {
        Exception e = assertThrows(Exception.class, () ->
                StartupCommandUtil.parseOfbizCommands(new String[]{"--portoffset", "ThisMustBeInteger54321"}));
        assertThat(e.getMessage(), containsString("you can only pass positive integers"));
    }

    @Test
    public void commandParserDoesNotAcceptArgumentForStatus() {
        Exception e = assertThrows(Exception.class, () ->
                StartupCommandUtil.parseOfbizCommands(new String[]{"--status", "thisArgNotAllowed"}));
        assertThat(e.getMessage(), containsString("unrecognized options / properties"));
    }

    @Test
    public void commandParserDoesNotAcceptArgumentForStart() {
        Exception e = assertThrows(Exception.class, () ->
                StartupCommandUtil.parseOfbizCommands(new String[]{"--start", "thisArgNotAllowed"}));
        assertThat(e.getMessage(), containsString("unrecognized options / properties"));
    }

    @Test
    public void commandParserDoesNotAcceptArgumentForShutdown() {
        Exception e = assertThrows(Exception.class, () ->
                StartupCommandUtil.parseOfbizCommands(new String[]{"--shutdown", "thisArgNotAllowed"}));
        assertThat(e.getMessage(), containsString("unrecognized options / properties"));
    }

    @Test
    public void commandParserCombinesMultipleArgsIntoOneCommand() throws StartupException {
        String[] multiArgCommand = new String[] {
                "--load-data", "readers=seed,seed-initial",
                "--load-data", "delegator=default",
                "-l", "timeout=7200" };

        List<StartupCommand> startupCommands = StartupCommandUtil.parseOfbizCommands(multiArgCommand);

        assertThat(startupCommands.size(), equalTo(1));
        assertThat(startupCommands.get(0).getProperties().size(), equalTo(3));
    }
}
