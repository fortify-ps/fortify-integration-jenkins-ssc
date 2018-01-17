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
package com.fortify.integration.jenkins.multiaction;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.kohsuke.stapler.DataBoundSetter;

import com.fortify.integration.jenkins.ssc.FortifySSCGlobalConfiguration;

import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;

/**
 * To use, extend this class with correct generics types, and include the following in the
 * config.jelly page for your concrete implementation:
 * 
 * <pre>
 	<f:nested>
    	<f:hetero-list name="actions" items="${descriptor.getInstanceOrDefault(instance).actions}" 
    		descriptors="${descriptor.enabledDescriptors}" targetType="${descriptor.targetType}" 
    		hasHeader="true" addCaption="${%Add Action}" deleteCaption="${%Delete Action}" 
    		honorOrder="true" />
    </f:nested>
 * </pre>
 * 
 * This will automatically load all actions enabled in the global configuration, 
 * and allow to enable, disable and configure these instances for the current job.
 * 
 * @author Ruud Senden
 *
 * @param <DescribableActionJobType>
 */
public abstract class AbstractMultiActionBuilder<DescribableActionJobType extends AbstractMultiActionConfigurableDescribable<?,?>> extends Builder implements SimpleBuildStep {
	private List<DescribableActionJobType> actions;

	public AbstractMultiActionBuilder() {
		setActions(new ArrayList<>());
	}
	
	public List<DescribableActionJobType> getActions() {
		return actions;
	}

	@DataBoundSetter
	public void setActions(List<DescribableActionJobType> actions) {
		this.actions = actions;
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
		for (DescribableActionJobType action : getActions()) {
			if (action != null) {
				perform(action, build, workspace, launcher, listener);
			}
		}
	}

	protected String getStartPerformMessage() {
		return getDescriptor().getDisplayName();
	}
	
	protected abstract void perform(DescribableActionJobType action, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException;

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
	
	public static abstract class AbstractDescriptorMultiActionBuilder<T extends BuildStep & Describable<T>> extends BuildStepDescriptor<T> {

		public final T getInstanceOrDefault(T instance) {
			T result = instance!=null ? instance : createDefaultInstance();
			System.out.println(this.getClass().getSimpleName()+".getInstanceOrDefault: "+result);
			return result;
		}
		
		public final List<Descriptor<?>> getEnabledDescriptors() {
			return getMultiActionGlobalConfiguration().getEnabledJobDescriptors();
		}
		
		public abstract T createDefaultInstance();

		protected abstract AbstractMultiActionGlobalConfiguration<?> getMultiActionGlobalConfiguration();
	}

}