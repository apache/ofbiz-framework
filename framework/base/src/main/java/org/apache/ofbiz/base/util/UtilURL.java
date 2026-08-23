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
package org.apache.ofbiz.base.util;

import java.io.File;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * URL Utilities - Simple Class for flexibly working with properties files
 *
 */
public final class UtilURL {

    private static final String MODULE = UtilURL.class.getName();
    private static final Map<String, URL> URL_MAP = new ConcurrentHashMap<>();

    private UtilURL() { }

    public static <C> URL fromClass(Class<C> contextClass) {
        String resourceName = contextClass.getName();
        int dotIndex = resourceName.lastIndexOf('.');

        if (dotIndex != -1) {
            resourceName = resourceName.substring(0, dotIndex);
        }
        resourceName += ".properties";

        return fromResource(contextClass, resourceName);
    }

    /**
     * Returns a <code>URL</code> instance from a resource name. Returns
     * <code>null</code> if the resource is not found.
     * <p>This method uses various ways to locate the resource, and in all
     * cases it tests to see if the resource exists - so it
     * is very inefficient.
     *
     * @param resourceName
     * @return
     */
    public static URL fromResource(String resourceName) {
        return fromResource(resourceName, null);
    }

    public static <C> URL fromResource(Class<C> contextClass, String resourceName) {
        if (contextClass == null) {
            return fromResource(resourceName, null);
        }
        return fromResource(resourceName, contextClass.getClassLoader());
    }

    /**
     * Returns a <code>URL</code> instance from a resource name. Returns
     * <code>null</code> if the resource is not found.
     * <p>This method uses various ways to locate the resource, and in all
     * cases it tests to see if the resource exists - so it
     * is very inefficient.
     *
     * @param resourceName
     * @param loader
     * @return
     */
    public static URL fromResource(String resourceName, ClassLoader loader) {
        URL url = URL_MAP.get(resourceName);
        URI uri;
        if (url != null) {
            try {
                uri = new URI(url.toString());
                url = uri.toURL();
            } catch (IllegalArgumentException | URISyntaxException | MalformedURLException e) {
                Debug.logWarning(e, "Exception thrown while copying URL", MODULE);
            }
        }
        if (loader == null) {
            try {
                loader = Thread.currentThread().getContextClassLoader();
            } catch (SecurityException e) {
                // Huh? The new object will be created by the current thread, so how is this any different than the previous code?
                loader = UtilURL.class.getClassLoader();
            }
        }
        url = loader.getResource(resourceName);
        if (url != null) {
            URL_MAP.put(resourceName, url);
            return url;
        }
        url = ClassLoader.getSystemResource(resourceName);
        if (url != null) {
            URL_MAP.put(resourceName, url);
            return url;
        }
        url = fromFilename(resourceName);
        if (url != null) {
            URL_MAP.put(resourceName, url);
            return url;
        }
        url = fromOfbizHomePath(resourceName);
        if (url != null) {
            URL_MAP.put(resourceName, url);
            return url;
        }
        url = fromUrlString(resourceName);
        if (url != null) {
            URL_MAP.put(resourceName, url);
        }
        return url;
    }

    public static URL fromFilename(String filename) {
        if (filename == null) {
            return null;
        }
        File file = new File(filename);
        URL url = null;

        try {
            if (file.exists()) {
                url = file.toURI().toURL();
            }
        } catch (MalformedURLException e) {
            Debug.logError(e, "unable to retrieve URL for file: " + filename, MODULE);
        }
        return url;
    }

    public static URL fromUrlString(String urlString) {
        URL url = null;
        URI uri;
        try {
            uri = new URI(urlString);
            url = uri.toURL();
        } catch (IllegalArgumentException | URISyntaxException | MalformedURLException e) {
            // We purposely don't want to do anything here.
        }
        return url;
    }

    /**
     * Same as {@link #fromUrlString(String)}, except the result is rejected unless it is safe to
     * dereference: an http or https URL whose host does not resolve to a loopback, link-local,
     * private, or otherwise reserved address. Use this instead of {@link #fromUrlString(String)}
     * whenever the URL comes from an untrusted source (e.g. a request parameter) and will be
     * fetched by the server, to avoid server-side request forgery against the local file system,
     * loopback services, and the internal network.
     *
     * <p>An optional host allow-list can be configured with the
     * {@code webtools.datafile.url.allowed.hosts} property in security.properties: a
     * comma-separated list of hostnames/domains. When set, only URLs whose host matches an entry
     * (or is a subdomain of one) are allowed, in addition to the address checks above.
     *
     * @throws GeneralException if the URL is present but not allowed
     */
    public static URL fromCheckedUrlString(String urlString) throws GeneralException {
        if (urlString == null) {
            return null;
        }
        URL url = fromUrlString(urlString);
        if (url == null) {
            return null;
        }
        checkUrlResourceAllowed(url);
        return url;
    }

    /**
     * Throws {@link GeneralException} unless {@code url} is an http or https URL whose host
     * resolves only to publicly routable addresses (and, when configured, is in the
     * {@code webtools.datafile.url.allowed.hosts} allow-list). See {@link #fromCheckedUrlString}.
     */
    public static void checkUrlResourceAllowed(URL url) throws GeneralException {
        String protocol = url.getProtocol();
        if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
            throw new GeneralException("URL only supports http/https protocols; rejected: " + protocol);
        }
        String host = url.getHost();
        if (UtilValidate.isEmpty(host)) {
            throw new GeneralException("URL has no host component");
        }

