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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.kohsuke.stapler.DataBoundSetter;
import org.springframework.core.OrderComparator;

import com.fortify.integration.jenkins.multiaction.AbstractMultiActionConfigurableDescribable.AbstractMultiActionConfigurableDescriptor;

import hudson.ExtensionList;
import hudson.model.Describable;
import hudson.model.Saveable;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.util.DescribableList;
import jenkins.model.Jenkins;
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
public abstract class AbstractMultiActionBuilder extends Builder implements SimpleBuildStep, Saveable {
	private volatile DescribableList<AbstractMultiActionConfigurableDescribable,AbstractMultiActionConfigurableDescriptor> dynamicJobConfigurationsList;
	private volatile DescribableList<AbstractMultiActionConfigurableDescribable,AbstractMultiActionConfigurableDescriptor> staticJobConfigurationsList;
	
	public final DescribableList<AbstractMultiActionConfigurableDescribable, AbstractMultiActionConfigurableDescriptor> getDynamicJobConfigurationsList() {
		if (dynamicJobConfigurationsList == null) {
			dynamicJobConfigurationsList = new DescribableList<AbstractMultiActionConfigurableDescribable,AbstractMultiActionConfigurableDescriptor>(this);
        }
		return dynamicJobConfigurationsList;
	}
	
	public final DescribableList<AbstractMultiActionConfigurableDescribable, AbstractMultiActionConfigurableDescriptor> getStaticJobConfigurationsList() {
		if (staticJobConfigurationsList == null) {
			staticJobConfigurationsList = new DescribableList<AbstractMultiActionConfigurableDescribable,AbstractMultiActionConfigurableDescriptor>(this);
        }
		return staticJobConfigurationsList;
	}

	@DataBoundSetter
	public void setDynamicJobConfigurationsList(List<? extends AbstractMultiActionConfigurableDescribable> dynamicJobConfigurations) throws IOException {
		getDynamicJobConfigurationsList().replaceBy(dynamicJobConfigurations);
	}

	@DataBoundSetter
	public void setStaticJobConfigurationsList(List<? extends AbstractMultiActionConfigurableDescribable> staticJobConfigurations) throws IOException {
		getStaticJobConfigurationsList().replaceBy(staticJobConfigurations);
	}
	
	public final DescribableList<AbstractMultiActionConfigurableDescribable, AbstractMultiActionConfigurableDescriptor> getSortedDynamicJobConfigurationsList() {
		return getSortedDescribableList(getDynamicJobConfigurationsList());
	}
	
	public final DescribableList<AbstractMultiActionConfigurableDescribable, AbstractMultiActionConfigurableDescriptor> getSortedStaticJobConfigurationsList() {
		return getSortedDescribableList(getStaticJobConfigurationsList());
	}

	private DescribableList<AbstractMultiActionConfigurableDescribable, AbstractMultiActionConfigurableDescriptor> getSortedDescribableList(
			DescribableList<AbstractMultiActionConfigurableDescribable, AbstractMultiActionConfigurableDescriptor> original) {
		DescribableList<AbstractMultiActionConfigurableDescribable, AbstractMultiActionConfigurableDescriptor> result = 
				new DescribableList<>(this, original);
		result.sort(new OrderComparator());
		return result;
	}
	
	@SuppressWarnings("unchecked")
	protected <R extends AbstractMultiActionConfigurableDescribable> R getStaticJobConfiguration(Class<R> type) {
		for ( AbstractMultiActionConfigurableDescribable staticConfigurable : getStaticJobConfigurationsList() ) {
			if ( staticConfigurable!=null && staticConfigurable.getClass().equals(type) ) {
				return (R)staticConfigurable;
			}
		}
		return null;
	}

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
		
		public final List<? extends AbstractMultiActionConfigurableDescriptor> getAllDynamicJobConfigurationDescriptors() {
			return getAllJobConfigurationDescriptors(getDynamicJobConfigurationDescriptorType());
		}

		public final List<? extends AbstractMultiActionConfigurableDescriptor> getAllStaticJobConfigurationDescriptors() {
			return getAllJobConfigurationDescriptors(getStaticJobConfigurationDescriptorType());
		}

		private List<? extends AbstractMultiActionConfigurableDescriptor> getAllJobConfigurationDescriptors(Class<? extends AbstractMultiActionConfigurableDescriptor> describableJobConfigurationActionDescriptorType) {
			ExtensionList<? extends AbstractMultiActionConfigurableDescriptor> list = Jenkins.getInstance().getExtensionList(describableJobConfigurationActionDescriptorType);
			List<? extends AbstractMultiActionConfigurableDescriptor> result = new ArrayList<>(list);
			result.sort(new OrderComparator());
			return result;
		}
		
		public final Class<?> getTargetType() {
			return AbstractMultiActionConfigurableDescribable.class;
		}
		
		protected abstract <D extends AbstractMultiActionConfigurableDescriptor> Class<D> getDynamicJobConfigurationDescriptorType();
		protected abstract <D extends AbstractMultiActionConfigurableDescriptor> Class<D> getStaticJobConfigurationDescriptorType();
		
		
		
		public abstract T createDefaultInstance();

		protected abstract AbstractMultiActionGlobalConfiguration<?> getMultiActionGlobalConfiguration();
	}

}