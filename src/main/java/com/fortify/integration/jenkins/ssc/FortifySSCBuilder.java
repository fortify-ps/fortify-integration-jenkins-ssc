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
package com.fortify.integration.jenkins.ssc;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;
import com.fortify.integration.jenkins.configurable.AbstractBuilder;
import com.fortify.integration.jenkins.configurable.AbstractConfigurable;
import com.fortify.integration.jenkins.configurable.AbstractConfigurable.AbstractDescriptorConfigurable;
import com.fortify.integration.jenkins.configurable.AbstractConfigurableWithErrorHandler.ErrorData;
import com.fortify.integration.jenkins.configurable.AbstractGlobalConfiguration;
import com.fortify.integration.jenkins.ssc.configurable.FortifySSCApplicationAndVersionName;
import com.fortify.integration.jenkins.ssc.configurable.FortifySSCApplicationAndVersionName.FortifySSCDescriptorApplicationAndVersionName;
import com.fortify.integration.jenkins.ssc.configurable.op.AbstractFortifySSCOp;
import com.fortify.integration.jenkins.ssc.configurable.op.AbstractFortifySSCOp.AbstractFortifySSCDescriptorOp;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.DescribableList;

public class FortifySSCBuilder extends AbstractBuilder {
	@DataBoundConstructor
	public FortifySSCBuilder() {}

	public FortifySSCApplicationAndVersionName getWith() {
		return getStaticJobConfiguration(FortifySSCApplicationAndVersionName.class);
	}

	// 'with' is a pretty strange name for this field, but it actually looks nice
	// in pipeline jobs: performOnSSC( with: [applicationName: 'x', ...] ops: [...]) 
	@DataBoundSetter
	public void setWith(FortifySSCApplicationAndVersionName with) throws IOException {
		setStaticJobConfigurationsList(Arrays.asList(with));;
	}
	
	public final DescribableList<AbstractConfigurable, AbstractDescriptorConfigurable> getOps() {
		return getDynamicJobConfigurationsList();
	}
	
	@DataBoundSetter
	public void setOps(List<? extends AbstractConfigurable> dynamicJobConfigurations) throws IOException {
		setDynamicJobConfigurationsList(dynamicJobConfigurations);
	}
	
	@Override
	public void perform(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
		PrintStream log = listener.getLogger();
		log.println("[INFO] Fortify Jenkins plugin");
		SSCAuthenticatingRestConnection conn = FortifySSCGlobalConfiguration.get().conn();
		if ( conn == null ) {
			log.println("[ERROR] SSC connection not configured in global configuration");
			build.setResult(Result.FAILURE);
		} else {
			log.println("[INFO] Using SSC instance at " + conn.getBaseUrl());
			DescribableList<AbstractConfigurable, AbstractDescriptorConfigurable> describables = getDynamicJobConfigurationsList();
			if ( describables==null || describables.size()==0 ) {
				log.println("[WARN] There are no operations configured to be performed");
			} else {
				ErrorData currentErrorData = new ErrorData();
				for ( AbstractConfigurable describable : describables) {
					if (describable != null) {
						if ( !perform(describable, build, workspace, launcher, listener, currentErrorData) ) { break; }
					}
				}
				currentErrorData.markBuild(build);
			}
		}
	}

	/**
	 * Perform the given operation
	 * @param describable The {@link AbstractFortifySSCOp} operation to perform
	 * @param run Current {@link Run}
	 * @param workspace Current workspace
	 * @param launcher Current {@link Launcher}
	 * @param listener Current {@link TaskListener}
	 * @param currentErrorData Current {@link ErrorData}
	 * @return true if we can continue with the next operation, false otherwise
	 * @throws InterruptedException May be thrown by various Jenkins methods
	 * @throws IOException May be thrown by various Jenkins methods
	 */
	protected boolean perform(AbstractConfigurable describable, Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener, ErrorData currentErrorData) throws InterruptedException, IOException {
		PrintStream log = listener.getLogger();
		EnvVars env = run.getEnvironment(listener);
		if ( describable instanceof AbstractFortifySSCOp ) {
			AbstractFortifySSCOp op = (AbstractFortifySSCOp)describable;
			listener.getLogger().println("[INFO] Start operation '"+op.getDescriptor().getDisplayName()+"'");
			try {
				op.performWithCheck(getWith(), run, workspace, launcher, listener);
			} catch ( Exception e ) {
				return !op.handleException(log, env, e, currentErrorData);
			} finally {
				listener.getLogger().println("[INFO] End operation '"+op.getDescriptor().getDisplayName()+"'");
			}
		}
		return true;
	}
	
	@Symbol("performOnSSC")
	@Extension
	public static class DescriptorImpl extends AbstractDescriptorBuilder {
	
		@SuppressWarnings("rawtypes")
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}
	
		@Override
		public String getDisplayName() {
			return "Fortify SSC Jenkins Plugin";
		}
		
		@Override
	    public String getDynamicJobConfigurationAddButtonDisplayName() {
			return "Add Operation";
		}
		
	    @Override
		public String getDynamicJobConfigurationDeleteButtonDisplayName() {
			return "Remove Operation";
		}
		
		@Override
		protected AbstractGlobalConfiguration<?> getConfigurableGlobalConfiguration() {
			return FortifySSCGlobalConfiguration.get();
		}
		
		@SuppressWarnings("unchecked") // TODO How to fix this warning?
		@Override
		protected Collection<Class<? extends AbstractDescriptorConfigurable>> getDynamicJobConfigurationDescriptorTypes() {
			return Arrays.asList(AbstractFortifySSCDescriptorOp.class);
		}

		@SuppressWarnings("unchecked") // TODO How to fix this warning?
		@Override
		protected Collection<Class<? extends AbstractDescriptorConfigurable>> getStaticJobConfigurationDescriptorTypes() {
			return Arrays.asList(FortifySSCDescriptorApplicationAndVersionName.class);
		}
		
		@Override
		protected boolean includeDynamicConfigurationDescriptorsWithoutGlobalConfiguration() {
			return false;
		}
	}

	@Override
	public void save() throws IOException {
		// TODO Auto-generated method stub
	}
}
