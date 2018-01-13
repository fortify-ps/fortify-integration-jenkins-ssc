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

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.fortify.client.ssc.api.SSCApplicationAPI;
import com.fortify.client.ssc.api.SSCApplicationVersionAPI;
import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;
import com.fortify.integration.jenkins.ssc.FortifySSCGlobalConfiguration;
import com.fortify.integration.jenkins.ssc.json.processor.AddNamesToComboBoxModel;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.ComboBoxModel;

// TODO Override set* methods to check whether values are being overridden when not allowed
public class FortifySSCApplicationAndVersionNameJobConfig extends AbstractFortifySSCApplicationAndVersionNameConfig<FortifySSCApplicationAndVersionNameJobConfig> {
	/**
	 * Default constructor
	 */
	@DataBoundConstructor
	public FortifySSCApplicationAndVersionNameJobConfig() {
		FortifySSCApplicationAndVersionNameGlobalConfig config = FortifySSCGlobalConfiguration.get().getApplicationAndVersionNameConfig();
		if ( config != null ) {
			super.setApplicationNameOverrideAllowed(config.isApplicationNameOverrideAllowed());
			super.setVersionNameOverrideAllowed(config.isVersionNameOverrideAllowed());
		}
	}
	
	/**
	 * Initialize with global config 
	 * @param other
	 */
	public FortifySSCApplicationAndVersionNameJobConfig(FortifySSCApplicationAndVersionNameGlobalConfig globalConfig) {
		this();
		if ( globalConfig != null ) {
			setApplicationName(globalConfig.getApplicationName());
			setVersionName(globalConfig.getVersionName());
		}
	}
	
	@Override
	public boolean isApplicationNameOverrideAllowed() {
		// Allow override if we either were previously configured to allow override, or if current global config allows override
		FortifySSCApplicationAndVersionNameGlobalConfig globalConfig = FortifySSCGlobalConfiguration.get().getApplicationAndVersionNameConfig();
		return super.isApplicationNameOverrideAllowed() || globalConfig==null || globalConfig.isApplicationNameOverrideAllowed();
	}
	
	@Override
	public boolean isVersionNameOverrideAllowed() {
		// Allow override if we either were previously configured to allow override, or if current global config allows override
		FortifySSCApplicationAndVersionNameGlobalConfig globalConfig = FortifySSCGlobalConfiguration.get().getApplicationAndVersionNameConfig();
		return super.isVersionNameOverrideAllowed() || globalConfig==null || globalConfig.isVersionNameOverrideAllowed();
	}
	
	@Extension
	public static final class DescriptorImpl extends Descriptor<FortifySSCApplicationAndVersionNameJobConfig> {
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
		
		public FortifySSCApplicationAndVersionNameJobConfig getInstanceOrDefault(FortifySSCApplicationAndVersionNameJobConfig instance) {
			return instance != null ? instance : new FortifySSCApplicationAndVersionNameJobConfig(FortifySSCGlobalConfiguration.get().getApplicationAndVersionNameConfig());
		}
    }
	

}
