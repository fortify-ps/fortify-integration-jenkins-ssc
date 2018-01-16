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
package com.fortify.integration.jenkins.ssc.describable.action;

import java.io.IOException;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.fortify.client.ssc.api.SSCIssueTemplateAPI;
import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;
import com.fortify.integration.jenkins.ssc.FortifySSCGlobalConfiguration;
import com.fortify.integration.jenkins.ssc.describable.FortifySSCDescribableApplicationAndVersionName;
import com.fortify.util.rest.json.JSONList;
import com.fortify.util.rest.json.JSONMap;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;

// TODO Override set* methods to check whether values are being overridden when not allowed
// TODO Don't display if global configuration disallows creating application versions
public class FortifySSCDescribableCreateApplicationVersionAction extends AbstractFortifySSCDescribableAction<FortifySSCDescribableCreateApplicationVersionAction> {
	private static final long serialVersionUID = 1L;
	private String issueTemplateName = null;
	
	/**
	 * Default constructor
	 */
	@DataBoundConstructor
	public FortifySSCDescribableCreateApplicationVersionAction() {}
	
	/**
	 * Copy constructor
	 * @param other
	 */
	public FortifySSCDescribableCreateApplicationVersionAction(FortifySSCDescribableCreateApplicationVersionAction other) {
		if ( other != null ) {
			setIssueTemplateName(other.getIssueTemplateName());
		}
	}
	
	public String getIssueTemplateName() {
		return isIssueTemplateNameOverrideAllowed() ? issueTemplateName : getDefaultConfig().getIssueTemplateName();
	}

	@DataBoundSetter
	public void setIssueTemplateName(String issueTemplateName) {
		this.issueTemplateName = issueTemplateName;
	}
	
	public boolean isIssueTemplateNameOverrideAllowed() {
		// Allow override if we either were previously configured to allow override, or if current global config allows override
		//FortifySSCDescribableActionCreateApplicationVersionGlobal globalConfig = getGlobalConfig();
		//return globalConfig==null || globalConfig.isOverrideAllowed("issueTemplateName");
		// TODO
		return true;
	}
	
	@Override
	public void perform(FortifySSCDescribableApplicationAndVersionName applicationAndVersionNameJobConfig, Run<?, ?> run,
			FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException 
	{
		FortifySSCGlobalConfiguration.get().checkEnabled(this.getDescriptor());
		EnvVars env = run.getEnvironment(listener);
		JSONMap applicationVersion = applicationAndVersionNameJobConfig.getApplicationVersion(env, false);
		if ( applicationVersion == null ) {
			createApplicationVersion(applicationAndVersionNameJobConfig.getExpandedApplicationName(env), applicationAndVersionNameJobConfig.getExpandedVersionName(env));
		}
	}
	
	private void createApplicationVersion(String expandedApplicationName, String expandedVersionName) throws AbortException {
		throw new AbortException("Creating application versions not yet implemented");
	}
	
	private static final FortifySSCDescribableCreateApplicationVersionAction getDefaultConfig() {
		return FortifySSCGlobalConfiguration.get().getDefaultConfig(FortifySSCDescribableCreateApplicationVersionAction.class);
	}

	@Symbol("sscCreateApplicationVersionIfNotExisting")
	@Extension
	public static final class FortifySSCDescriptorCreateApplicationVersionAction extends AbstractFortifySSCDescriptorAction<FortifySSCDescribableCreateApplicationVersionAction> {
		static final String DISPLAY_NAME = "Create application version if it does not yet exist";

		@Override
		public FortifySSCDescribableCreateApplicationVersionAction createDefaultInstanceWithConfiguration() {
			return new FortifySSCDescribableCreateApplicationVersionAction(getDefaultConfig());
		}
		
		@Override
		public FortifySSCDescribableCreateApplicationVersionAction createDefaultInstance() {
			return new FortifySSCDescribableCreateApplicationVersionAction();
		}
		
		@Override
		public String getDisplayName() {
			// TODO Internationalize this
			return DISPLAY_NAME;
		}
		
		public ListBoxModel doFillIssueTemplateNameItems() {
			final ListBoxModel items = new ListBoxModel();
			JSONList issueTemplates = getIssueTemplates();
			for ( JSONMap issueTemplate : issueTemplates.asValueType(JSONMap.class) ) {
				items.add(issueTemplate.get("name", String.class));
			}
			return items;
		}
        
        public String getDefaultIssueTemplateName() {
        	return getIssueTemplates().mapValue("defaultTemplate", true, "name", String.class);
        }
        
        protected JSONList getIssueTemplates() {
			SSCAuthenticatingRestConnection conn = FortifySSCGlobalConfiguration.get().conn();
			return conn.api(SSCIssueTemplateAPI.class).getIssueTemplates(true);
		}
		
		@Override
		public int getOrder() {
			return 100;
		}
	}
}
