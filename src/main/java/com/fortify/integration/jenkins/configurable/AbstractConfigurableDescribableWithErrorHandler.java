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

import hudson.model.Result;
import hudson.model.Run;
import hudson.util.ListBoxModel;

public abstract class AbstractConfigurableDescribableWithErrorHandler extends AbstractConfigurableDescribable {
	private static final long serialVersionUID = 1L;
	private String stopOnFailure = Boolean.TRUE.toString();
	private String buildResultOnFailure = Result.FAILURE.toString();
	
	public AbstractConfigurableDescribableWithErrorHandler() {}
	
	public AbstractConfigurableDescribableWithErrorHandler(AbstractConfigurableDescribableWithErrorHandler other) {
		setStopOnFailure(other.getStopOnFailure());
		setBuildResultOnFailure(other.getBuildResultOnFailure());
	}

	public String getStopOnFailure() {
		return getStopOnFailureWithLog(null);
	}
	
	public String getStopOnFailureWithLog(PrintStream log) {
		return getPropertyValueOrDefaultValueIfOverrideDisallowed(log, "stopOnFailure", stopOnFailure);
	}

	@DataBoundSetter
	public void setStopOnFailure(String stopOnFailure) {
		System.out.println("setStopOnFailure: "+stopOnFailure);
		this.stopOnFailure = stopOnFailure;
	}

	public String getBuildResultOnFailure() {
		return getBuildResultOnFailureWithLog(null);
	}
	
	public String getBuildResultOnFailureWithLog(PrintStream log) {
		return getPropertyValueOrDefaultValueIfOverrideDisallowed(log, "buildResultOnFailure", buildResultOnFailure);
	}

	@DataBoundSetter
	public void setBuildResultOnFailure(String buildResultOnFailure) {
		this.buildResultOnFailure = buildResultOnFailure;
	}
	
	public boolean handleException(PrintStream log, Exception e, ErrorData currentErrorData) {
		log.println("ERROR: "+e.getMessage());
		String buildResultOnFailure = getBuildResultOnFailureWithLog(log);
		String stopOnFailure = getStopOnFailureWithLog(log);
		currentErrorData.updateFinalBuildResult(buildResultOnFailure);
		return Boolean.TRUE.toString().equals(stopOnFailure);
	}

	public abstract static class AbstractDescriptorConfigurableDescribableWithErrorHandler extends AbstractDescriptorConfigurableDescribable {
		@Override
		public int getOrder() {
			return 0;
		}
		
		public ListBoxModel doFillStopOnFailureItems() {
			return ModelHelper.createBooleanListBoxModel(true);
		}
		
		public ListBoxModel doFillBuildResultOnFailureItems() {
			final ListBoxModel items = ModelHelper.createListBoxModel(true);
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
