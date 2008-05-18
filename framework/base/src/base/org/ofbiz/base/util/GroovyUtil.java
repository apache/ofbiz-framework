package org.ofbiz.base.util;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import org.ofbiz.base.location.FlexibleLocation;
import org.ofbiz.base.util.cache.UtilCache;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.codehaus.groovy.control.CompilationFailedException;


/**
 * GroovyUtil - Groovy Utilities
 *
 * @version $Rev$
 */
public class GroovyUtil {

    public static final String module = GroovyUtil.class.getName();

    public static UtilCache<String, Script> parsedScripts = new UtilCache<String, Script>("script.GroovyLocationParsedCache", 0, 0, false);


    private static GroovyShell getShell(Map context) {

        Binding binding = new Binding();
        if (context != null) {
            Set keySet = context.keySet();
            for (Object key : keySet) {
                binding.setVariable((String) key, context.get(key));
            }

            // include the context itself in for easier access in the scripts
            binding.setVariable("context", context);
        }

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return new GroovyShell(classLoader, binding);
    }

    public static Object runScriptAtLocation(String location, Map context) throws GeneralException {

        try {

            Script script = parsedScripts.get(location);
            if (script == null) {

                URL scriptUrl = FlexibleLocation.resolveLocation(location);
                GroovyShell shell = getShell(context);
                script = shell.parse(scriptUrl.openStream(), scriptUrl.getFile());
                if (Debug.verboseOn()) {
                    Debug.logVerbose("Caching Groovy script at: " + location, module);
                }
                parsedScripts.put(location, script);
            }
            
            return script.run();

        } catch (MalformedURLException e) {
            String errMsg = "Error loading Groovy script at [" + location + "]: " + e.toString();
            throw new GeneralException(errMsg, e);
        } catch (IOException e) {
            String errMsg = "Error loading Groovy script at [" + location + "]: " + e.toString();
            throw new GeneralException(errMsg, e);
        } catch (CompilationFailedException e) {
            String errMsg = "Error loading Groovy script at [" + location + "]: " + e.toString();
            throw new GeneralException(errMsg, e);
        }
    }

}
