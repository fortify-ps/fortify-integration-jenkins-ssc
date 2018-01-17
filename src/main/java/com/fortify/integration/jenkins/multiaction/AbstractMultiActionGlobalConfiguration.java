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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.kohsuke.stapler.DataBoundSetter;
import org.springframework.core.OrderComparator;

import com.fortify.integration.jenkins.multiaction.AbstractMultiActionConfigurableDescribable.AbstractMultiActionConfigurableDescriptor;
import com.fortify.integration.jenkins.multiaction.AbstractMultiActionDescribableGlobalConfiguration.AbstractMultiActionDescriptorGlobalConfiguration;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import hudson.AbortException;
import hudson.ExtensionList;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

/**
 * To use, extend this class with correct generics types, and include the following in the
 * config.jelly page for your concrete implementation:
 * 
 * <pre>
 	TODO
 * </pre>
 * 
 * This will automatically load all available DescriptorActionJobType instances (based on the class type returned
 * by {@link #getDescribableGlobalConfigurationDescriptorType()}), and allow to enable, disable and configure these instances to
 * define the global configuration for this plugin.
 * 
 * @author Ruud Senden
 *
 */
public abstract class AbstractMultiActionGlobalConfiguration<T extends AbstractMultiActionGlobalConfiguration<T>> extends AbstractGlobalConfiguration<T> 
{
	private List<AbstractMultiActionDescribableGlobalConfiguration<?>> enabledActionsDefaultConfigs;
	private transient Map<Class<Describable<?>>, AbstractMultiActionDescribableGlobalConfiguration<?>> targetTypeToEnabledActionsDefaultConfigsMap;

	public AbstractMultiActionGlobalConfiguration() {
		setEnabledActionsDefaultConfigs(getDefaultEnabledActionsDefaultConfigs());
	}
	
	public List<AbstractMultiActionDescribableGlobalConfiguration<?>> getEnabledActionsDefaultConfigs() {
		return enabledActionsDefaultConfigs;
	}

	@DataBoundSetter
	public void setEnabledActionsDefaultConfigs(List<AbstractMultiActionDescribableGlobalConfiguration<?>> enabledActionsDefaultConfigs) {
		this.enabledActionsDefaultConfigs = enabledActionsDefaultConfigs;
		this.targetTypeToEnabledActionsDefaultConfigsMap = null;
	}
	
	public Map<Class<Describable<?>>, AbstractMultiActionDescribableGlobalConfiguration<?>> getTargetTypeToEnabledActionsDefaultConfigsMap() {
		if ( targetTypeToEnabledActionsDefaultConfigsMap == null ) {
			targetTypeToEnabledActionsDefaultConfigsMap = Maps.uniqueIndex(enabledActionsDefaultConfigs, new Function<AbstractMultiActionDescribableGlobalConfiguration<?>, Class<Describable<?>>> () {
				@Override
				public Class<Describable<?>> apply(AbstractMultiActionDescribableGlobalConfiguration<?> input) {
					return input.getTargetType();
				}
			    
			    });
		};
		return targetTypeToEnabledActionsDefaultConfigsMap;
	}

	public void checkEnabled(Descriptor<?> descriptor) throws AbortException {
		if ( !isEnabled(descriptor) ) {
			// TODO Replace with something like this if called from pipeline job?
			//      descriptor.getClass().getAnnotation(Symbol.class).value()[0]
			throw new AbortException("Action '"+descriptor.getDisplayName()+"' is not enabled in global configuration");
		}
	}

	public boolean isEnabled(Descriptor<?> descriptor) {
		if ( descriptor instanceof AbstractMultiActionConfigurableDescriptor<?,?> ) {
			return getTargetTypeToEnabledActionsDefaultConfigsMap().containsKey(((AbstractMultiActionConfigurableDescriptor<?,?>)descriptor).getGlobalConfigurationTargetType());
		}
		return false;
	}

	public List<Descriptor<?>> getEnabledJobDescriptors() {
		List<Descriptor<?>> result = Lists.newArrayList(Iterables.transform(enabledActionsDefaultConfigs,
				new Function<AbstractMultiActionDescribableGlobalConfiguration<?>, Descriptor<?>>() {
					@Override
					public Descriptor<?> apply(AbstractMultiActionDescribableGlobalConfiguration<?> input) {
						return Jenkins.getInstance().getDescriptorOrDie(input.getTargetType());
					}
				}));
		result.sort(new OrderComparator());
		return result;
	}

	public final List<AbstractMultiActionDescriptorGlobalConfiguration<?>> getAllGlobalConfigurationDescriptors() {
		ExtensionList<AbstractMultiActionDescriptorGlobalConfiguration<?>> list = Jenkins.getInstance().getExtensionList(getDescribableGlobalConfigurationDescriptorType());
		List<AbstractMultiActionDescriptorGlobalConfiguration<?>> result = new ArrayList<>(list);
		result.sort(new OrderComparator());
		return result;
	}

	protected abstract <D extends AbstractMultiActionDescriptorGlobalConfiguration<?>> Class<D> getDescribableGlobalConfigurationDescriptorType();

	@SuppressWarnings("unchecked")
	public <R, C> R getDefaultConfig(Class<C> globalConfigurationType, Class<R> returnType) {
		AbstractMultiActionDescribableGlobalConfiguration<?> config = getTargetTypeToEnabledActionsDefaultConfigsMap().get(globalConfigurationType);
		return config == null ? null : (R)config.getTarget();
	}

	protected List<AbstractMultiActionDescribableGlobalConfiguration<?>> getDefaultEnabledActionsDefaultConfigs() {
		return !enableAllActionsByDefault() 
				? new ArrayList<>() : new ArrayList<>();
		/* TODO re-implement this
				: Lists.newArrayList(Iterables.transform(getAllGlobalConfigurationDescriptors(),
					new Function<AbstractMultiActionDescriptor, MultiActionDescribableType>() {
						@Override
						public MultiActionDescribableType apply(AbstractMultiActionDescriptor input) {
							return (MultiActionDescribableType) input.createDefaultInstance();
						}
					}));
		*/
	}
	
	protected boolean enableAllActionsByDefault() {
		return true;
	}
	
	public final Class<?> getTargetType() {
		return AbstractMultiActionDescribableGlobalConfiguration.class;
	}
}