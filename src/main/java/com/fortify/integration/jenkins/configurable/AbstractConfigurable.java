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

import org.springframework.beans.BeanUtils;
import org.springframework.core.Ordered;

import com.fortify.integration.jenkins.configurable.AbstractConfigurationForConfigurable.AbstractDescriptorConfigurationForConfigurable;

import hudson.EnvVars;
import hudson.model.Describable;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;

/**
 * <p>This abstract base class provides functionality for implementing 
 * a {@link Describable} that, in addition to regular data binding,
 * supports default values to be configured on the Jenkins global
 * configuration page.</p>
 * 
 * <p>For example, suppose that a concrete implementation provides 
 * a property named 'myProperty'. Extending from this base class
 * allows an administrator to configure a default value for 
 * 'myProperty' on the Jenkins global configuration page. The
 * administrator will also be able to specify whether that property 
 * can be overridden in other places where this Configurable
 * instance is used.</p>
 * 
 * <p>Concrete implementations follow similar patterns as a 
 * regular {@link Describable}, but with some differences:</p>
 * <ul>
 *  <li>Each {@link AbstractConfigurable} implementation must have a
 *      corresponding {@link AbstractConfigurationForConfigurable}
 *      implementation, named [AbstractConfigurableConcreteClassName]Configuration.</li>
 *  <li>For each property, an additional getter must be implemented
 *      that, given a {@link PrintStream} and {@link EnvVars}, calls
 *      getExpandedPropertyValueOrDefaultValueIfOverrideDisallowed().
 *      This allows for override checks and property expansion.
 *      In the {@link SimpleBuildStep#perform(hudson.model.Run, hudson.FilePath, hudson.Launcher, hudson.model.TaskListener)}
 *      method, this special getter must be used instead of the
 *      no-argument getter.</li>
 *  <li>The descriptor for the {@link AbstractConfigurable} implementation
 *      must extend from {@link AbstractDescriptorConfigurable}.</li>
 *  <li>Implementations may choose to override the {@link #configureDefaultValues()}
 *      method to set default values for properties. implementations
 *      should not use regular Java field initializers.<li>
 * </ul>
 *  
 * @author Ruud Senden
 *
 */
public abstract class AbstractConfigurable extends AbstractDescribable<AbstractConfigurable> implements Serializable {
	private static final long serialVersionUID = 1L;
	
	/**
	 * Default constructor, allowing for configuring basic default property
	 * values through the {@link #configureDefaultValues()} method, and
	 * initializing the current instance with default configuration property
	 * values through the {@link #configureValuesFromDefaultConfiguration(Describable)}
	 * method.
	 */
	public AbstractConfigurable() {
		configureDefaultValues();
		Describable<?> defaultConfiguration = getDescriptor().getConfiguration();
		if ( defaultConfiguration != null ) {
			configureValuesFromDefaultConfiguration(defaultConfiguration);
		}
	}
	
	/**
	 * This method can be overridden by implementations to set default property values.
	 * To properly support pipeline jobs, default values should not be declared
	 * directly on instance fields.
	 */
	protected void configureDefaultValues() {}
	
	/**
	 * By default, this method copies all bean properties from the given default 
	 * configuration to the current instance. If needed, concrete implementations 
	 * can override this method to initialize the current instance with global 
	 * configuration properties in some different way.
	 * 
	 * @param defaultConfiguration Default configuration, never null
	 */
	protected void configureValuesFromDefaultConfiguration(Describable<?> defaultConfiguration) {
		BeanUtils.copyProperties(defaultConfiguration, this);
	}
	
	/**
	 * Return super.getDescriptor(), with return value cast to current descriptor type.
	 */
	@Override
	public AbstractDescriptorConfigurable getDescriptor() {
		return (AbstractDescriptorConfigurable) super.getDescriptor();
	}
	
	/**
	 * This method throws an {@link AbortWithMessageException} exception
	 * with the given message if no global configuration is available,
	 *  
	 * @param message used when configuration unavailable
	 */
	public final void failIfConfigurationUnavailable(String message) {
		if ( !isConfigurationAvailable() ) {
			throw new AbortWithMessageException(message);
		}
	}
	
	/**
	 * This method returns true if global configuration is available,
	 * false otherwise.
	 * 
	 * @return true if configuration is available, false otherwise
	 */
	public final boolean isConfigurationAvailable() {
		return getDescriptor().isConfigurationAvailable();
	}
	
	
	/**
	 * Return whether the current instance is the configuration instance.
	 * Note that this method also returns true if there is no default
	 * configuration.
	 * @return true if the current instance is the default configuration
	 *         or if there is no configuration instance,
	 *         false otherwise.
	 */
	public final boolean isInstanceIsConfiguration() {
		Describable<?> defaultConfiguration = getDescriptor().getConfiguration();
		return defaultConfiguration==null 
				|| !defaultConfiguration.getClass().equals(this.getClass()) 
				|| this == defaultConfiguration;
	}

