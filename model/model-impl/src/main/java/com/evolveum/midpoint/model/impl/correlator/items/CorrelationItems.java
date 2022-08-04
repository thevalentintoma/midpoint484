/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.items;

import java.util.*;

import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.correlator.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemCorrelationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemsCorrelatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Collection of correlation items (for given correlation or correlation-like operation.)
 */
class CorrelationItems {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationItems.class);

    @NotNull private final List<CorrelationItem> items;

    private CorrelationItems(@NotNull List<CorrelationItem> items) {
        this.items = items;
        LOGGER.trace("CorrelationItems created:\n{}", items);
    }

    public static @NotNull CorrelationItems create(
            @NotNull CorrelatorContext<ItemsCorrelatorType> correlatorContext,
            @NotNull CorrelationContext correlationContext) throws ConfigurationException {

        List<CorrelationItem> items = new ArrayList<>();
        for (ItemCorrelationType itemBean : correlatorContext.getConfigurationBean().getItem()) {
            items.add(
                    CorrelationItem.create(itemBean, correlatorContext, correlationContext));
        }
        stateCheck(!items.isEmpty(), "No correlation items in %s", correlatorContext);
        return new CorrelationItems(items);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int size() {
        return items.size();
    }

    public Collection<CorrelationItem> getItems() {
        return items;
    }

    ObjectQuery createIdentityQuery(
            @NotNull Class<? extends ObjectType> focusType,
            @Nullable String archetypeOid) throws SchemaException {

        assert !items.isEmpty();

        S_FilterEntry nextStart = PrismContext.get().queryFor(focusType);
        PrismObjectDefinition<?> focusDef =
                MiscUtil.requireNonNull(
                        PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(focusType),
                        () -> "No definition for " + focusType);

        S_FilterExit currentEnd = null;
        for (int i = 0; i < items.size(); i++) {
            CorrelationItem correlationItem = items.get(i);
            currentEnd = correlationItem.addClauseToQueryBuilder(nextStart);
            if (i < items.size() - 1) {
                nextStart = currentEnd.and();
            } else {
                // We shouldn't modify the builder if we are at the end.
                // (The builder API does not mention it, but the state of the objects are modified on each operation.)
            }
        }

        assert currentEnd != null;

        // Finally, we add a condition for archetype (if needed)
        S_FilterExit end =
                archetypeOid != null ?
                        currentEnd.and().item(FocusType.F_ARCHETYPE_REF).ref(archetypeOid) :
                        currentEnd;

        return end.build();
    }
}
