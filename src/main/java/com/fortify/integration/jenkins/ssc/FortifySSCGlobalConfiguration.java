package com.fortify.integration.jenkins.ssc;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.fortify.client.ssc.api.SSCApplicationVersionAPI;
import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;
import com.fortify.integration.jenkins.configurable.AbstractConfigurableDescribableGlobalConfiguration.AbstractDescriptorConfigurableDescribableGlobalConfiguration;
import com.fortify.integration.jenkins.configurable.AbstractConfigurableGlobalConfiguration;
import com.fortify.integration.jenkins.ssc.configurable.FortifySSCDescribableApplicationAndVersionNameGlobalConfiguration.FortifySSCDescriptorApplicationAndVersionNameGlobalConfiguration;
import com.fortify.integration.jenkins.ssc.configurable.op.AbstractFortifySSCDescribableOpGlobalConfiguration.AbstractFortifySSCDescriptorOpGlobalConfiguration;

import hudson.Extension;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;

@Extension
public class FortifySSCGlobalConfiguration extends AbstractConfigurableGlobalConfiguration<FortifySSCGlobalConfiguration> {
	private String sscUrl ="";
	private transient SSCAuthenticatingRestConnection conn = null;
	
    /** @return the singleton instance */
    public static final FortifySSCGlobalConfiguration get() {
        return GlobalConfiguration.all().get(FortifySSCGlobalConfiguration.class);
    }

    public FortifySSCGlobalConfiguration() {
    	load();
    }

	public String getSscUrl() {
        return sscUrl;
    }

    @DataBoundSetter
    public void setSscUrl(String sscUrl) {
        this.sscUrl = sscUrl;
        save(); // Save immediately, so other global config sections can access SSC
    }
    
    public SSCAuthenticatingRestConnection conn() {
    	if ( this.conn == null && StringUtils.isNotBlank(sscUrl) ) {
    		this.conn = SSCAuthenticatingRestConnection.builder().baseUrl(sscUrl).build();
    	}
    	return this.conn;
    }
    
    @Override
    public String getDynamicGlobalConfigurationAddButtonDisplayName() {
		return "Enable Operation";
	}
	
    @Override
	public String getDynamicGlobalConfigurationDeleteButtonDisplayName() {
		return "Disable Operation";
	}
	
	@Override
	public boolean configure(StaplerRequest req, JSONObject json) throws hudson.model.Descriptor.FormException {
		boolean result = super.configure(req, json);
		// Update the connection objct, in case sscUrl was changed without cliking the Validate button
		this.conn = SSCAuthenticatingRestConnection.builder().baseUrl(sscUrl).build();
		save();
		return result;
	}

	public FormValidation doCheckSscUrl(@QueryParameter String value) {
        if (StringUtils.isEmpty(value)) {
            return FormValidation.error("Please specify the SSC URL.");
        }
        return FormValidation.ok();
    }
	
	public FormValidation doTestConnection(@QueryParameter("sscUrl") final String sscUrl) throws IOException, ServletException {
		try {
	    	this.conn = SSCAuthenticatingRestConnection.builder().baseUrl(sscUrl).build();
	    	conn.api(SSCApplicationVersionAPI.class).queryApplicationVersions().maxResults(1).build().getAll();
	    	// TODO Fix this if possible (automatically save and return to config page)
	        return FormValidation.ok("Success");
	    } catch (RuntimeException e) {
	        return FormValidation.error("Client error : "+e.getMessage());
	    }
	}
	
	@Override
	public FortifySSCGlobalConfiguration createDefaultInstance() {
		return new FortifySSCGlobalConfiguration();
	}
	
	@SuppressWarnings("unchecked") // TODO How to fix this warning?
	@Override
	protected Collection<Class<? extends AbstractDescriptorConfigurableDescribableGlobalConfiguration>> getDynamicGlobalConfigurationDescriptorTypes() {
		return Arrays.asList(AbstractFortifySSCDescriptorOpGlobalConfiguration.class);
	}

	@SuppressWarnings("unchecked") // TODO How to fix this warning?
	@Override
	protected Collection<Class<? extends AbstractDescriptorConfigurableDescribableGlobalConfiguration>> getStaticGlobalConfigurationDescriptorTypes() {
		return Arrays.asList(FortifySSCDescriptorApplicationAndVersionNameGlobalConfiguration.class);
	}
}
