/*

   Derby - Class org.apache.derby.client.am.ConnectionCallbackInterface

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

// Methods implemented by the common Connection class to handle
// certain events that may originate from the material or common layers.
//
// Reply implementations may update connection state via this interface.

public interface ConnectionCallbackInterface {
    public void completeLocalCommit();

    public void completeLocalRollback();

    public void completeAbnormalUnitOfWork();

    public void completeChainBreakingDisconnect();

    public void completeSqlca(Sqlca e);
}
