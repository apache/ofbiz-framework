package org.ofbiz.base.start;

import java.util.Map;

/**
 * A command line argument passed to ofbiz
 * 
 * <p>
 * A <tt>StartupCommand</tt> represents a processed command line argument passed 
 * to ofbiz such that it is no longer a raw string but an instance of this class.
 * For example: <code>java -jar ofbiz.jar --status</code> where status is a command.
 * </p>
 */
public class StartupCommand {
    private String name;
    private Map<String,String> properties;

    public String getName() {
        return name;
    }
    public Map<String,String> getProperties() {
        return properties;
    }

    private StartupCommand(Builder builder) {
        this.name = builder.name;
        this.properties = builder.properties;
    }

    public static class Builder {
        //required parameters
        private final String name;

        //optional parameters       
        private Map<String,String> properties;

        public Builder(String name) {
            this.name = name;
        }
        public Builder properties(Map<String,String> properties) {
            this.properties = properties;
            return this;
        }

        public StartupCommand build() {
            return new StartupCommand(this);
        }
    }
}
