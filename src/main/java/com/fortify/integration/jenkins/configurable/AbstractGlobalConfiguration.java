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

import com.fortify.integration.jenkins.configurable.AbstractConfigurationForConfigurable.AbstractDescriptorConfigurationForConfigurable;
import com.google.common.base.Function;
import com.google.common.collect.Maps;

import hudson.EnvVars;
import hudson.ExtensionList;
import hudson.model.Describable;
import hudson.util.DescribableList;
import hudson.util.ReflectionUtils;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

/**
 * <p>This abstract base class provides global configuration settings based on 
 * lists of {@link AbstractConfigurationForConfigurable}
 * instances. There is one list for static global configuration instances, 
 * and one list for dynamic global configuration instances. The static 
 * instances will always be shown on the global configuration page, whereas 
 * the dynamic instances can be added to or removed from the global configuration
 * by the Jenkins user.</p>
 * 
 * <p>Concrete implementations of this class will need to implement the 
 * {@link #getDynamicGlobalConfigurationDescriptorTypes()} and 
 * {@link #getStaticGlobalConfigurationDescriptorTypes()} methods to 
 * allow this class to determine which descriptors are to be included
 * for the dynamic instances, and which descriptors are to be included
 * for the static instances.</p>
 * 
 * @author Ruud Senden
 *
 * @param <T> Concrete type that extends this abstract base class
 */
public abstract class AbstractGlobalConfiguration<T extends AbstractGlobalConfiguration<T>> extends GlobalConfiguration 
{
	private volatile DescribableList<AbstractConfigurationForConfigurable,AbstractDescriptorConfigurationForConfigurable> dynamicGlobalConfigurationsList;
	private volatile DescribableList<AbstractConfigurationForConfigurable,AbstractDescriptorConfigurationForConfigurable> staticGlobalConfigurationsList;
	
	private transient Map<Class<? extends Describable<?>>, AbstractConfigurationForConfigurable> targetTypeToDynamicGlobalConfigurationsMap;
	private transient Map<Class<? extends Describable<?>>, AbstractConfigurationForConfigurable> targetTypeToStaticGlobalConfigurationsMap;
	
	public final AbstractGlobalConfiguration<T> getInstanceOrDefault(AbstractGlobalConfiguration<T> instance) {
		return instance!=null ? instance : createDefaultInstance();
	}
	
	@SuppressWarnings("unchecked")
	public AbstractGlobalConfiguration<T> createDefaultInstance() {
		try {
			return (AbstractGlobalConfiguration<T>) clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException | SecurityException e) {
			throw new RuntimeException("Error instantiating "+clazz.getName(), e);
		}
	}

	public final DescribableList<AbstractConfigurationForConfigurable, AbstractDescriptorConfigurationForConfigurable> getDynamicGlobalConfigurationsList() {
		if (dynamicGlobalConfigurationsList == null) {
			dynamicGlobalConfigurationsList = addDefaultDescribables(new DescribableList<AbstractConfigurationForConfigurable,AbstractDescriptorConfigurationForConfigurable>(this), getAllDynamicGlobalConfigurationDescriptors());
        }
		return dynamicGlobalConfigurationsList;
	}
	
	public final DescribableList<AbstractConfigurationForConfigurable, AbstractDescriptorConfigurationForConfigurable> getStaticGlobalConfigurationsList() {
		if (staticGlobalConfigurationsList == null) {
			staticGlobalConfigurationsList = addDefaultDescribables(new DescribableList<AbstractConfigurationForConfigurable,AbstractDescriptorConfigurationForConfigurable>(this), getAllStaticGlobalConfigurationDescriptors());
        }
		return staticGlobalConfigurationsList;
	}
	
	public final List<? extends AbstractDescriptorConfigurationForConfigurable> getAllDynamicGlobalConfigurationDescriptors() {
		return getAllGlobalConfigurationDescriptors(getDynamicGlobalConfigurationDescriptorTypes());
	}

