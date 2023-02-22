/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.prism.show;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.Toggle;
import com.evolveum.midpoint.gui.api.component.TogglePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.SimulationsGuiUtil;
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

public class ChangesPanel extends BasePanel<Void> {

    public enum ChangesView {

        SIMPLE,
        ADVANCED
    }

    private static final long serialVersionUID = 1L;

    private static final String ID_TITLE = "title";
    private static final String ID_TOGGLE = "toggle";
    private static final String ID_BODY = "body";
    private static final String ID_VISUALIZATIONS = "visualizations";
    private static final String ID_VISUALIZATION = "visualization";

    private IModel<ChangesView> changesViewModel;

    private IModel<List<VisualizationDto>> changesModel;

    public ChangesPanel(String id, IModel<List<ObjectDeltaType>> deltaModel, IModel<List<VisualizationDto>> visualizationModel) {
        super(id);

        initModels(deltaModel, visualizationModel);
        initLayout();
    }

    private void initModels(IModel<List<ObjectDeltaType>> deltaModel, IModel<List<VisualizationDto>> visualizationModel) {
        changesViewModel = Model.of(ChangesView.SIMPLE);

        changesModel = visualizationModel != null ? visualizationModel : new LoadableModel<>(false) {

            @Override
            protected List<VisualizationDto> load() {
                List<VisualizationDto> result = new ArrayList<>();

                for (ObjectDeltaType delta : deltaModel.getObject()) {
                    Visualization visualization = SimulationsGuiUtil.createVisualization(delta, getPageBase());
                    result.add(new VisualizationDto(visualization));
                }

                return result;
            }
        };
    }

    protected IModel<String> createTitle() {
        return createStringResource("ChangesPanel.title");
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "card"));

        Label title = new Label(ID_TITLE, createTitle());
        add(title);

        IModel<List<Toggle<ChangesView>>> toggleModel = new LoadableModel<>(false) {

            @Override
            protected List<Toggle<ChangesView>> load() {
                List<Toggle<ChangesView>> toggles = new ArrayList<>();

                Toggle<ChangesView> simple = new Toggle<>("fa-solid fa-magnifying-glass mr-1", "ChangesView.SIMPLE");
                simple.setValue(ChangesView.SIMPLE);
                simple.setActive(changesViewModel.getObject() == simple.getValue());
                toggles.add(simple);

                Toggle<ChangesView> advanced = new Toggle<>("fa-solid fa-microscope mr-1", "ChangesView.ADVANCED");
                advanced.setValue(ChangesView.ADVANCED);
                advanced.setActive(changesViewModel.getObject() == advanced.getValue());
                toggles.add(advanced);

                return toggles;
            }
        };

        TogglePanel<ChangesView> toggle = new TogglePanel<>(ID_TOGGLE, toggleModel) {

            @Override
            protected void itemSelected(AjaxRequestTarget target, IModel<Toggle<ChangesView>> item) {
                super.itemSelected(target, item);

                onChangesViewClicked(target, item.getObject());
            }
        };
        add(toggle);

        WebMarkupContainer body = new WebMarkupContainer(ID_BODY);
        body.setOutputMarkupId(true);
        add(body);

        Component visualizations = createVisualizations();
        body.add(visualizations);
    }

    private ListView<VisualizationDto> createVisualizations() {
        return new ListView<>(ID_VISUALIZATIONS, changesModel) {

            @Override
            protected void populateItem(ListItem<VisualizationDto> item) {
                IModel<VisualizationDto> model = item.getModel();

                boolean advanced = changesViewModel.getObject() == ChangesView.ADVANCED;

                if (advanced) {
                    expandVisualization(model.getObject());
                }

                Component component;
                if (!advanced && changesModel.getObject().size() == 1) {
                    component = new MainVisualizationPanel(ID_VISUALIZATION, model, false, advanced);
                } else {
                    component = new VisualizationPanel(ID_VISUALIZATION, model, false, advanced);
                }

                item.add(component);
            }
        };
    }

    private void onChangesViewClicked(AjaxRequestTarget target, Toggle<ChangesView> toggle) {
        changesViewModel.setObject(toggle.getValue());

        Component newOne = createVisualizations();
        Component existing = get(createComponentPath(ID_BODY, ID_VISUALIZATIONS));
        existing.replaceWith(newOne);

        target.add(get(ID_BODY));
    }

    private void expandVisualization(VisualizationDto dto) {
        dto.setMinimized(false);

        List<VisualizationDto> partials = dto.getPartialVisualizations();
        if (partials == null) {
            return;
        }

        for (VisualizationDto partial : partials) {
            expandVisualization(partial);
        }
    }
}
