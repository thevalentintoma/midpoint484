/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.*;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asPrismObject;
import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Methods that would belong to the {@link ShadowType} class but cannot go there because of JAXB.
 *
 * @author Radovan Semancik
 */
public class ShadowUtil {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowUtil.class);

    public static Collection<ResourceAttribute<?>> getPrimaryIdentifiers(ShadowType shadowType) {
        return getPrimaryIdentifiers(shadowType.asPrismObject());
    }

    public static Collection<ResourceAttribute<?>> getPrimaryIdentifiers(PrismObject<? extends ShadowType> shadow) {
        ResourceAttributeContainer attributesContainer = getAttributesContainer(shadow);
        if (attributesContainer == null) {
            return null;
        }
        return attributesContainer.getPrimaryIdentifiers();
    }

    public static Collection<ResourceAttribute<?>> getSecondaryIdentifiers(ShadowType shadowType) {
        return getSecondaryIdentifiers(shadowType.asPrismObject());
    }

    public static Collection<ResourceAttribute<?>> getSecondaryIdentifiers(PrismObject<? extends ShadowType> shadow) {
        ResourceAttributeContainer attributesContainer = getAttributesContainer(shadow);
        if (attributesContainer == null) {
            return null;
        }
        return attributesContainer.getSecondaryIdentifiers();
    }

    public static @NotNull Collection<ResourceAttribute<?>> getAllIdentifiers(PrismObject<? extends ShadowType> shadow) {
        ResourceAttributeContainer attributesContainer = getAttributesContainer(shadow);
        if (attributesContainer == null) {
            return List.of();
        }
        return attributesContainer.getAllIdentifiers();
    }

    public static @NotNull Collection<ResourceAttribute<?>> getAllIdentifiers(ShadowType shadow) {
        return getAllIdentifiers(shadow.asPrismObject());
    }

    public static ResourceAttribute<String> getNamingAttribute(ShadowType shadow){
        return getNamingAttribute(shadow.asPrismObject());
    }

    public static ResourceAttribute<String> getNamingAttribute(PrismObject<? extends ShadowType> shadow) {
        ResourceAttributeContainer attributesContainer = getAttributesContainer(shadow);
        if (attributesContainer == null) {
            return null;
        }
        return attributesContainer.getNamingAttribute();
    }

    public static @NotNull Collection<ResourceAttribute<?>> getAttributes(ShadowType shadowType) {
        return getAttributes(shadowType.asPrismObject());
    }

    public static @NotNull Collection<? extends Item<?, ?>> getAttributesTolerant(ShadowType shadowType) {
        var container = shadowType.asPrismObject().findContainer(ShadowType.F_ATTRIBUTES);
        return container != null ? container.getValue().getItems() : List.of();
    }

    public static @NotNull Collection<ResourceAttribute<?>> getAttributes(PrismObject<? extends ShadowType> shadow) {
        ResourceAttributeContainer attributesContainer = getAttributesContainer(shadow);
        return attributesContainer != null ? attributesContainer.getAttributes() : List.of();
    }

    /** Here we assume that the definition may not be applied yet. */
    public static @NotNull Collection<Item<?, ?>> getAttributesRaw(@NotNull ShadowType shadow) {
        PrismContainer<?> attributesContainer = shadow.asPrismObject().findContainer(ShadowType.F_ATTRIBUTES);
        if (attributesContainer != null && attributesContainer.hasAnyValue()) {
            return attributesContainer.getValue().getItems();
        } else {
            return List.of();
        }
    }

    public static <T> ResourceAttribute<T> getAttribute(ShadowType shadow, QName attrName) {
        return getAttributesContainer(shadow).findAttribute(attrName);
    }

    public static <T> ResourceAttribute<T> getAttribute(PrismObject<? extends ShadowType> shadow, QName attrName) {
        return getAttributesContainer(shadow).findAttribute(attrName);
    }

    public static ResourceAttributeContainer getAttributesContainer(ShadowType shadowType) {
        return getAttributesContainer(shadowType.asPrismObject());
    }

    public static @NotNull ResourceAttributeContainer getAttributesContainerRequired(ShadowType shadow) {
        return getAttributesContainerRequired(shadow.asPrismObject());
    }

    public static @NotNull ResourceAttributeContainer getAttributesContainerRequired(PrismObject<ShadowType> shadow) {
        return MiscUtil.stateNonNull(
                getAttributesContainer(shadow),
                "No attributes container in %s", shadow);
    }

    public static ResourceAttributeContainer getAttributesContainer(PrismObject<? extends ShadowType> shadow) {
        return castShadowContainer(shadow.getValue(), ShadowType.F_ATTRIBUTES, ResourceAttributeContainer.class);
    }

    /** Similar to {@link #getAttributesContainer(ShadowType)}. */
    public static ShadowAssociationsContainer getAssociationsContainer(@NotNull ShadowType shadow) {
        return getAssociationsContainer(shadow.asPrismObject());
    }

    // TODO what kind of exception?
    public static ShadowAssociationsContainer getAssociationsContainerRequired(@NotNull ShadowType shadow) {
        return MiscUtil.stateNonNull(
                getAssociationsContainer(shadow.asPrismObject()),
                "No associations container in %s", shadow);
    }

    /** Assuming the shadow has the correct definition. */
    public static ShadowAssociationsContainer getOrCreateAssociationsContainer(ShadowType shadow) {
        try {
            return (ShadowAssociationsContainer) shadow
                    .asPrismObject()
                    .<ShadowAssociationsType>findOrCreateContainer(ShadowType.F_ASSOCIATIONS);
        } catch (SchemaException e) {
            throw SystemException.unexpected(e);
        }
    }

    /** Similar to {@link #getAttributesContainer(ShadowType)}. */
    public static ShadowAssociationsContainer getAssociationsContainer(@NotNull PrismObject<ShadowType> shadow) {
        return castShadowContainer(
                shadow.asObjectable().asPrismContainerValue(), ShadowType.F_ASSOCIATIONS, ShadowAssociationsContainer.class);
    }

    private static <T> T castShadowContainer(
            @NotNull PrismContainerValue<?> parent, QName containerName, Class<T> expectedClass) {
        PrismContainer<?> container = parent.findContainer(containerName);
        if (container == null) {
            return null;
        } else if (expectedClass.isInstance(container)) {
            //noinspection unchecked
            return (T) container;
        } else {
            throw new IllegalStateException(
                    "Expected that <%s> will be %s but it is %s; in %s".formatted(
                            containerName, expectedClass.getSimpleName(), container.getClass(), parent));
        }
    }

    /** Assuming the shadow has the correct definition. */
    public static @NotNull ResourceAttributeContainer getOrCreateAttributesContainer(ShadowType shadow) {
        try {
            return (ResourceAttributeContainer) shadow
                    .asPrismObject()
                    .<ShadowAttributesType>findOrCreateContainer(ShadowType.F_ATTRIBUTES);
        } catch (SchemaException e) {
            throw SystemException.unexpected(e);
        }
    }

    public static @NotNull ResourceAttributeContainer getOrCreateAttributesContainer(PrismObject<ShadowType> shadow) {
        return getOrCreateAttributesContainer(shadow.asObjectable());
    }

    public static ResourceAttributeContainer getOrCreateAttributesContainer(
            ShadowType shadow, ResourceObjectDefinition definition) {
        return getOrCreateAttributesContainer(shadow.asPrismObject(), definition);
    }

    public static ResourceAttributeContainer getOrCreateAttributesContainer(
            PrismObject<? extends ShadowType> shadow, ResourceObjectDefinition definition) {
        ResourceAttributeContainer attributesContainer = getAttributesContainer(shadow);
        if (attributesContainer != null) {
            return attributesContainer;
        }
        ResourceAttributeContainer emptyContainer =
                ResourceAttributeContainer.createEmptyContainer(ShadowType.F_ATTRIBUTES, definition);
        try {
            shadow.add(emptyContainer);
        } catch (SchemaException e) {
            throw SystemException.unexpected(e);
        }
        return emptyContainer;
    }

    public static @NotNull ResourceObjectClassDefinition getObjectClassDefinition(@NotNull ShadowType shadow) {
        // TODO: maybe we can do something more intelligent here
        ResourceAttributeContainer attributesContainer = getAttributesContainer(shadow);
        return attributesContainer.getDefinition()
                .getComplexTypeDefinition()
                .getObjectClassDefinition();
    }

    /** The shadow must have the attributes container and a definition. */
    public static @NotNull ResourceObjectDefinition getResourceObjectDefinition(@NotNull ShadowType shadow) {
        ResourceAttributeContainer attributesContainer = MiscUtil.argNonNull(
                getAttributesContainer(shadow), "No attributes container in %s", shadow);
        return attributesContainer.getResourceObjectDefinitionRequired();
    }

    public static String getResourceOid(ShadowType shadowType) {
        return getResourceOid(shadowType.asPrismObject());
    }

    public static @NotNull String getResourceOidRequired(@NotNull ShadowType shadow) {
        return java.util.Objects.requireNonNull(
                getResourceOid(shadow),
                () -> "No resource OID in " + shadow);
    }

    public static String getResourceOid(PrismObject<ShadowType> shadow) {
        PrismReference resourceRef = shadow.findReference(ShadowType.F_RESOURCE_REF);
        if (resourceRef == null) {
            return null;
        }
        return resourceRef.getOid();
    }

    public static PolyString getResourceName(ShadowType shadowType) {
        return getResourceName(shadowType.asPrismObject());
    }

    public static PolyString getResourceName(PrismObject<ShadowType> shadow) {
        PrismReference resourceRef = shadow.findReference(ShadowType.F_RESOURCE_REF);
        if (resourceRef == null) {
            return null;
        }
        return resourceRef.getTargetName();
    }

    public static String getSingleStringAttributeValue(ShadowType shadow, QName attrName) {
        return getSingleStringAttributeValue(shadow.asPrismObject(), attrName);
    }

    private static String getSingleStringAttributeValue(PrismObject<ShadowType> shadow, QName attrName) {
        PrismContainer<?> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
        if (attributesContainer == null) {
            return null;
        }
        PrismProperty<?> attribute = attributesContainer.findProperty(ItemName.fromQName(attrName));
        if (attribute == null) {
            return null;
        }
        var realValue = attribute.getRealValue();
        // Slight hack, to deal with polystring/string values in repo
        if (realValue instanceof PolyString polyString) {
            return polyString.getOrig();
        } else {
            return (String) realValue;
        }
    }

    public static <T> @NotNull PrismPropertyValue<T> getSingleValueRequired(ShadowType shadow, QName attrName, Object errorCtx)
            throws SchemaException {
        PrismPropertyValue<T> value = getSingleValue(shadow, attrName, errorCtx);
        if (value != null) {
            return value;
        } else {
            throw new SchemaException(
                    "Attribute %s has no value%s".formatted(attrName, errorCtx));
        }
    }

    public static <T> @Nullable PrismPropertyValue<T> getSingleValue(ShadowType shadow, QName attrName, Object errorCtx)
            throws SchemaException {
        ResourceAttribute<T> attribute = getAttribute(shadow, attrName);
        if (attribute == null || attribute.isEmpty()) {
            return null;
        } else if (attribute.size() > 1) {
            throw new SchemaException(
                    "Attribute %s has more than one value%s".formatted(attrName, errorCtx));
        } else {
            return attribute.getValue();
        }
    }

    public static <T> List<T> getAttributeValues(ShadowType shadowType, QName attrName) {
        return getAttributeValues(shadowType.asPrismObject(), attrName);
    }

    public static <T> @NotNull List<T> getAttributeValues(PrismObject<? extends ShadowType> shadow, QName attrName) {
        PrismContainer<?> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
        if (attributesContainer == null || attributesContainer.isEmpty()) {
            return List.of();
        }
        PrismProperty<T> attr = attributesContainer.findProperty(ItemName.fromQName(attrName));
        if (attr == null) {
            return List.of();
        }
        List<T> realValues = new ArrayList<>();
        for (PrismPropertyValue<T> pval : attr.getValues()) {
            realValues.add(pval.getValue());
        }
        return realValues;
    }

    public static <T> T getAttributeValue(ShadowType shadowType, QName attrName) throws SchemaException {
        return getAttributeValue(shadowType.asPrismObject(), attrName);
    }

    public static <T> T getAttributeValue(PrismObject<? extends ShadowType> shadow, QName attrName) throws SchemaException {
        Collection<T> values = getAttributeValues(shadow, attrName);
        if (values.isEmpty()) {
            return null;
        }
        if (values.size() > 1) {
            throw new SchemaException("Attempt to get single value from multi-valued attribute "+attrName);
        }
        return values.iterator().next();
    }

    public static void setPassword(ShadowType shadow, ProtectedStringType passwordValue) {
        getOrCreateShadowPassword(shadow)
                .setValue(passwordValue);
    }

    public static void setPasswordIncomplete(ShadowType shadow) throws SchemaException {
        PasswordType password = getOrCreateShadowPassword(shadow);
        //noinspection unchecked
        PrismContainerValue<PasswordType> passwordContainer = password.asPrismContainerValue();
        PrismProperty<ProtectedStringType> valueProperty = passwordContainer.findOrCreateProperty(PasswordType.F_VALUE);
        valueProperty.setIncomplete(true);
    }

    public static @NotNull PasswordType getOrCreateShadowPassword(ShadowType shadow) {
        CredentialsType credentials = shadow.getCredentials();
        if (credentials == null) {
            credentials = new CredentialsType();
            shadow.setCredentials(credentials);
        }
        PasswordType password = credentials.getPassword();
        if (password == null) {
            password = new PasswordType();
            credentials.setPassword(password);
        }
        return password;
    }

    public static ActivationType getOrCreateActivation(ShadowType shadowType) {
        ActivationType activation = shadowType.getActivation();
        if (activation == null) {
            activation = new ActivationType();
            shadowType.setActivation(activation);
        }
        return activation;
    }

    /**
     * This is not supposed to be used in production code! It is just for the tests.
     */
    @VisibleForTesting
    public static void applyResourceSchema(
            PrismObject<? extends ShadowType> shadow,
            ResourceSchema resourceSchema) throws SchemaException {
        QName objectClass = shadow.asObjectable().getObjectClass();
        shadow
                .<ShadowAttributesType>findContainer(ShadowType.F_ATTRIBUTES)
                .applyDefinition(
                        resourceSchema
                                .findDefinitionForObjectClassRequired(objectClass)
                                .toResourceAttributeContainerDefinition());
    }

    public static PrismObjectDefinition<ShadowType> applyObjectDefinition(
            PrismObjectDefinition<ShadowType> shadowDefinition,
            ResourceObjectDefinition objectClassDefinition) throws SchemaException {
        // FIXME eliminate double cloning!
        return shadowDefinition.cloneWithNewDefinition(
                        ShadowType.F_ATTRIBUTES, objectClassDefinition.toResourceAttributeContainerDefinition())
                .cloneWithNewDefinition(
                        ShadowType.F_ASSOCIATIONS, objectClassDefinition.toShadowAssociationsContainerDefinition());
    }

    public static String getIntent(PrismObject<ShadowType> shadow) {
        return shadow != null ? getIntent(shadow.asObjectable()) : null;
    }

    /**
     * Returns intent from the shadow. Backwards compatible with older accountType. May also adjust for default
     * intent if necessary.
     */
    @Contract("null -> null")
    public static String getIntent(ShadowType shadow) {
        if (shadow == null) {
            return null;
        }
        return shadow.getIntent();
    }

    public static ShadowKindType getKind(PrismObject<ShadowType> shadow) {
        return shadow != null ? getKind(shadow.asObjectable()) : null;
    }

    @Contract("!null -> !null; null -> null")
    public static ShadowKindType getKind(ShadowType shadow) {
        if (shadow == null) {
            return null;
        }
        return ObjectUtils.defaultIfNull(shadow.getKind(), ShadowKindType.ACCOUNT);
    }

    public static <T> Collection<T> getAttributeValues(ShadowType shadow, QName attributeQname, Class<T> type) {
        ResourceAttributeContainer attributesContainer = getAttributesContainer(shadow);
        if (attributesContainer == null) {
            return null;
        }
        ResourceAttribute<T> attribute = attributesContainer.findAttribute(attributeQname);
        if (attribute == null) {
            return null;
        }
        return attribute.getRealValues(type);
    }

    public static ItemName getAttributeName(ItemPath attributePath, String message) throws SchemaException {
        if (attributePath == null || attributePath.isEmpty()) {
            return null;
        }
        Object firstPathSegment = attributePath.first();
        if (!ItemPath.isName(firstPathSegment)) {
            throw new SchemaException(message + ": first path segment is not a name segment");
        }
        ItemName firstName = ItemPath.toName(firstPathSegment);
        if (!QNameUtil.match(ShadowType.F_ATTRIBUTES, firstName)) {
            throw new SchemaException(message + ": first path segment is not "+ShadowType.F_ATTRIBUTES);
        }
        if (attributePath.isEmpty()) {
            throw new SchemaException(message + ": path too short ("+attributePath.size()+" segments)");
        }
        if (attributePath.size() > 2) {
            throw new SchemaException(message + ": path too long ("+attributePath.size()+" segments)");
        }
        Object secondPathSegment = attributePath.rest().first();
        if (!ItemPath.isName(secondPathSegment)) {
            throw new SchemaException(message + ": second path segment is not a name segment");
        }
        return ItemPath.toName(secondPathSegment);
    }

    public static void checkConsistence(PrismObject<? extends ShadowType> shadow, String desc) {
        PrismReference resourceRef = shadow.findReference(ShadowType.F_RESOURCE_REF);
        if (resourceRef == null) {
            throw new IllegalStateException("No resourceRef in "+shadow+" in "+desc);
        }
        if (StringUtils.isBlank(resourceRef.getOid())) {
            throw new IllegalStateException("Null or empty OID in resourceRef in "+desc);
        }
        ShadowType shadowType = shadow.asObjectable();
        if (shadowType.getObjectClass() == null) {
            throw new IllegalStateException("Null objectClass in "+desc);
        }
        PrismContainer<ShadowAttributesType> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
        if (attributesContainer != null) {
            if (!(attributesContainer instanceof ResourceAttributeContainer)) {
                throw new IllegalStateException("The attributes element expected to be ResourceAttributeContainer but it is "
                        +attributesContainer.getClass()+" instead in "+desc);
            }
            checkConsistency(attributesContainer.getDefinition(), " container definition in "+desc);
        }

        PrismContainerDefinition<ShadowAttributesType> attributesDefinition =
                shadow.getDefinition().findContainerDefinition(ShadowType.F_ATTRIBUTES);
        checkConsistency(attributesDefinition, " object definition in "+desc);
    }

    public static void checkConsistency(PrismContainerDefinition<ShadowAttributesType> attributesDefinition, String desc) {
        if (attributesDefinition == null) {
            throw new IllegalStateException("No definition for <attributes> in "+desc);
        }
        if (!(attributesDefinition instanceof ResourceAttributeContainerDefinition)) {
            throw new IllegalStateException("The attributes element definition expected to be ResourceAttributeContainerDefinition but it is "
                    +attributesDefinition.getClass()+" instead in "+desc);
        }
    }

    public static boolean isProtected(@Nullable ShadowType shadow) {
        return shadow != null && Boolean.TRUE.equals(shadow.isProtectedObject());
    }

    public static boolean isProtected(PrismObject<? extends ShadowType> shadow) {
        return isProtected(
                PrismObject.asObjectable(shadow));
    }

    public static boolean isDead(ShadowType shadow) {
        return Boolean.TRUE.equals(shadow.isDead());
    }

    public static boolean isDead(@NotNull PrismObject<ShadowType> shadow) {
        return isDead(shadow.asObjectable());
    }

    public static boolean isNotDead(PrismObject<ShadowType> shadow) {
        return !isDead(shadow);
    }

    public static boolean isNotDead(ShadowType shadow) {
        return !isDead(shadow);
    }

    public static boolean isDead(ObjectReferenceType projectionRef) {
        return !SchemaService.get().relationRegistry().isMember(projectionRef.getRelation());
    }

    public static boolean isNotDead(ObjectReferenceType projectionRef) {
        return !isDead(projectionRef);
    }

    public static boolean wasSynchronizedAfterDeath(ShadowType shadow) {
        XMLGregorianCalendar deathTimestamp = shadow.getDeathTimestamp();
        if (deathTimestamp == null) {
            LOGGER.trace("Dead shadow without death timestamp: {}", shadow);
            return false;
        }
        XMLGregorianCalendar fullSynchronizationTimestamp = shadow.getFullSynchronizationTimestamp();
        return fullSynchronizationTimestamp != null &&
                fullSynchronizationTimestamp.compare(deathTimestamp) == DatatypeConstants.GREATER;
    }

    public static boolean isExists(ShadowType shadow) {
        return !Boolean.FALSE.equals(shadow.isExists());
    }

    public static boolean isExists(PrismObject<ShadowType> shadow) {
        return isExists(shadow.asObjectable());
    }

    /** Null values mean "any" here. */
    public static boolean matches(
            @NotNull ShadowType shadow,
            @Nullable String resourceOid,
            @Nullable ShadowKindType kind,
            @Nullable String intent) {
        if (resourceOid != null && !shadowHasResourceOid(shadow, resourceOid)) {
            return false;
        }
        if (kind != null && kind != shadow.getKind()) {
            return false;
        }
        return intent == null || intent.equals(shadow.getIntent());
    }

    @SuppressWarnings("RedundantIfStatement")
    public static boolean matches(@NotNull ShadowType shadow, @NotNull ShadowDiscriminatorType discriminator) {
        String expectedResourceOid = Referencable.getOid(discriminator.getResourceRef());
        if (expectedResourceOid != null && !expectedResourceOid.equals(getResourceOid(shadow))) {
            return false;
        }

        ShadowKindType expectedKind = discriminator.getKind();
        if (expectedKind != null && shadow.getKind() != expectedKind) {
            return false;
        }

        String expectedIntent = discriminator.getIntent();
        if (expectedIntent != null && !expectedIntent.equals(shadow.getIntent())) {
            return false;
        }

        String expectedTag = discriminator.getTag();
        if (expectedTag != null && !expectedTag.equals(shadow.getTag())) {
            return false;
        }

        QName expectedObjectClassName = discriminator.getObjectClassName();
        if (expectedObjectClassName != null && QNameUtil.match(expectedObjectClassName, shadow.getObjectClass())) {
            return false;
        }

        // ignoring tombstone and discriminatorOrder
        return true;
    }

    /**
     * Returns true if the shadow has non-null resource OID that matches given value.
     *
     * Neither one of the OIDs (in shadow nor in discriminator) should be null.
     * So we intentionally return false even if they are both null (and therefore equal).
     *
     * In reality such situations sometimes occur (see MID-6673). Maybe we should find
     * and fix the primary cause and then strictly require non-nullity of these OIDs here.
     * But not now.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean shadowHasResourceOid(ShadowType shadow, String oid) {
        String shadowResourceOid = shadow.getResourceRef() != null ? shadow.getResourceRef().getOid() : null;
        return shadowResourceOid != null && shadowResourceOid.equals(oid);
    }

    /**
     * Interprets {@link ShadowDiscriminatorType} as a pattern. E.g. null discriminator kind is
     * interpreted to match any shadow kind.
     */
    public static boolean matchesPattern(ShadowType shadowType, ShadowDiscriminatorType discr) {
        if (shadowType == null) {
            return false;
        }
        if (discr == null || discr.getResourceRef() == null) {
            return false; // shouldn't occur
        }
        if (!shadowHasResourceOid(shadowType, discr.getResourceRef().getOid())) {
            return false;
        }
        if (discr.getKind() != null && !MiscUtil.equals(discr.getKind(), shadowType.getKind())) {
            return false;
        }
        if (discr.getIntent() == null) {
            return true;
        }
        return equalsIntent(shadowType.getIntent(), discr.getIntent()); // TODO ok?
    }

    // FIXME what if a == b == null ? The method should (most probably) return true in such case.
    private static boolean equalsIntent(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }

    public static boolean isConflicting(ShadowType shadow1, ShadowType shadow2) {
        if (!shadow1.getResourceRef().getOid().equals(shadow2.getResourceRef().getOid())) {
            return false;
        }
        if (!MiscUtil.equals(getKind(shadow1), getKind(shadow2))) {
            return false;
        }
        return equalsIntent(shadow1.getIntent(), shadow2.getIntent()); // TODO ok?
    }

    public static Object getHumanReadableNameLazily(PrismObject<? extends ShadowType> shadow) {
        return DebugUtil.lazy(() -> getHumanReadableName(shadow));
    }

    public static String getHumanReadableName(PrismObject<? extends ShadowType> shadow) {
        if (shadow == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        ShadowType shadowType = shadow.asObjectable();
        ShadowKindType kind = shadowType.getKind();
        if (kind != null) {
            sb.append(kind).append(" ");
        }
        sb.append("shadow ");
        boolean first = true;
        for (ResourceAttribute<?> iattr : emptyIfNull(getPrimaryIdentifiers(shadow))) {
            if (first) {
                sb.append("[");
                first  = false;
            } else {
                sb.append(",");
            }
            sb.append(iattr.getElementName().getLocalPart());
            sb.append("=");
            sb.append(iattr.getRealValue());
        }
        if (first) {
            sb.append("[");
        }
        sb.append("]");
        return sb.toString();
    }

    public static String getHumanReadableName(ShadowType shadowType) {
        if (shadowType == null) {
            return "null";
        }
        return getHumanReadableName(shadowType.asPrismObject());
    }

    public static boolean isFullShadow(ShadowType shadow) {
        CachingMetadataType cachingMetadata = shadow.getCachingMetadata();
        return cachingMetadata != null && cachingMetadata.getRetrievalTimestamp() != null;
    }

    public static PolyString determineShadowName(PrismObject<ShadowType> shadow) throws SchemaException {
        return determineShadowName(asObjectable(shadow));
    }

    public static PolyString determineShadowName(ShadowType shadow) throws SchemaException {
        String stringName = determineShadowStringName(shadow);
        return stringName != null ? PolyString.fromOrig(stringName) : null;
    }

    public static PolyStringType determineShadowNameRequired(AbstractShadow shadow) throws SchemaException {
        return PolyStringType.fromOrig(
                MiscUtil.requireNonNull(
                        determineShadowStringName(shadow.getBean()),
                        () -> "Name could not be determined for " + shadow));
    }

    /** This method is intentionally very lax concerning the shadow. (At least for now.) */
    private static String determineShadowStringName(ShadowType shadow) throws SchemaException {
        ResourceAttributeContainer attributesContainer = getAttributesContainer(shadow);
        if (attributesContainer == null) {
            return null;
        }
        ResourceAttribute<String> namingAttribute = attributesContainer.getNamingAttribute();
        if (namingAttribute == null || namingAttribute.isEmpty()) {
            // No naming attribute defined. Try to fall back to identifiers.
            Collection<ResourceAttribute<?>> identifiers = attributesContainer.getPrimaryIdentifiers();
            if (identifiers.size() == 1) { // There should be at most one primary identifier
                PrismProperty<?> identifier = identifiers.iterator().next();
                // Only single-valued identifiers
                Collection<? extends PrismPropertyValue<?>> values = identifier.getValues();
                if (values.size() == 1) {
                    PrismPropertyValue<?> value = values.iterator().next();
                    // and only strings
                    if (value.getValue() instanceof String string) {
                        return string;
                    } else if (value.getValue() instanceof PolyString polyString) {
                        return polyString.getOrig();
                    }
                }
            } else {
                // We could also try secondary identifiers...
                ResourceAttribute<String> nameAttribute = attributesContainer.findAttribute(SchemaConstants.ICFS_NAME);
                if (nameAttribute == null) { // this is suspicious
                    throw new SchemaException("Could not determine shadow name.");
                }
                return nameAttribute.getValue(String.class).getValue();
            }
            // Identifier is not usable as name
            // TODO: better identification of a problem
            throw new SchemaException("No naming attribute defined (and identifier not usable)");
        }
        // TODO: Error handling

        var values = namingAttribute.getOrigValues();
        if (values.size() > 1) {
            throw new SchemaException(
                    "Cannot determine name of shadow. Found more than one value for naming attribute (attr: "
                            + namingAttribute.getElementName() + ", values: " + namingAttribute.getValues() + ")");
        } else if (values.isEmpty()) {
            throw new SchemaException("Naming attribute has no value. Could not determine shadow name.");
        } else {
            return values.iterator().next().toString();
        }
    }

    public static boolean matchesAttribute(ItemPath path, QName attributeName) {
        return path.startsWithName(ShadowType.F_ATTRIBUTES)
                && path.rest().startsWithName(attributeName);
    }

    /** The shadow must have definitions applied. */
    public static @Nullable ResourceObjectIdentifier.Primary<?> getPrimaryIdentifier(@NotNull ShadowType shadow)
            throws SchemaException {
        return ResourceObjectIdentifiers.optionalOf(shadow)
                .map(ResourceObjectIdentifiers::getPrimaryIdentifier)
                .orElse(null);
    }

    public static @Nullable Object getPrimaryIdentifierValue(
            @NotNull ShadowType shadow, @NotNull ResourceObjectDefinition objectDefinition) throws SchemaException {
        var identifiersOptional = ResourceObjectIdentifiers.optionalOf(objectDefinition, shadow);
        if (identifiersOptional.isEmpty()) {
            return null;
        }
        var identifiers = identifiersOptional.get();
        if (!(identifiers instanceof ResourceObjectIdentifiers.WithPrimary withPrimary)) {
            return null;
        }
        return withPrimary.getPrimaryIdentifierRequired().getOrigValue();
    }

    // TODO: may be useful to move to ResourceObjectClassDefinition later?
    public static void validateAttributeSchema(ShadowType shadow, ResourceObjectDefinition objectDefinition)
            throws SchemaException {
        ResourceAttributeContainer attributesContainer = getAttributesContainer(shadow);
        for (ResourceAttribute<?> attribute: attributesContainer.getAttributes()) {
            validateAttribute(attribute, objectDefinition);
        }
    }

    // TODO: may be useful to move to ResourceAttributeDefinition later?
    private static <T> void validateAttribute(ResourceAttribute<T> attribute,
            ResourceObjectDefinition objectDefinition) throws SchemaException {
        QName attrName = attribute.getElementName();
        ResourceAttributeDefinition<?> attrDef = objectDefinition.findAttributeDefinition(attrName);
        if (attrDef == null) {
            throw new SchemaException("No definition for attribute "+attrName+" in object class "+objectDefinition);
        }
        List<PrismPropertyValue<T>> pvals = attribute.getValues();
        if (pvals.isEmpty()) {
            if (attrDef.isMandatory()) {
                throw new SchemaException("Mandatory attribute "+attrName+" has no value");
            } else {
                return;
            }
        }
        if (pvals.size() > 1 && attrDef.isSingleValue()) {
            throw new SchemaException("Single-value attribute "+attrName+" has "+pvals.size()+" values");
        }
        Class<?> expectedClass = attrDef.getTypeClass();
        for (PrismPropertyValue<T> pval: pvals) {
            T val = pval.getValue();
            if (val == null) {
                throw new SchemaException("Null value in attribute "+attrName);
            }
            if (!XmlTypeConverter.isMatchingType(expectedClass, val.getClass())) {
                throw new SchemaException("Wrong value in attribute "+attrName+"; expected class "+attrDef.getTypeClass().getSimpleName()+", but was "+val.getClass());
            }
        }
    }

    // TODO consider removal
    public static ProtectedStringType getPasswordValue(ShadowType shadowType) {
        if (shadowType == null) {
            return null;
        }
        CredentialsType creds = shadowType.getCredentials();
        if (creds == null) {
            return null;
        }
        PasswordType passwd = creds.getPassword();
        if (passwd == null) {
            return null;
        }
        return passwd.getValue();
    }

    public static Object shortDumpShadowLazily(PrismObject<ShadowType> shadow) {
        return DebugUtil.lazy(() -> shortDumpShadow(shadow));
    }

    public static Object shortDumpShadowLazily(ShadowType shadow) {
        return DebugUtil.lazy(() -> shortDumpShadow(shadow));
    }

    public static String shortDumpShadow(ShadowType shadow) {
        return shortDumpShadow(asPrismObject(shadow));
    }

    public static String shortDumpShadow(PrismObject<ShadowType> shadow) {
        if (shadow == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("shadow:");
        sb.append(shadow.getOid()).append("(");
        PolyString name = shadow.getName();
        if (name != null) {
            sb.append(name);
        } else {
            Collection<ResourceAttribute<?>> primaryIdentifiers = getPrimaryIdentifiers(shadow);
            if (primaryIdentifiers != null && !primaryIdentifiers.isEmpty()) {
                shortDumpShadowIdentifiers(sb, primaryIdentifiers);
            } else {
                Collection<ResourceAttribute<?>> secondaryIdentifiers = getSecondaryIdentifiers(shadow);
                if (secondaryIdentifiers != null && !secondaryIdentifiers.isEmpty()) {
                    shortDumpShadowIdentifiers(sb, secondaryIdentifiers);
                }
            }
        }
        ShadowType shadowBean = shadow.asObjectable();
        if (isDead(shadowBean)) {
            sb.append(";DEAD");
        }
        if (!isExists(shadowBean)) {
            sb.append(";NOTEXISTS");
        }
        sb.append(")");
        return sb.toString();
    }

    private static void shortDumpShadowIdentifiers(StringBuilder sb, Collection<ResourceAttribute<?>> identifiers) {
        Iterator<ResourceAttribute<?>> iterator = identifiers.iterator();
        while (iterator.hasNext()) {
            ResourceAttribute<?> identifier = iterator.next();
            sb.append(identifier.getElementName().getLocalPart());
            sb.append("=");
            sb.append(identifier.getRealValue());
            if (iterator.hasNext()) {
                sb.append(";");
            }
        }
    }

    public static boolean isKnown(ShadowKindType kind) {
        return kind != null && kind != ShadowKindType.UNKNOWN;
    }

    public static boolean isNotKnown(ShadowKindType kind) {
        return !isKnown(kind);
    }

    public static boolean isKnown(String intent) {
        return intent != null && !SchemaConstants.INTENT_UNKNOWN.equals(intent);
    }

    public static boolean isNotKnown(String intent) {
        return !isKnown(intent);
    }

    public static boolean isClassified(ShadowType shadow) {
        return isClassified(shadow.getKind(), shadow.getIntent());
    }

    public static boolean isClassified(ShadowKindType kind, String intent) {
        return isKnown(kind) && isKnown(intent);
    }

    @Contract("null -> null")
    public static ResourceObjectTypeIdentification getTypeIdentification(ShadowType shadow) {
        if (shadow != null) {
            return ResourceObjectTypeIdentification.createIfKnown(shadow);
        } else {
            return null;
        }
    }

    /**
     * Returns true if the shadow state indicates that it is 'gone', i.e. no longer on the resource.
     * This could be determined from the `dead` property or from the `shadowLifecycleState`. The latter is
     * more precise for shadows that have been fetched with the future point-in-time.
     *
     * PRECONDITION: shadow lifecycle state must be set up.
     */
    public static boolean isGone(@NotNull ShadowType shadow) {
        ShadowLifecycleStateType state = shadow.getShadowLifecycleState();
        stateCheck(state != null, "Unknown lifecycle state of %s", shadow);
        return state == ShadowLifecycleStateType.CORPSE || state == ShadowLifecycleStateType.TOMBSTONE;
    }

    /** As {@link #isGone(ShadowType)} but with possibly incomplete information. */
    public static boolean isGoneApproximate(@NotNull ShadowType shadow) {
        return shadow.getShadowLifecycleState() != null ?
                ShadowUtil.isGone(shadow) :
                ShadowUtil.isDead(shadow); // an approximation
    }

    public static ShadowCorrelationStateType getCorrelationStateRequired(@NotNull ShadowType shadow) {
        return MiscUtil.requireNonNull(
                shadow.getCorrelation(),
                () -> new IllegalStateException("No correlation state in shadow " + shadow));

    }
    public static <T extends AbstractCorrelatorStateType> T getCorrelatorStateRequired(@NotNull ShadowType shadow, Class<T> clazz)
            throws SchemaException {
        return MiscUtil.requireNonNull(
                MiscUtil.castSafely(getCorrelationStateRequired(shadow).getCorrelatorState(), clazz),
                () -> new IllegalStateException("No correlation state in shadow " + shadow));
    }

    public static void setCorrelatorState(@NotNull ShadowType shadow, @Nullable AbstractCorrelatorStateType state) {
        if (shadow.getCorrelation() == null) {
            if (state == null) {
                return;
            } else {
                shadow.setCorrelation(
                        new ShadowCorrelationStateType());
            }
        }

        shadow.getCorrelation().setCorrelatorState(state);
    }

    public static @NotNull QName getObjectClassRequired(@NotNull ShadowType shadow) throws SchemaException {
        return MiscUtil.requireNonNull(
                shadow.getObjectClass(),
                () -> "No object class in " + shadow);
    }

    public static @NotNull ShadowKindType resolveDefault(ShadowKindType kind) {
        return Objects.requireNonNullElse(kind, ShadowKindType.ACCOUNT);
    }

    public static @NotNull String resolveDefault(String intent) {
        return Objects.requireNonNullElse(intent, SchemaConstants.INTENT_DEFAULT);
    }

    public static boolean hasResourceModifications(
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications) {
        return modifications.stream()
                .anyMatch(ShadowUtil::isResourceModification);
    }

    public static boolean hasAttributeModifications(
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications) {
        return modifications.stream()
                .anyMatch(delta -> isAttributeModification(delta.getPath().firstName()));
    }

    public static @NotNull List<ItemDelta<?, ?>> getResourceModifications(
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications) {
        return modifications.stream()
                .filter(ShadowUtil::isResourceModification)
                .collect(Collectors.toList());
    }

    public static boolean isResourceModification(ItemDelta<?, ?> modification) {
        QName firstPathName = modification.getPath().firstName();
        return isAttributeModification(firstPathName) || isNonAttributeResourceModification(firstPathName);
    }

    public static boolean isAttributeModification(QName firstPathName) {
        return QNameUtil.match(firstPathName, ShadowType.F_ATTRIBUTES);
    }

    public static boolean isNonAttributeResourceModification(QName firstPathName) {
        return QNameUtil.match(firstPathName, ShadowType.F_ACTIVATION)
                || QNameUtil.match(firstPathName, ShadowType.F_CREDENTIALS)
                || QNameUtil.match(firstPathName, ShadowType.F_ASSOCIATIONS)
                || QNameUtil.match(firstPathName, ShadowType.F_AUXILIARY_OBJECT_CLASS);
    }

    public static @Nullable SynchronizationSituationDescriptionType getLastSyncSituationDescription(@NotNull ShadowType shadow) {
        return shadow.getSynchronizationSituationDescription().stream()
                .max(Comparator.comparing(desc -> XmlTypeConverter.toMillis(desc.getTimestamp())))
                .orElse(null);
    }

    public static List<PendingOperationType> sortPendingOperations(List<PendingOperationType> pendingOperations) {
        // Copy to mutable list that is not bound to the prism
        List<PendingOperationType> sortedList = new ArrayList<>(pendingOperations.size());
        sortedList.addAll(pendingOperations);
        sortedList.sort((o1, o2) -> XmlTypeConverter.compare(o1.getRequestTimestamp(), o2.getRequestTimestamp()));
        return sortedList;
    }

    /** Creates the resource attributes container with a proper definition. */
    @VisibleForTesting
    public static @NotNull ResourceAttributeContainer setupAttributesContainer(
            @NotNull ShadowType shadowBean, @NotNull ResourceObjectDefinition objectDefinition) throws SchemaException {
        PrismObjectDefinition<ShadowType> standardShadowDef =
                PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
        var updatedShadowDef = applyObjectDefinition(standardShadowDef, objectDefinition);
        shadowBean.asPrismObject().applyDefinition(updatedShadowDef);
        return getOrCreateAttributesContainer(shadowBean, objectDefinition);
    }

    /**
     * Returns the values of given association. The values are connected to the shadow. The list itself is not.
     *
     * Does not assume that shadow has a definition.
     */
    public static @NotNull Collection<ShadowAssociationValueType> getAssociationValues(
            @NotNull PrismObject<ShadowType> shadow, QName assocName) {
        PrismContainer<ShadowAssociationValueType> association =
                shadow.findContainer(ItemPath.create(ShadowType.F_ASSOCIATIONS, assocName));
        if (association == null) {
            return List.of();
        } else {
            return association.getRealValues();
        }
    }

    /**
     * TODO better name ... the idea is that the shadow has the correct definition, but currently we cannot use
     *  AbstractShadow, because there are some differences in definitions ... to be researched.
     *
     * It seems that the wrong definition came from primary delta fed to the model.
     * Unlike in provisioning, we don't apply definitions to such deltas thoroughly.
     *
     * TEMPORARY
     */
    public static @NotNull Collection<? extends ShadowAssociationValue> getAdoptedAssociationValues(
            @NotNull PrismObject<ShadowType> shadow, QName assocName) {
        var association = ShadowUtil.getAssociation(shadow, assocName);
        if (association == null) {
            return List.of();
        } else {
            return association.getAssociationValues();
        }
    }

    /**
     * Similar to {@link #getAssociationsContainer(ShadowType)} but has the advantage of never returning `null`.
     * The collection itself is immutable. The values, however, are connected to ones in the shadow, so can be changed
     * if needed.
     */
    public static @NotNull Collection<ShadowAssociation> getAssociations(@NotNull ShadowType shadow) {
        var container = getAssociationsContainer(shadow);
        if (container != null) {
            return List.copyOf(container.getAssociations());
        } else {
            return List.of();
        }
    }

    public static @NotNull Collection<ShadowAssociation> getAssociations(@NotNull PrismObject<ShadowType> shadow) {
        return getAssociations(shadow.asObjectable());
    }

    /** Adds primary identifier value to the shadow. Assumes there's no primary identifier yet. */
    public static void addPrimaryIdentifierValue(ShadowType shadow, Object primaryIdentifierValue) throws SchemaException {
        var attributes = getOrCreateAttributesContainer(shadow);
        attributes.add(
                attributes
                        .getResourceObjectDefinitionRequired()
                        .getPrimaryIdentifierRequired()
                        .instantiateFromRealValue(primaryIdentifierValue));
    }

    public static ShadowAssociation getAssociation(PrismObject<ShadowType> shadow, QName associationName) {
        var container = getAssociationsContainer(shadow);
        return container != null ? container.findAssociation(associationName) : null;
    }

    public static void addAttribute(ShadowType shadow, ResourceAttribute<?> attribute) throws SchemaException {
        getOrCreateAttributesContainer(shadow).add(attribute);
    }

    public static void addAssociation(ShadowType shadow, ShadowAssociation association) throws SchemaException {
        getOrCreateAssociationsContainer(shadow).add(association);
    }

    public static void addShadowItem(ShadowType shadow, ShadowItem<?, ?> item) throws SchemaException {
        if (item instanceof ResourceAttribute<?> attribute) {
            addAttribute(shadow, attribute);
        } else if (item instanceof ShadowAssociation association) {
            addAssociation(shadow, association);
        } else {
            throw new IllegalArgumentException("Neither attribute nor association: " + item);
        }
    }

    public static @NotNull ShadowAssociationsCollection getAssociationsCollection(@NotNull ShadowType shadowBean) {
        return ShadowAssociationsCollection.ofShadow(shadowBean);
    }
}
