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
import java.util.List;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.fortify.integration.jenkins.multiaction.AbstractDescribableActionJob;
import com.fortify.integration.jenkins.multiaction.AbstractMultiActionBuilder;
import com.fortify.integration.jenkins.ssc.describable.AbstractFortifySSCDescribableActionJob;
import com.fortify.integration.jenkins.ssc.describable.FortifySSCDescribableApplicationAndVersionNameJob;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;

public class FortifySSCJenkinsBuilder extends AbstractMultiActionBuilder<AbstractFortifySSCDescribableActionJob<?>> {
	// 'with' is a pretty strange name for this field, but it actually looks nice
	// in pipeline jobs: performSSCAction( with: [applicationName: 'x', ...] actions: [...]) 
	private FortifySSCDescribableApplicationAndVersionNameJob with;
	
	@DataBoundConstructor
	public FortifySSCJenkinsBuilder() {}

	public FortifySSCDescribableApplicationAndVersionNameJob getWith() {
		return with==null 
				? new FortifySSCDescribableApplicationAndVersionNameJob(FortifySSCGlobalConfiguration.get().getApplicationAndVersionNameConfig())
				: with;
	}

	@DataBoundSetter
	public void setWith(FortifySSCDescribableApplicationAndVersionNameJob with) {
		this.with = with;
	}

	@Override
	protected void perform(AbstractFortifySSCDescribableActionJob<?> action, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
		action.perform(with, build, workspace, launcher, listener);
	}
	
	@Symbol("sscPerformActions")
	@Extension
	public static class DescriptorImpl extends AbstractDescriptorMultiActionBuilder<Builder> {
	
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
			return AbstractDescribableActionJob.class;
		}
	}

}
