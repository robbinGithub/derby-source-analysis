/*

   Derby - Class org.apache.derby.client.am.SqlCode

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

// This class is for strong-typing.
//
// Dnc architected codes in the range +/- 4200 to 4299, plus one additional code for -4499.
//
// SQL codes are architected by the product that issues them.
//

public class SqlCode {
    private int code_;

    private SqlCode(int code) {
        code_ = code;
    }

    int getCode() {
        return code_;
    }

    public final static SqlCode invalidCommitOrRollbackUnderXA = new SqlCode(-4200);

    public final static SqlCode invalidSetAutoCommitUnderXA = new SqlCode(-4201);

    public final static SqlCode queuedXAError = new SqlCode(-4203);

    public final static SqlCode disconnectError = new SqlCode(-4499);

    public final static SqlCode undefinedError = new SqlCode(-99999);

}
