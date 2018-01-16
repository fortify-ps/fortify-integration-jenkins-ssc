package com.fortify.integration.jenkins.ssc;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.fortify.client.ssc.api.SSCApplicationVersionAPI;
import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;
import com.fortify.integration.jenkins.multiaction.AbstractDescribableActionGlobal;
import com.fortify.integration.jenkins.multiaction.AbstractMultiActionGlobalConfiguration;
import com.fortify.integration.jenkins.ssc.describable.AbstractFortifySSCDescribableActionGlobal;
import com.fortify.integration.jenkins.ssc.describable.AbstractFortifySSCDescribableActionGlobal.AbstractFortifySSCDescriptorActionGlobal;
import com.fortify.integration.jenkins.ssc.describable.FortifySSCDescribableApplicationAndVersionNameGlobal;

import hudson.Extension;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;

@SuppressWarnings("rawtypes")
@Extension
public class FortifySSCGlobalConfiguration extends AbstractMultiActionGlobalConfiguration<AbstractFortifySSCDescribableActionGlobal, AbstractFortifySSCDescriptorActionGlobal, FortifySSCGlobalConfiguration> {
	private String sscUrl ="";
	private FortifySSCDescribableApplicationAndVersionNameGlobal applicationAndVersionNameConfig;
	private transient SSCAuthenticatingRestConnection conn = null;
	
    /** @return the singleton instance */
    public static final FortifySSCGlobalConfiguration get() {
        return GlobalConfiguration.all().get(FortifySSCGlobalConfiguration.class);
    }

    public FortifySSCGlobalConfiguration() {}

	public String getSscUrl() {
        return sscUrl;
    }

    @DataBoundSetter
    public void setSscUrl(String sscUrl) {
        this.sscUrl = sscUrl;
        save(); // Save immediately, so other global config sections can access SSC
    }
    
    public FortifySSCDescribableApplicationAndVersionNameGlobal getApplicationAndVersionNameConfig() {
		return applicationAndVersionNameConfig;
	}

	@DataBoundSetter
	public void setApplicationAndVersionNameConfig(FortifySSCDescribableApplicationAndVersionNameGlobal applicationAndVersionNameConfig) {
		this.applicationAndVersionNameConfig = applicationAndVersionNameConfig;
	}
    
    public SSCAuthenticatingRestConnection conn() {
    	if ( this.conn == null ) {
    		this.conn = SSCAuthenticatingRestConnection.builder().baseUrl(sscUrl).build();
    	}
    	return this.conn;
    }
	
	public final Class<?> getTargetType() {
		System.out.println("getTargetType");
		return AbstractDescribableActionGlobal.class;
	}
	
	@Override
	public boolean configure(StaplerRequest req, JSONObject json) throws hudson.model.Descriptor.FormException {
		// Clear existing values 
		// (if de-selected in UI, json will not contain object and thus not overwrite previous config)
		setApplicationAndVersionNameConfig(null);
		setEnabledActionsGlobalConfigs(new ArrayList<>());
		
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
	
	@Override
	public FortifySSCGlobalConfiguration createDefaultInstance() {
		return new FortifySSCGlobalConfiguration();
	}

	@Override
	protected Class<AbstractFortifySSCDescriptorActionGlobal> getDescriptorActionGlobalType() {
		return AbstractFortifySSCDescriptorActionGlobal.class;
	}

}
