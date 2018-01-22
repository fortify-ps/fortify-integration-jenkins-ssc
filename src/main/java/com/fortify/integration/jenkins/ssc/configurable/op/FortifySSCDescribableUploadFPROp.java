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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.jenkinsci.Symbol;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.fortify.client.ssc.api.SSCArtifactAPI;
import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;
import com.fortify.integration.jenkins.configurable.ModelHelper;
import com.fortify.integration.jenkins.ssc.FortifySSCGlobalConfiguration;
import com.fortify.integration.jenkins.ssc.configurable.FortifySSCDescribableApplicationAndVersionName;
import com.fortify.util.rest.json.JSONMap;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.model.Describable;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import hudson.util.ComboBoxModel;

public class FortifySSCDescribableUploadFPROp extends AbstractFortifySSCDescribableOp {
	private static final long serialVersionUID = 1L;
	private String fprAntFilter = "**/*.fpr";
	private String processingTimeOutSeconds = "600";
	private String autoApprove = null;
	
	/**
	 * Default constructor
	 */
	@DataBoundConstructor
	public FortifySSCDescribableUploadFPROp() {}
	
	/**
	 * Copy constructor
	 * @param other
	 */
	public FortifySSCDescribableUploadFPROp(FortifySSCDescribableUploadFPROp other) {
		if ( other != null ) {
			setFprAntFilter(other.getFprAntFilter());
			setProcessingTimeOutSeconds(other.getProcessingTimeOutSeconds());
		}
	}
	
	public String getFprAntFilter() {
		return getExpandedFprAntFilter(null, null);
	}
	
	private String getExpandedFprAntFilter(PrintStream log, EnvVars env) {
		return getExpandedPropertyValueOrDefaultValueIfOverrideDisallowed(log, env, "fprAntFilter", fprAntFilter);
	}

	@DataBoundSetter
	public void setFprAntFilter(String fprAntFilter) {
		this.fprAntFilter = fprAntFilter;
	}

	public String getProcessingTimeOutSeconds() {
		return getExpandedProcessingTimeOutSeconds(null, null);
	}
	
	private String getExpandedProcessingTimeOutSeconds(PrintStream log, EnvVars env) {
		return getExpandedPropertyValueOrDefaultValueIfOverrideDisallowed(log, env, "processingTimeOutSeconds", processingTimeOutSeconds);
	}

	@DataBoundSetter
	public void setProcessingTimeOutSeconds(String processingTimeOutSeconds) {
		this.processingTimeOutSeconds = processingTimeOutSeconds;
	}

	public String getAutoApprove() {
		return getExpandedAutoApprove(null,null);
	}
	
	private String getExpandedAutoApprove(PrintStream log, EnvVars env) {
		return getExpandedPropertyValueOrDefaultValueIfOverrideDisallowed(log, env, "autoApprove", autoApprove);
	}

	@DataBoundSetter
	public void setAutoApprove(String autoApprove) {
		this.autoApprove = autoApprove;
	}

	@Override
	public void perform(FortifySSCDescribableApplicationAndVersionName applicationAndVersionNameJobConfig, Run<?, ?> run,
			FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
		PrintStream log = listener.getLogger();
		EnvVars env = run.getEnvironment(listener);
		SSCAuthenticatingRestConnection conn = FortifySSCGlobalConfiguration.get().conn();
		
		final String applicationVersionId = applicationAndVersionNameJobConfig.getApplicationVersionId(log, env);
		final FilePath fprFilePath = getFPRFilePath(workspace, log, env);
		//TODO Check really an int
		final int processingTimeoutSeconds = Integer.parseInt(getExpandedProcessingTimeOutSeconds(log, env));  
		final String autoApprove = getExpandedAutoApprove(log, env);
		
		final SSCArtifactAPI artifactApi = conn.api(SSCArtifactAPI.class);
		String artifactId = fprFilePath.act(new FileCallable<String>() {
			private static final long serialVersionUID = 1L;
			@Override
			public void checkRoles(RoleChecker checker) throws SecurityException {}

			@Override
			public String invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
				if ( "true".equals(autoApprove) ) {
					return artifactApi.uploadArtifactAndWaitProcessingCompletionWithApproval(applicationVersionId, f, "Auto-approved by Jenkins", processingTimeoutSeconds);
				} else {
					return artifactApi.uploadArtifactAndWaitProcessingCompletion(applicationVersionId, f, processingTimeoutSeconds);
				}
			}
		});
		JSONMap artifact = artifactApi.getArtifactById(artifactId, true);
		if ( processingTimeoutSeconds>0 && !"PROCESS_COMPLETE".equals(artifact.get("status")) ) {
			throw new AbortException("Artifact was uploaded but not processed");
		}
	}

	private FilePath getFPRFilePath(FilePath workspace, PrintStream log, EnvVars env) throws IOException, InterruptedException {
		String fprAntFilter = getExpandedFprAntFilter(log, env);
		FilePath[] list = workspace.list(fprAntFilter);
		if ( list.length == 0 ) {
			throw new AbortException("No FPR file found with filter '"+fprAntFilter+"'");
		} else if ( list.length > 1 ) {
			throw new AbortException("More than 1 FPR file found with filter '"+fprAntFilter+"'");
		} else {
			return list[0];
		}
	}
	
	@Symbol("sscUploadFPR")
	@Extension
	public static final class FortifySSCDescriptorUploadFPROp extends AbstractFortifySSCDescriptorOp {
		static final String DISPLAY_NAME = "Upload FPR file";

		@Override
		public FortifySSCDescribableUploadFPROp createDefaultInstanceWithConfiguration() {
			return new FortifySSCDescribableUploadFPROp(getDefaultConfiguration());
		}
		
		@Override
		public FortifySSCDescribableUploadFPROp createDefaultInstance() {
			return new FortifySSCDescribableUploadFPROp();
		}
		
		@Override
		protected FortifySSCDescribableUploadFPROp getDefaultConfiguration() {
			return (FortifySSCDescribableUploadFPROp)super.getDefaultConfiguration();
		}
		
		@Override
		protected Class<? extends Describable<?>> getGlobalConfigurationTargetType() {
			return FortifySSCDescribableUploadFPROp.class;
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
		
		public ComboBoxModel doFillAutoApproveItems() {
			return ModelHelper.createBooleanComboBoxModel();
		}
    }
}