	/**
	 * Return whether the given property name is allowed to be overridden
	 * in job configurations. If the current instance is the default 
	 * configuration, this method always returns true. Otherwise, the
	 * global configuration is checked to see whether override is allowed.
	 * @param propertyName to be checked whether override is allowed
	 * @return true if override is allowed or if the current instance is 
	 *         the configuration instance, false otherwise.
	 */
	public boolean isOverrideAllowed(String propertyName) {
		return isInstanceIsConfiguration() 
			|| getDescriptor().getGlobalConfiguration().isOverrideAllowed(this.getClass(), propertyName);
	}
	
	/**
	 * This method needs to be called by all getter methods for configurable properties.
	 * If the current instance is the global configuration, or if override is allowed,
	 * this method will return the given current value. Otherwise, this method will
	 * return the property value from the global configuration. If log is given, a message
	 * will be logged if the given current value is overridden with the global configuration
	 * value. If envVars is given and the property type is String, the property will be
	 * expanded.
	 * 
	 * @param <V> Property type
	 * @param log Jenkins console log
	 * @param envVars Jenkins {@link EnvVars}
	 * @param propertyName Property name to be checked whether override is allowed
	 * @param currentValue Current value of the given property name
	 * @return Current property value if override allowed, configuration value if override is set to WARN
	 * @throws AbortWithMessageException if property is overridden when override is set to FAIL
	 */
	protected final <V> V getExpandedPropertyValueOrDefaultValueIfOverrideDisallowed(PrintStream log, EnvVars envVars, String propertyName, V currentValue) throws AbortWithMessageException {
		return isInstanceIsConfiguration() 
					? currentValue 
					: getDescriptor().getGlobalConfiguration().getExpandedPropertyValueOrDefaultValueIfOverrideDisallowed(
							this.getClass(), log, envVars, propertyName, currentValue);
	}
	
	/**
	 * Abstract descriptor class for {@link AbstractConfigurable} instances. Each 
	 * concrete {@link AbstractConfigurable} implementation must have a corresponding
	 * descriptor that extends from this {@link AbstractDescriptorConfigurable}.
	 *  
	 * @author Ruud Senden
	 *
	 */
	public static abstract class AbstractDescriptorConfigurable extends AbstractDescriptor<AbstractConfigurable> implements Ordered {
		/**
		 * Get the configuration for our AbstractConfigurable from {@link AbstractGlobalConfiguration}.
		 * @return The configuration for our {@link AbstractConfigurable} implementation
		 */
		protected final Describable<?> getConfiguration() {
			return getGlobalConfiguration().getConfiguration(getConfigurationTargetType());
		}
		
		/**
		 * Indicate whether configuration is available.
		 * @return True if configuration is available, false otherwise
		 */
		public final boolean isConfigurationAvailable() {
			return getGlobalConfiguration().isGlobalConfigurationAvailable(getConfigurationTargetType());
		}
		
		/**
		 * Get the type of the target property in our {@link AbstractConfigurationForConfigurable}
		 * counterpart.
		 * @return Type of the target property in our {@link AbstractConfigurationForConfigurable}
		 * counterpart.
		 */
		protected final Class<? extends Describable<?>> getConfigurationTargetType() {
			return getConfigurationForConfigurableDescriptor().getTargetType();
		}
		
		/**
		 * Get the concrete {@link AbstractGlobalConfiguration} instance from our
		 * {@link AbstractConfigurationForConfigurable} counterpart.
		 * @return Our {@link AbstractGlobalConfiguration} implementation
		 */
		protected final AbstractGlobalConfiguration<?> getGlobalConfiguration() {
			return getConfigurationForConfigurableDescriptor().getGlobalConfiguration();
		}
		
		/**
		 * Get the descriptor for our {@link AbstractConfigurationForConfigurable} counterpart.
		 * @return The descriptor for our {@link AbstractConfigurationForConfigurable} counterpart
		 */
		protected final AbstractDescriptorConfigurationForConfigurable getConfigurationForConfigurableDescriptor() {
			return (AbstractDescriptorConfigurationForConfigurable) Jenkins.getInstance().getDescriptorOrDie(getConfigurationForConfigurableType());
		}
		
		/**
		 * This method returns the {@link AbstractConfigurationForConfigurable} counterpart for
		 * our own type. By default is looks for a class named "[OurOwnClassName]Configuration".
		 * If you use a different naming scheme, this method must be overridden.
		 * @return The {@link AbstractConfigurationForConfigurable} counterpart for our own type
		 */
		@SuppressWarnings("unchecked")
		protected Class<? extends AbstractConfigurationForConfigurable> getConfigurationForConfigurableType() {
			String configurationForConfigurableTypeName = clazz.getName()+"Configuration";
			try {
				return (Class<? extends AbstractConfigurationForConfigurable>) Class.forName(configurationForConfigurableTypeName);
			} catch ( ClassNotFoundException e ) {
				throw new RuntimeException("Configuration counterpart for "+clazz.getSimpleName()+" not found", e);
			}
		}
	}
}