	public final List<? extends AbstractDescriptorConfigurationForConfigurable> getAllStaticGlobalConfigurationDescriptors() {
		return getAllGlobalConfigurationDescriptors(getStaticGlobalConfigurationDescriptorTypes());
	}

	private <D extends AbstractDescriptorConfigurationForConfigurable> List<D> getAllGlobalConfigurationDescriptors(Collection<Class<D>> types) {
		List<D> result = new ArrayList<>();
		for ( Class<D> type : types ) {
			ExtensionList<D> list = Jenkins.getInstance().getExtensionList(type);
			result.addAll(list);
		}
		result.sort(new OrderComparator());
		return result;
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
		AbstractConfigurationForConfigurable config = getGlobalConfiguration(configurableDescribableType);
        if ( config==null ) { return false; }
        return config.isEnabledByDefault();
    }
	
	public final boolean isGlobalConfigurationAvailable(Class<?> configurableDescribableType) {
		return getTargetTypeToDynamicGlobalConfigurationsMap().containsKey(configurableDescribableType)
				|| getTargetTypeToStaticGlobalConfigurationsMap().containsKey(configurableDescribableType);
	}

	@SuppressWarnings("unchecked")
	public final <D extends Describable<?>> D getConfiguration(Class<D> type) {
		AbstractConfigurationForConfigurable config = getGlobalConfiguration(type);
		return config == null ? null : (D)config.getTarget();
	}
	
	public final boolean isOverrideAllowed(Class<?> configurableDescribableType, String propertyName) {
		AbstractConfigurationForConfigurable config = getGlobalConfiguration(configurableDescribableType);
		if ( config==null ) { return true; }
		if ( config.isOverrideAllowed() ) { return true; }
		return isGlobalConfigurationPropertyBlank(configurableDescribableType, propertyName);
	}
	
	public final <V> V getExpandedPropertyValueOrDefaultValueIfOverrideDisallowed(Class<?> configurableDescribableType, PrintStream log, EnvVars envVars, String propertyName, V currentValue) throws AbortWithMessageException {
		return getExpandedPropertyValueOrDefaultValueIfOverrideDisallowed(configurableDescribableType, log, envVars, propertyName, currentValue, false);
	}
	
