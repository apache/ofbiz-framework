package org.apache.ofbiz.base.container;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.ofbiz.base.start.StartupCommand;
import org.apache.ofbiz.base.start.StartupCommandUtil;

/**
 * Temporary class. This class is responsible for converting
 * StartupCommand instances to String[]. This is a workaround until all
 * containers have fully changed their signatures to adopt the StartupCommand
 * object 
 */
public class StartupCommandToArgsAdapter {
    /**
     * Generates the loaderArgs with arguments as expected by the
     * containers that will receive them. 
     * 
     * TODO A better solution is to change the signature of all 
     * containers to receive a <tt>List</tt> of <tt>StartupCommand</tt>s
     * instead and delete the methods adaptStartupCommandsToLoaderArgs
     * and retrieveCommandArguments along with the loaderArgs list.
     */
    public static String[] adaptStartupCommandsToLoaderArgs(List<StartupCommand> ofbizCommands) {
        List<String> loaderArgs = new ArrayList<String>();
        final String LOAD_DATA = StartupCommandUtil.StartupOption.LOAD_DATA.getName();
        final String TEST = StartupCommandUtil.StartupOption.TEST.getName();
        
        if(ofbizCommands.stream().anyMatch(command -> command.getName().equals(LOAD_DATA))) {
            retrieveCommandArguments(ofbizCommands, LOAD_DATA).entrySet().stream().forEach(entry -> 
            loaderArgs.add("-" + entry.getKey() + "=" + entry.getValue()));
        } else if(ofbizCommands.stream().anyMatch(command -> command.getName().equals(TEST))) {
            retrieveCommandArguments(ofbizCommands, TEST).entrySet().stream().forEach(entry -> 
            loaderArgs.add("-" + entry.getKey() + "=" + entry.getValue()));
        }
        return loaderArgs.toArray(new String[loaderArgs.size()]);
    }

    private static Map<String,String> retrieveCommandArguments(List<StartupCommand> ofbizCommands, String commandName) {
        return ofbizCommands.stream()
                .filter(option-> option.getName().equals(commandName))
                .collect(Collectors.toList()).get(0).getProperties();
    }
}
