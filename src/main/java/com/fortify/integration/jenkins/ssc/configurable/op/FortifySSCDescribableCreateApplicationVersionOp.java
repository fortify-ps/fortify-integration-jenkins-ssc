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
package com.fortify.integration.jenkins.ssc.configurable.op;

import java.io.IOException;
import java.io.PrintStream;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.fortify.client.ssc.api.SSCIssueTemplateAPI;
import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;
import com.fortify.integration.jenkins.configurable.ModelHelper;
import com.fortify.integration.jenkins.ssc.FortifySSCGlobalConfiguration;
import com.fortify.integration.jenkins.ssc.configurable.FortifySSCDescribableApplicationAndVersionName;
import com.fortify.util.rest.json.JSONList;
import com.fortify.util.rest.json.JSONMap;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Describable;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;

// TODO Override set* methods to check whether values are being overridden when not allowed
// TODO Don't display if global configuration disallows creating application versions
public class FortifySSCDescribableCreateApplicationVersionOp extends AbstractFortifySSCDescribableOp {
	private static final long serialVersionUID = 1L;
	private String issueTemplateName = null;
	
	/**
	 * Default constructor
	 */
	@DataBoundConstructor
	public FortifySSCDescribableCreateApplicationVersionOp() {}
	
	/**
	 * Copy constructor
	 * @param other
	 */
	public FortifySSCDescribableCreateApplicationVersionOp(FortifySSCDescribableCreateApplicationVersionOp other) {
		if ( other != null ) {
			setIssueTemplateName(other.getIssueTemplateName());
		}
	}
	
	public String getIssueTemplateName() {
		return getIssueTemplateNameWithLog(null);
	}
	
	private String getIssueTemplateNameWithLog(PrintStream log) {
		return getPropertyValueOrDefaultValueIfOverrideDisallowed(log, "issueTemplateName", issueTemplateName);
	}

	@DataBoundSetter
	public void setIssueTemplateName(String issueTemplateName) {
		this.issueTemplateName = issueTemplateName;
	}
	
	@Override
	public void perform(FortifySSCDescribableApplicationAndVersionName applicationAndVersionNameJobConfig, Run<?, ?> run,
			FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException 
	{
		EnvVars env = run.getEnvironment(listener);
		PrintStream log = listener.getLogger();
		JSONMap applicationVersion = applicationAndVersionNameJobConfig.getApplicationVersion(env, log, false);
		if ( applicationVersion == null ) {
			createApplicationVersion(log, applicationAndVersionNameJobConfig.getExpandedApplicationName(env, log), applicationAndVersionNameJobConfig.getExpandedVersionName(env, log));
		}
	}
	
	private void createApplicationVersion(PrintStream log, String expandedApplicationName, String expandedVersionName) throws AbortException {
		String issueTemplateName = getIssueTemplateNameWithLog(log);
		throw new AbortException("Creating application versions not yet implemented");
	}

	@Symbol("sscCreateApplicationVersionIfNotExisting")
	@Extension
	public static final class FortifySSCDescriptorCreateApplicationVersionOp extends AbstractFortifySSCDescriptorOp {
		static final String DISPLAY_NAME = "Create application version if it does not yet exist";

		@Override
		public FortifySSCDescribableCreateApplicationVersionOp createDefaultInstanceWithConfiguration() {
			return new FortifySSCDescribableCreateApplicationVersionOp(getDefaultConfiguration());
		}
		
		@Override
		public FortifySSCDescribableCreateApplicationVersionOp createDefaultInstance() {
			return new FortifySSCDescribableCreateApplicationVersionOp();
		}
		
		@Override
		protected FortifySSCDescribableCreateApplicationVersionOp getDefaultConfiguration() {
			return (FortifySSCDescribableCreateApplicationVersionOp)super.getDefaultConfiguration();
		}
		
		@Override
		protected Class<? extends Describable<?>> getGlobalConfigurationTargetType() {
			return FortifySSCDescribableCreateApplicationVersionOp.class;
		}
		
		@Override
		public String getDisplayName() {
			// TODO Internationalize this
			return DISPLAY_NAME;
		}
		
		public ListBoxModel doFillIssueTemplateNameItems(@QueryParameter String isGlobalConfig) {
			final ListBoxModel items = ModelHelper.createListBoxModel("true".equals(isGlobalConfig));
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
			return conn == null ? new JSONList() : conn.api(SSCIssueTemplateAPI.class).getIssueTemplates(true);
		}
		
		@Override
		public int getOrder() {
			return 100;
		}
	}
}
