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

import org.kohsuke.stapler.DataBoundSetter;
import org.springframework.core.Ordered;

import com.fortify.integration.jenkins.ssc.FortifySSCGlobalConfiguration;

import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

/**
 * TODO: update JavaDoc
 * Subclasses will need to implement a property (field/setter/getter) named 'target' with 
 * the corresponding {@link AbstractConfigurableDescribable} type.
 * 
 * @author Ruud Senden
 *
 * @param <T>
 */
public abstract class AbstractConfigurableDescribableGlobalConfiguration extends AbstractDescribable<AbstractConfigurableDescribableGlobalConfiguration> {
	private static final long serialVersionUID = 1L;
	private boolean enabledByDefault = true;
	private boolean allowOverride = true;
	
	public boolean isEnabledByDefault() {
		return enabledByDefault;
	}
	
	@DataBoundSetter
	public void setEnabledByDefault(boolean enabledByDefault) {
		this.enabledByDefault = enabledByDefault;
	}
	
	public boolean isAllowOverride() {
		return allowOverride;
	}

	@DataBoundSetter
	public void setAllowOverride(boolean allowOverride) {
		this.allowOverride = allowOverride;
	}

	public abstract Describable<?> getTarget();
	public abstract Class<? extends AbstractConfigurableDescribable> getTargetType();

	public static abstract class AbstractDescriptorConfigurableDescribableGlobalConfiguration extends AbstractDescriptor<AbstractConfigurableDescribableGlobalConfiguration> implements Ordered {
		/* This would be nice, but causes Jenkins to fail from starting up
		@Override
		public String getDisplayName() {
			return getTargetDescriptor().getDisplayName();
		}
		*/

		@Override
		public int getOrder() {
			return getTargetDescriptor().getOrder();
		}
		
		public final boolean isStatic() {
			return FortifySSCGlobalConfiguration.get().isStatic(getTargetType());
		}
		
		public final boolean isDynamic() {
			return FortifySSCGlobalConfiguration.get().isDynamic(getTargetType());
		}
		
		@SuppressWarnings("unchecked")
		public final <TD extends Descriptor<?> & Ordered> TD getTargetDescriptor() {
			return (TD) Jenkins.getInstance().getDescriptorOrDie(getTargetType());
		}
		
		protected abstract Class<? extends Describable<?>> getTargetType();
	}
}