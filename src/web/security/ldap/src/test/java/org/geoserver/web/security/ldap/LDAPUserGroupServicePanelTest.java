/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.ldap;

import static org.junit.Assert.assertNull;

import java.io.Serializable;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.ldap.LDAPTestUtils;
import org.geoserver.security.ldap.LDAPUserGroupServiceConfig;
import org.geoserver.security.web.AbstractSecurityWicketTestSupport;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.junit.After;
import org.junit.Assume;
import org.junit.Test;

/**
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 * @author Niels Charlier
 */
public class LDAPUserGroupServicePanelTest extends AbstractSecurityWicketTestSupport {

    private static final String GROUPS_BASE = "ou=Groups";

    private static final String USERS_BASE = "ou=People";

    private static final String GROUP_SEARCH_FILTER = "member=cn={0}";

    private static final String AUTH_USER = "admin";

    private static final String AUTH_PASSWORD = "secret";

    LDAPUserGroupServicePanel current;

    String relBase = "panel:";
    String base = "form:" + relBase;

    LDAPUserGroupServiceConfig config;

    FeedbackPanel feedbackPanel = null;

    private static final String ldapServerUrl = LDAPTestUtils.LDAP_SERVER_URL;
    private static final String basePath = LDAPTestUtils.LDAP_BASE_PATH;

    @After
    public void tearDown() throws Exception {
        LDAPTestUtils.shutdownEmbeddedServer();
    }

    protected void setupPanel(boolean needsAuthentication, boolean setRequiredFields) {
        config = new LDAPUserGroupServiceConfig();
        config.setName("test");
        if (setRequiredFields) {
            config.setServerURL(ldapServerUrl + "/" + basePath);
            config.setGroupSearchBase(GROUPS_BASE);
            config.setUserSearchBase(USERS_BASE);
        }
        config.setBindBeforeGroupSearch(needsAuthentication);
        config.setGroupSearchFilter(GROUP_SEARCH_FILTER);
        config.setUser(AUTH_USER);
        config.setPassword(AUTH_PASSWORD);
        setupPanel();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // disable url parameter encoding for these tests
        SecurityManagerConfig config = getSecurityManager().getSecurityConfig();
        config.setEncryptingUrlParams(false);
        getSecurityManager().saveSecurityConfig(config);
    }

    protected void setupPanel() {
        tester.startPage(
                new FormTestPage(
                        new ComponentBuilder() {
                            private static final long serialVersionUID = 1L;

                            public Component buildComponent(String id) {

                                return current =
                                        new LDAPUserGroupServicePanel(
                                                id, new Model<LDAPUserGroupServiceConfig>(config));
                            };
                        },
                        new CompoundPropertyModel<Object>(config)) {

                    private static final long serialVersionUID = -4090244876841730821L;

                    @Override
                    protected void onInitialize() {
                        feedbackPanel = new FeedbackPanel("feedback");
                        feedbackPanel.setOutputMarkupId(true);
                        add(feedbackPanel);
                        super.onInitialize();
                    }
                });
    }

    @Test
    public void testDataLoadedFromConfigurationWithoutAuthentication() throws Exception {
        Assume.assumeTrue(LDAPTestUtils.initLdapServer(true, ldapServerUrl, basePath));
        setupPanel(false, true);
        checkBaseConfig();

        assertNull(tester.getComponentFromLastRenderedPage("form:panel:authenticationPanel:user"));
        assertNull(
                tester.getComponentFromLastRenderedPage("form:panel:authenticationPanel:password"));
    }

    @Test
    public void testRequiredFields() throws Exception {
        Assume.assumeTrue(LDAPTestUtils.initLdapServer(true, ldapServerUrl, basePath));
        setupPanel(false, false);

        tester.newFormTester("form").submit();

        tester.assertErrorMessages(
                (Serializable[])
                        new String[] {
                            "Field 'Server URL' is required.",
                            "Field 'Group search base' is required.",
                            "Field 'User search base' is required."
                        });
    }

    @Test
    public void testDataLoadedFromConfigurationWithAuthentication() throws Exception {
        Assume.assumeTrue(LDAPTestUtils.initLdapServer(true, ldapServerUrl, basePath));
        setupPanel(true, true);
        checkBaseConfig();

        tester.assertModelValue("form:panel:authenticationPanel:user", AUTH_USER);
        tester.assertModelValue("form:panel:authenticationPanel:password", AUTH_PASSWORD);
    }

    @Test
    public void testAuthenticationDisabled() throws Exception {
        Assume.assumeTrue(LDAPTestUtils.initLdapServer(true, ldapServerUrl, basePath));
        setupPanel(false, true);
        tester.assertInvisible("form:panel:authenticationPanel");
        tester.newFormTester("form").setValue("panel:bindBeforeGroupSearch", "on");
        tester.executeAjaxEvent("form:panel:bindBeforeGroupSearch", "click");
        tester.assertVisible("form:panel:authenticationPanel");
    }

    public void testAuthenticationEnabled() throws Exception {
        Assume.assumeTrue(LDAPTestUtils.initLdapServer(true, ldapServerUrl, basePath));
        setupPanel(true, true);
        tester.assertVisible("form:panel:authenticationPanel");
        tester.newFormTester("form").setValue("panel:bindBeforeGroupSearch", "");
        tester.executeAjaxEvent("form:panel:bindBeforeGroupSearch", "click");
        tester.assertInvisible("form:panel:authenticationPanelContainer:authenticationPanel");
    }

    private void checkBaseConfig() {
        tester.assertModelValue("form:panel:serverURL", ldapServerUrl + "/" + basePath);
        tester.assertModelValue("form:panel:groupSearchBase", GROUPS_BASE);
        tester.assertModelValue("form:panel:groupSearchFilter", GROUP_SEARCH_FILTER);
        tester.assertModelValue(
                "form:panel:allGroupsSearchFilter", config.getAllGroupsSearchFilter());
    }
}
