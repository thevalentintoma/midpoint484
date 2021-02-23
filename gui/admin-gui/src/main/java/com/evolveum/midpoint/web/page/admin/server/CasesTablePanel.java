/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import java.util.Collection;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.web.component.data.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.data.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.cases.PageCase;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;

/**
* @author lskublik
 */

public abstract class CasesTablePanel extends MainObjectListPanel<CaseType> {

    private static final long serialVersionUID = 1L;

    public CasesTablePanel(String id) {
        this(id, null);
    }

    public CasesTablePanel(String id, Collection<SelectorOptions<GetOperationOptions>> options) {
        super(id, CaseType.class, options);
    }

    @Override
    protected void objectDetailsPerformed(AjaxRequestTarget target, CaseType object) {
        PageParameters pageParameters = new PageParameters();
        pageParameters.add(OnePageParameterEncoder.PARAMETER, object.getOid());
        getPageBase().navigateToNext(PageCase.class, pageParameters);
    }

    @Override
    protected List<IColumn<SelectableBean<CaseType>, String>> createDefaultColumns() {
        return ColumnUtils.getDefaultCaseColumns(getPageBase(), isDashboard());
    }

    @Override
    protected ISelectableDataProvider<CaseType, SelectableBean<CaseType>> createProvider() {
        PageStorage storage = getPageStorage();
        SelectableBeanObjectDataProvider<CaseType> provider = new SelectableBeanObjectDataProvider<CaseType>(
                getPageBase(), getSearchModel(), null) {
            private static final long serialVersionUID = 1L;

            @Override
            protected PageStorage getPageStorage() {
                return storage;
            }

            @NotNull
            @Override
            protected List<ObjectOrdering> createObjectOrderings(SortParam<String> sortParam) {
                return WebComponentUtil.createMetadataOrdering(sortParam, "createTimestamp", getPrismContext());
            }

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                return CasesTablePanel.this.getCustomizeContentQuery();
            }
        };
        provider.setCompiledObjectCollectionView(getObjectCollectionView());
        provider.setOptions(createOptions());
        provider.setSort(MetadataType.F_CREATE_TIMESTAMP.getLocalPart(), SortOrder.DESCENDING);

        return provider;
    }

    @Override
    protected boolean isCreateNewObjectEnabled() {
        return false;
    }

    @Override
    protected ObjectQuery getCustomizeContentQuery() {
        ObjectFilter casesFilter = getCasesFilter();
        ObjectQuery query = null;
        if (casesFilter != null){
            query = CasesTablePanel.this.getPageBase().getPrismContext().queryFactory().createQuery(casesFilter);
        }
        return query;
    }

    @Override
    protected IColumn<SelectableBean<CaseType>, String> createCheckboxColumn() {
        if (isDashboard()){
            return null;
        } else {
            return super.createCheckboxColumn();
        }
    }

    @Override
    protected boolean isHeaderVisible() {
        return !isDashboard();
    }

    protected ObjectFilter getCasesFilter(){
        return null;
    }

    @Override
    protected boolean hideFooterIfSinglePage() {
        return isDashboard();
    }

    protected boolean isDashboard(){
        return false;
    }
}
