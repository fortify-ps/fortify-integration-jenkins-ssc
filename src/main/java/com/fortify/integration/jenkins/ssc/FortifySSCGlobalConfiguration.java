package com.fortify.integration.jenkins.ssc;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.fortify.client.ssc.api.SSCApplicationVersionAPI;
import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;
import com.fortify.integration.jenkins.multiaction.AbstractMultiActionGlobalConfiguration;
import com.fortify.integration.jenkins.ssc.describable.AbstractFortifySSCDescribableStaticGlobalConfiguration.AbstractFortifySSCDescriptorStaticGlobalConfiguration;
import com.fortify.integration.jenkins.ssc.describable.action.AbstractFortifySSCDescribableActionGlobalConfiguration.AbstractFortifySSCDescriptorActionGlobalConfiguration;

import hudson.Extension;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;

@Extension
public class FortifySSCGlobalConfiguration extends AbstractMultiActionGlobalConfiguration<FortifySSCGlobalConfiguration> {
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
    public boolean isShowDynamicGlobalConfigurationsList() {
    	return FortifySSCGlobalConfiguration.get().isConnectionAvailable();
    }
    
    @Override
    public boolean isShowStaticGlobalConfigurationsList() {
    	return FortifySSCGlobalConfiguration.get().isConnectionAvailable();
    }
    
    @Override
    public String getDynamicGlobalConfigurationAddButtonDisplayName() {
		return "Enable Action";
	}
	
    @Override
	public String getDynamicGlobalConfigurationDeleteButtonDisplayName() {
		return "Disable Action";
	}
	
	@Override
	public boolean configure(StaplerRequest req, JSONObject json) throws hudson.model.Descriptor.FormException {
		boolean result = super.configure(req, json);
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
	    	SSCAuthenticatingRestConnection conn = SSCAuthenticatingRestConnection.builder().baseUrl(sscUrl).build();
	    	conn.api(SSCApplicationVersionAPI.class).queryApplicationVersions().maxResults(1).build().getAll();
	    	// TODO Fix this if possible (automatically save and return to config page)
	        return FormValidation.warning("Success; please apply the changes and refresh this page to see additional configuration options");
	    } catch (RuntimeException e) {
	        return FormValidation.error("Client error : "+e.getMessage());
	    }
	}
	
	// TODO Remove code duplication with method above
	public boolean isConnectionAvailable() {
		try {
	    	SSCAuthenticatingRestConnection conn = SSCAuthenticatingRestConnection.builder().baseUrl(sscUrl).build();
	    	conn.api(SSCApplicationVersionAPI.class).queryApplicationVersions().maxResults(1).build().getAll();
	        return true;
	    } catch (RuntimeException e) {
	        return false;
	    }
	}
	
	@Override
	public FortifySSCGlobalConfiguration createDefaultInstance() {
		return new FortifySSCGlobalConfiguration();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected Class<AbstractFortifySSCDescriptorActionGlobalConfiguration> getDynamicGlobalConfigurationDescriptorType() {
		return AbstractFortifySSCDescriptorActionGlobalConfiguration.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Class<AbstractFortifySSCDescriptorStaticGlobalConfiguration> getStaticGlobalConfigurationDescriptorType() {
		return AbstractFortifySSCDescriptorStaticGlobalConfiguration.class;
	}

}
