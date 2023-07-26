/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.details.objects;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.getFocusTypeObject;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.MiningRoleTypeChunk;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.MiningUserTypeChunk;
import com.evolveum.midpoint.gui.impl.page.admin.user.PageUser;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkIconPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

public class ProcessBusinessRolePanel extends BasePanel<String> implements Popupable {

    private static final String ID_BUTTON_OK = "ok";
    private static final String ID_CANCEL_OK = "cancel";
    List<MiningRoleTypeChunk> miningRoleTypeChunks;
    List<MiningUserTypeChunk> miningUserTypeChunks;
    RoleAnalysisProcessMode mode;

    public ProcessBusinessRolePanel(String id, IModel<String> messageModel, List<MiningRoleTypeChunk> miningRoleTypeChunks, List<MiningUserTypeChunk> miningUserTypeChunks,
            RoleAnalysisProcessMode mode) {
        super(id, messageModel);
        this.mode = mode;
        this.miningRoleTypeChunks = miningRoleTypeChunks;
        this.miningUserTypeChunks = miningUserTypeChunks;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        PageBase pageBase = (PageBase) getPage();
        OperationResult operationResult = new OperationResult("prepareObjects");

        List<PrismObject<FocusType>> elements = new ArrayList<>();
        List<PrismObject<FocusType>> points = new ArrayList<>();

        DisplayType displayTypeElement;
        DisplayType displayTypePoints;
        if (mode.equals(RoleAnalysisProcessMode.ROLE)) {

            for (MiningRoleTypeChunk miningRoleTypeChunk : miningRoleTypeChunks) {
                if (miningRoleTypeChunk.getStatus().equals(ClusterObjectUtils.Status.ADD)) {
                    List<String> roles = miningRoleTypeChunk.getRoles();
                    for (String s : roles) {
                        elements.add(getFocusTypeObject(pageBase, s, operationResult));
                    }
                }
            }

            for (MiningUserTypeChunk miningUserTypeChunk : miningUserTypeChunks) {
                if (miningUserTypeChunk.getStatus().equals(ClusterObjectUtils.Status.ADD)) {
                    List<String> users = miningUserTypeChunk.getUsers();
                    for (String s : users) {
                        points.add(getFocusTypeObject(pageBase, s, operationResult));
                    }
                }
            }

            displayTypeElement = GuiDisplayTypeUtil.createDisplayType(
                    WebComponentUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE));
            displayTypePoints = GuiDisplayTypeUtil.createDisplayType(
                    WebComponentUtil.createDefaultBlackIcon(UserType.COMPLEX_TYPE));

        } else {

            for (MiningRoleTypeChunk miningRoleTypeChunk : miningRoleTypeChunks) {
                if (miningRoleTypeChunk.getStatus().equals(ClusterObjectUtils.Status.ADD)) {
                    List<String> roles = miningRoleTypeChunk.getRoles();
                    for (String s : roles) {
                        points.add(getFocusTypeObject(pageBase, s, operationResult));
                    }
                }
            }

            for (MiningUserTypeChunk miningUserTypeChunk : miningUserTypeChunks) {
                if (miningUserTypeChunk.getStatus().equals(ClusterObjectUtils.Status.ADD)) {
                    List<String> users = miningUserTypeChunk.getUsers();
                    for (String s : users) {
                        elements.add(getFocusTypeObject(pageBase, s, operationResult));
                    }
                }
            }

            displayTypeElement = GuiDisplayTypeUtil.createDisplayType(
                    WebComponentUtil.createDefaultBlackIcon(UserType.COMPLEX_TYPE));
            displayTypePoints = GuiDisplayTypeUtil.createDisplayType(
                    WebComponentUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE));
        }

        ListView<PrismObject<FocusType>> listViewElements = new ListView<>("list_elements", elements) {
            @Override
            protected void populateItem(ListItem<PrismObject<FocusType>> listItem) {
                PrismObject<FocusType> modelObject = listItem.getModelObject();
                listItem.add(new AjaxLinkIconPanel("object",
                        createStringResource(modelObject.getName()),
                        createStringResource(modelObject.getName()), displayTypeElement) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        PageParameters parameters = new PageParameters();
                        parameters.add(OnePageParameterEncoder.PARAMETER, modelObject.getOid());

                        if (mode.equals(RoleAnalysisProcessMode.ROLE)) {
                            ((PageBase) getPage()).navigateToNext(PageRole.class, parameters);
                        } else {
                            ((PageBase) getPage()).navigateToNext(PageUser.class, parameters);
                        }
                    }

                });
            }
        };
        add(listViewElements);

        ListView<PrismObject<FocusType>> listPoints = new ListView<>("list_points", points) {
            @Override
            protected void populateItem(ListItem<PrismObject<FocusType>> listItem) {
                PrismObject<FocusType> modelObject = listItem.getModelObject();
                listItem.add(new AjaxLinkIconPanel("object",
                        createStringResource(modelObject.getName()),
                        createStringResource(modelObject.getName()), displayTypePoints) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        PageParameters parameters = new PageParameters();
                        parameters.add(OnePageParameterEncoder.PARAMETER, modelObject.getOid());

                        if (mode.equals(RoleAnalysisProcessMode.ROLE)) {
                            ((PageBase) getPage()).navigateToNext(PageUser.class, parameters);
                        } else {
                            ((PageBase) getPage()).navigateToNext(PageRole.class, parameters);
                        }
                    }

                });
            }
        };
        add(listPoints);

        addConfirmationComponents();
    }

    private void addConfirmationComponents() {
        AjaxButton confirmButton = new AjaxButton(ID_BUTTON_OK, createStringResource("Button.ok")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
            }
        };
        add(confirmButton);

        AjaxButton cancelButton = new AjaxButton(ID_CANCEL_OK,
                createStringResource("Button.cancel")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onClose(target);
            }
        };
        add(cancelButton);
    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    @Override
    public int getWidth() {
        return 1400;
    }

    @Override
    public int getHeight() {
        return 500;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return new StringResourceModel("RoleMining.todo.title");
    }
}
