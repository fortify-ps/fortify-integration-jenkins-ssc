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
import hudson.util.ComboBoxModel;
import hudson.util.FormValidation;

// TODO Add support for selecting SSC filterset
public class FortifySSCDescribableCheckIssueCountOp extends AbstractFortifySSCDescribableOp {
	private static final long serialVersionUID = 1L;
	private String searchString = "";
	private String operator = ">";
	private String rhsNumber = "0";
	
	/**
	 * Default constructor
	 */
	@DataBoundConstructor
	public FortifySSCDescribableCheckIssueCountOp() { super(null); }
	
	/**
	 * Copy constructor
	 * @param other
	 */
	public FortifySSCDescribableCheckIssueCountOp(FortifySSCDescribableCheckIssueCountOp other) {
		super(other);
		if ( other != null ) {
			setSearchString(other.getSearchString());
			setOperator(other.getOperator());
			setRhsNumber(other.getRhsNumber());
		}
	}
	
	public String getSearchString() {
		return getExpandedSearchString(null, null);
	}
	
	public String getExpandedSearchString(PrintStream log, EnvVars env) {
		return getExpandedPropertyValueOrDefaultValueIfOverrideDisallowed(log, env, "searchString", searchString);
	}

	@DataBoundSetter
	public void setSearchString(String searchString) {
		this.searchString = searchString;
	}

	public String getOperator() {
		return getOperator(null, null);
	}
	
	public String getOperator(PrintStream log, EnvVars env) {
		return getExpandedPropertyValueOrDefaultValueIfOverrideDisallowed(log, env, "operator", operator);
	}

	@DataBoundSetter
	public void setOperator(String operator) {
		this.operator = operator;
	}
	
	public String getRhsNumber() {
		return getRhsNumber(null, null);
	}

	public String getRhsNumber(PrintStream log, EnvVars env) {
		return getExpandedPropertyValueOrDefaultValueIfOverrideDisallowed(log, env, "rhsNumber", rhsNumber);
	}

	@DataBoundSetter
	public void setRhsNumber(String rhsNumber) {
		this.rhsNumber = rhsNumber;
	}

	@Override
	public void perform(FortifySSCDescribableApplicationAndVersionName applicationAndVersionNameJobConfig, Run<?, ?> run,
			FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
		PrintStream log = listener.getLogger();
		EnvVars env = run.getEnvironment(listener);
		//TODO Check really an int
		int numberToCompare = Integer.parseInt(getRhsNumber(log, env));
		String operator = getOperator(log, env);
		String searchString = getExpandedSearchString(log, env);
		
		SSCAuthenticatingRestConnection conn = FortifySSCGlobalConfiguration.get().conn();
		final String applicationVersionId = applicationAndVersionNameJobConfig.getApplicationVersionId(log, env);
		int numberOfIssues = conn.api(SSCIssueAPI.class).queryIssues(applicationVersionId)
			.paramFilter(searchString).maxResults(numberToCompare+1).paramFields("id").useCache(false)
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
		
		public ComboBoxModel doFillOperatorItems() {
			final ComboBoxModel items = new ComboBoxModel();
			items.add("<");
			items.add("=");
			items.add(">");
			return items;
		}
		
		public FormValidation doCheckSearchString(@QueryParameter String searchString) {
			if ( searchString != null && searchString.contains("${") ) {
				return FormValidation.warning("Cannot validate search string containing variables");
			} else {
				try {
					SSCAuthenticatingRestConnection conn = FortifySSCGlobalConfiguration.get().conn();
					if ( conn != null ) {
						JSONMap result = conn.api(SSCIssueAPI.class).validateIssueSearchString(searchString);
						if ( Boolean.TRUE.equals(result.get("valid", Boolean.class)) ) { //explicit equals to prevent rare NPE
							return FormValidation.ok();
						} else {
							return FormValidation.error(result.get("msg", String.class));
						}
					} else {
						return FormValidation.error("Cannot validate search string: SSC connection not available");
					}
				} catch ( Exception e ) {
					e.printStackTrace();
					return FormValidation.error("Cannot validate search string: "+e.getMessage());
				}
			}
		}
    }
}
