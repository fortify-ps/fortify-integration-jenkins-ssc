/*******************************************************************************
 * (c) Copyright 2017 EntIT Software LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.integration.jenkins.ssc.describable;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.model.AbstractDescribableImpl;

public abstract class AbstractFortifySSCApplicationAndVersionNameConfig<T extends AbstractFortifySSCApplicationAndVersionNameConfig<T>> extends AbstractDescribableImpl<T> {
	private String applicationName = "";
	private String versionName = "";
	private boolean applicationNameOverrideAllowed = true;
	private boolean versionNameOverrideAllowed = true;
	
	public String getApplicationName() {
		return applicationName;
	}

	@DataBoundSetter
	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	public String getVersionName() {
		return versionName;
	}

	@DataBoundSetter
	public void setVersionName(String versionName) {
		this.versionName = versionName;
	}

	public boolean isApplicationNameOverrideAllowed() {
		return applicationNameOverrideAllowed;
	}

	protected void setApplicationNameOverrideAllowed(boolean applicationNameOverrideAllowed) {
		this.applicationNameOverrideAllowed = applicationNameOverrideAllowed;
	}

	public boolean isVersionNameOverrideAllowed() {
		return versionNameOverrideAllowed;
	}

	protected void setVersionNameOverrideAllowed(boolean versionNameOverrideAllowed) {
		this.versionNameOverrideAllowed = versionNameOverrideAllowed;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
	
	public abstract static class AbstractFortifySSCApplicationAndVersionNameConfigDescriptor<T extends AbstractFortifySSCApplicationAndVersionNameConfig<T>> extends AbstractInstanceOrDefaultDescriptor<T> {
        @Override
        public final String getPropertyName() {
        	return "applicationAndVersionNameConfig";
        }
	}
}