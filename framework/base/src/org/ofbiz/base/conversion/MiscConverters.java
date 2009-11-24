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
import java.io.Reader;
import java.sql.Blob;
import java.sql.Clob;
import java.util.Locale;

import org.ofbiz.base.util.UtilMisc;

/** Miscellaneous Converter classes. */
public class MiscConverters implements ConverterLoader {

    public static final int CHAR_BUFFER_SIZE = 4096;

    public static class BlobToBlob extends AbstractConverter<Blob, Blob> {
        public BlobToBlob() {
            super(Blob.class, Blob.class);
        }

        public Blob convert(Blob obj) throws ConversionException {
            try {
                return new javax.sql.rowset.serial.SerialBlob(obj.getBytes(1, (int) obj.length()));
            } catch (Exception e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class BlobToByteArray extends AbstractConverter<Blob, byte[]> {
        public BlobToByteArray() {
            super(Blob.class, byte[].class);
        }

        public byte[] convert(Blob obj) throws ConversionException {
            try {
                return obj.getBytes(1, (int) obj.length());
            } catch (Exception e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class ByteArrayToBlob extends AbstractConverter<byte[], Blob> {
        public ByteArrayToBlob() {
            super(byte[].class, Blob.class);
        }

        public Blob convert(byte[] obj) throws ConversionException {
            try {
                return new javax.sql.rowset.serial.SerialBlob(obj);
            } catch (Exception e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class ClobToString extends AbstractConverter<Clob, String> {
        public ClobToString() {
            super(Clob.class, String.class);
        }

        public String convert(Clob obj) throws ConversionException {
            StringBuilder strBuf = new StringBuilder();
            char[] inCharBuffer = new char[CHAR_BUFFER_SIZE];
            int charsRead = 0;
            Reader clobReader = null;
            try {
                clobReader =  obj.getCharacterStream();
                while ((charsRead = clobReader.read(inCharBuffer, 0, CHAR_BUFFER_SIZE)) > 0) {
                    strBuf.append(inCharBuffer, 0, charsRead);
                }
            } catch (Exception e) {
                throw new ConversionException(e);
            }
            finally {
                if (clobReader != null) {
                    try {
                        clobReader.close();
                    } catch (IOException e) {}
                }
            }
            return strBuf.toString();
        }
    }

    public static class LocaleToString extends AbstractConverter<Locale, String> {
        public LocaleToString() {
            super(Locale.class, String.class);
        }

        public String convert(Locale obj) throws ConversionException {
             return obj.toString();
        }
    }

    public static class StringToClob extends AbstractConverter<String, Clob> {
        public StringToClob() {
            super(String.class, Clob.class);
        }

        public Clob convert(String obj) throws ConversionException {
            try {
                return new javax.sql.rowset.serial.SerialClob(obj.toCharArray());
            } catch (Exception e) {
                throw new ConversionException(e);
            }
        }
    }

    public static class StringToLocale extends AbstractConverter<String, Locale> {
        public StringToLocale() {
            super(String.class, Locale.class);
        }

        public Locale convert(String obj) throws ConversionException {
            Locale loc = UtilMisc.parseLocale(obj);
            if (loc != null) {
                return loc;
            } else {
                throw new ConversionException("Could not convert " + obj + " to Locale: ");
            }
        }
    }

    public void loadConverters() {
        Converters.loadContainedConverters(MiscConverters.class);
    }
}
