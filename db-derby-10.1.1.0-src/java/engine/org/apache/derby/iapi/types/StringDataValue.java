/*

   Derby - Class org.apache.derby.iapi.types.StringDataValue

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

package org.apache.derby.iapi.types;

import org.apache.derby.iapi.error.StandardException;

public interface StringDataValue extends ConcatableDataValue
{
	// TRIM() types
	public static final int BOTH		= 0;
	public static final int TRAILING	= 1;
	public static final int LEADING		= 2;

	/**
	 * The SQL concatenation '||' operator.
	 *
	 * @param leftOperand	String on the left hand side of '||'
	 * @param rightOperand	String on the right hand side of '||'
	 * @param result	The result of a previous call to this method,
	 *					null if not called yet.
	 *
	 * @return	A ConcatableDataValue containing the result of the '||'
	 *
	 * @exception StandardException		Thrown on error
	 */
	public StringDataValue concatenate(
				StringDataValue leftOperand,
				StringDataValue rightOperand,
				StringDataValue result)
		throws StandardException;

	/**
	 * The SQL like() function with out escape clause.
	 *
	 * @param pattern	the pattern to use
	 *
	 * @return	A BooleanDataValue containing the result of the like
	 *
	 * @exception StandardException		Thrown on error
	 */
	public BooleanDataValue like(DataValueDescriptor pattern)
							throws StandardException;

	/**
	 * The SQL like() function WITH escape clause.
	 *
	 * @param pattern	the pattern to use
	 * @param escape	the escape character
	 *
	 * @return	A BooleanDataValue containing the result of the like
	 *
	 * @exception StandardException		Thrown on error
	 */
	public BooleanDataValue like(DataValueDescriptor pattern,
									DataValueDescriptor escape)
							throws StandardException;

	/**
	 * The SQL trim(), ltrim() and rtrim() functions.
	 *
	 * @param trimType	Type of trim
	 * @param result	The result of a previous call to this method,
	 *					null if not called yet.
	 *
	 * @return	A StringDataValue containing the result of the trim()
	 *
	 * @exception StandardException		Thrown on error
	 */
	public StringDataValue trim(
				int trimType,
				StringDataValue result)
		throws StandardException;

	/** 
	 * Convert the string to upper case.
	 *
	 * @param result	The result (reusable - allocate if null).
	 * 
	 * @return	The string converted to upper case.
	 *
	 * @exception StandardException		Thrown on error
	 */
	public StringDataValue upper(StringDataValue result)
							throws StandardException;

	/** 
	 * Convert the string to lower case.
	 *
	 * @param result	The result (reusable - allocate if null).
	 * 
	 * @return	The string converted to lower case.
	 *
	 * @exception StandardException		Thrown on error
	 */
	public StringDataValue lower(StringDataValue result)
							throws StandardException;

    /**
     * Position in searchFrom of the first occurrence of this.value.
     * The search begins from position start.  0 is returned if searchFrom does
     * not contain this.value.  Position 1 is the first character in searchFrom.
     *
     * @param searchFrom    - The string to search from
     * @param start         - The position to search from in string searchFrom
     * @param result        - The object to return
     *
     * @return  The position in searchFrom the fist occurrence of this.value.
     *              0 is returned if searchFrom does not contain this.value.
     * @exception StandardException     Thrown on error
     */
    public NumberDataValue locate(  StringDataValue searchFrom, 
                                    NumberDataValue start,
                                    NumberDataValue result)
                                    throws StandardException;


	/**
	 * Get a char array.  Typically, this is a simple
	 * getter that is cheaper than getString() because
	 * we always need to create a char array when
	 * doing I/O.  Use this instead of getString() where
	 * reasonable.
	 * <p>
	 * <b>WARNING</b>: may return a character array that has spare
	 * characters at the end.  MUST be used in conjunction
	 * with getLength() to be safe.
	 * 
	 * @exception StandardException		Thrown on error
	 */
	public char[] getCharArray() throws StandardException;

    /*
     * Get an XML value from a string.
     *
     * NOTE: The XMLParse operator is defined here since it
     * is called with a _String_ operand, not with an XML
     * operand.  That said, though, the bulk of the work is
     * is done by a corresponding method that is defined
     * on the XMLDataValue interface (implementations
     * of this method should just call the XMLDataValue
     * method, passing in the correct String data).
     *
     * @param result The result (reusable - allocate if null).
     * @return An XML value corresponding to the string, if
     *  the string is valid XML.
     * @exception StandardException Thrown on error
     */
    public XMLDataValue XMLParse(XMLDataValue result, boolean preserveWS)
        throws StandardException;

}