	@SuppressWarnings("unchecked")
	public final <V> V getExpandedPropertyValueOrDefaultValueIfOverrideDisallowed(Class<?> configurableDescribableType, PrintStream log, EnvVars envVars, String propertyName, V currentValue, boolean overrideFailOnOverride) throws AbortWithMessageException {
		V result = null;
		if ( isOverrideAllowed(configurableDescribableType, propertyName) ) {
			result = currentValue;
		} else {
			result = (V)getGlobalConfigurationPropertyValue(configurableDescribableType, propertyName, Object.class);
			if ( log != null && !ObjectUtils.equals(result, currentValue)) { 
				if ( !overrideFailOnOverride && getGlobalConfiguration(configurableDescribableType).isFailOnOverride() ) {
					throw new AbortWithMessageException("Property "+propertyName+" may not be overridden (default value: '"+result+"', supplied value: '"+currentValue+"')");
				} else {
					log.println("[WARN] Property "+propertyName+" may not be overridden, using default value '"+result+"' instead of supplied value '"+currentValue+"'");
				}
			}
		}
		if ( envVars != null && result instanceof String && StringUtils.isNotBlank((String)result) ) {
			result = (V)envVars.expand((String)result);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public final <R> R getGlobalConfigurationPropertyValue(Class<?> configurableDescribableType, String propertyName, Class<R> returnType) {
		AbstractConfigurationForConfigurable config = getGlobalConfiguration(configurableDescribableType);
		try {
			return config == null ? null : (R)ReflectionUtils.getPublicProperty(config.getTarget(), propertyName);
		} catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
			throw new RuntimeException("Error getting property value "+propertyName+" on "+config, e);
		}
	}
	
	public final boolean isGlobalConfigurationPropertyBlank(Class<?> configurableDescribableType, String propertyName) {
		Object value = getGlobalConfigurationPropertyValue(configurableDescribableType, propertyName, Object.class);
		if ( value instanceof String ) {
			return StringUtils.isBlank((String)value);
		} else {
			return value==null;
		}
	}
	
	private AbstractConfigurationForConfigurable getGlobalConfiguration(Class<?> configurableDescribableType) {
		AbstractConfigurationForConfigurable result = getTargetTypeToDynamicGlobalConfigurationsMap().get(configurableDescribableType);
		if ( result == null ) {
			result = getTargetTypeToStaticGlobalConfigurationsMap().get(configurableDescribableType);
		}
		return result;
	}

	private final Map<Class<? extends Describable<?>>, AbstractConfigurationForConfigurable> getTargetTypeToDynamicGlobalConfigurationsMap() {
		if ( targetTypeToDynamicGlobalConfigurationsMap==null ) {
			targetTypeToDynamicGlobalConfigurationsMap = createTargetTypesToDefaultConfigsMap(getDynamicGlobalConfigurationsList());
		}
		return targetTypeToDynamicGlobalConfigurationsMap;
	}
	
	private final Map<Class<? extends Describable<?>>, AbstractConfigurationForConfigurable> getTargetTypeToStaticGlobalConfigurationsMap() {
		if ( targetTypeToStaticGlobalConfigurationsMap==null ) {
			targetTypeToStaticGlobalConfigurationsMap = createTargetTypesToDefaultConfigsMap(getStaticGlobalConfigurationsList());
		}
		return targetTypeToStaticGlobalConfigurationsMap;
	}

	private Map<Class<? extends Describable<?>>, AbstractConfigurationForConfigurable> createTargetTypesToDefaultConfigsMap(List<AbstractConfigurationForConfigurable> defaultConfigs) {
		return defaultConfigs==null ? new HashMap<>() : Maps.uniqueIndex(defaultConfigs, new Function<AbstractConfigurationForConfigurable, Class<? extends Describable<?>>> () {
			@Override
			public Class<? extends Describable<?>> apply(AbstractConfigurationForConfigurable input) {
				return input.getTargetType();
			}
		    
		});
	}
	
	private DescribableList<AbstractConfigurationForConfigurable,AbstractDescriptorConfigurationForConfigurable> addDefaultDescribables(DescribableList<AbstractConfigurationForConfigurable,AbstractDescriptorConfigurationForConfigurable> list, List<? extends AbstractDescriptorConfigurationForConfigurable> descriptorList) {
        for ( AbstractDescriptorConfigurationForConfigurable descriptor : descriptorList ) {
            list.add(descriptor.createDefaultInstance());
        }
        return list;
    }
	
	protected abstract <D extends AbstractDescriptorConfigurationForConfigurable> Collection<Class<D>> getDynamicGlobalConfigurationDescriptorTypes();
	protected abstract <D extends AbstractDescriptorConfigurationForConfigurable> Collection<Class<D>> getStaticGlobalConfigurationDescriptorTypes();
	
	@Override
	public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
		try {
			getDynamicGlobalConfigurationsList().rebuildHetero(req, json, getAllDynamicGlobalConfigurationDescriptors(), "dynamicGlobalConfigurationsList");
		} catch (IOException e) {
			throw new FormException("Error updating configuration", e, "dynamicGlobalConfigurationsList");
		} finally {
			this.targetTypeToDynamicGlobalConfigurationsMap = null;
		}
		try {
			JSONObject staticGlobalConfigurationJSON = json.getJSONObject("staticGlobalConfigurationsList");
			if ( staticGlobalConfigurationJSON!=null && !staticGlobalConfigurationJSON.isNullObject() ) {
				getStaticGlobalConfigurationsList().rebuild(req, staticGlobalConfigurationJSON, getAllStaticGlobalConfigurationDescriptors());
			}
		} catch (IOException e) {
			throw new FormException("Error updating configuration", e, "staticGlobalConfigurationsList");
		} finally {
			this.targetTypeToStaticGlobalConfigurationsMap = null;
		}
		return super.configure(req, json);
	}
}