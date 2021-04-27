/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.object;

import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;

/**
 * Mapping between {@link QTrigger} and {@link TriggerType}.
 *
 * @param <OR> type of the owner row
 */
public class QTriggerMapping<OR extends MObject>
        extends QContainerMapping<TriggerType, QTrigger<OR>, MTrigger, OR> {

    public static final String DEFAULT_ALIAS_NAME = "trg";

    public static final QTriggerMapping<MObject> INSTANCE = new QTriggerMapping<>();

    // We can't declare Class<QTrigger<OR>>.class, so we cheat a bit.
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private QTriggerMapping() {
        super(QTrigger.TABLE_NAME, DEFAULT_ALIAS_NAME,
                TriggerType.class, (Class) QTrigger.class);

        addItemMapping(TriggerType.F_HANDLER_URI, uriMapper(q -> q.handlerUriId));
        addItemMapping(TriggerType.F_TIMESTAMP, timestampMapper(q -> q.timestampValue));
    }

    @Override
    protected QTrigger<OR> newAliasInstance(String alias) {
        return new QTrigger<>(alias);
    }

    @Override
    public TriggerSqlTransformer<OR> createTransformer(SqlTransformerSupport transformerSupport) {
        return new TriggerSqlTransformer<>(transformerSupport, this);
    }

    @Override
    public MTrigger newRowObject() {
        return new MTrigger();
    }
}
