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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import com.fortify.integration.jenkins.ssc.describable.FortifySSCApplicationAndVersionNameJobConfig;
import com.fortify.integration.jenkins.ssc.describable.IFortifySSCPerformWithApplicationAndVersionNameJobConfig;
import com.google.common.collect.ImmutableSet;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import net.sf.json.JSONObject;

public class FortifySSCStep extends Step {
	private FortifySSCApplicationAndVersionNameJobConfig applicationAndVersionNameConfig = new FortifySSCApplicationAndVersionNameJobConfig();
	private IFortifySSCPerformWithApplicationAndVersionNameJobConfig[] actions;
	
	@DataBoundConstructor
	public FortifySSCStep() {}
	
	public String getApplicationName() {
		return applicationAndVersionNameConfig.getApplicationName();
	}

	@DataBoundSetter
	public void setApplicationName(String applicationName) {
		applicationAndVersionNameConfig.setApplicationName(applicationName);
	}

	public String getVersionName() {
		return applicationAndVersionNameConfig.getVersionName();
	}

	@DataBoundSetter
	public void setVersionName(String versionName) {
		applicationAndVersionNameConfig.setVersionName(versionName);
	}
	
	public IFortifySSCPerformWithApplicationAndVersionNameJobConfig[] getActions() {
		return actions;
	}

	@DataBoundSetter
	public void setActions(IFortifySSCPerformWithApplicationAndVersionNameJobConfig[] actions) {
		this.actions = actions;
	}

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new ExecutionImpl(context, this);
	}

	public void perform(Run<?, ?> run, FilePath workspace, TaskListener listener, Launcher launcher) throws InterruptedException, IOException {
		System.out.println(applicationAndVersionNameConfig);
		if ( actions != null ) {
			for ( IFortifySSCPerformWithApplicationAndVersionNameJobConfig action : actions ) {
				System.out.println(action);
				action.perform(applicationAndVersionNameConfig, run, workspace, launcher, listener);
			}
		}
		System.out.println(run.getEnvironment(listener));
	}

	private static class ExecutionImpl extends SynchronousNonBlockingStepExecution<Void> {
		private static final long serialVersionUID = 1L;
		private transient FortifySSCStep step;
		
		public ExecutionImpl(StepContext context, FortifySSCStep step) {
			super(context);
			this.step = step;
		}
		
		@Override
        protected Void run() throws Exception {
			StepContext c = getContext();
			step.perform(c.get(Run.class), c.get(FilePath.class), c.get(TaskListener.class), c.get(Launcher.class));
            return null;
        }
	}

	@Extension
	public static final class DescriptorImpl extends StepDescriptor {
		@Override
		public String getDisplayName() {
			return "Perform Fortify SSC action";
		}

		@Override
		public String getFunctionName() {
			return "sscPerformActions";
		}

		@Override
		public Set<Class<?>> getRequiredContext() {
			return ImmutableSet.of(Run.class, FilePath.class, TaskListener.class, Launcher.class);
		}
		
		public final List<Descriptor<?>> getEnabledDescriptors() {
			return FortifySSCGlobalConfiguration.get().getEnabledJobDescriptors();
		}
		
		// TODO Remove this method if not used in jelly.config
		public final Map<Descriptor<?>, Describable<?>> getEnabledDescriptorInstances() {
			Map<Descriptor<?>, Describable<?>> result = new HashMap<>();
			for ( Descriptor<?> descriptor : getEnabledDescriptors() ) {
				result.put(descriptor, null);
			}
			return result;
		}
	}
}
