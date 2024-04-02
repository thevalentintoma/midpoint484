/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.schema.config.ConfigurationItem.*;
import static com.evolveum.midpoint.schema.config.ConfigurationItemOrigin.inResourceOrAncestor;
import static com.evolveum.midpoint.schema.util.ResourceObjectTypeDefinitionTypeUtil.SuperReference;
import static com.evolveum.midpoint.util.DebugUtil.lazy;
import static com.evolveum.midpoint.util.MiscUtil.configCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.config.*;
import com.evolveum.midpoint.schema.merger.objdef.ResourceObjectTypeDefinitionMergeOperation;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.AssociationsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * Creates refined class and object type definitions in {@link ResourceSchemaImpl} objects.
 *
 * These definitions are derived from:
 *
 * 1. native object class definitions ({@link NativeObjectClassDefinition}) (obtained dynamically or statically),
 * 2. configured {@link SchemaHandlingType} beans in resource definition.
 *
 * This class is instantiated for each parsing operation.
 *
 * TODO migrate all uses of {@link SchemaException} in the configuration beans to {@link ConfigurationException} ones
 *  The schema exception should be thrown only if there is a genuine error in the underlying native schema
 */
class ResourceSchemaParser {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceSchemaParser.class);

    /** Resource whose refined schema is being parsed. */
    @NotNull private final ResourceType resource;

    /** The `schemaHandling` from {@link #resource}, empty if missing. */
    @NotNull private final SchemaHandlingConfigItem schemaHandling;

    /** The extract of the resource information, to be weaved into type/class definitions. */
    @NotNull private final BasicResourceInformation basicResourceInformation;

    /** Raw resource schema we start with. */
    @NotNull private final NativeResourceSchema nativeSchema;

    /** TEMPORARY! */
    @NotNull private final Set<QName> ignoredAttributes;

    /** This is used when parsing "new" association types. */
    @Nullable private final AssociationsCapabilityConfigItem associationsCapabilityCI;

    @NotNull private final String contextDescription;

    /** The schema being created. */
    @NotNull private final ResourceSchemaImpl resourceSchema;

    /**
     * Class definition configuration items. We don't want to put these (instead of beans) into
     * {@link ResourceObjectClassDefinition} objects, as they are too fat. However, we need them
     * when parsing specific features of object classes. The value may be `null` if there's no config item.
     *
     * Indexed by class name local part (as the names are in `ri` namespace).
     */
    @NotNull private final Map<String, ResourceObjectClassDefinitionConfigItem> classDefinitionConfigItemMap = new HashMap<>();

    /**
     * As {@link #classDefinitionConfigItemMap} but for object types (regular and associated).
     * Indexed by type identification, e.g., `account/default`.
     */
    @NotNull private final Map<ResourceObjectTypeIdentification, AbstractResourceObjectTypeDefinitionConfigItem<?>> typeDefinitionConfigItemMap = new HashMap<>();

    private ResourceSchemaParser(
            @NotNull ResourceType resource,
            @NotNull SchemaHandlingConfigItem schemaHandling,
            @NotNull NativeResourceSchema nativeSchema,
            @Nullable AssociationsCapabilityConfigItem associationsCapabilityCI,
            @NotNull String contextDescription,
            @NotNull ResourceSchemaImpl resourceSchema) {
        this.resource = resource;
        this.schemaHandling = schemaHandling;
        this.basicResourceInformation = BasicResourceInformation.of(resource);
        this.nativeSchema = nativeSchema;
        this.ignoredAttributes = new ResourceSchemaAdjuster(resource, nativeSchema).getIgnoredAttributes();
        this.associationsCapabilityCI = associationsCapabilityCI;
        this.contextDescription = contextDescription;
        this.resourceSchema = resourceSchema;
    }

    static CompleteResourceSchema parseComplete(@NotNull ResourceType resource, @NotNull NativeResourceSchema nativeSchema)
            throws SchemaException, ConfigurationException {
        var schemaHandlingBean = resource.getSchemaHandling();
        var schemaHandling =
                schemaHandlingBean != null ?
                        configItem(
                                schemaHandlingBean,
                                ConfigurationItemOrigin.inResourceOrAncestor(resource, ResourceType.F_SCHEMA_HANDLING),
                                SchemaHandlingConfigItem.class) :
                        emptySchemaHandlingConfigItem();
        var associationCapabilityBean = CapabilityUtil.getCapability(resource, null, AssociationsCapabilityType.class);
        var associationsCapabilityCI = configItemNullable(
                associationCapabilityBean,
                inResourceOrAncestor(
                        resource,
                        ItemPath.create(ResourceType.F_CAPABILITIES, CapabilitiesType.F_CONFIGURED, CapabilityCollectionType.F_ASSOCIATIONS)),
                AssociationsCapabilityConfigItem.class);
        var completeResourceSchema = new CompleteResourceSchemaImpl(
                nativeSchema,
                BasicResourceInformation.of(resource),
                ResourceTypeUtil.isCaseIgnoreAttributeNames(resource));
        var parser = new ResourceSchemaParser(
                resource,
                schemaHandling,
                nativeSchema,
                associationsCapabilityCI,
                "definition of " + resource,
                completeResourceSchema);
        parser.parse();
        return completeResourceSchema;
    }

    private static @NotNull SchemaHandlingConfigItem emptySchemaHandlingConfigItem() {
        return configItem(
                new SchemaHandlingType(),
                ConfigurationItemOrigin.generated(), // hopefully no errors will be reported here
                SchemaHandlingConfigItem.class);
    }

    static BareResourceSchema parseBare(@NotNull NativeResourceSchema nativeSchema) throws SchemaException {
        var bareSchema = new BareResourceSchemaImpl(nativeSchema);
        var parser = new ResourceSchemaParser(
                new ResourceType(),
                emptySchemaHandlingConfigItem(),
                nativeSchema,
                null,
                "bare schema",
                bareSchema);
        try {
            parser.parse();
        } catch (ConfigurationException e) {
            throw SystemException.unexpected(e);
        }
        return bareSchema;
    }

    /** Creates the parsed resource schema. */
    private void parse() throws SchemaException, ConfigurationException {

        schemaHandling.checkAttributeNames();

        createEmptyObjectClassDefinitions();
        createEmptyObjectTypeDefinitions();

        // In theory, this could be done alongside creation of empty object type definitions.
        resolveAuxiliaryObjectClassNames();

        // We can parse attributes only after we have all the object class info parsed (including auxiliary object classes)
        parseAttributes();

        // Protected objects and delineation. They refer to attributes.
        parseOtherFeatures();

        // Associations refer to object types, attributes, and delineation, so they must come after them.
        parseAssociationClassesImplementations();
        parseAssociationTypesDefinitions();
        parseAssociationItems();

        resourceSchema.freeze();
    }

    private void createEmptyObjectClassDefinitions() throws ConfigurationException, SchemaException {

        LOGGER.trace("Creating refined object class definitions");

        var classDefinitionCIs = schemaHandling.getObjectClasses();
        for (var classDefinitionCI : classDefinitionCIs) {
            var objectClassName = classDefinitionCI.getObjectClassName();
            var rawObjectClassDefinition =
                    classDefinitionCI.configNonNull(
                            nativeSchema.findObjectClassDefinition(objectClassName),
                            "Object class %s referenced in %s does not exist on the resource".formatted(objectClassName, DESC));
            assertClassNotDefinedYet(objectClassName);
            resourceSchema.add(
                    ResourceObjectClassDefinitionImpl.create(
                            basicResourceInformation, rawObjectClassDefinition, classDefinitionCI.value()));
            classDefinitionConfigItemMap.put(classDefinitionCI.getObjectClassName().getLocalPart(), classDefinitionCI);
        }

        LOGGER.trace("Created {} refined object class definitions from beans; creating remaining ones", classDefinitionCIs.size());

        for (var nativeObjectClassDefinition : nativeSchema.getObjectClassDefinitions()) {
            if (resourceSchema.findObjectClassDefinition(nativeObjectClassDefinition.getQName()) == null) {
                resourceSchema.add(
                        ResourceObjectClassDefinitionImpl.create(
                                basicResourceInformation, nativeObjectClassDefinition, null));
                classDefinitionConfigItemMap.put(nativeObjectClassDefinition.getName(), null);
            }
        }

        LOGGER.trace("Successfully created {} refined object type definitions (in total)",
                resourceSchema.getObjectClassDefinitions().size());
    }

    private void assertClassNotDefinedYet(QName objectClassName) throws ConfigurationException {
        MiscUtil.configCheck(
                resourceSchema.findObjectClassDefinition(objectClassName) == null,
                "Multiple definitions for object class %s in %s", objectClassName, resource);
    }

    private void createEmptyObjectTypeDefinitions() throws SchemaException, ConfigurationException {
        LOGGER.trace("Creating empty object type definitions");
        int created = 0;
        for (var typeDefinitionCI : schemaHandling.getAllObjectTypes()) {
            if (!typeDefinitionCI.isAbstract()) {
                ResourceObjectTypeDefinition definition = createEmptyObjectTypeDefinition(typeDefinitionCI);
                LOGGER.trace("Created (empty) object type definition: {}", definition);
                assertTypeNotDefinedYet(definition);
                resourceSchema.add(definition);
                created++;
            } else {
                LOGGER.trace("Ignoring abstract definition bean: {}", typeDefinitionCI);
            }
        }
        checkForMultipleDefaults();
        LOGGER.trace("Successfully created {} empty object type definitions", created);
    }


    private void assertTypeNotDefinedYet(ResourceObjectTypeDefinition definition) throws ConfigurationException {
        ResourceObjectTypeIdentification identification = definition.getTypeIdentification();
        var existing = resourceSchema.getObjectTypeDefinition(identification);
        if (existing != null) {
            throw new ConfigurationException("Multiple definitions of " + identification + " in " + contextDescription);
        }
    }

    private <B extends ResourceObjectTypeDefinitionType> ResourceObjectTypeDefinition createEmptyObjectTypeDefinition(
            @NotNull AbstractResourceObjectTypeDefinitionConfigItem<B> definitionCI)
            throws SchemaException, ConfigurationException {

        ResourceObjectTypeIdentification identification = definitionCI.getTypeIdentification();

        // Object class refinement is not merged here (yet). We assume that the object class name could be woven into
        // the bean at any level. And we hope that although we do the merging in the top-bottom direction, it will cause
        // no harm if we merge the object class refinement (i.e. topmost component) at last.
        ObjectTypeExpansion expansion = new ObjectTypeExpansion();
        B expandedBean = expansion.expand(definitionCI.value());

        // We assume that the path was not changed. Quite a hack, though.
        AbstractResourceObjectTypeDefinitionConfigItem<?> expandedCI;
        if (definitionCI instanceof AssociatedResourceObjectTypeDefinitionConfigItem) {
            //noinspection RedundantTypeArguments : The type arguments aren't redundant: they are needed for some Java compilers
            expandedCI = ConfigurationItem.<AssociatedResourceObjectTypeDefinitionType, AssociatedResourceObjectTypeDefinitionConfigItem>configItem(
                    (AssociatedResourceObjectTypeDefinitionType) expandedBean,
                    definitionCI.origin(),
                    AssociatedResourceObjectTypeDefinitionConfigItem.class);
        } else {
            //noinspection RedundantTypeArguments : see above
            expandedCI = ConfigurationItem.<ResourceObjectTypeDefinitionType, ResourceObjectTypeDefinitionConfigItem>configItem(
                    expandedBean,
                    definitionCI.origin(),
                    ResourceObjectTypeDefinitionConfigItem.class);
        }

        QName objectClassName = expandedCI.getObjectClassName();
        ResourceObjectClassDefinition objectClassDefinition = getObjectClassDefinitionRequired(objectClassName, expandedCI);

        ResourceObjectTypeDefinitionType objectClassRefinementBean = objectClassDefinition.getDefinitionBean();
        merge(expandedBean, objectClassRefinementBean); // no-op if refinement bean is empty

        typeDefinitionConfigItemMap.put(expandedCI.getTypeIdentification(), expandedCI);

        return new ResourceObjectTypeDefinitionImpl(
                basicResourceInformation,
                identification,
                expansion.getAncestorsIds(),
                objectClassDefinition,
                CloneUtil.toImmutable(expandedBean));
    }

    private @NotNull ResourceObjectClassDefinition getObjectClassDefinitionRequired(
            @NotNull QName objectClassName, @NotNull ConfigurationItem<?> contextCI) throws ConfigurationException {
        return contextCI.configNonNull(
                resourceSchema.findObjectClassDefinition(objectClassName),
                "No definition found for object class '%s' referenced in %s", objectClassName, DESC);
    }

    /** Merges "source" (super-type) into "target" (sub-type). */
    private void merge(
            @NotNull ResourceObjectTypeDefinitionType target,
            @NotNull ResourceObjectTypeDefinitionType source) throws SchemaException, ConfigurationException {
        new ResourceObjectTypeDefinitionMergeOperation(target, source, null)
                .execute();
    }

    /**
     * Checks that there is at most single default for any kind.
     *
     * @throws ConfigurationException If there's a problem. Note that during run time, we throw {@link IllegalStateException}
     * in these cases (as we assume this check was already done).
     */
    private void checkForMultipleDefaults() throws ConfigurationException {
        for (ShadowKindType kind : ShadowKindType.values()) {
            var defaults = resourceSchema.getObjectTypeDefinitions().stream()
                    .filter(def -> def.matchesKind(kind) && def.isDefaultForKind())
                    .collect(Collectors.toList());
            configCheck(defaults.size() <= 1, "More than one default %s definition in %s: %s",
                    kind, contextDescription, defaults);
        }
    }

    /**
     * Fills in list of auxiliary object class definitions (in object type definitions)
     * with definitions resolved from their qualified names.
     */
    private void resolveAuxiliaryObjectClassNames() throws ConfigurationException {
        for (ResourceObjectDefinition objectDef: resourceSchema.getResourceObjectDefinitions()) {
            new ResourceObjectDefinitionParser(objectDef)
                    .resolveAuxiliaryObjectClassNames();
        }
    }

    /**
     * Creates definitions for associations; includes resolving their targets (given by kind + intent(s)).
     */
    private void parseAssociationItems() throws ConfigurationException {
        for (var objectDefinition : resourceSchema.getResourceObjectDefinitions()) {
            new ResourceObjectDefinitionParser(objectDefinition)
                    .parseAssociationItems();
        }
    }

    /**
     * Creates {@link ShadowAssociationClassImplementation} objects
     * in {@link ResourceSchemaImpl#associationClassImplementationsMap}.
     *
     * Does *not* include legacy simulated associations! They are "anonymous" as they do not have a type name.
     */
    private void parseAssociationClassesImplementations() throws ConfigurationException {

        // Simulated associations (from capabilities)
        if (associationsCapabilityCI != null) {
            for (SimulatedAssociationClassConfigItem simulatedAssociationClassDefinitionCI : associationsCapabilityCI.getAssociationClasses()) {
                resourceSchema.addAssociationClassImplementation(
                        ShadowAssociationClassSimulationDefinition.Modern.parse(
                                simulatedAssociationClassDefinitionCI,
                                schemaHandling.findAssociationTypeCI(simulatedAssociationClassDefinitionCI.getName()),
                                resourceSchema));
            }
        }

        // Native associations
        for (NativeAssociationClassDefinition associationClassDefinition : nativeSchema.getAssociationClassDefinitions()) {
            resourceSchema.addAssociationClassImplementation(
                    ShadowAssociationClassImplementation.Native.of(
                            associationClassDefinition,
                            resourceSchema));
        }
    }

    /**
     * Creates {@link ShadowAssociationClassDefinition} objects in {@link ResourceSchemaImpl#associationTypeDefinitionsMap}.
     * These are based on native or simulated association classes.
     *
     * Legacy simulated associations are not dealt with here.
     */
    private void parseAssociationTypesDefinitions() throws ConfigurationException {
        // Explicitly defined types
        for (var assocTypeDefCI : schemaHandling.getAssociationTypes()) {
            String className = assocTypeDefCI.getAssociationClassLocalName();
            var implementation = assocTypeDefCI.configNonNull(
                    resourceSchema.getAssociationClassImplementation(className),
                    "No association class '%s' in %s", className, DESC);
            resourceSchema.addAssociationTypeDefinition(
                    ShadowAssociationClassDefinition.fromAssociationType(assocTypeDefCI, implementation, resourceSchema));
        }

        // Types from association classes
        for (var associationClassImplementation : resourceSchema.getAssociationClassImplementations()) {
            if (resourceSchema.getAssociationTypeDefinition(associationClassImplementation.getName()) == null) {
                resourceSchema.addAssociationTypeDefinition(
                        ShadowAssociationClassDefinition.fromImplementation(associationClassImplementation));
            }
        }
    }

    private void parseAttributes() throws ConfigurationException {
        for (ResourceObjectDefinition objectDef : resourceSchema.getResourceObjectDefinitions()) {
            new ResourceObjectDefinitionParser(objectDef)
                    .parseAttributes();
        }
    }

    private void parseOtherFeatures() throws SchemaException, ConfigurationException {
        for (ResourceObjectDefinition objectDef : resourceSchema.getResourceObjectDefinitions()) {
            new ResourceObjectDefinitionParser(objectDef)
                    .parseOtherFeatures();
        }
    }

    /**
     * Creates and updates {@link ResourceObjectTypeDefinition} from
     *
     * - "raw" {@link ResourceObjectClassDefinition},
     * - refinements defined in {@link ResourceObjectTypeDefinitionType} (`schemaHandling`)
     *
     * Note: this class is instantiated multiple times during parsing of a schema. It should not be
     * a problem, as it is quite lightweight.
     */
    private class ResourceObjectDefinitionParser {

        /**
         * Specific object definition being updated.
         */
        @NotNull private final AbstractResourceObjectDefinitionImpl definition;

        /**
         * Definition CI from `schemaHandling` section; taken from {@link #definition}.
         *
         * It is merged from all the super-resources and super-types. Its CI origin and CI parent are
         * set artificially (for now).
         */
        @NotNull private final AbstractResourceObjectDefinitionConfigItem<?> definitionCI;

        ResourceObjectDefinitionParser(@NotNull ResourceObjectDefinition definition) {
            this.definition = (AbstractResourceObjectDefinitionImpl) definition;
            if (definition instanceof ResourceObjectTypeDefinition typeDefinition) {
                this.definitionCI = stateNonNull(
                        typeDefinitionConfigItemMap.get(typeDefinition.getTypeIdentification()),
                        "No cached configuration item for %s", typeDefinition);
            } else if (definition instanceof ResourceObjectClassDefinition) {
                //noinspection RedundantTypeArguments : actually needed by the Java compiler
                this.definitionCI = Objects.requireNonNullElseGet(
                        classDefinitionConfigItemMap.get(definition.getObjectClassName().getLocalPart()),
                        () -> ConfigurationItem.<ResourceObjectTypeDefinitionType, ResourceObjectClassDefinitionConfigItem>configItem(
                                new ResourceObjectTypeDefinitionType(),
                                ConfigurationItemOrigin.generated(),
                                ResourceObjectClassDefinitionConfigItem.class));
            } else {
                throw new IllegalStateException("Neither object type or object class? " + definition);
            }
        }

        void resolveAuxiliaryObjectClassNames() throws ConfigurationException {
            for (QName auxObjectClassName : definitionCI.getAuxiliaryObjectClassNames()) {
                definition.addAuxiliaryObjectClassDefinition(
                        getObjectClassDefinitionRequired(auxObjectClassName, definitionCI));
            }
        }

        /**
         * Here we
         */
        void parseAssociationItems() throws ConfigurationException {

            LOGGER.trace("Parsing associations of {}", definition);

            // First, we process native associations
            for (var nativeAssocDef : definition.getNativeObjectClassDefinition().getAssociationDefinitions()) {
                parseNativeAssociation(nativeAssocDef);
            }

            // Then, let's process "modern" simulated associations
            for (var assocTypeDef : resourceSchema.getAssociationTypes()) {
                var simulationDefinition = assocTypeDef.getSimulationDefinition();
                if (simulationDefinition != null) {
                    parseModernSimulatedAssociation(simulationDefinition);
                }
            }

            // Finally, let's take legacy simulated associations
            for (var assocDefCI : definitionCI.getAssociations()) {
                QName assocName = assocDefCI.getAssociationName();
                if (!definition.containsAssociationDefinition(assocName)) {
                    // Not present -> it must be legacy one
                    definition.addAssociationDefinition(
                            new LegacyAssociationParser(assocDefCI.asLegacy(), definition)
                                    .parse());
                }
            }
        }

        private void parseNativeAssociation(@NotNull NativeShadowAssociationDefinition nativeAssocDef)
                throws ConfigurationException {
            ItemName assocName = nativeAssocDef.getItemName();
            LOGGER.trace("Parsing association {}", assocName);

            var assocDefBean = value(definitionCI.getAssociationDefinitionIfPresent(assocName));
            var assocTypeDef = stateNonNull(
                    resourceSchema.getAssociationTypeDefinition(nativeAssocDef.getTypeName()),
                    "Unknown association type '%s' (for '%s' in %s) in %s",
                    nativeAssocDef.getTypeName(), assocName, definition, resourceSchema);
            definition.add(
                    ShadowAssociationDefinitionImpl.fromNative(nativeAssocDef, assocTypeDef, assocDefBean));
        }

        private void parseModernSimulatedAssociation(@NotNull ShadowAssociationClassSimulationDefinition simulationDefinition)
                throws ConfigurationException {
            ItemName assocName = simulationDefinition.getLocalSubjectItemName();
            LOGGER.trace("Parsing association {}", assocName);

            var assocDefBean = value(definitionCI.getAssociationDefinitionIfPresent(assocName));
            var assocTypeDef = stateNonNull(
                    resourceSchema.getAssociationTypeDefinition(simulationDefinition.getQName()),
                    "Unknown association type '%s' (for '%s' in %s) in %s",
                    simulationDefinition.getQName(), assocName, definition, resourceSchema);
            definition.add(
                    ShadowAssociationDefinitionImpl.fromSimulated(simulationDefinition, assocTypeDef, assocDefBean));
        }

        /**
         * Fills-in attribute definitions in `typeDef` by traversing all "raw" attributes defined in the structural
         * object class and all the auxiliary object classes.
         *
         * Initializes identifier names and protected objects patterns.
         */
        private void parseAttributes() throws ConfigurationException {

            LOGGER.trace("Parsing attributes of {}", definition);

            parseAttributesFromNativeObjectClass(definition.getNativeObjectClassDefinition(), false);
            for (ResourceObjectDefinition auxDefinition : definition.getAuxiliaryDefinitions()) {
                parseAttributesFromNativeObjectClass(auxDefinition.getNativeObjectClassDefinition(), true);
            }

            assertNoOtherAttributes();

            setupIdentifiers();
        }

        /**
         * There should be no attributes in the `schemaHandling` definition without connector-provided (raw schema)
         * counterparts.
         */
        private void assertNoOtherAttributes() throws ConfigurationException {
            for (ResourceAttributeDefinitionConfigItem attributeDefCI : definitionCI.getAttributes()) {
                QName attrName = attributeDefCI.getAttributeName();
                // TODO check that we really look into aux object classes
                if (!definition.containsAttributeDefinition(attrName)
                        && !attributeDefCI.isIgnored()) {
                    throw attributeDefCI.configException(
                            "Definition of attribute '%s' not found in object class '%s' "
                                    + "nor auxiliary object classes for '%s' as defined in %s",
                            attrName, definition.getObjectClassName(), definition, DESC);
                }
            }
        }

        /**
         * Takes all attributes from resource object class definition, and pairs (enriches)
         * them with `schemaHandling` information.
         */
        private void parseAttributesFromNativeObjectClass(@NotNull NativeObjectClassDefinition nativeClassDef, boolean auxiliary)
                throws ConfigurationException {
            for (NativeShadowAttributeDefinition<?> attrDef : nativeClassDef.getAttributeDefinitions()) {
                parseNativeAttribute(attrDef, auxiliary);
            }
        }

        private void parseNativeAttribute(@NotNull NativeShadowAttributeDefinition<?> rawAttrDef, boolean fromAuxClass)
                throws ConfigurationException {

            ItemName attrName = rawAttrDef.getItemName();

            LOGGER.trace("Parsing attribute {} (auxiliary = {})", attrName, fromAuxClass);

            // We MUST NOT skip ignored attribute definitions here. We must include them in the schema as
            // the shadows will still have that attributes and we will need their type definition to work
            // well with them. They may also be mandatory. We cannot pretend that they do not exist.

            if (definition.containsAttributeDefinition(attrName)) {
                if (fromAuxClass) {
                    return;
                } else {
                    throw definitionCI.configException("Duplicate definition of attribute '%s' in %s", attrName, DESC);
                }
            }

            ResourceAttributeDefinitionType attrDefBean = value(definitionCI.getAttributeDefinitionIfPresent(attrName));
            ResourceAttributeDefinition<?> attrDef;
            try {
                boolean ignored = ignoredAttributes.contains(attrName);
                attrDef = ResourceAttributeDefinitionImpl.create(rawAttrDef, attrDefBean, ignored);
            } catch (SchemaException e) { // TODO throw the configuration exception right in the 'create' method
                throw definitionCI.configException("Error while parsing attribute '%s' in %s: %s", attrName, DESC, e.getMessage());
            }
            definition.add(attrDef);

            if (attrDef.isDisplayNameAttribute()) {
                definition.setDisplayNameAttributeName(attrName);
            }
        }

        /**
         * Copy all primary identifiers from the raw definition.
         *
         * For secondary ones, use configured information (if present). Otherwise, use raw definition as well.
         */
        private void setupIdentifiers() {
            NativeObjectClassDefinition nativeDefinition = definition.getNativeObjectClassDefinition();

            for (ResourceAttributeDefinition<?> attrDef : definition.getAttributeDefinitions()) {
                ItemName attrName = attrDef.getItemName();

                if (nativeDefinition.isPrimaryIdentifier(attrName)) {
                    definition.getPrimaryIdentifiersNames().add(attrName);
                }
                if (attrDef.isSecondaryIdentifierOverride() == null) {
                    if (nativeDefinition.isSecondaryIdentifier(attrName)) {
                        definition.getSecondaryIdentifiersNames().add(attrName);
                    }
                } else {
                    if (attrDef.isSecondaryIdentifierOverride()) {
                        definition.getSecondaryIdentifiersNames().add(attrName);
                    }
                }
            }
        }

        /**
         * Parses protected objects, delineation, and so on.
         */
        private void parseOtherFeatures() throws SchemaException, ConfigurationException {
            parseProtected();
            parseDelineation();
        }

        /**
         * Converts protected objects patterns from "bean" to "compiled" form.
         */
        private void parseProtected() throws SchemaException, ConfigurationException {
            List<ResourceObjectPatternType> protectedPatternBeans = definitionCI.value().getProtected();
            if (protectedPatternBeans.isEmpty()) {
                return;
            }
            var prismObjectDef = definition.toPrismObjectDefinition();
            for (ResourceObjectPatternType protectedPatternBean : protectedPatternBeans) {
                definition.addProtectedObjectPattern(
                        convertToPattern(protectedPatternBean, prismObjectDef));
            }
        }

        private ResourceObjectPattern convertToPattern(
                ResourceObjectPatternType patternBean, PrismObjectDefinition<ShadowType> prismObjectDef)
                throws SchemaException, ConfigurationException {
            SearchFilterType filterBean =
                    MiscUtil.configNonNull(
                            patternBean.getFilter(),
                            () -> "No filter in resource object pattern");
            ObjectFilter filter =
                    MiscUtil.configNonNull(
                            PrismContext.get().getQueryConverter().parseFilter(filterBean, prismObjectDef),
                            () -> "No filter in resource object pattern");
            return new ResourceObjectPattern(definition, filter);
        }

        private void parseDelineation() throws ConfigurationException {
            QName objectClassName = definition.getObjectClassName();
            var definitionBean = definitionCI.value(); // TODO move this processing right into the CI
            List<QName> auxiliaryObjectClassNames = definitionCI.getAuxiliaryObjectClassNames();
            ResourceObjectTypeDelineationType delineationBean = definitionBean.getDelineation();
            if (delineationBean != null) {
                definitionCI.configCheck(definitionBean.getBaseContext() == null,
                        "Legacy base context cannot be set when delineation is configured; in %s", DESC);
                definitionCI.configCheck(definitionBean.getSearchHierarchyScope() == null,
                        "Legacy search hierarchy scope cannot be set when delineation is configured; in %s", DESC);
                definition.setDelineation(
                        ResourceObjectTypeDelineation.of(
                                delineationBean,
                                objectClassName,
                                auxiliaryObjectClassNames,
                                definition));
            } else {
                definition.setDelineation(
                        ResourceObjectTypeDelineation.of(
                                definitionBean.getBaseContext(),
                                definitionBean.getSearchHierarchyScope(),
                                objectClassName,
                                auxiliaryObjectClassNames));
            }
        }
    }

    /** Resolves super-type references for given object type definition bean. */
    private class ObjectTypeExpansion {

        /** Object type identifiers that have been seen when resolving the ancestors. Used also to detect cycles. */
        private final Set<ResourceObjectTypeIdentification> ancestorsIds = new HashSet<>();

        /**
         * Expands the definition by resolving its ancestor (if there's any), recursively if needed.
         *
         * Does not modify existing {@link #resource} object, so it clones the beans that are being expanded.
         */
        private <T extends ResourceObjectTypeDefinitionType> @NotNull T expand(@NotNull T definitionBean)
                throws ConfigurationException, SchemaException {
            var superRef = definitionBean.getSuper();
            if (superRef == null) {
                return definitionBean;
            } else {
                ResourceObjectTypeDefinitionType superBean = find(superRef);
                if (!ancestorsIds.add(ResourceObjectTypeIdentification.of(superBean))) {
                    throw new ConfigurationException(
                            "A cycle in super-type hierarchy, detected at %s (contains: %s) in %s".formatted(
                                    superBean, ancestorsIds, contextDescription));
                }
                ResourceObjectTypeDefinitionType expandedSuperBean = expand(superBean);
                //noinspection unchecked
                T expandedSubBean = (T) definitionBean.clone();
                merge(expandedSubBean, expandedSuperBean);
                return expandedSubBean;
            }
        }

        /** Resolves the reference to a super-type. Must be in the same resource. TODO migrate to CIs. */
        private @NotNull ResourceObjectTypeDefinitionType find(@NotNull ResourceObjectTypeIdentificationType superRefBean)
                throws ConfigurationException {
            SuperReference superRef = SuperReference.of(superRefBean);
            List<ResourceObjectTypeDefinitionType> matching = schemaHandling.getAllObjectTypes().stream()
                    .map(ci -> ci.value())
                    .filter(superRef::matches)
                    .collect(Collectors.toList());
            return MiscUtil.extractSingletonRequired(matching,
                    () -> new ConfigurationException("Multiple definitions matching " + superRef + " found in " + contextDescription),
                    () -> new ConfigurationException("No definition matching " + superRef + " found in " + contextDescription));
        }

        private @NotNull Set<ResourceObjectTypeIdentification> getAncestorsIds() {
            return ancestorsIds;
        }
    }

    /** Parses given legacy ("pre-4.9") association type. */
    private class LegacyAssociationParser {

        @NotNull private final ResourceObjectAssociationConfigItem.Legacy associationDefCI;

        /** This one is being built. */
        @NotNull private final ResourceObjectTypeDefinition subjectTypeDefinition;

        LegacyAssociationParser(
                @NotNull ResourceObjectAssociationConfigItem.Legacy associationDefCI,
                @NotNull AbstractResourceObjectDefinitionImpl subjectDefinition) throws ConfigurationException {
            this.associationDefCI = associationDefCI;
            if (!(subjectDefinition instanceof ResourceObjectTypeDefinition _subjectTypeDefinition)) {
                throw new ConfigurationException("Associations cannot be defined on object classes: " + subjectDefinition);
            }
            this.subjectTypeDefinition = _subjectTypeDefinition;
        }

        @NotNull ShadowAssociationDefinitionImpl parse() throws ConfigurationException {

            var objectTypeDefinitions = getObjectTypeDefinitions();

            var simulationDefinition = ShadowAssociationClassSimulationDefinition.Legacy.parse(
                    associationDefCI, resourceSchema, subjectTypeDefinition, objectTypeDefinitions);

            var associationTypeDefinition = ShadowAssociationClassDefinition.parseLegacy(
                    associationDefCI, simulationDefinition, subjectTypeDefinition, objectTypeDefinitions);

            checkNotPresentAsNative();

            return ShadowAssociationDefinitionImpl.parseLegacy(associationTypeDefinition, associationDefCI);
        }

        private void checkNotPresentAsNative() throws ConfigurationException {
            var nativeClassDef = subjectTypeDefinition.getNativeObjectClassDefinition();
            ItemName associationName = associationDefCI.getAssociationName();
            associationDefCI.configCheck(
                    nativeClassDef.findAssociationDefinition(associationName) == null,
                    "Native association '%s' already exists in %s; referenced by %s",
                    associationName, nativeClassDef, DESC);
        }

        /**
         * Returns object type definition matching given kind and one of the intents, specified in the association definition.
         *
         * (If no intents are provided, default type for given kind is returned.
         * We are not very eager here - by default we mean just the flag "default for kind" being set.)
         *
         * The matching types must share at least the object class name. This is checked by this method.
         * However, in practice they must share much more, as described in the description for
         * {@link ResourceObjectAssociationType#getIntent()} (see XSD).
         */
        private @NotNull Collection<ResourceObjectTypeDefinition> getObjectTypeDefinitions()
                throws ConfigurationException {

            var kind = associationDefCI.getKind();
            var intents = associationDefCI.getIntents();

            Predicate<ResourceObjectTypeDefinition> predicate =
                    objectDef -> {
                        if (objectDef.getKind() != kind) {
                            return false;
                        }
                        if (((Collection<String>) intents).isEmpty()) {
                            return objectDef.isDefaultForKind();
                        } else {
                            return ((Collection<String>) intents).contains(objectDef.getIntent());
                        }
                    };

            var predicateDescription = lazy(() -> "kind " + kind + ", intents " + intents);

            Collection<ResourceObjectTypeDefinition> matching =
                    resourceSchema.getObjectTypeDefinitions().stream()
                            .filter(predicate)
                            .toList();
            if (matching.isEmpty()) {
                throw associationDefCI.configException(
                        "No matching object type definition found for %s in %s", predicateDescription, DESC);
            } else if (ResourceSchemaUtil.areDefinitionsCompatible(matching)) {
                return matching;
            } else {
                throw associationDefCI.configException(
                        "Incompatible definitions found for %s in %s: %s", predicateDescription, DESC, matching);
            }
        }
    }
}
