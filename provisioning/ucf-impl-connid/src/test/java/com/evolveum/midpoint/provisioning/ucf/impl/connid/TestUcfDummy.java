/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;

import static com.evolveum.midpoint.schema.processor.ResourceSchemaFactory.*;
import static com.evolveum.midpoint.schema.processor.ShadowAssociationParticipantRole.OBJECT;
import static com.evolveum.midpoint.schema.processor.ShadowAssociationParticipantRole.SUBJECT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.provisioning.ucf.api.UcfFetchErrorReportingMethod.EXCEPTION;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.test.DummyHrScenario.*;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import com.evolveum.icf.dummy.connector.DummyConnector;
import com.evolveum.icf.dummy.resource.*;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorConfigurationOptions.CompleteSchemaProvider;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.test.DummyHrScenario;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Simple UCF tests, using dummy resource. Some tests even avoid UCF/ConnId.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-ucf-connid-test.xml" })
public class TestUcfDummy extends AbstractUcfDummyTest {

    private static final File HR_RESOURCE_DUMMY_FILE = new File(UcfTestUtil.TEST_DIR, "hr-resource-dummy.xml");

    /** Dummy resource with the support of hierarchical (LDAP-like) object names. Used for 2xx tests. */
    private DummyResource hierarchicalResource;
    private DummyResourceContoller hierarchicalResourceCtl;

    /** Scenario with associations, HR-style. Used for 3xx tests. */
    private DummyHrScenario hrScenario;

    /** Connector instance to access {@link #hrScenario} via UCF. */
    private ConnectorInstance hrConnectorInstance;

    @Test
    public void test000PrismContextSanity() {
        SchemaRegistry schemaRegistry = PrismTestUtil.getPrismContext().getSchemaRegistry();

        PrismSchema icfcSchema = schemaRegistry.findSchemaByNamespace(SchemaConstants.NS_ICF_CONFIGURATION);
        assertNotNull("ICFC schema not found in the context", icfcSchema);
        PrismContainerDefinition<ConnectorConfigurationType> configurationPropertiesDef =
                icfcSchema.findContainerDefinitionByElementName(ICF_CONFIGURATION_PROPERTIES_NAME);
        assertNotNull("icfc:configurationProperties container definition not found", configurationPropertiesDef);

        PrismSchema icfsSchema = schemaRegistry.findSchemaByNamespace(SchemaConstants.NS_ICF_SCHEMA);
        assertNotNull("ICFS schema not found in the context (" + SchemaConstants.NS_ICF_SCHEMA + ")", icfsSchema);
    }

    @Test
    public void test001ResourceSanity() {
        displayDumpable("Resource", resource);

        assertEquals("Wrong oid", "ef2bc95b-76e0-59e2-86d6-9999dddddddd", resource.getOid());
        PrismObjectDefinition<ResourceType> resourceDefinition = resource.getDefinition();
        assertNotNull("No resource definition", resourceDefinition);
        PrismAsserts.assertObjectDefinition(
                resourceDefinition, SchemaConstantsGenerated.C_RESOURCE, ResourceType.COMPLEX_TYPE, ResourceType.class);
        assertEquals("Wrong class in resource", ResourceType.class, resource.getCompileTimeClass());
        ResourceType resourceType = resource.asObjectable();
        assertNotNull("asObjectable resulted in null", resourceType);

        assertPropertyValue(resource, "name", PolyString.fromOrig("Dummy Resource"));
        assertPropertyDefinition(resource, "name", PolyStringType.COMPLEX_TYPE, 0, 1);

        PrismContainer<?> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
        assertContainerDefinition(configurationContainer, "configuration", ConnectorConfigurationType.COMPLEX_TYPE, 0, 1);
        PrismContainerValue<?> configContainerValue = configurationContainer.getValue();
        Collection<Item<?, ?>> configItems = configContainerValue.getItems();
        assertEquals("Wrong number of config items", 2, configItems.size());

        PrismContainer<?> dummyConfigPropertiesContainer =
                configurationContainer.findContainer(ICF_CONFIGURATION_PROPERTIES_NAME);
        assertNotNull("No icfc:configurationProperties container", dummyConfigPropertiesContainer);
        Collection<Item<?, ?>> dummyConfigPropItems = dummyConfigPropertiesContainer.getValue().getItems();
        assertEquals("Wrong number of dummy ConfigPropItems items", 4, dummyConfigPropItems.size());
    }

