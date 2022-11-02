package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.prism.Containerable;

import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxModeType;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

public class AxiomSearchPanel<C extends Containerable> extends BasePanel<AxiomQueryWrapper<C>> {

    private static final String ID_AXIOM_QUERY_FIELD = "axiomQueryField";

    public AxiomSearchPanel(String id, IModel<AxiomQueryWrapper<C>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        TextField<String> queryDslField = new TextField<>(ID_AXIOM_QUERY_FIELD,
                new PropertyModel<>(getModel(), com.evolveum.midpoint.web.component.search.Search.F_DSL_QUERY));
        queryDslField.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        queryDslField.add(AttributeAppender.append("placeholder", getPageBase().createStringResource("SearchPanel.insertAxiomQuery")));
//        queryDslField.add(createVisibleBehaviour(SearchBoxModeType.AXIOM_QUERY));
//        queryDslField.add(AttributeAppender.append("class", createValidityStyle()));
        add(queryDslField);
    }

}
