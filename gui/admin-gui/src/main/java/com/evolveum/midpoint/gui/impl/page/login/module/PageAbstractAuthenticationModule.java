/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.login.module;

import java.io.Serial;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.authentication.api.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.gui.impl.page.login.AbstractPageLogin;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.AuthenticationSequenceTypeUtil;
import com.evolveum.midpoint.schema.util.SecurityPolicyUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Umbrella class for each page created for any authentication module.
 *
 * If new module is added, page should be created extending this class.
 * This class do the common stuff such as preparing form with CSRF field,
 * adding links to actions such as self-registration, password reset,
 * username recovery.
 */
public abstract class PageAbstractAuthenticationModule<MA extends ModuleAuthentication> extends AbstractPageLogin {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CSRF_FIELD = "csrfField";
    private static final String ID_FORM = "form";

    private static final String DOT_CLASS = PageLogin.class.getName() + ".";
    protected static final String OPERATION_LOAD_RESET_PASSWORD_POLICY = DOT_CLASS + "loadPasswordResetPolicy";

    private static final String OPERATION_GET_SECURITY_POLICY = DOT_CLASS + "getSecurityPolicy";

    private final LoadableDetachableModel<SecurityPolicyType> securityPolicyModel;

    private static final Trace LOGGER = TraceManager.getTrace(PageLogin.class);

    private static final String ID_LOGIN_RECOVERY = "loginRecovery";
    private static final String ID_RESET_PASSWORD = "resetPassword";
    private static final String ID_SELF_REGISTRATION = "selfRegistration";


    public PageAbstractAuthenticationModule(PageParameters parameters) {
        super(parameters);

        this.securityPolicyModel = new LoadableDetachableModel<>() {
            @Override
            protected SecurityPolicyType load() {
                Task task = createAnonymousTask(OPERATION_LOAD_RESET_PASSWORD_POLICY);
                OperationResult parentResult = new OperationResult(OPERATION_LOAD_RESET_PASSWORD_POLICY);
                try {
                    return getModelInteractionService().getSecurityPolicy((PrismObject<? extends FocusType>) null,
                            getArchetypeOid(), task, parentResult);
                } catch (CommonException e) {
                    LOGGER.warn("Cannot read credentials policy: " + e.getMessage(), e);
                }
                return null;
            }
        };
    }



    public PageAbstractAuthenticationModule() {
        this(null);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    protected UserType searchUser() {
        GuiProfiledPrincipal principal = AuthUtil.getPrincipalUser();
        if (principal != null) {
            FocusType focus = principal.getFocus();
            return (UserType) focus;
        }
        return null;
    }

    @Override
    protected final void initCustomLayout() {

        MidpointForm form = new MidpointForm(ID_FORM);
        form.add(AttributeModifier.replace("action", (IModel<String>) this::getUrlProcessingLogin));
        add(form);

        SecurityPolicyType securityPolicy = loadSecurityPolicyType();
        addLoginRecoveryLink(securityPolicy);
        addForgotPasswordLink(securityPolicy);
        addRegistrationLink(securityPolicy);

        WebMarkupContainer csrfField = SecurityUtils.createHiddenInputForCsrf(ID_CSRF_FIELD);
        form.add(csrfField);
        initModuleLayout(form);
    }

    protected abstract void initModuleLayout(MidpointForm form);


    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        confirmAuthentication();
    }

    @Override
    protected void onAfterRender() {
        super.onAfterRender();
    }


    protected void confirmAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean loginPageForAnotherModule = false;
        if (authentication instanceof MidpointAuthentication mpAuthentication) {
            PageDescriptor descriptor = getClass().getAnnotation(PageDescriptor.class);
            if (descriptor != null && !descriptor.authModule().isEmpty()) {
                ModuleAuthentication module = mpAuthentication.getProcessingModuleAuthentication();
                if (module != null) {
                    loginPageForAnotherModule = !module.getModuleTypeName().equals(descriptor.authModule());
                }
            }
        }

