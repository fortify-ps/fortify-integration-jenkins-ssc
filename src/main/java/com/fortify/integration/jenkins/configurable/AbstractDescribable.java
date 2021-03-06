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

import java.io.Serializable;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

/**
 * Simple abstract base class for {@link AbstractDescribableImpl} implementations.
 * This class provides a default {@link #toString()} implementation, and a
 * method for returning either the given instance, or a new instance if the 
 * given instance is null. If no instance was previously configured in Jenkins,
 * the instance variable in Jelly pages will be null. Calling the 
 * {@link AbstractDescriptor#getInstanceOrDefault(AbstractDescribable)} 
 * method from Jelly pages allows those pages to use default values for all properties,
 * as configured in concrete {@link AbstractDescribable} implementations.
 * 
 * @author Ruud Senden
 *
 * @param <T> Concrete type that extends this abstract base class
 */
public abstract class AbstractDescribable<T extends AbstractDescribable<T>> extends AbstractDescribableImpl<T> implements Serializable {
	private static final long serialVersionUID = 1L;

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
	
	public static abstract class AbstractDescriptor<T extends AbstractDescribable<T>> extends Descriptor<T> {
		public T getInstanceOrDefault(T instance) {
			return instance!=null ? instance : createDefaultInstance();
		}
		
		public final T createDefaultInstance() {
			try {
				return clazz.newInstance();
			} catch (InstantiationException | IllegalAccessException | SecurityException e) {
				throw new RuntimeException("Error instantiating "+clazz.getName(), e);
			}
		}
	}
}