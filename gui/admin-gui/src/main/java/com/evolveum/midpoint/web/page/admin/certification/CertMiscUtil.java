/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.certification;

import java.io.Serial;
import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.cases.api.util.QueryUtils;
import com.evolveum.midpoint.certification.api.OutcomeUtils;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;
import com.evolveum.midpoint.gui.api.model.LoadableModel;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.wicket.chartjs.ChartData;
import com.evolveum.wicket.chartjs.ChartDataset;
import com.evolveum.wicket.chartjs.DoughnutChartConfiguration;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.gui.api.page.PageBase;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NO_RESPONSE;

import static java.util.Collections.singleton;

public class CertMiscUtil {

    private static final Trace LOGGER = TraceManager.getTrace(CertMiscUtil.class);
    private static final String OPERATION_LOAD_CAMPAIGNS_OIDS = "loadCampaignsOids";

    public static String getStopReviewOnText(List<AccessCertificationResponseType> stopOn, PageBase page) {
        if (stopOn == null) {
            return page.getString("PageCertDefinition.stopReviewOnDefault");
        } else if (stopOn.isEmpty()) {
            return page.getString("PageCertDefinition.stopReviewOnNone");
        } else {
            List<String> names = new ArrayList<>(stopOn.size());
            for (AccessCertificationResponseType r : stopOn) {
                names.add(page.createStringResource(r).getString());
            }
            return StringUtils.join(names, ", ");
        }
    }

