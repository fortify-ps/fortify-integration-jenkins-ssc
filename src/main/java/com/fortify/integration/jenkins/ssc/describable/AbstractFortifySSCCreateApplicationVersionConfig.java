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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.kohsuke.stapler.DataBoundSetter;

import com.fortify.client.ssc.api.SSCIssueTemplateAPI;
import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;
import com.fortify.integration.jenkins.ssc.FortifySSCGlobalConfiguration;
import com.fortify.util.rest.json.JSONList;
import com.fortify.util.rest.json.JSONMap;

import hudson.model.AbstractDescribableImpl;
import hudson.util.ListBoxModel;

public abstract class AbstractFortifySSCCreateApplicationVersionConfig<T extends AbstractFortifySSCCreateApplicationVersionConfig<T>> extends AbstractDescribableImpl<T> {
	private String issueTemplateName;
	private boolean issueTemplateNameOverrideAllowed = false;

	public String getIssueTemplateName() {
		return issueTemplateName;
	}

	@DataBoundSetter
	public void setIssueTemplateName(String issueTemplateName) {
		this.issueTemplateName = issueTemplateName;
	}

	public boolean isIssueTemplateNameOverrideAllowed() {
		return issueTemplateNameOverrideAllowed;
	}

	protected void setIssueTemplateNameOverrideAllowed(boolean issueTemplateNameOverrideAllowed) {
		this.issueTemplateNameOverrideAllowed = issueTemplateNameOverrideAllowed;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	public abstract static class AbstractFortifySSCCreateApplicationVersionConfigDescriptor<T extends AbstractFortifySSCCreateApplicationVersionConfig<T>> extends AbstractInstanceOrDefaultDescriptor<T> {
        @Override
        public final String getPropertyName() {
        	return "createApplicationVersionConfig";
        }
		
		public ListBoxModel doFillIssueTemplateNameItems() {
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
			SSCAuthenticatingRestConnection conn = FortifySSCGlobalConfiguration.get().conn();
			return conn.api(SSCIssueTemplateAPI.class).getIssueTemplates(true);
		}
    }
}