/*
 * Copyright 2004,2005 The Apache Software Foundation.
 * Copyright 2006 International Business Machines Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.axis2.jaxws.message.util;

import org.apache.axis2.jaxws.message.util.impl.XMLStreamReaderFromDOM;
import org.w3c.dom.Element;

import javax.xml.stream.XMLStreamReader;

/** DOMReader Creates an XMLStreamReader backed by a DOM tree. */
public class DOMReader extends Reader {

    Element element;

    /**
     * @param reader
     * @param resettable
     */
    public DOMReader(Element element) {
        super(_newReader(element), true);

        this.element = element;
    }

    /* (non-Javadoc)
      * @see org.apache.axis2.jaxws.message.util.Reader#newReader()
      */
    @Override
    protected XMLStreamReader newReader() {
        return _newReader(element);
    }

    /**
     * Utility method to get the stream reader
     *
     * @param element
     * @return
     */
    private static XMLStreamReader _newReader(Element element) {
        // Construct a reader from an element
        return new XMLStreamReaderFromDOM(element);
    }
}
