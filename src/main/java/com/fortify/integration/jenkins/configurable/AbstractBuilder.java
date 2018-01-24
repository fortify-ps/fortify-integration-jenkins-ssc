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
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;
import org.springframework.core.OrderComparator;

import com.fortify.integration.jenkins.configurable.AbstractConfigurable.AbstractDescriptorConfigurable;

import hudson.Extension;
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
 * <p>This is the {@link Builder} counterpart for {@link AbstractGlobalConfiguration}, maintaining
 * lists of static and dynamic {@link AbstractConfigurable} instances, to allow these to be configured
 * on job configuration pages or in pipeline jobs.</p>
 * 
 * <p>Concrete implementations will need to provide getter and setter methods annotated with {@link DataBoundSetter} 
 * to configure the dynamic and static job configurations. Usually these methods have friendly names to be used in 
 * pipeline jobs. In addition, concrete implementations will need to provide a descriptor that extends from 
 * {@link AbstractDescriptorBuilder}.</p>
 * 
 * @author Ruud Senden
 *
 */
public abstract class AbstractBuilder extends Builder implements SimpleBuildStep, Saveable {
	private volatile DescribableList<AbstractConfigurable,AbstractDescriptorConfigurable> dynamicJobConfigurationsList;
	private volatile DescribableList<AbstractConfigurable,AbstractDescriptorConfigurable> staticJobConfigurationsList;
	
	public final DescribableList<AbstractConfigurable, AbstractDescriptorConfigurable> getDynamicJobConfigurationsList() {
		if (dynamicJobConfigurationsList == null) {
			dynamicJobConfigurationsList = getDescriptor().addDefaultDynamicJobConfigurationsList(new DescribableList<AbstractConfigurable,AbstractDescriptorConfigurable>(this));
        }
		return dynamicJobConfigurationsList;
	}
	
	public final DescribableList<AbstractConfigurable, AbstractDescriptorConfigurable> getStaticJobConfigurationsList() {
		if (staticJobConfigurationsList == null) {
			staticJobConfigurationsList = new DescribableList<AbstractConfigurable,AbstractDescriptorConfigurable>(this);
        }
		return staticJobConfigurationsList;
	}

	protected void setDynamicJobConfigurationsList(List<? extends AbstractConfigurable> dynamicJobConfigurations) throws IOException {
		getDynamicJobConfigurationsList().replaceBy(dynamicJobConfigurations);
	}

	protected void setStaticJobConfigurationsList(List<? extends AbstractConfigurable> staticJobConfigurations) throws IOException {
		getStaticJobConfigurationsList().replaceBy(staticJobConfigurations);
	}
	
	@SuppressWarnings("unchecked")
	protected <R extends AbstractConfigurable> R getStaticJobConfiguration(Class<R> type) {
		for ( AbstractConfigurable staticConfigurable : getStaticJobConfigurationsList() ) {
			if ( staticConfigurable!=null && staticConfigurable.getClass().equals(type) ) {
				return (R)staticConfigurable;
			}
		}
		return null;
	}
	
	@Override
	public AbstractDescriptorBuilder getDescriptor() {
		return (AbstractDescriptorBuilder)super.getDescriptor();
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}
	
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
	
	/**
	 * Abstract descriptor implementation for {@link AbstractBuilder} instances.
	 * Concrete implementations will need to extend from this abstract descriptor, and be
	 * annotated with both {@link Symbol} (to allow easy invocation from pipeline jobs) and
	 * {@link Extension}.
	 * 
	 * @author Ruud Senden
	 *
	 */
	public static abstract class AbstractDescriptorBuilder extends BuildStepDescriptor<Builder> {
		public final AbstractBuilder getInstanceOrDefault(AbstractBuilder instance) {
			return instance!=null ? instance : createDefaultInstance();
		}
		
		public AbstractBuilder createDefaultInstance() {
			try {
				return (AbstractBuilder) clazz.newInstance();
			} catch (InstantiationException | IllegalAccessException | SecurityException e) {
				throw new RuntimeException("Error instantiating "+clazz.getName(), e);
			}
		}
		
		public String getDynamicJobConfigurationAddButtonDisplayName() {
			return "Add";
		}
		
		public String getDynamicJobConfigurationDeleteButtonDisplayName() {
			return "Delete";
		}
		
		public final List<? extends AbstractDescriptorConfigurable> getAllDynamicJobConfigurationDescriptors() {
			return getAllJobConfigurationDescriptors(getDynamicJobConfigurationDescriptorTypes(), includeDynamicConfigurationDescriptorsWithoutGlobalConfiguration());
		}

		public final List<? extends AbstractDescriptorConfigurable> getAllStaticJobConfigurationDescriptors() {
			return getAllJobConfigurationDescriptors(getStaticJobConfigurationDescriptorTypes(), includeStaticConfigurationDescriptorsWithoutGlobalConfiguration());
		}

		private final <D extends AbstractDescriptorConfigurable> List<D> getAllJobConfigurationDescriptors(Collection<Class<D>> types, boolean includeDescriptorsWithoutGlobalConfiguration) {
			List<D> result = new ArrayList<>();
			for ( Class<D> type : types ) {
				ExtensionList<D> list = Jenkins.getInstance().getExtensionList(type);
				result.addAll(list);
			}
			if ( !includeDescriptorsWithoutGlobalConfiguration ) {
				result.removeIf(new Predicate<AbstractDescriptorConfigurable>() {
					@Override
					public boolean test(AbstractDescriptorConfigurable d) {
						return !d.isConfigurationAvailable();
					}
				});
			}
			result.sort(new OrderComparator());
			return result;
		}
		
		private DescribableList<AbstractConfigurable, AbstractDescriptorConfigurable> addDefaultDynamicJobConfigurationsList(DescribableList<AbstractConfigurable, AbstractDescriptorConfigurable> list) {
            for ( AbstractDescriptorConfigurable descriptor :  getAllDynamicJobConfigurationDescriptors() ) {
                if ( getConfigurableGlobalConfiguration().isEnabledByDefault(descriptor.getConfigurationTargetType()) ) {
                    list.add(descriptor.createDefaultInstance());
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
			return AbstractConfigurable.class;
		}
		
		@Override
		public Builder newInstance(StaplerRequest req, JSONObject formData) throws FormException {
			AbstractBuilder newInstance = (AbstractBuilder) super.newInstance(req, formData);
		
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
		
		protected abstract <D extends AbstractDescriptorConfigurable> Collection<Class<D>> getDynamicJobConfigurationDescriptorTypes();
		protected abstract <D extends AbstractDescriptorConfigurable> Collection<Class<D>> getStaticJobConfigurationDescriptorTypes();

		protected abstract AbstractGlobalConfiguration<?> getConfigurableGlobalConfiguration();
	}

}