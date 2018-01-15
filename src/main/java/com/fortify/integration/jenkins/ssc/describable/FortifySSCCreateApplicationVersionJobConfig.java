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

import java.io.IOException;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import com.fortify.integration.jenkins.ssc.FortifySSCGlobalConfiguration;
import com.fortify.util.rest.json.JSONMap;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;

// TODO Override set* methods to check whether values are being overridden when not allowed
// TODO Don't display if global configuration disallows creating application versions
public class FortifySSCCreateApplicationVersionJobConfig 
	extends AbstractFortifySSCCreateApplicationVersionConfig<FortifySSCCreateApplicationVersionJobConfig>
	implements IFortifySSCPerformWithApplicationAndVersionNameJobConfig
{
	/**
	 * Default constructor
	 */
	@DataBoundConstructor
	public FortifySSCCreateApplicationVersionJobConfig() {
		FortifySSCCreateApplicationVersionGlobalConfig config = FortifySSCGlobalConfiguration.get().getCreateApplicationVersionConfig();
		if ( config != null ) {
			super.setIssueTemplateNameOverrideAllowed(config.isIssueTemplateNameOverrideAllowed());
		}
	}
	
	/**
	 * Initialize with global config 
	 * @param other
	 */
	public FortifySSCCreateApplicationVersionJobConfig(FortifySSCCreateApplicationVersionGlobalConfig globalConfig) {
		this();
		if ( globalConfig != null ) {
			setIssueTemplateName(globalConfig.getIssueTemplateName());
		}
	}
	
	@Override
	public boolean isIssueTemplateNameOverrideAllowed() {
		// Allow override if we either were previously configured to allow override, or if current global config allows override
		FortifySSCCreateApplicationVersionGlobalConfig globalConfig = FortifySSCGlobalConfiguration.get().getCreateApplicationVersionConfig();
		return super.isIssueTemplateNameOverrideAllowed() || globalConfig==null || globalConfig.isIssueTemplateNameOverrideAllowed();
	}
	
	@Override
	public void perform(FortifySSCApplicationAndVersionNameJobConfig applicationAndVersionNameJobConfig, Run<?, ?> run,
			FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException 
	{
		EnvVars env = run.getEnvironment(listener);
		JSONMap applicationVersion = applicationAndVersionNameJobConfig.getApplicationVersion(env, false);
		if ( applicationVersion == null ) {
			createApplicationVersion(applicationAndVersionNameJobConfig.getExpandedApplicationName(env), applicationAndVersionNameJobConfig.getExpandedVersionName(env));
		}
	}
	
	private void createApplicationVersion(String expandedApplicationName, String expandedVersionName) throws AbortException {
		throw new AbortException("Creating application versions not yet implemented");
	}

	@Symbol("sscCreateApplicationVersionIfNotExisting")
	@Extension
	public static final class FortifySSCCreateApplicationVersionJobConfigDescriptor extends AbstractFortifySSCCreateApplicationVersionConfigDescriptor<FortifySSCCreateApplicationVersionJobConfig> {
		@Override
		public FortifySSCCreateApplicationVersionJobConfig createDefaultInstance() {
			return new FortifySSCCreateApplicationVersionJobConfig(FortifySSCGlobalConfiguration.get().getCreateApplicationVersionConfig());
		}
		
		@Override
		public String getDisplayName() {
			// TODO Internationalize this
			return "Create application version if it does not yet exist";
		}
	}
}
