/*
 * $Id: CompressionInputStream.java 5462 2005-08-05 18:35:48Z jonesde $
 *
 * Copyright (c) 1998, 1999 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license to use,
 * modify and redistribute this software in source and binary code form,
 * provided that i) this copyright notice and license appear on all copies of
 * the software; and ii) Licensee does not utilize the software in a manner
 * which is disparaging to Sun.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
 * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line control of
 * aircraft, air traffic, aircraft navigation or aircraft communications; or in
 * the design, construction, operation or maintenance of any nuclear
 * facility. Licensee represents and warrants that it will not use or
 * redistribute the Software for such purposes.
 */

package org.ofbiz.service.rmi.socket.zip;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CompressionInputStream extends FilterInputStream implements CompressionConstants {
    /*
     * Constructor calls constructor of superclass
     */
    public CompressionInputStream(InputStream in) {
        super(in);
    }

    /*
     * Buffer of unpacked 6-bit codes
     * from last 32 bits read.
     */
    int buf[] = new int[5];

    /*
     * Position of next code to read in buffer (5 signifies end).
     */
    int bufPos = 5;

    /*
     * Reads in format code and decompresses character accordingly.
     */

    public int read() throws IOException {
        try {
            int code;

            // Read in and ignore empty bytes (NOP's) as long as they
            // arrive.
            do {
                code = readCode();
            } while (code == NOP);

            if (code >= BASE) {
                // Retrieve index of character in codeTable if the
                // code is in the correct range.
                return codeTable.charAt(code - BASE);
            } else if (code == RAW) {
                // read in the lower 4 bits and the higher 4 bits,
                // and return the reconstructed character
                int high = readCode();
                int low = readCode();
                return (high << 4) | low;
            } else
                throw new IOException("unknown compression code: " + code);
        } catch (EOFException e) {
            // Return the end of file code
            return -1;
        }
    }

    /*
     * This method reads up to len bytes from the input stream.
     * Returns if read blocks before len bytes are read.
     */
    public int read(byte b[], int off, int len) throws IOException {

        if (len <= 0) {
            return 0;
        }

        int c = read();
        if (c == -1) {
            return -1;
        }
        b[off] = (byte) c;

        int i = 1;
        // Try to read up to len bytes or until no
        // more bytes can be read without blocking.
        try {
            for (; (i < len) && (in.available() > 0); i++) {
                c = read();
                if (c == -1) {
                    break;
                }
                if (b != null) {
                    b[off + i] = (byte) c;
                }
            }
        } catch (IOException ee) {
        }
        return i;
    }

    /*
     * If there is no more data to decode left in buf, read the
     * next four bytes from the wire. Then store each group of 6
     * bits in an element of buf.  Return one element of buf.
     */
    private int readCode() throws IOException {
        // As soon as all the data in buf has been read
        // (when bufPos == 5) read in another four bytes.
        if (bufPos == 5) {
            int b1 = in.read();
            int b2 = in.read();
            int b3 = in.read();
            int b4 = in.read();

            // make sure none of the bytes signify the
            // end of the data in the stream
            if ((b1 | b2 | b3 | b4) < 0) {
                throw new EOFException();
            }
            // Assign each group of 6 bits to an element of
            // buf
            int pack = (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
            buf[0] = (pack >>> 24) & 0x3F;
            buf[1] = (pack >>> 18) & 0x3F;
            buf[2] = (pack >>> 12) & 0x3F;
            buf[3] = (pack >>> 6) & 0x3F;
            buf[4] = (pack >>> 0) & 0x3F;
            bufPos = 0;
        }
        return buf[bufPos++];
    }
}
