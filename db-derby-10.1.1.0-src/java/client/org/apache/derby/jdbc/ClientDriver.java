/*

   Derby - Class org.apache.derby.jdbc.ClientDriver

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

package org.apache.derby.jdbc;

import org.apache.derby.client.am.Configuration;
import org.apache.derby.client.am.ResourceUtilities;
import org.apache.derby.client.am.SqlException;
import org.apache.derby.client.am.Utils;
import org.apache.derby.client.am.Version;
import org.apache.derby.client.resources.ResourceKeys;


public class ClientDriver implements java.sql.Driver {
    private transient int traceFileSuffixIndex_ = 0;

    private final static int DERBY_REMOTE_PROTOCOL = 1;

    static private SqlException exceptionsOnLoadDriver__ = null;
    // Keep track of the registere driver so that we can deregister it if we're a stored proc.
    static private ClientDriver registeredDriver__ = null;

    static {
        // This may possibly hit the race-condition bug of java 1.1.
        // The Configuration static clause should execute before the following line does.
        if (Configuration.exceptionsOnLoadResources != null) {
            exceptionsOnLoadDriver__ =
                    Utils.accumulateSQLException(Configuration.exceptionsOnLoadResources,
                            exceptionsOnLoadDriver__);
        }
        try {
            registeredDriver__ = new ClientDriver();
            java.sql.DriverManager.registerDriver(registeredDriver__);
        } catch (java.sql.SQLException e) {
            // A null log writer is passed, because jdbc 1 sql exceptions are automatically traced
            exceptionsOnLoadDriver__ =
                    new SqlException(null, "Error occurred while trying to register Dnc driver with JDBC 1 Driver Manager");
            exceptionsOnLoadDriver__.setNextException(e);
        }
    }

    public ClientDriver() {
    }

    public java.sql.Connection connect(String url,
                                       java.util.Properties properties) throws java.sql.SQLException {
        if (exceptionsOnLoadDriver__ != null) {
            throw exceptionsOnLoadDriver__;
        }

        if (properties == null) {
            properties = new java.util.Properties();
        }

        java.util.StringTokenizer urlTokenizer =
                new java.util.StringTokenizer(url, "/:= \t\n\r\f", true);

        int protocol = tokenizeProtocol(url, urlTokenizer);
        if (protocol == 0) {
            return null; // unrecognized database URL prefix.
        }

        String slashOrNull = null;
        if (protocol == DERBY_REMOTE_PROTOCOL) {
            try {
                slashOrNull = urlTokenizer.nextToken(":/");
            } catch (java.util.NoSuchElementException e) {
                // A null log writer is passed, because jdbc 1 sqlexceptions are automatically traced
                throw new SqlException(null, e, "Invalid database url syntax: " + url);
            }
        }
        String server = tokenizeServerName(urlTokenizer, url);    // "/server"
        int port = tokenizeOptionalPortNumber(urlTokenizer, url); // "[:port]/"
        if (port == 0) {
            port = ClientDataSource.propertyDefault_portNumber;
        }

        // longDatabase is the databaseName and attributes.  This will be
        // sent to network server as the databaseName
        String database = tokenizeDatabase(urlTokenizer, url); // "database"
        java.util.Properties augmentedProperties = tokenizeURLProperties(url, properties);


        int traceLevel;
        try {
            traceLevel = ClientDataSource.getTraceLevel(augmentedProperties);
        } catch (java.lang.NumberFormatException e) {
            // A null log writer is passed, because jdbc 1 sqlexceptions are automatically traced
            throw new SqlException(null, e, "trouble reading traceLevel connection property");
        }

        // Jdbc 1 connections will write driver trace info on a
        // driver-wide basis using the jdbc 1 driver manager log writer.
        // This log writer may be narrowed to the connection-level
        // This log writer will be passed to the agent constructor.
        org.apache.derby.client.am.LogWriter dncLogWriter =
                ClientDataSource.computeDncLogWriterForNewConnection(java.sql.DriverManager.getLogWriter(),
                        ClientDataSource.getTraceDirectory(augmentedProperties),
                        ClientDataSource.getTraceFile(augmentedProperties),
                        ClientDataSource.getTraceFileAppend(augmentedProperties),
                        traceLevel,
                        "_driver",
                        traceFileSuffixIndex_++);

        org.apache.derby.client.net.NetConnection conn =
                new org.apache.derby.client.net.NetConnection((org.apache.derby.client.net.NetLogWriter) dncLogWriter,
                        java.sql.DriverManager.getLoginTimeout(),
                        server,
                        port,
                        database,
                        augmentedProperties);
        return conn;
    }

    public boolean acceptsURL(String url) throws java.sql.SQLException {
        java.util.StringTokenizer urlTokenizer = new java.util.StringTokenizer(url, "/:=; \t\n\r\f", true);
        int protocol = tokenizeProtocol(url, urlTokenizer);
        return protocol != 0;
    }

    public java.sql.DriverPropertyInfo[] getPropertyInfo(String url,
                                                         java.util.Properties properties) throws java.sql.SQLException {
        java.sql.DriverPropertyInfo driverPropertyInfo[] = new java.sql.DriverPropertyInfo[2];

        // If there are no properties set already,
        // then create a dummy properties just to make the calls go thru.
        if (properties == null) {
            properties = new java.util.Properties();
        }

        driverPropertyInfo[0] =
                new java.sql.DriverPropertyInfo(ClientDataSource.propertyKey_user,
                        properties.getProperty(ClientDataSource.propertyKey_user, ClientDataSource.propertyDefault_user));

        driverPropertyInfo[1] =
                new java.sql.DriverPropertyInfo(ClientDataSource.propertyKey_password,
                        properties.getProperty(ClientDataSource.propertyKey_password));

        driverPropertyInfo[0].description =
                ResourceUtilities.getResource(ResourceKeys.propertyDescription__user);
        driverPropertyInfo[1].description =
                ResourceUtilities.getResource(ResourceKeys.propertyDescription__password);

        driverPropertyInfo[0].required = true;
        driverPropertyInfo[1].required = false; // depending on the security mechanism

        return driverPropertyInfo;
    }

    public int getMajorVersion() {
        return Version.getMajorVersion();
    }

    public int getMinorVersion() {
        return Version.getMinorVersion();
    }

    public boolean jdbcCompliant() {
        return Configuration.jdbcCompliant;
    }

    // ----------------helper methods---------------------------------------------

    // Tokenize one of the following:
    //  "jdbc:derby:"
    // and return 0 if the protcol is unrecognized
    // return DERBY_PROTOCOL for "jdbc:derby"
    private static int tokenizeProtocol(String url, java.util.StringTokenizer urlTokenizer) throws SqlException {
        // Is this condition necessary, StringTokenizer constructor may do this for us
        if (url == null) {
            return 0;
        }

        if (urlTokenizer == null) {
            return 0;
        }

        try {
            String jdbc = urlTokenizer.nextToken(":");
            if (!jdbc.equals("jdbc")) {
                return 0;
            }
            if (!urlTokenizer.nextToken(":").equals(":")) {
                return 0; // Skip over the first colon in jdbc:derby:
            }
            String dbname = urlTokenizer.nextToken(":");
            int protocol = 0;
            if (dbname.equals("derby") && (url.indexOf("derby://") != -1)) {
                // For Derby AS need to check for // since jdbc:derby: is also the
                // embedded prefix
                protocol = DERBY_REMOTE_PROTOCOL;
            } else {
                return 0;
            }

            if (!urlTokenizer.nextToken(":").equals(":")) {
                return 0; // Skip over the second colon in jdbc:derby:
            }

            return protocol;
        } catch (java.util.NoSuchElementException e) {
            return 0;
        }
    }

    // tokenize "/server" from URL jdbc:derby://server:port/
    // returns server name
    private static String tokenizeServerName(java.util.StringTokenizer urlTokenizer,
                                             String url) throws SqlException {
        try {
            if (!urlTokenizer.nextToken("/").equals("/"))
            // A null log writer is passed, because jdbc 1 sqlexceptions are automatically traced
            {
                throw new SqlException(null, "Invalid database url syntax: " + url);
            }
            return urlTokenizer.nextToken("/:");
        } catch (java.util.NoSuchElementException e) {
            // A null log writer is passed, because jdbc 1 sqlexceptions are automatically traced
            throw new SqlException(null, e, "Invalid database url syntax: " + url);
        }
    }

    // tokenize "[:portNumber]/" from URL jdbc:derby://server[:port]/
    // returns the portNumber or zero if portNumber is not specified.
    private static int tokenizeOptionalPortNumber(java.util.StringTokenizer urlTokenizer,
                                                  String url) throws SqlException {
        try {
            String firstToken = urlTokenizer.nextToken(":/");
            if (firstToken.equals(":")) {
                String port = urlTokenizer.nextToken("/");
                if (!urlTokenizer.nextToken("/").equals("/")) {
                    // A null log writer is passed, because jdbc 1 sqlexceptions are automatically traced
                    throw new SqlException(null, "Invalid database url syntax: " + url);
                }
                return Integer.parseInt(port);
            } else if (firstToken.equals("/")) {
                return 0;
            } else {
                // A null log writer is passed, because jdbc 1 sqlexceptions are automatically traced
                throw new SqlException(null, "Invalid database url syntax: " + url);
            }
        } catch (java.util.NoSuchElementException e) {
            // A null log writer is passed, because jdbc 1 sqlexceptions are automatically traced
            throw new SqlException(null, e, "Invalid database url syntax: " + url);
        }
    }

    //return database name and attributes
    private static String tokenizeDatabase(java.util.StringTokenizer urlTokenizer,
                                           String url) throws SqlException {
        try {
            String databaseName = urlTokenizer.nextToken(" \t\n\r\f");
            return databaseName;
        } catch (java.util.NoSuchElementException e) {
            // A null log writer is passed, because jdbc 1 sqlexceptions are automatically traced
            throw new SqlException(null, e, "Invalid database url syntax: " + url);
        }
    }

    private static java.util.Properties tokenizeURLProperties(String url,
                                                              java.util.Properties properties)
            throws SqlException {
        String attributeString = null;
        int attributeIndex = -1;

        if ((url != null) &&
                ((attributeIndex = url.indexOf(";")) != -1)) {
            attributeString = url.substring(attributeIndex);
        }
        return ClientDataSource.tokenizeAttributes(attributeString, properties);
    }


}



