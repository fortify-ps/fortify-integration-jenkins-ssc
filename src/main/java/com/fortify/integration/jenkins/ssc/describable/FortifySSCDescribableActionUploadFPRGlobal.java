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
package com.fortify.integration.jenkins.ssc.describable;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

public class FortifySSCDescribableActionUploadFPRGlobal extends AbstractFortifySSCDescribableActionGlobal<FortifySSCDescribableActionUploadFPRGlobal> {
	public static final int ORDER = 200;
	private static final String DEFAULT_FPR_ANT_FILTER = "**/*.fpr";
	private String fprAntFilter = DEFAULT_FPR_ANT_FILTER;
	private int processingTimeOutSeconds = 60;
	
	@DataBoundConstructor
	public FortifySSCDescribableActionUploadFPRGlobal() {}
	
	public String getFprAntFilter() {
		return fprAntFilter;
	}
	
	@DataBoundSetter
	public void setFprAntFilter(String fprAntFilter) {
		this.fprAntFilter = fprAntFilter;
	}
	public int getProcessingTimeOutSeconds() {
		return processingTimeOutSeconds;
	}
	
	@DataBoundSetter
	public void setProcessingTimeOutSeconds(int processingTimeOutSeconds) {
		this.processingTimeOutSeconds = processingTimeOutSeconds;
	}
	
	@Override
	public Descriptor<?> getJobConfigDescriptor() {
		return Jenkins.getInstance().getDescriptorOrDie(FortifySSCDescribableActionUploadFPRJob.class);
	}
	
	@Extension
	public static final class FortifySSCUploadFPRGlobalConfigDescriptor extends AbstractFortifySSCDescriptorActionGlobal<FortifySSCDescribableActionUploadFPRGlobal> {
		@Override
		public String getDisplayName() {
			// TODO Internationalize this
			return "Enable upload of FPR files";
		}
		
		@Override
		public FortifySSCDescribableActionUploadFPRGlobal createDefaultInstance() {
			return new FortifySSCDescribableActionUploadFPRGlobal();
		}
		
		@Override
		public int getOrder() {
			return ORDER;
		}
	}
}
