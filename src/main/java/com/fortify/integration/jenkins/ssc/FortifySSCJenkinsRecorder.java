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

import org.jenkinsci.Symbol;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.fortify.client.ssc.api.SSCApplicationVersionAPI;
import com.fortify.client.ssc.api.SSCArtifactAPI;
import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;
import com.fortify.integration.jenkins.ssc.describable.FortifySSCApplicationAndVersionNameJobConfig;
import com.fortify.integration.jenkins.ssc.describable.FortifySSCCreateApplicationVersionJobConfig;
import com.fortify.util.rest.json.JSONMap;

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
import jenkins.tasks.SimpleBuildStep;

public class FortifySSCJenkinsRecorder extends Recorder implements SimpleBuildStep {
	private FortifySSCApplicationAndVersionNameJobConfig applicationAndVersionNameConfig;
	private FortifySSCCreateApplicationVersionJobConfig createApplicationVersionConfig;

	@DataBoundConstructor
	public FortifySSCJenkinsRecorder() {}

	public FortifySSCApplicationAndVersionNameJobConfig getApplicationAndVersionNameConfig() {
		return applicationAndVersionNameConfig;
	}

	@DataBoundSetter
	public void setApplicationAndVersionNameConfig(FortifySSCApplicationAndVersionNameJobConfig applicationAndVersionNameConfig) {
		this.applicationAndVersionNameConfig = applicationAndVersionNameConfig;
	}

	public FortifySSCCreateApplicationVersionJobConfig getCreateApplicationVersionConfig() {
		return createApplicationVersionConfig;
	}

	@DataBoundSetter
	public void setCreateApplicationVersionConfig(FortifySSCCreateApplicationVersionJobConfig createApplicationVersionConfig) {
		this.createApplicationVersionConfig = createApplicationVersionConfig;
	}



	@Override
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
		PrintStream log = listener.getLogger();
		log.println("HPE Security Fortify Jenkins plugin: " + FortifySSCGlobalConfiguration.get().getSscUrl());
		EnvVars env = run.getEnvironment(listener);
		SSCAuthenticatingRestConnection conn = FortifySSCGlobalConfiguration.get().conn();
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
		String applicationName = env.expand(getApplicationAndVersionNameConfig().getApplicationName());
		String versionName = env.expand(getApplicationAndVersionNameConfig().getVersionName());
		JSONMap applicationVersion = conn.api(SSCApplicationVersionAPI.class).queryApplicationVersions()
			.applicationName(applicationName).versionName(versionName).build().getUnique();
		if ( applicationVersion == null ) {
			if ( FortifySSCGlobalConfiguration.get().getCreateApplicationVersionConfig() == null ) {
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
	}

	

}
