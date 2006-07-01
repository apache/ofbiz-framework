/*
 * $Id: CompressionOutputStream.java 5462 2005-08-05 18:35:48Z jonesde $
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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class CompressionOutputStream extends FilterOutputStream implements CompressionConstants {

    /*
     * Constructor calls constructor of superclass.
     */
    public CompressionOutputStream(OutputStream out) {
        super(out);
    }

    /*
     * Buffer of 6-bit codes to pack into next 32-bit word
     * Five 6-bit codes fit into 4 words.
     */
    int buf[] = new int[5];

    /*
     * Index of valid codes waiting in buf.
     */
    int bufPos = 0;


    /*
     * This method writes one byte to the socket stream.
     */
    public void write(int b) throws IOException {
        // force argument to one byte
        b &= 0xFF;

        // Look up pos in codeTable to get its encoding.
        int pos = codeTable.indexOf((char) b);

        if (pos != -1) {
            // If pos is in the codeTable, write BASE + pos into buf.
            // By adding BASE to pos, we know that the characters in
            // the codeTable will always have a code between 2 and 63
            // inclusive. This allows us to use RAW (RAW is equal to
            // 1) to signify that the next two groups of 6-bits are
            // necessary for decompression of the next character.

            writeCode(BASE + pos);
        } else {
            // Otherwise, write RAW into buf to signify that the
            // Character is being sent in 12 bits.
            writeCode(RAW);

            // Write the last 4 bits of b into the buf.
            writeCode(b >> 4);

            // Truncate b to contain data in only the first 4 bits,
            // and write the first 4 bits of b into buf.
            writeCode(b & 0xF);
        }
    }

    /*
     * This method writes up to len bytes to the socket stream.
     */
    public void write(byte b[], int off, int len) throws IOException {
        /*
         * This implementation is quite inefficient because it has to
         * call the other write method for every byte in the array.  It
         * could be optimized for performance by doing all the processing
         * in this method.
         */
        for (int i = 0; i < len; i++)
            write(b[off + i]);
    }


    /*
     * Clears buffer of all data (zeroes it out).
     */
    public void flush() throws IOException {
        while (bufPos > 0)
            writeCode(NOP);
    }

    /*
     * This method actually puts the data into the output stream after
     * packing the data from all 5 bytes in buf into one word.
     * Remember, each byte has, at most, 6 significant bits.
     */
    private void writeCode(int c) throws IOException {
        buf[bufPos++] = c;
        if (bufPos == 5) {      // write next word when we have 5 codes
            int pack = (buf[0] << 24) | (buf[1] << 18) | (buf[2] << 12) |
                    (buf[3] << 6) | buf[4];
            out.write((pack >>> 24) & 0xFF);
            out.write((pack >>> 16) & 0xFF);
            out.write((pack >>> 8) & 0xFF);
            out.write((pack >>> 0) & 0xFF);
            bufPos = 0;
        }
    }
}
