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
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.springframework.core.OrderComparator;

import com.fortify.integration.jenkins.configurable.AbstractConfigurableDescribableGlobalConfiguration.AbstractDescriptorConfigurableDescribableGlobalConfiguration;
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
public abstract class AbstractConfigurableGlobalConfiguration<T extends AbstractConfigurableGlobalConfiguration<T>> extends AbstractGlobalConfiguration<T> 
{
	private volatile DescribableList<AbstractConfigurableDescribableGlobalConfiguration,AbstractDescriptorConfigurableDescribableGlobalConfiguration> dynamicGlobalConfigurationsList;
	private volatile DescribableList<AbstractConfigurableDescribableGlobalConfiguration,AbstractDescriptorConfigurableDescribableGlobalConfiguration> staticGlobalConfigurationsList;
	
	private transient Map<Class<? extends AbstractConfigurableDescribable>, AbstractConfigurableDescribableGlobalConfiguration> targetTypeToDynamicGlobalConfigurationsMap;
	private transient Map<Class<? extends AbstractConfigurableDescribable>, AbstractConfigurableDescribableGlobalConfiguration> targetTypeToStaticGlobalConfigurationsMap;

	public final DescribableList<AbstractConfigurableDescribableGlobalConfiguration, AbstractDescriptorConfigurableDescribableGlobalConfiguration> getDynamicGlobalConfigurationsList() {
		if (dynamicGlobalConfigurationsList == null) {
			dynamicGlobalConfigurationsList = new DescribableList<AbstractConfigurableDescribableGlobalConfiguration,AbstractDescriptorConfigurableDescribableGlobalConfiguration>(this);
        }
		return dynamicGlobalConfigurationsList;
	}
	
	public final DescribableList<AbstractConfigurableDescribableGlobalConfiguration, AbstractDescriptorConfigurableDescribableGlobalConfiguration> getStaticGlobalConfigurationsList() {
		if (staticGlobalConfigurationsList == null) {
			staticGlobalConfigurationsList = new DescribableList<AbstractConfigurableDescribableGlobalConfiguration,AbstractDescriptorConfigurableDescribableGlobalConfiguration>(this);
        }
		return staticGlobalConfigurationsList;
	}
	
	public final List<? extends AbstractDescriptorConfigurableDescribableGlobalConfiguration> getAllDynamicGlobalConfigurationDescriptors() {
		return getAllGlobalConfigurationDescriptors(getDynamicGlobalConfigurationDescriptorTypes());
	}

	public final List<? extends AbstractDescriptorConfigurableDescribableGlobalConfiguration> getAllStaticGlobalConfigurationDescriptors() {
		return getAllGlobalConfigurationDescriptors(getStaticGlobalConfigurationDescriptorTypes());
	}

	private <D extends AbstractDescriptorConfigurableDescribableGlobalConfiguration> List<D> getAllGlobalConfigurationDescriptors(Collection<Class<D>> types) {
		List<D> result = new ArrayList<>();
		for ( Class<D> type : types ) {
			ExtensionList<D> list = Jenkins.getInstance().getExtensionList(type);
			result.addAll(list);
		}
		result.sort(new OrderComparator());
		return result;
	}
	
	public boolean isShowStaticGlobalConfigurationsList() {
		return true;
	}
	
	public boolean isShowDynamicGlobalConfigurationsList() {
		return true;
	}
	
	public String getDynamicGlobalConfigurationAddButtonDisplayName() {
		return "Add";
	}
	
	public String getDynamicGlobalConfigurationDeleteButtonDisplayName() {
		return "Delete";
	}
	
	public final boolean isStatic(Class<?> configurableDescribableType) {
		return getTargetTypeToStaticGlobalConfigurationsMap().containsKey(configurableDescribableType);
	}
	
	public final boolean isDynamic(Class<?> configurableDescribableType) {
		return getTargetTypeToDynamicGlobalConfigurationsMap().containsKey(configurableDescribableType);
	}
	
	public final boolean isEnabledByDefault(Class<?> configurableDescribableType) {
		AbstractConfigurableDescribableGlobalConfiguration config = getGlobalConfiguration(configurableDescribableType);
        if ( config==null ) { return false; }
        return config.isEnabledByDefault();
    }
	
	public final void failIfGlobalConfigurationUnavailable(Class<?> configurableDescribableType, String message) throws AbortException {
		if ( !isGlobalConfigurationAvailable(configurableDescribableType) ) {
			// TODO Replace with something like this if called from pipeline job?
			//      descriptor.getClass().getAnnotation(Symbol.class).value()[0]
			throw new AbortException(message);
		}
	}

	public final boolean isGlobalConfigurationAvailable(Class<?> configurableDescribableType) {
		return getTargetTypeToDynamicGlobalConfigurationsMap().containsKey(configurableDescribableType)
				|| getTargetTypeToStaticGlobalConfigurationsMap().containsKey(configurableDescribableType);
	}

	public final Describable<?> getDefaultConfiguration(Class<?> configurableDescribableType) {
		AbstractConfigurableDescribableGlobalConfiguration config = getGlobalConfiguration(configurableDescribableType);
		return config == null ? null : config.getTarget();
	}
	
