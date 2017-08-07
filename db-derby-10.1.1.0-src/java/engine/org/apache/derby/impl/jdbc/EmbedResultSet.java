/*

   Derby - Class org.apache.derby.impl.jdbc.EmbedResultSet

   Copyright 1997, 2005 The Apache Software Foundation or its licensors, as applicable.

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

package org.apache.derby.impl.jdbc;

import org.apache.derby.iapi.services.sanity.SanityManager;

import org.apache.derby.iapi.error.StandardException;

import org.apache.derby.iapi.sql.conn.LanguageConnectionContext;
import org.apache.derby.iapi.sql.conn.StatementContext;

import org.apache.derby.iapi.sql.ResultSet;
import org.apache.derby.iapi.sql.Row;
import org.apache.derby.iapi.sql.ParameterValueSet;
import org.apache.derby.iapi.sql.execute.ExecCursorTableReference;
import org.apache.derby.iapi.sql.execute.ExecRow;
import org.apache.derby.iapi.sql.execute.NoPutResultSet;

import org.apache.derby.iapi.sql.dictionary.TableDescriptor;
import org.apache.derby.iapi.sql.dictionary.SchemaDescriptor;

import org.apache.derby.iapi.sql.Activation;
import org.apache.derby.iapi.sql.execute.CursorActivation;

import org.apache.derby.iapi.types.DataValueDescriptor;
import org.apache.derby.iapi.types.VariableSizeDataValue;
import org.apache.derby.iapi.sql.ResultDescription;
import org.apache.derby.iapi.services.io.StreamStorable;

import org.apache.derby.iapi.services.io.LimitInputStream;
import org.apache.derby.iapi.services.io.NewByteArrayInputStream;
import org.apache.derby.iapi.services.io.LimitReader;
import org.apache.derby.iapi.error.ExceptionSeverity;
import org.apache.derby.iapi.reference.JDBC20Translation;
import org.apache.derby.iapi.reference.SQLState;
import org.apache.derby.iapi.util.StringUtil;

/* can't import these due to name overlap:
import java.sql.ResultSet;
*/
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.ResultSetMetaData;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;

import java.util.Calendar;

/**
 * A EmbedResultSet for results from the EmbedStatement family. 
    <P><B>Supports</B>
   <UL>
   <LI> JSR 169
   </UL>
 * @author ames
 */

