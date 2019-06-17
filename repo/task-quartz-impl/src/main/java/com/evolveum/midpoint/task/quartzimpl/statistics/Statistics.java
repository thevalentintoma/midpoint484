/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.task.quartzimpl.statistics;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.perf.PerformanceInformation;
import com.evolveum.midpoint.repo.api.perf.PerformanceMonitor;
import com.evolveum.midpoint.schema.statistics.RepositoryPerformanceInformationUtil;
import com.evolveum.midpoint.schema.statistics.RepositoryPerformanceInformationUtil;
import com.evolveum.midpoint.schema.statistics.*;
import com.evolveum.midpoint.schema.statistics.CachePerformanceInformationUtil;
import com.evolveum.midpoint.schema.statistics.CachePerformanceInformationUtil;
import com.evolveum.midpoint.schema.statistics.MethodsPerformanceInformationUtil;
import com.evolveum.midpoint.util.aspect.MethodsPerformanceInformation;
import com.evolveum.midpoint.util.aspect.MethodsPerformanceMonitor;
import com.evolveum.midpoint.util.caching.CachePerformanceCollector;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.evolveum.midpoint.prism.xml.XmlTypeConverter.createXMLGregorianCalendar;
import static java.util.Collections.emptySet;

/**
 *  Code to manage operational statistics. Originally it was a part of the TaskQuartzImpl
 *  but it is cleaner to keep it separate.
 *
 *  It is used for
 *  1) running background tasks (RunningTask) - both heavyweight and lightweight
 *  2) transient tasks e.g. those invoked from GUI
 */
public class Statistics implements WorkBucketStatisticsCollector {

	private static final Trace LOGGER = TraceManager.getTrace(Statistics.class);
	private static final Trace PERFORMANCE_ADVISOR = TraceManager.getPerformanceAdvisorTrace();

	@NotNull private final PrismContext prismContext;

