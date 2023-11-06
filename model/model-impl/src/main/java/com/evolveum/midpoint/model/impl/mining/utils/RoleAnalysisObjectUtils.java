/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.utils;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.toShortString;

import static java.util.Collections.singleton;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getCurrentXMLGregorianCalendar;
import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.loadIntersections;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType.F_MODIFY_TIMESTAMP;

import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;

import com.evolveum.midpoint.schema.SelectorOptions;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Utility methods for working with role analysis objects in the Midpoint system.
 */
public class RoleAnalysisObjectUtils {

    private static final Trace LOGGER = TraceManager.getTrace(RoleAnalysisObjectUtils.class);

    static Collection<SelectorOptions<GetOperationOptions>> defaultOptions = GetOperationOptionsBuilder.create().raw().build();

    /**
     * Retrieves a PrismObject of UserType object based on its OID.
     *
     * @param modelService The ModelService for accessing user data.
     * @param oid The OID of the UserType object to retrieve.
     * @param task The task associated with this operation.
     * @param result The operation result.
     * @return The PrismObject of UserType object, or null if not found.
     */
    public static PrismObject<UserType> getUserTypeObject(@NotNull ModelService modelService, String oid,
            Task task, OperationResult result) {

        try {
            return modelService.getObject(UserType.class, oid, defaultOptions, task, result);
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Couldn't get UserType object, Probably not set yet", ex);
        } finally {
            result.recomputeStatus();
        }
        return null;
    }

    /**
     * Retrieves a PrismObject of FocusType object based on its OID.
     *
     * @param modelService The ModelService for accessing data.
     * @param oid The OID of the FocusType object to retrieve.
     * @param task The task associated with this operation.
     * @param result The operation result.
     * @return The PrismObject of FocusType object, or null if not found.
     */
    public static PrismObject<FocusType> getFocusTypeObject(@NotNull ModelService modelService, String oid,
            Task task, OperationResult result) {

        try {
            return modelService.getObject(FocusType.class, oid, defaultOptions, task, result);
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Couldn't get FocusType object, Probably not set yet", ex);
        } finally {
            result.recomputeStatus();
        }
        return null;
    }

    /**
     * Retrieves a PrismObject of RoleType object based on its OID.
     *
     * @param modelService The ModelService for accessing role data.
     * @param oid The OID of the RoleType object to retrieve.
     * @param task The task associated with this operation.
     * @param result The operation result.
     * @return The PrismObject of RoleType object, or null if not found.
     */
    public static PrismObject<RoleType> getRoleTypeObject(@NotNull ModelService modelService, String oid,
            Task task, OperationResult result) {

        try {
            return modelService.getObject(RoleType.class, oid, defaultOptions, task, result);
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Couldn't get RoleType object, Probably not set yet", ex);
        } finally {
            result.recomputeStatus();
        }
        return null;
    }