    @Test
    public void test002ConnectorSchema() throws Exception {
        var connectorSchema = connectorFactory.generateConnectorConfigurationSchema(connectorBean);
        assertThat(connectorSchema).isNotNull();

        IntegrationTestTools.assertConnectorSchemaSanity(connectorSchema, "generated", true);
        assertEquals("Unexpected number of definitions", 3, connectorSchema.size());

        Document xsdSchemaDom = connectorSchema.serializeToXsd();
        displayValue("Serialized XSD connector schema", DOMUtil.serializeDOMToString(xsdSchemaDom));

        // Try to re-parse
        var reparsedConnectorSchema = ConnectorSchemaFactory.parse(DOMUtil.getFirstChildElement(xsdSchemaDom), "");
        IntegrationTestTools.assertConnectorSchemaSanity(reparsedConnectorSchema, "re-parsed", true);
        assertEquals("Unexpected number of definitions in re-parsed schema", 3, reparsedConnectorSchema.size());
    }

    /**
     * Test listing connectors. Very simple. Just test that the list is
     * non-empty and that there are mandatory values filled in.
     */
    @Test
    public void test010ListConnectors() throws Exception {
        OperationResult result = createOperationResult();
        Set<ConnectorType> connectors = connectorFactory.listConnectors(null, result);

        System.out.println("---------------------------------------------------------------------");
        assertNotNull(connectors);
        assertFalse(connectors.isEmpty());

        for (ConnectorType connector : connectors) {
            assertNotNull(connector.getName());
            System.out.printf("CONNECTOR OID=%s, name=%s, version=%s%n",
                    connector.getOid(), connector.getName(), connector.getConnectorVersion());
            System.out.println("--");
            System.out.println(ObjectTypeUtil.dump(connector));
            System.out.println("--");
        }

        System.out.println("---------------------------------------------------------------------");

        assertEquals("Unexpected number of connectors discovered", 8, connectors.size());
    }

