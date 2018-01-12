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
package com.fortify.integration.jenkins.ssc;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.fortify.client.ssc.api.SSCApplicationAPI;
import com.fortify.client.ssc.api.SSCApplicationVersionAPI;
import com.fortify.client.ssc.api.SSCArtifactAPI;
import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;
import com.fortify.integration.jenkins.ssc.config.FortifySSCApplicationVersionNameConfig;
import com.fortify.integration.jenkins.ssc.config.FortifySSCConfiguration;
import com.fortify.util.rest.json.JSONMap;
import com.fortify.util.rest.json.processor.AbstractJSONMapProcessor;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.ComboBoxModel;
import jenkins.tasks.SimpleBuildStep;

public class FortifySSCJenkinsRecorder extends Recorder implements SimpleBuildStep {
	private String applicationName;
	private String versionName;

	@DataBoundConstructor
	public FortifySSCJenkinsRecorder() {
		// TODO Auto-generated constructor stub
	}

	public String getApplicationName() {
		return StringUtils.isNotBlank(applicationName) ? applicationName : getDescriptor().getDefaultApplicationName();
	}

	@DataBoundSetter
	public void setApplicationName(String applicationName) {
		checkApplicationVersionNameOverrideEnabled(applicationName, getDescriptor().getDefaultApplicationName());
		this.applicationName = applicationName;
	}

	public String getVersionName() {
		return StringUtils.isNotBlank(versionName) ? versionName : getDescriptor().getDefaultVersionName();
	}

	@DataBoundSetter
	public void setVersionName(String versionName) {
		checkApplicationVersionNameOverrideEnabled(versionName, getDescriptor().getDefaultVersionName());
		this.versionName = versionName;
	}
	
	private void checkApplicationVersionNameOverrideEnabled(String value, String defaultValue) {
		if ( !getDescriptor().isDefaultApplicationVersionNameOverrideAllowed() && !defaultValue.equals(value) ) {
			throw new IllegalArgumentException("Application/version name may not be overridden in build job settings");
		}
	}

	@Override
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
		PrintStream log = listener.getLogger();
		log.println("HPE Security Fortify Jenkins plugin: " + FortifySSCConfiguration.get().getSscUrl());
		EnvVars env = run.getEnvironment(listener);
		SSCAuthenticatingRestConnection conn = FortifySSCConfiguration.get().conn();
		final String applicationVersionId = getApplicationVersionId(env, conn);
		FilePath[] list = workspace.list("**/*.fpr");
		if ( list.length == 0 ) {
			log.println("No FPR file found");
		} else if ( list.length > 1 ) {
			log.println("More than 1 FPR file found");
		} else {
			final SSCArtifactAPI artifactApi = conn.api(SSCArtifactAPI.class);
			String artifactId = list[0].act(new FileCallable<String>() {
				private static final long serialVersionUID = 1L;
				@Override
				public void checkRoles(RoleChecker checker) throws SecurityException {}

				@Override
				public String invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
					return artifactApi.uploadArtifactAndWaitProcessingCompletion(applicationVersionId, f, 60);
				}
			});
			log.println(artifactApi.getArtifactById(artifactId, false));
		}
	}

	private String getApplicationVersionId(EnvVars env, SSCAuthenticatingRestConnection conn) throws AbortException {
		String applicationName = env.expand(getApplicationName());
		String versionName = env.expand(getVersionName());
		JSONMap applicationVersion = conn.api(SSCApplicationVersionAPI.class).queryApplicationVersions()
			.applicationName(applicationName).versionName(versionName).build().getUnique();
		if ( applicationVersion == null ) {
			if ( FortifySSCConfiguration.get().getApplicationVersionCreationConfig() == null ) {
				throw new AbortException("Application version "+applicationName+":"+versionName+" not found");
			} else {
				throw new AbortException("Creating new application versions not yet implemented"); // TODO
			}
		}
		return applicationVersion.get("id", String.class);
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Symbol("fortifySSC") //TODO How do we make this symbol available for pipeline jobs?
	@Extension
	public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

		@SuppressWarnings("rawtypes")
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return "Fortify SSC Jenkins Plugin";
		}

		public ComboBoxModel doFillApplicationNameItems() {
			final ComboBoxModel items = new ComboBoxModel();
			SSCAuthenticatingRestConnection conn = FortifySSCConfiguration.get().conn();
			conn.api(SSCApplicationAPI.class).queryApplications().build()
					.processAll(new AddNamesAndIdsToComboBoxModel(items));
			return items;
		}
		
		public String getDefaultApplicationName() {
			FortifySSCApplicationVersionNameConfig config = FortifySSCConfiguration.get().getApplicationVersionNameConfig();
			return config==null ? null : config.getDefaultApplicationName();	
		}

		public ComboBoxModel doFillVersionNameItems(@QueryParameter String applicationName) {
			final ComboBoxModel items = new ComboBoxModel();
			SSCAuthenticatingRestConnection conn = FortifySSCConfiguration.get().conn();
			conn.api(SSCApplicationVersionAPI.class).queryApplicationVersions().applicationName(applicationName).build()
					.processAll(new AddNamesAndIdsToComboBoxModel(items));
			return items;
		}
		
		public String getDefaultVersionName() {
			FortifySSCApplicationVersionNameConfig config = FortifySSCConfiguration.get().getApplicationVersionNameConfig();
			return config==null ? null : config.getDefaultVersionName();	
		}
		
		public boolean isDefaultApplicationVersionNameOverrideAllowed() {
			FortifySSCApplicationVersionNameConfig config = FortifySSCConfiguration.get().getApplicationVersionNameConfig();
			return config == null || config.isDefaultApplicationVersionNameOverrideAllowed();
		}

		private static final class AddNamesAndIdsToComboBoxModel extends AbstractJSONMapProcessor {
			private final ComboBoxModel items;

			public AddNamesAndIdsToComboBoxModel(ComboBoxModel items) {
				this.items = items;
			}

			@Override
			public void process(JSONMap json) {
				items.add(json.get("name", String.class));
			}
		}
	}

	

}
