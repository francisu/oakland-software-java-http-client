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

package org.apache.axis2.cluster.configuration;

public class ConfigurationEvent {

	String configurationName;
	String parentConfigurationName;
	int configurationType;
	String policyId = null;
	
	public String getConfigurationName() {
		return configurationName;
	}

	public void setConfigurationName(String configurationName) {
		this.configurationName = configurationName;
	}

	public int getConfigurationType() {
		return configurationType;
	}

	public void setConfigurationType(int configurationType) {
		this.configurationType = configurationType;
	}

	public String getParentConfigurationName() {
		return parentConfigurationName;
	}

	public void setParentConfigurationName(String parentConfigurationName) {
		this.parentConfigurationName = parentConfigurationName;
	}

	public String getPolicyId() {
		return policyId;
	}

	public void setPolicyId(String policyId) {
		this.policyId = policyId;
	}
	
}