	public Statistics(@NotNull PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	private EnvironmentalPerformanceInformation environmentalPerformanceInformation = new EnvironmentalPerformanceInformation();
	private SynchronizationInformation synchronizationInformation;                // has to be explicitly enabled
	private IterativeTaskInformation iterativeTaskInformation;                    // has to be explicitly enabled
	private ActionsExecutedInformation actionsExecutedInformation;            // has to be explicitly enabled

	/**
	 * This data structure is synchronized explicitly. Because it is updated infrequently, it should be sufficient.
	 */
	private WorkBucketManagementPerformanceInformationType workBucketManagementPerformanceInformation;

	private final Object BUCKET_INFORMATION_LOCK = new Object();

	/**
	 * Most current version of repository performance information. Original (live) form of this information is accessible only
	 * from the task thread itself. So we have to refresh this item periodically from the task thread.
	 *
	 * DO NOT modify the content of this structure from multiple threads. The task thread should only replace the whole structure,
	 * while other threads should only read it.
	 */
	private volatile RepositoryPerformanceInformationType repositoryPerformanceInformation;
	private volatile RepositoryPerformanceInformationType initialRepositoryPerformanceInformation;

	/**
	 * Most current version of cache performance information. Original (live) form of this information is accessible only
	 * from the task thread itself. So we have to refresh this item periodically from the task thread.
	 *
	 * DO NOT modify the content of this structure from multiple threads. The task thread should only replace the whole structure,
	 * while other threads should only read it.
	 */
	private volatile CachesPerformanceInformationType cachesPerformanceInformation;
	private volatile CachesPerformanceInformationType initialCachesPerformanceInformation;

	/**
	 * Most current version of methods performance information. Original (live) form of this information is accessible only
	 * from the task thread itself. So we have to refresh this item periodically from the task thread.
	 *
	 * DO NOT modify the content of this structure from multiple threads. The task thread should only replace the whole structure,
	 * while other threads should only read it.
	 */
	private volatile MethodsPerformanceInformationType methodsPerformanceInformation;
	private volatile MethodsPerformanceInformationType initialMethodsPerformanceInformation;

	private EnvironmentalPerformanceInformation getEnvironmentalPerformanceInformation() {
		return environmentalPerformanceInformation;
	}

	private SynchronizationInformation getSynchronizationInformation() {
		return synchronizationInformation;
	}

	private IterativeTaskInformation getIterativeTaskInformation() {
		return iterativeTaskInformation;
	}

	private ActionsExecutedInformation getActionsExecutedInformation() {
		return actionsExecutedInformation;
	}

	@NotNull
	public List<String> getLastFailures() {
		return iterativeTaskInformation != null ? iterativeTaskInformation.getLastFailures() : Collections.emptyList();
	}

	private EnvironmentalPerformanceInformationType getAggregateEnvironmentalPerformanceInformation(Collection<Statistics> children) {
		if (environmentalPerformanceInformation == null) {
			return null;
		}
		EnvironmentalPerformanceInformationType rv = new EnvironmentalPerformanceInformationType();
		EnvironmentalPerformanceInformation.addTo(rv, environmentalPerformanceInformation.getAggregatedValue());
		for (Statistics child : children) {
			EnvironmentalPerformanceInformation info = child.getEnvironmentalPerformanceInformation();
			if (info != null) {
				EnvironmentalPerformanceInformation.addTo(rv, info.getAggregatedValue());
			}
		}
		return rv;
	}

	private IterativeTaskInformationType getAggregateIterativeTaskInformation(Collection<Statistics> children) {
		if (iterativeTaskInformation == null) {
			return null;
		}
		IterativeTaskInformationType rv = new IterativeTaskInformationType();
		IterativeTaskInformation.addTo(rv, iterativeTaskInformation.getAggregatedValue(), false);
		for (Statistics child : children) {
			IterativeTaskInformation info = child.getIterativeTaskInformation();
			if (info != null) {
				IterativeTaskInformation.addTo(rv, info.getAggregatedValue(), false);
			}
		}
		return rv;
	}

	private SynchronizationInformationType getAggregateSynchronizationInformation(Collection<Statistics> children) {
		if (synchronizationInformation == null) {
			return null;
		}
		SynchronizationInformationType rv = new SynchronizationInformationType();
		SynchronizationInformation.addTo(rv, synchronizationInformation.getAggregatedValue());
		for (Statistics child : children) {
			SynchronizationInformation info = child.getSynchronizationInformation();
			if (info != null) {
				SynchronizationInformation.addTo(rv, info.getAggregatedValue());
			}
		}
		return rv;
	}

	private ActionsExecutedInformationType getAggregateActionsExecutedInformation(Collection<Statistics> children) {
		if (actionsExecutedInformation == null) {
			return null;
		}
		ActionsExecutedInformationType rv = new ActionsExecutedInformationType();
		ActionsExecutedInformation.addTo(rv, actionsExecutedInformation.getAggregatedValue());
		for (Statistics child : children) {
			ActionsExecutedInformation info = child.getActionsExecutedInformation();
			if (info != null) {
				ActionsExecutedInformation.addTo(rv, info.getAggregatedValue());
			}
		}
		return rv;
	}

	private RepositoryPerformanceInformationType getAggregateRepositoryPerformanceInformation(Collection<Statistics> children) {
		if (repositoryPerformanceInformation == null) {
			return null;
		}
		RepositoryPerformanceInformationType rv = repositoryPerformanceInformation.clone();
		RepositoryPerformanceInformationUtil.addTo(rv, initialRepositoryPerformanceInformation);
		for (Statistics child : children) {
			RepositoryPerformanceInformationUtil.addTo(rv, child.getAggregateRepositoryPerformanceInformation(emptySet()));
		}
		return rv;
	}

	private CachesPerformanceInformationType getAggregateCachesPerformanceInformation(Collection<Statistics> children) {
		if (cachesPerformanceInformation == null) {
			return null;
		}
		CachesPerformanceInformationType rv = cachesPerformanceInformation.clone();
		CachePerformanceInformationUtil.addTo(rv, initialCachesPerformanceInformation);
		for (Statistics child : children) {
			CachePerformanceInformationUtil.addTo(rv, child.getAggregateCachesPerformanceInformation(emptySet()));
		}
		return rv;
	}

	private MethodsPerformanceInformationType getAggregateMethodsPerformanceInformation(Collection<Statistics> children) {
		if (methodsPerformanceInformation == null) {
			return null;
		}
		MethodsPerformanceInformationType rv = methodsPerformanceInformation.clone();
		MethodsPerformanceInformationUtil.addTo(rv, initialMethodsPerformanceInformation);
		for (Statistics child : children) {
			MethodsPerformanceInformationUtil.addTo(rv, child.getAggregateMethodsPerformanceInformation(emptySet()));
		}
		return rv;
	}

	private WorkBucketManagementPerformanceInformationType getWorkBucketManagementPerformanceInformation() {
		synchronized (BUCKET_INFORMATION_LOCK) {
			return workBucketManagementPerformanceInformation != null ? workBucketManagementPerformanceInformation.clone() : null;
		}
	}


	public OperationStatsType getAggregatedLiveOperationStats(Collection<Statistics> children) {
		EnvironmentalPerformanceInformationType env = getAggregateEnvironmentalPerformanceInformation(children);
		IterativeTaskInformationType itit = getAggregateIterativeTaskInformation(children);
		SynchronizationInformationType sit = getAggregateSynchronizationInformation(children);
		ActionsExecutedInformationType aeit = getAggregateActionsExecutedInformation(children);
		RepositoryPerformanceInformationType repo = getAggregateRepositoryPerformanceInformation(children);
		CachesPerformanceInformationType caches = getAggregateCachesPerformanceInformation(children);
		MethodsPerformanceInformationType methods = getAggregateMethodsPerformanceInformation(children);
		WorkBucketManagementPerformanceInformationType buckets = getWorkBucketManagementPerformanceInformation();   // this is not fetched from children (present on coordinator task only)
		if (env == null && itit == null && sit == null && aeit == null && repo == null && caches == null && methods == null && buckets == null) {
			return null;
		}
		OperationStatsType rv = new OperationStatsType();
		rv.setEnvironmentalPerformanceInformation(env);
		rv.setIterativeTaskInformation(itit);
		rv.setSynchronizationInformation(sit);
		rv.setActionsExecutedInformation(aeit);
		rv.setRepositoryPerformanceInformation(repo);
		rv.setCachesPerformanceInformation(caches);
		rv.setMethodsPerformanceInformation(methods);
		rv.setWorkBucketManagementPerformanceInformation(buckets);
		rv.setTimestamp(createXMLGregorianCalendar(new Date()));
		return rv;
	}

	public void recordState(String message) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("{}", message);
		}
		if (PERFORMANCE_ADVISOR.isDebugEnabled()) {
			PERFORMANCE_ADVISOR.debug("{}", message);
		}
		environmentalPerformanceInformation.recordState(message);
	}

	public void recordProvisioningOperation(String resourceOid, String resourceName, QName objectClassName,
			ProvisioningOperation operation, boolean success, int count, long duration) {
		environmentalPerformanceInformation
				.recordProvisioningOperation(resourceOid, resourceName, objectClassName, operation, success, count, duration);
	}

	public void recordNotificationOperation(String transportName, boolean success, long duration) {
		environmentalPerformanceInformation.recordNotificationOperation(transportName, success, duration);
	}

	public void recordMappingOperation(String objectOid, String objectName, String objectTypeName, String mappingName,
			long duration) {
		environmentalPerformanceInformation.recordMappingOperation(objectOid, objectName, objectTypeName, mappingName, duration);
	}

	public synchronized void recordSynchronizationOperationEnd(String objectName, String objectDisplayName, QName objectType,
			String objectOid,
			long started, Throwable exception, SynchronizationInformation.Record originalStateIncrement,
			SynchronizationInformation.Record newStateIncrement) {
		if (synchronizationInformation != null) {
			synchronizationInformation
					.recordSynchronizationOperationEnd(objectName, objectDisplayName, objectType, objectOid, started, exception,
							originalStateIncrement, newStateIncrement);
		}
	}

	public synchronized void recordSynchronizationOperationStart(String objectName, String objectDisplayName, QName objectType,
			String objectOid) {
		if (synchronizationInformation != null) {
			synchronizationInformation.recordSynchronizationOperationStart(objectName, objectDisplayName, objectType, objectOid);
		}
	}

	public synchronized void recordIterativeOperationEnd(String objectName, String objectDisplayName, QName objectType,
			String objectOid, long started, Throwable exception) {
		if (iterativeTaskInformation != null) {
			iterativeTaskInformation.recordOperationEnd(objectName, objectDisplayName, objectType, objectOid, started, exception);
		}
	}

	public void recordIterativeOperationEnd(ShadowType shadow, long started, Throwable exception) {
		recordIterativeOperationEnd(PolyString.getOrig(shadow.getName()), StatisticsUtil.getDisplayName(shadow),
				ShadowType.COMPLEX_TYPE, shadow.getOid(), started, exception);
	}

	public void recordIterativeOperationStart(ShadowType shadow) {
		recordIterativeOperationStart(PolyString.getOrig(shadow.getName()), StatisticsUtil.getDisplayName(shadow),
				ShadowType.COMPLEX_TYPE, shadow.getOid());
	}

	public synchronized void recordIterativeOperationStart(String objectName, String objectDisplayName, QName objectType,
			String objectOid) {
		if (iterativeTaskInformation != null) {
			iterativeTaskInformation.recordOperationStart(objectName, objectDisplayName, objectType, objectOid);
		}
	}

	public void recordObjectActionExecuted(String objectName, String objectDisplayName, QName objectType, String objectOid,
			ChangeType changeType, String channel, Throwable exception) {
		if (actionsExecutedInformation != null) {
			actionsExecutedInformation
					.recordObjectActionExecuted(objectName, objectDisplayName, objectType, objectOid, changeType, channel,
							exception);
		}
	}

	public void recordObjectActionExecuted(PrismObject<? extends ObjectType> object, ChangeType changeType, String channel, Throwable exception) {
		recordObjectActionExecuted(object, null, null, changeType, channel, exception);
	}

	public <T extends ObjectType> void recordObjectActionExecuted(PrismObject<T> object, Class<T> objectTypeClass,
			String defaultOid, ChangeType changeType, String channel, Throwable exception) {
		if (actionsExecutedInformation != null) {
			String name, displayName, oid;
			PrismObjectDefinition definition;
			Class<T> clazz;
			if (object != null) {
				name = PolyString.getOrig(object.getName());
				displayName = StatisticsUtil.getDisplayName(object);
				definition = object.getDefinition();
				clazz = object.getCompileTimeClass();
				oid = object.getOid();
				if (oid == null) {        // in case of ADD operation
					oid = defaultOid;
				}
			} else {
				name = null;
				displayName = null;
				definition = null;
				clazz = objectTypeClass;
				oid = defaultOid;
			}
			if (definition == null && clazz != null) {
				definition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(clazz);
			}
			QName typeQName;
			if (definition != null) {
				typeQName = definition.getTypeName();
			} else {
				typeQName = ObjectType.COMPLEX_TYPE;
			}
			actionsExecutedInformation
					.recordObjectActionExecuted(name, displayName, typeQName, oid, changeType, channel, exception);
		}
	}

	public void markObjectActionExecutedBoundary() {
		if (actionsExecutedInformation != null) {
			actionsExecutedInformation.markObjectActionExecutedBoundary();
		}
	}

	public void resetEnvironmentalPerformanceInformation(EnvironmentalPerformanceInformationType value) {
		environmentalPerformanceInformation = new EnvironmentalPerformanceInformation(value);
	}

	public void resetSynchronizationInformation(SynchronizationInformationType value) {
		synchronizationInformation = new SynchronizationInformation(value);
	}

	public void resetIterativeTaskInformation(IterativeTaskInformationType value) {
		iterativeTaskInformation = new IterativeTaskInformation(value);
	}

	public void resetActionsExecutedInformation(ActionsExecutedInformationType value) {
		actionsExecutedInformation = new ActionsExecutedInformation(value);
	}

	private void resetWorkBucketManagementPerformanceInformation(WorkBucketManagementPerformanceInformationType value) {
		synchronized (BUCKET_INFORMATION_LOCK) {
			workBucketManagementPerformanceInformation = value != null ? value.clone() : new WorkBucketManagementPerformanceInformationType();
		}
	}

	public void startCollectingOperationStatsFromZero(boolean enableIterationStatistics, boolean enableSynchronizationStatistics,
			boolean enableActionsExecutedStatistics, PerformanceMonitor performanceMonitor) {
		resetEnvironmentalPerformanceInformation(null);
		if (enableIterationStatistics) {
			resetIterativeTaskInformation(null);
		}
		if (enableSynchronizationStatistics) {
			resetSynchronizationInformation(null);
		}
		if (enableActionsExecutedStatistics) {
			resetActionsExecutedInformation(null);
		}
		resetWorkBucketManagementPerformanceInformation(null);
		initialRepositoryPerformanceInformation = null;
		initialCachesPerformanceInformation = null;
		initialMethodsPerformanceInformation = null;
		startCollectingRepoAndCacheStats(performanceMonitor);
	}

	public void startCollectingOperationStatsFromStoredValues(OperationStatsType stored, boolean enableIterationStatistics,
			boolean enableSynchronizationStatistics, boolean enableActionsExecutedStatistics, PerformanceMonitor performanceMonitor) {
		OperationStatsType initial = stored != null ? stored : new OperationStatsType();
		resetEnvironmentalPerformanceInformation(initial.getEnvironmentalPerformanceInformation());
		if (enableIterationStatistics) {
			resetIterativeTaskInformation(initial.getIterativeTaskInformation());
		} else {
			iterativeTaskInformation = null;
		}
		if (enableSynchronizationStatistics) {
			resetSynchronizationInformation(initial.getSynchronizationInformation());
		} else {
			synchronizationInformation = null;
		}
		if (enableActionsExecutedStatistics) {
			resetActionsExecutedInformation(initial.getActionsExecutedInformation());
		} else {
			actionsExecutedInformation = null;
		}
		resetWorkBucketManagementPerformanceInformation(initial.getWorkBucketManagementPerformanceInformation());
		initialRepositoryPerformanceInformation = initial.getRepositoryPerformanceInformation();
		initialCachesPerformanceInformation = initial.getCachesPerformanceInformation();
		initialMethodsPerformanceInformation = initial.getMethodsPerformanceInformation();
		startCollectingRepoAndCacheStats(performanceMonitor);
	}

	/**
	 * Cheap operation so we can (and should) invoke it frequently.
	 * But ALWAYS call it from the thread that executes the task in question; otherwise we get wrong data there.
	 */
	public void refreshStoredPerformanceStats(RepositoryService repositoryService) {
		refreshStoredRepositoryPerformanceInformation(repositoryService);
		refreshStoredCachePerformanceInformation();
		refreshStoredMethodsPerformanceInformation();
	}

	public void setInitialPerformanceStats(OperationStatsType operationStats) {
		initialRepositoryPerformanceInformation = operationStats != null ? operationStats.getRepositoryPerformanceInformation() : null;
		initialCachesPerformanceInformation = operationStats != null ? operationStats.getCachesPerformanceInformation() : null;
		initialMethodsPerformanceInformation = operationStats != null ? operationStats.getMethodsPerformanceInformation() : null;
	}

	private void refreshStoredRepositoryPerformanceInformation(RepositoryService repositoryService) {
		PerformanceInformation performanceInformation = repositoryService.getPerformanceMonitor().getThreadLocalPerformanceInformation();
		if (performanceInformation != null) {
			repositoryPerformanceInformation = performanceInformation.toRepositoryPerformanceInformationType();
		} else {
			repositoryPerformanceInformation = null;       // probably we are not collecting these
		}
	}

	private void refreshStoredMethodsPerformanceInformation() {
		MethodsPerformanceInformation performanceInformation = MethodsPerformanceMonitor.INSTANCE.getThreadLocalPerformanceInformation();
		if (performanceInformation != null) {
			methodsPerformanceInformation = MethodsPerformanceInformationUtil.toMethodsPerformanceInformationType(performanceInformation);
		} else {
			methodsPerformanceInformation = null;       // probably we are not collecting these
		}
	}

	private void refreshStoredCachePerformanceInformation() {
		Map<String, CachePerformanceCollector.CacheData> performanceMap = CachePerformanceCollector.INSTANCE
				.getThreadLocalPerformanceMap();
		if (performanceMap != null) {
			cachesPerformanceInformation = CachePerformanceInformationUtil.toCachesPerformanceInformationType(performanceMap);
		} else {
			cachesPerformanceInformation = null;
		}
	}

	@SuppressWarnings("Duplicates")
	@Override
	public void register(String situation, long totalTime, int conflictCount, long conflictWastedTime, int bucketWaitCount, long bucketWaitTime, int bucketsReclaimed) {
		synchronized (BUCKET_INFORMATION_LOCK) {
			WorkBucketManagementOperationPerformanceInformationType operation = null;
			for (WorkBucketManagementOperationPerformanceInformationType op : workBucketManagementPerformanceInformation.getOperation()) {
				if (op.getName().equals(situation)) {
					operation = op;
					break;
				}
			}
			if (operation == null) {
				operation = new WorkBucketManagementOperationPerformanceInformationType();
				operation.setName(situation);
				workBucketManagementPerformanceInformation.getOperation().add(operation);
			}
			operation.setCount(or0(operation.getCount()) + 1);
			addTime(operation, totalTime, WorkBucketManagementOperationPerformanceInformationType::getTotalTime,
					WorkBucketManagementOperationPerformanceInformationType::getMinTime,
					WorkBucketManagementOperationPerformanceInformationType::getMaxTime,
					WorkBucketManagementOperationPerformanceInformationType::setTotalTime,
					WorkBucketManagementOperationPerformanceInformationType::setMinTime,
					WorkBucketManagementOperationPerformanceInformationType::setMaxTime);
			if (conflictCount > 0 || conflictWastedTime > 0) {
				operation.setConflictCount(or0(operation.getConflictCount()) + conflictCount);
				addTime(operation, conflictWastedTime,
						WorkBucketManagementOperationPerformanceInformationType::getTotalWastedTime,
						WorkBucketManagementOperationPerformanceInformationType::getMinWastedTime,
						WorkBucketManagementOperationPerformanceInformationType::getMaxWastedTime,
						WorkBucketManagementOperationPerformanceInformationType::setTotalWastedTime,
						WorkBucketManagementOperationPerformanceInformationType::setMinWastedTime,
						WorkBucketManagementOperationPerformanceInformationType::setMaxWastedTime);
			}
			if (bucketWaitCount > 0 || bucketsReclaimed > 0 || bucketWaitTime > 0) {
				operation.setBucketWaitCount(or0(operation.getBucketWaitCount()) + bucketWaitCount);
				operation.setBucketsReclaimed(or0(operation.getBucketsReclaimed()) + bucketsReclaimed);
				addTime(operation, bucketWaitTime, WorkBucketManagementOperationPerformanceInformationType::getTotalWaitTime,
						WorkBucketManagementOperationPerformanceInformationType::getMinWaitTime,
						WorkBucketManagementOperationPerformanceInformationType::getMaxWaitTime,
						WorkBucketManagementOperationPerformanceInformationType::setTotalWaitTime,
						WorkBucketManagementOperationPerformanceInformationType::setMinWaitTime,
						WorkBucketManagementOperationPerformanceInformationType::setMaxWaitTime);
			}
		}
	}

	private void addTime(WorkBucketManagementOperationPerformanceInformationType operation,
			long time, Function<WorkBucketManagementOperationPerformanceInformationType, Long> getterTotal,
			Function<WorkBucketManagementOperationPerformanceInformationType, Long> getterMin,
			Function<WorkBucketManagementOperationPerformanceInformationType, Long> getterMax,
			BiConsumer<WorkBucketManagementOperationPerformanceInformationType, Long> setterTotal,
			BiConsumer<WorkBucketManagementOperationPerformanceInformationType, Long> setterMin,
			BiConsumer<WorkBucketManagementOperationPerformanceInformationType, Long>  setterMax) {
		setterTotal.accept(operation, or0(getterTotal.apply(operation)) + time);
		Long min = getterMin.apply(operation);
		if (min == null || time < min) {
			setterMin.accept(operation, time);
		}
		Long max = getterMax.apply(operation);
		if (max == null || time > max) {
			setterMax.accept(operation, time);
		}
	}

	private int or0(Integer n) {
		return n != null ? n : 0;
	}

	private long or0(Long n) {
		return n != null ? n : 0;
	}

	private void startCollectingRepoAndCacheStats(PerformanceMonitor performanceMonitor) {
		performanceMonitor.startThreadLocalPerformanceInformationCollection();
		CachePerformanceCollector.INSTANCE.startThreadLocalPerformanceInformationCollection();
		MethodsPerformanceMonitor.INSTANCE.startThreadLocalPerformanceInformationCollection();
	}
}
