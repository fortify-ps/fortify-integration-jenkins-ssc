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

import java.io.PrintStream;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.fortify.client.ssc.api.SSCApplicationAPI;
import com.fortify.client.ssc.api.SSCApplicationVersionAPI;
import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;
import com.fortify.integration.jenkins.ssc.FortifySSCGlobalConfiguration;
import com.fortify.integration.jenkins.ssc.json.processor.AddNamesToComboBoxModel;
import com.fortify.util.rest.json.JSONMap;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.util.ComboBoxModel;

public class FortifySSCDescribableApplicationAndVersionName extends AbstractFortifySSCDescribableStatic {
	private static final long serialVersionUID = 1L;
	private String applicationName = "";
	private String versionName = "";
	
	/**
	 * Default constructor
	 */
	@DataBoundConstructor
	public FortifySSCDescribableApplicationAndVersionName() {}
	
	/**
	 * Copy constructor
	 * @param other
	 */
	public FortifySSCDescribableApplicationAndVersionName(FortifySSCDescribableApplicationAndVersionName other) {
		if ( other != null ) {
			setApplicationName(other.getApplicationName());
			setVersionName(other.getVersionName());
		}
	}
	
	public String getApplicationName() {
		return getPropertyValueOrDefaultValueIfOverrideDisallowed("applicationName", applicationName);
	}
	
	private String getApplicationNameWithLog(PrintStream log) {
		return getPropertyValueOrDefaultValueIfOverrideDisallowed(log, "applicationName", applicationName);
	}

	@DataBoundSetter
	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	public String getVersionName() {
		return getPropertyValueOrDefaultValueIfOverrideDisallowed("versionName", versionName);
	}
	
	private String getVersionNameWithLog(PrintStream log) {
		return getPropertyValueOrDefaultValueIfOverrideDisallowed(log, "versionName", versionName);
	}

	@DataBoundSetter
	public void setVersionName(String versionName) {
		this.versionName = versionName;
	}
	
	public JSONMap getApplicationVersion(EnvVars env, PrintStream log, boolean failIfNotFound) throws AbortException {
		SSCAuthenticatingRestConnection conn = FortifySSCGlobalConfiguration.get().conn();
		String applicationName = getExpandedApplicationName(env, log);
		String versionName = getExpandedVersionName(env, log);
		checkNotBlank(applicationName, "Application name cannot be blank");
		checkNotBlank(versionName, "Version name cannot be blank");
		JSONMap applicationVersion = conn.api(SSCApplicationVersionAPI.class).queryApplicationVersions()
			.applicationName(applicationName).versionName(versionName).useCache(true).build().getUnique();
		if ( applicationVersion == null && failIfNotFound ) {
			throw new AbortException("Application version "+applicationName+":"+versionName+" not found");
		}
		return applicationVersion;
	}
	
	public String getApplicationVersionId(EnvVars env, PrintStream log) throws AbortException {
		return getApplicationVersion(env, log, true).get("id", String.class);
	}

	public String getExpandedVersionName(EnvVars env, PrintStream log) {
		return env.expand(getVersionNameWithLog(log));
	}

	public String getExpandedApplicationName(EnvVars env, PrintStream log) {
		return env.expand(getApplicationNameWithLog(log));
	}
	
	private void checkNotBlank(String stringToCheck, String messageIfBlank) throws AbortException {
		if ( StringUtils.isBlank(stringToCheck) ) {
			throw new AbortException(messageIfBlank);
		}
	}
	
	@Extension
	public static final class FortifySSCDescriptorApplicationAndVersionName extends AbstractFortifySSCDescriptorStatic {
		@Override
		protected FortifySSCDescribableApplicationAndVersionName getDefaultConfiguration() {
			return (FortifySSCDescribableApplicationAndVersionName)super.getDefaultConfiguration();
		}
		
        public ComboBoxModel doFillApplicationNameItems() {
			final ComboBoxModel items = new ComboBoxModel();
			SSCAuthenticatingRestConnection conn = FortifySSCGlobalConfiguration.get().conn();
			conn.api(SSCApplicationAPI.class).queryApplications().build()
					.processAll(new AddNamesToComboBoxModel(items));
			return items;
		}

		public ComboBoxModel doFillVersionNameItems(@QueryParameter String applicationName) {
			final ComboBoxModel items = new ComboBoxModel();
			if ( StringUtils.isNotBlank(applicationName) && !applicationName.contains("${") ) {
				SSCAuthenticatingRestConnection conn = FortifySSCGlobalConfiguration.get().conn();
				conn.api(SSCApplicationVersionAPI.class).queryApplicationVersions().applicationName(applicationName).build()
						.processAll(new AddNamesToComboBoxModel(items));
			}
			return items;
		}
		
		@Override
		public String getDisplayName() {
			// TODO Internationalize this
			return "Configure application and version name";
		}
		
		@Override
		public FortifySSCDescribableApplicationAndVersionName createDefaultInstance() {
			return new FortifySSCDescribableApplicationAndVersionName();
		}
		
		@Override
		public int getOrder() {
			return 100;
		}
    }
}
