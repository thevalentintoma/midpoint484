/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import static com.evolveum.midpoint.provisioning.impl.resourceobjects.EntitlementUtils.*;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.schema.config.AssociationConfigItem.AttributeBinding;
import com.evolveum.midpoint.schema.processor.ShadowAssociationClassSimulationDefinition;
import com.evolveum.midpoint.schema.processor.ShadowAssociationDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Reads the entitlements of a subject (resource object): either from the values of the subject itself,
 * or by searching for relevant objects on the resource (delegated to {@link EntitlementObjectSearch}).
 */
class EntitlementReader {

    private static final Trace LOGGER = TraceManager.getTrace(EntitlementReader.class);

    /** The resource object for which we are trying to obtain the entitlements. */
    @NotNull private final ResourceObject subject;

    /** {@link ProvisioningContext} of the subject fetch/search/whatever operation. */
    @NotNull private final ProvisioningContext subjectCtx;

    private EntitlementReader(@NotNull ResourceObject subject, @NotNull ProvisioningContext subjectCtx) {
        this.subject = subject;
        this.subjectCtx = subjectCtx;
    }

    /**
     * Fills-in the subject's associations container (`association`) based on the relevant resource object attribute values.
     * Note that for "object to subject" entitlements this involves a search operation.
     */
    public static void read(
            @NotNull ResourceObject subject,
            @NotNull ProvisioningContext subjectCtx,
            @NotNull OperationResult result)
            throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        new EntitlementReader(subject, subjectCtx)
                .doRead(result);
    }

    private void doRead(OperationResult result)
            throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        LOGGER.trace("Starting simulated associations read operation");
        for (ShadowAssociationDefinition associationDef : subjectCtx.getAssociationDefinitions()) {
            if (!isSimulated(associationDef)
                    || !isVisible(associationDef, subjectCtx)
                    || !doesMatchSubjectDelineation(associationDef, subjectCtx)) {
                continue;
            }
            var simulationDefinition = Objects.requireNonNull(associationDef.getSimulationDefinition());
            LOGGER.trace("Processing simulated association {}: {}", associationDef, simulationDefinition);
            var primaryBinding = simulationDefinition.getPrimaryAttributeBinding();
            var secondaryBinding = simulationDefinition.getSecondaryAttributeBinding();
            if (simulationDefinition.isSubjectToObject()) {
                convertSubjectAttributeToAssociation(primaryBinding, simulationDefinition);
            } else if (secondaryBinding != null) {
                convertSubjectAttributeToAssociation(secondaryBinding, simulationDefinition);
            } else {
                searchForAssociationTargetObjects(primaryBinding, simulationDefinition, result);
            }
        }
        LOGGER.trace("Finished simulated associations read operation");
    }

    /**
     * Creates the association values from the (subject) attribute values.
     *
     * It simply uses values of the subject side attribute of the binding to construct identifiers
     * pointing to the entitlement object.
     *
     * For example, if
     *
     * - isMemberOf: "cn=wheel,ou=Groups,dc=example,dc=com"; "cn=users,ou=Groups,dc=example,dc=com"
     * - referenced identifier is "dn" (of a group)
     * - association name = "ri:group"
     *
     * then the result would be:
     *
     *     ri:group:
     *       PCV: identifiers: { dn: "cn=wheel,ou=Groups,dc=example,dc=com" }
     *       PCV: identifiers: { dn: "cn=users,ou=Groups,dc=example,dc=com" }
     *
     * NOTE: These values are not filtered according to the association type definition;
     * because this is just a simulation of what would the resource do.
     * The filtering will come in upper layers - specifically, in shadowed object construction.
     */
    private <T> void convertSubjectAttributeToAssociation(
            @NotNull AttributeBinding bindingToUse,
            @NotNull ShadowAssociationClassSimulationDefinition simulationDefinition) throws SchemaException {

        // The "referencing" attribute, present on subject, e.g. isMemberOf.
        var subjectAttrValues = subject.<T>getAttributeValues(bindingToUse.subjectSide());
        if (subjectAttrValues.isEmpty()) {
            return; // ending here, to avoid e.g. creating the association if there are no values
        }

        // The "referenced" attribute, present on object, e.g. dn (Group DN).
        var objectAttrDef = simulationDefinition.<T>getObjectAttributeDefinition(bindingToUse);

        var association = subject
                .getOrCreateAssociationsContainer()
                .findOrCreateAssociation(simulationDefinition.getLocalSubjectItemName());

        for (var subjectAttrValue : subjectAttrValues) {
            var newAssociationValue =
                    association.createNewValueWithIdentifier(
                            objectAttrDef.instantiateFromValue(subjectAttrValue.clone()));
            LOGGER.trace("Association attribute value resolved to association value {}", newAssociationValue);
        }
    }

    /**
     * Resolves the simulated association by searching for the target objects.
     *
     * Looks for objects with the binding attribute value - e.g. `ri:members` - containing the value of the subject binding
     * attribute - e.g. `ri:dn`.
     *
     * Iterates through all the delineations specified in the simulation.
     *
     * NOTE: Just as {@link #convertSubjectAttributeToAssociation(AttributeBinding, ShadowAssociationClassSimulationDefinition)},
     * this method does not filter the results according object types specified in the association type definition.
     */
    private void searchForAssociationTargetObjects(
            @NotNull AttributeBinding primaryBinding,
            @NotNull ShadowAssociationClassSimulationDefinition simulationDefinition,
            @NotNull OperationResult result)
            throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        var searchOp = new EntitlementObjectSearch<>(subjectCtx, simulationDefinition, primaryBinding, subject.getBean());
        if (searchOp.getSubjectAttrValue() == null) {
            LOGGER.trace("Ignoring association {} ({}) because the subject does not have any value in attribute {} "
                    + "(it is strange, because it should be an identifier)",
                    simulationDefinition.getLocalSubjectItemName(), primaryBinding, primaryBinding.subjectSide());
            return;
        }

        searchOp.execute((objectFound, lResult) -> {
            objectFound.initialize(subjectCtx.getTask(), lResult);
            if (objectFound.isOk()) {
                ExistingResourceObject resourceObject = objectFound.getResourceObject();
                try {
                    addAssociationValueFromEntitlementObject(simulationDefinition, resourceObject);
                } catch (SchemaException e) {
                    throw new EntitlementObjectSearch.LocalTunnelException(e);
                }
                LOGGER.trace("Processed entitlement-to-subject association for subject {} and entitlement (object) {}",
                        subject.getHumanReadableNameLazily(),
                        resourceObject.getHumanReadableNameLazily());
            } else {
                // TODO better handling
                throw new SystemException(
                        "Couldn't process entitlement: " + objectFound + ": " + objectFound.getExceptionEncountered());
            }
            return true;
        }, result);
    }

    /**
     * Creates association value from resolved target object; and adds it into the respective association
     * in `associationContainer`.
     *
     * For example, if
     *
     * - target object is the group of = "cn=wheel,ou=Groups,dc=example,dc=com"
     * - association name = "ri:groups"
     *
     * then the result would be:
     *
     *     ri:groups:
     *       PCV: shadowRef: object: attributes: { dn: "cn=wheel,ou=Groups,dc=example,dc=com" }
     */
    private void addAssociationValueFromEntitlementObject(
            @NotNull ShadowAssociationClassSimulationDefinition simulationDefinition,
            @NotNull ExistingResourceObject entitlementObject) throws SchemaException {
        subject.getOrCreateAssociationsContainer()
                .findOrCreateAssociation(simulationDefinition.getLocalSubjectItemName())
                .createNewValueWithFullObject(entitlementObject);
    }
}