        if (authentication.isAuthenticated() || loginPageForAnotherModule) {
            MidPointApplication app = getMidpointApplication();
            throw new RestartResponseException(app.getHomePage());
        }
    }

    private void addLoginRecoveryLink(SecurityPolicyType securityPolicy) {
        String loginRecoveryUrl = getLoginRecoveryUrl(securityPolicy);
        addExternalLink(ID_LOGIN_RECOVERY, loginRecoveryUrl);
    }

    private void addExternalLink(String componentId, String linkUrl) {
        ExternalLink link = new ExternalLink(componentId, linkUrl);
        link.add(new VisibleBehaviour(() -> StringUtils.isNotBlank(linkUrl) && isLoginAndFirstModule()));
        add(link);
    }

    private boolean isLoginAndFirstModule() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof MidpointAuthentication mpAuthentication)) {
            return true;
        }

        AuthenticationSequenceType sequenceType = mpAuthentication.getSequence();
        if (!AuthenticationSequenceTypeUtil.hasChannelId(sequenceType, SecurityPolicyUtil.DEFAULT_CHANNEL)) {
            return false;
        }

        ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
        return mpAuthentication.getIndexOfModule(moduleAuthentication) == 0;

    }

    private String getLoginRecoveryUrl(SecurityPolicyType securityPolicy) {
        var loginRecoveryPolicy = securityPolicy.getLoginNameRecovery();
        if (loginRecoveryPolicy == null) {
            return "";
        }
        return getAuthLinkUrl(loginRecoveryPolicy.getAuthenticationSequenceIdentifier(), securityPolicy);
    }

    private void addForgotPasswordLink(SecurityPolicyType securityPolicy) {
        String urlResetPass = getPasswordResetUrl(securityPolicy);
        addExternalLink(ID_RESET_PASSWORD, urlResetPass);
    }

    private String getPasswordResetUrl(SecurityPolicyType securityPolicy) {
        String resetSequenceIdOrName = getResetPasswordAuthenticationSequenceName(securityPolicy);
        if (StringUtils.isBlank(resetSequenceIdOrName)) {
            return "";
        }
        return getAuthLinkUrl(resetSequenceIdOrName, securityPolicy);
    }

    private void addRegistrationLink(SecurityPolicyType securityPolicyType) {

        String urlRegistration = getRegistrationUrl(securityPolicyType);
        addExternalLink(ID_SELF_REGISTRATION, urlRegistration);
    }

    private SecurityPolicyType loadSecurityPolicyType() {
        return securityPolicyModel.getObject();
    }

    private String getResetPasswordAuthenticationSequenceName(SecurityPolicyType securityPolicyType) {
        if (securityPolicyType == null) {
            return null;
        }

        CredentialsResetPolicyType credentialsResetPolicyType = securityPolicyType.getCredentialsReset();
        if (credentialsResetPolicyType == null) {
            return null;
        }

        return credentialsResetPolicyType.getAuthenticationSequenceName();
    }

    private String getRegistrationUrl(SecurityPolicyType securityPolicy) {
        SelfRegistrationPolicyType selfRegistrationPolicy = SecurityPolicyUtil.getSelfRegistrationPolicy(securityPolicy);
        if (selfRegistrationPolicy == null || StringUtils.isBlank(selfRegistrationPolicy.getAdditionalAuthenticationSequence())) {
            return "";
        }
        return getAuthLinkUrl(selfRegistrationPolicy.getAdditionalAuthenticationSequence(), securityPolicy);
    }

    private String getAuthLinkUrl(String sequenceIdentifier, SecurityPolicyType securityPolicy) {
        String channelUrlSuffix = SecurityUtils.getChannelUrlSuffixFromAuthSequence(sequenceIdentifier, securityPolicy);
        if (StringUtils.isEmpty(channelUrlSuffix)) {
            LOGGER.warn("Authentication sequence '{}' does not exist", sequenceIdentifier);
            return "";
        }
        return "./" + ModuleWebSecurityConfiguration.DEFAULT_PREFIX_OF_MODULE + "/" + channelUrlSuffix;
    }

    protected String getUrlProcessingLogin() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof MidpointAuthentication mpAuthentication) {
                ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
                if (moduleAuthentication != null){
                    String prefix = moduleAuthentication.getPrefix();
                    return AuthUtil.stripSlashes(prefix) + "/spring_security_login";
                }
            }

            String key = "web.security.flexAuth.unsupported.auth.type";
            error(getString(key));
            return "/midpoint/spring_security_login";

    }

    @Override
    protected void onDetach() {
        securityPolicyModel.detach();
        super.onDetach();
    }

    @Override
    protected final boolean isBackButtonVisible() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof MidpointAuthentication mpAuthentication)) {
            return false;
        }
        int processingModuleIndex = mpAuthentication.getIndexOfModule(mpAuthentication.getProcessingModuleAuthentication());
        if (processingModuleIndex == 0
                && !AuthenticationSequenceTypeUtil.hasChannelId(mpAuthentication.getSequence(), SecurityPolicyUtil.DEFAULT_CHANNEL)) {
            return true;
        }
        return  processingModuleIndex > 0;
    }

    protected MA getAuthenticationModuleConfiguration() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof MidpointAuthentication mpAuthentication)) {
            getSession().error(getString("No midPoint authentication is found"));
            throw new RestartResponseException(PageError.class);
        }
        //noinspection unchecked
        return (MA) mpAuthentication.getProcessingModuleAuthentication();
    }

    //TODO should be here?
    protected SecurityPolicyType resolveSecurityPolicy(PrismObject<UserType> user) {
        return runPrivileged((Producer<SecurityPolicyType>) () -> {

            Task task = createAnonymousTask(OPERATION_GET_SECURITY_POLICY);
            task.setChannel(SchemaConstants.CHANNEL_SELF_REGISTRATION_URI);
            OperationResult result = new OperationResult(OPERATION_GET_SECURITY_POLICY);

            try {
                return getModelInteractionService().getSecurityPolicy(user, getArchetypeOid(), task, result);
            } catch (CommonException e) {
                LOGGER.error("Could not retrieve security policy: {}", e.getMessage(), e);
                return null;
            }

        });
    }

    public MidpointForm<?> getForm() {
        return (MidpointForm<?>) get(ID_FORM);
    }

    protected String getArchetypeOid() {
        return null;
    }

}
