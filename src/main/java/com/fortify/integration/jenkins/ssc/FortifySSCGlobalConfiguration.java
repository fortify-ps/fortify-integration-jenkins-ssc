package com.fortify.integration.jenkins.ssc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.fortify.client.ssc.api.SSCApplicationVersionAPI;
import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;
import com.fortify.integration.jenkins.ssc.describable.FortifySSCApplicationAndVersionNameGlobalConfig;
import com.fortify.integration.jenkins.ssc.describable.FortifySSCCreateApplicationVersionGlobalConfig;
import com.fortify.integration.jenkins.ssc.describable.FortifySSCUploadFPRGlobalConfig;

import hudson.Extension;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;

/**
 * Example of Jenkins global configuration.
 */
@Extension
public class FortifySSCGlobalConfiguration extends GlobalConfiguration {
	private String sscUrl ="";
	private FortifySSCApplicationAndVersionNameGlobalConfig applicationAndVersionNameConfig = null;
	private FortifySSCCreateApplicationVersionGlobalConfig createApplicationVersionConfig = null;
	private FortifySSCUploadFPRGlobalConfig uploadFPRConfig = null;
	
	private transient SSCAuthenticatingRestConnection conn = null;
	
    /** @return the singleton instance */
    public static FortifySSCGlobalConfiguration get() {
        return GlobalConfiguration.all().get(FortifySSCGlobalConfiguration.class);
    }

    public FortifySSCGlobalConfiguration() {
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
        save(); // Save immediately, so other global config sections can access SSC
    }

	public FortifySSCApplicationAndVersionNameGlobalConfig getApplicationAndVersionNameConfig() {
		System.out.println(applicationAndVersionNameConfig);
		return applicationAndVersionNameConfig;
	}

	@DataBoundSetter
	public void setApplicationAndVersionNameConfig(FortifySSCApplicationAndVersionNameGlobalConfig applicationAndVersionNameConfig) {
		this.applicationAndVersionNameConfig = applicationAndVersionNameConfig;
	}

	public FortifySSCCreateApplicationVersionGlobalConfig getCreateApplicationVersionConfig() {
		System.out.println(createApplicationVersionConfig);
		return createApplicationVersionConfig;
	}

	@DataBoundSetter
	public void setCreateApplicationVersionConfig(FortifySSCCreateApplicationVersionGlobalConfig createApplicationVersionConfig) {
		this.createApplicationVersionConfig = createApplicationVersionConfig;
	}

	public FortifySSCUploadFPRGlobalConfig getUploadFPRConfig() {
		System.out.println(uploadFPRConfig);
		return uploadFPRConfig;
	}

	@DataBoundSetter
	public void setUploadFPRConfig(FortifySSCUploadFPRGlobalConfig uploadFPRConfig) {
		this.uploadFPRConfig = uploadFPRConfig;
	}
	
	public boolean isEnabled(String name) {
		Boolean result = getEnabledMap().get(name);
		if ( result == null ) {
			throw new RuntimeException("Unknown name "+name);
		}
		return result;
	}

	private Map<String, Boolean> getEnabledMap() {
		Map<String, Boolean> result = new HashMap<>();
		addToEnabledMap(result, "createApplicationVersion", createApplicationVersionConfig);
		addToEnabledMap(result, "uploadFPR", uploadFPRConfig);
		return result;
	}

	private void addToEnabledMap(Map<String, Boolean> enabledMap, String name, Object config) {
		enabledMap.put(name, config!=null);
	}

	@Override
	public boolean configure(StaplerRequest req, JSONObject json) throws hudson.model.Descriptor.FormException {
		// Clear existing values 
		// (if de-selected in UI, json will not contain object and thus not overwrite previous config)
		setApplicationAndVersionNameConfig(null);
		setCreateApplicationVersionConfig(null);
		setUploadFPRConfig(null);
		
		System.out.println(json);
		
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
