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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.jenkinsci.Symbol;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.fortify.client.ssc.api.SSCArtifactAPI;
import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;
import com.fortify.integration.jenkins.multiaction.AbstractDescribableActionGlobal;
import com.fortify.integration.jenkins.ssc.FortifySSCGlobalConfiguration;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;

public class FortifySSCDescribableActionUploadFPRJob extends AbstractFortifySSCDescribableActionJob<FortifySSCDescribableActionUploadFPRJob> {
	private String fprAntFilter = "";
	private int processingTimeOutSeconds = 60;
	
	/**
	 * Default constructor
	 */
	@DataBoundConstructor
	public FortifySSCDescribableActionUploadFPRJob() {}
	
	/**
	 * Initialize with global config 
	 * @param other
	 */
	public FortifySSCDescribableActionUploadFPRJob(FortifySSCDescribableActionUploadFPRGlobal globalConfig) {
		if ( globalConfig != null ) {
			setFprAntFilter(globalConfig.getFprAntFilter());
			setProcessingTimeOutSeconds(globalConfig.getProcessingTimeOutSeconds());
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
	public void perform(FortifySSCDescribableApplicationAndVersionNameJob applicationAndVersionNameJobConfig, Run<?, ?> run,
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
	
	private static final FortifySSCDescribableActionUploadFPRGlobal getGlobalConfig() {
		return FortifySSCGlobalConfiguration.get().getGlobalConfig(FortifySSCDescribableActionUploadFPRGlobal.class);
	}
	
	@Symbol("sscUploadFPR")
	@Extension
	public static final class FortifySSCUploadFPRJobConfigDescriptor extends AbstractFortifySSCDescriptorActionJob<FortifySSCDescribableActionUploadFPRJob> {
		@Override
		public FortifySSCDescribableActionUploadFPRJob createDefaultInstance() {
			return new FortifySSCDescribableActionUploadFPRJob(getGlobalConfig());
		}
		
		@Override
		public String getDisplayName() {
			// TODO Internationalize this
			return "Upload FPR file";
		}
		
		@Override
		public Class<? extends AbstractDescribableActionGlobal<?>> getGlobalConfigClass() {
			return FortifySSCDescribableActionCreateApplicationVersionGlobal.class;
		}
		
		@Override
		public int getOrder() {
			return FortifySSCDescribableActionUploadFPRGlobal.ORDER;
		}
    }
}