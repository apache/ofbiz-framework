package org.ofbiz.base.start;

import java.io.PrintStream;
import java.io.PrintWriter;
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
public final class StartupCommandUtil {

    /* 
     * Make sure of defining the same set of values in:
     * 
     * - The StartupOptions defined below
     * - The commons-cli options defined below
     * - The getOfbizStartupOptions method defined below
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
        START_BATCH("batch"),
        START_DEBUG("debug"),
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
    private static final Option START_BATCH = Option.builder("c")
            .longOpt(StartupOption.START_BATCH.getName())
            .desc("Start OFBiz in batch mode")
            .hasArg(false)
            .build();
    private static final Option START_DEBUG = Option.builder("g")
            .longOpt(StartupOption.START_DEBUG.getName())
            .desc("Start OFBiz in debug mode")
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
                    + "--test component=base --test suitename=somesuite")
            .numberOfArgs(2)
            .valueSeparator('=')
            .optionalArg(true)
            .argName("key=value")
            .build();
    private static final Option TEST_LIST = Option.builder("x")
            .longOpt(StartupOption.TEST_LIST.getName())
            .desc("run the tests configured through testtools e.g.: "
                    + System.lineSeparator()
                    + "--testlist file=runtime/test-list-build.xml"
                    + System.lineSeparator()
                    + "--testlist mode=ant or --testlist mode=text")
            .numberOfArgs(2)
            .valueSeparator('=')
            .optionalArg(true)
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
        if(!commandLine.getArgList().isEmpty()) {
            throw new StartupException("unrecognized options / properties: " + commandLine.getArgList());
        }
        //TODO add validation logic for each command's options
        return mapCommonsCliOptionsToStartupCommands(commandLine);
    }

    static final void printOfbizStartupHelp(final PrintStream printStream) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(
                new PrintWriter(printStream, true),
                HelpFormatter.DEFAULT_WIDTH,
                "java -jar ofbiz.jar",
                System.lineSeparator() + "Executes OFBiz command e.g. start, shutdown, check status, etc",
                getOfbizStartupOptions(),
                HelpFormatter.DEFAULT_LEFT_PAD,
                HelpFormatter.DEFAULT_DESC_PAD,
                "note: Only one command can execute at a time. Portoffset may be appended",
                true);
    }

    private static final List<StartupCommand> mapCommonsCliOptionsToStartupCommands(final CommandLine commandLine) {
        List<Option> optionList = Arrays.asList(commandLine.getOptions()); 
        return optionList.stream()
                .map(option -> new StartupCommand.Builder(option.getLongOpt())
                    .properties(populateMapFromProperties(commandLine.getOptionProperties(option.getLongOpt())))
                    .build())
                .collect(Collectors.toList());
    }

    private static final Map<String,String> populateMapFromProperties(Properties properties) {
        return properties.entrySet().stream().collect(Collectors.toMap(
                entry -> String.valueOf(entry.getKey()),
                entry -> String.valueOf(entry.getValue())));
    }

    private static final Options getOfbizStartupOptions() {
        OptionGroup ofbizCommandOptions = new OptionGroup();
        ofbizCommandOptions.addOption(BOTH);
        ofbizCommandOptions.addOption(HELP);
        ofbizCommandOptions.addOption(LOAD_DATA);
        ofbizCommandOptions.addOption(POS);
        ofbizCommandOptions.addOption(SHUTDOWN);
        ofbizCommandOptions.addOption(START);
        ofbizCommandOptions.addOption(START_BATCH);
        ofbizCommandOptions.addOption(START_DEBUG);
        ofbizCommandOptions.addOption(STATUS);
        ofbizCommandOptions.addOption(TEST);
        ofbizCommandOptions.addOption(TEST_LIST);

        Options options = new Options();
        options.addOptionGroup(ofbizCommandOptions);
        options.addOption(PORTOFFSET);
        return options;
    }
}
