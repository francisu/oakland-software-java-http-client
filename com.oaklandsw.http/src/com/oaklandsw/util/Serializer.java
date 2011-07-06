/*
 * Copyright 1999-2002,2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.oaklandsw.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import javax.xml.transform.stream.StreamResult;

import org.xml.sax.ContentHandler;

/**
 * Interface for a DOM serializer implementation, factory for DOM and SAX
 * serializers, and static methods for serializing DOM documents.
 */
public interface Serializer {
	/**
	 * Sets the output based on the StreamResult
	 * 
	 * @param output
	 */
	public void setStreamResult(StreamResult output);

	/**
	 * Specifies an output stream to which the document should be serialized.
	 * This method should not be called while the serializer is in the process
	 * of serializing a document.
	 */
	public void setOutputByteStream(OutputStream output);

	public OutputStream getOutputByteStream();

	/**
	 * Specifies a writer to which the document should be serialized. This
	 * method should not be called while the serializer is in the process of
	 * serializing a document.
	 */
	public void setOutputCharStream(Writer output);

	public Writer getOutputCharStream();

	/**
	 * Return a {@link ContentHandler} interface into this serializer. If the
	 * serializer does not support the {@link ContentHandler} interface, it
	 * should return null.
	 */
	public ContentHandler asContentHandler() throws IOException;

}
