/*

   Derby - Class org.apache.derby.client.am.Cursor

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

// When we calculate column offsets make sure we calculate the correct offsets for double byte charactr5er data
// length from server is number of chars, not bytes
// Direct byte-level converters are called directly by this class, cross converters are deferred to the CrossConverters class.

public abstract class Cursor {
    protected Agent agent_;

    //-----------------------------varchar representations------------------------

    public final static int STRING = 0;
    public final static int VARIABLE_STRING = 2;       // uses a 2-byte length indicator
    public final static int VARIABLE_SHORT_STRING = 1; // aka Pascal L; uses a 1-byte length indicator
    public final static int NULL_TERMINATED_STRING = 3;

    public final static int BYTES = 4;
    public final static int VARIABLE_BYTES = 5;
    public final static int VARIABLE_SHORT_BYTES = 6;
    public final static int NULL_TERMINATED_BYTES = 7;

    public final static int SBCS_CLOB = 8;
    public final static int MBCS_CLOB = 9;
    public final static int DBCS_CLOB = 10;
    //-----------------------------internal state---------------------------------

    //-------------Structures for holding and scrolling the data -----------------
    public byte[] dataBuffer_;
    public java.io.ByteArrayOutputStream dataBufferStream_;
    public int position_; // This is the read head
    public int lastValidBytePosition_;
    public boolean hasLobs_; // is there at least one LOB column?

    // Current row positioning
    protected int currentRowPosition_;
    private int nextRowPosition_;
    // Let's new up a 2-dimensional array based on fetch-size and reuse so that
    protected int[] columnDataPosition_;

    // This is the actual, computed lengths of varchar fields, not the max length from query descriptor or DA
    protected int[] columnDataComputedLength_;
    // populate this for

    // All the data is in the buffers, but user may not have necessarily stepped to the last row yet.
    // This flag indicates that the server has returned all the rows, and is positioned
    // after last, for both scrollable and forward-only cursors.
    // For singleton cursors, this memeber will be set to true as soon as next is called.
    public boolean allRowsReceivedFromServer_;

    // Total number of rows read so far.
    // This should never exceed this.statement.maxRows
    int rowsRead_;

    // Maximum column size limit in bytes.
    int maxFieldSize_ = 0;

    // Row positioning for all cached rows
    // For scrollable result sets, these lists hold the offsets into the cached rowset buffer for each row of data.
    protected java.util.ArrayList columnDataPositionCache_ = new java.util.ArrayList();
    protected java.util.ArrayList columnDataLengthCache_ = new java.util.ArrayList();
    protected java.util.ArrayList columnDataIsNullCache_ = new java.util.ArrayList();
    public java.util.ArrayList isUpdateDeleteHoleCache_ = new java.util.ArrayList();
    public boolean isUpdateDeleteHole_;
    final static public java.lang.Boolean ROW_IS_NULL = new Boolean(true);
    final static public java.lang.Boolean ROW_IS_NOT_NULL = new Boolean(false);

    java.sql.Date recyclableDate_ = null;
    java.sql.Time recyclableTime_ = null;
    java.sql.Timestamp recyclableTimestamp_ = null;

    // For the net, this data comes from the query descriptor.

    public int[] jdbcTypes_;
    public int columns_;
    public boolean[] nullable_;
    public String[] charsetName_;
    public boolean[] isNull_;
    public int[] fdocaLength_; // this is the max length for

    //----------------------------------------------------------------------------

    public int[] ccsid_;
    char[] charBuffer_;

    //---------------------constructors/finalizer---------------------------------

    public Cursor(Agent agent) {
        agent_ = agent;
        dataBufferStream_ = new java.io.ByteArrayOutputStream();
    }

    public Cursor(Agent agent, byte[] dataBuffer) {
        this(agent);
        dataBuffer_ = dataBuffer;
        allRowsReceivedFromServer_ = false;
    }

    public void setNumberOfColumns(int numberOfColumns) {
        columnDataPosition_ = new int[numberOfColumns];
        columnDataComputedLength_ = new int[numberOfColumns];

        columns_ = numberOfColumns;
        nullable_ = new boolean[numberOfColumns];
        charsetName_ = new String[numberOfColumns];

        ccsid_ = new int[numberOfColumns];

        isNull_ = new boolean[numberOfColumns];
        jdbcTypes_ = new int[numberOfColumns];
    }

    // Makes the next row the current row.
    // Returns true if the current row position is a valid row position.
    public boolean next() throws SqlException {
        // local variable usd to hold the returned value from calculateColumnOffsetsForRow()
        boolean rowPositionIsValid = true;

        // reset lob data
        // clears out Cursor.lobs_ calculated for the current row when cursor is moved.
        clearLobData_();

        // mark the start of a new row.
        makeNextRowPositionCurrent();

        // Drive the CNTQRY outside of calculateColumnOffsetsForRow() if the dataBuffer_
        // contains no data since it has no abilities to handle replies other than
        // the QRYDTA, i.e. ENDQRYRM when the result set contains no more rows.
        while (!dataBufferHasUnprocessedData()) {
            if (allRowsReceivedFromServer_) {
                return false;
            }
            getMoreData_();
        }

        // The parameter passed in here is used as an index into the cached rowset for
        // scrollable cursors, for the arrays to be reused.  It is not used for forward-only
        // cursors, so just pass in 0.
        rowPositionIsValid = calculateColumnOffsetsForRow_(0);  // readFetchedRows()
        markNextRowPosition();
        return rowPositionIsValid;
    }

    //--------------------------reseting cursor state-----------------------------

    public final void setAllRowsReceivedFromServer(boolean b) {
        allRowsReceivedFromServer_ = b;
    }

    public final boolean currentRowPositionIsEqualToNextRowPosition() {
        return (currentRowPosition_ == nextRowPosition_);
    }

    // reset the beginning and ending position in the data buffer to 0
    // reset the currentRowPosition and nextRowPosition to 0
    // reset lastRowReached and sqlcode100Received to false
    // clear the column data offsets cache
    public final void resetDataBuffer() {
        position_ = 0;
        lastValidBytePosition_ = 0;
        currentRowPosition_ = 0;
        nextRowPosition_ = 0;
        allRowsReceivedFromServer_ = false;
        dataBufferStream_.reset();
    }

    public final boolean dataBufferHasUnprocessedData() {
        return (lastValidBytePosition_ - position_) > 0;
    }

    protected abstract boolean calculateColumnOffsetsForRow_(int row) throws SqlException, DisconnectException;

    protected abstract void clearLobData_();

    protected abstract void getMoreData_() throws SqlException;

    // Associate a new underlying COM or SQLDA output data buffer for this converter.
    public final void setBuffer(byte[] dataBuffer) {
        dataBuffer_ = dataBuffer;
    }

    public final void setIsUpdataDeleteHole(int row, boolean isRowNull) {
        isUpdateDeleteHole_ = isRowNull;
        Boolean nullIndicator = (isUpdateDeleteHole_ == true) ? ROW_IS_NULL : ROW_IS_NOT_NULL;
        if (isUpdateDeleteHoleCache_.size() == row) {
            isUpdateDeleteHoleCache_.add(nullIndicator);
        } else {
            isUpdateDeleteHoleCache_.set(row, nullIndicator);
        }
    }
    //---------------------------cursor positioning-------------------------------

    final int getPosition() {
        return position_;
    }

    final void setPosition(int newPosition) {
        position_ = newPosition;
    }

    public final void markCurrentRowPosition() {
        currentRowPosition_ = position_;
    }

    public final void markNextRowPosition() {
        nextRowPosition_ = position_;
    }

    public final void makeNextRowPositionCurrent() {
        currentRowPosition_ = nextRowPosition_;
    }

    final void repositionCursorToCurrentRow() {
        position_ = currentRowPosition_;
    }

    final void repositionCursorToNextRow() {
        position_ = nextRowPosition_;
    }

    public final byte[] getDataBuffer() {
        return dataBuffer_;
    }

    public final int getDataBufferLength() {
        return dataBuffer_.length;
    }

    public final int getLastValidBytePosition() {
        return lastValidBytePosition_;
    }

    // This tracks the total number of rows read into the client side buffer for
    // this result set, irregardless of scrolling.
    // Per jdbc semantics, this should never exceed statement.maxRows.
    // This event should be generated in the materialized cursor's implementation
    // of calculateColumnOffsetsForRow().
    public final void incrementRowsReadEvent() {
        rowsRead_++;
    }

    //------- the following getters are called on known column types -------------
    // Direct conversions only, cross conversions are handled by another set of getters.

    // Build a Java short from a 2-byte signed binary representation.
    private final short get_SMALLINT(int column) {
        return org.apache.derby.client.am.SignedBinary.getShort(dataBuffer_,
                columnDataPosition_[column - 1]);
    }

    // Build a Java int from a 4-byte signed binary representation.
    private final int get_INTEGER(int column) {
        return org.apache.derby.client.am.SignedBinary.getInt(dataBuffer_,
                columnDataPosition_[column - 1]);
    }

    // Build a Java long from an 8-byte signed binary representation.
    private final long get_BIGINT(int column) {
        return org.apache.derby.client.am.SignedBinary.getLong(dataBuffer_,
                columnDataPosition_[column - 1]);
    }

    // Build a Java float from a 4-byte floating point representation.
    private final float get_FLOAT(int column) {
        return org.apache.derby.client.am.FloatingPoint.getFloat(dataBuffer_,
                columnDataPosition_[column - 1]);
    }

    // Build a Java double from an 8-byte floating point representation.
    private final double get_DOUBLE(int column) {
        return org.apache.derby.client.am.FloatingPoint.getDouble(dataBuffer_,
                columnDataPosition_[column - 1]);
    }

    // Build a java.math.BigDecimal from a fixed point decimal byte representation.
    private final java.math.BigDecimal get_DECIMAL(int column) throws SqlException {
        try {
            return org.apache.derby.client.am.Decimal.getBigDecimal(dataBuffer_,
                    columnDataPosition_[column - 1],
                    getColumnPrecision(column - 1),
                    getColumnScale(column - 1));
        } catch (java.io.UnsupportedEncodingException e) {
            throw new SqlException(agent_.logWriter_, e, "Encoding is unsupported for conversion to BigDecimal");
        }
    }


    // Build a Java double from a fixed point decimal byte representation.
    private final double getDoubleFromDECIMAL(int column) throws SqlException {
        try {
            return org.apache.derby.client.am.Decimal.getDouble(dataBuffer_,
                    columnDataPosition_[column - 1],
                    getColumnPrecision(column - 1),
                    getColumnScale(column - 1));
        } catch (java.lang.IllegalArgumentException e) {
            throw new SqlException(agent_.logWriter_, e, "Decimal value is out of range for conversion to double");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new SqlException(agent_.logWriter_, e, "Encoding is unsupported for conversion to BigDecimal");
        }
    }

    // Build a Java long from a fixed point decimal byte representation.
    private final long getLongFromDECIMAL(int column) throws SqlException {
        try {
            return org.apache.derby.client.am.Decimal.getLong(dataBuffer_,
                    columnDataPosition_[column - 1],
                    getColumnPrecision(column - 1),
                    getColumnScale(column - 1));
        } catch (java.lang.IllegalArgumentException e) {
            throw new SqlException(agent_.logWriter_, e, "Decimal value is out of range for conversion to long");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new SqlException(agent_.logWriter_, e, "Encoding is unsupported for conversion to BigDecimal");
        }
    }

    // Build a Java String from a database VARCHAR or LONGVARCHAR field.
    //
    // Depending on the ccsid, length is the number of chars or number of bytes.
    // For 2-byte character ccsids, length is the number of characters,
    // for all other cases length is the number of bytes.
    // The length does not include the null terminator.
    private final String getVARCHAR(int column) throws SqlException {
        String tempString = null;
        try {
            if (ccsid_[column - 1] == 1200) {
                return getStringWithoutConvert(columnDataPosition_[column - 1] + 2, columnDataComputedLength_[column - 1] - 2);
            }

            // check for null encoding is needed because the net layer
            // will no longer throw an exception if the server didn't specify
            // a mixed or double byte ccsid (ccsid = 0).  this check for null in the
            // cursor is only required for types which can have mixed or double
            // byte ccsids.
            if (charsetName_[column - 1] == null) {
                throw new SqlException(agent_.logWriter_,
                        "Required character converter not available for data type.");
            }

            tempString = new String(dataBuffer_,
                    columnDataPosition_[column - 1] + 2,
                    columnDataComputedLength_[column - 1] - 2,
                    charsetName_[column - 1]);
            return (maxFieldSize_ == 0) ? tempString :
                    tempString.substring(0, java.lang.Math.min(maxFieldSize_, tempString.length()));
        } catch (java.io.UnsupportedEncodingException e) {
            throw new SqlException(agent_.logWriter_, e, "unsupported encoding for result set column " + column);
        }
    }

    // Build a Java String from a database CHAR field.
    private final String getCHAR(int column) throws SqlException {
        String tempString = null;
        if (ccsid_[column - 1] == 1200) {
            return getStringWithoutConvert(columnDataPosition_[column - 1], columnDataComputedLength_[column - 1]);
        }

        try {
            // check for null encoding is needed because the net layer
            // will no longer throw an exception if the server didn't specify
            // a mixed or double byte ccsid (ccsid = 0).  this check for null in the
            // cursor is only required for types which can have mixed or double
            // byte ccsids.
            if (charsetName_[column - 1] == null) {
                throw new SqlException(agent_.logWriter_,
                        "Required character converter not available for data type.");
            }

            tempString = new String(dataBuffer_,
                    columnDataPosition_[column - 1],
                    columnDataComputedLength_[column - 1],
                    charsetName_[column - 1]);
            return (maxFieldSize_ == 0) ? tempString :
                    tempString.substring(0, java.lang.Math.min(maxFieldSize_, tempString.length()));
        } catch (java.io.UnsupportedEncodingException e) {
            throw new SqlException(agent_.logWriter_, e, "unsupported encoding for result set column " + column);
        }
    }

    // Build a JDBC Date object from the DERBY ISO DATE field.
    private final java.sql.Date getDATE(int column) throws SqlException {
        return org.apache.derby.client.am.DateTime.dateBytesToDate(dataBuffer_,
                columnDataPosition_[column - 1],
                recyclableDate_);
    }

    // Build a JDBC Time object from the DERBY ISO TIME field.
    private final java.sql.Time getTIME(int column) throws SqlException {
        return org.apache.derby.client.am.DateTime.timeBytesToTime(dataBuffer_,
                columnDataPosition_[column - 1],
                recyclableTime_);
    }

    // Build a JDBC Timestamp object from the DERBY ISO TIMESTAMP field.
    private final java.sql.Timestamp getTIMESTAMP(int column) throws SqlException {
        return org.apache.derby.client.am.DateTime.timestampBytesToTimestamp(dataBuffer_,
                columnDataPosition_[column - 1],
                recyclableTimestamp_);
    }

    // Build a JDBC Timestamp object from the DERBY ISO DATE field.
    private final java.sql.Timestamp getTimestampFromDATE(int column) throws SqlException {
        return org.apache.derby.client.am.DateTime.dateBytesToTimestamp(dataBuffer_,
                columnDataPosition_[column - 1],
                recyclableTimestamp_);
    }

    // Build a JDBC Timestamp object from the DERBY ISO TIME field.
    private final java.sql.Timestamp getTimestampFromTIME(int column) throws SqlException {
        return org.apache.derby.client.am.DateTime.timeBytesToTimestamp(dataBuffer_,
                columnDataPosition_[column - 1],
                recyclableTimestamp_);
    }

    // Build a JDBC Date object from the DERBY ISO TIMESTAMP field.
    private final java.sql.Date getDateFromTIMESTAMP(int column) throws SqlException {
        return org.apache.derby.client.am.DateTime.timestampBytesToDate(dataBuffer_,
                columnDataPosition_[column - 1],
                recyclableDate_);
    }

    // Build a JDBC Time object from the DERBY ISO TIMESTAMP field.
    private final java.sql.Time getTimeFromTIMESTAMP(int column) throws SqlException {
        return org.apache.derby.client.am.DateTime.timestampBytesToTime(dataBuffer_,
                columnDataPosition_[column - 1],
                recyclableTime_);
    }

    private final String getStringFromDATE(int column) throws SqlException {
        return org.apache.derby.client.am.DateTime.dateBytesToDate(dataBuffer_,
                columnDataPosition_[column - 1],
                recyclableDate_).toString();
    }

    // Build a string object from the DERBY byte TIME representation.
    private final String getStringFromTIME(int column) throws SqlException {
        return org.apache.derby.client.am.DateTime.timeBytesToTime(dataBuffer_,
                columnDataPosition_[column - 1],
                recyclableTime_).toString();
    }

    // Build a string object from the DERBY byte TIMESTAMP representation.
    private final String getStringFromTIMESTAMP(int column) throws SqlException {
        return org.apache.derby.client.am.DateTime.timestampBytesToTimestamp(dataBuffer_,
                columnDataPosition_[column - 1],
                recyclableTimestamp_).toString();
    }

    // Extract bytes from a database java.sql.Types.BINARY field.
    // This is the DERBY type CHAR(n) FOR BIT DATA.
    private final byte[] get_CHAR_FOR_BIT_DATA(int column) throws SqlException {
        // There is no limit to the size of a column if maxFieldSize is zero.
        // Otherwise, use the smaller of maxFieldSize and the actual column length.
        int columnLength = (maxFieldSize_ == 0) ? columnDataComputedLength_[column - 1] :
                java.lang.Math.min(maxFieldSize_, columnDataComputedLength_[column - 1]);

        byte[] bytes = new byte[columnLength];
        System.arraycopy(dataBuffer_, columnDataPosition_[column - 1], bytes, 0, columnLength);
        return bytes;
    }

    // Extract bytes from a database java.sql.Types.VARBINARY or LONGVARBINARY field.
    // This includes the DERBY types:
    //   VARCHAR(n) FOR BIT DATA
    //   LONG VARCHAR(n) FOR BIT DATA
    private final byte[] get_VARCHAR_FOR_BIT_DATA(int column) throws SqlException {
        byte[] bytes;
        int columnLength = 0;
        columnLength = (maxFieldSize_ == 0) ? columnDataComputedLength_[column - 1] - 2 :
                java.lang.Math.min(maxFieldSize_, columnDataComputedLength_[column - 1] - 2);
        bytes = new byte[columnLength];
        System.arraycopy(dataBuffer_, columnDataPosition_[column - 1] + 2, bytes, 0, bytes.length);
        return bytes;
    }

    abstract public Blob getBlobColumn_(int column, Agent agent) throws SqlException;

    abstract public Clob getClobColumn_(int column, Agent agent) throws SqlException;

    // get the raw clob bytes, without translation.  dataOffset must be int[1]
    abstract public byte[] getClobBytes_(int column, int[] dataOffset /*output*/) throws SqlException;

    //------- the following getters perform any necessary cross-conversion _------

    final boolean getBoolean(int column) throws SqlException {
        switch (jdbcTypes_[column - 1]) {
        case java.sql.Types.SMALLINT:
            return agent_.crossConverters_.getBooleanFromShort(get_SMALLINT(column));
        case java.sql.Types.INTEGER:
            return agent_.crossConverters_.getBooleanFromInt(get_INTEGER(column));
        case java.sql.Types.BIGINT:
            return agent_.crossConverters_.getBooleanFromLong(get_BIGINT(column));
        case java.sql.Types.REAL:
            return agent_.crossConverters_.getBooleanFromFloat(get_FLOAT(column));
        case java.sql.Types.DOUBLE:
            return agent_.crossConverters_.getBooleanFromDouble(get_DOUBLE(column));
        case java.sql.Types.DECIMAL:
            // For performance we don't materialize the BigDecimal, but convert directly from decimal bytes to a long.
            return agent_.crossConverters_.getBooleanFromLong(getLongFromDECIMAL(column));
        case java.sql.Types.CHAR:
            return agent_.crossConverters_.getBooleanFromString(getCHAR(column));
        case java.sql.Types.VARCHAR:
        case java.sql.Types.LONGVARCHAR:
            return agent_.crossConverters_.getBooleanFromString(getVARCHAR(column));
        default:
            throw new ColumnTypeConversionException(agent_.logWriter_);
        }
    }

    final byte getByte(int column) throws SqlException {
        // This needs to be changed to use jdbcTypes[]
        switch (jdbcTypes_[column - 1]) {
        case java.sql.Types.SMALLINT:
            return agent_.crossConverters_.getByteFromShort(get_SMALLINT(column));
        case java.sql.Types.INTEGER:
            return agent_.crossConverters_.getByteFromInt(get_INTEGER(column));
        case java.sql.Types.BIGINT:
            return agent_.crossConverters_.getByteFromLong(get_BIGINT(column));
        case java.sql.Types.REAL:
            return agent_.crossConverters_.getByteFromFloat(get_FLOAT(column));
        case java.sql.Types.DOUBLE:
            return agent_.crossConverters_.getByteFromDouble(get_DOUBLE(column));
        case java.sql.Types.DECIMAL:
            // For performance we don't materialize the BigDecimal, but convert directly from decimal bytes to a long.
            return agent_.crossConverters_.getByteFromLong(getLongFromDECIMAL(column));
        case java.sql.Types.CHAR:
            return agent_.crossConverters_.getByteFromString(getCHAR(column));
        case java.sql.Types.VARCHAR:
        case java.sql.Types.LONGVARCHAR:
            return agent_.crossConverters_.getByteFromString(getVARCHAR(column));
        default:
            throw new ColumnTypeConversionException(agent_.logWriter_);
        }
    }

    final short getShort(int column) throws SqlException {
        switch (jdbcTypes_[column - 1]) {
        case java.sql.Types.SMALLINT:
            return get_SMALLINT(column);
        case java.sql.Types.INTEGER:
            return agent_.crossConverters_.getShortFromInt(get_INTEGER(column));
        case java.sql.Types.BIGINT:
            return agent_.crossConverters_.getShortFromLong(get_BIGINT(column));
        case java.sql.Types.REAL:
            return agent_.crossConverters_.getShortFromFloat(get_FLOAT(column));
        case java.sql.Types.DOUBLE:
            return agent_.crossConverters_.getShortFromDouble(get_DOUBLE(column));
        case java.sql.Types.DECIMAL:
            // For performance we don't materialize the BigDecimal, but convert directly from decimal bytes to a long.
            return agent_.crossConverters_.getShortFromLong(getLongFromDECIMAL(column));
        case java.sql.Types.CHAR:
            return agent_.crossConverters_.getShortFromString(getCHAR(column));
        case java.sql.Types.VARCHAR:
        case java.sql.Types.LONGVARCHAR:
            return agent_.crossConverters_.getShortFromString(getVARCHAR(column));
        default:
            throw new ColumnTypeConversionException(agent_.logWriter_);
        }
    }

    final int getInt(int column) throws SqlException {
        switch (jdbcTypes_[column - 1]) {
        case java.sql.Types.SMALLINT:
            return (int) get_SMALLINT(column);
        case java.sql.Types.INTEGER:
            return get_INTEGER(column);
        case java.sql.Types.BIGINT:
            return agent_.crossConverters_.getIntFromLong(get_BIGINT(column));
        case java.sql.Types.REAL:
            return agent_.crossConverters_.getIntFromFloat(get_FLOAT(column));
        case java.sql.Types.DOUBLE:
            return agent_.crossConverters_.getIntFromDouble(get_DOUBLE(column));
        case java.sql.Types.DECIMAL:
            // For performance we don't materialize the BigDecimal, but convert directly from decimal bytes to a long.
            return agent_.crossConverters_.getIntFromLong(getLongFromDECIMAL(column));
        case java.sql.Types.CHAR:
            return agent_.crossConverters_.getIntFromString(getCHAR(column));
        case java.sql.Types.VARCHAR:
        case java.sql.Types.LONGVARCHAR:
            return agent_.crossConverters_.getIntFromString(getVARCHAR(column));
        default:
            throw new ColumnTypeConversionException(agent_.logWriter_);
        }
    }

    final long getLong(int column) throws SqlException {
        switch (jdbcTypes_[column - 1]) {
        case java.sql.Types.SMALLINT:
            return (long) get_SMALLINT(column);
        case java.sql.Types.INTEGER:
            return (long) get_INTEGER(column);
        case java.sql.Types.BIGINT:
            return get_BIGINT(column);
        case java.sql.Types.REAL:
            return agent_.crossConverters_.getLongFromFloat(get_FLOAT(column));
        case java.sql.Types.DOUBLE:
            return agent_.crossConverters_.getLongFromDouble(get_DOUBLE(column));
        case java.sql.Types.DECIMAL:
            // For performance we don't materialize the BigDecimal, but convert directly from decimal bytes to a long.
            return getLongFromDECIMAL(column);
        case java.sql.Types.CHAR:
            return agent_.crossConverters_.getLongFromString(getCHAR(column));
        case java.sql.Types.VARCHAR:
        case java.sql.Types.LONGVARCHAR:
            return agent_.crossConverters_.getLongFromString(getVARCHAR(column));
        default:
            throw new ColumnTypeConversionException(agent_.logWriter_);
        }
    }

    final float getFloat(int column) throws SqlException {
        switch (jdbcTypes_[column - 1]) {
        case java.sql.Types.REAL:
            return get_FLOAT(column);
        case java.sql.Types.DOUBLE:
            return agent_.crossConverters_.getFloatFromDouble(get_DOUBLE(column));
        case java.sql.Types.DECIMAL:
            // For performance we don't materialize the BigDecimal, but convert directly from decimal bytes to a long.
            return agent_.crossConverters_.getFloatFromDouble(getDoubleFromDECIMAL(column));
        case java.sql.Types.SMALLINT:
            return (float) get_SMALLINT(column);
        case java.sql.Types.INTEGER:
            return (float) get_INTEGER(column);
        case java.sql.Types.BIGINT:
            return (float) get_BIGINT(column);
        case java.sql.Types.CHAR:
            return agent_.crossConverters_.getFloatFromString(getCHAR(column));
        case java.sql.Types.VARCHAR:
        case java.sql.Types.LONGVARCHAR:
            return agent_.crossConverters_.getFloatFromString(getVARCHAR(column));
        default:
            throw new ColumnTypeConversionException(agent_.logWriter_);
        }
    }

    final double getDouble(int column) throws SqlException {
        switch (jdbcTypes_[column - 1]) {
        case java.sql.Types.REAL:
            double d = (double) get_FLOAT(column);
            return d;
            //return (double) get_FLOAT (column);
        case java.sql.Types.DOUBLE:
            return get_DOUBLE(column);
        case java.sql.Types.DECIMAL:
            // For performance we don't materialize the BigDecimal, but convert directly from decimal bytes to a long.
            return getDoubleFromDECIMAL(column);
        case java.sql.Types.SMALLINT:
            return (double) get_SMALLINT(column);
        case java.sql.Types.INTEGER:
            return (double) get_INTEGER(column);
        case java.sql.Types.BIGINT:
            return (double) get_BIGINT(column);
        case java.sql.Types.CHAR:
            return agent_.crossConverters_.getDoubleFromString(getCHAR(column));
        case java.sql.Types.VARCHAR:
        case java.sql.Types.LONGVARCHAR:
            return agent_.crossConverters_.getDoubleFromString(getVARCHAR(column));
        default:
            throw new ColumnTypeConversionException(agent_.logWriter_);
        }
    }

    final java.math.BigDecimal getBigDecimal(int column) throws SqlException {
        switch (jdbcTypes_[column - 1]) {
        case java.sql.Types.DECIMAL:
            return get_DECIMAL(column);
        case java.sql.Types.REAL:
            // Can't use the following commented out line because it changes precision of the result.
            //return new java.math.BigDecimal (get_FLOAT (column));
            float f = get_FLOAT(column);
            return new java.math.BigDecimal(String.valueOf(f));
        case java.sql.Types.DOUBLE:
            // Can't use the following commented out line because it changes precision of the result.
            return new java.math.BigDecimal(String.valueOf(get_DOUBLE(column)));
        case java.sql.Types.SMALLINT:
            return java.math.BigDecimal.valueOf(get_SMALLINT(column));
        case java.sql.Types.INTEGER:
            return java.math.BigDecimal.valueOf(get_INTEGER(column));
        case java.sql.Types.BIGINT:
            return java.math.BigDecimal.valueOf(get_BIGINT(column));
        case java.sql.Types.CHAR:
            return agent_.crossConverters_.getBigDecimalFromString(getCHAR(column));
        case java.sql.Types.VARCHAR:
        case java.sql.Types.LONGVARCHAR:
            return agent_.crossConverters_.getBigDecimalFromString(getVARCHAR(column));
        default:
            throw new ColumnTypeConversionException(agent_.logWriter_);
        }
    }

    final java.sql.Date getDate(int column) throws SqlException {
        switch (jdbcTypes_[column - 1]) {
        case java.sql.Types.DATE:
            return getDATE(column);
        case java.sql.Types.TIMESTAMP:
            return getDateFromTIMESTAMP(column);
        case java.sql.Types.CHAR:
            return agent_.crossConverters_.getDateFromString(getCHAR(column));
        case java.sql.Types.VARCHAR:
        case java.sql.Types.LONGVARCHAR:
            return agent_.crossConverters_.getDateFromString(getVARCHAR(column));
        default:
            throw new ColumnTypeConversionException(agent_.logWriter_);
        }
    }

    final java.sql.Time getTime(int column) throws SqlException {
        switch (jdbcTypes_[column - 1]) {
        case java.sql.Types.TIME:
            return getTIME(column);
        case java.sql.Types.TIMESTAMP:
            return getTimeFromTIMESTAMP(column);
        case java.sql.Types.CHAR:
            return agent_.crossConverters_.getTimeFromString(getCHAR(column));
        case java.sql.Types.VARCHAR:
        case java.sql.Types.LONGVARCHAR:
            return agent_.crossConverters_.getTimeFromString(getVARCHAR(column));
        default:
            throw new ColumnTypeConversionException(agent_.logWriter_);
        }
    }

    final java.sql.Timestamp getTimestamp(int column) throws SqlException {
        switch (jdbcTypes_[column - 1]) {
        case java.sql.Types.TIMESTAMP:
            return getTIMESTAMP(column);
        case java.sql.Types.DATE:
            return getTimestampFromDATE(column);
        case java.sql.Types.TIME:
            return getTimestampFromTIME(column);
        case java.sql.Types.CHAR:
            return agent_.crossConverters_.getTimestampFromString(getCHAR(column));
        case java.sql.Types.VARCHAR:
        case java.sql.Types.LONGVARCHAR:
            return agent_.crossConverters_.getTimestampFromString(getVARCHAR(column));
        default:
            throw new ColumnTypeConversionException(agent_.logWriter_);
        }
    }

    final String getString(int column) throws SqlException {
        String tempString = null;
        switch (jdbcTypes_[column - 1]) {
        case java.sql.Types.CHAR:
            return getCHAR(column);
        case java.sql.Types.VARCHAR:
        case java.sql.Types.LONGVARCHAR:
            return getVARCHAR(column);

        case java.sql.Types.SMALLINT:
            return String.valueOf(get_SMALLINT(column));
        case java.sql.Types.INTEGER:
            return String.valueOf(get_INTEGER(column));
        case java.sql.Types.BIGINT:
            return String.valueOf(get_BIGINT(column));
        case java.sql.Types.REAL:
            return String.valueOf(get_FLOAT(column));
        case java.sql.Types.DOUBLE:
            return String.valueOf(get_DOUBLE(column));
        case java.sql.Types.DECIMAL:
            // We could get better performance here if we didn't materialize the BigDecimal,
            // but converted directly from decimal bytes to a string.
            return String.valueOf(get_DECIMAL(column));
        case java.sql.Types.DATE:
            return getStringFromDATE(column);
        case java.sql.Types.TIME:
            return getStringFromTIME(column);
        case java.sql.Types.TIMESTAMP:
            return getStringFromTIMESTAMP(column);
        case Types.BINARY:
            tempString =
                    agent_.crossConverters_.getStringFromBytes(get_CHAR_FOR_BIT_DATA(column));
            return (maxFieldSize_ == 0) ? tempString :
                    tempString.substring(0, java.lang.Math.min(maxFieldSize_, tempString.length()));
        case java.sql.Types.VARBINARY:
        case java.sql.Types.LONGVARBINARY:
            tempString =
                    agent_.crossConverters_.getStringFromBytes(get_VARCHAR_FOR_BIT_DATA(column));
            return (maxFieldSize_ == 0) ? tempString :
                    tempString.substring(0, java.lang.Math.min(maxFieldSize_, tempString.length()));
        case java.sql.Types.BLOB:
            Blob b = (Blob) getBlobColumn_(column, agent_);
            return agent_.crossConverters_.getStringFromBytes(b.getBytes(1, (int) b.length()));
        case java.sql.Types.CLOB:
            Clob c = getClobColumn_(column, agent_);
            return c.getSubString(1, (int) c.length());
        default:
            throw new ColumnTypeConversionException(agent_.logWriter_);
        }
    }

    final byte[] getBytes(int column) throws SqlException {
        switch (jdbcTypes_[column - 1]) {
        case java.sql.Types.BINARY:
            return get_CHAR_FOR_BIT_DATA(column);
        case java.sql.Types.VARBINARY:
        case java.sql.Types.LONGVARBINARY:
            return get_VARCHAR_FOR_BIT_DATA(column);
        case java.sql.Types.BLOB:
            Blob b = (Blob) getBlobColumn_(column, agent_);
            return b.getBytes(1, (int) b.length());
        default:
            throw new ColumnTypeConversionException(agent_.logWriter_);
        }
    }

    public final java.io.InputStream getBinaryStream(int column) throws SqlException {
        switch (jdbcTypes_[column - 1]) {
        case java.sql.Types.BINARY:
            return new java.io.ByteArrayInputStream(get_CHAR_FOR_BIT_DATA(column));
        case java.sql.Types.VARBINARY:
        case java.sql.Types.LONGVARBINARY:
            return new java.io.ByteArrayInputStream(get_VARCHAR_FOR_BIT_DATA(column));
        case java.sql.Types.BLOB:
            Blob b = (Blob) getBlobColumn_(column, agent_);
            return b.getBinaryStream();
        default:
            throw new ColumnTypeConversionException(agent_.logWriter_);
        }
    }

    public final java.io.InputStream getAsciiStream(int column) throws SqlException {
        switch (jdbcTypes_[column - 1]) {
        case java.sql.Types.CLOB:
            Clob c = getClobColumn_(column, agent_);
            return c.getAsciiStream();
        case java.sql.Types.CHAR:
            try {
                return new java.io.ByteArrayInputStream(getCHAR(column).getBytes("US-ASCII"));
            } catch (java.io.UnsupportedEncodingException e) {
                throw new SqlException(agent_.logWriter_, e.getMessage());
            }
        case java.sql.Types.VARCHAR:
        case java.sql.Types.LONGVARCHAR:
            try {
                return new java.io.ByteArrayInputStream(getVARCHAR(column).getBytes("US-ASCII"));
            } catch (java.io.UnsupportedEncodingException e) {
                throw new SqlException(agent_.logWriter_, e.getMessage());
            }
        case java.sql.Types.BINARY:
            return new java.io.ByteArrayInputStream(get_CHAR_FOR_BIT_DATA(column));
        case java.sql.Types.VARBINARY:
        case java.sql.Types.LONGVARBINARY:
            return new java.io.ByteArrayInputStream(get_VARCHAR_FOR_BIT_DATA(column));
        case java.sql.Types.BLOB:
            Blob b = (Blob) getBlobColumn_(column, agent_);
            return b.getBinaryStream();
        default:
            throw new ColumnTypeConversionException(agent_.logWriter_);
        }
    }

    public final java.io.InputStream getUnicodeStream(int column) throws SqlException {
        switch (jdbcTypes_[column - 1]) {
        case java.sql.Types.CLOB:
            {
                Clob c = getClobColumn_(column, agent_);
                String s = c.getSubString(1L, (int) c.length());
                try {
                    return new java.io.ByteArrayInputStream(s.getBytes("UTF-8"));
                } catch (java.io.UnsupportedEncodingException e) {
                    throw new SqlException(agent_.logWriter_, e.getMessage());
                }
            }
        case java.sql.Types.CHAR:
            {
                try {
                    return new java.io.ByteArrayInputStream(getCHAR(column).getBytes("UTF-8"));
                } catch (java.io.UnsupportedEncodingException e) {
                    throw new SqlException(agent_.logWriter_, e.getMessage());
                }
            }
        case java.sql.Types.VARCHAR:
        case java.sql.Types.LONGVARCHAR:
            try {
                return new java.io.ByteArrayInputStream(getVARCHAR(column).getBytes("UTF-8"));
            } catch (java.io.UnsupportedEncodingException e) {
                throw new SqlException(agent_.logWriter_, e.getMessage());
            }
        case java.sql.Types.BINARY:
            return new java.io.ByteArrayInputStream(get_CHAR_FOR_BIT_DATA(column));
        case java.sql.Types.VARBINARY:
        case java.sql.Types.LONGVARBINARY:
            return new java.io.ByteArrayInputStream(get_VARCHAR_FOR_BIT_DATA(column));
        case java.sql.Types.BLOB:
            Blob b = (Blob) getBlobColumn_(column, agent_);
            return b.getBinaryStream();
        default:
            throw new ColumnTypeConversionException(agent_.logWriter_);
        }
    }

    public final java.io.Reader getCharacterStream(int column) throws SqlException {
        switch (jdbcTypes_[column - 1]) {
        case java.sql.Types.CLOB:
            Clob c = getClobColumn_(column, agent_);
            return c.getCharacterStream();
        case java.sql.Types.CHAR:
            return new java.io.StringReader(getCHAR(column));
        case java.sql.Types.VARCHAR:
        case java.sql.Types.LONGVARCHAR:
            return new java.io.StringReader(getVARCHAR(column));
        case java.sql.Types.BINARY:
            try {
                return new java.io.InputStreamReader(new java.io.ByteArrayInputStream(get_CHAR_FOR_BIT_DATA(column)), "UTF-16BE");
            } catch (java.io.UnsupportedEncodingException e) {
                throw new SqlException(agent_.logWriter_, "UnsupportedEncodingException: " + e.getMessage());
            }
        case java.sql.Types.VARBINARY:
        case java.sql.Types.LONGVARBINARY:
            try {
                return new java.io.InputStreamReader(new java.io.ByteArrayInputStream(get_VARCHAR_FOR_BIT_DATA(column)), "UTF-16BE");
            } catch (java.io.UnsupportedEncodingException e) {
                throw new SqlException(agent_.logWriter_, "UnsupportedEncodingException: " + e.getMessage());
            }
        case java.sql.Types.BLOB:
            try {
                Blob b = (Blob) getBlobColumn_(column, agent_);
                return new java.io.InputStreamReader(b.getBinaryStream(), "UTF-16BE");
            } catch (java.io.UnsupportedEncodingException e) {
                throw new SqlException(agent_.logWriter_, "UnsupportedEncodingException: " + e.getMessage());
            }
        default:
            throw new ColumnTypeConversionException(agent_.logWriter_);
        }
    }

    public final java.sql.Blob getBlob(int column) throws SqlException {
        switch (jdbcTypes_[column - 1]) {
        case Types.BLOB:
            return getBlobColumn_(column, agent_);
        default:
            throw new ColumnTypeConversionException(agent_.logWriter_);
        }
    }

    public final java.sql.Clob getClob(int column) throws SqlException {
        switch (jdbcTypes_[column - 1]) {
        case Types.CLOB:
            return getClobColumn_(column, agent_);
        default:
            throw new ColumnTypeConversionException(agent_.logWriter_);
        }
    }

    public final java.sql.Array getArray(int column) throws SqlException {
        throw new SqlException(agent_.logWriter_, "not yet implemented");
    }

    public final java.sql.Ref getRef(int column) throws SqlException {
        throw new SqlException(agent_.logWriter_, "not yet implemented");
    }

    public final Object getObject(int column) throws SqlException {
        switch (jdbcTypes_[column - 1]) {
        case java.sql.Types.SMALLINT:
            return new Integer(get_SMALLINT(column)); // See Table 4 in JDBC 1 spec (pg. 932 in jdbc book)
        case java.sql.Types.INTEGER:
            return new Integer(get_INTEGER(column));
        case java.sql.Types.BIGINT:
            return new Long(get_BIGINT(column));
        case java.sql.Types.REAL:
            return new Float(get_FLOAT(column));
        case java.sql.Types.DOUBLE:
            return new Double(get_DOUBLE(column));
        case java.sql.Types.DECIMAL:
            return get_DECIMAL(column);
        case java.sql.Types.DATE:
            return getDATE(column);
        case java.sql.Types.TIME:
            return getTIME(column);
        case java.sql.Types.TIMESTAMP:
            return getTIMESTAMP(column);
        case java.sql.Types.CHAR:
            return getCHAR(column);
        case java.sql.Types.VARCHAR:
        case java.sql.Types.LONGVARCHAR:
            return getVARCHAR(column);
        case Types.BINARY:
            return get_CHAR_FOR_BIT_DATA(column);
        case java.sql.Types.VARBINARY:
        case java.sql.Types.LONGVARBINARY:
            return get_VARCHAR_FOR_BIT_DATA(column);
        case java.sql.Types.BLOB:
            return getBlobColumn_(column, agent_);
        case java.sql.Types.CLOB:
            return getClobColumn_(column, agent_);
        default:
            throw new ColumnTypeConversionException(agent_.logWriter_);
        }
    }

    public final void allocateCharBuffer() {
        // compute the maximum char length
        int maxCharLength = 0;
        for (int i = 0; i < columns_; i++) {
            switch (jdbcTypes_[i]) {
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
                if (fdocaLength_[i] > maxCharLength) {
                    maxCharLength = fdocaLength_[i];
                }
            }
        }

        // allocate char buffer to accomodate largest result column
        charBuffer_ = new char[maxCharLength];
    }

    private final String getStringWithoutConvert(int position, int actualLength) throws SqlException {
        int start = position;
        int end = position + actualLength;

        int charCount = 0;
        while (start < end) {
            charBuffer_[charCount++] = (char) (((dataBuffer_[start] & 0xff) << 8) | (dataBuffer_[start + 1] & 0xff));
            start += 2;
        }

        return new String(charBuffer_, 0, charCount);
    }

    public void nullDataForGC() {
        dataBuffer_ = null;
        dataBufferStream_ = null;
        columnDataPosition_ = null;
        columnDataComputedLength_ = null;
        columnDataPositionCache_ = null;
        columnDataLengthCache_ = null;
        columnDataIsNullCache_ = null;
        jdbcTypes_ = null;
        nullable_ = null;
        charsetName_ = null;
        this.ccsid_ = null;
        isUpdateDeleteHoleCache_ = null;
        isNull_ = null;
        fdocaLength_ = null;
        charBuffer_ = null;
    }

    private final int getColumnPrecision(int column) {
        return ((fdocaLength_[column] >> 8) & 0xff);
    }

    private final int getColumnScale(int column) {
        return (fdocaLength_[column] & 0xff);
    }

    // Only used by Sqlca.getMessage() when using a locale encoding
    // to convert errror message text instead of relying on server encoding as usual.
    final byte[] getBytesFromVARCHAR(int column) throws SqlException {
        byte[] bytes;
        bytes = new byte[columnDataComputedLength_[column - 1] - 2];
        System.arraycopy(dataBuffer_, columnDataPosition_[column - 1] + 2, bytes, 0, bytes.length);
        return bytes;
    }
}