        String allowedHostsStr = UtilProperties.getPropertyValue("security", "webtools.datafile.url.allowed.hosts", "");
        if (UtilValidate.isNotEmpty(allowedHostsStr)) {
            String lcHost = host.toLowerCase(Locale.ROOT);
            boolean hostAllowed = false;
            for (String entry : allowedHostsStr.split(",")) {
                String allowedEntry = entry.trim().toLowerCase(Locale.ROOT);
                if (UtilValidate.isEmpty(allowedEntry)) {
                    continue;
                }
                if (lcHost.equals(allowedEntry) || lcHost.endsWith("." + allowedEntry)) {
                    hostAllowed = true;
                    break;
                }
            }
            if (!hostAllowed) {
                throw new GeneralException("URL host is not in the allowed list: " + host);
            }
        }

        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new GeneralException("URL host cannot be resolved: " + host);
        }
        if (addresses.length == 0) {
            throw new GeneralException("URL host resolved to no addresses: " + host);
        }
        for (InetAddress addr : addresses) {
            checkNotPrivateOrReservedAddress(addr);
        }
    }

    /**
     * Throws {@link GeneralException} if {@code addr} belongs to a private, loopback,
     * link-local, or otherwise reserved IP range (IPv4 and IPv6).
     */
    private static void checkNotPrivateOrReservedAddress(InetAddress addr) throws GeneralException {
        if (addr.isLoopbackAddress()) {
            throw new GeneralException("URL target resolves to a loopback address: " + addr.getHostAddress());
        }
        if (addr.isLinkLocalAddress()) {
            throw new GeneralException("URL target resolves to a link-local address: " + addr.getHostAddress());
        }
        if (addr.isSiteLocalAddress()) {
            throw new GeneralException("URL target resolves to a private (site-local) address: " + addr.getHostAddress());
        }
        if (addr.isAnyLocalAddress()) {
            throw new GeneralException("URL target resolves to a wildcard address: " + addr.getHostAddress());
        }
        if (addr.isMulticastAddress()) {
            throw new GeneralException("URL target resolves to a multicast address: " + addr.getHostAddress());
        }
        byte[] b = addr.getAddress();
        if (addr instanceof Inet4Address) {
            int i0 = b[0] & 0xFF;
            int i1 = b[1] & 0xFF;
            // 0.0.0.0/8 - "this" network (RFC 1122)
            if (i0 == 0) {
                throw new GeneralException("URL target resolves to a reserved network address (0.0.0.0/8): " + addr.getHostAddress());
            }
            // 100.64.0.0/10 - shared address space / CGNAT (RFC 6598)
            if (i0 == 100 && i1 >= 64 && i1 <= 127) {
                throw new GeneralException("URL target resolves to a shared address space (CGNAT, 100.64.0.0/10): " + addr.getHostAddress());
            }
            // 192.0.0.0/24 - IETF protocol assignments (RFC 6890)
            if (i0 == 192 && i1 == 0 && (b[2] & 0xFF) == 0) {
                throw new GeneralException("URL target resolves to an IETF reserved address (192.0.0.0/24): " + addr.getHostAddress());
            }
            // 198.18.0.0/15 - network benchmarking (RFC 2544)
            if (i0 == 198 && (i1 == 18 || i1 == 19)) {
                throw new GeneralException("URL target resolves to a benchmarking address (198.18.0.0/15): " + addr.getHostAddress());
            }
            // 240.0.0.0/4 - reserved for future use (RFC 1112)
            if ((i0 & 0xF0) == 240) {
                throw new GeneralException("URL target resolves to a reserved address (240.0.0.0/4): " + addr.getHostAddress());
            }
        } else if (addr instanceof Inet6Address) {
            // fc00::/7 - Unique Local Addresses (ULA), private IPv6 (RFC 4193)
            if ((b[0] & 0xFE) == 0xFC) {
                throw new GeneralException("URL target resolves to a unique-local (private) IPv6 address: " + addr.getHostAddress());
            }
            // ::ffff:0:0/96 - IPv4-mapped IPv6; re-validate the embedded IPv4 address
            boolean isIpv4Mapped = true;
            for (int i = 0; i < 10; i++) {
                if (b[i] != 0) {
                    isIpv4Mapped = false;
                    break;
                }
            }
            if (isIpv4Mapped && (b[10] & 0xFF) == 0xFF && (b[11] & 0xFF) == 0xFF) {
                try {
                    checkNotPrivateOrReservedAddress(
                            InetAddress.getByAddress(new byte[]{b[12], b[13], b[14], b[15]}));
                } catch (UnknownHostException e) {
                    throw new GeneralException("URL target contains an invalid IPv4-mapped IPv6 address");
                }
            }
        }
    }

    public static URL fromOfbizHomePath(String filename) {
        String ofbizHome = System.getProperty("ofbiz.home");
        if (ofbizHome == null) {
            Debug.logWarning("No ofbiz.home property set in environment", MODULE);
            return null;
        }
        String newFilename = ofbizHome;
        if (!newFilename.endsWith("/") && !filename.startsWith("/")) {
            newFilename = newFilename + "/";
        }
        newFilename = newFilename + filename;
        return fromFilename(newFilename);
    }

    public static String getOfbizHomeRelativeLocation(URL fileUrl) {
        String ofbizHome = System.getProperty("ofbiz.home");
        String path = fileUrl.getPath();
        if (path.startsWith(ofbizHome)) {
            // note: the +1 is to remove the leading slash
            path = path.substring(ofbizHome.length() + 1);
        }
        return path;
    }
}
