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

import org.kohsuke.stapler.DataBoundSetter;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.model.Result;
import hudson.model.Run;
import hudson.util.ComboBoxModel;

/**
 * <p>This abstract {@link AbstractConfigurable} implementation adds support for
 * error handling. It provides a method {@link #handleException(PrintStream, EnvVars, Exception, ErrorData)}
 * that can be called by {@link AbstractBuilder} implementations to determine
 * whether any remaining operations should be executed or not, and for determining the final
 * build result.</p>
 * 
 * @author Ruud Senden
 *
 */
public abstract class AbstractConfigurableWithErrorHandler extends AbstractConfigurable {
	private static final long serialVersionUID = 1L;
	private String stopOnFailure;
	private String buildResultOnFailure;
	
	public AbstractConfigurableWithErrorHandler() {}
	
	@Override
	protected final void configureDefaultValues() {
		setStopOnFailure("Yes");
		setBuildResultOnFailure(Result.FAILURE.toString());
		configureDefaultValuesAfterErrorHandler();
	}

	protected void configureDefaultValuesAfterErrorHandler() {}

	public String getStopOnFailure() {
		return getStopOnFailureWithLog(null, null);
	}
	
	public String getStopOnFailureWithLog(PrintStream log, EnvVars env) {
		return getExpandedPropertyValueOrDefaultValueIfOverrideDisallowedWithoutFail(log, env, "stopOnFailure", stopOnFailure);
	}

	@DataBoundSetter
	public void setStopOnFailure(String stopOnFailure) {
		this.stopOnFailure = stopOnFailure;
	}

	public String getBuildResultOnFailure() {
		return getBuildResultOnFailureWithLog(null, null);
	}
	
	public String getBuildResultOnFailureWithLog(PrintStream log, EnvVars env) {
		return getExpandedPropertyValueOrDefaultValueIfOverrideDisallowedWithoutFail(log, env, "buildResultOnFailure", buildResultOnFailure);
	}

	@DataBoundSetter
	public void setBuildResultOnFailure(String buildResultOnFailure) {
		this.buildResultOnFailure = buildResultOnFailure;
	}
	
	public boolean handleException(PrintStream log, EnvVars env, Exception e, ErrorData currentErrorData) {
		if ( e instanceof AbortException || e instanceof AbortWithMessageException ) {
			log.println("[ERROR] "+e.getMessage());
		} else {
			log.println("[ERROR] Unexpected exception occured");
			e.printStackTrace(log);
		}
		String buildResultOnFailure = getBuildResultOnFailureWithLog(log, env);
		String stopOnFailure = getStopOnFailureWithLog(log, env);
		currentErrorData.updateFinalBuildResult(buildResultOnFailure);
		return ModelHelper.isBooleanComboBoxValueTrue(stopOnFailure);
	}
	
	/**
	 * Same as {@link #getExpandedPropertyValueOrDefaultValueIfOverrideDisallowed(PrintStream, EnvVars, String, Object)},
	 * but doesn't throw an exception if overrideFailOnOverride is set to true. This method usually shouldn't
	 * be called, except for methods that define how to handle errors 
	 * (i.e. {@link AbstractConfigurableWithErrorHandler}).
	 * @param log
	 * @param envVars
	 * @param propertyName
	 * @param currentValue
	 * @param overrideFailOnOverride
	 * @return
	 */
	private final <V> V getExpandedPropertyValueOrDefaultValueIfOverrideDisallowedWithoutFail(PrintStream log, EnvVars envVars, String propertyName, V currentValue) {
		return isInstanceIsDefaultConfiguration() 
					? currentValue 
					: getDescriptor().getGlobalConfigurationWithConfigurations().getExpandedPropertyValueOrDefaultValueIfOverrideDisallowed(
							this.getClass(), log, envVars, propertyName, currentValue, true);
	}

	public abstract static class AbstractDescriptorConfigurableWithErrorHandler extends AbstractDescriptorConfigurable {
		@Override
		public int getOrder() {
			return 0;
		}
		
		public ComboBoxModel doFillStopOnFailureItems() {
			return ModelHelper.createBooleanComboBoxModel();
		}
		
		public ComboBoxModel doFillBuildResultOnFailureItems() {
			final ComboBoxModel items = new ComboBoxModel();
			items.add(Result.SUCCESS.toString());
			items.add(Result.UNSTABLE.toString());
			items.add(Result.FAILURE.toString());
			return items;
		}
	}
	
	public static final class ErrorData {
		private Result finalBuildResult = Result.SUCCESS;

		public Result getFinalBuildResult() {
			return finalBuildResult;
		}

		public void updateFinalBuildResult(String currentResult) {
			this.finalBuildResult = finalBuildResult.combine(Result.fromString(currentResult));
		}
		
		public void markBuild(Run<?,?> build) {
			build.setResult(getFinalBuildResult());
		}
	}

}
