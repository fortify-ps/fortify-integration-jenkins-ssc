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

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.fortify.integration.jenkins.ssc.describable.AbstractFortifySSCBuildStepDescriptor;
import com.fortify.integration.jenkins.ssc.describable.AbstractFortifySSCJobConfigWithApplicationVersionAction;
import com.fortify.integration.jenkins.ssc.describable.FortifySSCApplicationAndVersionNameJobConfig;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;

public class FortifySSCJenkinsBuilder extends Builder implements SimpleBuildStep {
	// 'select' is a pretty strange name for this field, but it actually looks nice
	// in pipeline jobs: performSSCAction select: [applicationName: 'x', ...] actions: [...] 
	private FortifySSCApplicationAndVersionNameJobConfig select;
	private List<AbstractFortifySSCJobConfigWithApplicationVersionAction<?>> actions;

	@DataBoundConstructor
	public FortifySSCJenkinsBuilder() {
		setActions(new ArrayList<>());
	}

	public FortifySSCApplicationAndVersionNameJobConfig getSelect() {
		return select==null 
				? new FortifySSCApplicationAndVersionNameJobConfig(FortifySSCGlobalConfiguration.get().getApplicationAndVersionNameConfig())
				: select;
	}

	@DataBoundSetter
	public void setSelect(FortifySSCApplicationAndVersionNameJobConfig select) {
		this.select = select;
	}

	public List<AbstractFortifySSCJobConfigWithApplicationVersionAction<?>> getActions() {
		return actions;
	}

	@DataBoundSetter
	public void setActions(List<AbstractFortifySSCJobConfigWithApplicationVersionAction<?>> actions) {
		this.actions = actions;
	}

	@Override
	public void perform(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
		PrintStream log = listener.getLogger();
		log.println(
				"HPE Security Fortify Jenkins plugin: " + FortifySSCGlobalConfiguration.get().conn().getBaseResource());
		// TODO Move this to AbstractFortifySSCJobConfigWithApplicationVersionAction implementations that actually
		//      need a workspace
		if (workspace == null) { 
			throw new AbortException("no workspace for " + build);
		}
		for (AbstractFortifySSCJobConfigWithApplicationVersionAction<?> action : getActions()) {
			if (action != null) {
				action.perform(select, build, workspace, launcher, listener);
			}
		}
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Symbol("sscPerformActions")
	@Extension
	public static class DescriptorImpl extends AbstractFortifySSCBuildStepDescriptor<Builder> {

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
		public final FortifySSCJenkinsBuilder createDefaultInstance() {
			System.out.println("createDefaultInstance");
			return new FortifySSCJenkinsBuilder();
		}
		
		public final Class<?> getTargetType() {
			System.out.println("getTargetType");
			return AbstractFortifySSCJobConfigWithApplicationVersionAction.class;
		}
	}

}
