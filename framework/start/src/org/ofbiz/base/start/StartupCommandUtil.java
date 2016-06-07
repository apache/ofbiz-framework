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
package org.ofbiz.base.start;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * A utility class for processing OFBiz command line arguments
 * 
 * <p>
 * Defines OFBiz startup options called through main e.g. --load-data or --help
 * in addition to utility methods for parsing and handling these options
 * </p> 
 */
final class StartupCommandUtil {

    /* 
     * Make sure of defining the same set of values in:
     * 
     * - The StartupOptions in the StartupOption enum
     * - The commons-cli options (e.g. BOTH, HELP, etc ...)
     * - The getOfbizStartupOptions method
     * 
     * Keeping these items in sync means that OFBiz behaves correctly
     * while being decoupled from the commons-cli library and the only
     * place where commons-cli is used is in this class
     */

    public enum StartupOption {
        BOTH("both"),
        HELP("help"),
        LOAD_DATA("load-data"),
        PORTOFFSET("portoffset"),
        POS("pos"),
        SHUTDOWN("shutdown"),
        START("start"),
        STATUS("status"),
        TEST("test"),
        TEST_LIST("testlist");
        
        private String name;
        private StartupOption(String name) {
            this.name = name;
        }
        public String getName() {
            return name;
        }
    }

    private static final Option BOTH = Option.builder("b")
            .longOpt(StartupOption.BOTH.getName())
            .desc("Runs simultaneously both the POS (Point of Sales) application and OFBiz server")
            .hasArg(false)
            .build();
    private static final Option HELP = Option.builder("?")
            .longOpt(StartupOption.HELP.getName())
            .desc("Prints this help screen to the user")
            .hasArg(false)
            .build();
    private static final Option LOAD_DATA = Option.builder("l")
            .longOpt(StartupOption.LOAD_DATA.getName())
            .desc("Creates tables/load data e.g:"
                    + System.lineSeparator()
                    + "-l readers=seed,demo,ext"
                    + System.lineSeparator()
                    + "-l timeout=7200"
                    + System.lineSeparator()
                    + "-l delegator=default"
                    + System.lineSeparator()
                    + "-l group=org.ofbiz"
                    + System.lineSeparator()
                    + "-l dir=directory/of/files"
                    + System.lineSeparator()
                    + "-l file=/tmp/dataload.xml")
            .numberOfArgs(2)
            .valueSeparator('=')
            .optionalArg(true)
            .argName("key=value")
            .build();
    private static final Option PORTOFFSET = Option.builder("o")
            .longOpt(StartupOption.PORTOFFSET.getName())
            .desc("Offsets the default network port for OFBiz")
            .hasArg()
            .argName("offset")
            .optionalArg(false)
            .build();
    private static final Option POS = Option.builder("p")
            .longOpt(StartupOption.POS.getName())
            .desc("Runs the POS (Point of Sales) application")
            .hasArg(false)
            .build();
    private static final Option SHUTDOWN = Option.builder("d")
            .longOpt(StartupOption.SHUTDOWN.getName())
            .desc("Shutdown OFBiz")
            .hasArg(false)
            .build();
    private static final Option START = Option.builder("u")
            .longOpt(StartupOption.START.getName())
            .desc("Start OFBiz")
            .hasArg(false)
            .build();
    private static final Option STATUS = Option.builder("s")
            .longOpt(StartupOption.STATUS.getName())
            .desc("Gives the status of OFBiz")
            .hasArg(false)
            .build();
    private static final Option TEST = Option.builder("t")
            .longOpt(StartupOption.TEST.getName())
            .desc("Runs the selected test or all if none selected e.g.: "
                    + System.lineSeparator()
                    + "--test component=base --test case=somecase"
                    + System.lineSeparator()
                    + "or"
                    + System.lineSeparator()
                    + "--test component=base --test suitename=xyz")
            .numberOfArgs(2)
            .valueSeparator('=')
            .optionalArg(true)
            .argName("key=value")
            .build();
    private static final Option TEST_LIST = Option.builder("x")
            .longOpt(StartupOption.TEST_LIST.getName())
            .desc("Generates an ant build script or a text file of all tests in ofbiz."
                    + System.lineSeparator()
                    + "The ant build script can run all test suites in isolation."
                    + System.lineSeparator()
                    + "The text file contains all tests in the format <component>:<suite-name>."
                    + System.lineSeparator()
                    + "The file arg determines the output file, and mode determines ant or text e.g:"
                    + System.lineSeparator()
                    + "--testlist file=runtime/test-list-build.xml"
                    + System.lineSeparator()
                    + "and"
                    + System.lineSeparator()
                    + "--testlist mode=ant or --testlist mode=text")
            .numberOfArgs(2)
            .valueSeparator('=')
            .argName("key=value")
            .build();

    static final List<StartupCommand> parseOfbizCommands(final String[] args) throws StartupException {
        CommandLine commandLine = null;
        CommandLineParser parser = new DefaultParser();
        try {
            commandLine = parser.parse(StartupCommandUtil.getOfbizStartupOptions(), args);
        } catch (ParseException e) {
            throw new StartupException(e.getMessage());
        }
        validateAllCommandArguments(commandLine);
        return mapCommonsCliOptionsToStartupCommands(commandLine);
    }

