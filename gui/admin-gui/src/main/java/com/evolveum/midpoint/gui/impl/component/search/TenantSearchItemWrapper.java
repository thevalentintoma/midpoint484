/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;

import javax.xml.namespace.QName;
import java.util.List;

public class TenantSearchItemWrapper extends AbstractSearchItemWrapper<ObjectReferenceType> {

    private UserInterfaceFeatureType tenantConfig;

    public TenantSearchItemWrapper(UserInterfaceFeatureType tenantConfig) {
        super();
        this.tenantConfig = tenantConfig;
    }

    //TODO in  panel!
//    @Override
//    public boolean isEnabled() {
//        return !getSearchConfig().isIndirect();
//    }
//
//    @Override
//    public boolean isVisible() {
//        return !getSearchConfig().isIndirect();
//    }

    @Override
    public Class<TenantSearchItemPanel> getSearchItemPanelClass() {
        return TenantSearchItemPanel.class;
    }

    @Override
    public DisplayableValue<ObjectReferenceType> getDefaultValue() {
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setType(OrgType.COMPLEX_TYPE);
        return new SearchValue<>(ref);
    }

    @Override
    public <C extends Containerable> ObjectFilter createFilter(Class<C> type, PageBase pageBase, VariablesMap variables) {
        return null;
    }

//    @Override
//    public DisplayableValue<ObjectReferenceType> getValue() {
////        if (tenantConfig.get.getTenantRef() == null) {
////            return getDefaultValue();
////        }
////        return new SearchValue<>(getSearchConfig().getTenantRef());
//    }

    @Override
    public String getName() {
        return "abstractRoleMemberPanel.tenant";
    }

//    @Override
//    public String getHelp() {
//        return "";
//    }

    @Override
    public String getHelp() {
        String help = tenantConfig.getDisplay() != null ? WebComponentUtil.getTranslatedPolyString(tenantConfig.getDisplay().getHelp()) : null;
        if (help != null) {
            return help;
        }
        help = getTenantDefinition().getHelp();
        if (StringUtils.isNotEmpty(help)) {
            return help;
        }
        return getTenantDefinition().getDocumentation();
    }

    @Override
    public String getTitle() {
        return ""; //todo
    }

//    public boolean isApplyFilter() {
//        //todo check
//        return SearchBoxScopeType.SUBTREE.equals(getSearchConfig().getDefaultScope());
//    }

    public PrismReferenceDefinition getTenantDefinition() {
        return getReferenceDefinition(AssignmentType.F_TENANT_REF);
    }

    protected PrismReferenceDefinition getReferenceDefinition(ItemName refName) {
        return PrismContext.get().getSchemaRegistry()
                .findContainerDefinitionByCompileTimeClass(AssignmentType.class)
                .findReferenceDefinition(refName);
    }

}
