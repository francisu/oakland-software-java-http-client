/*
* Copyright 2007 The Apache Software Foundation.
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

package org.apache.axis2.dataretrieval;

/**
 * Axis 2 Data Locator responsibles for retrieving Policy  metadata.
 * The class is created as model for policy specific data locator; and also
 * easier for any future implementation policy specific data retrieval logic.
 */
public class PolicyDataLocator extends BaseAxisDataLocator implements AxisDataLocator {

    protected PolicyDataLocator() {

    }

    /**
     * Constructor
     */
    protected PolicyDataLocator(ServiceData[] data) {
        dataList = data;
    }

}
