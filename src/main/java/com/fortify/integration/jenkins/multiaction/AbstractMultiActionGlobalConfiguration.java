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
import java.util.Collection;
import java.util.List;

import org.kohsuke.stapler.DataBoundSetter;
import org.springframework.core.OrderComparator;

import com.fortify.integration.jenkins.multiaction.AbstractDescribableActionGlobal.AbstractDescriptorActionGlobal;
import com.fortify.integration.jenkins.multiaction.AbstractDescribableActionJob.AbstractDescriptorActionJob;
import com.fortify.integration.jenkins.multiaction.AbstractDescribableJob.AbstractDescriptorJob;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import hudson.AbortException;
import hudson.ExtensionList;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

/**
 * To use, extend this class with correct generics types, and include the following in the
 * config.jelly page for your concrete implementation:
 * 
 * <pre>
 	<f:nested>
    	<f:hetero-list name="enabledActionsGlobalConfigs" items="${descriptor.getInstanceOrDefault(instance).enabledActionsGlobalConfigs}" 
    		descriptors="${descriptor.globalConfigActionDescriptors}" targetType="${descriptor.targetType}" 
    		hasHeader="true" addCaption="${%Enable Action}" deleteCaption="${%Disable Action}" 
    		oneEach="true" honorOrder="true" />
    </f:nested>
 * </pre>
 * 
 * This will automatically load all available DescriptorActionGlobalType instances (based on the class type returned
 * by {@link #getDescriptorActionGlobalType()}), and allow to enable, disable and configure these instances to
 * define the global configuration for this plugin.
 * 
 * @author Ruud Senden
 *
 * @param <DescribableActionGlobalType>
 * @param <DescriptorActionGlobalType>
 * @param <T>
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractMultiActionGlobalConfiguration
		<DescribableActionGlobalType extends AbstractDescribableActionGlobal,
		 DescriptorActionGlobalType extends AbstractDescriptorActionGlobal,
		 T extends AbstractMultiActionGlobalConfiguration<DescribableActionGlobalType, DescriptorActionGlobalType, T>> 
		 extends AbstractGlobalConfiguration<T> 
{
	private ImmutableMap<Class<DescribableActionGlobalType>, DescribableActionGlobalType> enabledActionsGlobalConfigs;

	public AbstractMultiActionGlobalConfiguration() {
		setEnabledActionsGlobalConfigs(getDefaultEnabledActionsGlobalConfigs());
        load();
	}
	
	public Collection<DescribableActionGlobalType> getEnabledActionsGlobalConfigs() {
		return enabledActionsGlobalConfigs.values();
	}

	@DataBoundSetter
	public void setEnabledActionsGlobalConfigs(Collection<DescribableActionGlobalType> enabledActions) {
		this.enabledActionsGlobalConfigs = Maps.uniqueIndex(enabledActions, new Function<DescribableActionGlobalType, Class<DescribableActionGlobalType>> () {
			@Override @SuppressWarnings("unchecked")
			public Class<DescribableActionGlobalType> apply(DescribableActionGlobalType input) {
				return (Class<DescribableActionGlobalType>) input.getClass();
			}
		    
		    });;
	}

	public void checkEnabled(Descriptor<?> descriptor) throws AbortException {
		if ( !isEnabled(descriptor) ) {
			// TODO Replace with something like this if called from pipeline job?
			//      descriptor.getClass().getAnnotation(Symbol.class).value()[0]
			throw new AbortException("Action '"+descriptor.getDisplayName()+"' is not enabled in global configuration");
		}
	}

	public boolean isEnabled(Descriptor<?> descriptor) {
		if ( descriptor instanceof AbstractDescriptorJob<?> ) {
			return enabledActionsGlobalConfigs.containsKey(((AbstractDescriptorActionJob<?>)descriptor).getGlobalConfigClass());
		}
		return false;
	}

	public List<Descriptor<?>> getEnabledJobDescriptors() {
		List<Descriptor<?>> result = Lists.newArrayList(Iterables.transform(enabledActionsGlobalConfigs.values(),
				new Function<DescribableActionGlobalType, Descriptor<?>>() {
					@Override
					public Descriptor<?> apply(DescribableActionGlobalType input) {
						return input.getJobConfigDescriptor();
					}
				}));
		result.sort(new OrderComparator());
		return result;
	}

	public final List<DescriptorActionGlobalType> getGlobalConfigActionDescriptors() {
		ExtensionList<DescriptorActionGlobalType> list = Jenkins.getInstance().getExtensionList(getDescriptorActionGlobalType());
		List<DescriptorActionGlobalType> result = new ArrayList<>(list);
		result.sort(new OrderComparator());
		return result;
	}

	protected abstract Class<DescriptorActionGlobalType> getDescriptorActionGlobalType();

	@SuppressWarnings("unchecked")
	public <GlobalConfig> GlobalConfig getGlobalConfig(Class<GlobalConfig> type) {
		return (GlobalConfig) enabledActionsGlobalConfigs.get(type);
	}

	@SuppressWarnings("unchecked")
	protected Collection<DescribableActionGlobalType> getDefaultEnabledActionsGlobalConfigs() {
		return !enableAllActionsByDefault() 
				? new ArrayList<>()
				: Lists.newArrayList(Iterables.transform(getGlobalConfigActionDescriptors(),
					new Function<AbstractDescriptorActionGlobal, DescribableActionGlobalType>() {
						@Override
						public DescribableActionGlobalType apply(AbstractDescriptorActionGlobal input) {
							return (DescribableActionGlobalType) input.createDefaultInstance();
						}
					}));
	}
	
	protected boolean enableAllActionsByDefault() {
		return true;
	}
}