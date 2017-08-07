/*

   Derby - Class org.apache.derby.client.am.ClobOutputStream

   Copyright (c) 2001, 2005 The Apache Software Foundation or its licensors, where applicable.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

*/

package org.apache.derby.client.am;


public class ClobOutputStream extends java.io.OutputStream {
    private Clob clob_;
    private long offset_;

    public ClobOutputStream(Clob clob, long offset) throws SqlException {
        clob_ = clob;
        offset_ = offset;
        if (offset_ > clob_.sqlLength_) {
            throw new IndexOutOfBoundsException();
        }
    }

    public void write(int b) throws java.io.IOException {
        clob_.string_ = clob_.string_.substring(0, (int) offset_ - 1);
        clob_.string_ = clob_.string_.concat("" + b + "");
        clob_.asciiStream_ = new java.io.StringBufferInputStream(clob_.string_);
        clob_.unicodeStream_ = new java.io.StringBufferInputStream(clob_.string_);
        clob_.characterStream_ = new java.io.StringReader(clob_.string_);
        clob_.sqlLength_ = clob_.string_.length();
    }


    public void write(byte b[], int off, int len) throws java.io.IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if ((off < 0) || (off > b.length) || (len < 0) ||
                ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }

        byte[] newByte = new byte[len];
        System.arraycopy(b, off, newByte, 0, len);
        String str = new String(newByte);
        clob_.string_ = clob_.string_.substring(0, (int) offset_ - 1);
        clob_.string_ = clob_.string_.concat(str);
        clob_.asciiStream_ = new java.io.StringBufferInputStream(clob_.string_);
        clob_.unicodeStream_ = new java.io.StringBufferInputStream(clob_.string_);
        clob_.characterStream_ = new java.io.StringReader(clob_.string_);
        clob_.sqlLength_ = clob_.string_.length();
    }
}

