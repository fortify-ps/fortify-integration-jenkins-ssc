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
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import com.fortify.integration.jenkins.multiaction.AbstractMultiActionBuilder;
import com.fortify.integration.jenkins.multiaction.AbstractMultiActionConfigurableDescribable;
import com.fortify.integration.jenkins.multiaction.AbstractMultiActionConfigurableDescribable.AbstractMultiActionConfigurableDescriptor;
import com.fortify.integration.jenkins.multiaction.AbstractMultiActionGlobalConfiguration;
import com.fortify.integration.jenkins.ssc.describable.AbstractFortifySSCDescribableStatic.AbstractFortifySSCDescriptorStatic;
import com.fortify.integration.jenkins.ssc.describable.FortifySSCDescribableApplicationAndVersionName;
import com.fortify.integration.jenkins.ssc.describable.action.AbstractFortifySSCDescribableAction;
import com.fortify.integration.jenkins.ssc.describable.action.AbstractFortifySSCDescribableAction.AbstractFortifySSCDescriptorAction;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.DescribableList;
import net.sf.json.JSONObject;

public class FortifySSCJenkinsBuilder extends AbstractMultiActionBuilder {
	@DataBoundConstructor
	public FortifySSCJenkinsBuilder() {}

	public FortifySSCDescribableApplicationAndVersionName getWith() {
		return getStaticJobConfiguration(FortifySSCDescribableApplicationAndVersionName.class);
	}

	// 'with' is a pretty strange name for this field, but it actually looks nice
	// in pipeline jobs: performSSCAction( with: [applicationName: 'x', ...] actions: [...]) 
	@DataBoundSetter
	public void setWith(FortifySSCDescribableApplicationAndVersionName with) throws IOException {
		setStaticJobConfigurationsList(Arrays.asList(with));;
	}
	
	public final DescribableList<AbstractMultiActionConfigurableDescribable, AbstractMultiActionConfigurableDescriptor> getActions() {
		return getDynamicJobConfigurationsList();
	}
	
	@DataBoundSetter
	public void setActions(List<? extends AbstractMultiActionConfigurableDescribable> dynamicJobConfigurations) throws IOException {
		System.out.println("setActions: "+dynamicJobConfigurations);
		setDynamicJobConfigurationsList(dynamicJobConfigurations);
	}
	
	@Override
	public void perform(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
		PrintStream log = listener.getLogger();
		if ( StringUtils.isNotBlank(getStartPerformMessage()) ) {
			log.println(getStartPerformMessage());
		}
		// TODO Move this to AbstractFortifySSCJobConfigWithApplicationVersionAction implementations that actually
		//      need a workspace
		if (workspace == null) { 
			throw new AbortException("no workspace for " + build);
		}
		for ( AbstractMultiActionConfigurableDescribable action : getDynamicJobConfigurationsList()) {
			if (action != null) {
				perform(action, build, workspace, launcher, listener);
			}
		}
	}

	protected void perform(AbstractMultiActionConfigurableDescribable action, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
		if ( action instanceof AbstractFortifySSCDescribableAction ) {
			((AbstractFortifySSCDescribableAction)action).performWithCheck(getWith(), build, workspace, launcher, listener);
		}
	}
	
	private String getStartPerformMessage() {
		return "HPE Security Fortify Jenkins plugin: " + FortifySSCGlobalConfiguration.get().conn().getBaseUrl();
	}
	
	@Override
	public void save() throws IOException {
		// TODO Auto-generated method stub
	}
	
	@Symbol("sscPerformActions")
	@Extension
	public static class DescriptorImpl extends AbstractDescriptorMultiActionBuilder {
	
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
	    public String getDynamicJobConfigurationAddButtonDisplayName() {
			return "Enable Action";
		}
		
	    @Override
		public String getDynamicJobConfigurationDeleteButtonDisplayName() {
			return "Disable Action";
		}
	
		@Override
		public final FortifySSCJenkinsBuilder createDefaultInstance() {
			return new FortifySSCJenkinsBuilder();
		}
		
		@Override
		protected AbstractMultiActionGlobalConfiguration<?> getMultiActionGlobalConfiguration() {
			return FortifySSCGlobalConfiguration.get();
		}

		@SuppressWarnings("unchecked")
		@Override
		protected Class<AbstractFortifySSCDescriptorAction> getDynamicJobConfigurationDescriptorType() {
			return AbstractFortifySSCDescriptorAction.class;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected Class<AbstractFortifySSCDescriptorStatic> getStaticJobConfigurationDescriptorType() {
			return AbstractFortifySSCDescriptorStatic.class;
		}
		
		@Override
		protected boolean includeDynamicConfigurationDescriptorsWithoutGlobalConfiguration() {
			return false;
		}
		
		@Override
		public boolean configure(StaplerRequest req, JSONObject json) throws hudson.model.Descriptor.FormException {
			System.out.println(json.toString(2));
			return super.configure(req, json);
		}
	}
}
