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

import java.io.Serializable;

import org.springframework.core.Ordered;

import com.fortify.integration.jenkins.ssc.FortifySSCGlobalConfiguration;

import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

public abstract class AbstractMultiActionConfigurableDescribable<C extends Describable<C>, T extends AbstractMultiActionConfigurableDescribable<C, T>> extends AbstractDescribable<T> implements Serializable {
	private static final long serialVersionUID = 1L;
	
	@SuppressWarnings("unchecked")
	@Override
	public AbstractMultiActionConfigurableDescriptor<C,T> getDescriptor() {
		return (AbstractMultiActionConfigurableDescriptor<C,T>) super.getDescriptor();
	}
	
	protected C getDefaultConfiguration() {
		return getDescriptor().getDefaultConfiguration();
	}
	
	public static abstract class AbstractMultiActionConfigurableDescriptor<C extends Describable<C>, T extends AbstractMultiActionConfigurableDescribable<C,T>> extends AbstractDescriptor<T> implements Ordered {		
		/**
		 * By default we return our own class. Subclasses must override this method
		 * if our global configuration class is not the same as ourselves.
		 * 
		 * @return
		 */
		@SuppressWarnings("unchecked")
		protected Class<C> getGlobalConfigurationTargetType() {
			return (Class<C>)getT();
		}
		
		protected final Descriptor<?> getGlobalConfigurationDescriptor() {
			return Jenkins.getInstance().getDescriptorOrDie(getGlobalConfigurationTargetType());
		}
		
		protected final Class<?> getGlobalConfigurationClass() {
			return getGlobalConfigurationDescriptor().getT();
		}
		
		protected C getDefaultConfiguration() {
			return FortifySSCGlobalConfiguration.get().getDefaultConfig(getGlobalConfigurationClass(), getGlobalConfigurationTargetType());
		}
		
		protected abstract AbstractMultiActionGlobalConfiguration<?> getMultiActionGlobalConfiguration();
	}
}