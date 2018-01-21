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
package com.fortify.integration.jenkins.ssc.action;

import hudson.Plugin;
import hudson.PluginWrapper;
import hudson.model.Action;
import jenkins.model.Jenkins;

public final class FortifySSCPageAction implements Action {
	private final FortifySSCPublishAction action;

	public FortifySSCPageAction(FortifySSCPublishAction action) {
		this.action = action;
	}

	public FortifySSCPublishAction getAction() {
		return action;
	}

	@Override
	public String getIconFileName() {
		PluginWrapper wrapper = Jenkins.getInstance().getPluginManager().getPlugin(Plugin.DummyImpl.class);
		return "/plugin/"+wrapper.getShortName()+"/icons/SSC-32x32.gif";
	}

	@Override
	public String getDisplayName() {
		return "Fortify Assessment";
	}

	@Override
	public String getUrlName() {
		return "fortify-ssc-issues";
	}
	
	
}