    @Test
    public void test020CreateConfiguredConnector() throws Exception {
        cc = connectorFactory.createConnectorInstance(connectorBean,
                "dummy",
                "description of dummy test connector instance");
        OperationResult result = createOperationResult();

        // WHEN
        configure(
                resourceBean.getConnectorConfiguration(),
                ResourceTypeUtil.getSchemaGenerationConstraints(resourceBean),
                result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
    }

    @Test
    public void test022ConnectorStatsConfigured() throws Exception {
        // WHEN
        ConnectorOperationalStatus operationalStatus = cc.getOperationalStatus();

        // THEN
        displayDumpable("Connector operational status", operationalStatus);
        assertNotNull("null operational status", operationalStatus);

        assertEquals("Wrong connectorClassName", DummyConnector.class.getName(), operationalStatus.getConnectorClassName());
        assertNull("Wrong poolConfigMinSize", operationalStatus.getPoolConfigMinSize());
        assertEquals("Wrong poolConfigMaxSize", (Integer) 10, operationalStatus.getPoolConfigMaxSize());
        assertEquals("Wrong poolConfigMinIdle", (Integer) 1, operationalStatus.getPoolConfigMinIdle());
        assertEquals("Wrong poolConfigMaxIdle", (Integer) 10, operationalStatus.getPoolConfigMaxIdle());
        assertEquals("Wrong poolConfigWaitTimeout", (Long) 150000L, operationalStatus.getPoolConfigWaitTimeout());
        assertEquals("Wrong poolConfigMinEvictableIdleTime", (Long) 120000L, operationalStatus.getPoolConfigMinEvictableIdleTime());
        assertEquals("Wrong poolStatusNumIdle", (Integer) 0, operationalStatus.getPoolStatusNumIdle());
        assertEquals("Wrong poolStatusNumActive", (Integer) 0, operationalStatus.getPoolStatusNumActive());
    }

    @Test
    public void test030ResourceSchema() throws Exception {
        OperationResult result = createOperationResult();

        cc = connectorFactory.createConnectorInstance(connectorBean,
                "dummy",
                "description of dummy test connector instance");

        configure(
                resourceBean.getConnectorConfiguration(),
                null,
                result);

        // WHEN
        resourceSchema = nativeToBare(cc.fetchResourceSchema(result));

        // THEN
        displayDumpable("Generated resource schema", resourceSchema);
        assertEquals("Unexpected number of definitions", 4, resourceSchema.size());

        dummyResourceCtl.assertDummyResourceSchemaSanityExtended(resourceSchema, resourceBean, true);

        Document xsdSchemaDom = resourceSchema.serializeNativeToXsd();
        assertNotNull("No serialized resource schema", xsdSchemaDom);
        displayValue("Serialized XSD resource schema", DOMUtil.serializeDOMToString(xsdSchemaDom));

        // Try to re-parse
        var reparsedResourceSchema = ResourceSchemaFactory.parseNativeSchemaAsBare(xsdSchemaDom);
        displayDumpable("Re-parsed resource schema", reparsedResourceSchema);
        assertEquals("Unexpected number of definitions in re-parsed schema", 4, reparsedResourceSchema.size());

        dummyResourceCtl.assertDummyResourceSchemaSanityExtended(reparsedResourceSchema, resourceBean, true);
    }

    /**
     * Currently this test fails, because the limited raw schema (accounts only) is not sufficient for the complete schema
     * (as there are associations). To be resolved later.
     */
    @Test(enabled = false)
    public void test031ResourceSchemaAccountObjectClass() throws Exception {
        OperationResult result = createOperationResult();

        cc = connectorFactory.createConnectorInstance(connectorBean,
                "dummy",
                "description of dummy test connector instance");

        configure(
                resourceBean.getConnectorConfiguration(),
                List.of(RI_ACCOUNT_OBJECT_CLASS),
                result);

        // WHEN
        resourceSchema = ResourceSchemaFactory.nativeToBare(cc.fetchResourceSchema(result));

        // THEN
        displayDumpable("Generated resource schema", resourceSchema);

        var definitions = resourceSchema.getDefinitions();
        assertEquals("Unexpected number of definitions", 1, definitions.size());
        assertEquals("Unexpected number of object class definitions", 1, resourceSchema.getObjectClassDefinitions().size());
        display("RESOURCE SCHEMA DEFINITION" + definitions.iterator().next().getTypeName());
    }

    @Test
    public void test033ConnectorStatsInitialized() throws Exception {
        // WHEN
        ConnectorOperationalStatus operationalStatus = cc.getOperationalStatus();

        // THEN
        displayDumpable("Connector operational status", operationalStatus);
        assertNotNull("null operational status", operationalStatus);

        assertEquals("Wrong connectorClassName", DummyConnector.class.getName(), operationalStatus.getConnectorClassName());
        assertNull("Wrong poolConfigMinSize", operationalStatus.getPoolConfigMinSize());
        assertEquals("Wrong poolConfigMaxSize", (Integer) 10, operationalStatus.getPoolConfigMaxSize());
        assertEquals("Wrong poolConfigMinIdle", (Integer) 1, operationalStatus.getPoolConfigMinIdle());
        assertEquals("Wrong poolConfigMaxIdle", (Integer) 10, operationalStatus.getPoolConfigMaxIdle());
        assertEquals("Wrong poolConfigWaitTimeout", (Long) 150000L, operationalStatus.getPoolConfigWaitTimeout());
        assertEquals("Wrong poolConfigMinEvictableIdleTime", (Long) 120000L, operationalStatus.getPoolConfigMinEvictableIdleTime());
        assertEquals("Wrong poolStatusNumIdle", (Integer) 1, operationalStatus.getPoolStatusNumIdle());
        assertEquals("Wrong poolStatusNumActive", (Integer) 0, operationalStatus.getPoolStatusNumActive());
    }

    @Test
    public void test040AddAccount() throws Exception {
        OperationResult result = createOperationResult();

        var accountClassDefinition = resourceSchema.findObjectClassDefinitionRequired(RI_ACCOUNT_OBJECT_CLASS);

        var shadow = ShadowBuilder.withDefinition(accountClassDefinition)
                .onResource(resource.getOid())
                .withAttribute(SchemaConstants.ICFS_NAME, ACCOUNT_JACK_USERNAME)
                .asPrismObject();

        when();
        cc.addObject(shadow, null, result);

        then();
        DummyAccount dummyAccount = dummyResource.getAccountByName(ACCOUNT_JACK_USERNAME);
        assertNotNull("Account " + ACCOUNT_JACK_USERNAME + " was not created", dummyAccount);
        assertNotNull("Account " + ACCOUNT_JACK_USERNAME + " has no username", dummyAccount.getName());
    }

    @Test
    public void test050Search() throws Exception {

        var accountClassDefinition = resourceSchema.findObjectClassDefinitionRequired(RI_ACCOUNT_OBJECT_CLASS);

        final List<PrismObject<ShadowType>> searchResults = new ArrayList<>();

        UcfObjectHandler handler = (ucfObject, result) -> {
            displayDumpable("Search: found", ucfObject);
            checkUcfObject(ucfObject, accountClassDefinition);
            searchResults.add(ucfObject.getPrismObject());
            return true;
        };

        UcfExecutionContext ctx = createExecutionContext();
        OperationResult result = createOperationResult();

        when();
        cc.search(
                accountClassDefinition, null, handler,
                null, null, null, null,
                ctx, result);

        then();
        assertEquals("Unexpected number of search results", 1, searchResults.size());
    }

    private void checkUcfObject(UcfResourceObject ucfResourceObject, ResourceObjectClassDefinition objectClassDefinition) {
        var object = ucfResourceObject.getPrismObject();
        ShadowType bean = object.asObjectable();
        assertNotNull("No objectClass in shadow " + object, bean.getObjectClass());
        assertEquals("Wrong objectClass in shadow " + object, objectClassDefinition.getTypeName(), bean.getObjectClass());
        Collection<ResourceAttribute<?>> attributes = ShadowUtil.getAttributes(object);
        assertNotNull("No attributes in shadow " + object, attributes);
        assertFalse("Empty attributes in shadow " + object, attributes.isEmpty());
    }

    /** MID-8145 */
    @Test
    public void test060GetByUidWithNameHint() throws Exception {
        given();
        UcfExecutionContext ctx = createExecutionContext();
        OperationResult result = createOperationResult();

        var accountClassDefinition = resourceSchema.findObjectClassDefinitionRequired(RI_ACCOUNT_OBJECT_CLASS);
        var uidAttr = accountClassDefinition.<String>getPrimaryIdentifierRequired().instantiateFromRealValue("jack");
        var nameAttr = accountClassDefinition.<String>getSecondaryIdentifierRequired().instantiateFromRealValue("jack");
        var identification = ResourceObjectIdentification.withPrimary(accountClassDefinition, uidAttr, List.of(nameAttr));

        when("getting account by UID with name hint");
        var resourceObject = cc.fetchObject(identification, null, ctx, result);

        then("account is retrieved OK");
        displayDumpable("resourceObject retrieved", resourceObject);
        assertThat(resourceObject).as("resourceObject").isNotNull();
        checkUcfObject(resourceObject, accountClassDefinition);
    }

    /** No operation, no sync changes. */
    @Test
    public void test100FetchEmptyChanges() throws Exception {
        OperationResult result = createOperationResult();
        var accountClassDefinition = resourceSchema.findObjectClassDefinitionRequired(RI_ACCOUNT_OBJECT_CLASS);

        when("current token is fetched");
        UcfSyncToken lastToken = cc.fetchCurrentToken(accountClassDefinition, null, result);

        then();
        displayValue("Token", lastToken);
        assertNotNull("No last sync token", lastToken);

        when("changes are fetched");
        CollectingChangeListener handler = new CollectingChangeListener();
        cc.fetchChanges(accountClassDefinition, lastToken, null, null, null, handler, result);

        then();
        assertThat(handler.getChanges()).as("changes").isEmpty();
    }

    /** An account is manually added to the resource; then a single sync change is fetched. */
    @Test
    public void test101FetchAddChange() throws Exception {
        OperationResult result = createOperationResult();
        var accountClassDefinition = resourceSchema.findObjectClassDefinitionRequired(RI_ACCOUNT_OBJECT_CLASS);

        UcfSyncToken lastToken = cc.fetchCurrentToken(accountClassDefinition, null, result);
        assertNotNull("No last sync token", lastToken);

        given("account is added to the resource");
        dummyResource.setSyncStyle(DummySyncStyle.DUMB);
        DummyAccount newAccount = new DummyAccount("blackbeard");
        newAccount.addAttributeValues("fullname", "Edward Teach");
        newAccount.setEnabled(true);
        newAccount.setPassword("shiverMEtimbers");
        dummyResource.addAccount(newAccount);

        when("changes are fetched");
        CollectingChangeListener handler = new CollectingChangeListener();
        cc.fetchChanges(accountClassDefinition, lastToken, null, null, null, handler, result);

        then("there is 1 change, and is sane");

        List<UcfLiveSyncChange> changes = handler.getChanges();
        AssertJUnit.assertEquals(1, changes.size());
        UcfLiveSyncChange change = changes.get(0);
        assertNotNull("null change", change);
        var resourceObject = change.getResourceObject();
        assertNotNull("null current resource object", resourceObject);
        PrismAsserts.assertParentConsistency(resourceObject.getPrismObject());
        Collection<ResourceAttribute<?>> identifiers = change.getIdentifiers();
        assertNotNull("null identifiers", identifiers);
        assertFalse("empty identifiers", identifiers.isEmpty());
    }

    /** Checks if the hierarchical support on {@link DummyResource} works OK (no UCF/ConnId contact). */
    @Test
    public void test200AddHierarchicalObjects() throws Exception {
        initializeHierarchicalResourceIfNeeded();

        given("an org exists");
        hierarchicalResourceCtl.addOrg("org200");

        when("top-level account is added");
        hierarchicalResourceCtl.addAccount("test");

        then("it is there");
        assertThat(hierarchicalResource.getAccountByName("test")).isNotNull();

        when("account in non-existent org is added");
        try {
            hierarchicalResourceCtl.addAccount("test:org200a");
            fail("unexpected success");
        } catch (ObjectDoesNotExistException e) {
            then("exception is thrown");
            assertExpectedException(e)
                    .hasMessageContaining(
                            "Cannot add object with name 'test:org200a' because its parent org 'org200a' does not exist");
        }

        when("account in existing org is added");
        hierarchicalResourceCtl.addAccount("test:org200");

        then("it is there");
        assertThat(hierarchicalResource.getAccountByName("test:org200")).isNotNull();
    }

    /** Again, checks the hierarchical support (deleting non-empty org); no UCF/ConnId access here. */
    @Test
    public void test210DeleteNonEmptyOrgInHierarchy() throws Exception {
        initializeHierarchicalResourceIfNeeded();

        given("an org with account exists");
        hierarchicalResourceCtl.addOrg("org210");
        hierarchicalResourceCtl.addAccount("test:org210");

        when("org is being deleted");
        try {
            hierarchicalResource.deleteOrgByName("org210");
            fail("unexpected success");
        } catch (SchemaViolationException e) {
            assertExpectedException(e);
        }
    }

    /** Again a hierarchical support test (rename org -> renaming its content). No UCF/ConnId. */
    @Test
    public void test220RenameOrgInHierarchy() throws Exception {
        initializeHierarchicalResourceIfNeeded();

        given("two nested orgs with an account exist");
        DummyOrg root = hierarchicalResourceCtl.addOrg("root220");
        DummyOrg org = hierarchicalResourceCtl.addOrg("org220:root220");
        hierarchicalResourceCtl.addAccount("test:org220:root220");

        displayDumpable("root", root);
        displayDumpable("org", org);

        when("root is renamed");
        hierarchicalResource.renameOrg(root.getId(), "root220", "root220a");

        then("orgs and account have new names");
        assertOrgExists("root220a");
        assertOrgExists("org220:root220a");
        assertAccountExists("test:org220:root220a");

        when("org is renamed");
        hierarchicalResource.renameOrg(org.getId(), "org220:root220a", "org220a:root220a");

        then("orgs and account have new names");
        assertOrgExists("root220a");
        assertOrgExists("org220a:root220a");
        assertAccountExists("test:org220a:root220a");
    }

    /** For test2xx. */
    private void initializeHierarchicalResourceIfNeeded() throws Exception {
        if (hierarchicalResource != null) {
            return;
        }

        hierarchicalResourceCtl = DummyResourceContoller.create("hierarchical")
                .extendSchemaPirate();

        hierarchicalResource = hierarchicalResourceCtl.getDummyResource();
        hierarchicalResource.setUidMode(UidMode.UUID);
        hierarchicalResource.setHierarchicalObjectsEnabled(true);

        // No need for UCF/ConnId access
    }

    private void assertOrgExists(String name)
            throws ConflictException, FileNotFoundException, SchemaViolationException, InterruptedException, ConnectException {
        DummyOrg org = hierarchicalResource.getOrgByName(name);
        assertThat(org).as("org named '" + name + "'").isNotNull();
    }

    private void assertAccountExists(String name)
            throws ConflictException, FileNotFoundException, SchemaViolationException, InterruptedException, ConnectException {
        DummyAccount account = hierarchicalResource.getAccountByName(name);
        assertThat(account).as("account named '" + name + "'").isNotNull();
    }

    /** Checks whether the schema related to associations is fetched, serialized, and parsed correctly. */
    @Test
    public void test300AssociationsSchema() throws Exception {
        initializeHrScenarioIfNeeded();

        var completeSchema = hrScenario.getResourceSchemaRequired();
        checkHrSchema(completeSchema);

        when("native schema is serialized to XSD and reparsed");
        var schemaDocument = completeSchema.serializeNativeToXsd();
        displayValue("Native XML schema", DOMUtil.serializeDOMToString(schemaDocument));

        var reparsedNativeSchema = parseNativeSchema(schemaDocument.getDocumentElement(), "");
        displayDumpable("Reparsed native schema", reparsedNativeSchema);

        var reparsedCompleteSchema = parseCompleteSchema(hrScenario.getResourceBean(), reparsedNativeSchema);
        displayDumpable("Reparsed complete schema", reparsedCompleteSchema);

        checkHrSchema(reparsedCompleteSchema);
    }

    private void checkHrSchema(CompleteResourceSchema completeSchema) throws SchemaException {
        then("native object class definitions are OK");
        var nativeSchema = completeSchema.getNativeSchema();
        assertThat(nativeSchema.getObjectClassDefinitions()).as("object class definitions").hasSize(7);

        and("native 'contract' class definition is OK");
        var contractClassDefN = nativeSchema.findObjectClassDefinition(Contract.OBJECT_CLASS_NAME.xsd());
        assertThat(contractClassDefN).as("contract definition").isNotNull();

        and("native 'contract <-> org' and 'contract <-> person' associations definitions are OK");
        // contract-org
        var orgDefN = contractClassDefN.findAssociationDefinition(Contract.LinkNames.ORG.q());
        assertThat(orgDefN).as("contract.org association definition").isNotNull();
        assertThat(orgDefN.getTypeName()).as("contract.org association type").isEqualTo(ContractOrgUnit.NAME.xsd());
        assertThat(orgDefN.getAssociationParticipantRole()).as("role of contract in contract-org association").isEqualTo(SUBJECT);
        // contract-person; The item on the contract side is invisible, and serves just a information holder
        // for the association participant. This may change in the future, if we find a different way of represent that info.
        var personItemDefN = contractClassDefN.findAssociationDefinition(Contract.LinkNames.PERSON.q());
        assertThat(personItemDefN).as("contract.person definition").isNotNull();
        assertThat(personItemDefN.getTypeName()).as("contract.person association type").isEqualTo(PersonContract.NAME.xsd());
        assertThat(personItemDefN.getAssociationParticipantRole()).as("role of contract in contract-org association").isEqualTo(OBJECT);
        assertThat(personItemDefN.canRead()).isFalse();
        assertThat(personItemDefN.canModify()).isFalse();
        assertThat(personItemDefN.canAdd()).isFalse();

        and("complete schema is OK");
        var contractClassDefCS = completeSchema.findDefinitionForObjectClassRequired(Contract.OBJECT_CLASS_NAME.xsd());
        var orgDefCS = contractClassDefCS.findAssociationDefinitionRequired(Contract.LinkNames.ORG.q());
        var assocTypeDef = orgDefCS.getAssociationClassDefinition();
        assertThat(assocTypeDef).as("association type definition").isNotNull();
        assertThat(assocTypeDef.getClassName()).as("association type name").isEqualTo(ContractOrgUnit.NAME.xsd());
    }

    /** Creates some associations manually, and then queries them via UCF. */
    @Test
    public void test310QueryAssociations() throws Exception {
        initializeHrScenarioIfNeeded();

        given("some objects and links are created");

        DummyObject sciences = hrScenario.orgUnit.add("sciences")
                .addAttributeValues(OrgUnit.AttributeNames.DESCRIPTION.local(), "Faculty of Sciences");
        DummyObject law = hrScenario.orgUnit.add("law")
                .addAttributeValues(OrgUnit.AttributeNames.DESCRIPTION.local(), "Faculty of Law");

        DummyObject john = hrScenario.person.add("john")
                .addAttributeValue(Person.AttributeNames.FIRST_NAME.local(), "John")
                .addAttributeValue(Person.AttributeNames.LAST_NAME.local(), "Doe")
                .addAttributeValue(Person.AttributeNames.TITLE.local(), "Ing.");

        DummyObject johnContractSciences = hrScenario.contract.add("john-sciences");
        DummyObject johnContractLaw = hrScenario.contract.add("john-law");

        hrScenario.personContract.add(john, johnContractSciences);
        hrScenario.personContract.add(john, johnContractLaw);

        hrScenario.contractOrgUnit.add(johnContractSciences, sciences);
        hrScenario.contractOrgUnit.add(johnContractLaw, law);

        then("associations on the resource are OK");

        displayDumpable("dummy resource", hrScenario.getDummyResource());

        assertThat(john.getLinkedObjects(Person.LinkNames.CONTRACT.local()))
                .as("john's contracts")
                .containsExactlyInAnyOrder(johnContractSciences, johnContractLaw);
        assertThat(johnContractSciences.getLinkedObjects(Contract.LinkNames.ORG.local()))
                .as("john first contract's org")
                .containsExactlyInAnyOrder(sciences);
        assertThat(johnContractLaw.getLinkedObjects(Contract.LinkNames.ORG.local()))
                .as("john second contract's org")
                .containsExactlyInAnyOrder(law);
        assertThat(sciences.getLinkedObjects(OrgUnit.LinkNames.CONTRACT.local()))
                .as("sciences' contracts")
                .containsExactlyInAnyOrder(johnContractSciences);
        assertThat(law.getLinkedObjects(OrgUnit.LinkNames.CONTRACT.local()))
                .as("law's contracts")
                .containsExactlyInAnyOrder(johnContractLaw);

        when("associations are queried via UCF");

        OperationResult result = createOperationResult();
        UcfExecutionContext ctx = createExecutionContext(hrScenario.getResourceBean());

        ResourceObjectDefinition personDefinition = hrScenario.person.getObjectClassDefinition();
        var handler = new UcfObjectHandler.Collecting();
        hrConnectorInstance.search(
                personDefinition,
                PrismContext.get().queryFor(ShadowType.class)
                        .item(
                                Person.AttributeNames.FIRST_NAME.path(),
                                personDefinition.findAttributeDefinitionRequired(Person.AttributeNames.FIRST_NAME.q()))
                        .eq("John")
                        .build(),
                handler,
                null, null, null, EXCEPTION, ctx, result);

        then("these are OK");

        List<UcfResourceObject> objects = handler.getCollectedObjects();
        assertThat(objects).as("objects found").hasSize(1);
        UcfResourceObject johnUcfObject = objects.get(0);

        displayDumpable("john's UCF object", johnUcfObject);
        assertThat(johnUcfObject.getAttributeRealValues(Person.AttributeNames.FIRST_NAME.q()))
                .as("john's first name")
                .containsExactlyInAnyOrder("John");
        assertThat(johnUcfObject.getAttributeRealValues(Person.AttributeNames.LAST_NAME.q()))
                .as("john's last name")
                .containsExactlyInAnyOrder("Doe");
        assertThat(johnUcfObject.getAttributeRealValues(Person.AttributeNames.TITLE.q()))
                .as("john's title")
                .containsExactlyInAnyOrder("Ing.");
        var associations = johnUcfObject.getAssociations();
        assertThat(associations).as("john's associations").hasSize(1);
        var contractItem = associations.iterator().next();
        assertThat(contractItem.getElementName()).as("association name").isEqualTo(Person.LinkNames.CONTRACT.q());
        List<? extends ShadowAssociationValue> contracts = contractItem.getAssociationValues();
        assertThat(contracts).as("john's contracts").hasSize(2);
        for (ShadowAssociationValue contract : contracts) {
            assertThat(contract.getTargetObjectClassName())
                    .as("target class name")
                    .isEqualTo(Contract.OBJECT_CLASS_NAME.xsd());
            var contractAttrContainer = contract.getAttributesContainerRequired();
            assertThat(contractAttrContainer.getAttributes()).as("contract attributes").hasSize(2);
            var contractAssocContainer = contract.getAssociationsContainer();
            assertThat(contractAssocContainer.getAssociations()).as("contract associations").hasSize(1);
            var orgAssociation = contractAssocContainer.getAssociations().iterator().next();
            assertThat(orgAssociation.getElementName()).as("association name").isEqualTo(Contract.LinkNames.ORG.q());
            var orgs = orgAssociation.getAssociationValues();
            assertThat(orgs).as("contract's orgs").hasSize(1);
            var org = orgs.iterator().next();
            var orgAttrContainer = org.getAttributesContainerRequired();
            assertThat(orgAttrContainer.getAttributes()).as("org attributes in contract").hasSize(1);
            ResourceAttribute<?> orgAttribute = orgAttrContainer.getAttributes().iterator().next();
            assertThat(orgAttribute.getElementName()).as("org attribute name").isEqualTo(ICFS_NAME);
            var orgName = (String) orgAttribute.getRealValue();

            var contractName = contractAttrContainer.getNamingAttribute().getRealValue();
            if ("john-sciences".equals(contractName)) {
                assertThat(orgName).as("associated org name").isEqualTo("sciences");
            } else if ("john-law".equals(contractName)) {
                assertThat(orgName).as("associated org name").isEqualTo("law");
            } else {
                throw new AssertionError("Unknown contract: " + contractName);
            }
        }
    }

    /** For test3xx. */
    private void initializeHrScenarioIfNeeded() throws Exception {
        if (hrScenario != null) {
            return;
        }

        OperationResult result = createOperationResult();

        var resourceDef = PrismTestUtil.<ResourceType>parseObjectable(HR_RESOURCE_DUMMY_FILE);

        DummyResourceContoller hrResourceCtl = DummyResourceContoller.create("hr")
                .setResource(resourceDef);
        hrScenario = DummyHrScenario.on(hrResourceCtl).initialize();

        var configuration = resourceDef.getConnectorConfiguration();
        displayDumpable("Configuration", configuration);

        hrConnectorInstance = connectorFactory
                .createConnectorInstance(connectorBean, "hr", "")
                .configure(
                        configuration.asPrismContainerValue(),
                        new ConnectorConfigurationOptions()
                                .completeSchemaProvider(CompleteSchemaProvider.forResource(resourceDef)),
                        result)
                .initialize(null, null, result);

        var nativeSchema = hrConnectorInstance.fetchResourceSchema(result);
        displayDumpable("HR resource schema (native)", nativeSchema);
        assertThat(nativeSchema).as("native HR resource schema").isNotNull();

        hrScenario.attachResourceSchema(
                parseCompleteSchema(resourceDef, nativeSchema));

        displayDumpable("HR resource schema", hrScenario.getResourceSchemaRequired());
    }

    @Test
    public void test500SelfTest() {
        // GIVEN
        OperationResult testResult = createOperationResult();

        // WHEN
        connectorFactoryIcfImpl.selfTest(testResult);

        // THEN
        testResult.computeStatus();
        IntegrationTestTools.display(testResult);
        TestUtil.assertSuccess(testResult);
    }
}
