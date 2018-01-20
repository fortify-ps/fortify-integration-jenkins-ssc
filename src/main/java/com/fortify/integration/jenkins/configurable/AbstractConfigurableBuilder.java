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
package com.fortify.integration.jenkins.configurable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.kohsuke.stapler.StaplerRequest;
import org.springframework.core.OrderComparator;

import com.fortify.integration.jenkins.configurable.AbstractConfigurableDescribable.AbstractDescriptorConfigurableDescribable;

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
public abstract class AbstractConfigurableBuilder extends Builder implements SimpleBuildStep, Saveable {
	private volatile DescribableList<AbstractConfigurableDescribable,AbstractDescriptorConfigurableDescribable> dynamicJobConfigurationsList;
	private volatile DescribableList<AbstractConfigurableDescribable,AbstractDescriptorConfigurableDescribable> staticJobConfigurationsList;
	
	public final DescribableList<AbstractConfigurableDescribable, AbstractDescriptorConfigurableDescribable> getDynamicJobConfigurationsList() {
		if (dynamicJobConfigurationsList == null) {
			dynamicJobConfigurationsList = getDescriptor().addDefaultDynamicJobConfigurationsList(new DescribableList<AbstractConfigurableDescribable,AbstractDescriptorConfigurableDescribable>(this));
        }
		return dynamicJobConfigurationsList;
	}
	
	public final DescribableList<AbstractConfigurableDescribable, AbstractDescriptorConfigurableDescribable> getStaticJobConfigurationsList() {
		if (staticJobConfigurationsList == null) {
			staticJobConfigurationsList = new DescribableList<AbstractConfigurableDescribable,AbstractDescriptorConfigurableDescribable>(this);
        }
		return staticJobConfigurationsList;
	}

	protected void setDynamicJobConfigurationsList(List<? extends AbstractConfigurableDescribable> dynamicJobConfigurations) throws IOException {
		getDynamicJobConfigurationsList().replaceBy(dynamicJobConfigurations);
	}

	protected void setStaticJobConfigurationsList(List<? extends AbstractConfigurableDescribable> staticJobConfigurations) throws IOException {
		getStaticJobConfigurationsList().replaceBy(staticJobConfigurations);
	}
	
	@SuppressWarnings("unchecked")
	protected <R extends AbstractConfigurableDescribable> R getStaticJobConfiguration(Class<R> type) {
		for ( AbstractConfigurableDescribable staticConfigurable : getStaticJobConfigurationsList() ) {
			if ( staticConfigurable!=null && staticConfigurable.getClass().equals(type) ) {
				return (R)staticConfigurable;
			}
		}
		return null;
	}
	
	@Override
	public AbstractDescriptorConfigurableBuilder getDescriptor() {
		return (AbstractDescriptorConfigurableBuilder)super.getDescriptor();
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
	
	public static abstract class AbstractDescriptorConfigurableBuilder extends BuildStepDescriptor<Builder> {

		public final AbstractConfigurableBuilder getInstanceOrDefault(AbstractConfigurableBuilder instance) {
			return instance!=null ? instance : createDefaultInstance();
		}
		
		public boolean isShowStaticJobConfigurationsList() {
			return true;
		}
		
		public boolean isShowDynamicJobConfigurationsList() {
			return true;
		}
		
		public String getDynamicJobConfigurationAddButtonDisplayName() {
			return "Add";
		}
		
		public String getDynamicJobConfigurationDeleteButtonDisplayName() {
			return "Delete";
		}
		
		public final List<? extends AbstractDescriptorConfigurableDescribable> getAllDynamicJobConfigurationDescriptors() {
			return getAllJobConfigurationDescriptors(getDynamicJobConfigurationDescriptorTypes(), includeDynamicConfigurationDescriptorsWithoutGlobalConfiguration());
		}

		public final List<? extends AbstractDescriptorConfigurableDescribable> getAllStaticJobConfigurationDescriptors() {
			return getAllJobConfigurationDescriptors(getStaticJobConfigurationDescriptorTypes(), includeStaticConfigurationDescriptorsWithoutGlobalConfiguration());
		}

		private final <D extends AbstractDescriptorConfigurableDescribable> List<D> getAllJobConfigurationDescriptors(Collection<Class<D>> types, boolean includeDescriptorsWithoutGlobalConfiguration) {
			List<D> result = new ArrayList<>();
			for ( Class<D> type : types ) {
				ExtensionList<D> list = Jenkins.getInstance().getExtensionList(type);
				result.addAll(list);
			}
			if ( !includeDescriptorsWithoutGlobalConfiguration ) {
				result.removeIf(new Predicate<AbstractDescriptorConfigurableDescribable>() {
					@Override
					public boolean test(AbstractDescriptorConfigurableDescribable d) {
						return !d.isGlobalConfigurationAvailable();
					}
				});
			}
			result.sort(new OrderComparator());
			return result;
		}
		
		private DescribableList<AbstractConfigurableDescribable, AbstractDescriptorConfigurableDescribable> addDefaultDynamicJobConfigurationsList(DescribableList<AbstractConfigurableDescribable, AbstractDescriptorConfigurableDescribable> list) {
            for ( AbstractDescriptorConfigurableDescribable descriptor :  getAllDynamicJobConfigurationDescriptors() ) {
                if ( getMultiActionGlobalConfiguration().isEnabledByDefault(descriptor.getGlobalConfigurationTargetType()) ) {
                    list.add(descriptor.createDefaultInstanceWithConfiguration());
                }
            }
            return list;
        }
		
		protected boolean includeDynamicConfigurationDescriptorsWithoutGlobalConfiguration() {
			return true;
		}
		
		protected boolean includeStaticConfigurationDescriptorsWithoutGlobalConfiguration() {
			return true;
		}
		
		public final Class<?> getTargetType() {
			return AbstractConfigurableDescribable.class;
		}
		
		@Override
		public Builder newInstance(StaplerRequest req, JSONObject formData) throws FormException {
			AbstractConfigurableBuilder newInstance = (AbstractConfigurableBuilder) super.newInstance(req, formData);
		
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
		
		protected abstract <D extends AbstractDescriptorConfigurableDescribable> Collection<Class<D>> getDynamicJobConfigurationDescriptorTypes();
		protected abstract <D extends AbstractDescriptorConfigurableDescribable> Collection<Class<D>> getStaticJobConfigurationDescriptorTypes();
		
		public abstract AbstractConfigurableBuilder createDefaultInstance();

		protected abstract AbstractConfigurableGlobalConfiguration<?> getMultiActionGlobalConfiguration();
	}

}