	public final boolean isOverrideAllowed(Class<?> configurableDescribableType, String propertyName) {
		AbstractConfigurableDescribableGlobalConfiguration config = getGlobalConfiguration(configurableDescribableType);
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
		if ( !ObjectUtils.equals(result, currentValue) && log != null ) {
			log.println("WARNING: Using default configuration value '"+result+"' for property "+propertyName+" because override is disabled in global configuration");
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public final <R> R getGlobalConfigurationPropertyValue(Class<?> configurableDescribableType, String propertyName, Class<R> returnType) {
		AbstractConfigurableDescribableGlobalConfiguration config = getGlobalConfiguration(configurableDescribableType);
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
	
	private AbstractConfigurableDescribableGlobalConfiguration getGlobalConfiguration(Class<?> configurableDescribableType) {
		AbstractConfigurableDescribableGlobalConfiguration result = getTargetTypeToDynamicGlobalConfigurationsMap().get(configurableDescribableType);
		if ( result == null ) {
			result = getTargetTypeToStaticGlobalConfigurationsMap().get(configurableDescribableType);
		}
		return result;
	}

	private final Map<Class<? extends AbstractConfigurableDescribable>, AbstractConfigurableDescribableGlobalConfiguration> getTargetTypeToDynamicGlobalConfigurationsMap() {
		if ( targetTypeToDynamicGlobalConfigurationsMap==null ) {
			targetTypeToDynamicGlobalConfigurationsMap = createTargetTypesToDefaultConfigsMap(getDynamicGlobalConfigurationsList());
		}
		return targetTypeToDynamicGlobalConfigurationsMap;
	}
	
	private final Map<Class<? extends AbstractConfigurableDescribable>, AbstractConfigurableDescribableGlobalConfiguration> getTargetTypeToStaticGlobalConfigurationsMap() {
		if ( targetTypeToStaticGlobalConfigurationsMap==null ) {
			targetTypeToStaticGlobalConfigurationsMap = createTargetTypesToDefaultConfigsMap(getStaticGlobalConfigurationsList());
		}
		return targetTypeToStaticGlobalConfigurationsMap;
	}

	private Map<Class<? extends AbstractConfigurableDescribable>, AbstractConfigurableDescribableGlobalConfiguration> createTargetTypesToDefaultConfigsMap(List<AbstractConfigurableDescribableGlobalConfiguration> defaultConfigs) {
		return defaultConfigs==null ? new HashMap<>() : Maps.uniqueIndex(defaultConfigs, new Function<AbstractConfigurableDescribableGlobalConfiguration, Class<? extends AbstractConfigurableDescribable>> () {
			@Override
			public Class<? extends AbstractConfigurableDescribable> apply(AbstractConfigurableDescribableGlobalConfiguration input) {
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
	
	protected abstract <D extends AbstractDescriptorConfigurableDescribableGlobalConfiguration> Collection<Class<D>> getDynamicGlobalConfigurationDescriptorTypes();
	protected abstract <D extends AbstractDescriptorConfigurableDescribableGlobalConfiguration> Collection<Class<D>> getStaticGlobalConfigurationDescriptorTypes();
	
	/**
	 * Generics helper method to generate collections for 
	 * {@link #getDynamicGlobalConfigurationDescriptorTypes()} and
	 * {@link #getStaticGlobalConfigurationDescriptorTypes()}
	 * @param type
	 * @return
	 */
	protected <D extends AbstractDescriptorConfigurableDescribableGlobalConfiguration> Class<D> d(Class<D> type) {
		return type;
	}
	
	public final Class<?> getTargetType() {
		return AbstractConfigurableDescribableGlobalConfiguration.class;
	}
	
	@Override
	public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
		try {
			this.targetTypeToDynamicGlobalConfigurationsMap = null;
			// Only rebuild if the dynamic global configuration list is shown
			// to prevent configuration list to be cleared if it is (temporarily)
			// not shown.
			if ( isShowDynamicGlobalConfigurationsList() ) {
				getDynamicGlobalConfigurationsList().rebuildHetero(req, json, getAllDynamicGlobalConfigurationDescriptors(), "dynamicGlobalConfigurationsList");
			}
		} catch (IOException e) {
			throw new FormException("Error updating configuration", e, "dynamicGlobalConfigurationsList");
		}
		try {
			this.targetTypeToStaticGlobalConfigurationsMap = null;
			JSONObject staticGlobalConfigurationJSON = json.getJSONObject("staticGlobalConfigurationsList");
			if ( staticGlobalConfigurationJSON!=null && !staticGlobalConfigurationJSON.isNullObject() ) {
				getStaticGlobalConfigurationsList().rebuild(req, staticGlobalConfigurationJSON, getAllStaticGlobalConfigurationDescriptors());
			}
		} catch (IOException e) {
			throw new FormException("Error updating configuration", e, "staticGlobalConfigurationsList");
		}
		return super.configure(req, json);
	}
}