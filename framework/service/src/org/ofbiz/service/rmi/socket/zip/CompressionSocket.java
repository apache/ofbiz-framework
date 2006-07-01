/*
 * $Id: CompressionSocket.java 5462 2005-08-05 18:35:48Z jonesde $
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

class CompressionSocket extends Socket {

    /* InputStream used by socket */
    private InputStream in;
    /* OutputStream used by socket */
    private OutputStream out;

    /*
     * No-arg constructor for class CompressionSocket
     */
    public CompressionSocket() {
        super();
    }

    /*
     * Constructor for class CompressionSocket
     */
    public CompressionSocket(String host, int port) throws IOException {
        super(host, port);
    }

    /*
     * Returns a stream of type CompressionInputStream
     */
    public InputStream getInputStream() throws IOException {
        if (in == null) {
            in = new CompressionInputStream(super.getInputStream());
        }
        return in;
    }

    /*
     * Returns a stream of type CompressionOutputStream
     */
    public OutputStream getOutputStream() throws IOException {
        if (out == null) {
            out = new CompressionOutputStream(super.getOutputStream());
        }
        return out;
    }

    /*
     * Flush the CompressionOutputStream before
     * closing the socket.
     */
    public synchronized void close() throws IOException {
        OutputStream o = getOutputStream();
        o.flush();
        super.close();
    }
}