    static final void printOfbizStartupHelp(final PrintStream printStream) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(
                new PrintWriter(printStream, true),
                HelpFormatter.DEFAULT_WIDTH + 6,
                "java -jar ofbiz.jar",
                System.lineSeparator() + "Executes OFBiz command e.g. start, shutdown, check status, etc",
                getOfbizStartupOptions(),
                HelpFormatter.DEFAULT_LEFT_PAD,
                HelpFormatter.DEFAULT_DESC_PAD,
                "note: Only one command can execute at a time. Portoffset may be appended."
                    + System.lineSeparator()
                    + "Also a command must be invoked separately for each argument e.g."
                    + System.lineSeparator()
                    + "java -jar ofbiz.jar --test component=somecomp --test case=somecase",
                true);
    }

    static final void highlightAndPrintErrorMessage(String errorMessage) {
        System.err.println(
                "==============================================================================="
                + System.lineSeparator()
                + errorMessage
                + System.lineSeparator()
                + "==============================================================================="
                );
    }

    /**
     * Generates the loaderArgs with arguments as expected by the
     * containers that will receive them. 
     * 
     * TODO A better solution is to change the signature of all 
     * containers to receive a <tt>List</tt> of <tt>StartupCommand</tt>s
     * instead and delete the methods adaptStartupCommandsToLoaderArgs
     * and retrieveCommandArguments along with the loaderArgs list.
     */
    static List<String> adaptStartupCommandsToLoaderArgs(List<StartupCommand> ofbizCommands) {
        List<String> loaderArgs = new ArrayList<String>();
        final String LOAD_DATA = StartupCommandUtil.StartupOption.LOAD_DATA.getName();
        final String TEST = StartupCommandUtil.StartupOption.TEST.getName();
        final String TEST_LIST = StartupCommandUtil.StartupOption.TEST_LIST.getName();
        
        if(ofbizCommands.stream().anyMatch(command -> command.getName().equals(LOAD_DATA))) {
            retrieveCommandArguments(ofbizCommands, LOAD_DATA).entrySet().stream().forEach(entry -> 
            loaderArgs.add("-" + entry.getKey() + "=" + entry.getValue()));
        } else if(ofbizCommands.stream().anyMatch(command -> command.getName().equals(TEST))) {
            retrieveCommandArguments(ofbizCommands, TEST).entrySet().stream().forEach(entry -> 
            loaderArgs.add("-" + entry.getKey() + "=" + entry.getValue()));
        } else if(ofbizCommands.stream().anyMatch(command -> command.getName().equals(TEST_LIST))) {
            Map<String,String> testListArgs = retrieveCommandArguments(ofbizCommands, TEST_LIST);
            loaderArgs.add(testListArgs.get("file"));
            loaderArgs.add("-" + testListArgs.get("mode"));
        }
        return loaderArgs;
    }

    private static Map<String,String> retrieveCommandArguments(List<StartupCommand> ofbizCommands, String commandName) {
        return ofbizCommands.stream()
                .filter(option-> option.getName().equals(commandName))
                .collect(Collectors.toList()).get(0).getProperties();
    }

    private static final Options getOfbizStartupOptions() {
        OptionGroup ofbizCommandOptions = new OptionGroup();
        ofbizCommandOptions.addOption(BOTH);
        ofbizCommandOptions.addOption(HELP);
        ofbizCommandOptions.addOption(LOAD_DATA);
        ofbizCommandOptions.addOption(POS);
        ofbizCommandOptions.addOption(SHUTDOWN);
        ofbizCommandOptions.addOption(START);
        ofbizCommandOptions.addOption(STATUS);
        ofbizCommandOptions.addOption(TEST);
        ofbizCommandOptions.addOption(TEST_LIST);

        Options options = new Options();
        options.addOptionGroup(ofbizCommandOptions);
        options.addOption(PORTOFFSET);
        return options;
    }

    private static final List<StartupCommand> mapCommonsCliOptionsToStartupCommands(final CommandLine commandLine) {
        List<Option> optionList = Arrays.asList(commandLine.getOptions()); 
        return optionList.stream()
                .map(option -> new StartupCommand.Builder(option.getLongOpt())
                    .properties(populateMapFromProperties(commandLine.getOptionProperties(option.getLongOpt())))
                    .build())
                .collect(Collectors.toList());
    }

    private static final Map<String,String> populateMapFromProperties(final Properties properties) {
        return properties.entrySet().stream().collect(Collectors.toMap(
                entry -> String.valueOf(entry.getKey()),
                entry -> String.valueOf(entry.getValue())));
    }

    private static final void validateAllCommandArguments(CommandLine commandLine) throws StartupException {
        // Make sure no extra options are passed
        if(!commandLine.getArgList().isEmpty()) {
            throw new StartupException("unrecognized options / properties: " + commandLine.getArgList());
        }
        // TEST_LIST validation
        if(commandLine.hasOption(StartupOption.TEST_LIST.getName())) {
            Properties optionProperties = commandLine.getOptionProperties(StartupOption.TEST_LIST.getName());
            if(!optionProperties.containsKey("file")
                    || !optionProperties.containsKey("mode") ) {
                throw new StartupException("You must pass both file and mode arguments to --" + StartupOption.TEST_LIST.getName());
            } else if (!optionProperties.get("mode").toString().equals("text")
                    && !optionProperties.get("mode").toString().equals("ant")) {
                throw new StartupException("mode only accepts text or ant in the option --"  + StartupOption.TEST_LIST.getName());
            }
        // PORTOFFSET validation
        } else if(commandLine.hasOption(StartupOption.PORTOFFSET.getName())) {
            Properties optionProperties = commandLine.getOptionProperties(StartupOption.PORTOFFSET.getName());
            try {
                int portOffset = Integer.parseInt(optionProperties.keySet().iterator().next().toString());
                if(portOffset < 0) {
                    throw new StartupException("you can only pass positive integers to the option --" + StartupOption.PORTOFFSET.getName());
                }
            } catch (NumberFormatException e) {
                throw new StartupException("you can only pass positive integers to the option --" + StartupOption.PORTOFFSET.getName(), e);
            }
        }
        //TODO add more validations
    }
}
