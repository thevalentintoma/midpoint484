/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.perf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.schema.DefinitionUpdateOption;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.internals.InternalsConfig;

import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.AbstractEmptyInternalModelTest;
import com.evolveum.midpoint.model.impl.controller.SchemaTransformer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.ParsedGetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.ExtensionValueGenerator;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.asserter.AssignmentGenerator;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.CheckedConsumer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Tests the performance of various `model-impl` components.
 *
 * First, it checks the `applySchemasAndSecurity` method.
 *
 * Later, it may be extended or split into smaller tests. The time will tell.
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestPerformance extends AbstractEmptyInternalModelTest {

    private static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "perf");

    private static final TestObject<ObjectTemplateType> OBJECT_TEMPLATE_PERSON = TestObject.file(
            TEST_DIR, "object-template-person.xml", "202c9a5e-b876-4009-8b87-25688b12f7b8");
    private static final TestObject<ArchetypeType> ARCHETYPE_PERSON = TestObject.file(
            TEST_DIR, "archetype-person.xml", "3bb58e36-04b3-4601-a57a-4aaaa3b64458");
    private static final TestObject<RoleType> ROLE_CAN_READ_ALL = TestObject.file(
            TEST_DIR, "role-can-read-all.xml", "8d79c980-0999-49f7-ba11-6776dad41770");
    private static final TestObject<UserType> USER_CAN_READ_ALL = TestObject.file(
            TEST_DIR, "user-can-read-all.xml", "564261c3-efe8-4f35-845e-f928395d2cf1");
    private static final TestObject<RoleType> ROLE_CAN_READ_ALMOST_ALL = TestObject.file(
            TEST_DIR, "role-can-read-almost-all.xml", "b6774d03-b2c5-4b1b-a175-6deacbdd0115");
    private static final TestObject<UserType> USER_CAN_READ_ALMOST_ALL = TestObject.file(
            TEST_DIR, "user-can-read-almost-all.xml", "78eaaa5c-b8f1-4959-b356-6c41c04d613e");
    private static final TestObject<RoleType> ROLE_CAN_READ_FEW = TestObject.file(
            TEST_DIR, "role-can-read-few.xml", "46302f20-2197-4345-9d4b-ab183a028aa9");
    private static final TestObject<UserType> USER_CAN_READ_FEW = TestObject.file(
            TEST_DIR, "user-can-read-few.xml", "a17e2af6-7b60-4cf3-bebf-513af4a61b16");

    @Autowired private SchemaTransformer schemaTransformer;

    private final ExtensionValueGenerator extensionValueGenerator = ExtensionValueGenerator.withDefaults();
    private final AssignmentGenerator assignmentGenerator = AssignmentGenerator.withDefaults();

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        initTestObjects(initTask, initResult,
                OBJECT_TEMPLATE_PERSON, ARCHETYPE_PERSON,
                ROLE_CAN_READ_ALL, USER_CAN_READ_ALL,
                ROLE_CAN_READ_ALMOST_ALL, USER_CAN_READ_ALMOST_ALL,
                ROLE_CAN_READ_FEW, USER_CAN_READ_FEW);
        InternalsConfig.reset(); // We want to measure performance, so no consistency checking and the like.
    }

    @Override
    protected boolean isAvoidLoggingChange() {
        return false;
    }

    /** Tests schema/security application for full autz (superuser). */
    @Test
    public void test100ApplyFullAutz() throws CommonException {
        executeAutzTest("full", user -> assertReadAll(user));
    }

    /** Tests schema/security application for "read all" autz. */
    @Test
    public void test110ApplyReadAll() throws CommonException {
        login(USER_CAN_READ_ALL.get());
        executeAutzTest("read all", user -> assertReadAll(user));
    }

    private void assertReadAll(UserType user) {
        assertUser(user.asPrismObject(), "after")
                .extension()
                .assertSize(50)
                .end();
        var assignment = user.getAssignment().get(0);
        assertThat(assignment.getTargetRef()).isNotNull();
        assertThat(assignment.getDescription()).isNotNull();
    }

    /** Tests schema/security application for "read almost all" autz. */
    @Test
    public void test120ApplyReadAlmostAll() throws CommonException {
        login(USER_CAN_READ_ALMOST_ALL.get());
        executeAutzTest("read almost all", user -> assertReadAlmostAll(user));
    }

    private void assertReadAlmostAll(UserType user) throws CommonException {
        assertUser(user.asPrismObject(), "after")
                .extension()
                .assertSize(49)
                .end();
        var assignment = user.getAssignment().get(0);
        assertThat(assignment.getTargetRef()).isNotNull();
        assertThat(assignment.getDescription()).isNotNull();
    }

    /** Tests schema/security application for "read few" autz. */
    @Test
    public void test130ApplyReadFew() throws CommonException {
        login(USER_CAN_READ_FEW.get());
        executeAutzTest("read few", user -> assertReadFew(user));
    }

    private void assertReadFew(UserType user) throws CommonException {
        assertUser(user.asPrismObject(), "after")
                .extension()
                .assertSize(1)
                .end();
        var assignment = user.getAssignment().get(0);
        assertThat(assignment.getTargetRef()).isNull();
        assertThat(assignment.getDescription()).isNotNull();
    }

    private void executeAutzTest(
            String autzLabel, CheckedConsumer<UserType> autzAsserter)
            throws CommonException {

        executeAutzTest1(autzLabel, DefinitionUpdateOption.DEEP, autzAsserter);
        executeAutzTest1(autzLabel, DefinitionUpdateOption.ROOT_ONLY, autzAsserter);
        executeAutzTest1(autzLabel, DefinitionUpdateOption.NONE, autzAsserter);
    }

    private void executeAutzTest1(
            String autzLabel, DefinitionUpdateOption definitionUpdateOption, CheckedConsumer<UserType> autzAsserter)
            throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        int iterations = 5;
        int numUsers = 4000;

        String label = autzLabel + "/" + definitionUpdateOption;

        when("heating up: " + label);
        int heatUpExecutions = numUsers * 2;
        long heatUpDuration = executeSingleAutzTestIteration(
                heatUpExecutions, definitionUpdateOption,
                user -> {
                    if (autzAsserter != null) {
                        autzAsserter.accept(user);
                    }
                    checkDefinition(user, definitionUpdateOption);
                }, task, result);

        display(String.format(
                "Heat-up %s: Applied to a single user in %.3f ms (%,d executions)",
                label, (double) heatUpDuration / heatUpExecutions, heatUpExecutions));

        when("testing: " + label);
        long start = System.currentTimeMillis();
        long netDuration = 0;
        for (int i = 0; i < iterations; i++) {
            netDuration += executeSingleAutzTestIteration(
                    numUsers, definitionUpdateOption, null, task, result);
        }
        long grossDuration = System.currentTimeMillis() - start;
        int executions = numUsers * iterations;
        display(String.format(
                "Testing %s: Applied to a single user in %.3f ms (%,d ms gross and %,d ms net duration, %,d executions)",
                label, (double) netDuration / executions, grossDuration, netDuration, executions));
    }

    private long executeSingleAutzTestIteration(
            int numUsers, DefinitionUpdateOption definitionUpdateOption,
            CheckedConsumer<UserType> asserter, Task task, OperationResult result)
            throws CommonException {
        SearchResultList<PrismObject<UserType>> users = new SearchResultList<>();
        for (int i = 0; i < numUsers; i++) {
            UserType user = new UserType()
                    .name("user " + i)
                    .assignment(ARCHETYPE_PERSON.assignmentTo()
                            .description("just-to-make-test-happy"));
            PrismObject<UserType> userPrismObject = user.asPrismObject();
            extensionValueGenerator.populateExtension(userPrismObject, 50);
            assignmentGenerator.populateAssignments(user, 50);
            assignmentGenerator.createRoleRefs(user);
            userPrismObject.freeze();
            users.add(userPrismObject);
        }

        var parsedOptions = ParsedGetOperationOptions.of(
                GetOperationOptions.createReadOnly()
                        .definitionUpdate(definitionUpdateOption));

        long start = System.currentTimeMillis();
        var usersAfter =
                schemaTransformer.applySchemasAndSecurityToObjects(users, parsedOptions, task, result);
        long duration = System.currentTimeMillis() - start;

        if (asserter != null) {
            asserter.accept(usersAfter.get(0).asObjectable());
        }

        return duration;
    }

    private void checkDefinition(UserType user, DefinitionUpdateOption option) {
        PrismObjectDefinition<UserType> rootDef = user.asPrismObject().getDefinition();
        assertThat(rootDef.findPropertyDefinition(UserType.F_DESCRIPTION).getDisplayName())
                .as("description displayName in root def")
                .isEqualTo(option == DefinitionUpdateOption.NONE ? "ObjectType.description" : "X-DESCRIPTION");
        if (user.getDescription() != null) {
            var localDef = user.asPrismObject().findProperty(UserType.F_DESCRIPTION).getDefinition();
            assertThat(localDef.getDisplayName())
                    .as("description displayName (local)")
                    .isEqualTo(option != DefinitionUpdateOption.DEEP ? "ObjectType.description" : "X-DESCRIPTION");
        }
    }
}
