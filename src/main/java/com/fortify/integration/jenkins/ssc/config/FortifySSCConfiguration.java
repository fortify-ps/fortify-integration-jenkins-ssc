package com.fortify.integration.jenkins.ssc.config;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.fortify.client.ssc.api.SSCApplicationVersionAPI;
import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;

import hudson.Extension;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;

/**
 * Example of Jenkins global configuration.
 */
@Extension
public class FortifySSCConfiguration extends GlobalConfiguration {
	private String sscUrl ="";
	private FortifySSCApplicationVersionNameConfig applicationVersionNameConfig = null;
	private FortifySSCApplicationVersionCreationConfig applicationVersionCreationConfig = null;
	
	private transient SSCAuthenticatingRestConnection conn = null;
	
    /** @return the singleton instance */
    public static FortifySSCConfiguration get() {
        return GlobalConfiguration.all().get(FortifySSCConfiguration.class);
    }

    public FortifySSCConfiguration() {
        // When Jenkins is restarted, load any saved configuration from disk.
        load();
    }
    
    public SSCAuthenticatingRestConnection conn() {
    	if ( this.conn == null ) {
    		this.conn = SSCAuthenticatingRestConnection.builder().baseUrl(sscUrl).build();
    	}
    	return this.conn;
    }

    public String getSscUrl() {
        return sscUrl;
    }

    @DataBoundSetter
    public void setSscUrl(String sscUrl) {
        this.sscUrl = sscUrl;
    }
    
    public FortifySSCApplicationVersionNameConfig getApplicationVersionNameConfig() {
    	return applicationVersionNameConfig;
	}

    @DataBoundSetter
	public void setApplicationVersionNameConfig(FortifySSCApplicationVersionNameConfig applicationVersionNameConfig) {
    	this.applicationVersionNameConfig = applicationVersionNameConfig;
	}

	
	public FortifySSCApplicationVersionCreationConfig getApplicationVersionCreationConfig() {
		return applicationVersionCreationConfig;
	}

	@DataBoundSetter
	public void setApplicationVersionCreationConfig(FortifySSCApplicationVersionCreationConfig applicationVersionCreationConfig) {
		this.applicationVersionCreationConfig = applicationVersionCreationConfig;
	}

	@Override
	public boolean configure(StaplerRequest req, JSONObject json) throws hudson.model.Descriptor.FormException {
		if ( !json.containsKey("applicationVersionNameConfig") ) {
			// Set field to null if de-selected in configuration page 
			setApplicationVersionNameConfig(null);
		}
		if ( !json.containsKey("applicationVersionCreationConfig") ) {
			// Set field to null if de-selected in configuration page 
			setApplicationVersionCreationConfig(null);
		}
		super.configure(req, json);
		save();
		return true;
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
	        return FormValidation.ok("Success");
	    } catch (RuntimeException e) {
	        return FormValidation.error("Client error : "+e.getMessage());
	    }
	}

}
