/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.container;

import java.util.Objects;
import javax.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Persister;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROperationResultStatus;
import com.evolveum.midpoint.repo.sql.data.common.id.RContainerId;
import com.evolveum.midpoint.repo.sql.query.definition.IdQueryProperty;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.query.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.query.definition.OwnerIdGetter;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointSingleTablePersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationExecutionType;

@JaxbType(type = OperationExecutionType.class)
@Entity
@IdClass(RContainerId.class)
@Table(name = "m_operation_execution", indexes = {
        @Index(name = "iOpExecTaskOid", columnList = "taskRef_targetOid"),
        @Index(name = "iOpExecInitiatorOid", columnList = "initiatorRef_targetOid"),
        @Index(name = "iOpExecStatus", columnList = "status"),
        @Index(name = "iOpExecStatus", columnList = "status"),
        @Index(name = "iOpExecOwnerOid", columnList = "owner_oid") })
@Persister(impl = MidPointSingleTablePersister.class)
public class ROperationExecution implements Container<RObject> {

    private Boolean trans;

    private RObject owner;
    private String ownerOid;
    private Integer id;

    private REmbeddedReference initiatorRef;
    private REmbeddedReference taskRef;
    private ROperationResultStatus status;
    private XMLGregorianCalendar timestamp;

    public ROperationExecution() {
        this(null);
    }

    public ROperationExecution(RObject owner) {
        this.setOwner(owner);
    }

    @Id
    @JoinColumn(foreignKey = @ForeignKey(name = "fk_op_exec_owner"))
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @NotQueryable
    @Override
    public RObject getOwner() {
        return owner;
    }

    @Override
    public void setOwner(RObject owner) {
        this.owner = owner;
    }

    @Column(name = "owner_oid", length = RUtil.COLUMN_LENGTH_OID, nullable = false)
    @OwnerIdGetter()
    @Override
    public String getOwnerOid() {
        if (owner != null && ownerOid == null) {
            ownerOid = owner.getOid();
        }
        return ownerOid;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    @Id
    @GeneratedValue(generator = "ContainerIdGenerator")
    @GenericGenerator(name = "ContainerIdGenerator", strategy = "com.evolveum.midpoint.repo.sql.util.ContainerIdGenerator")
    @Column(name = "id")
    @IdQueryProperty
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Embedded
    public REmbeddedReference getInitiatorRef() {
        return initiatorRef;
    }

    public void setInitiatorRef(REmbeddedReference initiatorRef) {
        this.initiatorRef = initiatorRef;
    }

    @Embedded
    public REmbeddedReference getTaskRef() {
        return taskRef;
    }

    public void setTaskRef(REmbeddedReference taskRef) {
        this.taskRef = taskRef;
    }

    public ROperationResultStatus getStatus() {
        return status;
    }

    public void setStatus(ROperationResultStatus status) {
        this.status = status;
    }

    @Column(name = "timestampValue")
    public XMLGregorianCalendar getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(XMLGregorianCalendar timestamp) {
        this.timestamp = timestamp;
    }

    @Transient
    @Override
    public Boolean isTransient() {
        return trans;
    }

    @Override
    public void setTransient(Boolean trans) {
        this.trans = trans;
    }

    public static void fromJaxb(@NotNull OperationExecutionType jaxb, @NotNull ROperationExecution repo,
            RObject parent, RepositoryContext repositoryContext) throws DtoTranslationException {
        repo.setOwner(parent);
        fromJaxb(jaxb, repo, repositoryContext, null);
    }

    public static void fromJaxb(@NotNull OperationExecutionType jaxb, @NotNull ROperationExecution repo,
            ObjectType parent, RepositoryContext repositoryContext,
            IdGeneratorResult generatorResult) {
        repo.setOwnerOid(parent.getOid());
        fromJaxb(jaxb, repo, repositoryContext, generatorResult);
    }

    private static void fromJaxb(@NotNull OperationExecutionType jaxb, @NotNull ROperationExecution repo,
            RepositoryContext repositoryContext, IdGeneratorResult generatorResult) {
        if (generatorResult != null) {
            repo.setTransient(generatorResult.isTransient(jaxb.asPrismContainerValue()));
        }
        repo.setId(RUtil.toInteger(jaxb.getId()));
        repo.setTaskRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getTaskRef(), repositoryContext.relationRegistry));
        repo.setInitiatorRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getInitiatorRef(), repositoryContext.relationRegistry));
        repo.setStatus(RUtil.getRepoEnumValue(jaxb.getStatus(), ROperationResultStatus.class));
        repo.setTimestamp(jaxb.getTimestamp());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ROperationExecution)) {
            return false;
        }

        ROperationExecution that = (ROperationExecution) o;
        return Objects.equals(getOwnerOid(), that.getOwnerOid())
                && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOwnerOid(), id);
    }

    @Override
    public String toString() {
        return "ROperationExecution{" +
                "ownerOid='" + ownerOid + '\'' +
                ", id=" + id +
                ", initiatorRef=" + initiatorRef +
                ", taskRef=" + taskRef +
                ", status=" + status +
                ", timestamp=" + timestamp +
                '}';
    }
}
