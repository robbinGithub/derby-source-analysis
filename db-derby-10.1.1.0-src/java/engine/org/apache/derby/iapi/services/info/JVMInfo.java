/*

   Derby - Class org.apache.derby.iapi.services.info.JVMInfo

   Copyright 1999, 2004 The Apache Software Foundation or its licensors, as applicable.

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

package org.apache.derby.iapi.services.info;


/**
	What's the current JDK runtime environment.
 */
public abstract class JVMInfo
{
	/**
		The JVM's runtime environment.
		<UL>
		<LI> 1 - not used was JDK 1.1
		<LI> 2 - J2SE_13- JDK 1.2, 1.3
		<LI> 4 - J2SE_14 - JDK 1.4.0 or 1.4.1
		<LI> 5 - J2SE_142 - JDK 1.4.2
		<LI> 6 - J2SE_15 - JDK 1.5
		</UL>
		@return The JVM's runtime environment.
	*/
	public static final int JDK_ID;

	public static final int J2SE_13 = 2;
	public static final int J2SE_14 = 4;
	public static final int J2SE_142 = 5;
	public static final int J2SE_15 = 6; // aka J2SE 5.0

	public static final boolean J2ME;

	/**
    JDBC Boolean type - Types.BIT in JDK1.1 & 1.2 & 1.3, Types.BOOLEAN in JDK1.4
	*/
	public static final int JAVA_SQL_TYPES_BOOLEAN;

	static 
	{
		int id;

		//
		// If the property java.specification.version is set, then try to parse
		// that.  Anything we don't recognize, default to Java 2 platform
		// because java.specification.version is a property that is introduced
		// in Java 2.  We hope that JVM vendors don't implement Java 1 and
		// set a Java 2 system property.
		// 
		// Otherwise, see if we recognize what is set in java.version.
		// If we don't recoginze that, or if the property is not set, assume
		// version 1.3.
		//
		String javaVersion;
		String javaSpec;
		boolean isJ2ME;

		try {
			javaSpec = System.getProperty("java.specification.name");
		} catch (SecurityException se) {
			// some vms do not know about this property so they
			// throw a security exception when access is restricted.
			javaSpec = null;
		}

		try {
			javaVersion = System.getProperty("java.specification.version", "1.3");

		} catch (SecurityException se) {
			// some vms do not know about this property so they
			// throw a security exception when access is restricted.
			javaVersion = "1.3";
		}

		if (javaSpec != null && javaSpec.startsWith("J2ME"))
		{
			// IBM's WCTME 5.7 returns these values for CDC 1.0 profiles.
			// "J2ME Foundation Specification"
			//

			// Foundation 1.0 and Personal Profile 1.0 based
			// upon CDC 1.0 which is JDK 1.3 based
			id = J2SE_13;
			isJ2ME = true;
		}
		else
		{
			// J2SE/J2EE
			isJ2ME = false;

			if (javaVersion.equals("1.2") || javaVersion.equals("1.3"))
			{	
				id = J2SE_13; //jdk1.3 is still Java2 platform with the same API
			}
			else if (javaVersion.equals("1.4"))
			{
				String vmVersion = System.getProperty("java.version", "1.4.0");

				if (JVMInfo.vmCheck(vmVersion, "1.4.0") || JVMInfo.vmCheck(vmVersion, "1.4.1"))
					id = J2SE_14;
				else
					id = J2SE_142;
			}
			else if (javaVersion.equals("1.5"))
			{
				id = J2SE_15;
			}
			else
			{
				// aussme our lowest support unless the java spec
				// is greater than our highest level.
				id = J2SE_13;

				try {

					if (Float.valueOf(javaVersion).floatValue() > 1.4f)
						id = 5;
				} catch (NumberFormatException nfe) {
				}
			}
		}

		JDK_ID = id;
		J2ME = isJ2ME;
		JAVA_SQL_TYPES_BOOLEAN = (isJ2ME || id >= J2SE_14) ?
			org.apache.derby.iapi.reference.JDBC30Translation.SQL_TYPES_BOOLEAN :java.sql.Types.BIT;
	}

	/**
		Check the vmVersion against a speciifc value.
		Sun jvms are of the form
	*/
	private static boolean vmCheck(String vmVersion, String id)
	{
		return vmVersion.equals(id) || vmVersion.startsWith(id + "_");
	}

	/**
		Return Derby's understanding of the virtual machine's environment.
	*/
	public static String derbyVMLevel()
	{
		switch (JDK_ID)
		{
		case J2SE_13: return J2ME ? "J2ME - JDBC for CDC/FP 1.0" : "J2SE 1.3 - JDBC 2.1";
		case J2SE_14: return "J2SE 1.4 - JDBC 3.0";
		case J2SE_142: return "J2SE 1.4.2 - JDBC 3.0";
		case J2SE_15: return "J2SE 5.0 - JDBC 3.0";
		default: return "?-?";
		}
	}
}
