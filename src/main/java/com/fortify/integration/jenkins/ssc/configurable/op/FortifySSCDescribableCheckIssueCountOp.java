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
import java.io.PrintStream;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.fortify.client.ssc.api.SSCIssueAPI;
import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;
import com.fortify.integration.jenkins.configurable.ModelHelper;
import com.fortify.integration.jenkins.ssc.FortifySSCGlobalConfiguration;
import com.fortify.integration.jenkins.ssc.configurable.FortifySSCDescribableApplicationAndVersionName;
import com.fortify.util.rest.json.JSONMap;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Describable;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

// TODO Add support for selecting SSC filterset
public class FortifySSCDescribableCheckIssueCountOp extends AbstractFortifySSCDescribableOp {
	private static final long serialVersionUID = 1L;
	private String searchString = "";
	private String operator = ">";
	private int rhsNumber = 0;
	
	/**
	 * Default constructor
	 */
	@DataBoundConstructor
	public FortifySSCDescribableCheckIssueCountOp() {}
	
	/**
	 * Copy constructor
	 * @param other
	 */
	public FortifySSCDescribableCheckIssueCountOp(FortifySSCDescribableCheckIssueCountOp other) {
		if ( other != null ) {
			setSearchString(other.getSearchString());
			setOperator(other.getOperator());
			setRhsNumber(other.getRhsNumber());
		}
	}
	
	public String getSearchString() {
		return getSearchString(null);
	}
	
	public String getSearchString(PrintStream log) {
		return getPropertyValueOrDefaultValueIfOverrideDisallowed(log, "searchString", searchString);
	}

	@DataBoundSetter
	public void setSearchString(String searchString) {
		this.searchString = searchString;
	}

	public String getOperator() {
		return getOperator(null);
	}
	
	public String getOperator(PrintStream log) {
		return getPropertyValueOrDefaultValueIfOverrideDisallowed(log, "operator", operator);
	}

	@DataBoundSetter
	public void setOperator(String operator) {
		this.operator = operator;
	}
	
	public int getRhsNumber() {
		return getRhsNumber(null);
	}

	public int getRhsNumber(PrintStream log) {
		return getPropertyValueOrDefaultValueIfOverrideDisallowed(log, "rhsNumber", rhsNumber);
	}

	@DataBoundSetter
	public void setRhsNumber(int rhsNumber) {
		this.rhsNumber = rhsNumber;
	}

	@Override
	public void perform(FortifySSCDescribableApplicationAndVersionName applicationAndVersionNameJobConfig, Run<?, ?> run,
			FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
		PrintStream log = listener.getLogger();
		EnvVars env = run.getEnvironment(listener);
		int numberToCompare = getRhsNumber(log);
		String operator = getOperator(log);
		String searchString = getSearchString(log);
		
		SSCAuthenticatingRestConnection conn = FortifySSCGlobalConfiguration.get().conn();
		final String applicationVersionId = applicationAndVersionNameJobConfig.getApplicationVersionId(env, log);
		int numberOfIssues = conn.api(SSCIssueAPI.class).queryIssues(applicationVersionId)
			.paramFilter(searchString).maxResults(rhsNumber+1).paramFields("id").useCache(false)
			.build().getAll().size();
		if ( compare(numberOfIssues, operator, numberToCompare) ) {
			throw new AbortException("Number of issues matching '"+searchString+"' "+operator+" "+numberToCompare);
		}
	}

	private boolean compare(int value1, String operator, int value2) {
		switch (operator) {
			case "<":
				return value1 < value2;
			case "=":
				return value1 == value2;
			case ">":
				return value1 > value2;
			default:
				throw new IllegalArgumentException("Illegal compare operator '"+operator+"'");
		}
	}
	
	@Symbol("checkIssueCount")
	@Extension
	public static final class FortifySSCDescriptorCheckIssueCountOp extends AbstractFortifySSCDescriptorOp {
		static final String DISPLAY_NAME = "Check Issue Count";

		@Override
		public FortifySSCDescribableCheckIssueCountOp createDefaultInstanceWithConfiguration() {
			return new FortifySSCDescribableCheckIssueCountOp(getDefaultConfiguration());
		}
		
		@Override
		public FortifySSCDescribableCheckIssueCountOp createDefaultInstance() {
			return new FortifySSCDescribableCheckIssueCountOp();
		}
		
		@Override
		protected FortifySSCDescribableCheckIssueCountOp getDefaultConfiguration() {
			return (FortifySSCDescribableCheckIssueCountOp)super.getDefaultConfiguration();
		}
		
		@Override
		protected Class<? extends Describable<?>> getGlobalConfigurationTargetType() {
			return FortifySSCDescribableCheckIssueCountOp.class;
		}
		
		@Override
		public String getDisplayName() {
			// TODO Internationalize this
			return DISPLAY_NAME;
		}
		
		@Override
		public int getOrder() {
			return 500;
		}
		
		public ListBoxModel doFillOperatorItems(@QueryParameter String isGlobalConfig) {
			final ListBoxModel items = ModelHelper.createListBoxModel("true".equals(isGlobalConfig));
			items.add("<");
			items.add("=");
			items.add(">");
			return items;
		}
		
		public FormValidation doCheckSearchString(@QueryParameter String searchString) {
			SSCAuthenticatingRestConnection conn = FortifySSCGlobalConfiguration.get().conn();
			JSONMap result = conn.api(SSCIssueAPI.class).validateIssueSearchString(searchString);
			if ( Boolean.TRUE.equals(result.get("valid", Boolean.class)) ) { //explicit equals to prevent rare NPE
				return FormValidation.ok();
			} else {
				return FormValidation.error(result.get("msg", String.class));
			}
		}
    }
}
