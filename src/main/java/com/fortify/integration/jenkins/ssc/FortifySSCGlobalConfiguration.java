package com.fortify.integration.jenkins.ssc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.springframework.core.OrderComparator;

import com.fortify.client.ssc.api.SSCApplicationVersionAPI;
import com.fortify.client.ssc.connection.SSCAuthenticatingRestConnection;
import com.fortify.integration.jenkins.ssc.describable.AbstractFortifySSCGlobalConfigForApplicationVersionAction;
import com.fortify.integration.jenkins.ssc.describable.AbstractFortifySSCGlobalConfigForApplicationVersionAction.AbstractFortifySSCGlobalConfigForApplicationVersionActionDescriptor;
import com.fortify.integration.jenkins.ssc.describable.AbstractFortifySSCGlobalConfiguration;
import com.fortify.integration.jenkins.ssc.describable.AbstractFortifySSCJobConfig.AbstractFortifySSCJobConfigDescriptor;
import com.fortify.integration.jenkins.ssc.describable.FortifySSCApplicationAndVersionNameGlobalConfig;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import hudson.AbortException;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

/**
 * Example of Jenkins global configuration.
 */
@Extension
public class FortifySSCGlobalConfiguration extends AbstractFortifySSCGlobalConfiguration<FortifySSCGlobalConfiguration> {
	private String sscUrl ="";
	private FortifySSCApplicationAndVersionNameGlobalConfig applicationAndVersionNameConfig;
	private ImmutableMap<Class<AbstractFortifySSCGlobalConfigForApplicationVersionAction<?>>, AbstractFortifySSCGlobalConfigForApplicationVersionAction<?>> enabledActionsGlobalConfigs;
	
	private transient SSCAuthenticatingRestConnection conn = null;
	
    /** @return the singleton instance */
    public static final FortifySSCGlobalConfiguration get() {
        return GlobalConfiguration.all().get(FortifySSCGlobalConfiguration.class);
    }

    public FortifySSCGlobalConfiguration() {
    	setEnabledActionsGlobalConfigs(getDefaultEnabledActionsGlobalConfigs());
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
    
    public FortifySSCApplicationAndVersionNameGlobalConfig getApplicationAndVersionNameConfig() {
		return applicationAndVersionNameConfig;
	}

	@DataBoundSetter
	public void setApplicationAndVersionNameConfig(FortifySSCApplicationAndVersionNameGlobalConfig applicationAndVersionNameConfig) {
		this.applicationAndVersionNameConfig = applicationAndVersionNameConfig;
	}
    
    public Collection<AbstractFortifySSCGlobalConfigForApplicationVersionAction<?>> getEnabledActionsGlobalConfigs() {
		return enabledActionsGlobalConfigs.values();
	}

    @DataBoundSetter
	public void setEnabledActionsGlobalConfigs(Collection<AbstractFortifySSCGlobalConfigForApplicationVersionAction<?>> enabledActions) {
		this.enabledActionsGlobalConfigs = Maps.uniqueIndex(enabledActions, new Function<AbstractFortifySSCGlobalConfigForApplicationVersionAction<?>, Class<AbstractFortifySSCGlobalConfigForApplicationVersionAction<?>>> () {
			@Override @SuppressWarnings("unchecked")
			public Class<AbstractFortifySSCGlobalConfigForApplicationVersionAction<?>> apply(AbstractFortifySSCGlobalConfigForApplicationVersionAction<?> input) {
				return (Class<AbstractFortifySSCGlobalConfigForApplicationVersionAction<?>>) input.getClass();
			}
		    
		    });;
	}

	public SSCAuthenticatingRestConnection conn() {
    	if ( this.conn == null ) {
    		this.conn = SSCAuthenticatingRestConnection.builder().baseUrl(sscUrl).build();
    	}
    	return this.conn;
    }
	
	public void checkEnabled(Descriptor<?> descriptor) throws AbortException {
		if ( !isEnabled(descriptor) ) {
			// TODO Replace with something like this if called from pipeline job?
			//      descriptor.getClass().getAnnotation(Symbol.class).value()[0]
			throw new AbortException("Action '"+descriptor.getDisplayName()+"' is not enabled in global configuration");
		}
	}
	
	public boolean isEnabled(Descriptor<?> descriptor) {
		if ( descriptor instanceof AbstractFortifySSCJobConfigDescriptor<?> ) {
			return enabledActionsGlobalConfigs.containsKey(((AbstractFortifySSCJobConfigDescriptor<?>)descriptor).getGlobalConfigClass());
		}
		return false;
	}
	
	public List<Descriptor<?>> getEnabledJobDescriptors() {
		List<Descriptor<?>> result = Lists.newArrayList(Iterables.transform(enabledActionsGlobalConfigs.values(),
				new Function<AbstractFortifySSCGlobalConfigForApplicationVersionAction<?>, Descriptor<?>>() {
					@Override
					public Descriptor<?> apply(AbstractFortifySSCGlobalConfigForApplicationVersionAction<?> input) {
						return input.getJobConfigDescriptor();
					}
				}));
		result.sort(new OrderComparator());
		return result;
	}
	
	public final Class<?> getTargetType() {
		System.out.println("getTargetType");
		return AbstractFortifySSCGlobalConfigForApplicationVersionAction.class;
	}
	
	@SuppressWarnings("rawtypes") // TODO Any way to avoid this warning?
	public final List<AbstractFortifySSCGlobalConfigForApplicationVersionActionDescriptor> getGlobalConfigActionDescriptors() {
		ExtensionList<AbstractFortifySSCGlobalConfigForApplicationVersionActionDescriptor> list = Jenkins.getInstance().getExtensionList(AbstractFortifySSCGlobalConfigForApplicationVersionActionDescriptor.class);
		List<AbstractFortifySSCGlobalConfigForApplicationVersionActionDescriptor> result = new ArrayList<>(list);
		result.sort(new OrderComparator());
		return result;
	}
	
	@SuppressWarnings("unchecked") // TODO Any way to avoid this warning?
	public <T> T getGlobalConfig(Class<T> type) {
		return (T) enabledActionsGlobalConfigs.get(type);
	}
	
	@SuppressWarnings("rawtypes") // TODO Any way to avoid this warning?
	private Collection<AbstractFortifySSCGlobalConfigForApplicationVersionAction<?>> getDefaultEnabledActionsGlobalConfigs() {
		return Lists.newArrayList(Iterables.transform(getGlobalConfigActionDescriptors(),
				new Function<AbstractFortifySSCGlobalConfigForApplicationVersionActionDescriptor, AbstractFortifySSCGlobalConfigForApplicationVersionAction<?>>() {
					@Override
					public AbstractFortifySSCGlobalConfigForApplicationVersionAction<?> apply(AbstractFortifySSCGlobalConfigForApplicationVersionActionDescriptor input) {
						return (AbstractFortifySSCGlobalConfigForApplicationVersionAction<?>) input.createDefaultInstance();
					}
				}));
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

}
