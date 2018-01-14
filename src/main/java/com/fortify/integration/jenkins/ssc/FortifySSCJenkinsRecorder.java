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

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.fortify.integration.jenkins.ssc.describable.FortifySSCApplicationAndVersionNameJobConfig;
import com.fortify.integration.jenkins.ssc.describable.FortifySSCCreateApplicationVersionJobConfig;
import com.fortify.integration.jenkins.ssc.describable.FortifySSCUploadFPRJobConfig;
import com.fortify.integration.jenkins.ssc.describable.IFortifySSCPerformWithApplicationAndVersionNameJobConfig;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

public class FortifySSCJenkinsRecorder extends Recorder {
	private FortifySSCApplicationAndVersionNameJobConfig applicationAndVersionNameConfig;
	private FortifySSCCreateApplicationVersionJobConfig createApplicationVersionConfig;
	private FortifySSCUploadFPRJobConfig uploadFPRConfig;

	@DataBoundConstructor
	public FortifySSCJenkinsRecorder() {
	}

	public FortifySSCApplicationAndVersionNameJobConfig getApplicationAndVersionNameConfig() {
		System.out.println(applicationAndVersionNameConfig);
		return applicationAndVersionNameConfig;
	}

	@DataBoundSetter
	public void setApplicationAndVersionNameConfig(
			FortifySSCApplicationAndVersionNameJobConfig applicationAndVersionNameConfig) {
		this.applicationAndVersionNameConfig = applicationAndVersionNameConfig;
	}

	public FortifySSCCreateApplicationVersionJobConfig getCreateApplicationVersionConfig() {
		System.out.println(createApplicationVersionConfig);
		return createApplicationVersionConfig;
	}

	@DataBoundSetter
	public void setCreateApplicationVersionConfig(
			FortifySSCCreateApplicationVersionJobConfig createApplicationVersionConfig) {
		this.createApplicationVersionConfig = createApplicationVersionConfig;
	}

	public FortifySSCUploadFPRJobConfig getUploadFPRConfig() {
		System.out.println(uploadFPRConfig);
		return uploadFPRConfig;
	}

	@DataBoundSetter
	public void setUploadFPRConfig(FortifySSCUploadFPRJobConfig uploadFPRConfig) {
		this.uploadFPRConfig = uploadFPRConfig;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
			throws InterruptedException, IOException {
		PrintStream log = listener.getLogger();
		log.println(
				"HPE Security Fortify Jenkins plugin: " + FortifySSCGlobalConfiguration.get().conn().getBaseResource());
		perform(build, launcher, listener, getCreateApplicationVersionConfig(), getUploadFPRConfig());
		return true;
	}

	private void perform(AbstractBuild<?, ?> build, Launcher launcher, TaskListener listener,
			IFortifySSCPerformWithApplicationAndVersionNameJobConfig... performers)
			throws InterruptedException, IOException {
		FilePath workspace = build.getWorkspace();
		if (workspace == null) {
			throw new AbortException("no workspace for " + build);
		}
		for (IFortifySSCPerformWithApplicationAndVersionNameJobConfig performer : performers) {
			if (performer != null) {
				performer.perform(getApplicationAndVersionNameConfig(), build, workspace, launcher, listener);
			}
		}
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

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

		public static final List<Descriptor<?>> getEnabledDescriptors() {
			return FortifySSCGlobalConfiguration.get().getEnabledJobDescriptors();
		}
	}

}
