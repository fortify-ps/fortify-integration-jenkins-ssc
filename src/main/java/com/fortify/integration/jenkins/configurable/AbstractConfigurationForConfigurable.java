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

import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

/**
 * <p>This abstract class holds the configuration for a corresponding
 * {@link AbstractConfigurable}. It provides some generic settings,
 * like whether default settings may be overridden in job configurations, and
 * whether the corresponding {@link AbstractConfigurable} should
 * be enabled by default in job configurations.</p>
 * 
 * <p>Concrete implementations will need to provide a property named 'target'
 * with the concrete type that holds the default configuration, and the 
 * corresponding getter and data-bound setter methods. Usuaully this 'target'
 * property has the same type as our {@link AbstractConfigurable} counterpart,
 * but in theory any {@link Descriptor} type that also implements the {@link Ordered}
 * interface can be used.</p>
 * 
 * @author Ruud Senden
 *
 */
public abstract class AbstractConfigurationForConfigurable extends AbstractDescribable<AbstractConfigurationForConfigurable> {
	public static enum AllowOverride { YES, WARN, FAIL }
	private static final long serialVersionUID = 1L;
	private boolean enabledByDefault = true;
	private AllowOverride allowOverride = AllowOverride.YES;
	
	public boolean isEnabledByDefault() {
		return enabledByDefault;
	}
	
	@DataBoundSetter
	public void setEnabledByDefault(boolean enabledByDefault) {
		this.enabledByDefault = enabledByDefault;
	}
	
	public boolean isOverrideAllowed() {
		return AllowOverride.YES.equals(allowOverride);
	}
	
	public boolean isFailOnOverride() {
		return AllowOverride.FAIL.equals(allowOverride);
	}
	
	public AllowOverride getAllowOverride() {
		return allowOverride;
	}

	@DataBoundSetter
	public void setAllowOverride(AllowOverride allowOverride) {
		this.allowOverride = allowOverride;
	}
	
	public final Class<? extends Describable<?>> getTargetType() {
		return getDescriptor().getTargetType();
	}
	
	@Override
	public AbstractDescriptorConfigurationForConfigurable getDescriptor() {
		return (AbstractDescriptorConfigurationForConfigurable) super.getDescriptor();
	}

	public abstract Describable<?> getTarget();

	public static abstract class AbstractDescriptorConfigurationForConfigurable extends AbstractDescriptor<AbstractConfigurationForConfigurable> implements Ordered {
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
		
		public ListBoxModel doFillAllowOverrideItems() {
			final ListBoxModel items = new ListBoxModel();
			items.add("Yes", AllowOverride.YES.toString());
			items.add("No (fail if overridden)", AllowOverride.FAIL.toString());
			items.add("No (use default if overridden)", AllowOverride.WARN.toString());
			return items;
		}
		
		public final boolean isStatic() {
			return getConfigurableGlobalConfiguration().isStatic(getTargetType());
		}
		
		public final boolean isDynamic() {
			return getConfigurableGlobalConfiguration().isDynamic(getTargetType());
		}
		
		@SuppressWarnings("unchecked")
		public final <TD extends Descriptor<?> & Ordered> TD getTargetDescriptor() {
			return (TD) Jenkins.getInstance().getDescriptorOrDie(getTargetType());
		}
		
		@SuppressWarnings("unchecked")
		protected final Class<? extends Describable<?>> getTargetType() {
			return getPropertyType("target").clazz;
		}
		
		protected abstract AbstractGlobalConfiguration<?> getConfigurableGlobalConfiguration();
	}
}