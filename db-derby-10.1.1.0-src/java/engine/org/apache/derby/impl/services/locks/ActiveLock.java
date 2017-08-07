/*

   Derby - Class org.apache.derby.impl.services.locks.ActiveLock

   Copyright 1998, 2004 The Apache Software Foundation or its licensors, as applicable.

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

package org.apache.derby.impl.services.locks;

import org.apache.derby.iapi.services.locks.Lockable;
import org.apache.derby.iapi.services.locks.C_LockFactory;

import org.apache.derby.iapi.error.StandardException;
import org.apache.derby.iapi.services.context.ContextService;
import org.apache.derby.iapi.services.context.ContextManager;
import org.apache.derby.iapi.services.sanity.SanityManager;


/**
	A Lock represents a granted or waiting lock request.

	<BR>
	MT - Mutable - Immutable identity : Thread Aware
*/

public final class ActiveLock extends Lock {

	/**
		Set to true if the object waiting on this thread should wake up,
		MT - mutable - java synchronized(this)
		
		唤醒标记
	*/
	byte wakeUpNow;

	/**
		Set to true if the Lock potentially could be granted.

		MT - mutable - single thread required
	*/
	boolean potentiallyGranted;

	/**
		If true then this lock can be granted even if
		it is not the first lock request on the wait queue.
		This can occur if the compatability space already holds
		a lock on the object.
	*/
	protected boolean canSkip;

	/**
		Initialize the lock, should be seen as part of the constructor. A future
		version of this class may become mutable - mutable identity.

		MT - single thread required
	*/
	protected ActiveLock(Object space, Lockable ref, Object qualifier) {
		super(space, ref, qualifier);
	}

	/**	
		Set the potentially granted flag, returns true if the
		flag changed its state.

		MT - single thread required
	*/
	protected boolean setPotentiallyGranted() {
		if (!potentiallyGranted) {
			potentiallyGranted = true;
			return true;
		}
		return false;
	}

	/**	
		Clear the potentially granted flag.

		MT - single thread required
	*/
	protected void clearPotentiallyGranted() {
		potentiallyGranted = false;
	}

	/**
		Wait for a lock to be granted, returns when the lock is granted.
		<P>
		The sleep wakeup scheme depends on the two booleans wakeUpNow & potentiallyGranted.
		  
		MT - Single thread required - and assumed to be the thread requesting the lock.
		
		等待锁被授予，当锁被授予时返回
		
		睡眠唤醒方案取决于两布尔wakeupnow和potentiallygranted。
		
		MT -单线程要求-并假设是线程请求锁。
		
		@return true if the wait ended early (ie. someone else woke us up). 如果等待结束早（如果别人叫醒了我们），返回true。

		@exception StandardException timeout, deadlock or thread interrupted “standardexception超时，死锁 或线程中断
	*/
	protected synchronized byte waitForGrant(int timeout)
		throws StandardException
	{
 
		if (wakeUpNow == Constants.WAITING_LOCK_IN_WAIT) {

			try {


				if (timeout == C_LockFactory.WAIT_FOREVER) {
					wait();
				}
				else if (timeout > 0) {
					wait(timeout);
				}

			} catch (InterruptedException ie) {
				throw StandardException.interrupt(ie);
			}
		}

		byte why = wakeUpNow;
		wakeUpNow = Constants.WAITING_LOCK_IN_WAIT;
		return why;
	}

	/**
		Wake up anyone sleeping on this lock.

		MT - Thread Safe
	*/
	protected synchronized void wakeUp(byte why) {
		// If we were picked as a deadlock victim then don't
		// override the wakeup reason with another one.
		if (wakeUpNow != Constants.WAITING_LOCK_DEADLOCK)
			wakeUpNow = why;
		notify();
	}
}