/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.basic.ResourceAssociationTypeBasicWizardPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeDefinitionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeSubjectDefinitionType;

import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.SchemaHandlingTypeWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.PreviewResourceObjectTypeDataWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.ResourceObjectTypeWizardPreviewPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.activation.ActivationsWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.associations.AssociationsWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attributeMapping.AttributeMappingWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.basic.ResourceObjectTypeBasicWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.capabilities.CapabilitiesWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation.CorrelationWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.credentials.CredentialsWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.synchronization.SynchronizationWizardPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
public class ResourceAssociationTypeWizardPanel extends SchemaHandlingTypeWizardPanel<ShadowAssociationTypeDefinitionType> {

    public ResourceAssociationTypeWizardPanel(
            String id,
            WizardPanelHelper<ShadowAssociationTypeDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    @Override
    protected ResourceAssociationTypeBasicWizardPanel createNewTypeWizard(String id, WizardPanelHelper<ShadowAssociationTypeDefinitionType, ResourceDetailsModel> helper) {
        return new ResourceAssociationTypeBasicWizardPanel(id, helper);
    }

    @Override
    protected ResourceAssociationTypeWizardPreviewPanel createTypePreview() {
        return new ResourceAssociationTypeWizardPreviewPanel(getIdOfChoicePanel(), createHelper(false)) {
            @Override
            protected void onTileClickPerformed(ResourceAssociationTypePreviewTileType value, AjaxRequestTarget target) {
                switch (value) {
                    case BASIC:
                        showResourceObjectTypeBasic(target);
                        break;
                    case ATTRIBUTE_MAPPING:
                        showTableForAttributes(target);
                        break;
                    case SYNCHRONIZATION:
                        showSynchronizationConfigWizard(target);
                        break;
                    case CORRELATION:
                        showCorrelationItemsTable(target);
                        break;
                    case ACTIVATION:
                        showActivationsWizard(target);
                        break;
                }

            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                super.onExitPerformed(target);
                ResourceAssociationTypeWizardPanel.this.onExitPerformed(target);
            }
        };
    }

    private void showResourceObjectTypeBasic(AjaxRequestTarget target) {
        showChoiceFragment(
                target,
                new ResourceAssociationTypeBasicWizardPanel(getIdOfChoicePanel(), createHelper(true))
        );
    }

    private void showCorrelationItemsTable(AjaxRequestTarget target) {
        showChoiceFragment(
                target,
                new CorrelationWizardPanel(
                        getIdOfChoicePanel(),
                        createHelper(
                                ItemPath.create(
                                        ShadowAssociationTypeDefinitionType.F_SUBJECT,
                                        ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION,
                                        ShadowAssociationDefinitionType.F_CORRELATION),
                                false))
        );
    }

    private void showSynchronizationConfigWizard(AjaxRequestTarget target) {
        showWizardFragment(
                target,
                new SynchronizationWizardPanel<>(
                        getIdOfWizardPanel(),
                        createHelper(
                                ItemPath.create(
                                        ShadowAssociationTypeDefinitionType.F_SUBJECT,
                                        ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION,
                                        ShadowAssociationDefinitionType.F_SYNCHRONIZATION),
                                false))
        );
    }

    private void showActivationsWizard(AjaxRequestTarget target) {
        showWizardFragment(
                target,
                new ActivationsWizardPanel(
                        getIdOfWizardPanel(),
                        createHelper(
                                ItemPath.create(
                                        ShadowAssociationTypeDefinitionType.F_SUBJECT,
                                        ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION,
                                        ShadowAssociationDefinitionType.F_ACTIVATION),
                                false))
        );
    }

    private void showTableForAttributes(AjaxRequestTarget target) {
        showWizardFragment(
                target,
                new AttributeMappingWizardPanel<>(
                        getIdOfWizardPanel(),
                        createHelper(
                                ItemPath.create(
                                        ShadowAssociationTypeDefinitionType.F_SUBJECT,
                                        ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION),
                                false))
        );
    }
}
