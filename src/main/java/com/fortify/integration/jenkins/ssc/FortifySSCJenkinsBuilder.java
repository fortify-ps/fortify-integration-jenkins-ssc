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
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.fortify.integration.jenkins.configurable.AbstractConfigurableBuilder;
import com.fortify.integration.jenkins.configurable.AbstractConfigurableDescribable;
import com.fortify.integration.jenkins.configurable.AbstractConfigurableDescribable.AbstractDescriptorConfigurableDescribable;
import com.fortify.integration.jenkins.configurable.AbstractConfigurableDescribableWithErrorHandler.ErrorData;
import com.fortify.integration.jenkins.configurable.AbstractConfigurableGlobalConfiguration;
import com.fortify.integration.jenkins.ssc.configurable.FortifySSCDescribableApplicationAndVersionName;
import com.fortify.integration.jenkins.ssc.configurable.FortifySSCDescribableApplicationAndVersionName.FortifySSCDescriptorApplicationAndVersionName;
import com.fortify.integration.jenkins.ssc.configurable.op.AbstractFortifySSCDescribableOp;
import com.fortify.integration.jenkins.ssc.configurable.op.AbstractFortifySSCDescribableOp.AbstractFortifySSCDescriptorOp;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.DescribableList;

public class FortifySSCJenkinsBuilder extends AbstractConfigurableBuilder {
	@DataBoundConstructor
	public FortifySSCJenkinsBuilder() {}

	public FortifySSCDescribableApplicationAndVersionName getWith() {
		return getStaticJobConfiguration(FortifySSCDescribableApplicationAndVersionName.class);
	}

	// 'with' is a pretty strange name for this field, but it actually looks nice
	// in pipeline jobs: performOnSSC( with: [applicationName: 'x', ...] ops: [...]) 
	@DataBoundSetter
	public void setWith(FortifySSCDescribableApplicationAndVersionName with) throws IOException {
		setStaticJobConfigurationsList(Arrays.asList(with));;
	}
	
	public final DescribableList<AbstractConfigurableDescribable, AbstractDescriptorConfigurableDescribable> getOps() {
		return getDynamicJobConfigurationsList();
	}
	
	@DataBoundSetter
	public void setOps(List<? extends AbstractConfigurableDescribable> dynamicJobConfigurations) throws IOException {
		setDynamicJobConfigurationsList(dynamicJobConfigurations);
	}
	
	@Override
	public void perform(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
		PrintStream log = listener.getLogger();
		if ( StringUtils.isNotBlank(getStartPerformMessage()) ) {
			log.println(getStartPerformMessage());
		}
		ErrorData currentErrorData = new ErrorData();
		for ( AbstractConfigurableDescribable describable : getDynamicJobConfigurationsList()) {
			if (describable != null) {
				if ( !perform(describable, build, workspace, launcher, listener, currentErrorData) ) { break; }
			}
		}
		currentErrorData.markBuild(build);
	}

	/**
	 * Perform the given operation
	 * @param describable
	 * @param build
	 * @param workspace
	 * @param launcher
	 * @param listener
	 * @param currentErrorData
	 * @return true if we can continue with the next operation, false otherwise
	 * @throws InterruptedException
	 * @throws IOException
	 */
	protected boolean perform(AbstractConfigurableDescribable describable, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener, ErrorData currentErrorData) throws InterruptedException, IOException {
		PrintStream log = listener.getLogger();
		if ( describable instanceof AbstractFortifySSCDescribableOp ) {
			AbstractFortifySSCDescribableOp op = (AbstractFortifySSCDescribableOp)describable;
			try {
				op.performWithCheck(getWith(), build, workspace, launcher, listener);
			} catch ( Exception e ) {
				return !op.handleException(log, e, currentErrorData);
			}
		}
		return true;
	}
	
	private String getStartPerformMessage() {
		return "HPE Security Fortify Jenkins plugin: " + FortifySSCGlobalConfiguration.get().conn().getBaseUrl();
	}
	
	@Symbol("performOnSSC")
	@Extension
	public static class DescriptorImpl extends AbstractDescriptorConfigurableBuilder {
	
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
			return "Enable Operation";
		}
		
	    @Override
		public String getDynamicJobConfigurationDeleteButtonDisplayName() {
			return "Disable Operation";
		}
	
		@Override
		public final FortifySSCJenkinsBuilder createDefaultInstance() {
			return new FortifySSCJenkinsBuilder();
		}
		
		@Override
		protected AbstractConfigurableGlobalConfiguration<?> getConfigurableGlobalConfiguration() {
			return FortifySSCGlobalConfiguration.get();
		}
		
		@SuppressWarnings("unchecked") // TODO How to fix this warning?
		@Override
		protected Collection<Class<? extends AbstractDescriptorConfigurableDescribable>> getDynamicJobConfigurationDescriptorTypes() {
			return Arrays.asList(AbstractFortifySSCDescriptorOp.class);
		}

		@SuppressWarnings("unchecked") // TODO How to fix this warning?
		@Override
		protected Collection<Class<? extends AbstractDescriptorConfigurableDescribable>> getStaticJobConfigurationDescriptorTypes() {
			return Arrays.asList(FortifySSCDescriptorApplicationAndVersionName.class);
		}
		
		@Override
		protected boolean includeDynamicConfigurationDescriptorsWithoutGlobalConfiguration() {
			return false;
		}
	}

	@Override
	public void save() throws IOException {
		// TODO Auto-generated method stub
	}
}
