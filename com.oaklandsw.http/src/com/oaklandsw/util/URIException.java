//
// Some portions copyright 2002-2003, oakland software, all rights reserved.
//
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.oaklandsw.util;

import java.io.IOException;

/**
 * The URI parsing and escape encoding exception.
 * <p>
 * 
 */
public class URIException extends IOException {

	// ----------------------------------------------------------- constructors

	/**
	 * Default constructor.
	 */
	public URIException() {
	}

	/**
	 * The constructor with a reason code argument.
	 * 
	 * @param rc
	 *            the reason code
	 */
	public URIException(int rc) {
		setReasonCode(rc);
	}

	/**
	 * The constructor with a reason string and its code arguments.
	 * 
	 * @param rc
	 *            the reason code
	 * @param r
	 *            the reason
	 */
	public URIException(int rc, String r) {
		super(r); // for backward compatibility of Throwable
		this.reason = r;
		setReasonCode(rc);
	}

	/**
	 * The constructor with a reason string argument.
	 * 
	 * @param r
	 *            the reason
	 */
	public URIException(String r) {
		super(r); // for backward compatibility of Throwable
		this.reason = r;
		setReasonCode(UNKNOWN);
	}

	// -------------------------------------------------------------- constants

	/**
	 * No specified reason code.
	 */
	public static final int UNKNOWN = 0;

	/**
	 * The URI parsing error.
	 */
	public static final int PARSING = 1;

	/**
	 * The unsupported character encoding.
	 */
	public static final int UNSUPPORTED_ENCODING = 2;

	/**
	 * The URI escape or unescape error.
	 */
	public static final int ESCAPING = 3;

	// ------------------------------------------------------------- properties

	/**
	 * The reason code.
	 */
	protected int reasonCode;

	/**
	 * The reason message.
	 */
	protected String reason;

	// ---------------------------------------------------------------- methods

	/**
	 * Get the reason code.
	 * 
	 * @return the reason code
	 */
	public int getReasonCode() {
		return reasonCode;
	}

	/**
	 * Set the reason code.
	 * 
	 * @param rc
	 *            reason code
	 */
	public void setReasonCode(int rc) {
		this.reasonCode = rc;
	}

	/**
	 * Get the reason message.
	 * 
	 * @return the reason message
	 */
	public String getReason() {
		return reason;
	}

	/**
	 * Set the reason message.
	 * 
	 * @param r
	 *            reason message
	 */
	public void setReason(String r) {
		this.reason = r;
	}

}
