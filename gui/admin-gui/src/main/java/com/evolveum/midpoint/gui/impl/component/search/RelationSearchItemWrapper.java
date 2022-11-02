/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationSearchItemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxScopeType;

import org.apache.commons.collections4.CollectionUtils;

import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.search.SearchValue;

import java.util.Collections;
import java.util.List;

public class RelationSearchItemWrapper extends AbstractSearchItemWrapper<QName> {

    public static final String F_SUPPORTED_RELATIONS = "supportedRelations";

    private RelationSearchItemConfigurationType relationSearchItemConfigurationType;
    private List<QName> supportedRelations;



    public RelationSearchItemWrapper(RelationSearchItemConfigurationType relationSearchItemConfigurationType) {
        super();
        this.relationSearchItemConfigurationType = relationSearchItemConfigurationType;
        this.supportedRelations = relationSearchItemConfigurationType.getSupportedRelations();
    }

    @Override
    public boolean isEnabled() {
        return CollectionUtils.isNotEmpty(relationSearchItemConfigurationType.getSupportedRelations());
    }

    //TODO should be in panel!!!
    public boolean isVisible() {
        return true;
//        return CollectionUtils.isEmpty(relationSearchItemConfigurationType.getSupportedRelations())
//                || relationSearchItemConfigurationType.getDefaultScope() == null
//                || !SearchBoxScopeType.SUBTREE.equals(getSearchConfig().getDefaultScope());
    }

    @Override
    public Class<RelationSearchItemPanel> getSearchItemPanelClass() {
        return RelationSearchItemPanel.class;
    }

    @Override
    public String getName() {
        return "relationDropDownChoicePanel.relation";
    }

    @Override
    public String getHelp() {
        return "relationDropDownChoicePanel.tooltip.relation";
    }

//    @Override
//    public DisplayableValue<QName> getDefaultValue() {
//        return new SearchValue<>();
//    }

//    @Override
//    public DisplayableValue<QName> getValue() {
//        return new Se
////        if (relationSearchItemConfigurationType.getDefaultValue() == null) {
////            return getDefaultValue();
////        }
////        return new SearchValue<>(relationSearchItemConfigurationType.getDefaultValue());
//    }

//    @Override
//    protected String getNameResourceKey() {
//        return "relationDropDownChoicePanel.relation";
//    }

//    @Override
//    protected String getHelpResourceKey() {
//        return "relationDropDownChoicePanel.tooltip.relation";
//    }

    @Override
    public String getTitle() {
        return ""; //todo
    }

    @Override
    public DisplayableValue<QName> getDefaultValue() {
        return new SearchValue<>(relationSearchItemConfigurationType.getDefaultValue());
    }

    @Override
    public <C extends Containerable> ObjectFilter createFilter(Class<C> type, PageBase pageBase, VariablesMap variables) {
        return null;
    }

    public List<QName> getRelationsForSearch() {
        QName relation = getValue().getValue();
        if (QNameUtil.match(relation, PrismConstants.Q_ANY)){
            return relationSearchItemConfigurationType.getSupportedRelations();
        }

        return Collections.singletonList(relation);
    }

    public List<QName> getSupportedRelations() {
        return supportedRelations;
    }

    //TODO should be in member search
//    @Override
//    public boolean isApplyFilter(SearchBoxModeType searchBoxMode) {
//        return !SearchBoxScopeType.SUBTREE.equals(getSearchConfig().getDefaultScope());
//    }
}
