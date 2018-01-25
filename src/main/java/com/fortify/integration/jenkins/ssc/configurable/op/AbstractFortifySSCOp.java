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

import java.io.IOException;

import com.fortify.integration.jenkins.configurable.AbortWithMessageException;
import com.fortify.integration.jenkins.configurable.AbstractConfigurableWithErrorHandler;
import com.fortify.integration.jenkins.ssc.configurable.FortifySSCApplicationAndVersionName;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;

/**
 * Abstract base class for all SSC operations. Each operation 
 * @author Ruud Senden
 *
 */
public abstract class AbstractFortifySSCOp extends AbstractConfigurableWithErrorHandler {
	private static final long serialVersionUID = 1L;
	
	public final void performWithCheck(FortifySSCApplicationAndVersionName applicationAndVersionNameJobConfig, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
		failIfConfigurationUnavailable(getDescriptor().getDisplayName()+" not enabled in global configuration");
		if ( requiresWorkspace() && workspace == null ) { 
			throw new AbortWithMessageException("no workspace for " + build);
		}
		perform(applicationAndVersionNameJobConfig, build, workspace, launcher, listener);
	}
	
	/**
	 * By default this method returns true, indicating that this action requires
	 * a workspace. Operations that don't require a workspace should override this
	 * method to return false.
	 * @return true if this operation requires a workspace, false otherwise
	 */
	protected boolean requiresWorkspace() {
		return true;
	}

	public abstract void perform(FortifySSCApplicationAndVersionName applicationAndVersionNameJobConfig, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException;
	
	public static abstract class AbstractFortifySSCDescriptorOp extends AbstractDescriptorConfigurableWithErrorHandler {}
}