/*
 * Copyright 2001-2004 The Apache Software Foundation.
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
package org.apache.axis2.util;

import org.apache.axis2.description.AxisDescription;
import org.apache.axis2.description.PolicyInclude;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyRegistry;

public class AxisPolicyLocator implements PolicyRegistry {

    AxisDescription subject = null;
    
    public AxisPolicyLocator(AxisDescription subject) {
        this.subject = subject;
    }

    public Policy lookup(String key) {
        if (subject == null) {
            return null;
        }
        
        PolicyInclude policyInclude = subject.getPolicyInclude();
        PolicyRegistry policyRegistry = policyInclude.getPolicyRegistry();
        Policy policy = policyRegistry.lookup(key);
        
        if (policy != null) {
            return policy;
        }
        
        AxisDescription parent = subject.getParent();
        
        if (parent == null) {
            return null;
            
        } else  {
            AxisPolicyLocator locator = new AxisPolicyLocator(parent);
            return locator.lookup(key);
        }
                    
    }

    public void register(String key, Policy policy) {
        throw new UnsupportedOperationException();
    }

    public void remove(String key) {
        throw new UnsupportedOperationException();
    }
}
