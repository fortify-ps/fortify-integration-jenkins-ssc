package com.fortify.integration.jenkins.ssc;

import com.fortify.integration.jenkins.ssc.config.FortifySSCConfiguration;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.jvnet.hudson.test.RestartableJenkinsRule;

public class FortifySSCConfigurationTest {

    @Rule
    public RestartableJenkinsRule rr = new RestartableJenkinsRule();

    /**
     * Tries to exercise enough code paths to catch common mistakes:
     * <ul>
     * <li>missing {@code load}
     * <li>missing {@code save}
     * <li>misnamed or absent getter/setter
     * <li>misnamed {@code textbox}
     * </ul>
     */
    @Test
    public void uiAndStorage() {
    	final String sscUrl = "https://user:password@somehost.com/ssc";
        rr.then(r -> {
            assertNull("not set initially", FortifySSCConfiguration.get().getSscUrl());
            HtmlForm config = r.createWebClient().goTo("configure").getFormByName("config");
            HtmlTextInput textbox = config.getInputByName("_.sscUrl");
            textbox.setText(sscUrl);
            r.submit(config);
            assertEquals("global config page let us edit it", sscUrl, FortifySSCConfiguration.get().getSscUrl());
        });
        rr.then(r -> {
            assertEquals("still there after restart of Jenkins", sscUrl, FortifySSCConfiguration.get().getSscUrl());
        });
    }

}
