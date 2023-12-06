package org.apache.ofbiz.base.util;

import org.apache.ofbiz.base.util.cache.UtilCache;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * Custom ResourceBundle class. This class extends ResourceBundle
 * to add custom bundle caching code and support for the OFBiz custom XML
 * properties file format.
 */
public class UtilResourceBundle extends ResourceBundle {

    private static final String MODULE = UtilResourceBundle.class.getName();
    private static final UtilCache<String, UtilResourceBundle> BUNDLE_CACHE = UtilCache.createUtilCache("properties.UtilPropertiesBundleCache");
    private Properties properties = null;
    private Locale locale = null;
    private int hashCode = hashCode();

    protected UtilResourceBundle() {
    }

    UtilResourceBundle(Properties properties, Locale locale, UtilResourceBundle parent) {
        this.properties = properties;
        this.locale = locale;
        setParent(parent);
        String hashString = properties.toString();
        if (parent != null) {
            hashString += parent.properties;
        }
        this.hashCode = hashString.hashCode();
    }

    public static ResourceBundle getBundle(String resource, Locale locale, ClassLoader loader) throws MissingResourceException {
        String resourceName = UtilProperties.createResourceName(resource, locale, true);
        UtilResourceBundle bundle = BUNDLE_CACHE.get(resourceName);
        if (bundle == null) {
            double startTime = System.currentTimeMillis();
            List<Locale> candidateLocales = UtilProperties.getCandidateLocales(locale);
            UtilResourceBundle parentBundle = null;
            int numProperties = 0;
            while (!candidateLocales.isEmpty()) {
                Locale candidateLocale = candidateLocales.remove(candidateLocales.size() - 1);
                // ResourceBundles are connected together as a singly-linked list
                String lookupName = UtilProperties.createResourceName(resource, candidateLocale, true);
                UtilResourceBundle lookupBundle = BUNDLE_CACHE.get(lookupName);
                if (lookupBundle == null) {
                    Properties newProps = UtilProperties.getProperties(resource, candidateLocale);
                    if (UtilValidate.isNotEmpty(newProps)) {
                        // The last bundle we found becomes the parent of the new bundle
                        parentBundle = bundle;
                        bundle = new UtilResourceBundle(newProps, candidateLocale, parentBundle);
                        BUNDLE_CACHE.putIfAbsent(lookupName, bundle);
                        numProperties = newProps.size();
                    }
                } else {
                    parentBundle = bundle;
                    bundle = lookupBundle;
                }
            }
            if (bundle == null) {
                throw new MissingResourceException("Resource " + resource + ", locale " + locale + " not found", null, null);
            } else if (!bundle.getLocale().equals(locale)) {
                // Create a "dummy" bundle for the requested locale
                bundle = new UtilResourceBundle(bundle.properties, locale, parentBundle);
            }
            double totalTime = System.currentTimeMillis() - startTime;
            if (Debug.infoOn()) {
                Debug.logInfo("ResourceBundle " + resource + " (" + locale + ") created in " + totalTime / 1000.0 + "s with "
                        + numProperties + " properties", UtilResourceBundle.MODULE);
            }
            BUNDLE_CACHE.putIfAbsent(resourceName, bundle);
        }
        return bundle;
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == null ? false : obj.hashCode() == this.hashCode;
    }

    @Override
    public Locale getLocale() {
        return this.locale;
    }

    @Override
    protected Object handleGetObject(String key) {
        return properties.get(key);
    }

    @Override
    public Enumeration<String> getKeys() {
        return new Enumeration<String>() {
            private Iterator<String> i = UtilGenerics.cast(properties.keySet().iterator());

            @Override
            public boolean hasMoreElements() {
                return (i.hasNext());
            }

            @Override
            public String nextElement() {
                return i.next();
            }
        };
    }

}
