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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.jenkinsci.Symbol;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.fortify.client.ssc.api.SSCArtifactAPI;
import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;
import com.fortify.integration.jenkins.ssc.FortifySSCGlobalConfiguration;
import com.fortify.integration.jenkins.ssc.describable.FortifySSCDescribableApplicationAndVersionName;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;

public class FortifySSCDescribableUploadFPRAction extends AbstractFortifySSCDescribableAction<FortifySSCDescribableUploadFPRAction> {
	private static final long serialVersionUID = 1L;
	private String fprAntFilter = "**/*.fpr";
	private int processingTimeOutSeconds = 600;
	
	/**
	 * Default constructor
	 */
	@DataBoundConstructor
	public FortifySSCDescribableUploadFPRAction() {}
	
	/**
	 * Copy constructor
	 * @param other
	 */
	public FortifySSCDescribableUploadFPRAction(FortifySSCDescribableUploadFPRAction other) {
		if ( other != null ) {
			setFprAntFilter(other.getFprAntFilter());
			setProcessingTimeOutSeconds(other.getProcessingTimeOutSeconds());
		}
	}
	
	public String getFprAntFilter() {
		return fprAntFilter;
	}

	@DataBoundSetter
	public void setFprAntFilter(String fprAntFilter) {
		this.fprAntFilter = fprAntFilter;
	}

	public int getProcessingTimeOutSeconds() {
		return processingTimeOutSeconds;
	}

	@DataBoundSetter
	public void setProcessingTimeOutSeconds(int processingTimeOutSeconds) {
		this.processingTimeOutSeconds = processingTimeOutSeconds;
	}

	@Override
	public void perform(FortifySSCDescribableApplicationAndVersionName applicationAndVersionNameJobConfig, Run<?, ?> run,
			FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
		FortifySSCGlobalConfiguration.get().checkEnabled(this.getDescriptor());
		PrintStream log = listener.getLogger();
		EnvVars env = run.getEnvironment(listener);
		SSCAuthenticatingRestConnection conn = FortifySSCGlobalConfiguration.get().conn();
		final String applicationVersionId = applicationAndVersionNameJobConfig.getApplicationVersionId(env);
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
	
	private static final FortifySSCDescribableUploadFPRAction getDefaultConfig() {
		return FortifySSCGlobalConfiguration.get().getDefaultConfig(FortifySSCDescribableUploadFPRAction.class);
	}
	
	@Symbol("sscUploadFPR")
	@Extension
	public static final class FortifySSCDescriptorUploadFPRAction extends AbstractFortifySSCDescriptorAction<FortifySSCDescribableUploadFPRAction> {
		static final String DISPLAY_NAME = "Upload FPR file";

		@Override
		public FortifySSCDescribableUploadFPRAction createDefaultInstanceWithConfiguration() {
			return new FortifySSCDescribableUploadFPRAction(getDefaultConfig());
		}
		
		@Override
		public FortifySSCDescribableUploadFPRAction createDefaultInstance() {
			return new FortifySSCDescribableUploadFPRAction();
		}
		
		@Override
		public String getDisplayName() {
			// TODO Internationalize this
			return DISPLAY_NAME;
		}
		
		@Override
		public int getOrder() {
			return 200;
		}
    }
}
