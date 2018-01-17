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

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;
import org.springframework.core.OrderComparator;

import com.fortify.integration.jenkins.multiaction.AbstractMultiActionDescribableGlobalConfiguration.AbstractMultiActionDescriptorGlobalConfiguration;
import com.fortify.integration.jenkins.util.ModelHelper;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import hudson.AbortException;
import hudson.ExtensionList;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.ReflectionUtils;
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
	private transient Map<Class<AbstractMultiActionConfigurableDescribable<?,?>>, AbstractMultiActionDescribableGlobalConfiguration<?>> targetTypeToEnabledActionsDefaultConfigsMap;

	public AbstractMultiActionGlobalConfiguration() {
		setEnabledActionsDefaultConfigs(getDefaultEnabledActionsDefaultConfigs());
	}
	
	public final List<AbstractMultiActionDescribableGlobalConfiguration<?>> getEnabledActionsDefaultConfigs() {
		return enabledActionsDefaultConfigs;
	}

	@DataBoundSetter
	public final void setEnabledActionsDefaultConfigs(List<AbstractMultiActionDescribableGlobalConfiguration<?>> enabledActionsDefaultConfigs) {
		this.enabledActionsDefaultConfigs = enabledActionsDefaultConfigs;
		this.targetTypeToEnabledActionsDefaultConfigsMap = null;
	}

	public final List<Descriptor<?>> getEnabledJobDescriptors() {
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
	
	@SuppressWarnings("unchecked")
	public final void checkEnabled(Class<?> configurableDescribableType) throws AbortException {
		if ( !isEnabled(configurableDescribableType) ) {
			// TODO Replace with something like this if called from pipeline job?
			//      descriptor.getClass().getAnnotation(Symbol.class).value()[0]
			throw new AbortException("Action '"+Jenkins.getInstance().getDescriptorOrDie((Class<? extends Describable<?>>) configurableDescribableType)+"' is not enabled in global configuration");
		}
	}

	public final boolean isEnabled(Class<?> configurableDescribableType) {
		return getTargetTypeToEnabledActionsDefaultConfigsMap().containsKey(configurableDescribableType);
	}

	@SuppressWarnings("unchecked")
	public final <R> R getDefaultConfig(Class<?> configurableDescribableType, Class<R> returnType) {
		AbstractMultiActionDescribableGlobalConfiguration<?> config = getTargetTypeToEnabledActionsDefaultConfigsMap().get(configurableDescribableType);
		return config == null ? null : (R)config.getTarget();
	}
	
	public final boolean isOverrideAllowed(Class<?> configurableDescribableType, String propertyName) {
		AbstractMultiActionDescribableGlobalConfiguration<?> config = getTargetTypeToEnabledActionsDefaultConfigsMap().get(configurableDescribableType);
		if ( config==null ) { return true; }
		if ( config.isAllowOverride() ) { return true; }
		return isGlobalConfigurationPropertyBlankOrNotSpecified(configurableDescribableType, propertyName);
	}
	
	@SuppressWarnings("unchecked")
	public final <V> V getPropertyValueOrDefaultValueIfOverrideDisallowed(Class<?> configurableDescribableType, String propertyName, V currentValue) {
		return isOverrideAllowed(configurableDescribableType, propertyName) 
					? currentValue
					: (V)getGlobalConfigurationPropertyValue(configurableDescribableType, propertyName, Object.class);
	}
	
	public final <V> V getPropertyValueOrDefaultValueIfOverrideDisallowed(Class<?> globalConfigurationClass, PrintStream log, String propertyName, V currentValue) {
		V result = getPropertyValueOrDefaultValueIfOverrideDisallowed(globalConfigurationClass, propertyName, currentValue);
		if ( !ObjectUtils.equals(result, currentValue) ) {
			log.println("WARNING: Using default configuration value '"+result+"' for property "+propertyName+" because override is disabled in global configuration");
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public final <R> R getGlobalConfigurationPropertyValue(Class<?> configurableDescribableType, String propertyName, Class<R> returnType) {
		AbstractMultiActionDescribableGlobalConfiguration<?> config = getTargetTypeToEnabledActionsDefaultConfigsMap().get(configurableDescribableType);
		try {
			return config == null ? null : (R)ReflectionUtils.getPublicProperty(config.getTarget(), propertyName);
		} catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
			throw new RuntimeException("Error getting property value "+propertyName+" on "+config);
		}
	}
	
	public final boolean isGlobalConfigurationPropertyBlankOrNotSpecified(Class<?> configurableDescribableType, String propertyName) {
		Object value = getGlobalConfigurationPropertyValue(configurableDescribableType, propertyName, Object.class);
		if ( value instanceof String ) {
			return StringUtils.isBlank((String)value) || ModelHelper.isNotSpecified((String)value);
		} else {
			return value==null;
		}
	}

	private final Map<Class<AbstractMultiActionConfigurableDescribable<?,?>>, AbstractMultiActionDescribableGlobalConfiguration<?>> getTargetTypeToEnabledActionsDefaultConfigsMap() {
		if ( targetTypeToEnabledActionsDefaultConfigsMap == null ) {
			targetTypeToEnabledActionsDefaultConfigsMap = Maps.uniqueIndex(enabledActionsDefaultConfigs, new Function<AbstractMultiActionDescribableGlobalConfiguration<?>, Class<AbstractMultiActionConfigurableDescribable<?,?>>> () {
				@Override
				public Class<AbstractMultiActionConfigurableDescribable<?,?>> apply(AbstractMultiActionDescribableGlobalConfiguration<?> input) {
					return input.getTargetType();
				}
			    
			    });
		};
		return targetTypeToEnabledActionsDefaultConfigsMap;
	}

	protected List<AbstractMultiActionDescribableGlobalConfiguration<?>> getDefaultEnabledActionsDefaultConfigs() {
		return new ArrayList<>();
		
		/*
		return !enableAllActionsByDefault() 
				? new ArrayList<>() 
				: Lists.newArrayList(Iterables.transform(getAllGlobalConfigurationDescriptors(),
					new Function<AbstractMultiActionDescriptor, MultiActionDescribableType>() {
						@Override
						public MultiActionDescribableType apply(AbstractMultiActionDescriptor input) {
							return (MultiActionDescribableType) input.createDefaultInstance();
						}
					}));
		*/
	}
	
	/*
	protected boolean enableAllActionsByDefault() {
		return true;
	}
	*/
	
	protected abstract <D extends AbstractMultiActionDescriptorGlobalConfiguration<?>> Class<D> getDescribableGlobalConfigurationDescriptorType();
	
	public final Class<?> getTargetType() {
		return AbstractMultiActionDescribableGlobalConfiguration.class;
	}
}