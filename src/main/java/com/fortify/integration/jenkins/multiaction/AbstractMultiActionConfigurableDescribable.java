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
import java.io.Serializable;

import org.springframework.core.Ordered;

import com.fortify.integration.jenkins.ssc.FortifySSCGlobalConfiguration;

import hudson.AbortException;
import hudson.model.Describable;

public abstract class AbstractMultiActionConfigurableDescribable<C extends Describable<C>, T extends AbstractMultiActionConfigurableDescribable<C, T>> extends AbstractDescribable<T> implements Serializable {
	private static final long serialVersionUID = 1L;
	
	@SuppressWarnings("unchecked")
	@Override
	public AbstractMultiActionConfigurableDescriptor<C,T> getDescriptor() {
		return (AbstractMultiActionConfigurableDescriptor<C,T>) super.getDescriptor();
	}
	
	public final boolean isEnabled() {
		return getMultiActionGlobalConfiguration().isEnabled(this.getClass());
	}
	
	public final void checkEnabled() throws AbortException {
		getMultiActionGlobalConfiguration().checkEnabled(this.getClass());
	}
	
	/**
	 * Return whether the current instance is the default configuration.
	 * Note that this method also returns true if there is no default
	 * configuration.
	 * @return
	 */
	public final boolean isInstanceIsDefaultConfiguration() {
		C defaultConfiguration = getDefaultConfiguration();
		return defaultConfiguration==null 
				|| !defaultConfiguration.getClass().equals(this.getClass()) 
				|| this == defaultConfiguration;
	}
	
	public boolean isOverrideAllowed(String propertyName) {
		return isInstanceIsDefaultConfiguration() 
			|| getMultiActionGlobalConfiguration().isOverrideAllowed(this.getClass(), propertyName);
	}
	
	/**
	 * This method needs to be called by all getter methods for configurable properties.
	 * If the current instance is the global configuration, or if override is allowed,
	 * this method will return the given current value. Otherwise, this method will
	 * return the property value from the global configuration.
	 *  
	 * @param propertyName
	 * @param currentValue
	 * @return
	 */
	protected final <V> V getPropertyValueOrDefaultValueIfOverrideDisallowed(String propertyName, V currentValue) {
		return isInstanceIsDefaultConfiguration() 
					? currentValue 
					: getMultiActionGlobalConfiguration().getPropertyValueOrDefaultValueIfOverrideDisallowed(
							this.getClass(), propertyName, currentValue);
	}
	
	/**
	 * Same as {@link #getPropertyValueOrDefaultValueIfOverrideDisallowed(String, Object)},
	 * but logs a warning if default configuration value is used instead of current value.
	 * @return
	 */
	protected final <V> V getPropertyValueOrDefaultValueIfOverrideDisallowed(PrintStream log, String propertyName, V currentValue) {
		return isInstanceIsDefaultConfiguration() 
					? currentValue 
					: getMultiActionGlobalConfiguration().getPropertyValueOrDefaultValueIfOverrideDisallowed(
							this.getClass(), log, propertyName, currentValue);
	}
	
	public final C getDefaultConfiguration() {
		return getDescriptor().getDefaultConfiguration();
	}
	
	protected final AbstractMultiActionGlobalConfiguration<?> getMultiActionGlobalConfiguration() {
		return getDescriptor().getMultiActionGlobalConfiguration();
	}
	
	public static abstract class AbstractMultiActionConfigurableDescriptor<C extends Describable<C>, T extends AbstractMultiActionConfigurableDescribable<C,T>> extends AbstractDescriptor<T> implements Ordered {		
		/**
		 * By default we return the dynamically added default configuration.
		 * Subclasses that are statically configured must override this method.
		 * This method assumes that the default configuration target type
		 * is the same as the configurable type (C and T class parameters are
		 * the same class). If not, this method must be overridden to get the 
		 * default configuration using the correct types.
		 * @return
		 */
		@SuppressWarnings("unchecked")
		protected C getDefaultConfiguration() {
			return (C)FortifySSCGlobalConfiguration.get().getDefaultConfig(getT(), getT());
		}
		
		protected abstract AbstractMultiActionGlobalConfiguration<?> getMultiActionGlobalConfiguration();
	}
}