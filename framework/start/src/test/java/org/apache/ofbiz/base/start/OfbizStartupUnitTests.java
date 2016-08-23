package org.apache.ofbiz.base.start;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class OfbizStartupUnitTests {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void commandParserDoesNotAcceptMoreThanOneCommand() throws StartupException {
        expectedException.expectMessage("an option from this group has already been selected");

        StartupCommandUtil.parseOfbizCommands(new String[]{"--help", "--status"});
    }

    @Test
    public void commandParserDoesNotAcceptPortoffsetWithoutArgument() throws StartupException {
        expectedException.expectMessage("Missing argument for option");

        StartupCommandUtil.parseOfbizCommands(new String[]{"--portoffset"});
    }

    @Test
    public void commandParserDoesNotAcceptPortoffsetWithoutPositiveInteger() throws StartupException {
        expectedException.expectMessage("you can only pass positive integers");

        StartupCommandUtil.parseOfbizCommands(new String[]{"--portoffset", "ThisMustBeInteger54321"});
    }

    @Test
    public void commandParserDoesNotAcceptArgumentForStatus() throws StartupException {
        expectedException.expectMessage("unrecognized options / properties");

        StartupCommandUtil.parseOfbizCommands(new String[]{"--status", "thisArgNotAllowed"});
    }

    @Test
    public void commandParserDoesNotAcceptArgumentForStart() throws StartupException {
        expectedException.expectMessage("unrecognized options / properties");

        StartupCommandUtil.parseOfbizCommands(new String[]{"--start", "thisArgNotAllowed"});
    }

    @Test
    public void commandParserDoesNotAcceptArgumentForShutdown() throws StartupException {
        expectedException.expectMessage("unrecognized options / properties");

        StartupCommandUtil.parseOfbizCommands(new String[]{"--shutdown", "thisArgNotAllowed"});
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
