/*

   Derby - Class org.apache.derby.client.am.DatabaseMetaData

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

import org.apache.derby.jdbc.ClientDataSource;

// Note:
//   Tag members using the strictest visibility.
// Note:
//   Mark methods synchronized if and only if they update object state and are public.
// Not yet done:
//   Application heap data should be copied for shiraz.
//   Save for future pass to avoid clutter during development.
// Not yet done:
//   Apply meaning-preserving program transformations for performance,
//   including the replacement of slow ADTs with faster unsynchronized ADTs.
//   Save for future pass to avoid clutter during development.
// Not yet done:
//   Assign an ErrorKey, ResourceKey, and Resource for each throw statement.
//   Save for future pass to avoid maintenance during development.

public abstract class DatabaseMetaData implements java.sql.DatabaseMetaData {
    //----------------------------- constants  -----------------------------------

    private final static short SQL_BEST_ROWID = 1;
    private final static short SQL_ROWVER = 2;

    private final static short SQL_INDEX_UNIQUE = 0;
    private final static short SQL_INDEX_ALL = 1;

    //---------------------navigational members-----------------------------------

    protected Agent agent_;
    protected Connection connection_;

    //-----------------------------state------------------------------------------

    private final static int numberOfMetaDataInfoMethods__ = 108;
    private Object[] metaDataInfoCache_ = new Object[numberOfMetaDataInfoMethods__];
    private boolean metaDataInfoIsCached_ = false;

    public ProductLevel productLevel_;

    private ResultSet lastGetColumnPrivilegesResultSet_ = null;
    private ResultSet lastGetColumnsResultSet_ = null;
    private ResultSet lastGetForeignKeysResultSet_ = null;
    private ResultSet lastGetPrimaryKeysResultSet_ = null;
    private ResultSet lastGetProcedureColumnsResultSet_ = null;
    private ResultSet lastGetProceduresResultSet_ = null;
    private ResultSet lastGetSpecialColumnsResultSet_ = null;
    private ResultSet lastGetStatisticsResultSet_ = null;
    private ResultSet lastGetTablePrivilegesResultSet_ = null;
    private ResultSet lastGetTablesResultSet_ = null;
    private ResultSet lastGetUDTsResultSet_ = null;
    private ResultSet lastGetTypeInfoResultSet_ = null;
    private ResultSet lastGetAttrResultSet_ = null;
    private ResultSet lastGetSuperTypesResultSet_ = null;
    private ResultSet lastGetSuperTablesResultSet_ = null;

    public boolean useServerXAState_ = true;

    //---------------------constructors/finalizer---------------------------------

    protected DatabaseMetaData(Agent agent, Connection connection, ProductLevel productLevel) {
        agent_ = agent;
        connection_ = connection;
        productLevel_ = productLevel;
        computeFeatureSet_();
        if (connection.isXAConnection()) {
            connection.xaHostVersion_ = productLevel_.versionLevel_;
        }
    }

    // ---------------------------jdbc 1------------------------------------------

    //----------------------------------------------------------------------
    // First, a variety of minor information about the target database.

    private final static int allProceduresAreCallable__ = 0;

    public boolean allProceduresAreCallable() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(allProceduresAreCallable__);
    }

    private final static int allTablesAreSelectable__ = 1;

    public boolean allTablesAreSelectable() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(allTablesAreSelectable__);
    }

    private final static int nullsAreSortedHigh__ = 2;

    public boolean nullsAreSortedHigh() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(nullsAreSortedHigh__);
    }

    private final static int nullsAreSortedLow__ = 3;

    public boolean nullsAreSortedLow() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(nullsAreSortedLow__);
    }

    private final static int nullsAreSortedAtStart__ = 4;

    public boolean nullsAreSortedAtStart() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(nullsAreSortedAtStart__);
    }

    private final static int nullsAreSortedAtEnd__ = 5;

    public boolean nullsAreSortedAtEnd() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(nullsAreSortedAtEnd__);
    }

    private final static int usesLocalFiles__ = 6;

    public boolean usesLocalFiles() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(usesLocalFiles__);
    }

    private final static int usesLocalFilePerTable__ = 7;

    public boolean usesLocalFilePerTable() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(usesLocalFilePerTable__);
    }

    private final static int storesUpperCaseIdentifiers__ = 8;

    public boolean storesUpperCaseIdentifiers() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(storesUpperCaseIdentifiers__);
    }


    private final static int storesLowerCaseIdentifiers__ = 9;

    public boolean storesLowerCaseIdentifiers() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(storesLowerCaseIdentifiers__);
    }

    private final static int storesMixedCaseIdentifiers__ = 10;

    public boolean storesMixedCaseIdentifiers() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(storesMixedCaseIdentifiers__);
    }

    private final static int storesUpperCaseQuotedIdentifiers__ = 11;

    public boolean storesUpperCaseQuotedIdentifiers() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(storesUpperCaseQuotedIdentifiers__);
    }

    private final static int storesLowerCaseQuotedIdentifiers__ = 12;

    public boolean storesLowerCaseQuotedIdentifiers() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(storesLowerCaseQuotedIdentifiers__);
    }

    private final static int storesMixedCaseQuotedIdentifiers__ = 13;

    public boolean storesMixedCaseQuotedIdentifiers() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(storesMixedCaseQuotedIdentifiers__);
    }

    private final static int getSQLKeywords__ = 14;

    public String getSQLKeywords() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoString(getSQLKeywords__);
    }

    private final static int getNumericFunctions__ = 15;

    public String getNumericFunctions() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoString(getNumericFunctions__);
    }

    private final static int getStringFunctions__ = 16;

    public String getStringFunctions() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoString(getStringFunctions__);
    }

    private final static int getSystemFunctions__ = 17;

    public String getSystemFunctions() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoString(getSystemFunctions__);
    }

    private final static int getTimeDateFunctions__ = 18;

    public String getTimeDateFunctions() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoString(getTimeDateFunctions__);
    }

    private final static int getSearchStringEscape__ = 19;

    public String getSearchStringEscape() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoString(getSearchStringEscape__);
    }

    private final static int getExtraNameCharacters__ = 20;

    public String getExtraNameCharacters() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoString(getExtraNameCharacters__);
    }

    private final static int supportsAlterTableWithAddColumn__ = 21;

    public boolean supportsAlterTableWithAddColumn() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsAlterTableWithAddColumn__);
    }

    private final static int supportsAlterTableWithDropColumn__ = 22;

    public boolean supportsAlterTableWithDropColumn() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsAlterTableWithDropColumn__);
    }

    private final static int supportsConvert__ = 23;

    public boolean supportsConvert() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsConvert__);
    }

    private final static int supportsConvertType__ = 24;

    public boolean supportsConvert(int fromType, int toType) throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean_supportsConvert(supportsConvertType__, fromType, toType);
    }

    private final static int supportsDifferentTableCorrelationNames__ = 25;

    public boolean supportsDifferentTableCorrelationNames() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsDifferentTableCorrelationNames__);
    }

    private final static int supportsExpressionsInOrderBy__ = 26;

    public boolean supportsExpressionsInOrderBy() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsExpressionsInOrderBy__);
    }

    private final static int supportsOrderByUnrelated__ = 27;

    public boolean supportsOrderByUnrelated() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsOrderByUnrelated__);
    }

    private final static int supportsGroupBy__ = 28;

    public boolean supportsGroupBy() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsGroupBy__);
    }

    private final static int supportsGroupByUnrelated__ = 29;

    public boolean supportsGroupByUnrelated() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsGroupByUnrelated__);
    }

    private final static int supportsGroupByBeyondSelect__ = 30;

    public boolean supportsGroupByBeyondSelect() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsGroupByBeyondSelect__);
    }

    private final static int supportsMultipleResultSets__ = 31;

    public boolean supportsMultipleResultSets() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsMultipleResultSets__);
    }

    private final static int supportsMultipleTransactions__ = 32;

    public boolean supportsMultipleTransactions() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsMultipleTransactions__);
    }

    private final static int supportsCoreSQLGrammar__ = 33;

    public boolean supportsCoreSQLGrammar() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsCoreSQLGrammar__);
    }

    private final static int supportsExtendedSQLGrammar__ = 34;

    public boolean supportsExtendedSQLGrammar() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsExtendedSQLGrammar__);
    }

    private final static int supportsANSI92IntermediateSQL__ = 35;

    public boolean supportsANSI92IntermediateSQL() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsANSI92IntermediateSQL__);
    }

    private final static int supportsANSI92FullSQL__ = 36;

    public boolean supportsANSI92FullSQL() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsANSI92FullSQL__);
    }

    private final static int supportsIntegrityEnhancementFacility__ = 37;

    public boolean supportsIntegrityEnhancementFacility() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsIntegrityEnhancementFacility__);
    }

    private final static int supportsOuterJoins__ = 38;

    public boolean supportsOuterJoins() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsOuterJoins__);
    }

    private final static int supportsFullOuterJoins__ = 39;

    public boolean supportsFullOuterJoins() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsFullOuterJoins__);
    }

    private final static int supportsLimitedOuterJoins__ = 40;

    public boolean supportsLimitedOuterJoins() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsLimitedOuterJoins__);
    }

    private final static int getSchemaTerm__ = 41;

    public String getSchemaTerm() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoString(getSchemaTerm__);
    }

    private final static int getProcedureTerm__ = 42;

    public String getProcedureTerm() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoString(getProcedureTerm__);
    }

    private final static int getCatalogTerm__ = 43;

    public String getCatalogTerm() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoString(getCatalogTerm__);
    }

    private final static int isCatalogAtStart__ = 44;

    public boolean isCatalogAtStart() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(isCatalogAtStart__);
    }

    private final static int getCatalogSeparator__ = 45;

    public String getCatalogSeparator() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoString(getCatalogSeparator__);
    }

    private final static int supportsSchemasInDataManipulation__ = 46;

    public boolean supportsSchemasInDataManipulation() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsSchemasInDataManipulation__);
    }

    private final static int supportsSchemasInProcedureCalls__ = 47;

    public boolean supportsSchemasInProcedureCalls() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsSchemasInProcedureCalls__);
    }

    private final static int supportsSchemasInTableDefinitions__ = 48;

    public boolean supportsSchemasInTableDefinitions() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsSchemasInTableDefinitions__);
    }


    private final static int supportsSchemasInIndexDefinitions__ = 49;

    public boolean supportsSchemasInIndexDefinitions() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsSchemasInIndexDefinitions__);
    }

    private final static int supportsSchemasInPrivilegeDefinitions__ = 50;

    public boolean supportsSchemasInPrivilegeDefinitions() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsSchemasInPrivilegeDefinitions__);
    }

    private final static int supportsCatalogsInDataManipulation__ = 51;

    public boolean supportsCatalogsInDataManipulation() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsCatalogsInDataManipulation__);
    }

    private final static int supportsCatalogsInProcedureCalls__ = 52;

    public boolean supportsCatalogsInProcedureCalls() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsCatalogsInProcedureCalls__);
    }

    private final static int supportsCatalogsInTableDefinitions__ = 53;

    public boolean supportsCatalogsInTableDefinitions() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsCatalogsInTableDefinitions__);
    }

    private final static int supportsCatalogsInIndexDefinitions__ = 54;

    public boolean supportsCatalogsInIndexDefinitions() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsCatalogsInIndexDefinitions__);
    }

    private final static int supportsCatalogsInPrivilegeDefinitions__ = 55;

    public boolean supportsCatalogsInPrivilegeDefinitions() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsCatalogsInPrivilegeDefinitions__);
    }

    private final static int supportsPositionedDelete__ = 56;

    public boolean supportsPositionedDelete() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsPositionedDelete__);
    }


    private final static int supportsPositionedUpdate__ = 57;

    public boolean supportsPositionedUpdate() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsPositionedUpdate__);
    }

    private final static int supportsSelectForUpdate__ = 58;

    public boolean supportsSelectForUpdate() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsSelectForUpdate__);
    }

    private final static int supportsStoredProcedures__ = 59;

    public boolean supportsStoredProcedures() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsStoredProcedures__);
    }

    private final static int supportsSubqueriesInComparisons__ = 60;

    public boolean supportsSubqueriesInComparisons() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsSubqueriesInComparisons__);
    }

    private final static int supportsUnion__ = 61;

    public boolean supportsUnion() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsUnion__);
    }

    private final static int supportsUnionAll__ = 62;

    public boolean supportsUnionAll() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsUnionAll__);
    }

    private final static int supportsOpenCursorsAcrossCommit__ = 63;

    public boolean supportsOpenCursorsAcrossCommit() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsOpenCursorsAcrossCommit__);
    }

    private final static int supportsOpenCursorsAcrossRollback__ = 64;

    public boolean supportsOpenCursorsAcrossRollback() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsOpenCursorsAcrossRollback__);
    }

    private final static int supportsOpenStatementsAcrossCommit__ = 65;

    public boolean supportsOpenStatementsAcrossCommit() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsOpenStatementsAcrossCommit__);
    }


    private final static int supportsOpenStatementsAcrossRollback__ = 66;

    public boolean supportsOpenStatementsAcrossRollback() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsOpenStatementsAcrossRollback__);
    }

    //----------------------------------------------------------------------
    // The following group of methods exposes various limitations
    // based on the target database with the current driver.
    // Unless otherwise specified, a result of zero means there is no
    // limit, or the limit is not known.
    private final static int getMaxBinaryLiteralLength__ = 67;

    public int getMaxBinaryLiteralLength() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoInt(getMaxBinaryLiteralLength__);
    }

    private final static int getMaxCharLiteralLength__ = 68;

    public int getMaxCharLiteralLength() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoInt(getMaxCharLiteralLength__);
    }

    private final static int getMaxColumnNameLength__ = 69;

    public int getMaxColumnNameLength() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoInt(getMaxColumnNameLength__);
    }

    private final static int getMaxColumnsInGroupBy__ = 70;

    public int getMaxColumnsInGroupBy() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoInt(getMaxColumnsInGroupBy__);
    }

    private final static int getMaxColumnsInIndex__ = 71;

    public int getMaxColumnsInIndex() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoInt(getMaxColumnsInIndex__);
    }

    private final static int getMaxColumnsInOrderBy__ = 72;

    public int getMaxColumnsInOrderBy() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoInt(getMaxColumnsInOrderBy__);
    }

    private final static int getMaxColumnsInSelect__ = 73;

    public int getMaxColumnsInSelect() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoInt(getMaxColumnsInSelect__);
    }

    private final static int getMaxColumnsInTable__ = 74;

    public int getMaxColumnsInTable() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoInt(getMaxColumnsInTable__);
    }

    private final static int getMaxConnections__ = 75;

    public int getMaxConnections() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoInt(getMaxConnections__);
    }

    private final static int getMaxCursorNameLength__ = 76;

    public int getMaxCursorNameLength() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoInt(getMaxCursorNameLength__);
    }

    private final static int getMaxIndexLength__ = 77;

    public int getMaxIndexLength() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoInt(getMaxIndexLength__);
    }

    private final static int getMaxSchemaNameLength__ = 78;

    public int getMaxSchemaNameLength() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoInt(getMaxSchemaNameLength__);
    }

    private final static int getMaxProcedureNameLength__ = 79;

    public int getMaxProcedureNameLength() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoInt(getMaxProcedureNameLength__);
    }

    private final static int getMaxCatalogNameLength__ = 80;

    public int getMaxCatalogNameLength() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoInt(getMaxCatalogNameLength__);
    }

    private final static int getMaxRowSize__ = 81;

    public int getMaxRowSize() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoInt(getMaxRowSize__);
    }

    private final static int doesMaxRowSizeIncludeBlobs__ = 82;

    public boolean doesMaxRowSizeIncludeBlobs() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(doesMaxRowSizeIncludeBlobs__);
    }

    private final static int getMaxStatementLength__ = 83;

    public int getMaxStatementLength() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoInt(getMaxStatementLength__);
    }

    private final static int getMaxStatements__ = 84;

    public int getMaxStatements() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoInt(getMaxStatements__);
    }

    private final static int getMaxTableNameLength__ = 85;

    public int getMaxTableNameLength() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoInt(getMaxTableNameLength__);
    }

    private final static int getMaxTablesInSelect__ = 86;

    public int getMaxTablesInSelect() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoInt(getMaxTablesInSelect__);
    }

    private final static int getMaxUserNameLength__ = 87;

    public int getMaxUserNameLength() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoInt(getMaxUserNameLength__);
    }

    private final static int getDefaultTransactionIsolation__ = 88;

    public int getDefaultTransactionIsolation() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoInt(getDefaultTransactionIsolation__);
    }

    private final static int supportsTransactions__ = 89;

    public boolean supportsTransactions() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsTransactions__);
    }

    // Stored Procedure will return a String containing a
    // comma seperated list of all supported levels
    private final static int supportsTransactionIsolationLevel__ = 90;

    public boolean supportsTransactionIsolationLevel(int level) throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBooleanWithType(supportsTransactionIsolationLevel__, level);
    }


    private final static int supportsDataDefinitionAndDataManipulationTransactions__ = 91;

    public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsDataDefinitionAndDataManipulationTransactions__);
    }

    private final static int supportsDataManipulationTransactionsOnly__ = 92;

    public boolean supportsDataManipulationTransactionsOnly() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsDataManipulationTransactionsOnly__);
    }

    private final static int dataDefinitionCausesTransactionCommit__ = 93;

    public boolean dataDefinitionCausesTransactionCommit() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(dataDefinitionCausesTransactionCommit__);
    }

    private final static int dataDefinitionIgnoredInTransactions__ = 94;

    public boolean dataDefinitionIgnoredInTransactions() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(dataDefinitionIgnoredInTransactions__);
    }

    // Stored Procedure will return a String containing a
    // comma seperated list of all the supported resultSet types
    private final static int supportsResultSetType__ = 95;

    public boolean supportsResultSetType(int type) throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBooleanWithType(supportsResultSetType__, type);
    }

    private final static int supportsResultSetConcurrency__ = 96;

    public boolean supportsResultSetConcurrency(int type, int concurrency) throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoInt_SupportsResultSetConcurrency(supportsResultSetConcurrency__, type, concurrency);
    }

    // Stored Procedure will return a String containing a
    // comma seperated list of all the supported result Set types
    private final static int ownUpdatesAreVisible__ = 97;

    public boolean ownUpdatesAreVisible(int type) throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBooleanWithType(ownUpdatesAreVisible__, type);
    }

    // Stored Procedure will return a String containing a
    // comma seperated list of all the supported result Set types
    private final static int ownDeletesAreVisible__ = 98;

    public boolean ownDeletesAreVisible(int type) throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBooleanWithType(ownDeletesAreVisible__, type);
    }

    // Stored Procedure will return a String containing a
    // comma seperated list all the supported result Set types
    private final static int ownInsertsAreVisible__ = 99;

    public boolean ownInsertsAreVisible(int type) throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBooleanWithType(ownInsertsAreVisible__, type);
    }

    // Stored Procedure will return a String containing a
    // comma seperated list of all the supported result Set types
    private final static int othersUpdatesAreVisible__ = 100;

    public boolean othersUpdatesAreVisible(int type) throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBooleanWithType(othersUpdatesAreVisible__, type);
    }

    // Stored Procedure will return a String containing a
    // comma seperated list of all the supported result Set types
    private final static int othersDeletesAreVisible__ = 101;

    public boolean othersDeletesAreVisible(int type) throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBooleanWithType(othersDeletesAreVisible__, type);
    }

    // Stored Procedure will return a String containing a
    // comma seperated list of all the supported result Set types
    private final static int othersInsertsAreVisible__ = 102;

    public boolean othersInsertsAreVisible(int type) throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBooleanWithType(othersInsertsAreVisible__, type);
    }

    // Stored Procedure will return a String containing a
    // comma seperated list of all the supported result Set types
    private final static int updatesAreDetected__ = 103;

    public boolean updatesAreDetected(int type) throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBooleanWithType(updatesAreDetected__, type);
    }

    // Stored Procedure will return a String containing a
    // comma seperated list of all the supported result Set types
    private final static int deletesAreDetected__ = 104;

    public boolean deletesAreDetected(int type) throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBooleanWithType(deletesAreDetected__, type);
    }

    // Stored Procedure will return a String containing a
    // comma seperated list of all the supported result Set types
    private final static int insertsAreDetected__ = 105;

    public boolean insertsAreDetected(int type) throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBooleanWithType(insertsAreDetected__, type);
    }

    private final static int supportsBatchUpdates__ = 106;

    public boolean supportsBatchUpdates() throws SqlException {
        checkForClosedConnection();
        return getMetaDataInfoBoolean(supportsBatchUpdates__);
    }

    public boolean supportsSavepoints() throws SqlException {
        checkForClosedConnection();
        if (productLevel_.greaterThanOrEqualTo(5, 2, 0)) {
            return true;
        }

        return false;
    }

    // start tagging all abstract methods with an underscore like this !!
    abstract public String getURL_() throws SqlException;

    public String getURL() throws SqlException {
        checkForClosedConnection();
        return getURL_();
    }

    public String getUserName() throws SqlException {
        checkForClosedConnection();
        return connection_.user_;
    }

    public boolean isReadOnly() throws SqlException {
        return false;
    }

    public String getDatabaseProductName() throws SqlException {
        checkForClosedConnection();
        return productLevel_.databaseProductName_;
    }

    public String getDatabaseProductVersion() throws SqlException {
        checkForClosedConnection();
        return productLevel_.databaseProductVersion_;

    }

    public String getDriverName() throws SqlException {
        checkForClosedConnection();
        return Configuration.dncDriverName;
    }

    public String getDriverVersion() throws SqlException {
        checkForClosedConnection();
        return Version.getDriverVersion();
    }

    // JDBC signature also does not throw SqlException, so we don't check for closed connection.
    public int getDriverMajorVersion() {
        return Version.getMajorVersion();
    }

    // JDBC signature also does not throw SqlException, so we don't check for closed connection.
    public int getDriverMinorVersion() {
        return Version.getMinorVersion();
    }

    //All JDBC Drivers must return false for this method. For this reason we choose
    //to return FALSE
    public boolean supportsMixedCaseIdentifiers() throws SqlException {
        checkForClosedConnection();
        return false;
    }

    public boolean supportsMixedCaseQuotedIdentifiers() throws SqlException {
        checkForClosedConnection();
        return true;
    }

    public String getIdentifierQuoteString() throws SqlException {
        checkForClosedConnection();
        return "\"";
    }

    public boolean supportsColumnAliasing() throws SqlException {
        checkForClosedConnection();
        return true;
    }

    public boolean nullPlusNonNullIsNull() throws SqlException {
        checkForClosedConnection();
        return true;
    }

    public boolean supportsTableCorrelationNames() throws SqlException {
        checkForClosedConnection();
        return true;
    }

    public boolean supportsLikeEscapeClause() throws SqlException {
        checkForClosedConnection();
        return true;
    }

    public boolean supportsNonNullableColumns() throws SqlException {
        checkForClosedConnection();
        return true;
    }

    public boolean supportsMinimumSQLGrammar() throws SqlException {
        checkForClosedConnection();
        return true;
    }

    public boolean supportsANSI92EntryLevelSQL() throws SqlException {
        checkForClosedConnection();
        return true;
    }

    public boolean supportsSubqueriesInExists() throws SqlException {
        checkForClosedConnection();
        return true;
    }

    public boolean supportsSubqueriesInIns() throws SqlException {
        checkForClosedConnection();
        return true;
    }

    public boolean supportsSubqueriesInQuantifieds() throws SqlException {
        checkForClosedConnection();
        return true;
    }

    public boolean supportsCorrelatedSubqueries() throws SqlException {
        checkForClosedConnection();
        return true;
    }


    //------------------------catalog query methods follow--------------------------------------------

    // call stored procedure SQLProcedures
    // SYSIBM.SQLProcedures(
    //              CatalogName varchar(128),
    //              SchemaName  varchar(128),
    //              ProcName    varchar(128),
    //              Options     varchar(4000))
    //
    public java.sql.ResultSet getProcedures(String catalog,
                                            String schemaPattern,
                                            String procedureNamePattern) throws SqlException {
        synchronized (connection_) {
            if (agent_.loggingEnabled()) {
                agent_.logWriter_.traceEntry(this, "getProcedures", catalog, schemaPattern, procedureNamePattern);
            }
            return getProceduresX(catalog, schemaPattern, procedureNamePattern);
        }
    }


    private ResultSet getProceduresX(String catalog,
                                     String schemaPattern,
                                     String procedureNamePattern) throws SqlException {
        checkForClosedConnection();
        ;

        PreparedStatement cs = prepareMetaDataQuery("SYSIBM.SQLPROCEDURES(?,?,?,?)");

        cs.setStringX(1, catalog);
        cs.setStringX(2, schemaPattern);
        cs.setStringX(3, procedureNamePattern);
        cs.setStringX(4, getOptions());
        lastGetProceduresResultSet_ = executeCatalogQuery(cs);
        return lastGetProceduresResultSet_;
    }


    // call stored procedure SQLProcedureCols
    // SYSIBM.SQLProcedureCols(
    //              CatalogName varchar(128),
    //              SchemaName  varchar(128),
    //              ProcName    varchar(128),
    //              ParamName   varchar(128),
    //              Options     varchar(4000))
    //
    public java.sql.ResultSet getProcedureColumns(String catalog,
                                                  String schemaPattern,
                                                  String procedureNamePattern,
                                                  String columnNamePattern) throws SqlException {
        synchronized (connection_) {
            if (agent_.loggingEnabled()) {
                agent_.logWriter_.traceEntry(this, "getProcedureColumns", catalog, schemaPattern, procedureNamePattern, columnNamePattern);
            }
            return getProcedureColumnsX(catalog, schemaPattern, procedureNamePattern, columnNamePattern);
        }
    }

    private ResultSet getProcedureColumnsX(String catalog,
                                           String schemaPattern,
                                           String procedureNamePattern,
                                           String columnNamePattern) throws SqlException {
        checkForClosedConnection();
        ;

        PreparedStatement cs = prepareMetaDataQuery("SYSIBM.SQLPROCEDURECOLS(?,?,?,?,?)");

        cs.setStringX(1, catalog);
        cs.setStringX(2, schemaPattern);
        cs.setStringX(3, procedureNamePattern);
        cs.setStringX(4, columnNamePattern);
        cs.setStringX(5, getOptions());
        lastGetProcedureColumnsResultSet_ = executeCatalogQuery(cs);
        return lastGetProcedureColumnsResultSet_;
    }


    // call stored procedure SQLTables
    // SYSIBM.SQLTables(
    //              CatalogName varchar(128),
    //              SchemaName  varchar(128),
    //              TableName   varchar(128),
    //              TaleType    varchar(4000),
    //              Options     varchar(4000))
    //
    public java.sql.ResultSet getTables(String catalog,
                                        String schemaPattern,
                                        String tableNamePattern,
                                        String types[]) throws SqlException {
        synchronized (connection_) {
            if (agent_.loggingEnabled()) {
                agent_.logWriter_.traceEntry(this, "getTables", catalog, schemaPattern, tableNamePattern, types);
            }
            return getTablesX(catalog, schemaPattern, tableNamePattern, types);
        }
    }

    private ResultSet getTablesX(String catalog,
                                 String schemaPattern,
                                 String tableNamePattern,
                                 String types[]) throws SqlException {
        checkForClosedConnection();

        PreparedStatement cs = prepareMetaDataQuery("SYSIBM.SQLTABLES(?,?,?,?,?)");

        if (catalog == null) {
            cs.setNullX(1, java.sql.Types.VARCHAR);
        } else {
            cs.setStringX(1, catalog);
        }

        if (schemaPattern == null) {
            cs.setNullX(2, java.sql.Types.VARCHAR);
        } else {
            cs.setStringX(2, schemaPattern);
        }

        if (tableNamePattern == null) {
            cs.setNullX(3, java.sql.Types.VARCHAR);
        } else {
            cs.setStringX(3, tableNamePattern);
        }

        String tableTypes = new String();
        int i = 0;
        if (types == null) {
            cs.setNullX(4, java.sql.Types.VARCHAR);
        } else if (types.length == 1 && (types[0].trim()).equals("%")) {
            cs.setStringX(4, types[0]);
        } else {
            while (i < types.length) {
                if (i > 0) {
                    tableTypes = tableTypes.concat(",");
                }
                tableTypes = tableTypes.concat("'" + types[i] + "'");
                i++;
            }
            cs.setStringX(4, tableTypes);
        }
        cs.setStringX(5, getOptions());
        lastGetTablesResultSet_ = executeCatalogQuery(cs);
        return lastGetTablesResultSet_;
    }

    // call stored procedure SQLTables
    // SYSIBM.SQLTables(
    //              CatalogName varchar(128),
    //              SchemaName  varchar(128),
    //              TableName   varchar(128),
    //              TaleType    varchar(4000),
    //              Options     varchar(4000))
    //
    public java.sql.ResultSet getSchemas() throws SqlException {
        synchronized (connection_) {
            if (agent_.loggingEnabled()) {
                agent_.logWriter_.traceEntry(this, "getSchemas");
            }
            return getSchemasX();
        }
    }

    private ResultSet getSchemasX() throws SqlException {
        checkForClosedConnection();
        ;

        PreparedStatement cs = prepareMetaDataQuery("SYSIBM.SQLTABLES('', '', '', '', 'GETSCHEMAS=1')");
        return (ResultSet) cs.executeQueryX();
    }


    // DERBY does not have the notion of a catalog, so we return a result set with no rows.
    public java.sql.ResultSet getCatalogs() throws SqlException {
        synchronized (connection_) {
            if (agent_.loggingEnabled()) {
                agent_.logWriter_.traceEntry(this, "getCatalogs");
            }
            return getCatalogsX();
        }
    }

    private ResultSet getCatalogsX() throws SqlException {
        checkForClosedConnection();

        PreparedStatement cs = prepareMetaDataQuery("SYSIBM.SQLTABLES('', '', '', '', 'GETCATALOGS=1')");
        return (ResultSet) cs.executeQueryX();
    }

    // call stored procedure SQLTables
    // SYSIBM.SQLTables(
    //              CatalogName varchar(128),
    //              SchemaName  varchar(128),
    //              TableName   varchar(128),
    //              TableType   varchar(4000),
    //              Options     varchar(4000))
    public java.sql.ResultSet getTableTypes() throws SqlException {
        synchronized (connection_) {
            if (agent_.loggingEnabled()) {
                agent_.logWriter_.traceEntry(this, "getTableTypes");
            }
            return getTableTypesX();
        }
    }

    private ResultSet getTableTypesX() throws SqlException {
        checkForClosedConnection();
        ;

        PreparedStatement cs = null;
        cs = prepareMetaDataQuery("SYSIBM.SQLTABLES(?,?,?,?,?)");

        cs.setStringX(1, "");
        cs.setStringX(2, "");
        cs.setStringX(3, "");
        cs.setStringX(4, "%");
        int cursorHold;
        if (connection_.resultSetHoldability_ == ClientDataSource.HOLD_CURSORS_OVER_COMMIT) {
            cursorHold = 1;
        } else {
            cursorHold = 0;
        }
        cs.setStringX(5, "DATATYPE='JDBC';GETTABLETYPES=1; CURSORHOLD=" + cursorHold);
        lastGetTablesResultSet_ = executeCatalogQuery(cs);
        return lastGetTablesResultSet_;
    }


    // call stored procedure SQLColumns
    // SYSIBM.SQLColumns(
    //              CatalogName varchar(128),
    //              SchemaName  varchar(128),
    //              TableName   varchar(128),
    //              ColumnName  varchar(128),
    //              Options     varchar(4000))
    //
    public java.sql.ResultSet getColumns(String catalog,
                                         String schemaPattern,
                                         String tableNamePattern,
                                         String columnNamePattern) throws SqlException {
        synchronized (connection_) {
            if (agent_.loggingEnabled()) {
                agent_.logWriter_.traceEntry(this, "getColumns", catalog, schemaPattern, tableNamePattern, columnNamePattern);
            }
            checkForClosedConnection();
            return getColumnsX(catalog, schemaPattern, tableNamePattern, columnNamePattern);
        }
    }

    private ResultSet getColumnsX(String catalog,
                                  String schemaPattern,
                                  String tableNamePattern,
                                  String columnNamePattern) throws SqlException {
        checkForClosedConnection();

        PreparedStatement cs = prepareMetaDataQuery("SYSIBM.SQLCOLUMNS(?,?,?,?,?)");

        cs.setStringX(1, catalog);
        cs.setStringX(2, schemaPattern);
        cs.setStringX(3, tableNamePattern);
        cs.setStringX(4, columnNamePattern); //Always null  for JDBC
        cs.setStringX(5, getOptions());
        lastGetColumnsResultSet_ = executeCatalogQuery(cs);
        return lastGetColumnsResultSet_;
    }


    // call stored procedure SQLColumnPrivileges
    // SYSIBM.SQLColPrivileges(
    //              CatalogName varchar(128),
    //              SchemaName  varchar(128),
    //              TableName   varchar(128),
    //              ColumnName  varchar(128),
    //              Options     varchar(4000))
    //
    public java.sql.ResultSet getColumnPrivileges(String catalog,
                                                  String schema,
                                                  String table,
                                                  String columnNamePattern) throws SqlException {
        synchronized (connection_) {
            if (agent_.loggingEnabled()) {
                agent_.logWriter_.traceEntry(this, "getColumnPrivileges", catalog, schema, table, columnNamePattern);
            }
            return getColumnPrivilegesX(catalog, schema, table, columnNamePattern);
        }
    }

    private ResultSet getColumnPrivilegesX(String catalog,
                                           String schema,
                                           String table,
                                           String columnNamePattern) throws SqlException {
        checkForClosedConnection();
        // check input params, table and columnNamePattern cannot be null
        if (table == null) {
            throw new SqlException(agent_.logWriter_, "getColumnPrivileges(): null not allowed for table name");
        }

        PreparedStatement cs = prepareMetaDataQuery("SYSIBM.SQLCOLPRIVILEGES(?,?,?,?,?)");

        cs.setStringX(1, catalog);
        cs.setStringX(2, schema);
        cs.setStringX(3, table);
        cs.setStringX(4, columnNamePattern);
        cs.setStringX(5, getOptions());
        lastGetColumnPrivilegesResultSet_ = executeCatalogQuery(cs);
        return lastGetColumnPrivilegesResultSet_;
    }


    // call stored procedure SQLTablePrivileges
    // SYSIBM.SQLTablePrivileges(
    //              CatalogName varchar(128),
    //              SchemaName  varchar(128),
    //              TableName   varchar(128),
    //              Options     varchar(4000))
    //
    public java.sql.ResultSet getTablePrivileges(String catalog,
                                                 String schemaPattern,
                                                 String tableNamePattern) throws SqlException {
        synchronized (connection_) {
            if (agent_.loggingEnabled()) {
                agent_.logWriter_.traceEntry(this, "getTablePrivileges", catalog, schemaPattern, tableNamePattern);
            }
            return getTablePrivilegesX(catalog, schemaPattern, tableNamePattern);
        }
    }

    private ResultSet getTablePrivilegesX(String catalog,
                                          String schemaPattern,
                                          String tableNamePattern) throws SqlException {
        checkForClosedConnection();
        ;

        PreparedStatement cs = prepareMetaDataQuery("SYSIBM.SQLTABLEPRIVILEGES(?,?,?,?)");

        cs.setStringX(1, catalog);
        cs.setStringX(2, schemaPattern);
        cs.setStringX(3, tableNamePattern);
        cs.setStringX(4, getOptions());
        lastGetTablePrivilegesResultSet_ = executeCatalogQuery(cs);
        return lastGetTablePrivilegesResultSet_;
    }


    // call stored procedure
    // SYSIBM.SQLSPECIALCOLUMNS ( IN COLTYPE SMALLINT,
    //                            IN CATALOG_NAME VARCHAR(128),
    //                            IN SCHEMA_NAME  VARCHAR(128),
    //                            IN TABLE_NAME   VARCHAR(128),
    //                            IN SCOPE        SMALLINT,
    //                            IN NULLABLE     SMALLINT,
    //                            IN OPTIONS      VARCHAR(4000) )
    //
    public java.sql.ResultSet getBestRowIdentifier(String catalog,
                                                   String schema,
                                                   String table,
                                                   int scope,
                                                   boolean nullable) throws SqlException {
        synchronized (connection_) {
            if (agent_.loggingEnabled()) {
                agent_.logWriter_.traceEntry(this, "getBestRowIdentifier", catalog, schema, table, scope, nullable);
            }
            return getBestRowIdentifierX(catalog, schema, table, scope, nullable);
        }
    }

    private ResultSet getBestRowIdentifierX(String catalog,
                                            String schema,
                                            String table,
                                            int scope,
                                            boolean nullable) throws SqlException {
        checkForClosedConnection();
        ;

        // check input params
        //
        // validate input table, which can not be null
        if (table == null) {
            throw new SqlException(agent_.logWriter_, "getBestRowIdentifier(): null not allowed for table name");
        }
        PreparedStatement cs = prepareMetaDataQuery("SYSIBM.SQLSPECIALCOLUMNS(?,?,?,?,?,?,?)");

        cs.setIntX(1, SQL_BEST_ROWID);
        cs.setStringX(2, catalog);
        cs.setStringX(3, schema);
        cs.setStringX(4, table);
        cs.setIntX(5, scope);
        if (nullable) {
            cs.setShortX(6, (short) 1);
        } else {
            cs.setShortX(6, (short) 0);
        }
        cs.setStringX(7, getOptions());
        lastGetSpecialColumnsResultSet_ = executeCatalogQuery(cs);
        return lastGetSpecialColumnsResultSet_;
    }


    public java.sql.ResultSet getVersionColumns(String catalog,
                                                String schema,
                                                String table) throws SqlException {
        synchronized (connection_) {
            if (agent_.loggingEnabled()) {
                agent_.logWriter_.traceEntry(this, "getVersionColumns", catalog, schema, table);
            }
            return getVersionColumnsX(catalog, schema, table);
        }
    }

    private ResultSet getVersionColumnsX(String catalog,
                                         String schema,
                                         String table) throws SqlException {
        checkForClosedConnection();

        // validate input table, which can not be null
        if (table == null) {
            throw new SqlException(agent_.logWriter_, "getBestRowIdentifier(): null not allowed for table name");
        }
        PreparedStatement cs = prepareMetaDataQuery("SYSIBM.SQLSPECIALCOLUMNS(?,?,?,?,?,?,?)");

        cs.setIntX(1, SQL_ROWVER);
        cs.setStringX(2, catalog);
        cs.setStringX(3, schema);
        cs.setStringX(4, table);
        cs.setIntX(5, 0);
        cs.setShortX(6, (short) 0);
        cs.setStringX(7, getOptions());

        lastGetSpecialColumnsResultSet_ = executeCatalogQuery(cs);
        return lastGetSpecialColumnsResultSet_;
    }

    // call stored procedure SQLPrimaryKeys
    // SYSIBM.SQLPrimaryKeys(
    //              CatalogName varchar(128),
    //              SchemaName  varchar(128),
    //              TableName   varchar(128),
    //              Options     varchar(4000))
    //
    public java.sql.ResultSet getPrimaryKeys(String catalog,
                                             String schema,
                                             String table) throws SqlException {
        synchronized (connection_) {
            if (agent_.loggingEnabled()) {
                agent_.logWriter_.traceEntry(this, "getPrimaryKeys", catalog, schema, table);
            }
            return getPrimaryKeysX(catalog, schema, table);
        }
    }

    private ResultSet getPrimaryKeysX(String catalog,
                                      String schema,
                                      String table) throws SqlException {
        checkForClosedConnection();
        ;

        // validate the input table name
        if (table == null) {
            throw new SqlException(agent_.logWriter_, "getIndexInfo(): null not allowed for table name");
        }
        PreparedStatement cs = prepareMetaDataQuery("SYSIBM.SQLPRIMARYKEYS(?,?,?,?)");

        cs.setStringX(1, catalog);
        cs.setStringX(2, schema);
        cs.setStringX(3, table);
        cs.setStringX(4, getOptions());
        lastGetPrimaryKeysResultSet_ = executeCatalogQuery(cs);
        return lastGetPrimaryKeysResultSet_;
    }


    // call storlastGetPrimaryKeysResultSet_ed procedure SQLForeignKeys
    // SYSIBM.SQLForeignKeys(
    //              PKCatalogName varchar(128),
    //              PKSchemaName  varchar(128),
    //              PKTableName   varchar(128),
    //              FKCatalogName varchar(128),
    //              FKSchemaName  varchar(128),
    //              FKTableName   varchar(128),
    //              Options       varchar(4000))
    //
    public java.sql.ResultSet getImportedKeys(String catalog,
                                              String schema,
                                              String table) throws SqlException {
        synchronized (connection_) {
            if (agent_.loggingEnabled()) {
                agent_.logWriter_.traceEntry(this, "getImportedKeys", catalog, schema, table);
            }
            return getImportedKeysX(catalog, schema, table);
        }
    }

    private ResultSet getImportedKeysX(String catalog,
                                       String schema,
                                       String table) throws SqlException {
        checkForClosedConnection();

        PreparedStatement cs = prepareMetaDataQuery("SYSIBM.SQLFOREIGNKEYS(?,?,?,?,?,?,?)");

        cs.setStringX(1, "");
        cs.setStringX(2, null);
        cs.setStringX(3, "");
        cs.setStringX(4, catalog);
        cs.setStringX(5, schema);
        cs.setStringX(6, table);
        // We're passing the keyword EXPORTEDKEY, but this support may not be in the GA version of SPs.
        // As a workaround in getCrossReference(), we'll just "select * where 0=1" when primaryTable==""
        if (connection_.resultSetHoldability_ == ClientDataSource.HOLD_CURSORS_OVER_COMMIT) {
            cs.setStringX(7, "DATATYPE='JDBC';IMPORTEDKEY=1; CURSORHOLD=1");
        } else {
            cs.setStringX(7, "DATATYPE='JDBC';IMPORTEDKEY=1; CURSORHOLD=0");
        }
        lastGetForeignKeysResultSet_ = executeCatalogQuery(cs);
        return lastGetForeignKeysResultSet_;
    }

    // call stored procedure SQLForeignKeys
    // SYSIBM.SQLForeignKeys(
    //              PKCatalogName varchar(128),
    //              PKSchemaName  varchar(128),
    //              PKTableName   varchar(128),
    //              FKCatalogName varchar(128),
    //              FKSchemaName  varchar(128),
    //              FKTableName   varchar(128),
    //              Options       varchar(4000))
    //
    public java.sql.ResultSet getExportedKeys(String catalog,
                                              String schema,
                                              String table) throws SqlException {
        synchronized (connection_) {
            if (agent_.loggingEnabled()) {
                agent_.logWriter_.traceEntry(this, "getExportedKeys", catalog, schema, table);
            }
            return getExportedKeysX(catalog, schema, table);
        }
    }

    private ResultSet getExportedKeysX(String catalog,
                                       String schema,
                                       String table) throws SqlException {
        checkForClosedConnection();
        ;

        PreparedStatement cs = prepareMetaDataQuery("SYSIBM.SQLFOREIGNKEYS(?,?,?,?,?,?,?)");

        cs.setStringX(1, catalog);
        cs.setStringX(2, schema);
        cs.setStringX(3, table);
        cs.setStringX(4, "");
        cs.setStringX(5, null);
        cs.setStringX(6, "");
        // We're passing the keyword EXPORTEDKEY, but this support may not be in the GA version of SPs.
        // As a workaround in getCrossReference(), we'll just "select * where 0=1" when foreignTable==""
        if (connection_.resultSetHoldability_ == ClientDataSource.HOLD_CURSORS_OVER_COMMIT) {
            cs.setStringX(7, "DATATYPE='JDBC';EXPORTEDKEY=1; CURSORHOLD=1");
        } else {
            cs.setStringX(7, "DATATYPE='JDBC';EXPORTEDKEY=1; CURSORHOLD=0");
        }
        lastGetForeignKeysResultSet_ = executeCatalogQuery(cs);
        return lastGetForeignKeysResultSet_;
    }

    // call stored procedure SQLForeignKeys
    // SYSIBM.SQLForeignKeys(
    //              PKCatalogName varchar(128),
    //              PKSchemaName  varchar(128),
    //              PKTableName   varchar(128),
    //              FKCatalogName varchar(128),
    //              FKSchemaName  varchar(128),
    //              FKTableName   varchar(128),
    //              Options       varchar(4000))
    //
    public java.sql.ResultSet getCrossReference(String primaryCatalog,
                                                String primarySchema,
                                                String primaryTable,
                                                String foreignCatalog,
                                                String foreignSchema,
                                                String foreignTable) throws SqlException {
        synchronized (connection_) {
            if (agent_.loggingEnabled()) {
                agent_.logWriter_.traceEntry(this, "getCrossReference", primaryCatalog, primarySchema, primaryTable, foreignCatalog, foreignSchema, foreignTable);
            }
            return getCrossReferenceX(primaryCatalog, primarySchema, primaryTable,
                    foreignCatalog, foreignSchema, foreignTable);
        }
    }


    private ResultSet getCrossReferenceX(String primaryCatalog,
                                         String primarySchema,
                                         String primaryTable,
                                         String foreignCatalog,
                                         String foreignSchema,
                                         String foreignTable) throws SqlException {
        checkForClosedConnection();
        ;

        // check input params, primaryTable and foreignTable cannot be null
        if (primaryTable == null) {
            throw new SqlException(agent_.logWriter_, "getCrossReference(): null not allowed for primary table name");
        }

        if (foreignTable == null) {
            throw new SqlException(agent_.logWriter_, "getCrossReference(): null not allowed for foreign table name");
        }

        PreparedStatement cs = prepareMetaDataQuery("SYSIBM.SQLFOREIGNKEYS(?,?,?,?,?,?,?)");

        cs.setStringX(1, primaryCatalog);
        cs.setStringX(2, primarySchema);
        cs.setStringX(3, primaryTable);
        cs.setStringX(4, foreignCatalog);
        cs.setStringX(5, foreignSchema);
        cs.setStringX(6, foreignTable);
        cs.setStringX(7, getOptions());
        lastGetForeignKeysResultSet_ = executeCatalogQuery(cs);
        return lastGetForeignKeysResultSet_;
    }

    // call stored procedure SQLGetTypeInfo
    // SYSIBM.SQLGetTypeInfo (IN DATATYPE SMALLINT,
    //                        IN Options VARCHAR(4000))
    //
    //
    public java.sql.ResultSet getTypeInfo() throws SqlException {
        synchronized (connection_) {
            if (agent_.loggingEnabled()) {
                agent_.logWriter_.traceEntry(this, "getTypeInfo");
            }
            return getTypeInfoX();
        }
    }

    private ResultSet getTypeInfoX() throws SqlException {
        checkForClosedConnection();
        ;

        // check if the last call's resultset is closed or not.
        PreparedStatement cs = prepareMetaDataQuery("SYSIBM.SQLGETTYPEINFO(?,?)");

        cs.setShortX(1, (short) 0);
        cs.setStringX(2, getOptions());
        lastGetTypeInfoResultSet_ = executeCatalogQuery(cs);
        return lastGetTypeInfoResultSet_;
    }


    // call stored procedure SQLStatistics
    // SYSIBM.SQLStatistics(
    //              CatalogName varchar(128),
    //              SchemaName  varchar(128),
    //              TableName   varchar(128),
    //              Unique      Smallint,
    //              Reserved    Smallint,
    //              Options     varchar(4000))
    //
    public java.sql.ResultSet getIndexInfo(String catalog,
                                           String schema,
                                           String table,
                                           boolean unique,
                                           boolean approximate) throws SqlException {
        synchronized (connection_) {
            if (agent_.loggingEnabled()) {
                agent_.logWriter_.traceEntry(this, "getIndexInfo", catalog, schema, table, unique, approximate);
            }
            return getIndexInfoX(catalog, schema, table, unique, approximate);
        }
    }

    private ResultSet getIndexInfoX(String catalog,
                                    String schema,
                                    String table,
                                    boolean unique,
                                    boolean approximate) throws SqlException {
        checkForClosedConnection();

        // validate the input table name
        if (table == null) {
            throw new SqlException(agent_.logWriter_, "getIndexInfo(): null not allowed for table name");
        }
        PreparedStatement cs = prepareMetaDataQuery("SYSIBM.SQLSTATISTICS(?,?,?,?,?,?)");

        cs.setStringX(1, catalog);
        cs.setStringX(2, schema);
        cs.setStringX(3, table);

        if (unique) {
            cs.setShortX(4, SQL_INDEX_UNIQUE);
        } else {
            cs.setShortX(4, SQL_INDEX_ALL);
        }

        if (approximate) {
            cs.setShortX(5, (short) 1);
        } else {
            cs.setShortX(5, (short) 0);
        }

        cs.setStringX(6, getOptions());
        lastGetStatisticsResultSet_ = executeCatalogQuery(cs);
        return lastGetStatisticsResultSet_;
    }


    //--------------------------JDBC 2.0-----------------------------

    public java.sql.ResultSet getUDTs(String catalog,
                                      String schemaPattern,
                                      String typeNamePattern,
                                      int[] types) throws SqlException {
        synchronized (connection_) {
            if (agent_.loggingEnabled()) {
                agent_.logWriter_.traceEntry(this, "getUDTs", catalog, schemaPattern, typeNamePattern, types);
            }
            return getUDTsX(catalog, schemaPattern, typeNamePattern, types);
        }
    }

    private ResultSet getUDTsX(String catalog,
                               String schemaPattern,
                               String typeNamePattern,
                               int[] types) throws SqlException {
        checkForClosedConnection();
        ;

        PreparedStatement cs = prepareMetaDataQuery("SYSIBM.SQLUDTS(?,?,?,?,?)");

        cs.setStringX(1, catalog);
        cs.setStringX(2, schemaPattern);
        cs.setStringX(3, typeNamePattern);
        int i = 0;
        String udtTypes = new String();
        while (types != null && i < types.length) {
            if (i > 0) {
                udtTypes = udtTypes.concat(",");
            }
            udtTypes = udtTypes.concat(String.valueOf(types[i]));
            i++;
        }
        cs.setStringX(4, udtTypes);
        cs.setStringX(5, getOptions());
        lastGetUDTsResultSet_ = executeCatalogQuery(cs);
        return lastGetUDTsResultSet_;
    }


    // helper method for the catalog queries only
    private String getOptions() {
        int cursorHold;
        if (connection_.resultSetHoldability_ == ClientDataSource.HOLD_CURSORS_OVER_COMMIT) {
            cursorHold = 1;
        } else {
            cursorHold = 0;
        }
        return "DATATYPE='JDBC';DYNAMIC=0;REPORTPUBLICPRIVILEGES=1;CURSORHOLD=" + cursorHold;

    }

    // Derby uses a PreparedStatement argument rather than a callable statement
    private ResultSet executeCatalogQuery(PreparedStatement cs) throws SqlException {
        try {
            return cs.executeQueryX();
        } catch (SqlException e) {
            if (e.getErrorCode() == -440) {
                SqlException newException = new SqlException(agent_.logWriter_,
                        "The required stored procedure is not installed on the server.");
                newException.setNextException(e);
                throw newException;
            } else if (e.getErrorCode() == -444) {
                SqlException newException = new SqlException(agent_.logWriter_,
                        "The load module name for the stored procedure on the server is not found. ");
                newException.setNextException(e);
                throw newException;
            } else {
                throw e;
            }
        }
    }

    public java.sql.Connection getConnection() throws SqlException {
        checkForClosedConnection();
        return connection_;
    }

    // ------------------- JDBC 3.0 -------------------------

    public boolean supportsNamedParameters() throws SqlException {
        checkForClosedConnection();
        return false;
    }

    public boolean supportsMultipleOpenResults() throws SqlException {
        checkForClosedConnection();
        return true;
    }

    public boolean supportsGetGeneratedKeys() throws SqlException {
        checkForClosedConnection();
        return false;
    }

    public java.sql.ResultSet getSuperTypes(String catalog,
                                            String schemaPattern,
                                            String typeNamePattern) throws SqlException {
        synchronized (connection_) {
            if (agent_.loggingEnabled()) {
                agent_.logWriter_.traceEntry(this, "getSuperTypes", catalog, schemaPattern, typeNamePattern);
            }
            return getSuperTypesX();
        }
    }

    private ResultSet getSuperTypesX() throws SqlException {
        checkForClosedConnection();
        String sql = "SELECT CAST(NULL AS VARCHAR(128)) AS TYPE_CAT," +
                "CAST(NULL AS VARCHAR(128)) AS TYPE_SCHEM," +
                "VARCHAR('', 128) AS TYPE_NAME," +
                "CAST(NULL AS VARCHAR(128)) AS SUPERTYPE_CAT," +
                "CAST(NULL AS VARCHAR(128)) AS SUPERTYPE_SCHEM," +
                "VARCHAR('', 128) AS SUPERTYPE_NAME " +
                "FROM SYSIBM.SYSDUMMY1 WHERE 1=0 WITH UR ";
        PreparedStatement ps = connection_.prepareDynamicCatalogQuery(sql);
        lastGetSuperTypesResultSet_ = ps.executeQueryX();
        return lastGetSuperTypesResultSet_;
    }

    public java.sql.ResultSet getSuperTables(String catalog,
                                             String schemaPattern,
                                             String tableNamePattern) throws SqlException {
        synchronized (connection_) {
            if (agent_.loggingEnabled()) {
                agent_.logWriter_.traceEntry(this, "getSuperTables", catalog, schemaPattern, tableNamePattern);
            }
            return getSuperTablesX();
        }
    }

    private ResultSet getSuperTablesX() throws SqlException {
        checkForClosedConnection();
        java.lang.String sql = "SELECT CAST(NULL AS VARCHAR(128)) AS TABLE_CAT," +
                "CAST(NULL AS VARCHAR(128)) AS TABLE_SCHEM," +
                "VARCHAR('', 128) AS TABLE_NAME," +
                "VARCHAR('', 128) AS SUPERTABLE_NAME FROM SYSIBM.SYSDUMMY1 " +
                "WHERE 1=0 WITH UR";
        PreparedStatement ps = connection_.prepareDynamicCatalogQuery(sql);
        lastGetSuperTablesResultSet_ = ps.executeQueryX();
        return lastGetSuperTablesResultSet_;
    }


    public java.sql.ResultSet getAttributes(String catalog,
                                            String schemaPattern,
                                            String typeNamePattern,
                                            String attributeNamePattern) throws SqlException {
        synchronized (connection_) {
            if (agent_.loggingEnabled()) {
                agent_.logWriter_.traceEntry(this, "getAttributes", catalog, schemaPattern, typeNamePattern, attributeNamePattern);
            }
            return getAttributesX();
        }
    }

    private ResultSet getAttributesX() throws SqlException {
        checkForClosedConnection();
        java.lang.String sql = "SELECT CAST(NULL AS VARCHAR(128)) AS TYPE_CAT," +
                "CAST(NULL AS VARCHAR(128)) AS TYPE_SCHEM," +
                "VARCHAR('', 128) AS TYPE_NAME," +
                "VARCHAR('',128) AS ATTR_NAME," +
                "SMALLINT(0) AS DATA_TYPE," +
                "VARCHAR('',129) AS ATTR_TYPE_NAME," +
                "0 AS ATTR_SIZE," +
                "0 AS DECIMAL_DIGITS," +
                "0 AS NUM_PREC_RADIX," +
                "2 AS NULLABLE," +
                "CAST(NULL AS VARCHAR(254)) AS REMARKS," +
                "CAST(NULL AS VARCHAR(128)) AS ATTR_DEF," +
                "0 AS SQL_DATA_TYPE," +
                "0 AS SQL_DATETIME_SUB," +
                "0 AS CHAR_OCTET_LENGTH," +
                "0 AS ORDINAL_POSITION," +
                "VARCHAR('',128) AS IS_NULLABLE," +
                "CAST(NULL AS VARCHAR(128)) AS SCOPE_CATALOG," +
                "CAST(NULL AS VARCHAR(128)) AS SCOPE_SCHEMA," +
                "CAST(NULL AS VARCHAR(128)) AS SCOPE_TABLE," +
                "CAST(NULL AS SMALLINT) AS SOURCE_DATA_TYPE " +
                "FROM SYSIBM.SYSDUMMY1 WHERE 1=0 WITH UR";
        PreparedStatement ps = connection_.prepareDynamicCatalogQuery(sql);
        lastGetAttrResultSet_ = ps.executeQueryX();
        return lastGetAttrResultSet_;
    }

    public boolean supportsResultSetHoldability(int holdability) throws SqlException {
        checkForClosedConnection();
        return true;
    }

    public int getResultSetHoldability() throws SqlException {
        checkForClosedConnection();
        return java.sql.ResultSet.HOLD_CURSORS_OVER_COMMIT;
    }

    public int getDatabaseMajorVersion() throws SqlException {
        checkForClosedConnection();
        return productLevel_.versionLevel_;
    }

    public int getDatabaseMinorVersion() throws SqlException {
        checkForClosedConnection();
        return productLevel_.releaseLevel_;
    }

    public int getJDBCMajorVersion() throws SqlException {
        checkForClosedConnection();
        return 3;
    }

    public int getJDBCMinorVersion() throws SqlException {
        checkForClosedConnection();
        return 0;
    }

    public int getSQLStateType() throws SqlException {
        checkForClosedConnection();
        return sqlStateSQL99;
    }

    public boolean locatorsUpdateCopy() throws SqlException {
        checkForClosedConnection();
        return false;
    }

    public boolean supportsStatementPooling() throws SqlException {
        checkForClosedConnection();
        return false;
    }

    //--------------------Abstract material layer call-down methods-----------------

    // Compute feature set based on release
    abstract protected void computeFeatureSet_();

    //------------helper methods for meta data info call methods------------------


    private boolean getMetaDataInfoBoolean(int infoCallIndex) throws SqlException {
        if (metaDataInfoIsCached_) {
            return ((Integer) metaDataInfoCache_[infoCallIndex]).intValue() != 0;
        }
        metaDataInfoCall();
        return ((Integer) metaDataInfoCache_[infoCallIndex]).intValue() != 0;
    }

    private int getMetaDataInfoInt(int infoCallIndex) throws SqlException {
        if (metaDataInfoIsCached_) {
            return ((Integer) metaDataInfoCache_[infoCallIndex]).intValue();
        }
        metaDataInfoCall();
        return ((Integer) metaDataInfoCache_[infoCallIndex]).intValue();
    }

    private String getMetaDataInfoString(int infoCallIndex) throws SqlException {
        if (metaDataInfoIsCached_) {
            return (String) metaDataInfoCache_[infoCallIndex];
        }
        metaDataInfoCall();
        return (String) metaDataInfoCache_[infoCallIndex];
    }

    private boolean getMetaDataInfoBooleanWithType(int infoCallIndex, int type) throws SqlException {
        // Stored Procedure will return a String containing a
        // comma seperated list of all the supported result Set types
        // not throwing any exception right now even if the the type is wrong as per the spec
        String returnedFromSP = null;
        if (metaDataInfoIsCached_) {
            returnedFromSP = (String) metaDataInfoCache_[infoCallIndex];
        } else {
            metaDataInfoCall();
            returnedFromSP = (String) metaDataInfoCache_[infoCallIndex];
        }
        java.util.StringTokenizer st = new java.util.StringTokenizer(returnedFromSP, ",");
        while (st.hasMoreTokens()) {
            if ((new Integer(st.nextToken())).intValue() == type) {
                return true;
            }
        }
        return false;
    }

    private boolean getMetaDataInfoInt_SupportsResultSetConcurrency(int infoCallIndex, int type, int concurrency) throws SqlException {
        // The stored procured will return a String containg a list of concurrency and list of resultSet types which support
        // a perticular concurrency
        // For eg. if the database supports concurrency CONCUR_READ_ONLY(1007) in ResultSet type TYPE_FORWARD_ONLY(1003),
        // TYPE_SCROLL_INSENSITIVE(1004), TYPE_SCROLL_SENSITIVE(1005) and
        // supports concurrency CONCUR_UPDATBLE(1008) in resultSet TYPE_SCROLL_SENSITIVE(1005)
        // then stored procedure will return a string "1007,1003,1004,1005;1008,1005"
        // see how concurrency and supported result set types are seperated by ";"
        String returnedFromSP = null;
        if (metaDataInfoIsCached_) {
            returnedFromSP = (String) metaDataInfoCache_[infoCallIndex];
        } else {
            metaDataInfoCall();
            returnedFromSP = (String) metaDataInfoCache_[infoCallIndex];
        }
        java.util.StringTokenizer st = new java.util.StringTokenizer(returnedFromSP, ";");
        while (st.hasMoreTokens()) {
            java.util.StringTokenizer stForType = new java.util.StringTokenizer(st.nextToken(), ",");
            if ((new Integer(stForType.nextToken())).intValue() == concurrency) {
                while (st.hasMoreTokens()) {
                    if ((new Integer(st.nextToken())).intValue() == type) {
                        return true;
                    }
                }
                return false;
            }
        }
        return false;
    }

    private boolean getMetaDataInfoBoolean_supportsConvert(int infoCallIndex, int fromType, int toType) throws SqlException {
        // The Stored procedure will return a String contain a list of all the valid conversions it support
        // For eg. If the database conversion from char(1) to date(91), time(92) and
        // Decimal(3) to char(1) ,double(8)
        // then StoredProcedure string will return "1,91,92;3,1,8"
        // see how fromTypes are seperated by ";"
        String returnedFromSP = null;
        if (metaDataInfoIsCached_) {
            returnedFromSP = (String) metaDataInfoCache_[infoCallIndex];
        } else {
            metaDataInfoCall();
            returnedFromSP = (String) metaDataInfoCache_[infoCallIndex];
        }
        java.util.StringTokenizer st = new java.util.StringTokenizer(returnedFromSP, ";");
        while (st.hasMoreTokens()) {
            java.util.StringTokenizer stForType = new java.util.StringTokenizer(st.nextToken(), ",");
            if ((new Integer(stForType.nextToken())).intValue() == fromType) {
                while (st.hasMoreTokens()) {
                    if ((new Integer(st.nextToken())).intValue() == toType) {
                        return true;
                    }
                }
                return false;
            }
        }
        return false;
    }

    // We synchronize at this level so that we don't have to synchronize all
    // the meta data info methods.  If we just return hardwired answers we don't
    // need to synchronize at the higher level.
    private void metaDataInfoCall() throws SqlException {
        synchronized (connection_) {
            ResultSet rs;

            // These remote calls return a result set containing a single row.
            // Each column in the row corresponds to a particular get meta data info
            // method.
            PreparedStatement ps = prepareMetaDataQuery("SYSIBM.MetaData()");
            rs = (ResultSet) ps.executeQueryX();
            rs.nextX();
            int ColumnCount = ((ColumnMetaData) rs.getMetaDataX()).getColumnCount();
            for (int infoCallIndex = 0;
                 (infoCallIndex < ColumnCount && infoCallIndex < metaDataInfoCache_.length);
                 infoCallIndex++) {
                metaDataInfoCache_[infoCallIndex] = rs.getObjectX(infoCallIndex + 1);
            }
            metaDataInfoIsCached_ = true;
            rs.closeX();
        }
    }


    //----------------------------helper methods----------------------------------


    private PreparedStatement prepareMetaDataQuery(String cmd) throws SqlException {
        PreparedStatement ps;

        ps = (org.apache.derby.client.am.PreparedStatement)
                connection_.prepareStatementX("CALL " + cmd,
                        java.sql.ResultSet.TYPE_FORWARD_ONLY,
                        java.sql.ResultSet.CONCUR_READ_ONLY,
                        connection_.resultSetHoldability_,
                        java.sql.Statement.NO_GENERATED_KEYS,
                        null);
        return ps;
    }

    private void checkForClosedConnection() throws SqlException {
        if (connection_.isClosedX()) {
            agent_.checkForDeferredExceptions();
            throw new SqlException(agent_.logWriter_, "DatabaseMetaData method called after connection was closed");
        } else {
            agent_.checkForDeferredExceptions();
        }
    }
}