    public static LoadableModel<List<ProgressBar>> createCampaignProgressBarModel(AccessCertificationCampaignType campaign,
            MidPointPrincipal principal) {
        return new LoadableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<ProgressBar> load() {
                float casesCount = campaign.getCase() != null ? campaign.getCase().size() : 0;
                if (casesCount == 0) {
                    ProgressBar allCasesProgressBar = new ProgressBar(casesCount, ProgressBar.State.SECONDARY);
                    return Collections.singletonList(allCasesProgressBar);
                }
                float completed;
                if (principal != null) {
                    completed = CertCampaignTypeUtil.getCasesCompletedPercentageCurrStageCurrIterationByReviewer(campaign, principal.getOid());
                } else {
                    completed = CertCampaignTypeUtil.getCasesCompletedPercentageCurrStageCurrIteration(campaign);
                }

                ProgressBar completedProgressBar = new ProgressBar(completed, ProgressBar.State.INFO);
                return Collections.singletonList(completedProgressBar);
            }
        };
    }

    public static AccessCertificationResponseType getStageOutcome(AccessCertificationCaseType aCase, int stageNumber) {
        Set<AccessCertificationResponseType> stageOutcomes = aCase.getEvent().stream()
                .filter(e -> e instanceof StageCompletionEventType && e.getStageNumber() == stageNumber)
                .map(e -> OutcomeUtils.normalizeToNonNull(OutcomeUtils.fromUri(((StageCompletionEventType) e).getOutcome())))
                .collect(Collectors.toSet());
        Collection<AccessCertificationResponseType> nonNullOutcomes = CollectionUtils.subtract(stageOutcomes, singleton(NO_RESPONSE));
        if (!nonNullOutcomes.isEmpty()) {
            return nonNullOutcomes.iterator().next();
        } else if (!stageOutcomes.isEmpty()) {
            return NO_RESPONSE;
        } else {
            return null;
        }
    }

    public static DoughnutChartConfiguration createDoughnutChartConfigForCampaigns(List<String> campaignOids, MidPointPrincipal principal,
            PageBase pageBase) {
        DoughnutChartConfiguration config = new DoughnutChartConfiguration();

        ChartData chartData = new ChartData();
        chartData.addDataset(createDataSet(campaignOids, principal, pageBase));

        config.setData(chartData);
        return config;
    }

    private static ChartDataset createDataSet(List<String> campaignOids, MidPointPrincipal principal, PageBase pageBase) {
        ChartDataset dataset = new ChartDataset();
//        dataset.setLabel("Not decided");

        dataset.setFill(true);

        long notDecidedCertItemsCount = countOpenCertItems(campaignOids, principal, true, pageBase);
        long allOpenCertItemsCount = countOpenCertItems(campaignOids, principal, false, pageBase);
        long decidedCertItemsCount = allOpenCertItemsCount - notDecidedCertItemsCount;

        dataset.addData(decidedCertItemsCount);
        dataset.addBackgroudColor("blue");

        dataset.addData(notDecidedCertItemsCount);
        dataset.addBackgroudColor("grey");

        return dataset;
    }

    public static long countOpenCertItems(List<String> campaignOids, MidPointPrincipal principal, boolean notDecidedOnly,
            PageBase pageBase) {
        long count = 0;

        Task task = pageBase.createSimpleTask("countCertificationWorkItems");
        OperationResult result = task.getResult();
        try {
            ObjectQuery query = QueryUtils.createQueryForOpenWorkItemsForCampaigns(campaignOids, principal, notDecidedOnly);
            count = pageBase.getModelService()
                    .countContainers(AccessCertificationWorkItemType.class, query, null, task, result);
        } catch (Exception ex) {
            LOGGER.error("Couldn't count certification work items", ex);
            pageBase.showResult(result);
        }
        return count;
    }

    public static List<String> getActiveCampaignsOids(boolean onlyForLoggedInUser, PageBase pageBase) {
        OperationResult result = new OperationResult(OPERATION_LOAD_CAMPAIGNS_OIDS);
        ObjectQuery campaignsQuery;
        if (onlyForLoggedInUser) {
             campaignsQuery = getPrincipalActiveCampaignsQuery(pageBase);
        } else {
            campaignsQuery = getAllActiveCampaignsQuery(pageBase);
        }
        List<PrismObject<AccessCertificationCampaignType>> campaigns = WebModelServiceUtils.searchObjects(
                AccessCertificationCampaignType.class, campaignsQuery, null, result, pageBase);
        return campaigns.stream().map(PrismObject::getOid).toList();
    }

    public static ObjectQuery getPrincipalActiveCampaignsQuery(PageBase pageBase) {
        FocusType principal = pageBase.getPrincipalFocus();

        return pageBase.getPrismContext().queryFor(AccessCertificationCampaignType.class)
                .item(AccessCertificationCampaignType.F_CASE, AccessCertificationCaseType.F_WORK_ITEM,
                        AccessCertificationWorkItemType.F_ASSIGNEE_REF)
                .ref(principal.getOid())
                .and()
                .item(AccessCertificationCampaignType.F_CASE, AccessCertificationCaseType.F_WORK_ITEM,
                        AccessCertificationWorkItemType.F_CLOSE_TIMESTAMP)
                .isNull()
                .build();
    }

    public static ObjectQuery getAllActiveCampaignsQuery(PageBase pageBase) {
        return pageBase.getPrismContext().queryFor(AccessCertificationCampaignType.class)
                .item(AccessCertificationCampaignType.F_CASE, AccessCertificationCaseType.F_WORK_ITEM,
                        AccessCertificationWorkItemType.F_CLOSE_TIMESTAMP)
                .isNull()
                .build();
    }

    public static LoadableModel<String> getCampaignStageLoadableModel(@NotNull AccessCertificationCampaignType campaign) {
        return new LoadableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                AccessCertificationStageType stage = CertCampaignTypeUtil.getCurrentStage(campaign);
                int stageNumber = stage != null ? stage.getNumber() : 0;
                int numberOfStages = CertCampaignTypeUtil.getNumberOfStages(campaign);
                return stageNumber + "/" + numberOfStages;
            }
        };
    }

    public static LoadableModel<String> getCampaignIterationLoadableModel(@NotNull AccessCertificationCampaignType campaign) {
        return new LoadableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                return "" + CertCampaignTypeUtil.norm(campaign.getIteration());
            }
        };
    }

}
