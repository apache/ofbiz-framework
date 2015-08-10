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
package org.ofbiz.base.conversion;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/** Net Converter classes. */
public class NetConverters implements ConverterLoader {
    public static final String module = NetConverters.class.getName();

    public static class StringToInetAddress extends AbstractConverter<String, InetAddress> {
        public StringToInetAddress() {
            super(String.class, InetAddress.class);
        }

        public InetAddress convert(String obj) throws ConversionException {
            try {
                return InetAddress.getByName(obj);
            } catch (IOException e) {
                throw (ConversionException) new ConversionException(e.getMessage()).initCause(e);
            }
        }
    }

    public static class InetAddressToString extends AbstractConverter<InetAddress, String> {
        public InetAddressToString() {
            super(InetAddress.class, String.class);
        }

        public String convert(InetAddress obj) throws ConversionException {
            String hostName = obj.getHostName();
            if (hostName != null) return hostName;
            return obj.getHostAddress();
        }
    }

    public static class StringToURI extends AbstractConverter<String, URI> {
        public StringToURI() {
            super(String.class, URI.class);
        }

        public URI convert(String obj) throws ConversionException {
            try {
                return new URI(obj);
            } catch (URISyntaxException e) {
                throw (ConversionException) new ConversionException(e.getMessage()).initCause(e);
            }
        }
    }

    public static class URIToString extends AbstractConverter<URI, String> {
        public URIToString() {
            super(URI.class, String.class);
        }

        public String convert(URI obj) throws ConversionException {
            return obj.toString();
        }
    }

    public static class StringToURL extends AbstractConverter<String, URL> {
        public StringToURL() {
            super(String.class, URL.class);
        }

        public URL convert(String obj) throws ConversionException {
            try {
                return new URL(obj);
            } catch (MalformedURLException e) {
                throw (ConversionException) new ConversionException(e.getMessage()).initCause(e);
            }
        }
    }

    public static class URLToString extends AbstractConverter<URL, String> {
        public URLToString() {
            super(URL.class, String.class);
        }

        public String convert(URL obj) throws ConversionException {
            return obj.toString();
        }
    }

    public static class URIToURL extends AbstractConverter<URI, URL> {
        public URIToURL() {
            super(URI.class, URL.class);
        }

        public URL convert(URI obj) throws ConversionException {
            try {
                return obj.toURL();
            } catch (MalformedURLException e) {
                throw (ConversionException) new ConversionException(e.getMessage()).initCause(e);
            }
        }
    }

    public static class URLToURI extends AbstractConverter<URL, URI> {
        public URLToURI() {
            super(URL.class, URI.class);
        }

        public URI convert(URL obj) throws ConversionException {
            try {
                return obj.toURI();
            } catch (URISyntaxException e) {
                throw (ConversionException) new ConversionException(e.getMessage()).initCause(e);
            }
        }
    }

    public void loadConverters() {
        Converters.loadContainedConverters(NetConverters.class);
    }
}
