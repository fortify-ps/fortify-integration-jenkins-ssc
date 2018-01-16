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

import java.util.ArrayList;
import java.util.List;

import org.kohsuke.stapler.DataBoundSetter;

import jenkins.model.Jenkins;

/**
 * TODO: update JavaDoc
 * Subclasses will need to implement a property (field/setter/getter) named 'target' with 
 * the corresponding {@link AbstractMultiActionDescribable} type.
 * 
 * @author Ruud Senden
 *
 * @param <T>
 */
public abstract class AbstractMultiActionDescribableGlobalConfiguration<T extends AbstractMultiActionDescribableGlobalConfiguration<T>> extends AbstractMultiActionDescribable<T> {
	private static final long serialVersionUID = 1L;
	private boolean enabledByDefault;
	
	public boolean isEnabledByDefault() {
		return enabledByDefault;
	}
	
	@DataBoundSetter
	public void setEnabledByDefault(boolean enabledByDefault) {
		this.enabledByDefault = enabledByDefault;
	}
	
	public abstract AbstractMultiActionDescribable<?> getTarget();
	public abstract <TargetType extends AbstractMultiActionDescribable<?>> Class<TargetType> getTargetType();

	public static abstract class AbstractMultiActionDescriptorGlobalConfiguration<T extends AbstractMultiActionDescribableGlobalConfiguration<T>> extends AbstractMultiActionDescriptor<T> {
		/*
		@Override
		public String getDisplayName() {
			return getTargetDescriptor().getDisplayName();
		}
		*/

		@Override
		public int getOrder() {
			return getTargetDescriptor().getOrder();
		}
		
		public AbstractMultiActionDescriptor<?> getTargetDescriptor() {
			return (AbstractMultiActionDescriptor<?>) Jenkins.getInstance().getDescriptorOrDie(getTargetType());
		}
		
		protected abstract Class<? extends AbstractMultiActionDescribable<?>> getTargetType();
		
		public List<String> getConfigurableOverrideDisallowedPropertyNames() {
			return new ArrayList<>();
		}
	}
}