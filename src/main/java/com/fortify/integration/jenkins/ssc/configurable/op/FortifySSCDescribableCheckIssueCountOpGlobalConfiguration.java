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
package com.fortify.integration.jenkins.ssc.configurable.op;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.fortify.integration.jenkins.ssc.configurable.op.FortifySSCDescribableCheckIssueCountOp.FortifySSCDescriptorCheckIssueCountOp;

import hudson.Extension;

public class FortifySSCDescribableCheckIssueCountOpGlobalConfiguration extends AbstractFortifySSCDescribableOpGlobalConfiguration {
	private static final long serialVersionUID = 1L;
	private FortifySSCDescribableCheckIssueCountOp target;
	
	@DataBoundConstructor
	public FortifySSCDescribableCheckIssueCountOpGlobalConfiguration() {}
	
	public FortifySSCDescribableCheckIssueCountOp getTarget() {
		return target;
	}

	@DataBoundSetter
	public void setTarget(FortifySSCDescribableCheckIssueCountOp target) {
		this.target = target;
	}
	
	@Extension
	public static final class FortifySSCDescriptorUploadFPROpGlobalConfiguration extends AbstractFortifySSCDescriptorOpGlobalConfiguration {        
        @Override
        public FortifySSCDescribableCheckIssueCountOpGlobalConfiguration createDefaultInstance() {
        	return new FortifySSCDescribableCheckIssueCountOpGlobalConfiguration();
        }
        
        @Override
        public String getDisplayName() {
        	return FortifySSCDescriptorCheckIssueCountOp.DISPLAY_NAME;
        }
    }
}