public abstract class EmbedResultSet extends ConnectionChild 
    implements java.sql.ResultSet, Comparable {

	// cursor movement
	protected static final int FIRST = 1;
	protected static final int NEXT = 2;
	protected static final int LAST = 3;
	protected static final int PREVIOUS = 4;
	protected static final int BEFOREFIRST = 5;
	protected static final int AFTERLAST = 6;
	protected static final int ABSOLUTE = 7;
	protected static final int RELATIVE = 8;

	// mutable state
	protected ExecRow currentRow;
	//deleteRow & updateRow make rowData null so that ResultSet is not positioned on deleted/updated row.
	private DataValueDescriptor[] rowData;
	protected boolean wasNull;
	protected boolean isClosed;
	private Object	currentStream;

	// immutable state
	protected ResultSet theResults;
	private boolean forMetaData;
	private ResultSetMetaData rMetaData;
	private SQLWarning topWarning;

	// This activation is set by EmbedStatement
	// for a single execution Activation. Ie.
	// a ResultSet from a Statement.executeQuery().
	// In this case the finalization of the ResultSet
	// will mark the Activation as unused.
	// c.f. EmbedPreparedStatement.finalize().
	Activation finalizeActivation;

	// Order of creation 
	final int order;

  
	private final ResultDescription resultDescription;

    // max rows limit for this result set
    private int maxRows;
    // The Maximum field size limt set for this result set
    private final int maxFieldSize;

    /*
     * Incase of forward only cursors we limit the number of rows
     * returned if the maxRows is set. The following varible is used
     * to keep the count of number of rows returned to the user.
     */
    private int NumberofFetchedRows;


	/*
		we hang on to the statement to prevent GC from
		closing it under us
	 */
	protected final EmbedStatement stmt;
	private EmbedStatement owningStmt;

	protected final boolean isAtomic;

	protected final int concurrencyOfThisResultSet;

	//copyOfDatabaseRow will keep the original contents of the columns of the current row which got updated.
	//These will be used if user decides to cancel the changes made to the row using cancelRowUpdates.
	private DataValueDescriptor[] copyOfDatabaseRow;
	private boolean[] columnGotUpdated; //these are the columns which have been updated so far. Used to build UPDATE...WHERE CURRENT OF sql
	private boolean currentRowHasBeenUpdated; //Gets set to true after first updateXXX on a row. Gets reset to false when the cursor moves off the row

    private int fetchDirection;
    private int fetchSize;

	/**
	 * This class provides the glue between the Cloudscape
	 * resultset and the JDBC resultset, mapping calls-to-calls.
	 */
	public EmbedResultSet(EmbedConnection conn, ResultSet resultsToWrap,
		boolean forMetaData, EmbedStatement stmt, boolean isAtomic)
        throws SQLException {

		super(conn);

		if (SanityManager.DEBUG)
		SanityManager.ASSERT(resultsToWrap!=null);
		theResults = resultsToWrap;
		this.forMetaData = forMetaData;
		this.stmt = owningStmt = stmt;
		this.isAtomic = isAtomic;

		//If the Statement object has CONCUR_READ_ONLY set on it then the concurrency on the ResultSet object will be CONCUR_READ_ONLY also.
		//But, if the Statement object has CONCUR_UPDATABLE set on it, then the concurrency on the ResultSet object can be
		//CONCUR_READ_ONLY or CONCUR_UPDATABLE depending on whether the underlying language resultset is updateable or not.
		//If the underlying language resultset is not updateable, then the concurrency of the ResultSet object will be CONCUR_READ_ONLY
		//and a warning will be issued on the ResultSet object.
		if (stmt == null) concurrencyOfThisResultSet = JDBC20Translation.CONCUR_READ_ONLY;
		else if (stmt.getResultSetConcurrency() == JDBC20Translation.CONCUR_READ_ONLY)
			concurrencyOfThisResultSet = JDBC20Translation.CONCUR_READ_ONLY;
		else {
			if (!isForUpdate()) { //language resultset not updatable
				concurrencyOfThisResultSet = JDBC20Translation.CONCUR_READ_ONLY;
				SQLWarning w = StandardException.newWarning(SQLState.QUERY_NOT_QUALIFIED_FOR_UPDATABLE_RESULTSET);
				if (topWarning == null)
					topWarning = w;
				else
					topWarning.setNextWarning(w);
			} else
					concurrencyOfThisResultSet = JDBC20Translation.CONCUR_UPDATABLE;
		}

		// Fill in the column types
		resultDescription = theResults.getResultDescription();

		//initialize arrays related to updateRow implementation
		columnGotUpdated = new boolean[getMetaData().getColumnCount()];
		copyOfDatabaseRow = new DataValueDescriptor[columnGotUpdated.length];

        // assign the max rows and maxfiled size limit for this result set
        if (stmt != null)
        {
           // At connectivity level we handle only for forward only cursor
           if (stmt.resultSetType == JDBC20Translation.TYPE_FORWARD_ONLY)
               maxRows = stmt.maxRows;

           maxFieldSize = stmt.MaxFieldSize;
        }
		else
			maxFieldSize = 0;

		order = conn.getResultSetOrderId();
	}

	/**
		JDBC states that a ResultSet is closed when garbage collected.
		We simply mark the activation as unused. Some later use
		of the connection will clean everything up.

		@exception Throwable Allows any exception to be thrown during finalize
	*/
	protected void finalize() throws Throwable {
		super.finalize();

		if (finalizeActivation != null) {
			finalizeActivation.markUnused();
		}		
	}

	// onRow protects us from making requests of
	// resultSet that would fail with NullPointerExceptions
	// or milder problems due to not having a row.
	protected final DataValueDescriptor[] checkOnRow() throws SQLException	{

		DataValueDescriptor[] theCurrentRow = rowData;

		if (theCurrentRow == null)
			throw newSQLException(SQLState.NO_CURRENT_ROW);

		return theCurrentRow;
	}

	/**
		Check the column is in range *and* return the JDBC type of the column.

		@exception SQLException ResultSet is not on a row or columnIndex is out of range.
	*/
	final int getColumnType(int columnIndex) throws SQLException {
		checkOnRow(); // first make sure there's a row
		
		if (columnIndex < 1 ||
		    columnIndex > resultDescription.getColumnCount())
			throw newSQLException(SQLState.COLUMN_NOT_FOUND, 
                         new Integer(columnIndex));

		return resultDescription.getColumnDescriptor(columnIndex).getType().getJDBCTypeId();
	}

	/*
	 * java.sql.ResultSet interface
	 */
    /**
     * A ResultSet is initially positioned before its first row; the
     * first call to next makes the first row the current row; the
     * second call makes the second row the current row, etc.
     *
     * <P>If an input stream from the previous row is open, it is
     * implicitly closed. The ResultSet's warning chain is cleared
     * when a new row is read.
     *
     * @return true if the new current row is valid; false if there
     * are no more rows
	 * @exception SQLException thrown on failure.
     */
    public boolean next() throws SQLException 
	{
        // we seem to have some trigger paths which don't have
        // statement initialized, may not need this check in those cases
        if (maxRows !=0 )
        {
            NumberofFetchedRows++;    
            // check whether we hit the maxRows limit 
            if (NumberofFetchedRows > maxRows) 
            {
                //we return false for the next call when maxRows is hit
                closeCurrentStream();
                return false;
            }
        }

	    //since we are moving off of the current row, need to initialize state corresponding to updateRow implementation
	    for (int i=0; i < columnGotUpdated.length; i++)
            columnGotUpdated[i] = false;
	    currentRowHasBeenUpdated = false;

	    return movePosition(NEXT, 0, "next");
	}

	protected boolean movePosition(int position, String positionText)
		throws SQLException
	{
		return movePosition(position, 0, positionText);
	}

	protected boolean movePosition(int position, int row, String positionText)
		throws SQLException
	{
		closeCurrentStream();	// closing currentStream does not depend on the
								// underlying connection.  Do this outside of
								// the connection synchronization.

		checkExecIfClosed(positionText);	// checking result set closure does not depend
								// on the underlying connection.  Do this
								// outside of the connection synchronization.


		synchronized (getConnectionSynchronization()) {

					setupContextStack();
		    try {
				LanguageConnectionContext lcc = getEmbedConnection().getLanguageConnection();
		    try {

				/* Push and pop a StatementContext around a next call
				 * so that the ResultSet will get correctly closed down
				 * on an error.
				 * (Cache the LanguageConnectionContext)
				 */
				StatementContext statementContext = lcc.pushStatementContext(isAtomic, getSQLText(),
														getParameterValueSet(), false);

				switch (position)
				{
					case BEFOREFIRST:
						currentRow = theResults.setBeforeFirstRow();
						break;

					case FIRST:
						currentRow = theResults.getFirstRow();
						break;

					case NEXT:
						currentRow = theResults.getNextRow();
						break;

					case LAST:
						currentRow = theResults.getLastRow();
						break;

					case AFTERLAST:
						currentRow = theResults.setAfterLastRow();
						break;

					case PREVIOUS:
						currentRow = theResults.getPreviousRow();
						break;

					case ABSOLUTE:
						currentRow = theResults.getAbsoluteRow(row);
						break;

					case RELATIVE:
						currentRow = theResults.getRelativeRow(row);
						break;

					default:
						if (SanityManager.DEBUG)
						{
							SanityManager.THROWASSERT(
								"Unexpected value for position - " + position);
						}
				}

				lcc.popStatementContext(statementContext, null);
				
		    } catch (Throwable t) {
				/*
				 * Need to close the result set here because the error might
				 * cause us to lose the current connection if this is an XA
				 * connection and we won't be able to do the close later
				 */
				throw closeOnTransactionError(t);
			}
         
			SQLWarning w = theResults.getWarnings();
			if (w != null) {
				if (topWarning == null)
					topWarning = w;
				else
					topWarning.setNextWarning(w);
			}

		    boolean onRow = (currentRow!=null);

			//if (onRow && !(currentRow instanceof org.apache.derby.impl.sql.execute.ValueRow))
			//	System.out.println(currentRow.getClass());

		    /*
			    Connection.setAutoCommit says that a statement completes,
			    and will autoCommit, when it fetches the last row or is closed.
			    This means a close will get a "Cursor already closed" error.
				This rule only applies when doing a next() - if it were applied
				to scrolling actions (like FIRST or LAST) it would close
				the cursor when doing any operation on a scrolling cursor.

			    if autocommit, this will commit
		     */
		    if (!onRow && (position == NEXT)) {

		     // In case of resultset for MetaData, we will only commit
		     // if we are the only statement currently opened for this
		     // connection; otherwise we don't want to affect other
		     // resultSet's by committing the MetaData one.
		     // There is no internal xact (xact isolation) for MetaData type
		     // of resultSet; therefore committing (to release locks) would end
		     // up committing all the other resultSet for this connection.
		     //
		     // We do synchronize on the connection, therefore Activation count
		     // should be valid and protected.
		     //
			//LanguageConnectionContext lcc = getEmbedConnection().getLanguageConnection();
		     if (forMetaData && (lcc.getActivationCount() > 1)) {
		     	// we do not want to commit here as there seems to be other
		     	// statements/resultSets currently opened for this connection.
		     } else if (owningStmt != null)
				 // allow the satement to commit if required.
		     	owningStmt.resultSetClosing(this);
		    }

			rowData = onRow ? currentRow.getRowArray() : null;

			return onRow;
			} finally {
			    restoreContextStack();
			}
		}

	}




    /**
     * In some cases, it is desirable to immediately release a
     * ResultSet's database and JDBC resources instead of waiting for
     * this to happen when it is automatically closed; the close
     * method provides this immediate release.
     *
     * <P><B>Note:</B> A ResultSet is automatically closed by the
     * Statement that generated it when that Statement is closed,
     * re-executed, or is used to retrieve the next result from a
     * sequence of multiple results. A ResultSet is also automatically
     * closed when it is garbage collected.
	 * @exception SQLException thrown on failure.
     */
    public void close() throws SQLException	{

		/* if this result is already closed, don't try to close again
		 * we may have closed it earlier because of an error and trying
		 * to close again will cause a different problem if the connection
		 * has been closed as in XA error handling
		 */
		if (isClosed)
			return;

		closeCurrentStream();	// closing currentStream does not depend on the
								// underlying connection.  Do this outside of
								// the connection synchronization.
		// Would like to throw an exception if already closed, but
		// some code assumes you can close a ResultSet more than once.
		// checkIfClosed("close");

		// synchronize out here so the close and the autocommit are
		// both in the same sync block.
		synchronized (getConnectionSynchronization()) {

			try {
				setupContextStack(); // make sure there's context
			} catch (SQLException se) {
				// we may get an exception here if this is part of an XA transaction
				// and the transaction has been committed
				// just give up and return
				return;
			}

			try	{
				try	{
				    theResults.finish(); // release the result set, don't just close it
				} catch (Throwable t) {
					throw handleException(t);
				}

			    // In case of resultset for MetaData, we will only commit
		        // if we are the only statement currently opened for this
		        // connection; otherwise we don't want to affect other
		        // resultSet's by committing the MetaData one.
		        // There is no internal xact (xact isolation) for MetaData type
		        // of resultSet; therefore committing (to release locks) would end
		        // up committing all the other resultSet for this connection.
		        //
		        // We do synchronize on the connection, therefore Activation count
		        // should be valid and protected.
		        //
		        if (forMetaData) {

					LanguageConnectionContext lcc = getEmbedConnection().getLanguageConnection();
		        	if (lcc.getActivationCount() > 1) {
		     		  // we do not want to commit here as there seems to be other
					  // statements/resultSets currently opened for this connection.
					} else if (owningStmt != null)
						// allow the satement to commit if required.
		     			owningStmt.resultSetClosing(this);
		
				} else if (owningStmt != null) {
						// allow the satement to commit if required.
		     			owningStmt.resultSetClosing(this);
		     	}

			} finally {
				isClosed = true;
			    restoreContextStack();
			}

			// the idea is to release resources, so:
			currentRow = null;
			rowData = null;
			rMetaData = null; // let it go, we can make a new one
	    //since we are moving off of the current row(by closing the resultset), need to initialize state corresponding to updateRow implementation
	    for (int i=0; i < columnGotUpdated.length; i++)
				columnGotUpdated[i] = false;
	    currentRowHasBeenUpdated = false;

			// we hang on to theResults and messenger
			// in case more calls come in on this resultSet
		}

	}

    /**
     * A column may have the value of SQL NULL; wasNull reports whether
     * the last column read had this special value.
     * Note that you must first call getXXX on a column to try to read
     * its value and then call wasNull() to find if the value was
     * the SQL NULL.
     *
     * <p> we take the least exception approach and simply return false
     * if no column has been read yet.
     *
     * @return true if last column read was SQL NULL
	 *
	 * @exception SQLException		Thrown if this ResultSet is closed
     */
    public final boolean wasNull() throws SQLException {
		checkIfClosed("wasNull");
		return wasNull;
	}

    //======================================================================
    // Methods for accessing results by column index
    //======================================================================

    /**
     * Get the value of a column in the current row as a Java String.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the result is null
	 * @exception SQLException thrown on failure.
     */
    public final String getString(int columnIndex) throws SQLException {

			try {

				DataValueDescriptor dvd = getColumn(columnIndex);

				if (wasNull = dvd.isNull())
					return null;

				String value = dvd.getString();

				// check for the max field size limit 
                if (maxFieldSize > 0 && isMaxFieldSizeType(getColumnType(columnIndex)))
                {
                    if (value.length() > maxFieldSize )
                    {
                        value = value.substring(0, maxFieldSize);
                    }
                }
     
				return value;

			} catch (Throwable t) {
				throw noStateChangeException(t);
			}
	}

    /**
     * Get the value of a column in the current row as a Java boolean.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the result is false
	 * @exception SQLException thrown on failure.
     */
    public final boolean getBoolean(int columnIndex) throws SQLException {


		try {

			DataValueDescriptor dvd = getColumn(columnIndex);

			if (wasNull = dvd.isNull())
				return false;

			return dvd.getBoolean();

		} catch (StandardException t) {
			throw noStateChangeException(t);
		}
	}

    /**
     * Get the value of a column in the current row as a Java byte.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the result is 0
	 * @exception SQLException thrown on failure.
     */
    public final byte getByte(int columnIndex) throws SQLException {

		try {

			DataValueDescriptor dvd = getColumn(columnIndex);

			if (wasNull = dvd.isNull())
				return 0;

			return dvd.getByte();

		} catch (StandardException t) {
			throw noStateChangeException(t);
		}
	}

    /**
     * Get the value of a column in the current row as a Java short.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the result is 0
	 * @exception SQLException thrown on failure.
     */
    public final short getShort(int columnIndex) throws SQLException {


		try {

			DataValueDescriptor dvd = getColumn(columnIndex);

			if (wasNull = dvd.isNull())
				return 0;

			return dvd.getShort();

		} catch (StandardException t) {
			throw noStateChangeException(t);
		}
	}

    /**
     * Get the value of a column in the current row as a Java int.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the result is 0
	 * @exception SQLException thrown on failure.
     */
    public final int getInt(int columnIndex) throws SQLException	{

		try {

			DataValueDescriptor dvd = getColumn(columnIndex);

			if (wasNull = dvd.isNull())
				return 0;

			return dvd.getInt();

		} catch (StandardException t) {
			throw noStateChangeException(t);
		}
	}

    /**
     * Get the value of a column in the current row as a Java long.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the result is 0
	 * @exception SQLException thrown on failure.
     */
    public final long getLong(int columnIndex) throws SQLException {

		try {

			DataValueDescriptor dvd = getColumn(columnIndex);

			if (wasNull = dvd.isNull())
				return 0;

			return dvd.getLong();

		} catch (StandardException t)	{
			throw noStateChangeException(t);
		}
	}

    /**
     * Get the value of a column in the current row as a Java float.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the result is 0
	 * @exception SQLException thrown on failure.
     */
    public final float getFloat(int columnIndex) throws SQLException {

		try {

			DataValueDescriptor dvd = getColumn(columnIndex);

			if (wasNull = dvd.isNull())
				return 0.0F;

			return dvd.getFloat();

		} catch (StandardException t)	{
			throw noStateChangeException(t);
		}
	}

    /**
     * Get the value of a column in the current row as a Java double.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the result is 0
	 * @exception SQLException thrown on failure.
     */
    public final double getDouble(int columnIndex) throws SQLException {

		try {

			DataValueDescriptor dvd = getColumn(columnIndex);

			if (wasNull = dvd.isNull())
				return 0.0;

			return dvd.getDouble();

		} catch (StandardException t)	{
			throw noStateChangeException(t);
		}
	}

    /**
     * Get the value of a column in the current row as a Java byte array.
     * The bytes represent the raw values returned by the driver.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the result is null
	 * @exception SQLException thrown on failure.
     */
    public final byte[] getBytes(int columnIndex) throws SQLException	{

		try {

			DataValueDescriptor dvd = getColumn(columnIndex);

			if (wasNull = dvd.isNull())
				return null;

			byte[] value = dvd.getBytes();

            // check for the max field size limit 
            if (maxFieldSize > 0 && isMaxFieldSizeType(getColumnType(columnIndex)))
            {
                 if (value.length > maxFieldSize)
                 {
                     byte [] limited_value = new byte[maxFieldSize];
                     System.arraycopy(value, 0, limited_value, 
                                                   0 , maxFieldSize);
                     value = limited_value;
                 }
             }
			
			return value;

		} catch (StandardException t) {
			throw noStateChangeException(t);
		}
	}

    /**
     * Get the value of a column in the current row as a java.sql.Date object.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the result is null
	 * @exception SQLException thrown on failure.
     */
    public final Date getDate(int columnIndex) throws SQLException {
        return getDate( columnIndex, (Calendar) null);
	}

    /**
     * Get the value of a column in the current row as a java.sql.Time object.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the result is null
	 * @exception SQLException thrown on failure.
     */
    public final Time getTime(int columnIndex) throws SQLException {
        return getTime( columnIndex, (Calendar) null);
	}

    /**
     * Get the value of a column in the current row as a java.sql.Timestamp object.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the result is null
	 * @exception SQLException thrown on failure.
     */
    public final Timestamp getTimestamp(int columnIndex) throws SQLException {
        return getTimestamp( columnIndex, (Calendar) null);
	}

    /**
     * JDBC 2.0
     *
     * Get the value of a column in the current row as a java.sql.Date 
     * object.  Use the calendar to construct an appropriate millisecond
     * value for the Date, if the underlying database doesn't store
     * timezone information.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param cal the calendar to use in constructing the date
     * @return the column value; if the value is SQL NULL, the result is null
     * @exception SQLException if a database-access error occurs.
     */
    public java.sql.Date getDate(int columnIndex, Calendar cal)
        throws SQLException 
    {
		try {

			DataValueDescriptor dvd = getColumn(columnIndex);

			if (wasNull = dvd.isNull())
				return null;

            if( cal == null)
                cal = getCal();
            
			return dvd.getDate( cal);

		} catch (StandardException t) {
			throw noStateChangeException(t);
		}
    }

    /**
     * JDBC 2.0
     *
     * Get the value of a column in the current row as a java.sql.Date 
     * object. Use the calendar to construct an appropriate millisecond
     * value for the Date, if the underlying database doesn't store
     * timezone information.
     *
     * @param columnName is the SQL name of the column
     * @param cal the calendar to use in constructing the date
     * @return the column value; if the value is SQL NULL, the result is null
     * @exception SQLException if a database-access error occurs.
     */
    public java.sql.Date getDate(String columnName, Calendar cal) 
                throws SQLException 
        {
                return getDate( findColumnName(columnName), cal);
        }

    /**
     * JDBC 2.0
     *
     * Get the value of a column in the current row as a java.sql.Time 
     * object. Use the calendar to construct an appropriate millisecond
     * value for the Time, if the underlying database doesn't store
     * timezone information.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param cal the calendar to use in constructing the time
     * @return the column value; if the value is SQL NULL, the result is null
     * @exception SQLException if a database-access error occurs.
     */
    public java.sql.Time getTime(int columnIndex, Calendar cal)
        throws SQLException 
    {
		try {

			DataValueDescriptor dvd = getColumn(columnIndex);

			if (wasNull = dvd.isNull())
				return null;

            if( cal == null)
                cal = getCal();
			return dvd.getTime( cal);

		} catch (StandardException t) {
			throw noStateChangeException(t);
		}
    }

    /**
     * JDBC 2.0
     *
     * Get the value of a column in the current row as a java.sql.Time 
     * object. Use the calendar to construct an appropriate millisecond
     * value for the Time, if the underlying database doesn't store
     * timezone information.
     *
     * @param columnName is the SQL name of the column
     * @param cal the calendar to use in constructing the time
     * @return the column value; if the value is SQL NULL, the result is null
     * @exception SQLException if a database-access error occurs.
     */
    public java.sql.Time getTime(String columnName, Calendar cal)
           throws SQLException 
        {
                return getTime( findColumnName( columnName), cal);
        }

    /**
     * JDBC 2.0
     *
     * Get the value of a column in the current row as a java.sql.Timestamp 
     * object. Use the calendar to construct an appropriate millisecond
     * value for the Timestamp, if the underlying database doesn't store
     * timezone information.
     *
     * @param columnName is the SQL name of the column
     * @param cal the calendar to use in constructing the timestamp
     * @return the column value; if the value is SQL NULL, the result is null
     * @exception SQLException if a database-access error occurs.
     */
    public java.sql.Timestamp getTimestamp(String columnName, Calendar cal)     
      throws SQLException 
        {
                return getTimestamp(findColumnName(columnName), cal);
        }

    /**
     * JDBC 2.0
     *
     * Get the value of a column in the current row as a java.sql.Timestamp 
     * object. Use the calendar to construct an appropriate millisecond
     * value for the Timestamp, if the underlying database doesn't store
     * timezone information.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @param cal the calendar to use in constructing the timestamp
     * @return the column value; if the value is SQL NULL, the result is null
     * @exception SQLException if a database-access error occurs.
     */
    public java.sql.Timestamp getTimestamp(int columnIndex, Calendar cal) 
        throws SQLException 
    {
		try {

			DataValueDescriptor dvd = getColumn(columnIndex);

			if (wasNull = dvd.isNull())
				return null;

            if( cal == null)
                cal = getCal();
			return dvd.getTimestamp( cal);

		} catch (StandardException t) {
			throw noStateChangeException(t);
		}
    }

	/**
     * JDBC 2.0
     *
     * <p>Get the value of a column in the current row as a java.io.Reader.
     *
     * @exception SQLException database error.
     */
    public final java.io.Reader getCharacterStream(int columnIndex)
		throws SQLException
	{
		int lmfs;
		int colType = getColumnType(columnIndex);
		switch (colType) {
		case Types.CHAR:
		case Types.VARCHAR:
		case Types.LONGVARCHAR:
			lmfs = maxFieldSize;
			break;
		case Types.CLOB: // Embedded and JCC extension - CLOB is not subject to max field size.
			lmfs = 0;
			break;

		// JDBC says to support these, but no defintion exists for the output.
		// match JCC which treats the bytes as a UTF16-BE stream
		case Types.BINARY:
		case Types.VARBINARY:
		case Types.LONGVARBINARY:
		case Types.BLOB:
			try {
				java.io.InputStream is = getBinaryStream(columnIndex);
				if (is == null)
					return null;
				java.io.Reader r = new java.io.InputStreamReader(is, "UTF-16BE");
				currentStream = r;
				return r;
			} catch (java.io.UnsupportedEncodingException uee) {
				throw new SQLException(uee.getMessage());
			}
		default:
			throw dataTypeConversion("java.io.Reader", columnIndex);
		}

		Object syncLock = getConnectionSynchronization();

		synchronized (syncLock) {

		boolean pushStack = false;
		try {

			DataValueDescriptor dvd = getColumn(columnIndex);

			if (wasNull = dvd.isNull()) { return null; }

			pushStack = true;
			setupContextStack();

			StreamStorable ss = (StreamStorable) dvd;

			InputStream stream = ss.returnStream();

			if (stream == null) {

				String val = dvd.getString();
				if (lmfs > 0) {
					if (val.length() > lmfs)
						val = val.substring(0, lmfs);
				}
				java.io.Reader ret = new java.io.StringReader(val);
				currentStream = ret;
				return ret;
			}

			java.io.Reader ret = new UTF8Reader(stream, lmfs, this, syncLock);
			currentStream = ret;
			return ret;

		} catch (Throwable t) {
			throw noStateChangeException(t);
		} finally {
			if (pushStack) { restoreContextStack(); }
		}
	  }
     }

    /**
		Pushes a converter on top of getCharacterStream().
	 *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return a Java input stream that delivers the database column value
     * as a stream of one byte ASCII characters.  If the value is SQL NULL
     * then the result is null.
	 * @exception SQLException thrown on failure.
     */
    public final InputStream getAsciiStream(int columnIndex) throws SQLException {

		int colType = getColumnType(columnIndex);
		switch (colType) {
		case Types.CHAR:
		case Types.VARCHAR:
		case Types.LONGVARCHAR:
		case Types.CLOB: // Embedded and JCC extension
			break;

		// JDBC says to support these, we match JCC by returning the raw bytes.
		case Types.BINARY:
		case Types.VARBINARY:
		case Types.LONGVARBINARY:
		case Types.BLOB:
			return getBinaryStream(columnIndex);

		default:
			throw dataTypeConversion("java.io.InputStream(ASCII)", columnIndex);
		}

		java.io.Reader reader = getCharacterStream(columnIndex);
		if (reader == null)
			return null;

		return new ReaderToAscii(reader);
	}

    /**
	 * Get the column as an InputStream. If the column is already of type
	   InputStream then just return it, otherwise convert the column to a set
	   of bytes and create a stream out of the bytes.
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return a Java input stream that delivers the database column value
     * as a stream of uninterpreted bytes.  If the value is SQL NULL
     * then the result is null.
	 * @exception SQLException thrown on failure.
     */
    public final InputStream getBinaryStream(int columnIndex) throws SQLException {

		int lmfs;
		int colType = getColumnType(columnIndex);
		switch (colType) {
		case Types.BINARY:
		case Types.VARBINARY:
		case Types.LONGVARBINARY:
			lmfs = maxFieldSize;
			break;
		case Types.BLOB:
			lmfs = 0;
			break;

		default:
			throw dataTypeConversion("java.io.InputStream", columnIndex);
		}

		Object syncLock = getConnectionSynchronization();

		synchronized (syncLock) {

		boolean pushStack = false;
		try {

			DataValueDescriptor dvd = getColumn(columnIndex);

			if (wasNull = dvd.isNull()) { return null; }

			pushStack = true;
			setupContextStack();

			StreamStorable ss = (StreamStorable) dvd;

			InputStream stream = ss.returnStream();

			if (stream == null)
			{
				stream = new NewByteArrayInputStream(dvd.getBytes());
			} else
			{
				stream = new BinaryToRawStream(stream, dvd);
			}

            if (lmfs > 0)
            {
                // Just wrap the InputStream with a LimitInputStream class
                LimitInputStream  limitResultIn = new  LimitInputStream(stream);
                limitResultIn.setLimit(lmfs);
                stream = limitResultIn;
            }
			currentStream = stream;
			return stream;

		} catch (Throwable t) {
			throw noStateChangeException(t);
		} finally {
			if (pushStack) { restoreContextStack(); }
		}
	  }
	}

    //======================================================================
    // Methods for accessing results by column name
    //======================================================================


    /**
     * Get the value of a column in the current row as a Java String.
     *
     * @param columnName is the SQL name of the column
     * @return the column value; if the value is SQL NULL, the result is null
	 * @exception SQLException thrown on failure.
     */
    public final String getString(String columnName) throws SQLException {
    	return (getString(findColumnName(columnName)));
	}

    /**
     * Get the value of a column in the current row as a Java boolean.
     *
     * @param columnName is the SQL name of the column
     * @return the column value; if the value is SQL NULL, the result is false
	 * @exception SQLException thrown on failure.
     */
    public final boolean getBoolean(String columnName) throws SQLException {
    	return (getBoolean(findColumnName(columnName)));
	}

    /**
     * Get the value of a column in the current row as a Java byte.
     *
     * @param columnName is the SQL name of the column
     * @return the column value; if the value is SQL NULL, the result is 0
	 * @exception SQLException thrown on failure.
     */
    public final byte getByte(String columnName) throws SQLException	{
    	return (getByte(findColumnName(columnName)));
	}

    /**
     * Get the value of a column in the current row as a Java short.
     *
     * @param columnName is the SQL name of the column
     * @return the column value; if the value is SQL NULL, the result is 0
	 * @exception SQLException thrown on failure.
     */
    public final short getShort(String columnName) throws SQLException {
    	return (getShort(findColumnName(columnName)));
	}

    /**
     * Get the value of a column in the current row as a Java int.
     *
     * @param columnName is the SQL name of the column
     * @return the column value; if the value is SQL NULL, the result is 0
	 * @exception SQLException thrown on failure.
     */
    public final int getInt(String columnName) throws SQLException {
    	return (getInt(findColumnName(columnName)));
	}

    /**
     * Get the value of a column in the current row as a Java long.
     *
     * @param columnName is the SQL name of the column
     * @return the column value; if the value is SQL NULL, the result is 0
	 * @exception SQLException thrown on failure.
     */
    public final long getLong(String columnName) throws SQLException {
    	return (getLong(findColumnName(columnName)));
	}

    /**
     * Get the value of a column in the current row as a Java float.
     *
     * @param columnName is the SQL name of the column
     * @return the column value; if the value is SQL NULL, the result is 0
	 * @exception SQLException thrown on failure.
     */
    public final float getFloat(String columnName) throws SQLException {
    	return (getFloat(findColumnName(columnName)));
	}

    /**
     * Get the value of a column in the current row as a Java double.
     *
     * @param columnName is the SQL name of the column
     * @return the column value; if the value is SQL NULL, the result is 0
	 * @exception SQLException thrown on failure.
     */
    public final double getDouble(String columnName) throws SQLException {
    	return (getDouble(findColumnName(columnName)));
	}

    /**
     * Get the value of a column in the current row as a Java byte array.
     * The bytes represent the raw values returned by the driver.
     *
     * @param columnName is the SQL name of the column
     * @return the column value; if the value is SQL NULL, the result is null
	 * @exception SQLException thrown on failure.
     */
    public final byte[] getBytes(String columnName) throws SQLException {
    	return (getBytes(findColumnName(columnName)));
	}

    /**
     * Get the value of a column in the current row as a java.sql.Date object.
     *
     * @param columnName is the SQL name of the column
     * @return the column value; if the value is SQL NULL, the result is null
	 * @exception SQLException thrown on failure.
     */
    public final Date getDate(String columnName) throws SQLException {
    	return (getDate(findColumnName(columnName)));
	}

    /**
     * Get the value of a column in the current row as a java.sql.Time object.
     *
     * @param columnName is the SQL name of the column
     * @return the column value; if the value is SQL NULL, the result is null
	 * @exception SQLException thrown on failure.
     */
    public final Time getTime(String columnName) throws SQLException {
    	return (getTime(findColumnName(columnName)));
	}

    /**
     * Get the value of a column in the current row as a java.sql.Timestamp object.
     *
     * @param columnName is the SQL name of the column
     * @return the column value; if the value is SQL NULL, the result is null
	 * @exception SQLException thrown on failure.
     */
    public final Timestamp getTimestamp(String columnName) throws SQLException {
    	return (getTimestamp(findColumnName(columnName)));
	}

	/**
     * JDBC 2.0
     *
     * <p>Get the value of a column in the current row as a java.io.Reader.
         *
         * @exception SQLException Feature not implemented for now.
     */
    public final java.io.Reader getCharacterStream(String columnName)
    throws SQLException {
    	return (getCharacterStream(findColumnName(columnName)));
    }

    /**
     * A column value can be retrieved as a stream of ASCII characters
     * and then read in chunks from the stream.  This method is particularly
     * suitable for retrieving large LONGVARCHAR values.  The JDBC driver will
     * do any necessary conversion from the database format into ASCII.
     *
     * <P><B>Note:</B> All the data in the returned stream must
     * be read prior to getting the value of any other column. The
     * next call to a get method implicitly closes the stream.
     *
     * @param columnName is the SQL name of the column
     * @return a Java input stream that delivers the database column value
     * as a stream of one byte ASCII characters.  If the value is SQL NULL
     * then the result is null.
	 * @exception SQLException thrown on failure.
     */
    public final InputStream getAsciiStream(String columnName) throws SQLException {
    	return (getAsciiStream(findColumnName(columnName)));
	}

    /**
     * A column value can be retrieved as a stream of uninterpreted bytes
     * and then read in chunks from the stream.  This method is particularly
     * suitable for retrieving large LONGVARBINARY values.
     *
     * <P><B>Note:</B> All the data in the returned stream must
     * be read prior to getting the value of any other column. The
     * next call to a get method implicitly closes the stream.
     *
     * @param columnName is the SQL name of the column
     * @return a Java input stream that delivers the database column value
     * as a stream of uninterpreted bytes.  If the value is SQL NULL
     * then the result is null.
	 * @exception SQLException thrown on failure.
     */
    public final InputStream getBinaryStream(String columnName) throws SQLException {
    	return (getBinaryStream(findColumnName(columnName)));
	}

    /**
	 * JDBC 3.0
	 * 
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a java.net.URL object in the Java programming
	 * language.
	 * 
	 * @param columnIndex -
	 *            the first column is 1, the second is 2
	 * @return the column value as a java.net.URL object, if the value is SQL
	 *         NULL, the value returned is null in the Java programming language
	 * @exception SQLException
	 *                Feature not implemented for now.
	 */
	public URL getURL(int columnIndex) throws SQLException {
		throw Util.notImplemented();
	}

	/**
	 * JDBC 3.0
	 * 
	 * Retrieves the value of the designated column in the current row of this
	 * ResultSet object as a java.net.URL object in the Java programming
	 * language.
	 * 
	 * @param columnName -
	 *            the SQL name of the column
	 * @return the column value as a java.net.URL object, if the value is SQL
	 *         NULL, the value returned is null in the Java programming language
	 * @exception SQLException
	 *                Feature not implemented for now.
	 */
	public URL getURL(String columnName) throws SQLException {
		throw Util.notImplemented();
	}
 
	//=====================================================================
    // Advanced features:
    //=====================================================================

    /**
     * <p>The first warning reported by calls on this ResultSet is
     * returned. Subsequent ResultSet warnings will be chained to this
     * SQLWarning.
     *
     * <P>The warning chain is automatically cleared each time a new
     * row is read.
     *
     * <P><B>Note:</B> This warning chain only covers warnings caused
     * by ResultSet methods.  Any warning caused by statement methods
     * (such as reading OUT parameters) will be chained on the
     * Statement object.
     *
     * @return the first SQLWarning or null
	 *
	 * @exception SQLException 	Thrown if this ResultSet is closed
     */
    public final SQLWarning getWarnings() throws SQLException {
		checkIfClosed("getWarnings");
		return topWarning;
	}

    /**
     * After this call getWarnings returns null until a new warning is
     * reported for this ResultSet.
	 *
	 * @exception SQLException	Thrown if this ResultSet is closed
     */
    public final void clearWarnings() throws SQLException {
		checkIfClosed("clearWarnings");
		topWarning = null;
	}

    /**
     * Get the name of the SQL cursor used by this ResultSet.
     *
     * <P>In SQL, a result table is retrieved through a cursor that is
     * named. The current row of a result can be updated or deleted
     * using a positioned update/delete statement that references the
     * cursor name.
     *
     * <P>JDBC supports this SQL feature by providing the name of the
     * SQL cursor used by a ResultSet. The current row of a ResultSet
     * is also the current row of this SQL cursor.
     *
     * <P><B>Note:</B> If positioned update is not supported a
     * SQLException is thrown
     *
     * @return the ResultSet's SQL cursor name
	 * @exception SQLException thrown on failure.
     */
    public final String getCursorName() throws SQLException {

	  checkIfClosed("getCursorName");	// checking result set closure does not depend
								// on the underlying connection.  Do this
								// outside of the connection synchronization.
	  
	  return theResults.getCursorName();
	}

    /**
     * The number, types and properties of a ResultSet's columns
     * are provided by the getMetaData method.
     *
     * @return the description of a ResultSet's columns
	 * @exception SQLException thrown on failure.
     */
    public ResultSetMetaData getMetaData() throws SQLException {

	  checkIfClosed("getMetaData");	// checking result set closure does not depend
								// on the underlying connection.  Do this
								// outside of the connection synchronization.

	  synchronized (getConnectionSynchronization()) {


		if (rMetaData == null) {
			// cache this object and keep returning it
			rMetaData = newEmbedResultSetMetaData(resultDescription);
		}
		return rMetaData;
	  }
	}

    /**
     * <p>Get the value of a column in the current row as a Java object.
     *
     * <p>This method will return the value of the given column as a
     * Java object.  The type of the Java object will be the default
     * Java Object type corresponding to the column's SQL type,
     * following the mapping specified in the JDBC spec.
     *
     * <p>This method may also be used to read datatabase specific abstract
     * data types.
	 *
	 * JDBC 2.0
     *
     * New behavior for getObject().
     * The behavior of method getObject() is extended to materialize  
     * data of SQL user-defined types.  When the column @columnIndex is 
     * a structured or distinct value, the behavior of this method is as 
     * if it were a call to: getObject(columnIndex, 
     * this.getStatement().getConnection().getTypeMap()).
     *
     * @param columnIndex the first column is 1, the second is 2, ...
     * @return A java.lang.Object holding the column value.
	 * @exception SQLException thrown on failure.
     */
    public final Object getObject(int columnIndex) throws SQLException {


		// need special handling for some types.
		int colType = getColumnType(columnIndex);
		switch (colType) {
		case Types.CHAR:
		case Types.VARCHAR:
		case Types.LONGVARCHAR:
			// handles maxfield size correctly
			return getString(columnIndex);

		case Types.CLOB:
			return getClob(columnIndex);

		case Types.BINARY:
		case Types.VARBINARY:
		case Types.LONGVARBINARY:
			// handles maxfield size correctly
			return getBytes(columnIndex);

		case Types.BLOB:
			return getBlob(columnIndex);

		default:
			break;
		}

		try {

			DataValueDescriptor dvd = getColumn(columnIndex);
			if (wasNull = dvd.isNull())
				return null;

			return dvd.getObject();

		} catch (StandardException t) {
			throw noStateChangeException(t);
		}
	}

    /**
     * <p>Get the value of a column in the current row as a Java object.
     *
     * <p>This method will return the value of the given column as a
     * Java object.  The type of the Java object will be the default
     * Java Object type corresponding to the column's SQL type,
     * following the mapping specified in the JDBC spec.
     *
     * <p>This method may also be used to read datatabase specific abstract
     * data types.
     *
     * JDBC 2.0
     *
     * New behavior for getObject().
     * The behavior of method getObject() is extended to materialize  
     * data of SQL user-defined types.  When the column @columnName is 
     * a structured or distinct value, the behavior of this method is as 
     * if it were a call to: getObject(columnName, 
     * this.getStatement().getConnection().getTypeMap()).
     *
     * @param columnName is the SQL name of the column
     * @return A java.lang.Object holding the column value.
	 * @exception SQLException thrown on failure.
     */
    public final Object getObject(String columnName) throws SQLException {
    	return (getObject(findColumnName(columnName)));
	}


    //----------------------------------------------------------------

    /**
     * Map a Resultset column name to a ResultSet column index.
     *
     * @param columnName the name of the column
     * @return the column index
	 * @exception SQLException thrown on failure.
     */
	public final int findColumn(String columnName) throws SQLException {
		checkIfClosed("findColumn");
		return findColumnName(columnName);
	}
	
    /////////////////////////////////////////////////////////////////////////
    //
    //      JDBC 2.0        -       New public methods
    //
    /////////////////////////////////////////////////////////////////////////


	//---------------------------------------------------------------------
	// Getter's and Setter's
	//---------------------------------------------------------------------

	/**
	 * JDBC 2.0
	 * 
	 * Return the Statement that produced the ResultSet.
	 * 
	 * @return the Statment that produced the result set, or null if the result
	 *         was produced some other way.
	 */
	public final Statement getStatement()
    {
            return stmt;
    }

	//---------------------------------------------------------------------
	// Traversal/Positioning
	//---------------------------------------------------------------------

	/**
	 * JDBC 2.0
	 * 
	 * <p>
	 * Determine if the cursor is before the first row in the result set.
	 * 
	 * @return true if before the first row, false otherwise. Returns false when
	 *         the result set contains no rows.
	 * @exception SQLException
	 *                Thrown on error.
	 */
	public boolean isBeforeFirst() throws SQLException {
		return checkRowPosition(ResultSet.ISBEFOREFIRST, "isBeforeFirst");
	}

	/**
	 * JDBC 2.0
	 * 
	 * <p>
	 * Determine if the cursor is after the last row in the result set.
	 * 
	 * @return true if after the last row, false otherwise. Returns false when
	 *         the result set contains no rows.
	 * @exception SQLException
	 *                Thrown on error.
	 */
	public boolean isAfterLast() throws SQLException {
		return checkRowPosition(ResultSet.ISAFTERLAST, "isAfterLast");
	}

	/**
	 * JDBC 2.0
	 * 
	 * <p>
	 * Determine if the cursor is on the first row of the result set.
	 * 
	 * @return true if on the first row, false otherwise.
	 * @exception SQLException
	 *                Thrown on error.
	 */
	public boolean isFirst() throws SQLException {
		return checkRowPosition(ResultSet.ISFIRST, "isFirst");
	}

	/**
	 * JDBC 2.0
	 * 
	 * <p>
	 * Determine if the cursor is on the last row of the result set. Note:
	 * Calling isLast() may be expensive since the JDBC driver might need to
	 * fetch ahead one row in order to determine whether the current row is the
	 * last row in the result set.
	 * 
	 * @return true if on the last row, false otherwise.
	 * @exception SQLException
	 *                Thrown on error.
	 */
	public boolean isLast() throws SQLException {
		return checkRowPosition(ResultSet.ISLAST, "isLast");
	}

	/**
	 * JDBC 2.0
	 * 
	 * <p>
	 * Moves to the front of the result set, just before the first row. Has no
	 * effect if the result set contains no rows.
	 * 
	 * @exception SQLException
	 *                if a database-access error occurs, or result set type is
	 *                TYPE_FORWARD_ONLY
	 */
	public void beforeFirst() throws SQLException {
		// beforeFirst is only allowed on scroll cursors
		checkScrollCursor("beforeFirst()");
		movePosition(BEFOREFIRST, "beforeFirst");
	}

	/**
	 * JDBC 2.0
	 * 
	 * <p>
	 * Moves to the end of the result set, just after the last row. Has no
	 * effect if the result set contains no rows.
	 * 
	 * @exception SQLException
	 *                if a database-access error occurs, or result set type is
	 *                TYPE_FORWARD_ONLY.
	 */
	public void afterLast() throws SQLException {
		// afterLast is only allowed on scroll cursors
		checkScrollCursor("afterLast()");
		movePosition(AFTERLAST, "afterLast");
	}

	/**
	 * JDBC 2.0
	 * 
	 * <p>
	 * Moves to the first row in the result set.
	 * 
	 * @return true if on a valid row, false if no rows in the result set.
	 * @exception SQLException
	 *                if a database-access error occurs, or result set type is
	 *                TYPE_FORWARD_ONLY.
	 */
	public boolean first() throws SQLException {
		// first is only allowed on scroll cursors
		checkScrollCursor("first()");
		return movePosition(FIRST, "first");
	}

	/**
	 * JDBC 2.0
	 * 
	 * <p>
	 * Moves to the last row in the result set.
	 * 
	 * @return true if on a valid row, false if no rows in the result set.
	 * @exception SQLException
	 *                if a database-access error occurs, or result set type is
	 *                TYPE_FORWARD_ONLY.
	 */
	public boolean last() throws SQLException {
		// last is only allowed on scroll cursors
		checkScrollCursor("last()");
		return movePosition(LAST, "last");
	}

	/**
	 * JDBC 2.0
	 * 
	 * <p>
	 * Determine the current row number. The first row is number 1, the second
	 * number 2, etc.
	 * 
	 * @return the current row number, else return 0 if there is no current row
	 * @exception SQLException
	 *                if a database-access error occurs.
	 */
	public int getRow() throws SQLException {
		// getRow() is only allowed on scroll cursors
		checkScrollCursor("getRow()");

		/*
		 * * We probably needn't bother getting the text of * the underlying
		 * statement but it is better to be * consistent and we aren't
		 * particularly worried * about performance of getRow().
		 */
		return theResults.getRowNumber();
	}

	/**
	 * JDBC 2.0
	 * 
	 * <p>
	 * Move to an absolute row number in the result set.
	 * 
	 * <p>
	 * If row is positive, moves to an absolute row with respect to the
	 * beginning of the result set. The first row is row 1, the second is row 2,
	 * etc.
	 * 
	 * <p>
	 * If row is negative, moves to an absolute row position with respect to the
	 * end of result set. For example, calling absolute(-1) positions the cursor
	 * on the last row, absolute(-2) indicates the next-to-last row, etc.
	 * 
	 * <p>
	 * An attempt to position the cursor beyond the first/last row in the result
	 * set, leaves the cursor before/after the first/last row, respectively.
	 * 
	 * <p>
	 * Note: Calling absolute(1) is the same as calling first(). Calling
	 * absolute(-1) is the same as calling last().
	 * 
	 * @return true if on the result set, false if off.
	 * @exception SQLException
	 *                if a database-access error occurs, or row is 0, or result
	 *                set type is TYPE_FORWARD_ONLY.
	 */
	public boolean absolute(int row) throws SQLException {
		// absolute is only allowed on scroll cursors
		checkScrollCursor("absolute()");
		return movePosition(ABSOLUTE, row, "absolute");
	}

	/**
	 * JDBC 2.0
	 * 
	 * <p>
	 * Moves a relative number of rows, either positive or negative. Attempting
	 * to move beyond the first/last row in the result set positions the cursor
	 * before/after the the first/last row. Calling relative(0) is valid, but
	 * does not change the cursor position.
	 * 
	 * <p>
	 * Note: Calling relative(1) is different than calling next() since is makes
	 * sense to call next() when there is no current row, for example, when the
	 * cursor is positioned before the first row or after the last row of the
	 * result set.
	 * 
	 * @return true if on a row, false otherwise.
	 * @exception SQLException
	 *                if a database-access error occurs, or there is no current
	 *                row, or result set type is TYPE_FORWARD_ONLY.
	 */
	public boolean relative(int row) throws SQLException {
		// absolute is only allowed on scroll cursors
		checkScrollCursor("relative()");
		return movePosition(RELATIVE, row, "relative");
	}

	/**
	 * JDBC 2.0
	 * 
	 * <p>
	 * Moves to the previous row in the result set.
	 * 
	 * <p>
	 * Note: previous() is not the same as relative(-1) since it makes sense to
	 * call previous() when there is no current row.
	 * 
	 * @return true if on a valid row, false if off the result set.
	 * @exception SQLException
	 *                if a database-access error occurs, or result set type is
	 *                TYPE_FORWAR_DONLY.
	 */
	public boolean previous() throws SQLException {
		// previous is only allowed on scroll cursors
		checkScrollCursor("previous()");
		return movePosition(PREVIOUS, "previous");
	}

	//---------------------------------------------------------------------
	// Properties
	//---------------------------------------------------------------------

	/**
	 * JDBC 2.0
	 * 
	 * Give a hint as to the direction in which the rows in this result set will
	 * be processed. The initial value is determined by the statement that
	 * produced the result set. The fetch direction may be changed at any time.
	 * 
	 * @exception SQLException
	 *                if a database-access error occurs, or the result set type
	 *                is TYPE_FORWARD_ONLY and direction is not FETCH_FORWARD.
	 */
	public void setFetchDirection(int direction) throws SQLException {
		checkScrollCursor("setFetchDirection()");
		/*
		 * FetchDirection is meaningless to us. We just save it off and return
		 * the current value if asked.
		 */
		fetchDirection = direction;
	}

	/**
	 * JDBC 2.0
	 * 
	 * Return the fetch direction for this result set.
	 * 
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public int getFetchDirection() throws SQLException {
		if (fetchDirection == 0) {
			// value is not set at the result set level
			// get it from the statement level
			return stmt.getFetchDirection();
		} else
			return fetchDirection;
	}

	/**
	 * JDBC 2.0
	 * 
	 * Give the JDBC driver a hint as to the number of rows that should be
	 * fetched from the database when more rows are needed for this result set.
	 * If the fetch size specified is zero, then the JDBC driver ignores the
	 * value, and is free to make its own best guess as to what the fetch size
	 * should be. The default value is set by the statement that creates the
	 * result set. The fetch size may be changed at any time.
	 * 
	 * @param rows
	 *            the number of rows to fetch
	 * @exception SQLException
	 *                if a database-access error occurs, or the condition 0 <=
	 *                rows <= this.getMaxRows() is not satisfied.
	 */
	public void setFetchSize(int rows) throws SQLException {
		if (rows < 0 || (stmt.getMaxRows() != 0 && rows > stmt.getMaxRows())) {
			throw Util.generateCsSQLException(SQLState.INVALID_FETCH_SIZE,
					new Integer(rows));
		} else if (rows > 0) // if it is zero ignore the call
		{
			fetchSize = rows;
		}
	}

	/**
	 * JDBC 2.0
	 * 
	 * Return the fetch size for this result set.
	 * 
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public int getFetchSize() throws SQLException {
		if (fetchSize == 0) {
			// value is not set at the result set level
			//  get the default value from the statement
			return stmt.getFetchSize();
		} else
			return fetchSize;
	}

	/**
	 * JDBC 2.0
	 * 
	 * Return the type of this result set. The type is determined based on the
	 * statement that created the result set.
	 * 
	 * @return TYPE_FORWARD_ONLY, TYPE_SCROLL_INSENSITIVE, or
	 *         TYPE_SCROLL_SENSITIVE
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public int getType() throws SQLException {
		return stmt.getResultSetType();
	}

	/**
	 * JDBC 2.0
	 * 
	 * Return the concurrency of this result set. The concurrency is determined
	 * as follows If Statement object has CONCUR_READ_ONLY concurrency, then
	 * ResultSet object will also have the CONCUR_READ_ONLY concurrency. But if
	 * Statement object has CONCUR_UPDATABLE concurrency, then the concurrency
	 * of ResultSet object depends on whether the underlying language resultset
	 * is updatable or not. If the language resultset is updatable, then JDBC
	 * ResultSet object will also have the CONCUR_UPDATABLE concurrency. If
	 * lanugage resultset is not updatable, then JDBC ResultSet object
	 * concurrency will be set to CONCUR_READ_ONLY.
	 * 
	 * @return the concurrency type, CONCUR_READ_ONLY, etc.
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public int getConcurrency() throws SQLException {
		return concurrencyOfThisResultSet;
	}

    //---------------------------------------------------------------------
	// Updates
	//---------------------------------------------------------------------

	/**
	 * JDBC 2.0
	 * 
	 * Determine if the current row has been updated. The value returned depends
	 * on whether or not the result set can detect updates.
	 * 
	 * @return true if the row has been visibly updated by the owner or another,
	 *         and updates are detected
	 * @exception SQLException
	 *                if a database-access error occurs
	 * 
	 * @see EmbedDatabaseMetaData#updatesAreDetected
	 */
	public boolean rowUpdated() throws SQLException {
		return false;
	}

	/**
	 * JDBC 2.0
	 * 
	 * Determine if the current row has been inserted. The value returned
	 * depends on whether or not the result set can detect visible inserts.
	 * 
	 * @return true if inserted and inserts are detected
	 * @exception SQLException
	 *                if a database-access error occurs
	 * 
	 * @see EmbedDatabaseMetaData#insertsAreDetected
	 */
	public boolean rowInserted() throws SQLException {
		throw Util.notImplemented();
	}

	/**
	 * JDBC 2.0
	 *
	 * Determine if this row has been deleted. A deleted row may leave a visible
	 * "hole" in a result set. This method can be used to detect holes in a
	 * result set. The value returned depends on whether or not the result set
	 * can detect deletions.
	 *
	 * @return true if deleted and deletes are detected
	 * @exception SQLException
	 *                if a database-access error occurs
	 *
	 * @see EmbedDatabaseMetaData#deletesAreDetected
	 */
	public boolean rowDeleted() throws SQLException {
		return false;
	}

	//do following few checks before accepting updateXXX resultset api
	protected void checksBeforeUpdateXXX(String methodName, int columnIndex) throws SQLException {
      checksBeforeUpdateOrDelete(methodName, columnIndex);

      //1)Make sure for updateXXX methods, the column position is not out of range
      ResultDescription rd = theResults.getResultDescription();
      if (columnIndex < 1 || columnIndex > rd.getColumnCount())
        throw Util.generateCsSQLException(SQLState.LANG_INVALID_COLUMN_POSITION,
					new Integer(columnIndex), String.valueOf(rd.getColumnCount()));

      //2)Make sure the column corresponds to a column in the base table and it is not a derived column
      if (rd.getColumnDescriptor(columnIndex).getSourceTableName() == null)
        throw Util.generateCsSQLException(SQLState.COLUMN_NOT_FROM_BASE_TABLE,
					methodName);

      //3)If column not updatable then throw an exception
      if (!getMetaData().isWritable(columnIndex))
        throw Util.generateCsSQLException(SQLState.LANG_COLUMN_NOT_UPDATABLE_IN_CURSOR,
					theResults.getResultDescription().getColumnDescriptor(columnIndex).getName(),
					getCursorName());
	}

	//do following few checks before accepting updatable resultset api
	//1)Make sure this is an updatable ResultSet
	//2)Make sure JDBC ResultSet is not closed
	//3)Make sure JDBC ResultSet is positioned on a row
	//4)Make sure underneath language resultset is not closed
	protected void checksBeforeUpdateOrDelete(String methodName, int columnIndex) throws SQLException {

      //1)Make sure this is an updatable ResultSet
      if (getConcurrency() != JDBC20Translation.CONCUR_UPDATABLE)//if not updatable resultset, then throw exception
        throw Util.generateCsSQLException(SQLState.UPDATABLE_RESULTSET_API_DISALLOWED, methodName);

      //2)Make sure JDBC ResultSet is not closed
      checkIfClosed(methodName);

      //3)Make sure JDBC ResultSet is positioned on a row
      checkOnRow(); // first make sure there's a current row
      //in case of autocommit on, if there was an exception which caused runtime rollback in this transaction prior to this call,
      //the rollback code will mark the language resultset closed (it doesn't mark the JDBC ResultSet closed).
      //That is why alongwith the earlier checkIfClosed call in this method, there is a check for language resultset close as well.

      //4)Make sure underneath language resultset is not closed
      if (theResults.isClosed())
        throw Util.generateCsSQLException(SQLState.LANG_RESULT_SET_NOT_OPEN, methodName);
	}

	//mark the column as updated and return DataValueDescriptor for it. It will be used by updateXXX methods to put new values
	protected DataValueDescriptor getDVDforColumnToBeUpdated(int columnIndex, String updateMethodName) throws StandardException, SQLException {
      checksBeforeUpdateXXX(updateMethodName, columnIndex);
      if (columnGotUpdated[columnIndex-1] == false) {//this is the first updateXXX call on this column
        //this is the first updateXXX method call on this column. Save the original content of the column into copyOfDatabaseRow
        //The saved copy of the column will be needed if cancelRowUpdates is issued
        copyOfDatabaseRow[columnIndex - 1] = currentRow.getColumn(columnIndex).getClone();
      }
      columnGotUpdated[columnIndex-1] = true;
	    currentRowHasBeenUpdated = true;
      return currentRow.getColumn(columnIndex);
	}

	/**
	 * JDBC 2.0
	 * 
	 * Give a nullable column a null value.
	 * 
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateNull(int columnIndex) throws SQLException {
		try {
			getDVDforColumnToBeUpdated(columnIndex, "updateNull").setToNull();
		} catch (StandardException t) {
			throw noStateChangeException(t);
		}
	}

	/**
	 * JDBC 2.0
	 *
	 * Update a column with a boolean value.
	 *
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 *
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateBoolean(int columnIndex, boolean x) throws SQLException {
		try {
			getDVDforColumnToBeUpdated(columnIndex, "updateBoolean").setValue(x);
		} catch (StandardException t) {
			throw noStateChangeException(t);
		}
	}

	/**
	 * JDBC 2.0
	 *
	 * Update a column with a byte value.
	 *
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 *
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateByte(int columnIndex, byte x) throws SQLException {
		try {
			getDVDforColumnToBeUpdated(columnIndex, "updateByte").setValue(x);
		} catch (StandardException t) {
			throw noStateChangeException(t);
		}
	}

	/**
	 * JDBC 2.0
	 *
	 * Update a column with a short value.
	 *
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 *
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateShort(int columnIndex, short x) throws SQLException {
		try {
			getDVDforColumnToBeUpdated(columnIndex, "updateShort").setValue(x);
		} catch (StandardException t) {
			throw noStateChangeException(t);
		}
	}

	/**
	 * JDBC 2.0
	 *
	 * Update a column with an integer value.
	 *
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 *
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateInt(int columnIndex, int x) throws SQLException {
		try {
			getDVDforColumnToBeUpdated(columnIndex, "updateInt").setValue(x);
		} catch (StandardException t) {
			throw noStateChangeException(t);
		}
	}

	/**
	 * JDBC 2.0
	 *
	 * Update a column with a long value.
	 *
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 *
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateLong(int columnIndex, long x) throws SQLException {
		try {
			getDVDforColumnToBeUpdated(columnIndex, "updateLong").setValue(x);
		} catch (StandardException t) {
			throw noStateChangeException(t);
		}
	}

	/**
	 * JDBC 2.0
	 *
	 * Update a column with a float value.
	 *
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 *
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateFloat(int columnIndex, float x) throws SQLException {
		try {
			getDVDforColumnToBeUpdated(columnIndex, "updateFloat").setValue(x);
		} catch (StandardException t) {
			throw noStateChangeException(t);
		}
	}

	/**
	 * JDBC 2.0
	 *
	 * Update a column with a Double value.
	 *
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateDouble(int columnIndex, double x) throws SQLException {
		try {
			getDVDforColumnToBeUpdated(columnIndex, "updateDouble").setValue(x);
		} catch (StandardException t) {
			throw noStateChangeException(t);
		}
	}

	/**
	 * JDBC 2.0
	 * 
	 * Update a column with a String value.
	 * 
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateString(int columnIndex, String x) throws SQLException {
		try {
			getDVDforColumnToBeUpdated(columnIndex, "updateString").setValue(x);
		} catch (StandardException t) {
			throw noStateChangeException(t);
		}
	}

	/**
	 * JDBC 2.0
	 * 
	 * Update a column with a byte array value.
	 * 
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateBytes(int columnIndex, byte x[]) throws SQLException {
		try {
			getDVDforColumnToBeUpdated(columnIndex, "updateBytes").setValue(x);
		} catch (StandardException t) {
			throw noStateChangeException(t);
		}
	}

	/**
	 * JDBC 2.0
	 *
	 * Update a column with a Date value.
	 *
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 *
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateDate(int columnIndex, java.sql.Date x)
			throws SQLException {
		try {
			getDVDforColumnToBeUpdated(columnIndex, "updateDate").setValue(x);
		} catch (StandardException t) {
			throw noStateChangeException(t);
		}
	}

	/**
	 * JDBC 2.0
	 *
	 * Update a column with a Time value.
	 *
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 *
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateTime(int columnIndex, java.sql.Time x)
			throws SQLException {
		try {
			getDVDforColumnToBeUpdated(columnIndex, "updateTime").setValue(x);
		} catch (StandardException t) {
			throw noStateChangeException(t);
		}
	}

	/**
	 * JDBC 2.0
	 *
	 * Update a column with a Timestamp value.
	 *
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 *
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateTimestamp(int columnIndex, java.sql.Timestamp x)
			throws SQLException {
		try {
			getDVDforColumnToBeUpdated(columnIndex, "updateTimestamp").setValue(x);
		} catch (StandardException t) {
			throw noStateChangeException(t);
		}
	}

	/**
	 * JDBC 2.0
	 *
	 * Update a column with an ascii stream value.
	 *
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 *
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 * @param length
	 *            the length of the stream
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateAsciiStream(int columnIndex, java.io.InputStream x,
			int length) throws SQLException {
		checksBeforeUpdateXXX("updateAsciiStream", columnIndex);

		int colType = getColumnType(columnIndex);
		switch (colType) {
			case Types.CHAR:
			case Types.VARCHAR:
			case Types.LONGVARCHAR:
			case Types.CLOB:
				break;
			default:
				throw dataTypeConversion(columnIndex, "java.io.InputStream");
		}

		java.io.Reader r = null;
		if (x != null)
		{
			try {
				r = new java.io.InputStreamReader(x, "ISO-8859-1");
			} catch (java.io.UnsupportedEncodingException uee) {
				throw new SQLException(uee.getMessage());
			}
		}
		updateCharacterStream(columnIndex, r, length);
	}

	/**
	 * JDBC 2.0
	 *
	 * Update a column with a binary stream value.
	 *
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 *
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 * @param length
	 *            the length of the stream
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateBinaryStream(int columnIndex, java.io.InputStream x,
			int length) throws SQLException {
		checksBeforeUpdateXXX("updateBinaryStream", columnIndex);
		int colType = getColumnType(columnIndex);
		switch (colType) {
			case Types.BINARY:
			case Types.VARBINARY:
			case Types.LONGVARBINARY:
			case Types.BLOB:
				break;
			default:
				throw dataTypeConversion(columnIndex, "java.io.InputStream");
		}
		if (length < 0) //we are doing the check here and not in updateBinaryStreamInternal becuase updateClob needs to pass -1 for length.
			throw newSQLException(SQLState.NEGATIVE_STREAM_LENGTH);

		if (x == null)
		{
			updateNull(columnIndex);
			return;
		}

		updateBinaryStreamInternal(columnIndex, x, length,"updateBinaryStream");
	}

	protected void updateBinaryStreamInternal(int columnIndex,
						java.io.InputStream x, int length, String updateMethodName)
	    throws SQLException
	{
		try {
			getDVDforColumnToBeUpdated(columnIndex, updateMethodName).setValue(new RawToBinaryFormatStream(x, length), length);
		} catch (StandardException t) {
			throw noStateChangeException(t);
		}
	}

	/**
	 * JDBC 2.0
	 * 
	 * Update a column with a character stream value.
	 * 
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 * 
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 * @param length
	 *            the length of the stream
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateCharacterStream(int columnIndex, java.io.Reader x,
			int length) throws SQLException {
		//If the column type is the right datatype, this method will eventually call getDVDforColumnToBeUpdated which will check for
		//the read only resultset. But for other datatypes, we want to catch if this updateCharacterStream is being issued
		//against a read only resultset. And that is the reason for call to checksBeforeUpdateXXX here.
		checksBeforeUpdateXXX("updateCharacterStream", columnIndex);
		int colType = getColumnType(columnIndex);
		switch (colType) {
			case Types.CHAR:
			case Types.VARCHAR:
			case Types.LONGVARCHAR:
			case Types.CLOB:
				break;
			default:
				throw dataTypeConversion(columnIndex, "java.io.Reader");
		}
		if (length < 0) //we are doing the check here and not in updateCharacterStreamInternal becuase updateClob needs to pass -1 for length.
			throw newSQLException(SQLState.NEGATIVE_STREAM_LENGTH);

		if (x == null)
		{
			updateNull(columnIndex);
			return;
		}
		updateCharacterStreamInternal(columnIndex, x, length, "updateCharacterStream");
	}

    protected void updateCharacterStreamInternal(int columnIndex,
						java.io.Reader reader, int length, String updateMethodName)
	    throws SQLException
	{
		try {
			LimitReader limitIn = new LimitReader(reader);
			if (length != -1)
				limitIn.setLimit(length);
			ReaderToUTF8Stream utfIn = new ReaderToUTF8Stream(limitIn);
			getDVDforColumnToBeUpdated(columnIndex, updateMethodName).setValue(utfIn, length);
		} catch (StandardException t) {
			throw noStateChangeException(t);
		}
	}

	/**
	 * JDBC 2.0
	 *
	 * Update a column with an Object value.
	 *
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 *
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 * @param scale
	 *            For java.sql.Types.DECIMAL or java.sql.Types.NUMERIC types
	 *            this is the number of digits after the decimal. For all other
	 *            types this value will be ignored.
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateObject(int columnIndex, Object x, int scale)
			throws SQLException {
		updateObject(columnIndex, x);
		/*
		* If the parameter type is DECIMAL or NUMERIC, then
		* we need to set them to the passed scale.
		*/
		int colType = getColumnType(columnIndex);
		if ((colType == Types.DECIMAL) || (colType == Types.NUMERIC)) {
			if (scale < 0)
				throw newSQLException(SQLState.BAD_SCALE_VALUE, new Integer(scale));

			try {
				DataValueDescriptor value = currentRow.getColumn(columnIndex);

				int origvaluelen = value.getLength();
				((VariableSizeDataValue)
						value).setWidth(VariableSizeDataValue.IGNORE_PRECISION,
							scale,
							false);

			} catch (StandardException t) {
				throw EmbedResultSet.noStateChangeException(t);
			}
		}
	}

	/**
	 * JDBC 2.0
	 *
	 * Update a column with an Object value.
	 *
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 *
	 * @param columnIndex
	 *            the first column is 1, the second is 2, ...
	 * @param x
	 *            the new column value
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateObject(int columnIndex, Object x) throws SQLException {
		checksBeforeUpdateXXX("updateObject", columnIndex);
		int colType = getColumnType(columnIndex);
		if (colType == org.apache.derby.iapi.reference.JDBC20Translation.SQL_TYPES_JAVA_OBJECT) {
			try {
				getDVDforColumnToBeUpdated(columnIndex, "updateObject").setValue(x);
				return;
			} catch (StandardException t) {
				throw noStateChangeException(t);
			}
		}

		if (x == null) {
			updateNull(columnIndex);
			return;
		}

		if (x instanceof String) {
			updateString(columnIndex, (String) x);
			return;
		}

		if (x instanceof Boolean) {
			updateBoolean(columnIndex, ((Boolean) x).booleanValue());
			return;
		}

		if (x instanceof Short) {
			updateShort(columnIndex, ((Short) x).shortValue());
			return;
		}

		if (x instanceof Integer) {
			updateInt(columnIndex, ((Integer) x).intValue());
			return;
		}

		if (x instanceof Long) {
			updateLong(columnIndex, ((Long) x).longValue());
			return;
		}

		if (x instanceof Float) {
			updateFloat(columnIndex, ((Float) x).floatValue());
			return;
		}

		if (x instanceof Double) {
			updateDouble(columnIndex, ((Double) x).doubleValue());
			return;
		}

		if (x instanceof byte[]) {
			updateBytes(columnIndex, (byte[]) x);
			return;
		}

		if (x instanceof Date) {
			updateDate(columnIndex, (Date) x);
			return;
		}

		if (x instanceof Time) {
			updateTime(columnIndex, (Time) x);
			return;
		}

		if (x instanceof Timestamp) {
			updateTimestamp(columnIndex, (Timestamp) x);
			return;
		}

		if (x instanceof Blob) {
			updateBlob(columnIndex, (Blob) x);
			return;
		}

		if (x instanceof Clob) {
			updateClob(columnIndex, (Clob) x);
			return;
		}

		throw dataTypeConversion(columnIndex, x.getClass().getName());
	}

	/**
	 * JDBC 2.0
	 * 
	 * Update a column with a null value.
	 * 
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 * 
	 * @param columnName
	 *            the name of the column
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateNull(String columnName) throws SQLException {
		updateNull(findColumnName(columnName));
	}

	/**
	 * JDBC 2.0
	 * 
	 * Update a column with a boolean value.
	 * 
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 * 
	 * @param columnName
	 *            the name of the column
	 * @param x
	 *            the new column value
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateBoolean(String columnName, boolean x) throws SQLException {
		updateBoolean(findColumnName(columnName), x);
	}

	/**
	 * JDBC 2.0
	 * 
	 * Update a column with a byte value.
	 * 
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 * 
	 * @param columnName
	 *            the name of the column
	 * @param x
	 *            the new column value
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateByte(String columnName, byte x) throws SQLException {
		updateByte(findColumnName(columnName), x);
	}

	/**
	 * JDBC 2.0
	 * 
	 * Update a column with a short value.
	 * 
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 * 
	 * @param columnName
	 *            the name of the column
	 * @param x
	 *            the new column value
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateShort(String columnName, short x) throws SQLException {
		updateShort(findColumnName(columnName), x);
	}

	/**
	 * JDBC 2.0
	 * 
	 * Update a column with an integer value.
	 * 
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 * 
	 * @param columnName
	 *            the name of the column
	 * @param x
	 *            the new column value
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateInt(String columnName, int x) throws SQLException {
		updateInt(findColumnName(columnName), x);
	}

	/**
	 * JDBC 2.0
	 * 
	 * Update a column with a long value.
	 * 
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 * 
	 * @param columnName
	 *            the name of the column
	 * @param x
	 *            the new column value
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateLong(String columnName, long x) throws SQLException {
		updateLong(findColumnName(columnName), x);
	}

	/**
	 * JDBC 2.0
	 * 
	 * Update a column with a float value.
	 * 
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 * 
	 * @param columnName
	 *            the name of the column
	 * @param x
	 *            the new column value
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateFloat(String columnName, float x) throws SQLException {
		updateFloat(findColumnName(columnName), x);
	}

	/**
	 * JDBC 2.0
	 * 
	 * Update a column with a double value.
	 * 
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 * 
	 * @param columnName
	 *            the name of the column
	 * @param x
	 *            the new column value
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateDouble(String columnName, double x) throws SQLException {
		updateDouble(findColumnName(columnName), x);
	}

	/**
	 * JDBC 2.0
	 * 
	 * Update a column with a String value.
	 * 
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 * 
	 * @param columnName
	 *            the name of the column
	 * @param x
	 *            the new column value
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateString(String columnName, String x) throws SQLException {
		updateString(findColumnName(columnName), x);
	}

	/**
	 * JDBC 2.0
	 * 
	 * Update a column with a byte array value.
	 * 
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 * 
	 * @param columnName
	 *            the name of the column
	 * @param x
	 *            the new column value
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateBytes(String columnName, byte x[]) throws SQLException {
		updateBytes(findColumnName(columnName), x);
	}

	/**
	 * JDBC 2.0
	 * 
	 * Update a column with a Date value.
	 * 
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 * 
	 * @param columnName
	 *            the name of the column
	 * @param x
	 *            the new column value
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateDate(String columnName, java.sql.Date x)
			throws SQLException {
		updateDate(findColumnName(columnName), x);
	}

	/**
	 * JDBC 2.0
	 * 
	 * Update a column with a Time value.
	 * 
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 * 
	 * @param columnName
	 *            the name of the column
	 * @param x
	 *            the new column value
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateTime(String columnName, java.sql.Time x)
			throws SQLException {
		updateTime(findColumnName(columnName), x);
	}

	/**
	 * JDBC 2.0
	 * 
	 * Update a column with a Timestamp value.
	 * 
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 * 
	 * @param columnName
	 *            the name of the column
	 * @param x
	 *            the new column value
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateTimestamp(String columnName, java.sql.Timestamp x)
			throws SQLException {
		updateTimestamp(findColumnName(columnName), x);
	}

	/**
	 * JDBC 2.0
	 * 
	 * Update a column with an ascii stream value.
	 * 
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 * 
	 * @param columnName
	 *            the name of the column
	 * @param x
	 *            the new column value
	 * @param length
	 *            of the stream
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateAsciiStream(String columnName, java.io.InputStream x,
			int length) throws SQLException {
		updateAsciiStream(findColumnName(columnName), x, length);
	}

	/**
	 * JDBC 2.0
	 * 
	 * Update a column with a binary stream value.
	 * 
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 * 
	 * @param columnName
	 *            the name of the column
	 * @param x
	 *            the new column value
	 * @param length
	 *            of the stream
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateBinaryStream(String columnName, java.io.InputStream x,
			int length) throws SQLException {
		updateBinaryStream(findColumnName(columnName), x, length);
	}

	/**
	 * JDBC 2.0
	 * 
	 * Update a column with a character stream value.
	 * 
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 * 
	 * @param columnName
	 *            the name of the column
	 * @param x
	 *            the new column value
	 * @param length
	 *            of the stream
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateCharacterStream(String columnName, java.io.Reader reader,
			int length) throws SQLException {
		updateCharacterStream(findColumnName(columnName), reader, length);
	}

	/**
	 * JDBC 2.0
	 *
	 * Update a column with an Object value.
	 *
	 * The updateXXX() methods are used to update column values in the
	 * current row, or the insert row.  The updateXXX() methods do not
	 * update the underlying database, instead the updateRow() or insertRow()
	 * methods are called to update the database.
	 *
	 * @param columnName the name of the column
	 * @param x the new column value
	 * @param scale For java.sql.Types.DECIMAL or java.sql.Types.NUMERIC types
	 *  this is the number of digits after the decimal.  For all other
	 *  types this value will be ignored.
	 * @exception SQLException if a database-access error occurs
	 */
	public void updateObject(String columnName, Object x, int scale)
      throws SQLException {
		updateObject(findColumnName(columnName), x, scale);
	}

	/**
	 * JDBC 2.0
	 *
	 * Update a column with an Object value.
	 *
	 * The updateXXX() methods are used to update column values in the current
	 * row, or the insert row. The updateXXX() methods do not update the
	 * underlying database, instead the updateRow() or insertRow() methods are
	 * called to update the database.
	 *
	 * @param columnName
	 *            the name of the column
	 * @param x
	 *            the new column value
	 * @exception SQLException
	 *                if a database-access error occurs
	 */
	public void updateObject(String columnName, Object x) throws SQLException {
		updateObject(findColumnName(columnName), x);
	}

	/**
	 * JDBC 2.0
	 * 
	 * Insert the contents of the insert row into the result set and the
	 * database. Must be on the insert row when this method is called.
	 * 
	 * @exception SQLException
	 *                if a database-access error occurs, if called when not on
	 *                the insert row, or if all non-nullable columns in the
	 *                insert row have not been given a value
	 */
	public void insertRow() throws SQLException {
		throw Util.notImplemented();
	}

    /**
     * JDBC 2.0
     *
     * Update the underlying database with the new contents of the
     * current row.  Cannot be called when on the insert row.
     *
     * @exception SQLException if a database-access error occurs, or
     * if called when on the insert row
     */
    public void updateRow() throws SQLException {
			synchronized (getConnectionSynchronization()) {
        checksBeforeUpdateOrDelete("updateRow", -1);
        setupContextStack();
        LanguageConnectionContext lcc = null;
        StatementContext statementContext = null;
        try {
            if (currentRowHasBeenUpdated == false) //nothing got updated on this row 
                return; //nothing to do since no updates were made to this row

            //now construct the update where current of sql
            boolean foundOneColumnAlready = false;
            StringBuffer updateWhereCurrentOfSQL = new StringBuffer("UPDATE ");
            CursorActivation activation = getEmbedConnection().getLanguageConnection().lookupCursorActivation(getCursorName());


            ExecCursorTableReference targetTable = activation.getPreparedStatement().getTargetTable();
            updateWhereCurrentOfSQL.append(getFullBaseTableName(targetTable));//got the underlying (schema.)table name
            updateWhereCurrentOfSQL.append(" SET ");
            ResultDescription rd = theResults.getResultDescription();

            for (int i=1; i<=rd.getColumnCount(); i++) { //in this for loop we are constructing columnname=?,... part of the update sql
                if (columnGotUpdated[i-1]) { //if the column got updated, do following
                    if (foundOneColumnAlready)
                        updateWhereCurrentOfSQL.append(",");
                    //using quotes around the column name to preserve case sensitivity
                    updateWhereCurrentOfSQL.append("\"" + rd.getColumnDescriptor(i).getName() + "\"=?");
                    foundOneColumnAlready = true;
                }
            }
            //using quotes around the cursor name to preserve case sensitivity
            updateWhereCurrentOfSQL.append(" WHERE CURRENT OF \"" + getCursorName() + "\"");
            lcc = getEmbedConnection().getLanguageConnection();
            statementContext = lcc.pushStatementContext(isAtomic, updateWhereCurrentOfSQL.toString(), null, false);
            org.apache.derby.iapi.sql.PreparedStatement ps = lcc.prepareInternalStatement(updateWhereCurrentOfSQL.toString());
            Activation act = ps.getActivation(lcc, false);

            //in this for loop we are assigning values for parameters in sql constructed earlier with columnname=?,... 
            for (int i=1, paramPosition=0; i<=rd.getColumnCount(); i++) { 
                if (columnGotUpdated[i-1])  //if the column got updated, do following
                    act.getParameterValueSet().getParameterForSet(paramPosition++).setValue(currentRow.getColumn(i));
            }
            org.apache.derby.iapi.sql.ResultSet rs = ps.execute(act, false, true, true); //execute the update where current of sql
            rs.close();
            rs.finish();
            //For forward only resultsets, after a update, the ResultSet will be positioned right before the next row.
            rowData = null;
            currentRow = null;
            lcc.popStatementContext(statementContext, null);
        } catch (StandardException t) {
            throw closeOnTransactionError(t);
        } finally {
            if (statementContext != null)
                lcc.popStatementContext(statementContext, null);
            restoreContextStack();
        }
			}
    }

    /**
     * JDBC 2.0
     *
     * Delete the current row from the result set and the underlying
     * database.  Cannot be called when on the insert row.
     *
     * @exception SQLException if a database-access error occurs, or if
     * called when on the insert row.
     */
    public void deleteRow() throws SQLException {
        synchronized (getConnectionSynchronization()) {
            checksBeforeUpdateOrDelete("deleteRow", -1);

            setupContextStack();
            //now construct the delete where current of sql
            try {
                StringBuffer deleteWhereCurrentOfSQL = new StringBuffer("DELETE FROM ");
                CursorActivation activation = getEmbedConnection().getLanguageConnection().lookupCursorActivation(getCursorName());
                deleteWhereCurrentOfSQL.append(getFullBaseTableName(activation.getPreparedStatement().getTargetTable()));//get the underlying (schema.)table name
                //using quotes around the cursor name to preserve case sensitivity
                deleteWhereCurrentOfSQL.append(" WHERE CURRENT OF \"" + getCursorName() + "\"");

                LanguageConnectionContext lcc = getEmbedConnection().getLanguageConnection();
                StatementContext statementContext = lcc.pushStatementContext(isAtomic, deleteWhereCurrentOfSQL.toString(), null, false);
                org.apache.derby.iapi.sql.PreparedStatement ps = lcc.prepareInternalStatement(deleteWhereCurrentOfSQL.toString());
                org.apache.derby.iapi.sql.ResultSet rs = ps.execute(lcc, true); //execute delete where current of sql
                rs.close();
                rs.finish();
                //For forward only resultsets, after a delete, the ResultSet will be positioned right before the next row.
                rowData = null;
                currentRow = null;
                lcc.popStatementContext(statementContext, null);
            } catch (StandardException t) {
                    throw closeOnTransactionError(t);
            } finally {
                restoreContextStack();
            }
        }
    }

	private String getFullBaseTableName(ExecCursorTableReference targetTable) {
		//using quotes to preserve case sensitivity
		if (targetTable.getSchemaName() != null)
			return "\"" + targetTable.getSchemaName() + "\".\""
					+ targetTable.getBaseName() + "\"";
		else
			return "\"" + targetTable.getBaseName() + "\"";
	}

	/**
	 * JDBC 2.0
	 * 
	 * Refresh the value of the current row with its current value in the
	 * database. Cannot be called when on the insert row.
	 * 
	 * The refreshRow() method provides a way for an application to explicitly
	 * tell the JDBC driver to refetch a row(s) from the database. An
	 * application may want to call refreshRow() when caching or prefetching is
	 * being done by the JDBC driver to fetch the latest value of a row from the
	 * database. The JDBC driver may actually refresh multiple rows at once if
	 * the fetch size is greater than one.
	 * 
	 * All values are refetched subject to the transaction isolation level and
	 * cursor sensitivity. If refreshRow() is called after calling updateXXX(),
	 * but before calling updateRow() then the updates made to the row are lost.
	 * Calling refreshRow() frequently will likely slow performance.
	 * 
	 * @exception SQLException
	 *                if a database-access error occurs, or if called when on
	 *                the insert row.
	 */
	public void refreshRow() throws SQLException {
		throw Util.notImplemented();
	}

    /**
     * JDBC 2.0
     *
     * The cancelRowUpdates() method may be called after calling an
     * updateXXX() method(s) and before calling updateRow() to rollback 
     * the updates made to a row.  If no updates have been made or 
     * updateRow() has already been called, then this method has no 
     * effect.
     *
     * @exception SQLException if a database-access error occurs, or if
     * called when on the insert row.
     *
     */
    public void cancelRowUpdates () throws SQLException {
        checksBeforeUpdateOrDelete("cancelRowUpdates", -1);
        if (currentRowHasBeenUpdated == false) return; //nothing got updated on this row so cancelRowUpdates is a no-op in this case.

        for (int i=0; i < columnGotUpdated.length; i++){
            if (columnGotUpdated[i] == true) currentRow.setColumn(i+1, copyOfDatabaseRow[i]);//if column got updated, resotre the original data
            columnGotUpdated[i] = false;
        }
        currentRowHasBeenUpdated = false;
        //rowData needs to be refreshed with the currentRow otherwise it will continue to have changes made by updateXXX methods
        rowData = currentRow.getRowArray();
        }

	/**
	 * JDBC 2.0
	 * 
	 * Move to the insert row. The current cursor position is remembered while
	 * the cursor is positioned on the insert row.
	 * 
	 * The insert row is a special row associated with an updatable result set.
	 * It is essentially a buffer where a new row may be constructed by calling
	 * the updateXXX() methods prior to inserting the row into the result set.
	 * 
	 * Only the updateXXX(), getXXX(), and insertRow() methods may be called
	 * when the cursor is on the insert row. All of the columns in a result set
	 * must be given a value each time this method is called before calling
	 * insertRow(). UpdateXXX()must be called before getXXX() on a column.
	 * 
	 * @exception SQLException
	 *                if a database-access error occurs, or the result set is
	 *                not updatable
	 */
	public void moveToInsertRow() throws SQLException {
		throw Util.notImplemented();
	}

	/**
	 * JDBC 2.0
	 * 
	 * Move the cursor to the remembered cursor position, usually the current
	 * row. Has no effect unless the cursor is on the insert row.
	 * 
	 * @exception SQLException
	 *                if a database-access error occurs, or the result set is
	 *                not updatable
	 */
	public void moveToCurrentRow() throws SQLException {
		throw Util.notImplemented();
	}

    /**
	 * JDBC 2.0
	 * 
	 * Get a BLOB column.
	 * 
	 * @param i
	 *            the first column is 1, the second is 2, ...
	 * @return an object representing a BLOB
	 */
	public Blob getBlob(int columnIndex) throws SQLException {

		closeCurrentStream(); // closing currentStream does not depend on the
		// underlying connection. Do this outside of
		// the connection synchronization.

		checkIfClosed("getBlob"); // checking result set closure does not depend
		// on the underlying connection. Do this
		// outside of the connection synchronization.

		synchronized (getConnectionSynchronization()) {
			int colType = getColumnType(columnIndex);

			// DB2, only allow getBlob on a BLOB column.
			if (colType != Types.BLOB)
				throw dataTypeConversion("java.sql.Blob", columnIndex);

			boolean pushStack = false;
			try {
				DataValueDescriptor dvd = currentRow.getColumn(columnIndex);

				if (wasNull = dvd.isNull())
					return null;

				// should set up a context stack if we have a long column,
				// since a blob may keep a pointer to a long column in the
				// database
				if (dvd.getStream() != null)
					pushStack = true;

				if (pushStack)
					setupContextStack();

				return new EmbedBlob(dvd, getEmbedConnection());
			} catch (Throwable t) {
				throw handleException(t);
			} finally {
				if (pushStack)
					restoreContextStack();
			}
		}
	}

	/**
	 * JDBC 2.0
	 * 
	 * Get a CLOB column.
	 * 
	 * @param i
	 *            the first column is 1, the second is 2, ...
	 * @return an object representing a CLOB
	 */
	public final Clob getClob(int columnIndex) throws SQLException {

		closeCurrentStream(); // closing currentStream does not depend on the
		// underlying connection. Do this outside of
		// the connection synchronization.

		checkIfClosed("getClob"); // checking result set closure does not depend
		// on the underlying connection. Do this
		// outside of the connection synchronization.

		synchronized (getConnectionSynchronization()) {
			int colType = getColumnType(columnIndex);

			// DB2:, only allow getClob on a CLOB column.
			if (colType != Types.CLOB)
				throw dataTypeConversion("java.sql.Clob", columnIndex);

			boolean pushStack = false;
			try {

				DataValueDescriptor dvd = currentRow.getColumn(columnIndex);

				if (wasNull = dvd.isNull())
					return null;

				// should set up a context stack if we have a long column,
				// since a blob may keep a pointer to a long column in the
				// database
				if (dvd.getStream() != null)
					pushStack = true;

				if (pushStack)
					setupContextStack();

				return new EmbedClob(dvd, getEmbedConnection());
			} catch (Throwable t) {
				throw handleException(t);
			} finally {
				if (pushStack)
					restoreContextStack();
			}
		}
	}
	
    /**
	 * JDBC 2.0
	 * 
	 * Get a BLOB column.
	 * 
	 * @param colName
	 *            the column name
	 * @return an object representing a BLOB
	 */
	public final Blob getBlob(String columnName) throws SQLException {
		return (getBlob(findColumnName(columnName)));
	}

	/**
	 * JDBC 2.0
	 * 
	 * Get a CLOB column.
	 * 
	 * @param colName
	 *            the column name
	 * @return an object representing a CLOB
	 * @exception SQLException
	 *                Feature not implemented for now.
	 */
	public final Clob getClob(String columnName) throws SQLException {
		return (getClob(findColumnName(columnName)));
	}	

	
    /**
	 * JDBC 3.0
	 * 
	 * Updates the designated column with a java.sql.Blob value. The updater
	 * methods are used to update column values in the current row or the insert
	 * row. The updater methods do not update the underlying database; instead
	 * the updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnIndex -
	 *            the first column is 1, the second is 2
	 * @param x -
	 *            the new column value
	 * @exception SQLException
	 *                Feature not implemented for now.
	 */
	public void updateBlob(int columnIndex, Blob x) throws SQLException {
        checksBeforeUpdateXXX("updateBlob", columnIndex);
        int colType = getColumnType(columnIndex);
        if (colType != Types.BLOB)
            throw dataTypeConversion(columnIndex, "java.sql.Blob");

        if (x == null)
            updateNull(columnIndex);
        else
            updateBinaryStreamInternal(columnIndex, x.getBinaryStream(), -1, "updateBlob");
	}

	/**
	 * JDBC 3.0
	 * 
	 * Updates the designated column with a java.sql.Blob value. The updater
	 * methods are used to update column values in the current row or the insert
	 * row. The updater methods do not update the underlying database; instead
	 * the updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnName -
	 *            the SQL name of the column
	 * @param x -
	 *            the new column value
	 * @exception SQLException
	 *                Feature not implemented for now.
	 */
	public void updateBlob(String columnName, Blob x) throws SQLException {
		updateBlob(findColumnName(columnName), x);
	}

	/**
	 * JDBC 3.0
	 * 
	 * Updates the designated column with a java.sql.Clob value. The updater
	 * methods are used to update column values in the current row or the insert
	 * row. The updater methods do not update the underlying database; instead
	 * the updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnIndex -
	 *            the first column is 1, the second is 2
	 * @param x -
	 *            the new column value
	 * @exception SQLException
	 *                Feature not implemented for now.
	 */
	public void updateClob(int columnIndex, Clob x) throws SQLException {
        checksBeforeUpdateXXX("updateClob", columnIndex);
        int colType = getColumnType(columnIndex);
        if (colType != Types.CLOB)
            throw dataTypeConversion(columnIndex, "java.sql.Clob");

        if (x == null)
            updateNull(columnIndex);
        else
            updateCharacterStreamInternal(columnIndex, x.getCharacterStream(), -1, "updateClob");
	}

	/**
	 * JDBC 3.0
	 * 
	 * Updates the designated column with a java.sql.Clob value. The updater
	 * methods are used to update column values in the current row or the insert
	 * row. The updater methods do not update the underlying database; instead
	 * the updateRow or insertRow methods are called to update the database.
	 * 
	 * @param columnName -
	 *            the SQL name of the column
	 * @param x -
	 *            the new column value
	 * @exception SQLException
	 *                Feature not implemented for now.
	 */
	public void updateClob(String columnName, Clob x) throws SQLException {
		updateClob(findColumnName(columnName), x);
	}
	
	
	/*
	 * * End of JDBC public methods.
	 */

    /**
	 * Map a Resultset column name to a ResultSet column index.
	 * 
	 * @param columnName
	 *            the name of the column
	 * @param operation
	 *            the operation the caller is trying to do (for error
	 *            reporting). Null means don't do error checking.
	 * @return the column index
	 * @exception SQLException
	 *                thrown on failure.
	 */
    protected int findColumnName(String columnName)
						throws SQLException {
		// n.b. if we went through the JDBC interface,
		// there is a caching implementation in the JDBC doc
		// (appendix C). But we go through our own info, for now.
		// REVISIT: we might want to cache our own info...
		

		if (columnName == null)
			throw newSQLException(SQLState.NULL_COLUMN_NAME);

		ResultDescription rd = resultDescription;

    	// 1 or 0 based? assume 1 (probably wrong)
        // Changing the order in which columns are found from 1 till column count.
        // This is necessary in cases where the column names are the same but are in different cases.
        // This is because in updateXXX and getXXX methods column names are case insensitive
        // and in that case the first column should be returned.
        
        int columnCount = rd.getColumnCount();

        for(int i = 1 ; i<= columnCount;i++) {
    		String name = rd.getColumnDescriptor(i).getName();
    		if (StringUtil.SQLEqualsIgnoreCase(columnName, name)) {
    			return i;
    		}
    	}
    	throw newSQLException(SQLState.COLUMN_NOT_FOUND, columnName);
	}



	//
	// methods to be overridden in subimplementations
	// that want to stay within their subimplementation.
	//
	protected EmbedResultSetMetaData newEmbedResultSetMetaData(ResultDescription resultDesc) {
		return new EmbedResultSetMetaData(resultDesc.getColumnInfo());
	}

	/**
	 * Documented behaviour for streams is that they are implicitly closed on
	 * the next get*() method call.
	 */
	protected final void closeCurrentStream() {

		if (currentStream != null) {
			try {
				// 99% of the time, the stream is already closed.
				synchronized(this)
				{
					if (currentStream != null) {
						if (currentStream instanceof java.io.Reader)
							((java.io.Reader) currentStream).close();
						else
							((java.io.InputStream) currentStream).close();
					}
				}
			} catch (IOException ioe) {
				// just ignore, caller has already read the data they require
			} finally {
				currentStream = null;
			}
		}
	}

	/**
	 * Throw an exception if this ResultSet is closed.
	 *
	 * @param operation		The operation the caller is trying to perform
	 *
	 * @exception SQLException		Thrown if this ResultSet is closed.
	 */
	protected final void checkIfClosed(String operation) throws SQLException {
		if (isClosed) {
			throw newSQLException(SQLState.LANG_RESULT_SET_NOT_OPEN, operation);
		}
	}

	protected final void checkExecIfClosed(String operation) throws SQLException {
		
		checkIfClosed(operation);

		java.sql.Connection appConn = getEmbedConnection().getApplicationConnection();

		if ((appConn == null) || appConn.isClosed())
			throw Util.noCurrentConnection();
	}

	/**
	 * Try to see if we can fish the SQL Statement out of the local statement.
	 * @return null if we cannot figure out what SQL Statement is currently
	 *  executing
	 */
	protected String getSQLText()
	{
		if (stmt == null)
			return null;

		return stmt.getSQLText();
	}

	/**
	 * Try to see if we can fish the pvs out of the local statement.
	 * @return null if we cannot figure out what parameter value set is currently
	 *  using
	 */
	protected ParameterValueSet getParameterValueSet()
	{
		if (stmt == null)
			return null;

		return stmt.getParameterValueSet();
	}

    private static boolean isMaxFieldSizeType(int colType){
        return (colType == Types.BINARY || colType == Types.VARBINARY || 
            colType == Types.LONGVARBINARY || colType == Types.CHAR ||
            colType == Types.VARCHAR || colType == Types.LONGVARCHAR);
    }
	/*
	 * close result set if we have a transaction level error 
	 */
	protected final SQLException closeOnTransactionError(Throwable thrownException) throws SQLException
	{
		SQLException sqle = handleException(thrownException);
		if (thrownException instanceof StandardException)
		{
			StandardException se = (StandardException) thrownException;
			int severity = se.getSeverity();
			if (severity == ExceptionSeverity.TRANSACTION_SEVERITY)
			{
				try {
					close();
	    		} catch (Throwable t) {
		        	SQLException top = handleException(t);
					top.setNextException(sqle);
					sqle = top;
				}
			}
		}

		return sqle;
	}


	/**
		Get the column value for a getXXX() call.
		This method:
		<UL>
		<LI> Closes the current stream (as per JDBC)
		<LI> Throws a SQLException if the result set is closed
		<LI> Throws a SQLException if the ResultSet is not on a row
		<LI> Throws a SQLException if the columnIndex is out of range
		<LI> Returns the DataValueDescriptor for the column.
		</UL>
	*/
	protected final DataValueDescriptor getColumn(int columnIndex)
		throws SQLException, StandardException {

	  closeCurrentStream();

	  checkIfClosed("getXXX");

	   DataValueDescriptor[] theCurrentRow = checkOnRow(); // first make sure there's a row
		
	   try {
		   return theCurrentRow[columnIndex - 1];
	   } catch (ArrayIndexOutOfBoundsException aoobe) {
			throw newSQLException(SQLState.COLUMN_NOT_FOUND, 
				                new Integer(columnIndex));
	   }

	   // return theCurrentRow.getColumn(columnIndex);
	}


	/**
		An exception on many method calls to JDBC objects does not change the state
		of the transaction or statement, or even the underlying object. This method
		simply wraps the excecption in a SQLException. Examples are:
		<UL>
		<LI> getXXX() calls on ResultSet - ResultSet is not closed.
		<LI> setXXX() calls on PreparedStatement - ResultSet is not closed.
		</UL>
		In addition these exceptions must not call higher level objects to
		be closed (e.g. when executing a server side Java procedure). See bug 4397

	*/
	public static final SQLException noStateChangeException(Throwable thrownException) {

		// Any exception on a setXXX/getXXX method does not close
		// the ResultSet or the Statement. So we only need
		// to convert the exception to a SQLException.

		return TransactionResourceImpl.wrapInSQLException((SQLException) null, thrownException);

	}

	/**
		A dynamic result set was created in a procedure by a nested connection.
		Once the procedure returns, there is a good chance that connection is closed,
		so we re-attach the result set to the connection of the statement the called
		the procedure, which will be still open.
	*/
	void setDynamicResultSet(EmbedStatement owningStmt) {

		this.owningStmt = owningStmt;
		this.localConn = owningStmt.getEmbedConnection();
	}

	/*
	** Comparable (for ordering dynamic result sets from procedures) 
	*/

	public final int compareTo(Object other) {

		EmbedResultSet olrs = (EmbedResultSet) other;

		return order - olrs.order;

	}
	
    private void checkScrollCursor(String methodName) throws SQLException {

		if (stmt.getResultSetType() == JDBC20Translation.TYPE_FORWARD_ONLY)
			throw Util
					.newEmbedSQLException(
							SQLState.NOT_ON_FORWARD_ONLY_CURSOR,
							new Object[] { methodName },
							StandardException
									.getSeverityFromIdentifier(SQLState.NOT_ON_FORWARD_ONLY_CURSOR));
	}
	private boolean checkRowPosition(int position, String positionText)
			throws SQLException {
		// beforeFirst is only allowed on scroll cursors
		checkScrollCursor(positionText);

		checkIfClosed(positionText); // checking result set closure does not
									 // depend
		// on the underlying connection. Do this
		// outside of the connection synchronization.

		synchronized (getConnectionSynchronization()) {
			setupContextStack();
			try {
				try {

					/*
					 * Push and pop a StatementContext around a next call so
					 * that the ResultSet will get correctly closed down on an
					 * error. (Cache the LanguageConnectionContext)
					 */
					LanguageConnectionContext lcc = getEmbedConnection()
							.getLanguageConnection();
					StatementContext statementContext = lcc
							.pushStatementContext(isAtomic, getSQLText(),
									getParameterValueSet(), false);

					boolean result = theResults.checkRowPosition(position);

					lcc.popStatementContext(statementContext, null);

					return result;

				} catch (Throwable t) {
					/*
					 * Need to close the result set here because the error might
					 * cause us to lose the current connection if this is an XA
					 * connection and we won't be able to do the close later
					 */
					throw closeOnTransactionError(t);
				}

			} finally {
				restoreContextStack();
			}
		}
	}
	/**
	 * * Is this result set from a select for update statement?
	 */
	public final boolean isForUpdate()
	{
		if (theResults instanceof NoPutResultSet)
			return ((NoPutResultSet) theResults).isForUpdate();
		return false;
	}

	protected final SQLException dataTypeConversion(String targetType, int column) {
		return newSQLException(SQLState.LANG_DATA_TYPE_GET_MISMATCH, targetType,
			resultDescription.getColumnDescriptor(column).getType().getTypeId().getSQLTypeName());
	}

	protected final SQLException dataTypeConversion(int column, String targetType) {
		return newSQLException(SQLState.LANG_DATA_TYPE_GET_MISMATCH,
			resultDescription.getColumnDescriptor(column).getType().getTypeId().getSQLTypeName(), targetType);
	}
}

