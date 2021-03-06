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
package com.fortify.integration.jenkins.ssc.configurable;

import java.io.PrintStream;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.fortify.client.ssc.api.SSCApplicationAPI;
import com.fortify.client.ssc.api.SSCApplicationVersionAPI;
import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;
import com.fortify.integration.jenkins.configurable.AbortWithMessageException;
import com.fortify.integration.jenkins.configurable.AbstractConfigurable;
import com.fortify.integration.jenkins.ssc.FortifySSCGlobalConfiguration;
import com.fortify.integration.jenkins.ssc.json.processor.AddNamesToComboBoxModel;
import com.fortify.util.rest.json.JSONMap;

import hudson.EnvVars;
import hudson.Extension;
import hudson.util.ComboBoxModel;

public class FortifySSCApplicationAndVersionName extends AbstractConfigurable {
	private static final long serialVersionUID = 1L;
	private String applicationName;
	private String versionName;
	
	/**
	 * Default constructor
	 */
	@DataBoundConstructor
	public FortifySSCApplicationAndVersionName() {}
	
	public String getApplicationName() {
		return getExpandedApplicationName(null, null);
	}
	
	public String getExpandedApplicationName(PrintStream log, EnvVars env) {
		return getExpandedPropertyValueOrDefaultValueIfOverrideDisallowed(log, env, "applicationName", applicationName);
	}

	@DataBoundSetter
	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	public String getVersionName() {
		return getExpandedVersionName(null, null);
	}
	
	public String getExpandedVersionName(PrintStream log, EnvVars env) {
		return getExpandedPropertyValueOrDefaultValueIfOverrideDisallowed(log, env, "versionName", versionName);
	}

	@DataBoundSetter
	public void setVersionName(String versionName) {
		this.versionName = versionName;
	}
	
	public JSONMap getApplicationVersion(PrintStream log, EnvVars env) {
		JSONMap result = _getApplicationVersion(log, env, true);
		if ( result == null ) {
			String applicationName = getExpandedApplicationName(log, env);
			String versionName = getExpandedVersionName(log, env);
			throw new AbortWithMessageException("Application version "+applicationName+":"+versionName+" not found");
		}
		return result;
	}
	
	private JSONMap _getApplicationVersion(PrintStream log, EnvVars env, boolean useCache, String... fields) {
		SSCAuthenticatingRestConnection conn = FortifySSCGlobalConfiguration.get().conn();
		String applicationName = getExpandedApplicationName(log, env);
		String versionName = getExpandedVersionName(log, env);
		checkNotBlank(applicationName, "Application name cannot be blank");
		checkNotBlank(versionName, "Version name cannot be blank");
		JSONMap applicationVersion = conn.api(SSCApplicationVersionAPI.class).queryApplicationVersions()
			.applicationName(applicationName).versionName(versionName).useCache(useCache).build().getUnique();
		return applicationVersion;
	}
	
	public boolean doesApplicationVersionExist(PrintStream log, EnvVars env) {
		return _getApplicationVersion(log, env, false, "id")!=null;
	}
	
	public String getApplicationVersionId(PrintStream log, EnvVars env) {
		return getApplicationVersion(log, env).get("id", String.class);
	}
	
	private void checkNotBlank(String stringToCheck, String messageIfBlank) {
		if ( StringUtils.isBlank(stringToCheck) ) {
			throw new AbortWithMessageException(messageIfBlank);
		}
	}
	
	@Extension
	public static final class FortifySSCDescriptorApplicationAndVersionName extends AbstractDescriptorConfigurable {
		@Override
		public String getDisplayName() {
			// TODO Internationalize this
			return "Configure application and version name";
		}
		
        public ComboBoxModel doFillApplicationNameItems(@QueryParameter String refreshApplicationName) {
        	final ComboBoxModel items = new ComboBoxModel();
        	if ( StringUtils.isNotBlank(refreshApplicationName) ) {
	        	try {
					SSCAuthenticatingRestConnection conn = FortifySSCGlobalConfiguration.get().conn();
					if ( conn != null ) {
						conn.api(SSCApplicationAPI.class).queryApplications().build()
								.processAll(new AddNamesToComboBoxModel(items));
					}
	        	} catch ( Exception e ) {
	        		e.printStackTrace();
	        	}
        	}
			return items;
		}

		public ComboBoxModel doFillVersionNameItems(@QueryParameter String refreshVersionName, @QueryParameter String applicationName) {
			final ComboBoxModel items = new ComboBoxModel();
			if ( StringUtils.isNotBlank(refreshVersionName) && StringUtils.isNotBlank(applicationName) && !applicationName.contains("${") ) {
				try {
					SSCAuthenticatingRestConnection conn = FortifySSCGlobalConfiguration.get().conn();
					if ( conn != null ) {
						conn.api(SSCApplicationVersionAPI.class).queryApplicationVersions().applicationName(applicationName).build()
							.processAll(new AddNamesToComboBoxModel(items));
					}
				} catch ( Exception e ) {
					e.printStackTrace(); // TODO log this instead of printing to console?
				}
			}
			return items;
		}
		
		@Override
		public int getOrder() {
			return 100;
		}
    }
}
