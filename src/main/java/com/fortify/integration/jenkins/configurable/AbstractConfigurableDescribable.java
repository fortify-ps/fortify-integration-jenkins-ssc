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

import java.io.PrintStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.kohsuke.stapler.StaplerRequest;
import org.springframework.core.Ordered;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.model.Describable;
import net.sf.json.JSONObject;

public abstract class AbstractConfigurableDescribable extends AbstractDescribable<AbstractConfigurableDescribable> implements Serializable {
	private static final long serialVersionUID = 1L;
	
	@Override
	public AbstractDescriptorConfigurableDescribable getDescriptor() {
		return (AbstractDescriptorConfigurableDescribable) super.getDescriptor();
	}
	
	public final void failIfGlobalConfigurationUnavailable(String message) throws AbortException {
		getConfigurableGlobalConfiguration().failIfGlobalConfigurationUnavailable(this.getClass(), message);
	}
	
	public final boolean isGlobalConfigurationAvailable() {
		return getDescriptor().isGlobalConfigurationAvailable();
	}
	
	
	/**
	 * Return whether the current instance is the default configuration.
	 * Note that this method also returns true if there is no default
	 * configuration.
	 * @return
	 */
	public final boolean isInstanceIsDefaultConfiguration() {
		Describable<?> defaultConfiguration = getDefaultConfiguration();
		return defaultConfiguration==null 
				|| !defaultConfiguration.getClass().equals(this.getClass()) 
				|| this == defaultConfiguration;
	}

	public boolean isOverrideAllowed(String propertyName) {
		return isInstanceIsDefaultConfiguration() 
			|| getConfigurableGlobalConfiguration().isOverrideAllowed(this.getClass(), propertyName);
	}
	
	/**
	 * This method needs to be called by all getter methods for configurable properties.
	 * If the current instance is the global configuration, or if override is allowed,
	 * this method will return the given current value. Otherwise, this method will
	 * return the property value from the global configuration. If log is given, a message
	 * will be logged if the given current value is overridden with the global configuration
	 * value. If envVars is given and the property type is String, the property will be
	 * expanded.
	 */
	protected final <V> V getExpandedPropertyValueOrDefaultValueIfOverrideDisallowed(PrintStream log, EnvVars envVars, String propertyName, V currentValue) {
		return isInstanceIsDefaultConfiguration() 
					? currentValue 
					: getConfigurableGlobalConfiguration().getExpandedPropertyValueOrDefaultValueIfOverrideDisallowed(
							this.getClass(), log, envVars, propertyName, currentValue);
	}
	
	protected Describable<?> getDefaultConfiguration() {
		return getDescriptor().getDefaultConfiguration();
	}
	
	protected final AbstractConfigurableGlobalConfiguration<?> getConfigurableGlobalConfiguration() {
		return getDescriptor().getConfigurableGlobalConfiguration();
	}
	
	public static abstract class AbstractDescriptorConfigurableDescribable extends AbstractDescriptor<AbstractConfigurableDescribable> implements Ordered {
		private transient Set<String> checkBoxPropertyNames = new HashSet<String>();
		
		protected Describable<?> getDefaultConfiguration() {
			return getConfigurableGlobalConfiguration().getDefaultConfiguration(getGlobalConfigurationTargetType());
		}
		
		public final boolean isGlobalConfigurationAvailable() {
			return getConfigurableGlobalConfiguration().isGlobalConfigurationAvailable(getGlobalConfigurationTargetType());
		}
		
		@Override
		public AbstractConfigurableDescribable newInstance(StaplerRequest req, JSONObject formData) throws hudson.model.Descriptor.FormException {
			addUncheckedCheckboxValues(formData);
			return super.newInstance(req, formData);
		}
		
		@Override
		public boolean configure(StaplerRequest req, JSONObject json) throws hudson.model.Descriptor.FormException {
			addUncheckedCheckboxValues(json);
			return super.configure(req, json);
		}
		
		private final void addUncheckedCheckboxValues(JSONObject formData) {
			for ( String propertyName : checkBoxPropertyNames ) {
				if ( !formData.containsKey(propertyName) ) {
					formData.put(propertyName, false);
				}
			}
		}
		
		public final void addCheckBoxPropertyName(String propertyName) {
			this.checkBoxPropertyNames.add(propertyName);
		}
		
		protected abstract Class<? extends Describable<?>> getGlobalConfigurationTargetType();
		
		protected abstract AbstractConfigurableGlobalConfiguration<?> getConfigurableGlobalConfiguration();
	}
}