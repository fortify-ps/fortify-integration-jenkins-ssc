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
import java.util.function.Predicate;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.kohsuke.stapler.StaplerRequest;
import org.springframework.core.OrderComparator;

import com.fortify.integration.jenkins.multiaction.AbstractMultiActionConfigurableDescribable.AbstractMultiActionConfigurableDescriptor;

import hudson.ExtensionList;
import hudson.model.Saveable;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.util.DescribableList;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

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

	protected void setDynamicJobConfigurationsList(List<? extends AbstractMultiActionConfigurableDescribable> dynamicJobConfigurations) throws IOException {
		System.out.println("setDynamicJobConfigurationsList: "+dynamicJobConfigurations);
		getDynamicJobConfigurationsList().replaceBy(dynamicJobConfigurations);
	}

	protected void setStaticJobConfigurationsList(List<? extends AbstractMultiActionConfigurableDescribable> staticJobConfigurations) throws IOException {
		System.out.println("setStaticJobConfigurationsList: "+staticJobConfigurations);
		getStaticJobConfigurationsList().replaceBy(staticJobConfigurations);
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
	
	public static abstract class AbstractDescriptorMultiActionBuilder extends BuildStepDescriptor<Builder> {

		public final AbstractMultiActionBuilder getInstanceOrDefault(AbstractMultiActionBuilder instance) {
			AbstractMultiActionBuilder result = instance!=null ? instance : createDefaultInstance();
			System.out.println(this.getClass().getSimpleName()+".getInstanceOrDefault: "+result);
			return result;
		}
		
		public boolean isShowStaticGlobalConfigurationsList() {
			return true;
		}
		
		public boolean isShowDynamicGlobalConfigurationsList() {
			return true;
		}
		
		public String getDynamicJobConfigurationAddButtonDisplayName() {
			return "Add";
		}
		
		public String getDynamicJobConfigurationDeleteButtonDisplayName() {
			return "Delete";
		}
		
		public final List<? extends AbstractMultiActionConfigurableDescriptor> getAllDynamicJobConfigurationDescriptors() {
			return getAllJobConfigurationDescriptors(getDynamicJobConfigurationDescriptorType(), includeDynamicConfigurationDescriptorsWithoutGlobalConfiguration());
		}

		public final List<? extends AbstractMultiActionConfigurableDescriptor> getAllStaticJobConfigurationDescriptors() {
			return getAllJobConfigurationDescriptors(getStaticJobConfigurationDescriptorType(), includeStaticConfigurationDescriptorsWithoutGlobalConfiguration());
		}

		private final List<? extends AbstractMultiActionConfigurableDescriptor> getAllJobConfigurationDescriptors(Class<? extends AbstractMultiActionConfigurableDescriptor> describableJobConfigurationActionDescriptorType, boolean includeDescriptorsWithoutGlobalConfiguration) {
			ExtensionList<? extends AbstractMultiActionConfigurableDescriptor> list = Jenkins.getInstance().getExtensionList(describableJobConfigurationActionDescriptorType);
			List<? extends AbstractMultiActionConfigurableDescriptor> result = new ArrayList<>(list);
			if ( !includeDescriptorsWithoutGlobalConfiguration ) {
				result.removeIf(new Predicate<AbstractMultiActionConfigurableDescriptor>() {
					@Override
					public boolean test(AbstractMultiActionConfigurableDescriptor d) {
						return !d.isGlobalConfigurationAvailable();
					}
				});
			}
			result.sort(new OrderComparator());
			return result;
		}
		
		protected boolean includeDynamicConfigurationDescriptorsWithoutGlobalConfiguration() {
			return true;
		}
		
		protected boolean includeStaticConfigurationDescriptorsWithoutGlobalConfiguration() {
			return true;
		}
		
		public final Class<?> getTargetType() {
			return AbstractMultiActionConfigurableDescribable.class;
		}
		
		@Override
		public Builder newInstance(StaplerRequest req, JSONObject formData) throws FormException {
			System.out.println("\n\n"+this.getClass().getSimpleName()+"\n"+formData.toString(2));
			AbstractMultiActionBuilder newInstance = (AbstractMultiActionBuilder) super.newInstance(req, formData);
		
			try {
				newInstance.getDynamicJobConfigurationsList().rebuildHetero(req, formData, getAllDynamicJobConfigurationDescriptors(), "dynamicJobConfigurationsList");
			} catch (IOException e) {
				throw new FormException("Error updating configuration", e, "dynamicGlobalConfigurationsList");
			}
			try {
				JSONObject staticJobConfigurationJSON = formData.getJSONObject("staticJobConfigurationsList");
				if ( staticJobConfigurationJSON!=null && !staticJobConfigurationJSON.isNullObject() ) {
					newInstance.getStaticJobConfigurationsList().rebuild(req, staticJobConfigurationJSON, getAllStaticJobConfigurationDescriptors());
				}
			} catch (IOException e) {
				throw new FormException("Error updating configuration", e, "staticGlobalConfigurationsList");
			}
	
			return newInstance;
		}
		
		protected abstract <D extends AbstractMultiActionConfigurableDescriptor> Class<D> getDynamicJobConfigurationDescriptorType();
		protected abstract <D extends AbstractMultiActionConfigurableDescriptor> Class<D> getStaticJobConfigurationDescriptorType();
		
		public abstract AbstractMultiActionBuilder createDefaultInstance();

		protected abstract AbstractMultiActionGlobalConfiguration<?> getMultiActionGlobalConfiguration();
	}

}