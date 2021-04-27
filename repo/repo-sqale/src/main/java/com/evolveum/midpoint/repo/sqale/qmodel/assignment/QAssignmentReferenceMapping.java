/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.assignment;

import java.util.function.BiFunction;

import com.querydsl.core.types.Predicate;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReference;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReferenceMapping;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Mapping between {@link QObjectReference} and {@link ObjectReferenceType}.
 * The mapping is the same for all subtypes, see different `INSTANCE_*` constants below.
 *
 * @param <AOR> type of the row (M-bean) of the assignment owner
 */
public class QAssignmentReferenceMapping<AOR extends MObject>
        extends QReferenceMapping<QAssignmentReference, MAssignmentReference, QAssignment<AOR>, MAssignment> {

    public static final QAssignmentReferenceMapping<?> INSTANCE_ASSIGNMENT_CREATE_APPROVER =
            new QAssignmentReferenceMapping<>("m_assignment_ref_create_approver", "arefca");
    public static final QAssignmentReferenceMapping<?> INSTANCE_ASSIGNMENT_MODIFY_APPROVER =
            new QAssignmentReferenceMapping<>("m_assignment_ref_create_approver", "arefma");

    private QAssignmentReferenceMapping(String tableName, String defaultAliasName) {
        super(tableName, defaultAliasName, QAssignmentReference.class);

        // assignmentCid probably can't be mapped directly
    }

    @Override
    protected QAssignmentReference newAliasInstance(String alias) {
        return new QAssignmentReference(alias, tableName());
    }

    @Override
    public MAssignmentReference newRowObject(MAssignment ownerRow) {
        MAssignmentReference row = new MAssignmentReference();
        row.ownerOid = ownerRow.ownerOid;
        row.assignmentCid = ownerRow.cid;
        return row;
    }

    @Override
    public BiFunction<QAssignment<AOR>, QAssignmentReference, Predicate> joinOnPredicate() {
        return (a, r) -> a.ownerOid.eq(r.ownerOid).and(a.cid.eq(r.assignmentCid));
    }
}
