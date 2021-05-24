/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.ref;

/**
 * Enumeration of various types of reference entities (subtypes of {@link QReference}).
 *
 * * Order of values is irrelevant.
 * * Constant names must match the custom enum type ReferenceType in the database schema.
 */
public enum MReferenceType {

    // OBJECT REFERENCES
    ARCHETYPE,
    DELEGATED,
    INCLUDE,
    PROJECTION,
    OBJECT_CREATE_APPROVER,
    OBJECT_MODIFY_APPROVER,
    OBJECT_PARENT_ORG,
    PERSONA,
    RESOURCE_BUSINESS_CONFIGURATION_APPROVER,
    ROLE_MEMBERSHIP,

    // OTHER REFERENCES
    ASSIGNMENT_CREATE_APPROVER,
    ASSIGNMENT_MODIFY_APPROVER,
    CASE_WI_ASSIGNEE,
    CASE_WI_CANDIDATE,
}
