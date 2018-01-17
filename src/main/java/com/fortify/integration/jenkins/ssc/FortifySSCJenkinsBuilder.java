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

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.fortify.integration.jenkins.multiaction.AbstractMultiActionBuilder;
import com.fortify.integration.jenkins.multiaction.AbstractMultiActionGlobalConfiguration;
import com.fortify.integration.jenkins.ssc.describable.FortifySSCDescribableApplicationAndVersionName;
import com.fortify.integration.jenkins.ssc.describable.action.AbstractFortifySSCConfigurableDescribable;
import com.fortify.integration.jenkins.ssc.describable.action.AbstractFortifySSCDescribableAction;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;

public class FortifySSCJenkinsBuilder extends AbstractMultiActionBuilder<AbstractFortifySSCDescribableAction<?,?>> {
	// 'with' is a pretty strange name for this field, but it actually looks nice
	// in pipeline jobs: performSSCAction( with: [applicationName: 'x', ...] actions: [...]) 
	private FortifySSCDescribableApplicationAndVersionName with;
	
	@DataBoundConstructor
	public FortifySSCJenkinsBuilder() {}

	public FortifySSCDescribableApplicationAndVersionName getWith() {
		return with==null 
				? new FortifySSCDescribableApplicationAndVersionName(FortifySSCGlobalConfiguration.get().getApplicationAndVersionNameConfig())
				: with;
	}

	@DataBoundSetter
	public void setWith(FortifySSCDescribableApplicationAndVersionName with) {
		this.with = with;
	}

	@Override
	protected void perform(AbstractFortifySSCDescribableAction<?,?> action, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
		action.perform(with, build, workspace, launcher, listener);
	}
	
	@Override
	protected String getStartPerformMessage() {
		return "HPE Security Fortify Jenkins plugin: " + FortifySSCGlobalConfiguration.get().conn().getBaseUrl();
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
	
		@Override
		public final FortifySSCJenkinsBuilder createDefaultInstance() {
			return new FortifySSCJenkinsBuilder();
		}
		
		@Override
		protected AbstractMultiActionGlobalConfiguration<?> getMultiActionGlobalConfiguration() {
			return FortifySSCGlobalConfiguration.get();
		}
		
		public final Class<?> getTargetType() {
			return AbstractFortifySSCConfigurableDescribable.class;
		}
	}

}
