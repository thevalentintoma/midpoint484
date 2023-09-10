/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import com.evolveum.midpoint.web.util.ExpressionValidator;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemPanelContext;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ValueMetadataWrapperImpl;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public abstract class PrismValuePanel<T, IW extends ItemWrapper, VW extends PrismValueWrapper<T>> extends BasePanel<VW> {

    private static final transient Trace LOGGER = TraceManager.getTrace(PrismValuePanel.class);

    protected static final String ID_VALUE_FORM = "valueForm";
    private static final String ID_REMOVE_BUTTON = "removeButton";

    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_VALUE_CONTAINER = "valueContainer";

    protected static final String ID_HEADER_CONTAINER = "header";

    private static final String ID_INPUT = "input";
    private static final String ID_SHOW_METADATA = "showMetadata";

    private static final String ID_METADATA = "metadata";

    private final ItemPanelSettings settings;

    public PrismValuePanel(String id, IModel<VW> model, ItemPanelSettings settings) {
        super(id, model);
        this.settings = settings;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
        setOutputMarkupId(true);
    }

    private void initLayout() {
        MidpointForm<VW> form = new MidpointForm<>(ID_VALUE_FORM);
        form.setOutputMarkupId(true);
        add(form);
        form.add(createHeaderPanel());

        createValuePanel(form);

        createMetadataPanel(form);
    }

    private WebMarkupContainer createHeaderPanel() {

        WebMarkupContainer buttonContainer = new WebMarkupContainer(ID_HEADER_CONTAINER);

        AjaxLink<Void> removeButton = new AjaxLink<Void>(ID_REMOVE_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                try {
                    PrismValuePanel.this.remove(PrismValuePanel.this.getModelObject(), target);
                } catch (SchemaException e) {
                    LOGGER.error("Cannot remove value: {}", getModelObject());
                    getSession().error("Cannot remove value " + getModelObject());
                    target.add(getPageBase().getFeedbackPanel());
                }
            }
        };
        removeButton.add(new VisibleBehaviour(this::isRemoveButtonVisible));
        buttonContainer.add(removeButton);

        AjaxLink<Void> showMetadataButton = new AjaxLink<Void>(ID_SHOW_METADATA) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                showMetadataPerformed(PrismValuePanel.this.getModelObject(), target);
            }
        };
        buttonContainer.add(showMetadataButton);
        showMetadataButton.add(new VisibleBehaviour(() -> getModelObject() != null && getModelObject().getValueMetadata() != null && CollectionUtils.isNotEmpty(getModelObject().getValueMetadata().getValues())));

        addToHeader(buttonContainer);
        return buttonContainer;
    }

    protected void addToHeader(WebMarkupContainer headerContainer) {

    }

    private void createValuePanel(MidpointForm form) {

        GuiComponentFactory factory = null;
        if (getModelObject() != null && getModelObject().getParent() != null) {
            try {

                factory = getPageBase().getRegistry().findValuePanelFactory(getModelObject().getParent());

            } catch (Throwable e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Failed to find value panel factory for {}. Ignoring and continuing with default value panel.", e, getModelObject().getParent());
            }
        }

        WebMarkupContainer valueContainer = new WebMarkupContainer(ID_VALUE_CONTAINER);
        valueContainer.setOutputMarkupId(true);
        form.add(valueContainer);

        // feedback
        FeedbackAlerts feedback = new FeedbackAlerts(ID_FEEDBACK);

        feedback.setOutputMarkupId(true);

        if (factory == null) {
            Component defaultPanel = createDefaultPanel(ID_INPUT);
            valueContainer.add(defaultPanel);
            feedback.setFilter(new ComponentFeedbackMessageFilter(defaultPanel));
            valueContainer.add(feedback);
            return;
        }

        ItemPanelContext<T, ItemWrapper<?, ?>> panelCtx = createPanelCtx(new PropertyModel<>(getModel(), "parent"));
        panelCtx.setComponentId(ID_INPUT);
        panelCtx.setForm(getForm());
        panelCtx.setRealValueModel(getModel());
        panelCtx.setParentComponent(this);
        panelCtx.setAjaxEventBehavior(createEventBehavior());
        panelCtx.setMandatoryHandler(getMandatoryHandler());
        panelCtx.setVisibleEnableBehaviour(createVisibleEnableBehavior());
        panelCtx.setExpressionValidator(createExpressionValidator());
        panelCtx.setFeedback(feedback);

        Component component;
        try {
            component = factory.createPanel(panelCtx);
            valueContainer.add(component);
            factory.configure(panelCtx, component);
            valueContainer.add(feedback);

        } catch (Throwable e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot create panel", e);
            getSession().error("Cannot create panel");
            throw new RuntimeException(e);
        }

    }

    private ExpressionValidator createExpressionValidator() {
        ItemWrapper itemWrapper = getModelObject().getParent();
        return new ExpressionValidator(
                LambdaModel.of(() -> itemWrapper.getFormComponentValidator()), getPageBase()) {

            @Override
            protected ObjectType getObjectType() {
                return getObject();
            }
        };
    }

    protected void createMetadataPanel(MidpointForm form) {
        PrismValueMetadataPanel metadataPanel = new PrismValueMetadataPanel(ID_METADATA, new PropertyModel<>(getModel(), "valueMetadata"));
        metadataPanel.add(new VisibleBehaviour(() -> getModelObject().isShowMetadata()));
        metadataPanel.setOutputMarkupId(true);
        form.add(metadataPanel);
    }

    private AjaxEventBehavior createEventBehavior() {
        return new AjaxFormComponentUpdatingBehavior("change") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(getPageBase().getFeedbackPanel());
                target.add(getFeedback());
            }

            @Override
            protected void onError(AjaxRequestTarget target, RuntimeException e) {
                target.add(getPageBase().getFeedbackPanel());
                target.add(getFeedback());
            }

        };
    }

    private VisibleEnableBehaviour createVisibleEnableBehavior() {
        return new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return true;
            }

            @Override
            public boolean isEnabled() {
                if (getEditabilityHandler() != null) {
                    return getEditabilityHandler().isEditable(getModelObject().getParent());
                }
                return super.isEnabled();
            }
        };
    }

    protected ItemPanelSettings getSettings() {
        return settings;
    }

    protected ItemMandatoryHandler getMandatoryHandler() {
        if (settings == null) {
            return null;
        }

        return settings.getMandatoryHandler();
    }

    protected ItemEditabilityHandler getEditabilityHandler() {
        if (settings == null) {
            return null;
        }

        return settings.getEditabilityHandler();
    }

    protected abstract <PC extends ItemPanelContext> PC createPanelCtx(IModel<IW> wrapper);

    private <O extends ObjectType> O getObject() {

        PrismObjectWrapper<O> objectWrapper = getModelObject().getParent().findObjectWrapper();
        if (objectWrapper == null) {
            return null;
        }

        try {
            PrismObject<O> objectNew = objectWrapper.getObjectApplyDelta();
            return objectNew.asObjectable();
        } catch (SchemaException e) {
            LOGGER.error("Cannot apply deltas to object for validation: {}", e.getMessage(), e);
            return null;
        }
    }

    protected abstract Component createDefaultPanel(String id);

    protected abstract <PV extends PrismValue> PV createNewValue(IW itemWrapper);

    //TODO move to the ItemPanel, exception handling
    protected abstract void remove(VW valueToRemove, AjaxRequestTarget target) throws SchemaException;

    private void showMetadataPerformed(VW value, AjaxRequestTarget target) {
        boolean showMetadata = !value.isShowMetadata();
        value.setShowMetadata(showMetadata);
        getValueMetadata().unselect();

        target.add(PrismValuePanel.this);
    }

    protected boolean isRemoveButtonVisible() {
        boolean editability = true;
        if (getEditabilityHandler() != null) {
            editability = getEditabilityHandler().isEditable(getModelObject().getParent());
        }
        return editability && !getModelObject().getParent().isReadOnly() && !getModelObject().getParent().isMetadata();

    }

    protected MidpointForm<VW> getForm() {
        return (MidpointForm) get(ID_VALUE_FORM);
    }

    protected FeedbackAlerts getFeedback() {
        return (FeedbackAlerts) get(createComponentPath(ID_VALUE_FORM, ID_VALUE_CONTAINER, ID_FEEDBACK));
    }

    protected Component getValuePanel() {
        return (Component) get(createComponentPath(ID_VALUE_FORM, ID_VALUE_CONTAINER, ID_INPUT));
    }

    protected Component getValueContainer() {
        return (Component) get(createComponentPath(ID_VALUE_FORM, ID_VALUE_CONTAINER));
    }

    private ValueMetadataWrapperImpl getValueMetadata() {
        return getModelObject().getValueMetadata();
    }
}
