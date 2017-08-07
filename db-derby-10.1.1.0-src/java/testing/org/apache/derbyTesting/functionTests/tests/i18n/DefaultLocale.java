/*

   Derby - Class org.apache.derbyTesting.functionTests.tests.i18n.DefaultLocale

   Copyright 2000, 2005 The Apache Software Foundation or its licensors, as applicable.

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

package org.apache.derbyTesting.functionTests.tests.i18n;

import java.util.Locale;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;

public class DefaultLocale { 

	static String savedLocale;

	static {
		savedLocale=java.util.Locale.getDefault().toString();
		Locale.setDefault(new Locale("rr", "TT"));
	}


	// used in messageLocale test
	public static void checkDefaultLocale() throws SQLException
	{
		Connection conn = DriverManager.getConnection("jdbc:default:connection");
		String defLocale = java.util.Locale.getDefault().toString();
		//System.out.println(defLocale);
		if (!defLocale.equals("rr_TT"))
			throw new SQLException("wrong_locale");
	}

	// used in urlLocale test
	public static void checkRDefaultLocale() throws SQLException
	{
		Connection conn = DriverManager.getConnection("jdbc:default:connection");
		System.out.println(savedLocale);
		if (!savedLocale.equals("en_US"))
			throw new SQLException("wrong_locale");
	}

	// used in messageLocale test
	public static void checkDatabaseLocale(String Locale) throws SQLException
	{
		Connection conn = DriverManager.getConnection("jdbc:default:connection");
		String dbLocale = org.apache.derby.iapi.db.Factory.getDatabaseOfConnection().getLocale().toString();
		//System.out.println(dbLocale + "-");
		//System.out.println(Locale + "-");
		if (!dbLocale.toUpperCase().equals(Locale.toUpperCase().trim()))
			throw new SQLException("wrong locale");
	}

	// used in messageLocale test
	public static void setDefaultLocale(String Locale, String Code) throws SQLException
	{
		Connection conn = DriverManager.getConnection("jdbc:default:connection");
		java.util.Locale.setDefault(new java.util.Locale(Locale.trim(),Code.trim()));
	}




}
