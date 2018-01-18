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
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;
import org.springframework.core.OrderComparator;

import com.fortify.integration.jenkins.multiaction.AbstractMultiActionDescribableGlobalConfiguration.AbstractMultiActionDescriptorGlobalConfiguration;
import com.fortify.integration.jenkins.util.ModelHelper;
import com.google.common.base.Function;
import com.google.common.collect.Maps;

import hudson.AbortException;
import hudson.ExtensionList;
import hudson.model.Describable;
import hudson.util.DescribableList;
import hudson.util.ReflectionUtils;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

/**
 * To use, extend this class with correct generics types, and include the following in the
 * config.jelly page for your concrete implementation:
 * 
 * <pre>
 	TODO
 * </pre>
 * 
 * This will automatically load all available DescriptorActionJobType instances (based on the class type returned
 * by {@link #getDescribableGlobalConfigurationActionDescriptorType()}), and allow to enable, disable and configure these instances to
 * define the global configuration for this plugin.
 * 
 * @author Ruud Senden
 *
 */
public abstract class AbstractMultiActionGlobalConfiguration<T extends AbstractMultiActionGlobalConfiguration<T>> extends AbstractGlobalConfiguration<T> 
{
	private volatile DescribableList<AbstractMultiActionDescribableGlobalConfiguration,AbstractMultiActionDescriptorGlobalConfiguration> dynamicGlobalConfigurationsList;
	private volatile DescribableList<AbstractMultiActionDescribableGlobalConfiguration,AbstractMultiActionDescriptorGlobalConfiguration> staticGlobalConfigurationsList;
	
	private transient Map<Class<? extends AbstractMultiActionConfigurableDescribable>, AbstractMultiActionDescribableGlobalConfiguration> targetTypeToDynamicGlobalConfigurationsMap;
	private transient Map<Class<? extends AbstractMultiActionConfigurableDescribable>, AbstractMultiActionDescribableGlobalConfiguration> targetTypeToStaticGlobalConfigurationsMap;

	public final DescribableList<AbstractMultiActionDescribableGlobalConfiguration, AbstractMultiActionDescriptorGlobalConfiguration> getDynamicGlobalConfigurationsList() {
		if (dynamicGlobalConfigurationsList == null) {
			dynamicGlobalConfigurationsList = new DescribableList<AbstractMultiActionDescribableGlobalConfiguration,AbstractMultiActionDescriptorGlobalConfiguration>(this);
        }
		return dynamicGlobalConfigurationsList;
	}
	
	public final DescribableList<AbstractMultiActionDescribableGlobalConfiguration, AbstractMultiActionDescriptorGlobalConfiguration> getStaticGlobalConfigurationsList() {
		if (staticGlobalConfigurationsList == null) {
			staticGlobalConfigurationsList = new DescribableList<AbstractMultiActionDescribableGlobalConfiguration,AbstractMultiActionDescriptorGlobalConfiguration>(this);
        }
		return staticGlobalConfigurationsList;
	}

	@DataBoundSetter
	public void setDynamicGlobalConfigurationsList(List<? extends AbstractMultiActionDescribableGlobalConfiguration> dynamicGlobalConfigurations) throws IOException {
		getDynamicGlobalConfigurationsList().replaceBy(dynamicGlobalConfigurations);
	}

	@DataBoundSetter
	public void setStaticGlobalConfigurationsList(List<? extends AbstractMultiActionDescribableGlobalConfiguration> staticGlobalConfigurations) throws IOException {
		getStaticGlobalConfigurationsList().replaceBy(staticGlobalConfigurations);
	}

	public final DescribableList<AbstractMultiActionDescribableGlobalConfiguration, AbstractMultiActionDescriptorGlobalConfiguration> getSortedDynamicGlobalConfigurationsList() {
		return getSortedDescribableList(getDynamicGlobalConfigurationsList());
	}
	
	public final DescribableList<AbstractMultiActionDescribableGlobalConfiguration, AbstractMultiActionDescriptorGlobalConfiguration> getSortedStaticGlobalConfigurationsList() {
		return getSortedDescribableList(getStaticGlobalConfigurationsList());
	}

	private DescribableList<AbstractMultiActionDescribableGlobalConfiguration, AbstractMultiActionDescriptorGlobalConfiguration> getSortedDescribableList(
			DescribableList<AbstractMultiActionDescribableGlobalConfiguration, AbstractMultiActionDescriptorGlobalConfiguration> original) {
		DescribableList<AbstractMultiActionDescribableGlobalConfiguration, AbstractMultiActionDescriptorGlobalConfiguration> result = 
				new DescribableList<>(this, original);
		result.sort(new OrderComparator());
		return result;
	}
	
	public final List<? extends AbstractMultiActionDescriptorGlobalConfiguration> getAllDynamicGlobalConfigurationDescriptors() {
		return getAllGlobalConfigurationDescriptors(getDynamicGlobalConfigurationDescriptorType());
	}

	public final List<? extends AbstractMultiActionDescriptorGlobalConfiguration> getAllStaticGlobalConfigurationDescriptors() {
		return getAllGlobalConfigurationDescriptors(getStaticGlobalConfigurationDescriptorType());
	}

