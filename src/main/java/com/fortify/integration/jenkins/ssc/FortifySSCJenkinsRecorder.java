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
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.fortify.integration.jenkins.ssc.describable.AbstractFortifySSCBuildStepDescriptor;
import com.fortify.integration.jenkins.ssc.describable.AbstractFortifySSCJobConfigWithApplicationVersionAction;
import com.fortify.integration.jenkins.ssc.describable.FortifySSCApplicationAndVersionNameJobConfig;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

public class FortifySSCJenkinsRecorder extends Recorder {
	private FortifySSCApplicationAndVersionNameJobConfig applicationAndVersionNameConfig;
	private List<AbstractFortifySSCJobConfigWithApplicationVersionAction<?>> actions;

	@DataBoundConstructor
	public FortifySSCJenkinsRecorder() {
		setActions(new ArrayList<>());
	}

	public FortifySSCApplicationAndVersionNameJobConfig getApplicationAndVersionNameConfig() {
		System.out.println(applicationAndVersionNameConfig);
		return applicationAndVersionNameConfig;
	}

	@DataBoundSetter
	public void setApplicationAndVersionNameConfig(FortifySSCApplicationAndVersionNameJobConfig applicationAndVersionNameConfig) {
		this.applicationAndVersionNameConfig = applicationAndVersionNameConfig;
	}

	public List<AbstractFortifySSCJobConfigWithApplicationVersionAction<?>> getActions() {
		return actions;
	}

	@DataBoundSetter
	public void setActions(List<AbstractFortifySSCJobConfigWithApplicationVersionAction<?>> actions) {
		this.actions = actions;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
		PrintStream log = listener.getLogger();
		log.println(
				"HPE Security Fortify Jenkins plugin: " + FortifySSCGlobalConfiguration.get().conn().getBaseResource());
		FilePath workspace = build.getWorkspace();
		if (workspace == null) {
			throw new AbortException("no workspace for " + build);
		}
		for (AbstractFortifySSCJobConfigWithApplicationVersionAction<?> performer : getActions()) {
			if (performer != null) {
				performer.perform(getApplicationAndVersionNameConfig(), build, workspace, launcher, listener);
			}
		}
		return true;
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
	public static class DescriptorImpl extends AbstractFortifySSCBuildStepDescriptor<Publisher> {

		@SuppressWarnings("rawtypes")
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return "Fortify SSC Jenkins Plugin";
		}

		public final List<Descriptor<?>> getEnabledDescriptors() {
			System.out.println("getEnabledDescriptors");
			return FortifySSCGlobalConfiguration.get().getEnabledJobDescriptors();
		}

		@Override
		public final FortifySSCJenkinsRecorder createDefaultInstance() {
			System.out.println("createDefaultInstance");
			return new FortifySSCJenkinsRecorder();
		}
		
		public final Class<?> getTargetType() {
			System.out.println("getTargetType");
			return AbstractFortifySSCJobConfigWithApplicationVersionAction.class;
		}
	}

}
