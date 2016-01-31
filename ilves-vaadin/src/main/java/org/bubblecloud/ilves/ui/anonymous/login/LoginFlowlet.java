/**
 * Copyright 2013 Tommi S.E. Laukkanen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bubblecloud.ilves.ui.anonymous.login;

import com.vaadin.event.MouseEvents;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.themes.ValoTheme;
import org.apache.log4j.Logger;
import org.bubblecloud.ilves.component.flow.AbstractFlowlet;
import org.bubblecloud.ilves.model.AuthenticationDeviceType;
import org.bubblecloud.ilves.model.Company;
import org.bubblecloud.ilves.security.OAuthService;
import org.bubblecloud.ilves.security.SiteAuthenticationService;
import org.bubblecloud.ilves.util.OpenIdUtil;

import java.util.Map;
import java.util.UUID;

/**
 * Login Flowlet.
 *
 * @author Tommi S.E. Laukkanen
 */
public final class LoginFlowlet extends AbstractFlowlet {
    /** Default serial version UID. */
    private static final long serialVersionUID = 1L;

    /** The logger. */
    private static final Logger LOGGER = Logger.getLogger(LoginFlowlet.class);
    private TextField usernameField;
    private PasswordField passwordField;

    public PasswordField getPasswordField() {
        return passwordField;
    }

    public TextField getUsernameField() {
        return usernameField;
    }


    @Override
    public String getFlowletKey() {
        return "login";
    }

    @SuppressWarnings("serial")
    @Override
    public void initialize() {
        final Company company = getSite().getSiteContext().getObject(Company.class);

        final VerticalLayout layout = new VerticalLayout();
        layout.setMargin(false);
        layout.setSpacing(true);

        final Panel loginPanel = new Panel(getSite().localize("header-password-login"));
        layout.addComponent(loginPanel);

        final VerticalLayout passwordLoginLayout = new VerticalLayout();
        loginPanel.setContent(passwordLoginLayout);
        passwordLoginLayout.setMargin(true);
        passwordLoginLayout.setSpacing(true);

        usernameField = new TextField(getSite().localize("label-username"));
        usernameField.setId("username");
        usernameField.setWidth(100, Unit.PERCENTAGE);
        passwordLoginLayout.addComponent(usernameField);

        passwordField = new PasswordField(getSite().localize("label-password"));
        passwordField.setId("password");
        passwordField.setWidth(100, Unit.PERCENTAGE);
        passwordLoginLayout.addComponent(passwordField);

        final Button loginButton = new Button(getSite().localize("button-login"));
        loginButton.setStyleName(ValoTheme.BUTTON_PRIMARY);
        loginButton.setId("login");
        passwordLoginLayout.addComponent(loginButton);
        loginButton.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                final String emailAddress = usernameField.getValue().toLowerCase();
                final char[] password = passwordField.getValue().toCharArray();
                final AuthenticationDeviceType authenticationDeviceType = SiteAuthenticationService.getAuthenticationDeviceType(emailAddress);
                if (authenticationDeviceType == AuthenticationDeviceType.NONE) {
                    SiteAuthenticationService.login(emailAddress, password, null, UUID.randomUUID().toString());
                } else if (authenticationDeviceType == AuthenticationDeviceType.GOOGLE_AUTHENTICATOR) {
                    getFlow().forward(GoogleAuthenticatorFlowlet.class);
                }
            }
        });

        if (company.isSelfRegistration()) {
            final Button registerButton = new Button(getSite().localize("button-register") + " >>");
            registerButton.addClickListener(new ClickListener() {
                @Override
                public void buttonClick(final ClickEvent event) {
                    getFlow().forward(RegisterFlowlet.class);
                }
            });
            passwordLoginLayout.addComponent(registerButton);
        }

        if (company.isEmailPasswordReset()) {
            final Button forgotPasswordButton = new Button(getSite().localize("button-forgot-password") + " >>");
            forgotPasswordButton.addClickListener(new ClickListener() {
                @Override
                public void buttonClick(final ClickEvent event) {
                    getFlow().forward(ForgotPasswordFlowlet.class);
                }
            });
            passwordLoginLayout.addComponent(forgotPasswordButton);
        }

        /*
        try {
            final CustomLayout loginFormLayout = new CustomLayout(
                    JadeUtil.parse("/VAADIN/themes/ilves/layouts/login.jade"));
            Responsive.makeResponsive(loginFormLayout);
            layout.addComponent(loginFormLayout);

        } catch (final IOException e) {
            throw new SiteException("Error loading login form.", e);
        }
        */

        if (company.isOpenIdLogin()) {
            final Panel mainPanel = new Panel(getSite().localize("header-openid-login"));
            layout.addComponent(mainPanel);
            final HorizontalLayout openIdLayout = new HorizontalLayout();
            mainPanel.setContent(openIdLayout);
            openIdLayout.setMargin(true);
            openIdLayout.setSpacing(true);
            final String returnViewName = "openidlogin";
            final Map<String, String> urlIconMap = OpenIdUtil.getOpenIdProviderUrlIconMap();
            for (final String url : urlIconMap.keySet()) {
                openIdLayout.addComponent(OpenIdUtil.getLoginButton(url,urlIconMap.get(url), returnViewName));
            }
        }

        if (company.isoAuthLogin()) {
            final Panel oauthLoginPanel = new Panel(getSite().localize("header-oauth-login"));
            layout.addComponent(oauthLoginPanel);
            final HorizontalLayout oauthLoginLayout = new HorizontalLayout();
            oauthLoginPanel.setContent(oauthLoginLayout);
            oauthLoginLayout.setMargin(true);
            oauthLoginLayout.setSpacing(true);
            final Embedded embedded = new Embedded(null, getSite().getIcon("openid/github_32"));
            embedded.setStyleName("image-button");
            embedded.addClickListener(new MouseEvents.ClickListener() {
                @Override
                public void click(MouseEvents.ClickEvent event) {
                    try {
                        final String locationUri = OAuthService.requestOAuthLocationUri(getSite().getSiteContext());
                        if (locationUri != null) {
                            getUI().getPage().setLocation(locationUri);
                        }
                    } catch (final Exception e) {
                        LOGGER.error("Error in requesting OAuth location URI.", e);
                        Notification.show("Error in requesting OAuth location URI.", Notification.Type.ERROR_MESSAGE);
                    }

                }
            });
            oauthLoginLayout.addComponent(embedded);
        }

        setViewContent(layout);

    }


    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public void enter() {
        if (getSite().getSecurityProvider().getUser() != null) {
            UI.getCurrent().getNavigator().navigateTo(getSite().getCurrentNavigationVersion().getDefaultPageName());
        }
    }

}
