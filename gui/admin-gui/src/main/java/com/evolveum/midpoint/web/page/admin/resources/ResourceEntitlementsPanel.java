/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.resources;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

@PanelType(name = "resourceEntitlements")
@PanelInstance(identifier = "resourceEntitlements", status = ItemStatus.NOT_CHANGED, applicableFor = ResourceType.class)
@PanelDisplay(label = "Entitlements", order = 40)
public class ResourceEntitlementsPanel extends ResourceContentTabPanel {

    public ResourceEntitlementsPanel(String id, LoadableModel<PrismObjectWrapper<ResourceType>> model, ContainerPanelConfigurationType config) {
        super(id, ShadowKindType.ENTITLEMENT, model, config);
    }
}
