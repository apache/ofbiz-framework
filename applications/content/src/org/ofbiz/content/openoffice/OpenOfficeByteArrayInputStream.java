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
package org.ofbiz.content.openoffice;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.sun.star.io.XSeekable;
import com.sun.star.io.XInputStream;
import com.sun.star.io.BufferSizeExceededException;
import com.sun.star.io.NotConnectedException;

/**
 * OpenOfficeByteArrayInputStream Class
 */

public class OpenOfficeByteArrayInputStream extends ByteArrayInputStream implements XInputStream, XSeekable {
	
    public static final String module = OpenOfficeByteArrayInputStream.class.getName();
    
	public OpenOfficeByteArrayInputStream(byte [] bytes) {
		super(bytes);
	}
	
	
	public long getPosition() throws com.sun.star.io.IOException {
		return this.pos;
	}
	
	public long getLength() throws com.sun.star.io.IOException {
		return this.count;
	}
	
	public void seek(long pos1) throws com.sun.star.io.IOException, IllegalArgumentException {
		this.pos = (int)pos1;
	}

	public void skipBytes(int pos1) throws BufferSizeExceededException, 
                                             NotConnectedException, com.sun.star.io.IOException {
        skip(pos1);
	}

	public void closeInput() throws NotConnectedException, com.sun.star.io.IOException {
		
		try {
			close();
		} catch( IOException e) {
			String errMsg = e.getMessage();
			throw new com.sun.star.io.IOException( errMsg, this );
		}
		
    }
	
	public int readBytes(byte [][]buf, int pos2) throws BufferSizeExceededException, NotConnectedException, com.sun.star.io.IOException {
		
		int bytesRead = 0;
		byte [] buf2 = new byte[pos2];
		try {
			bytesRead = super.read(buf2);
		} catch( IOException e) {
			String errMsg = e.getMessage();
			throw new com.sun.star.io.IOException( errMsg, this );
		}
		
		if (bytesRead > 0) {
			if (bytesRead < pos2) {
				byte [] buf3 = new byte[bytesRead];
				System.arraycopy(buf2, 0, buf3, 0, bytesRead);
				buf[0] = buf3;
			} else {
				buf[0] = buf2;
			}
		} else {
			buf[0] = new byte[0];
		}
		return bytesRead;
	}

	public int readSomeBytes(byte [][]buf, int pos2) throws BufferSizeExceededException, NotConnectedException, com.sun.star.io.IOException {
		return readBytes(buf, pos2);
	}
}
