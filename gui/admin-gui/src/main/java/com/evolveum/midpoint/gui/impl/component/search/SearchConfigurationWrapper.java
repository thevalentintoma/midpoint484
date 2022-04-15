package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SearchConfigurationWrapper<C extends Containerable> implements Serializable {

    private Class<C> typeClass;

    private List<QName> allowedTypeList = new ArrayList<>();
    private List<AbstractSearchItemWrapper> itemsList = new ArrayList<>();
    private SearchBoxModeType defaultSearchBoxMode;
    private List<SearchBoxModeType> allowedModeList = new ArrayList<>();
    private List<QName> supportedRelations = new ArrayList<>();
    private QName defaultRelation;
    private String collectionViewName;
    private boolean allowAllTypeSearch;

    private boolean allowToConfigureSearchItems;
    public static final String F_INDIRECT = "config.indirectConfiguration.indirect";
    public static final String F_SCOPE = "config.scopeConfiguration.defaultValue";
    public static final String F_RELATION = "config.relationConfiguration.defaultValue";
    public static final String F_TENANT = "tenantRef";
    public static final String F_PROJECT = "projectRef";

    private ObjectReferenceType projectRef;
    private ObjectReferenceType tenantRef;

    public SearchConfigurationWrapper(Class<C> typeClass) {
        this(typeClass, null);
    }

    public SearchConfigurationWrapper(Class<C> typeClass, String collectionViewName) {
        this.typeClass = typeClass;
        this.collectionViewName = collectionViewName;
     }

    public SearchBoxModeType getDefaultSearchBoxMode() {
        return defaultSearchBoxMode;
    }

    public void setDefaultSearchBoxMode(SearchBoxModeType defaultSearchBoxMode) {
        this.defaultSearchBoxMode = defaultSearchBoxMode;
    }

    public List<SearchBoxModeType> getAllowedModeList() {
        return allowedModeList;
    }

    public SearchConfigurationWrapper addAllowedMode(SearchBoxModeType mode) {
        if (!allowedModeList.contains(mode)) {
            allowedModeList.add(mode);
        }
        return this;
    }

    public List<QName> getAllowedTypeList() {
        return allowedTypeList;
    }

    public SearchConfigurationWrapper addAllowedType(QName type) {
        if (!allowedTypeList.contains(type)) {
            allowedTypeList.add(type);
        }
        return this;
    }

    public String getCollectionViewName() {
        return collectionViewName;
    }

    public void setCollectionViewName(String collectionViewName) {
        this.collectionViewName = collectionViewName;
    }

    public Class<C> getTypeClass() {
        return typeClass;
    }

    public void setTypeClass(Class<C> typeClass) {
        this.typeClass = typeClass;
    }

    public List<AbstractSearchItemWrapper> getItemsList() {
        return itemsList;
    }

    public void addSearchItem(AbstractSearchItemWrapper searchItem) {
        itemsList.add(searchItem);
    }

    public QName getDefaultRelation() {
        return defaultRelation;
    }

    public void setDefaultRelation(QName defaultRelation) {
        this.defaultRelation = defaultRelation;
    }

    public Boolean getIndirect() {
        //todo fix
        return false;
//        return config.getIndirectConfiguration() != null ? config.getIndirectConfiguration().isIndirect() : null;
    }

    public void setIndirect(Boolean indirect) {
        //todo fix
//        if (config.getIndirectConfiguration() == null) {
//            config.setIndirectConfiguration(new IndirectSearchItemConfigurationType());
//        }
//        config.getIndirectConfiguration().setIndirect(indirect);
    }

    public boolean isAllowToConfigureSearchItems() {
        return allowToConfigureSearchItems;
    }

    public void setAllowToConfigureSearchItems(boolean allowToConfigureSearchItems) {
        this.allowToConfigureSearchItems = allowToConfigureSearchItems;
    }

    public SearchBoxScopeType getScope() {
        //todo fix
        return SearchBoxScopeType.ONE_LEVEL;
//        return config.getScopeConfiguration() != null ? config.getScopeConfiguration().getDefaultValue() : SearchBoxScopeType.ONE_LEVEL;
    }

    public void setScope(SearchBoxScopeType scope) {
        //todo fix
//        if (config.getScopeConfiguration() == null) {
//            config.setScopeConfiguration(new ScopeSearchItemConfigurationType());
//        }
//        config.getScopeConfiguration().setDefaultValue(scope);
    }

    public boolean isIndirect() {
        //todo fix
        return false;
//        return config.getIndirectConfiguration() != null
//                && config.getIndirectConfiguration().isIndirect() != null && config.getIndirectConfiguration().isIndirect().equals(Boolean.TRUE);
    }

    public List<QName> getSupportedRelations() {
        return supportedRelations; //todo fix config.getRelationConfiguration() != null ? config.getRelationConfiguration().getSupportedRelations() : new ArrayList<>();
    }

    public boolean isSearchScope(SearchBoxScopeType scopeType) {
        return getScope() != null && getScope().equals(scopeType);
    }

    public ObjectReferenceType getProjectRef() {
        return projectRef;
    }

    public void setProjectRef(ObjectReferenceType projectRef) {
        this.projectRef = projectRef;
    }

    public ObjectReferenceType getTenantRef() {
        return tenantRef;
    }

    public void setTenantRef(ObjectReferenceType tenantRef) {
        this.tenantRef = tenantRef;
    }

    public boolean isTenantEmpty() {
        return tenantRef == null || tenantRef.getOid() == null || tenantRef.getOid() == null || tenantRef.asReferenceValue().isEmpty();
    }

    public boolean isProjectEmpty() {
        return projectRef == null || projectRef.getOid() == null || projectRef.getOid() == null || projectRef.asReferenceValue().isEmpty();
    }

    public boolean isAllowAllTypeSearch() {
        return allowAllTypeSearch;
    }

    public void setAllowAllTypeSearch(boolean allowAllTypeSearch) {
        this.allowAllTypeSearch = allowAllTypeSearch;
    }
}
