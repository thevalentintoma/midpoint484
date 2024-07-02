/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;

import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

public class CertificationItemsTabbedPanel extends BasePanel<AccessCertificationCampaignType> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABBED_PANEL = "tabbedPanel";

    //TODO probably better would be wrapper model?
    public CertificationItemsTabbedPanel(String id, IModel<AccessCertificationCampaignType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        mainForm.setOutputMarkupId(true);
        add(mainForm);

        IModel<List<ITab>> tabs = createTabsModel();
        TabbedPanel<ITab> tabbedPanel = WebComponentUtil.createTabPanel(ID_TABBED_PANEL, getPageBase(), tabs.getObject(), null);
        tabbedPanel.add(new VisibleBehaviour(() -> getModelObject().getStageNumber() > 0));
        mainForm.add(tabbedPanel);
    }

    private LoadableModel<List<ITab>> createTabsModel() {
        return new LoadableModel<>(true) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<ITab> load() {
                List<ITab> tabs = new ArrayList<>();
                int iteration = getModelObject().getIteration();
                int currentStage = getModelObject().getStage()
                        .stream().filter(stage -> stage.getIteration() == iteration)
                        .toList().size();
                for (int i = 1; i <= currentStage; i++) {
                    tabs.add(createCountablePanelTab(i));
                }
                return tabs;
            }
        };
    }

    private PanelTab createCountablePanelTab(int stageNumber) {
        return new PanelTab(createStringResource("CertificationItemsPanel.tabPanel.title", stageNumber)) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                CertificationCasesPanel items = new CertificationCasesPanel(panelId, getModelObject().getOid(), stageNumber);
                items.setOutputMarkupId(true);
                return items;
            }
        };
    }
}
