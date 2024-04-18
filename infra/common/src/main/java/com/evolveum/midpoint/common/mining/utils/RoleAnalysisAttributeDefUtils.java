/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.utils;

import static com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef.extractRealValue;

import java.util.*;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.*;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RoleAnalysisAttributeDefUtils {

    public static final ItemName F_NAME = new ItemName(ObjectFactory.NAMESPACE, "name");
    public static final ItemName F_TITLE = new ItemName(ObjectFactory.NAMESPACE, "title");
    public static final ItemName F_ORGANIZATION = new ItemName(ObjectFactory.NAMESPACE, "organization");
    public static final ItemName F_ORGANIZATIONAL_UNIT = new ItemName(ObjectFactory.NAMESPACE, "organizationalUnit");
    public static final ItemName F_LOCALE = new ItemName(ObjectFactory.NAMESPACE, "locale");
    public static final ItemName F_LOCALITY = new ItemName(ObjectFactory.NAMESPACE, "locality");
    public static final ItemName F_COST_CENTER = new ItemName(ObjectFactory.NAMESPACE, "costCenter");
    public static final ItemName F_LIFECYCLE_STATE = new ItemName(ObjectFactory.NAMESPACE, "lifecycleState");
    public static final ItemName F_RISK_LEVEL = new ItemName(ObjectFactory.NAMESPACE, "riskLevel");

    public static RoleAnalysisAttributeDef getObjectNameDef() {
        return name;
    }

    public static RoleAnalysisAttributeDef name = new RoleAnalysisAttributeDef(
            F_NAME,
            false,
            "name",
            ObjectType.class,
            RoleAnalysisAttributeDef.IdentifierType.FINAL) {
        @Override
        public ObjectQuery getQuery(String value) {
            return PrismContext.get().queryFor(ObjectType.class)
                    .item(getPath()).eq(value)
                    .build();
        }
    };

    public static RoleAnalysisAttributeDef getOrgAssignment() {
        return orgAssignment;
    }

    public static RoleAnalysisAttributeDef orgAssignment = getRoleAnalysisItemDefAssignment(
            FocusType.F_ASSIGNMENT, "org assignment", OrgType.COMPLEX_TYPE);
    public static RoleAnalysisAttributeDef roleAssignment = getRoleAnalysisItemDefAssignment(
            FocusType.F_ASSIGNMENT, "role assignment", RoleType.COMPLEX_TYPE);
    public static RoleAnalysisAttributeDef serviceAssignment = getRoleAnalysisItemDefAssignment(
            FocusType.F_ASSIGNMENT, "service assignment", ServiceType.COMPLEX_TYPE);
    public static RoleAnalysisAttributeDef archetypeAssignment = getRoleAnalysisItemDefAssignment(
            FocusType.F_ASSIGNMENT, "archetype assignment", ArchetypeType.COMPLEX_TYPE);
    public static RoleAnalysisAttributeDef resourceAssignment = getRoleAnalysisItemDefAssignment(
            FocusType.F_ASSIGNMENT, "resource assignment", ResourceType.COMPLEX_TYPE);

    public static RoleAnalysisAttributeDef orgInducement = getRoleAnalysisItemDefAssignment(
            OrgType.F_INDUCEMENT, "org inducement", OrgType.COMPLEX_TYPE);

    public static RoleAnalysisAttributeDef roleInducement = getRoleAnalysisItemDefAssignment(
            RoleType.F_INDUCEMENT, "role inducement", RoleType.COMPLEX_TYPE);

    public static RoleAnalysisAttributeDef serviceInducement = getRoleAnalysisItemDefAssignment(
            ServiceType.F_INDUCEMENT, "service inducement", ServiceType.COMPLEX_TYPE);

    public static RoleAnalysisAttributeDef archetypeInducement = getRoleAnalysisItemDefAssignment(
            ArchetypeType.F_INDUCEMENT, "archetype inducement", ArchetypeType.COMPLEX_TYPE);

    public static RoleAnalysisAttributeDef getArchetypeRef() {
        return archetypeRef;
    }

    public static RoleAnalysisAttributeDef archetypeRef = new RoleAnalysisAttributeDef(
            ItemPath.create(FocusType.F_ARCHETYPE_REF),
            false,
            "archetype ref",
            ArchetypeType.class,
            RoleAnalysisAttributeDef.IdentifierType.OID) {

        @Override
        public ObjectQuery getQuery(String value) {
            return PrismContext.get().queryFor(FocusType.class)
                    .item(getPath()).ref(value)
                    .build();
        }

        @Override
        public String resolveSingleValueItem(@NotNull PrismObject<?> prismObject, @NotNull ItemPath itemPath) {
            Item<PrismValue, ItemDefinition<?>> property = prismObject.findItem(itemPath);

            if (property == null) {
                return null;
            }

            Object realValue = property.getRealValue();
            if (realValue instanceof ObjectReferenceType objectReference) {
                return objectReference.getOid();
            }
            return null;
        }
    };

    public static RoleAnalysisAttributeDef getTitle() {
        return title;
    }

    public static RoleAnalysisAttributeDef title = new RoleAnalysisAttributeDef(
            F_TITLE,
            false,
            "title",
            UserType.class,
            RoleAnalysisAttributeDef.IdentifierType.FINAL) {
        @Override
        public ObjectQuery getQuery(String value) {
            return PrismContext.get().queryFor(UserType.class)
                    .item(getPath()).eq(value)
                    .build();
        }
    };

    public static RoleAnalysisAttributeDef locale = new RoleAnalysisAttributeDef(
            F_LOCALE,
            false,
            "locale",
            FocusType.class,
            RoleAnalysisAttributeDef.IdentifierType.FINAL) {
        @Override
        public ObjectQuery getQuery(String value) {
            return PrismContext.get().queryFor(FocusType.class)
                    .item(getPath()).eq(value)
                    .build();
        }
    };

    public static RoleAnalysisAttributeDef getLocality() {
        return locality;
    }

    public static RoleAnalysisAttributeDef locality = new RoleAnalysisAttributeDef(
            F_LOCALITY,
            false,
            "locality",
            FocusType.class,
            RoleAnalysisAttributeDef.IdentifierType.FINAL) {
        @Override
        public ObjectQuery getQuery(String value) {
            return PrismContext.get().queryFor(FocusType.class)
                    .item(getPath()).eq(value)
                    .build();
        }
    };

    public static RoleAnalysisAttributeDef costCenter = new RoleAnalysisAttributeDef(
            F_COST_CENTER,
            false,
            "costCenter",
            FocusType.class,
            RoleAnalysisAttributeDef.IdentifierType.FINAL) {
        @Override
        public ObjectQuery getQuery(String value) {
            return PrismContext.get().queryFor(FocusType.class)
                    .item(getPath()).eq(value)
                    .build();
        }
    };

    public static RoleAnalysisAttributeDef lifecycleState = new RoleAnalysisAttributeDef(
            F_LIFECYCLE_STATE,
            false,
            "lifecycleState",
            FocusType.class,
            RoleAnalysisAttributeDef.IdentifierType.FINAL) {
        @Override
        public ObjectQuery getQuery(String value) {
            return PrismContext.get().queryFor(FocusType.class)
                    .item(getPath()).eq(value)
                    .build();
        }
    };

    public static RoleAnalysisAttributeDef riskLevel = new RoleAnalysisAttributeDef(
            F_RISK_LEVEL,
            false,
            "riskLevel",
            RoleType.class,
            RoleAnalysisAttributeDef.IdentifierType.FINAL) {
        @Override
        public ObjectQuery getQuery(String value) {
            return PrismContext.get().queryFor(RoleType.class)
                    .item(getPath()).eq(value)
                    .build();
        }
    };

    public static RoleAnalysisAttributeDef getAttributeByDisplayValue(String displayValue) {
        Map<String, RoleAnalysisAttributeDef> attributeMap = createAttributeMap();
        return attributeMap.get(displayValue);
    }

    public static @NotNull @UnmodifiableView Map<String, RoleAnalysisAttributeDef> createAttributeMap() {
        Map<String, RoleAnalysisAttributeDef> attributeMap = new HashMap<>();
        attributeMap.put(orgAssignment.getDisplayValue(), orgAssignment);
        attributeMap.put(roleAssignment.getDisplayValue(), roleAssignment);
        attributeMap.put(serviceAssignment.getDisplayValue(), serviceAssignment);
        attributeMap.put(archetypeAssignment.getDisplayValue(), archetypeAssignment);
        attributeMap.put(resourceAssignment.getDisplayValue(), resourceAssignment);
        attributeMap.put(orgInducement.getDisplayValue(), orgInducement);
        attributeMap.put(roleInducement.getDisplayValue(), roleInducement);
        attributeMap.put(serviceInducement.getDisplayValue(), serviceInducement);
        attributeMap.put(archetypeInducement.getDisplayValue(), archetypeInducement);
        attributeMap.put(archetypeRef.getDisplayValue(), archetypeRef);
        attributeMap.put(title.getDisplayValue(), title);
        attributeMap.put(locale.getDisplayValue(), locale);
        attributeMap.put(locality.getDisplayValue(), locality);
        attributeMap.put(costCenter.getDisplayValue(), costCenter);
        attributeMap.put(lifecycleState.getDisplayValue(), lifecycleState);
        attributeMap.put(riskLevel.getDisplayValue(), riskLevel);

        loadRoleExtension().forEach(attribute -> attributeMap.put(attribute.getDisplayValue(), attribute));
        loadUserExtension().forEach(attribute -> attributeMap.put(attribute.getDisplayValue(), attribute));
        return Collections.unmodifiableMap(attributeMap);
    }

    public static @NotNull List<RoleAnalysisAttributeDef> getAttributesForRoleAnalysis() {

        List<RoleAnalysisAttributeDef> analysisAttributeDefs = new ArrayList<>(List.of(
                lifecycleState,
                locale,
                locality,
                costCenter,
                riskLevel,
                orgAssignment,
                resourceAssignment,
                roleAssignment,
                serviceAssignment,
                orgInducement,
                roleInducement,
                serviceInducement,
                archetypeRef
        ));
        analysisAttributeDefs.addAll(loadRoleExtension());

        return Collections.unmodifiableList(analysisAttributeDefs);
    }

    public static @NotNull List<RoleAnalysisAttributeDef> getAttributesForUserAnalysis() {
        List<RoleAnalysisAttributeDef> analysisAttributeDefs = new ArrayList<>(List.of(
                title,
                locale,
                locality,
                costCenter,
                lifecycleState,
//                archetypeAssignment,
                orgAssignment,
                resourceAssignment,
                roleAssignment,
                serviceAssignment,
                archetypeRef
        ));

        analysisAttributeDefs.addAll(loadUserExtension());

        return Collections.unmodifiableList(analysisAttributeDefs);
    }

    @NotNull
    private static RoleAnalysisAttributeDef getRoleAnalysisItemDefAssignment(
            @NotNull ItemName itemName,
            @NotNull String displayValue,
            @NotNull QName targetType) {
        return new RoleAnalysisAttributeDef(
                ItemPath.create(itemName, AssignmentType.F_TARGET_REF),
                true,
                displayValue,
                null,
                RoleAnalysisAttributeDef.IdentifierType.OID) {
            @Override
            public @NotNull Set<String> resolveMultiValueItem(
                    @NotNull PrismObject<?> prismObject,
                    @NotNull ItemPath itemPath) {
                return resolveAssignment(prismObject, itemPath, targetType);
            }

            @Override
            public Class<? extends ObjectType> getTargetClassType() {
                Class<? extends ObjectType> objectClass = null;
                if (targetType.equals(OrgType.COMPLEX_TYPE)) {
                    objectClass = OrgType.class;
                } else if (targetType.equals(RoleType.COMPLEX_TYPE)) {
                    objectClass = RoleType.class;
                } else if (targetType.equals(ServiceType.COMPLEX_TYPE)) {
                    objectClass = ServiceType.class;
                } else if (targetType.equals(ArchetypeType.COMPLEX_TYPE)) {
                    objectClass = ArchetypeType.class;
                } else if (targetType.equals(ResourceType.COMPLEX_TYPE)) {
                    objectClass = ResourceType.class;
                }
                return objectClass;
            }

            @Override
            public ObjectQuery getQuery(String value) {

                Class<? extends ObjectType> objectClass = null;
                if (targetType.equals(OrgType.COMPLEX_TYPE)) {
                    objectClass = OrgType.class;
                } else if (targetType.equals(RoleType.COMPLEX_TYPE)) {
                    objectClass = RoleType.class;
                } else if (targetType.equals(ServiceType.COMPLEX_TYPE)) {
                    objectClass = ServiceType.class;
                } else if (targetType.equals(ArchetypeType.COMPLEX_TYPE)) {
                    objectClass = ArchetypeType.class;
                } else if (targetType.equals(ResourceType.COMPLEX_TYPE)) {
                    objectClass = ResourceType.class;
                }

                return PrismContext.get().queryFor(objectClass)
                        .item(getPath()).ref(value)
                        .build();
            }
        };
    }

    @NotNull
    private static Set<String> resolveAssignment(
            @NotNull PrismObject<?> prismObject,
            @NotNull ItemPath itemPath,
            @NotNull QName targetType) {
        Set<String> resolvedValues = new HashSet<>();
        Collection<Item<?, ?>> allItems = prismObject.getAllItems(itemPath);
        for (Item<?, ?> item : allItems) {
            Object realValue = item.getRealValue();
            if (realValue instanceof ObjectReferenceType objectReference) {
                QName refTargetType = objectReference.getType();
                if (refTargetType.equals(targetType)) {
                    resolvedValues.add(objectReference.getOid());
                }
            }
        }

        return resolvedValues;
    }

    @NotNull
    private static Set<String> resolveRefs(
            @NotNull PrismObject<?> prismObject,
            @NotNull ItemPath itemPath) {
        Set<String> resolvedValues = new HashSet<>();
        Collection<Item<?, ?>> allItems = prismObject.getAllItems(itemPath);
        for (Item<?, ?> item : allItems) {
            Object realValue = item.getRealValue();
            if (realValue instanceof ObjectReferenceType objectReference) {
                resolvedValues.add(objectReference.getOid());
            }
        }

        return resolvedValues;
    }

    private static @NotNull List<RoleAnalysisAttributeDef> loadRoleExtension() {

        List<RoleAnalysisAttributeDef> attributes = new ArrayList<>();
        PrismContainerDefinition<?> itemDefinitionByFullPath;

        try {
            itemDefinitionByFullPath = PrismContext.get().getSchemaRegistry()
                    .findItemDefinitionByFullPath(UserType.class, PrismContainerDefinition.class, UserType.F_EXTENSION);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

        List<?> definitions = itemDefinitionByFullPath.getDefinitions();

        for (Object definition : definitions) {
            if (definition instanceof PrismPropertyDefinition<?> prismPropertyDefinition) {
                Class<?> typeClass = prismPropertyDefinition.getTypeClass();
                boolean isSingleValue = prismPropertyDefinition.isSingleValue();

                ItemPath itemName = prismPropertyDefinition.getItemName();
                String attributeName = itemName + " extension";
                RoleAnalysisAttributeDef attribute = createRoleAttribute(
                        prismPropertyDefinition, typeClass, isSingleValue, attributeName);
                if (attribute != null) {
                    attributes.add(attribute);
                }
            }
        }
        return attributes;
    }

    private static @NotNull List<RoleAnalysisAttributeDef> loadUserExtension() {
        List<RoleAnalysisAttributeDef> attributes = new ArrayList<>();
        PrismContainerDefinition<?> itemDefinitionByFullPath;

        try {
            itemDefinitionByFullPath = PrismContext.get().getSchemaRegistry()
                    .findItemDefinitionByFullPath(UserType.class, PrismContainerDefinition.class, UserType.F_EXTENSION);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }

        List<?> definitions = itemDefinitionByFullPath.getDefinitions();

        for (Object definition : definitions) {
            if (definition instanceof PrismPropertyDefinition<?> prismPropertyDefinition) {
                Class<?> typeClass = prismPropertyDefinition.getTypeClass();
                boolean isSingleValue = prismPropertyDefinition.isSingleValue();

                ItemPath itemName = prismPropertyDefinition.getItemName();
                String attributeName = itemName + " extension";
                RoleAnalysisAttributeDef attribute = createUserAttribute(
                        prismPropertyDefinition, typeClass, isSingleValue, attributeName);
                if (attribute != null) {
                    attributes.add(attribute);
                }
            }
        }

        return attributes;
    }

    private static @Nullable RoleAnalysisAttributeDef createUserAttribute(
            @NotNull PrismPropertyDefinition<?> prismPropertyDefinition,
            @NotNull Class<?> typeClass,
            boolean isSingleValue,
            @NotNull String attributeName) {
        if (typeClass.equals(Integer.class)
                || typeClass.equals(Long.class)
                || typeClass.equals(Boolean.class)
                || typeClass.equals(Double.class)
                || typeClass.equals(String.class)
                || typeClass.equals(PolyString.class)) {

            return new RoleAnalysisAttributeDef(
                    ItemPath.create(UserType.F_EXTENSION, prismPropertyDefinition.getItemName()),
                    isSingleValue,
                    attributeName,
                    UserType.class,
                    RoleAnalysisAttributeDef.IdentifierType.FINAL) {

                @Override
                public ObjectQuery getQuery(String value) {
                    return PrismContext.get().queryFor(UserType.class)
                            .item(getPath()).eq(value)
                            .build();
                }
            };
        } else if (typeClass.equals(ObjectReferenceType.class)) {
            return new RoleAnalysisAttributeDef(
                    ItemPath.create(UserType.F_EXTENSION, prismPropertyDefinition.getItemName()),
                    isSingleValue,
                    attributeName,
                    UserType.class,
                    RoleAnalysisAttributeDef.IdentifierType.OID) {
                @Override
                public ObjectQuery getQuery(String value) {
                    return PrismContext.get().queryFor(UserType.class)
                            .item(getPath()).ref(value)
                            .build();
                }

                @Override
                public String resolveSingleValueItem(@NotNull PrismObject<?> prismObject, @NotNull ItemPath itemPath) {
                    return resolveRef(prismObject, itemPath);
                }

                @Override
                public @NotNull Set<String> resolveMultiValueItem(@NotNull PrismObject<?> prismObject, @NotNull ItemPath itemPath) {
                    return resolveRefs(prismObject, itemPath);
                }
            };
        }
        return null;
    }

    private static @Nullable RoleAnalysisAttributeDef createRoleAttribute(
            @NotNull PrismPropertyDefinition<?> prismPropertyDefinition,
            @NotNull Class<?> typeClass,
            boolean isSingleValue,
            @NotNull String attributeName) {
        if (typeClass.equals(Integer.class)
                || typeClass.equals(Long.class)
                || typeClass.equals(Boolean.class)
                || typeClass.equals(Double.class)
                || typeClass.equals(String.class)
                || typeClass.equals(PolyString.class)) {

            return new RoleAnalysisAttributeDef(
                    ItemPath.create(RoleType.F_EXTENSION, prismPropertyDefinition.getItemName()),
                    isSingleValue,
                    attributeName,
                    RoleType.class,
                    RoleAnalysisAttributeDef.IdentifierType.FINAL) {
                @Override
                public ObjectQuery getQuery(String value) {
                    return PrismContext.get().queryFor(RoleType.class)
                            .item(getPath()).eq(value)
                            .build();
                }
            };
        } else if (typeClass.equals(ObjectReferenceType.class)) {
            return new RoleAnalysisAttributeDef(
                    ItemPath.create(RoleType.F_EXTENSION, prismPropertyDefinition.getItemName()),
                    isSingleValue,
                    attributeName,
                    RoleType.class,
                    RoleAnalysisAttributeDef.IdentifierType.OID) {
                @Override
                public ObjectQuery getQuery(String value) {
                    return PrismContext.get().queryFor(RoleType.class)
                            .item(getPath()).ref(value)
                            .build();
                }

                @Override
                public String resolveSingleValueItem(@NotNull PrismObject<?> prismObject, @NotNull ItemPath itemPath) {
                    return resolveRef(prismObject, itemPath);
                }

                @Override
                public @NotNull Set<String> resolveMultiValueItem(@NotNull PrismObject<?> prismObject, @NotNull ItemPath itemPath) {
                    return resolveRefs(prismObject, itemPath);
                }
            };
        }
        return null;
    }

    @Nullable
    private static String resolveRef(@NotNull PrismObject<?> prismObject, @NotNull ItemPath itemPath) {
        Item<PrismValue, ItemDefinition<?>> property = prismObject.findItem(itemPath);
        if (property != null) {
            Object object = property.getRealValue();
            if (object instanceof ObjectReferenceType objectReference) {
                return objectReference.getOid();
            }
            return extractRealValue(object);
        }
        return null;
    }
}
