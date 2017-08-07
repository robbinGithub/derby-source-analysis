/*

   Derby - Class org.apache.derbyTesting.functionTests.tests.derbynet.dataSourcePermissions_net

   Copyright 2003, 2004 The Apache Software Foundation or its licensors, as applicable.

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

package org.apache.derbyTesting.functionTests.tests.derbynet;

import org.apache.derby.jdbc.EmbeddedDataSource;
import org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource;
import org.apache.derby.jdbc.EmbeddedXADataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.CallableStatement;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.DriverManager;

import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.sql.PooledConnection;
import javax.sql.XAConnection;
import javax.sql.ConnectionPoolDataSource;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;
import javax.sql.ConnectionEventListener;
import javax.sql.ConnectionEvent;
import org.apache.derby.tools.JDBCDisplayUtil;
import org.apache.derby.tools.ij;
import org.apache.derby.drda.NetworkServerControl;
import org.apache.derbyTesting.functionTests.util.TestUtil;
import java.io.*;
import java.net.InetAddress;
import java.util.Hashtable;
import java.util.Properties;

import javax.naming.*;
import javax.naming.directory.*;

import java.lang.reflect.*;

public class dataSourcePermissions_net extends org.apache.derbyTesting.functionTests.tests.jdbcapi.dataSourcePermissions
{

	private static final int NETWORKSERVER_PORT = 20000;

	private static NetworkServerControl networkServer = null;

	public static void main(String[] args) throws Exception {

		// Load harness properties.
		ij.getPropertyArg(args);

		// "runTest()" is going to try to connect to the database through
		// the server at port NETWORKSERVER_PORT.  Thus, we have to
		// start the server on that port before calling runTest.

		try {
			TestUtil.loadDriver();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Start the NetworkServer on another thread
		networkServer = new NetworkServerControl(InetAddress.getByName("localhost"),NETWORKSERVER_PORT);
		networkServer.start(null);

		// Wait for the NetworkServer to start.
		if (!isServerStarted(networkServer, 60))
			System.exit(-1);

		// Now, go ahead and run the test.
		try {
			dataSourcePermissions_net tester = new dataSourcePermissions_net();
			tester.setProperties();
			tester.runTest();
			if (TestUtil.isDerbyNetClientFramework())
				tester.testClientDataSourceProperties();

		} catch (Exception e) {
		// if we catch an exception of some sort, we need to make sure to
		// close our streams before returning; otherwise, we can get
		// hangs in the harness.  SO, catching all exceptions here keeps
		// us from exiting before closing the necessary streams.
			System.out.println("FAIL - Exiting due to unexpected error: " +
				e.getMessage());
			e.printStackTrace();
		}

		// Shutdown the server.
		networkServer.shutdown();
		// how do we do this with the new api?
		//networkServer.join();
		Thread.sleep(5000);
		System.out.println("Completed dataSourcePermissions_net");

		System.out.close();
		System.err.close();

	}


	public dataSourcePermissions_net() {
	}

	public void setProperties() {

		// Set required server properties.
		System.setProperty("database",
						   TestUtil.getJdbcUrlPrefix("localhost",
													 NETWORKSERVER_PORT) +
						   "wombat;create=true");
		System.setProperty("ij.user", "EDWARD");
		System.setProperty("ij.password", "noodle");

	}

	public String getJDBCUrl(String db, String attrs) {

		String s = TestUtil.getJdbcUrlPrefix("localhost", NETWORKSERVER_PORT)
			+ db;
		if (attrs != null)
			if (TestUtil.isJCCFramework())
				s = s + ":" + attrs + ";";
			else
				s = s + ";" + attrs;
		//System.out.println("getJDBCUrl:" + s);
		return s;

	}

	public javax.sql.DataSource getDS(String database, String user, String
									  password)
	{
		return getDS(database,user,password,null);
	}

	public javax.sql.DataSource getDS(String database, String user, String
									  password, Properties attrs)
	{

	if (attrs == null)
		attrs = new Properties();
	attrs.setProperty("databaseName", database);
	if (user != null)
		attrs.setProperty("user", user);
	if (password != null)
		attrs.setProperty("password", password);
	attrs = addRequiredAttributes(attrs);
	return TestUtil.getDataSource(attrs);
	}



	public javax.sql.ConnectionPoolDataSource getCPDS(String database, String user, String password) {
		Properties attrs = new Properties();
		attrs.setProperty("databaseName", database);
		if (user != null)
			attrs.setProperty("user", user);
		if (password != null)
			attrs.setProperty("password", password);
		attrs = addRequiredAttributes(attrs);
		return TestUtil.getConnectionPoolDataSource(attrs);
	}

	private Properties addRequiredAttributes(Properties attrs)
	{
		if (TestUtil.isJCCFramework())
		{
			attrs.setProperty("driverType","4");
		}

		attrs.setProperty("serverName","localhost");
		attrs.setProperty("portNumber","20000");
		//attrs.setProperty("retrieveMessagesFromServerOnGetMessage","true");
		return attrs;
	}

	public boolean supportsUnicodeNames() {
		return false;
	}

	public boolean supportsPooling() {
		return true;
	}
	public boolean supportsXA() {
		return false;
	}

	public void start() {
	}

	public void shutdown() {

		try {
			DriverManager.getConnection(TestUtil.getJdbcUrlPrefix("localhost",
																  NETWORKSERVER_PORT) +
										"wombat;shutdown=true",
				"EDWARD", "noodle");
			System.out.println("FAIL - Shutdown returned connection");

		} catch (SQLException sqle) {
			System.out.println("EXPECTED SHUTDOWN " + sqle.getMessage());
		}

	}
	protected static boolean isServerStarted(NetworkServerControl server, int ntries)
	{
		for (int i = 1; i <= ntries; i ++)
		{
			try {
				Thread.sleep(500);
				server.ping();
				return true;
			}
			catch (Exception e) {
				if (i == ntries)
					return false;
			}
		}
		return false;
	}

	/**
	 *  Test Client specific dataSource Properties
	 *
	 */
	public void testClientDataSourceProperties() throws SQLException
	{
		testRetrieveMessageText();
		testDescription();
	}

	/**
	 * Test property retrieveMessageText to retrieve message text
	 * Property defaults to true for Network Client but can be set to
	 * false to disable the procedure call.
	 */
	public void testRetrieveMessageText() throws SQLException
	{
		Connection conn;
		String retrieveMessageTextProperty = "retrieveMessageText";
		Class[] argType = { Boolean.TYPE };
		String methodName = TestUtil.getSetterName(retrieveMessageTextProperty);
		Object[] args;

		try {
			DataSource ds = getDS("wombat", "EDWARD", "noodle");
			Method sh = ds.getClass().getMethod(methodName, argType);
			args = new Boolean[] { new Boolean(false) };
			sh.invoke(ds, args);
			conn = ds.getConnection();
			checkMessageText(conn,"false");
			conn.close();

			// now try with retrieveMessageText = true
			ds = getDS("wombat", "EDWARD", "noodle");
			args = new Boolean[] { new Boolean(true) };
			sh.invoke(ds, args);
			conn = ds.getConnection();
			checkMessageText(conn,"true");
			conn.close();
		}
		catch (Exception e)
		{
			System.out.println("FAIL: testRetrieveMessageText() Unexpected Exception " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Test description property
	 */
	public void testDescription() throws SQLException
	{
		String descriptionProperty = "description";
		Class[] argType = { String.class};
		String setterMethodName = TestUtil.getSetterName(descriptionProperty);
		String getterMethodName = TestUtil.getGetterName(descriptionProperty);

		Object[] args;

		try {
			String setDescription = "Everything you ever wanted to know about this datasource";
			DataSource ds = getDS("wombat", "EDWARD", "noodle");
			// Set the description
			Method sh = ds.getClass().getMethod(setterMethodName, argType);
			args = new Object[] { new String(setDescription) };
			sh.invoke(ds, args);
			// Now check it
			sh = ds.getClass().getMethod(getterMethodName, null);
			String getDescription = (String) sh.invoke(ds, null);
			if (!setDescription.equals(getDescription))
				throw new Exception("getDescription() " + getDescription + 
									" does not match setDescription() ");
		}
		catch (Exception e)
		{
			System.out.println("FAIL: testDescription() Unexpected Exception " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void checkMessageText(Connection conn, String
								 retrieveMessageTextValue) throws SQLException
	{
		System.out.println("** checkMessageText() with retrieveMessageText= " +
						   retrieveMessageTextValue);

		try {
			conn.createStatement().executeQuery("SELECT * FROM APP.NOTTHERE");
		}
		catch (SQLException e)
		{
			String expectedSQLState = "42X05";
			String sqlState = e.getSQLState();
			if (sqlState == null || ! sqlState.equals(expectedSQLState))
			{
				System.out.println("Incorrect SQLState.  Got: " + sqlState +
								   " should be: " + expectedSQLState);
				throw e;
			}
			if (retrieveMessageTextValue.equals("true") )
				{
					if (e.getMessage().indexOf("does not exist") != -1)
						System.out.println("PASS: Message Text retrieved properly");
					else
					{
						System.out.println("FAIL: Message text was not retrieved");
						throw e;
					}
				}
			else
				// retrieveMessageTextValue is false
				if (e.getMessage().indexOf("does not exist") == -1)
				{
					System.out.println("PASS: Message text not retrieved");
				}
				else
				{
					System.out.println("FAIL: Message Text should not have been retrieved");
					throw e;
				}

		}
	}

}





