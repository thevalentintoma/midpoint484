/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.recomputeRoleAnalysisClusterDetectionOptions;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.simple.Tools.getScaleScript;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.detection.DetectionActionExecutorNew;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.object.DetectionOption;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.ClusterSummaryPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;

//TODO correct authorizations
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/roleAnalysisCluster", matchUrlForSecurity = "/admin/roleAnalysisCluster")
        },
        encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(
                actionUri = AuthorizationConstants.AUTZ_UI_ROLES_ALL_URL,
                label = "PageAdminRoles.auth.roleAll.label",
                description = "PageAdminRoles.auth.roleAll.description"),
        @AuthorizationAction(
                actionUri = AuthorizationConstants.AUTZ_UI_ROLE_URL,
                label = "PageRole.auth.role.label",
                description = "PageRole.auth.role.description") })

public class PageRoleAnalysisCluster extends AbstractPageObjectDetails<RoleAnalysisClusterType, ObjectDetailsModels<RoleAnalysisClusterType>> {

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(OnDomReadyHeaderItem.forScript(getScaleScript()));
    }

    @Override
    public StringResourceModel setSaveButtonTitle() {
        return ((PageBase) getPage()).createStringResource("PageAnalysisCluster.button.save");
    }

    @Override
    public void savePerformed(AjaxRequestTarget target) {

        //TODO
        OperationResult result = new OperationResult("ImportSessionObject");

        String clusterOid = getObjectDetailsModels().getObjectType().getOid();

        RoleAnalysisClusterType cluster = getObjectDetailsModels().getObjectWrapper().getObject().asObjectable();

        DetectionOption detectionOption = new DetectionOption(cluster);

        recomputeRoleAnalysisClusterDetectionOptions(clusterOid, (PageBase) getPage(), detectionOption, result);

        new DetectionActionExecutorNew(clusterOid, (PageBase) getPage(), result).executeDetectionProcess();

        PageParameters params = new PageParameters();
        params.add(OnePageParameterEncoder.PARAMETER, clusterOid);
        Class<? extends PageBase> detailsPageClass = WebComponentUtil.getObjectDetailsPage(RoleAnalysisClusterType.class);
        ((PageBase) getPage()).navigateToNext(detailsPageClass, params);
    }

    @Override
    public void afterDeletePerformed(AjaxRequestTarget target) {
        PageBase pageBase = (PageBase) getPage();
        Task task = pageBase.createSimpleTask("Recompute object");
        OperationResult result = task.getResult();

        RoleAnalysisClusterType cluster = getModelWrapperObject().getObjectOld().asObjectable();
        ObjectReferenceType roleAnalysisSessionRef = cluster.getRoleAnalysisSessionRef();
        ClusterObjectUtils.recomputeSessionStatic(result, roleAnalysisSessionRef.getOid(), cluster, pageBase);
    }

    public PageRoleAnalysisCluster() {
        super();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    @Override
    public Class<RoleAnalysisClusterType> getType() {
        return RoleAnalysisClusterType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<RoleAnalysisClusterType> summaryModel) {
        return new ClusterSummaryPanel(id, summaryModel, null);
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return createStringResource("PageMiningOperation.title");
    }

}

