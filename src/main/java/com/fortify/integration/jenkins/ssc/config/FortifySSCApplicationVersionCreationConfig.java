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
package com.fortify.integration.jenkins.ssc.config;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.fortify.client.ssc.api.SSCIssueTemplateAPI;
import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;
import com.fortify.util.rest.json.JSONList;
import com.fortify.util.rest.json.JSONMap;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;

public final class FortifySSCApplicationVersionCreationConfig extends AbstractDescribableImpl<FortifySSCApplicationVersionCreationConfig> {
	private String defaultIssueTemplateName;
	private boolean defaultIssueTemplateNameOverrideAllowed;
	
	@DataBoundConstructor
	public FortifySSCApplicationVersionCreationConfig() {}
	
	public String getDefaultIssueTemplateName() {
		return defaultIssueTemplateName;
	}

	@DataBoundSetter
	public void setDefaultIssueTemplateName(String defaultIssueTemplateName) {
		this.defaultIssueTemplateName = defaultIssueTemplateName;
	}

	public boolean isDefaultIssueTemplateNameOverrideAllowed() {
		return defaultIssueTemplateNameOverrideAllowed;
	}

	@DataBoundSetter
	public void setDefaultIssueTemplateNameOverrideAllowed(boolean defaultIssueTemplateNameOverrideAllowed) {
		this.defaultIssueTemplateNameOverrideAllowed = defaultIssueTemplateNameOverrideAllowed;
	}

	@Extension
    public static class DescriptorImpl extends Descriptor<FortifySSCApplicationVersionCreationConfig> {
        @Override
        public String getDisplayName() {
            return this.getClass().getSimpleName();
        }
        
        public ListBoxModel doFillDefaultIssueTemplateNameItems() {
			final ListBoxModel items = new ListBoxModel();
			JSONList issueTemplates = getIssueTemplates();
			for ( JSONMap issueTemplate : issueTemplates.asValueType(JSONMap.class) ) {
				items.add(issueTemplate.get("name", String.class));
			}
			return items;
		}
        
        public String getDefaultIssueTemplateName() {
        	return getIssueTemplates().mapValue("defaultTemplate", true, "name", String.class);
        }
        
        protected JSONList getIssueTemplates() {
			SSCAuthenticatingRestConnection conn = FortifySSCConfiguration.get().conn();
			return conn.api(SSCIssueTemplateAPI.class).getIssueTemplates(true);
		}
    }
}