    /**
     * Retrieves a PrismObject of RoleAnalysisClusterType object based on its OID.
     *
     * @param modelService The ModelService for accessing role analysis data.
     * @param oid The OID of the RoleAnalysisClusterType object to retrieve.
     * @param task The task associated with this operation.
     * @param result The operation result.
     * @return The PrismObject of RoleAnalysisClusterType object, or null if not found.
     */
    public static PrismObject<RoleAnalysisClusterType> getClusterTypeObject(@NotNull ModelService modelService, String oid,
            Task task, OperationResult result) {

        try {
            return modelService.getObject(RoleAnalysisClusterType.class, oid, defaultOptions, task, result);
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER,
                    "Couldn't get RoleAnalysisClusterType object, Probably not set yet", ex);
        } finally {
            result.recomputeStatus();
        }
        return null;
    }

    /**
     * Retrieves a PrismObject of RoleAnalysisSessionType object based on its OID.
     *
     * @param modelService The ModelService for accessing role analysis data.
     * @param oid The OID of the RoleAnalysisSessionType object to retrieve.
     * @param task The task associated with this operation.
     * @param result The operation result.
     * @return The PrismObject of RoleAnalysisSessionType object, or null if not found.
     */
    public static PrismObject<RoleAnalysisSessionType> getSessionTypeObject(@NotNull ModelService modelService,
            String oid, Task task, OperationResult result) {

        try {
            return modelService.getObject(RoleAnalysisSessionType.class, oid, defaultOptions, task, result);
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER,
                    "Couldn't get RoleAnalysisSessionType object, Probably not set yet", ex);
        } finally {
            result.recomputeStatus();
        }
        return null;
    }

    /**
     * Retrieves the number of RoleAnalysisSessionType objects in the system.
     *
     * @param modelService The ModelService for accessing role analysis data.
     * @param task The task associated with this operation.
     * @param result The operation result.
     * @return The number of RoleAnalysisSessionType objects in the system.
     */
    public static Integer getSessionTypeObjectCount(@NotNull ModelService modelService,
            Task task, OperationResult result) {

        try {
            return modelService.countObjects(RoleAnalysisSessionType.class, null, defaultOptions, task, result);
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER,
                    "Couldn't count RoleAnalysisSessionType object, Probably not set yet", ex);
        } finally {
            result.recomputeStatus();
        }
        return 0;
    }

    /**
     * Extracts a list of user members from set of RoleType object based on provided parameters.
     *
     * @param modelService The ModelService for accessing role analysis data.
     * @param userExistCache  The cache of user objects.
     * @param userFilter The UserType filter.
     * @param clusterMembers The set of cluster members.
     * @param task The task associated with this operation.
     * @param result   The operation result.
     * @return A list of user members.
     */
    //TODO Optimize this method. Loading role members is very expensive operation.
    public static ListMultimap<String, String> extractRoleMembers(ModelService modelService,
            Map<String, PrismObject<UserType>> userExistCache, ObjectFilter userFilter,
            Set<String> clusterMembers, Task task, OperationResult result) {

        ListMultimap<String, String> roleMemberCache = ArrayListMultimap.create();

        ObjectQuery query = PrismContext.get().queryFor(UserType.class)
                .exists(AssignmentHolderType.F_ASSIGNMENT)
                .block()
                .item(AssignmentType.F_TARGET_REF)
                .ref(clusterMembers.toArray(new String[0]))
                .endBlock().build();

        if (userFilter != null) {
            query.addFilter(userFilter);
        }

        ResultHandler<UserType> resultHandler = (userObject, parentResult) -> {
            try {
                boolean shouldCacheUser = false;
                List<AssignmentType> assignments = userObject.asObjectable().getAssignment();

                for (AssignmentType assignment : assignments) {
                    ObjectReferenceType targetRef = assignment.getTargetRef();
                    if (targetRef != null && clusterMembers.contains(targetRef.getOid())) {
                        roleMemberCache.put(targetRef.getOid(), userObject.getOid());
                        shouldCacheUser = true;
                    }
                }

                if (shouldCacheUser) {
                    userExistCache.put(userObject.getOid(), userObject);
                }
            } catch (Exception e) {
                String errorMessage = "Cannot resolve role members: " + toShortString(userObject.asObjectable()) + ": " + e.getMessage();
                throw new SystemException(errorMessage, e);
            }

            return true;
        };

        try {
            modelService.searchObjectsIterative(UserType.class, query, resultHandler, null,
                    task, result);
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Failed to search role member objects:", ex);
        } finally {
            result.recomputeStatus();
        }

        return roleMemberCache;
    }

    /**
     * Imports a RoleAnalysisClusterType object into the system.
     *
     * @param modelService The ModelService for accessing role analysis data.
     * @param cluster The cluster for importing.
     * @param roleAnalysisSessionDetectionOption The session detection option.
     * @param parentRef The parent Role analysis session reference.
     * @param task The task associated with this operation.
     * @param result The operation result.
     */
    public static void importRoleAnalysisClusterObject(@NotNull ModelService modelService,
            @NotNull PrismObject<RoleAnalysisClusterType> cluster,
            RoleAnalysisDetectionOptionType roleAnalysisSessionDetectionOption, ObjectReferenceType parentRef,
            Task task, OperationResult result) {
        cluster.asObjectable().setRoleAnalysisSessionRef(parentRef);
        cluster.asObjectable().setDetectionOption(roleAnalysisSessionDetectionOption);
        modelService.importObject(cluster, null, task, result);
    }

    /**
     * Modifies statistics of a RoleAnalysisSessionType object.
     *
     * @param modelService The ModelService for accessing role analysis data.
     * @param sessionRef The session reference.
     * @param sessionStatistic The session statistic to modify.
     * @param task The task associated with this operation.
     * @param result The operation result.
     */
    public static void modifySessionAfterClustering(ModelService modelService, ObjectReferenceType sessionRef,
            RoleAnalysisSessionStatisticType sessionStatistic,
            Task task, OperationResult result) {

        try {

            ObjectDelta<RoleAnalysisSessionType> delta = PrismContext.get().deltaFor(RoleAnalysisSessionType.class)
                    .item(RoleAnalysisSessionType.F_SESSION_STATISTIC)
                    .replace(sessionStatistic)
                    .asObjectDelta(sessionRef.getOid());

            modelService.executeChanges(singleton(delta), null, task, result);

        } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException | PolicyViolationException | SecurityViolationException e) {
            LOGGER.error("Couldn't modify  RoleAnalysisSessionType {}", sessionRef, e);
        }

    }

    /**
     * Replaces the detected patterns of a RoleAnalysisClusterType object.
     *
     * @param modelService The ModelService for accessing role analysis data.
     * @param clusterOid The cluster OID.
     * @param detectedPatterns The detected patterns to replace.
     * @param task The task associated with this operation.
     * @param result The operation result.
     */
    public static void replaceRoleAnalysisClusterDetectionPattern(ModelService modelService, String clusterOid,
            List<DetectedPattern> detectedPatterns, Task task, OperationResult result) {

        List<RoleAnalysisDetectionPatternType> roleAnalysisClusterDetectionTypes = loadIntersections(detectedPatterns);

        double max = 0;
        Collection<PrismContainerValue<?>> collection = new ArrayList<>();
        for (RoleAnalysisDetectionPatternType clusterDetectionType : roleAnalysisClusterDetectionTypes) {
            collection.add(clusterDetectionType.asPrismContainerValue());
            max = Math.max(max, clusterDetectionType.getClusterMetric());
        }

        PrismObject<RoleAnalysisClusterType> clusterTypeObject = getClusterTypeObject(modelService, clusterOid, task, result);

        if (clusterTypeObject == null) {
            return;
        }
        AnalysisClusterStatisticType clusterStatistics = clusterTypeObject.asObjectable().getClusterStatistics();

        AnalysisClusterStatisticType analysisClusterStatisticType = getAnalysisClusterStatisticType(max, clusterStatistics);

        try {

            ObjectDelta<RoleAnalysisClusterType> delta = PrismContext.get().deltaFor(RoleAnalysisClusterType.class)
                    .item(RoleAnalysisClusterType.F_DETECTED_PATTERN).replace(collection)
                    .item(RoleAnalysisClusterType.F_METADATA, F_MODIFY_TIMESTAMP).replace(getCurrentXMLGregorianCalendar())
                    .item(RoleAnalysisClusterType.F_CLUSTER_STATISTICS).replace(analysisClusterStatisticType
                            .asPrismContainerValue())
                    .asObjectDelta(clusterOid);

            modelService.executeChanges(singleton(delta), null, task, result);

        } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException | PolicyViolationException | SecurityViolationException e) {
            LOGGER.error("Couldn't modify RoleAnalysisClusterType {}", clusterOid, e);
        }

    }

    @NotNull
    private static AnalysisClusterStatisticType getAnalysisClusterStatisticType(double max,
            AnalysisClusterStatisticType clusterStatistics) {
        AnalysisClusterStatisticType analysisClusterStatisticType = new AnalysisClusterStatisticType();
        analysisClusterStatisticType.setDetectedReductionMetric(max);
        analysisClusterStatisticType.setMembershipDensity(clusterStatistics.getMembershipDensity());
        analysisClusterStatisticType.setRolesCount(clusterStatistics.getRolesCount());
        analysisClusterStatisticType.setUsersCount(clusterStatistics.getUsersCount());
        analysisClusterStatisticType.setMembershipMean(clusterStatistics.getMembershipMean());
        analysisClusterStatisticType.setMembershipRange(clusterStatistics.getMembershipRange());
        return analysisClusterStatisticType;
    }

    /**
     * Generates a set of object references based on a provided parameters.
     *
     * @param modelService The ModelService for accessing role analysis data.
     * @param objects The objects to create references for.
     * @param complexType The complex type of the objects.
     * @param task The task associated with this operation.
     * @param operationResult The operation result.
     * @return A set of object references.
     */
    public static @NotNull Set<ObjectReferenceType> createObjectReferences(ModelService modelService, Set<String> objects,
            QName complexType, Task task, OperationResult operationResult) {

        Set<ObjectReferenceType> objectReferenceList = new HashSet<>();
        for (String item : objects) {

            PrismObject<FocusType> object = getFocusTypeObject(modelService, item, task, operationResult);
            ObjectReferenceType objectReferenceType = new ObjectReferenceType();
            objectReferenceType.setType(complexType);
            objectReferenceType.setOid(item);
            if (object != null) {
                objectReferenceType.setTargetName(PolyStringType.fromOrig(object.getName().toString()));
            }
            objectReferenceList.add(objectReferenceType);

        }
        return objectReferenceList;
    }

    /**
     * Deletes all RoleAnalysisClusterType objects associated with a specific session.
     *
     * @param modelService The ModelService for accessing role analysis data.
     * @param sessionOid The session OID.
     * @param task The task associated with this operation.
     * @param result The operation result.
     */
    public static void deleteRoleAnalysisSessionClusters(ModelService modelService, String sessionOid,
            Task task, OperationResult result) {

        ResultHandler<RoleAnalysisClusterType> resultHandler = (object, parentResult) -> {
            try {
                deleteSingleRoleAnalysisCluster(modelService, object.asObjectable(), task, result);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return true;
        };

        ObjectQuery query = PrismContext.get().queryFor(RoleAnalysisClusterType.class)
                .item(RoleAnalysisClusterType.F_ROLE_ANALYSIS_SESSION_REF).ref(sessionOid)
                .build();

        try {
            modelService.searchObjectsIterative(RoleAnalysisClusterType.class, query, resultHandler, null,
                    task, result);
        } catch (Exception ex) {
            LoggingUtils.logExceptionOnDebugLevel(LOGGER, "Couldn't deleteRoleAnalysisSessionClusters", ex);
        }

    }

    /**
     * Deletes a single RoleAnalysisClusterType object.
     * @param modelService The ModelService for accessing role analysis data.
     * @param cluster The cluster to delete.
     * @param task The task associated with this operation.
     * @param result The operation result.
     */
    public static void deleteSingleRoleAnalysisCluster(@NotNull ModelService modelService,
            @NotNull RoleAnalysisClusterType cluster,
            Task task, OperationResult result) {

        String clusterOid = cluster.getOid();
        PrismObject<RoleAnalysisSessionType> sessionObject = getSessionTypeObject(modelService,
                cluster.getRoleAnalysisSessionRef().getOid(), task, result
        );

        if (sessionObject == null) {
            return;
        }

        try {

            ObjectDelta<RoleAnalysisClusterType> deleteDelta = PrismContext.get().deltaFactory().object()
                    .createDeleteDelta(RoleAnalysisClusterType.class, clusterOid);

            modelService.executeChanges(singleton(deleteDelta), null, task, result);

        } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException | PolicyViolationException | SecurityViolationException e) {
            LOGGER.error("Couldn't delete RoleAnalysisClusterType {}", clusterOid, e);
        }

        try {

            ObjectDelta<RoleAnalysisSessionType> delta = PrismContext.get().deltaFor(RoleAnalysisSessionType.class)
                    .item(RoleAnalysisSessionType.F_METADATA, F_MODIFY_TIMESTAMP).replace(getCurrentXMLGregorianCalendar())
                    .asObjectDelta(sessionObject.getOid());

            modelService.executeChanges(singleton(delta), null, task, result);

            recomputeSessionStatic(modelService, sessionObject.getOid(), cluster, task, result);

        } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException | PolicyViolationException | SecurityViolationException e) {
            LOGGER.error("Couldn't recompute RoleAnalysisSessionStatistic {}", sessionObject.getOid(), e);
        }

    }

    /**
     * Recomputes the statistics of a RoleAnalysisSessionType object.
     * @param modelService The ModelService for accessing role analysis data.
     * @param sessionOid The session OID.
     * @param roleAnalysisClusterType The cluster to recompute statistics for.
     * @param task The task associated with this operation.
     * @param result The operation result.
     */
    public static void recomputeSessionStatic(@NotNull ModelService modelService, String sessionOid,
            @NotNull RoleAnalysisClusterType roleAnalysisClusterType,
            Task task, OperationResult result) {
        PrismObject<RoleAnalysisSessionType> sessionTypeObject = getSessionTypeObject(modelService, sessionOid, task, result
        );

        assert sessionTypeObject != null;
        RoleAnalysisSessionType session = sessionTypeObject.asObjectable();

        int deletedClusterMembersCount = roleAnalysisClusterType.getMember().size();
        AnalysisClusterStatisticType clusterStatistics = roleAnalysisClusterType.getClusterStatistics();
        RoleAnalysisSessionStatisticType sessionStatistic = session.getSessionStatistic();

        if (sessionStatistic == null || clusterStatistics == null) {
            LOGGER.error("Couldn't recompute RoleAnalysisSessionStatistic {}. "
                    + "Statistic container is null:{},{}", sessionOid, sessionStatistic, clusterStatistics);
            return;
        }
        Double membershipDensity = clusterStatistics.getMembershipDensity();

        Integer processedObjectCount = sessionStatistic.getProcessedObjectCount();
        Double meanDensity = sessionStatistic.getMeanDensity();
        Integer clusterCount = sessionStatistic.getClusterCount();

        int newClusterCount = clusterCount - 1;

        RoleAnalysisSessionStatisticType recomputeSessionStatistic = new RoleAnalysisSessionStatisticType();

        if (newClusterCount == 0) {
            recomputeSessionStatistic.setMeanDensity(0.0);
            recomputeSessionStatistic.setProcessedObjectCount(0);
        } else {
            double recomputeMeanDensity = ((meanDensity * clusterCount) - (membershipDensity)) / newClusterCount;
            int recomputeProcessedObjectCount = processedObjectCount - deletedClusterMembersCount;
            recomputeSessionStatistic.setMeanDensity(recomputeMeanDensity);
            recomputeSessionStatistic.setProcessedObjectCount(recomputeProcessedObjectCount);
        }
        recomputeSessionStatistic.setClusterCount(newClusterCount);

        try {

            ObjectDelta<RoleAnalysisSessionType> delta = PrismContext.get().deltaFor(RoleAnalysisSessionType.class)
                    .item(RoleAnalysisSessionType.F_SESSION_STATISTIC).replace(recomputeSessionStatistic.asPrismContainerValue())
                    .asObjectDelta(sessionOid);

            modelService.executeChanges(singleton(delta), null, task, result);

        } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException | PolicyViolationException | SecurityViolationException e) {
            LOGGER.error("Couldn't recompute RoleAnalysisSessionStatistic {}", sessionOid, e);
        }

    }
}
