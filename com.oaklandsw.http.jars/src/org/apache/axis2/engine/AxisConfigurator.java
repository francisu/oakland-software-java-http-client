package org.apache.axis2.engine;

import org.apache.axis2.AxisFault;

/*
* Copyright 2004,2005 The Apache Software Foundation.
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
*
*
*/

public interface AxisConfigurator {

    /**
     * Configurationcontextfactory will invoke this method to get the AxisConfiguration
     *
     * @return AxisConfigurator
     */
    AxisConfiguration getAxisConfiguration() throws AxisFault;

    /**
     * Method to deploy services from the repository
     */
    void loadServices();

    /**
     * Engages the global modules specified in the configuration
     *
     * @throws AxisFault
     */
    void engageGlobalModules() throws AxisFault;
}
