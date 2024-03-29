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
*/


package org.apache.axis2.deployment;

import org.apache.axis2.AxisFault;

public class DeploymentException extends AxisFault {

    private static final long serialVersionUID = -206215612208580684L;

    public DeploymentException(String message) {
        super(message);
    }

    public DeploymentException(Throwable cause) {
        super(cause);
    }

    public DeploymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
