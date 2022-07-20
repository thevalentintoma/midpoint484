/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.focus;

import static com.evolveum.midpoint.repo.sqale.jsonb.JsonbPath.JSONB_TYPE;

import java.sql.Types;

import com.querydsl.core.types.dsl.ArrayPath;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.jsonb.JsonbPath;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainer;
import com.evolveum.midpoint.repo.sqlbase.querydsl.UuidPath;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QFocusIdentity<OR extends MFocus> extends QContainer<MFocusIdentity, OR> {

    private static final long serialVersionUID = -6856661540710930040L;

    /**
     * If `QFocusIdentity.class` is not enough because of generics, try `QFocusIdentity.CLASS`.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static final Class<QFocusIdentity<MFocus>> CLASS =
            (Class) QFocusIdentity.class;

    public static final String TABLE_NAME = "m_focus_identity";

    public static final ColumnMetadata FULL_SOURCE =
            ColumnMetadata.named("fullSource").ofType(Types.BINARY);
    public static final ColumnMetadata SOURCE_RESOURCE_REF_TARGET_OID =
            ColumnMetadata.named("sourceResourceRefTargetOid").ofType(UuidPath.UUID_TYPE);
    public static final ColumnMetadata ITEMS_ORIGINAL =
            ColumnMetadata.named("itemsOriginal").ofType(JSONB_TYPE);
    public static final ColumnMetadata ITEMS_NORMALIZED =
            ColumnMetadata.named("itemsNormalized").ofType(JSONB_TYPE);

    // attributes

    public final ArrayPath<byte[], Byte> fullSource = createByteArray("fullSource", FULL_SOURCE);
    public final UuidPath sourceResourceRefTargetOid =
            createUuid("sourceResourceRefTargetOid", SOURCE_RESOURCE_REF_TARGET_OID);
    public final JsonbPath itemsOriginal = addMetadata(
            add(new JsonbPath(forProperty("itemsOriginal"))), ITEMS_ORIGINAL);
    public final JsonbPath itemsNormalized = addMetadata(
            add(new JsonbPath(forProperty("itemsNormalized"))), ITEMS_NORMALIZED);

    public QFocusIdentity(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QFocusIdentity(String variable, String schema, String table) {
        super(MFocusIdentity.class, variable, schema, table);
    }

    @Override
    public BooleanExpression isOwnedBy(OR ownerRow) {
        return ownerOid.eq(ownerRow.oid);
    }
}