	private List<? extends AbstractMultiActionDescriptorGlobalConfiguration> getAllGlobalConfigurationDescriptors(Class<? extends AbstractMultiActionDescriptorGlobalConfiguration> describableGlobalConfigurationActionDescriptorType) {
		ExtensionList<? extends AbstractMultiActionDescriptorGlobalConfiguration> list = Jenkins.getInstance().getExtensionList(describableGlobalConfigurationActionDescriptorType);
		List<? extends AbstractMultiActionDescriptorGlobalConfiguration> result = new ArrayList<>(list);
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
		return getTargetTypeToDynamicGlobalConfigurationsMap().containsKey(configurableDescribableType);
	}

	public final Describable<?> getDefaultConfiguration(Class<?> configurableDescribableType) {
		AbstractMultiActionDescribableGlobalConfiguration config = getGlobalConfiguration(configurableDescribableType);
		return config == null ? null : config.getTarget();
	}
	
	public final boolean isOverrideAllowed(Class<?> configurableDescribableType, String propertyName) {
		AbstractMultiActionDescribableGlobalConfiguration config = getGlobalConfiguration(configurableDescribableType);
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
		AbstractMultiActionDescribableGlobalConfiguration config = getGlobalConfiguration(configurableDescribableType);
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
	
	private AbstractMultiActionDescribableGlobalConfiguration getGlobalConfiguration(Class<?> configurableDescribableType) {
		AbstractMultiActionDescribableGlobalConfiguration result = getTargetTypeToDynamicGlobalConfigurationsMap().get(configurableDescribableType);
		if ( result == null ) {
			result = getTargetTypeToStaticGlobalConfigurationsMap().get(configurableDescribableType);
		}
		return result;
	}

	private final Map<Class<? extends AbstractMultiActionConfigurableDescribable>, AbstractMultiActionDescribableGlobalConfiguration> getTargetTypeToDynamicGlobalConfigurationsMap() {
		if ( targetTypeToDynamicGlobalConfigurationsMap==null ) {
			targetTypeToDynamicGlobalConfigurationsMap = createTargetTypesToDefaultConfigsMap(getDynamicGlobalConfigurationsList());
		}
		return targetTypeToDynamicGlobalConfigurationsMap;
	}
	
	private final Map<Class<? extends AbstractMultiActionConfigurableDescribable>, AbstractMultiActionDescribableGlobalConfiguration> getTargetTypeToStaticGlobalConfigurationsMap() {
		if ( targetTypeToStaticGlobalConfigurationsMap==null ) {
			targetTypeToStaticGlobalConfigurationsMap = createTargetTypesToDefaultConfigsMap(getStaticGlobalConfigurationsList());
		}
		return targetTypeToStaticGlobalConfigurationsMap;
	}

	private Map<Class<? extends AbstractMultiActionConfigurableDescribable>, AbstractMultiActionDescribableGlobalConfiguration> createTargetTypesToDefaultConfigsMap(List<AbstractMultiActionDescribableGlobalConfiguration> defaultConfigs) {
		return defaultConfigs==null ? new HashMap<>() : Maps.uniqueIndex(defaultConfigs, new Function<AbstractMultiActionDescribableGlobalConfiguration, Class<? extends AbstractMultiActionConfigurableDescribable>> () {
			@Override
			public Class<? extends AbstractMultiActionConfigurableDescribable> apply(AbstractMultiActionDescribableGlobalConfiguration input) {
				return input.getTargetType();
			}
		    
		});
	}

	/* TODO re-implement this
	private List<AbstractMultiActionDescribableGlobalConfiguration> getDefaultEnabledActionsDefaultConfigs() {
		return !enableAllActionsByDefault() 
				? new ArrayList<>() 
				: Lists.newArrayList(Iterables.transform(getAllGlobalConfigurationDescriptors(),
					new Function<AbstractMultiActionDescriptor, MultiActionDescribableType>() {
						@Override
						public MultiActionDescribableType apply(AbstractMultiActionDescriptor input) {
							return (MultiActionDescribableType) input.createDefaultInstance();
						}
					}));
		
	}
	*/
	
	/*
	protected boolean enableAllActionsByDefault() {
		return true;
	}
	*/
	
	protected abstract <D extends AbstractMultiActionDescriptorGlobalConfiguration> Class<D> getDynamicGlobalConfigurationDescriptorType();
	protected abstract <D extends AbstractMultiActionDescriptorGlobalConfiguration> Class<D> getStaticGlobalConfigurationDescriptorType();
	
	public final Class<?> getTargetType() {
		return AbstractMultiActionDescribableGlobalConfiguration.class;
	}
	
	@Override
	public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
		this.targetTypeToDynamicGlobalConfigurationsMap = null;
		this.targetTypeToStaticGlobalConfigurationsMap = null;
		
		try {
			System.out.println("Dynamic before: "+getAllDynamicGlobalConfigurationDescriptors());
			getDynamicGlobalConfigurationsList().rebuildHetero(req, json, getAllDynamicGlobalConfigurationDescriptors(), "dynamicGlobalConfiguration");
			System.out.println("Dynamic after: "+getAllDynamicGlobalConfigurationDescriptors());
			System.out.println("Static before: "+getAllStaticGlobalConfigurationDescriptors());
			getStaticGlobalConfigurationsList().rebuild(req, json, getAllStaticGlobalConfigurationDescriptors());
			System.out.println("Static after: "+getAllStaticGlobalConfigurationDescriptors());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return super.configure(req, json);
	}
}