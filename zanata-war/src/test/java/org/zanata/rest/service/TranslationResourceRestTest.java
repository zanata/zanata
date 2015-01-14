package org.zanata.rest.service;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.dbunit.operation.DatabaseOperation;
import org.fedorahosted.tennera.jgettext.HeaderFields;
import org.hamcrest.Matchers;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.seam.security.Identity;
import org.jboss.seam.security.management.JpaIdentityStore;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.zanata.ZanataRestTest;
import org.zanata.common.ContentState;
import org.zanata.common.ContentType;
import org.zanata.common.LocaleId;
import org.zanata.common.MergeType;
import org.zanata.common.ProjectType;
import org.zanata.common.ResourceType;
import org.zanata.dao.AccountDAO;
import org.zanata.dao.ProjectIterationDAO;
import org.zanata.model.HAccount;
import org.zanata.model.HDocument;
import org.zanata.model.HLocale;
import org.zanata.model.HProject;
import org.zanata.model.HProjectIteration;
import org.zanata.model.HTextFlow;
import org.zanata.rest.RestUtil;
import org.zanata.rest.StringSet;
import org.zanata.rest.client.ISourceDocResource;
import org.zanata.rest.client.ITranslatedDocResource;
import org.zanata.rest.dto.Person;
import org.zanata.rest.dto.extensions.comment.SimpleComment;
import org.zanata.rest.dto.extensions.gettext.HeaderEntry;
import org.zanata.rest.dto.extensions.gettext.PoHeader;
import org.zanata.rest.dto.extensions.gettext.PoTargetHeader;
import org.zanata.rest.dto.extensions.gettext.PotEntryHeader;
import org.zanata.rest.dto.resource.AbstractResourceMeta;
import org.zanata.rest.dto.resource.Resource;
import org.zanata.rest.dto.resource.ResourceMeta;
import org.zanata.rest.dto.resource.TextFlow;
import org.zanata.rest.dto.resource.TextFlowTarget;
import org.zanata.rest.dto.resource.TranslationsResource;
import org.zanata.seam.SeamAutowire;
import org.zanata.security.ZanataIdentity;
import org.zanata.service.impl.*;
import org.zanata.util.HashUtil;
import org.zanata.util.RawRestTestUtils;
import org.zanata.webtrans.server.TranslationWorkspace;
import org.zanata.webtrans.server.TranslationWorkspaceManager;
import org.zanata.webtrans.server.rpc.UpdateTransUnitHandler;
import org.zanata.webtrans.shared.model.ProjectIterationId;
import org.zanata.webtrans.shared.model.TransUnitId;
import org.zanata.webtrans.shared.model.TransUnitUpdateRequest;
import org.zanata.webtrans.shared.model.WorkspaceId;
import org.zanata.webtrans.shared.rpc.SessionEventData;
import org.zanata.webtrans.shared.rpc.TransUnitUpdated.UpdateType;
import org.zanata.webtrans.shared.rpc.UpdateTransUnit;
import org.zanata.webtrans.shared.rpc.UpdateTransUnitResult;
import com.google.common.collect.Lists;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// single threaded because of ehcache (perhaps other reasons too)
@Test(singleThreaded = true)
public class TranslationResourceRestTest extends ZanataRestTest {
    private static final Logger log = LoggerFactory
            .getLogger(TranslationResourceRestTest.class);

    private static final String projectSlug = "sample-project";
    private static final String iter = "1.1";
    private static final String RESOURCE_PATH =
            "/projects/p/sample-project/iterations/i/1.0/r/";
    private static final String BAD_RESOURCE_PATH =
            "/projects/p/nonexistentProject/iterations/i/99.9/r/";
    private static final LocaleId DE = LocaleId.fromJavaName("de");
    private static final LocaleId FR = LocaleId.fromJavaName("fr");

    private static final String DOC2_NAME = "test.properties";
    private static final String DOC1_NAME = "foo.properties";

    StringSet extGettextComment = new StringSet("gettext;comment");
    StringSet extComment = new StringSet("comment");

    @Mock
    private ZanataIdentity mockIdentity;
    @Mock
    private TranslationWorkspaceManager transWorspaceManager;

    ISourceDocResource sourceDocResource;
    ITranslatedDocResource transResource;

    @BeforeClass
    void beforeClass() {
        MockitoAnnotations.initMocks(this);
        Identity.setSecurityEnabled(false);
    }

    @Override
    protected void prepareDBUnitOperations() {
        beforeTestOperations.add(new DataSetOperation(
                "org/zanata/test/model/ClearAllTables.dbunit.xml",
                DatabaseOperation.DELETE_ALL));
        beforeTestOperations.add(new DataSetOperation(
                "org/zanata/test/model/ProjectsData.dbunit.xml",
                DatabaseOperation.CLEAN_INSERT));
        beforeTestOperations.add(new DataSetOperation(
                "org/zanata/test/model/LocalesData.dbunit.xml",
                DatabaseOperation.CLEAN_INSERT));
        beforeTestOperations.add(new DataSetOperation(
                "org/zanata/test/model/AccountData.dbunit.xml",
                DatabaseOperation.CLEAN_INSERT));
    }

    @SuppressWarnings("serial")
    @Override
    protected void prepareResources() {
        SeamAutowire seamAutowire = getSeamAutowire();
        seamAutowire.use("session", getSession()).use("identity", mockIdentity)
                .use("translationWorkspaceManager", transWorspaceManager)
                .use("entityManager", super.getEm())
                .useImpl(CopyTransServiceImpl.class)
                .useImpl(TranslationServiceImpl.class)
                .useImpl(LocaleServiceImpl.class)
                .useImpl(DocumentServiceImpl.class)
                .useImpl(ResourceUtils.class)
                .useImpl(SecurityServiceImpl.class)
                .useImpl(ValidationServiceImpl.class)
                .useImpl(VersionStateCacheImpl.class)
                .useImpl(TranslationStateCacheImpl.class);

        TranslatedDocResourceService translatedDocResourceService =
                seamAutowire.autowire(TranslatedDocResourceService.class);
        SourceDocResourceService sourceDocResourceService =
                seamAutowire.autowire(SourceDocResourceService.class);

        resources.add(sourceDocResourceService);
        resources.add(translatedDocResourceService);
    }

    @BeforeMethod(dependsOnMethods = "prepareRestEasyFramework")
    public void createClient() {
        this.sourceDocResource =
                getClientRequestFactory().createProxy(ISourceDocResource.class,
                        createBaseURI(RESOURCE_PATH));
        this.transResource =
                getClientRequestFactory().createProxy(
                        ITranslatedDocResource.class,
                        createBaseURI(RESOURCE_PATH));
    }

    @Test
    public void fetchEmptyListOfResources() {
        doGetandAssertThatResourceListContainsNItems(0);
    }

    @Test
    public void createEmptyResource() {
        Resource sr = createSourceResource("my.txt");

        ClientResponse<String> response =
                sourceDocResource.post(sr, null, true);
        assertThat(response.getResponseStatus(), is(Status.CREATED));
        List<String> locationHeader = response.getHeaders().get("Location");
        assertThat(locationHeader.size(), is(1));
        assertThat(locationHeader.get(0), endsWith("r/my.txt"));
        doGetandAssertThatResourceListContainsNItems(1);
    }

    @Test
    public void createResourceWithContentUsingPost() {
        Resource sr = createSourceResource("my.txt");

        TextFlow stf = new TextFlow("tf1", LocaleId.EN, "tf1");
        sr.getTextFlows().add(stf);

        ClientResponse<String> postResponse =
                sourceDocResource.post(sr, null, true);
        assertThat(postResponse.getResponseStatus(), is(Status.CREATED));
        postResponse = sourceDocResource.post(sr, null, true);

        ClientResponse<Resource> resourceGetResponse =
                sourceDocResource.getResource("my.txt", null);
        assertThat(resourceGetResponse.getResponseStatus(), is(Status.OK));
        Resource gotSr = resourceGetResponse.getEntity();
        assertThat(gotSr.getTextFlows().size(), is(1));
        assertThat(gotSr.getTextFlows().get(0).getContents(), is(asList("tf1")));

    }

    @Test
    public void createResourceWithContentUsingPut() {
        Resource sr = createSourceResource("my.txt");

        TextFlow stf = new TextFlow("tf1", LocaleId.EN, "tf1");
        sr.getTextFlows().add(stf);

        ClientResponse<String> response =
                sourceDocResource.putResource("my.txt", sr, null);
        assertThat(response.getResponseStatus(), is(Status.CREATED));
        assertThat(response.getLocation().getHref(), endsWith("/r/my.txt"));

        ClientResponse<Resource> resourceGetResponse =
                sourceDocResource.getResource("my.txt", null);
        assertThat(resourceGetResponse.getResponseStatus(), is(Status.OK));
        Resource gotSr = resourceGetResponse.getEntity();
        assertThat(gotSr.getTextFlows().size(), is(1));
        assertThat(gotSr.getTextFlows().get(0).getContents(), is(asList("tf1")));

    }

    @Test
    public void createPoResourceWithPoHeader() {
        String docName = "my.txt";
        String docUri = RestUtil.convertToDocumentURIId(docName);
        Resource sr = createSourceResource(docName);

        TextFlow stf = new TextFlow("tf1", LocaleId.EN, "tf1");
        sr.getTextFlows().add(stf);

        // @formatter:off
        /*
        TODO: move this into an AbstractResourceMeta test (PoHeader is valid for source documents, not target)

        PoHeader poHeaderExt = new PoHeader("comment", new HeaderEntry("h1", "v1"), new HeaderEntry("h2", "v2"));
        sr.getExtensions(true).add(poHeaderExt);

        */
        // @formatter:on

        ClientResponse<String> postResponse =
                sourceDocResource.post(sr, null, true); // new
        // StringSet(PoHeader.ID));
        assertThat(postResponse.getResponseStatus(), is(Status.CREATED));
        doGetandAssertThatResourceListContainsNItems(1);

        ClientResponse<Resource> resourceGetResponse =
                sourceDocResource.getResource(docUri, null);
        // , new StringSet(PoHeader.ID));
        assertThat(resourceGetResponse.getResponseStatus(), is(Status.OK));
        Resource gotSr = resourceGetResponse.getEntity();
        assertThat(gotSr.getTextFlows().size(), is(1));
        assertThat(gotSr.getTextFlows().get(0).getContents(), is(asList("tf1")));

        // @formatter:off
        /*
        TODO: move this into an AbstractResourceMeta test

        assertThat(gotSr.getExtensions().size(), is(1));
        PoHeader gotPoHeader = gotSr.getExtensions().findByType(PoHeader.class);
        assertThat(gotPoHeader, notNullValue());
        assertThat(poHeaderExt.getComment(), is(gotPoHeader.getComment()));
        assertThat(poHeaderExt.getEntries(), is(gotPoHeader.getEntries()));
        */
        // @formatter:on
    }

    // NB this test breaks in Maven if the dev profile is active (because of the
    // imported testdata)
    @Test
    public void publishTranslations() {
        createResourceWithContentUsingPut();

        TranslationsResource entity = new TranslationsResource();
        TextFlowTarget target = new TextFlowTarget();
        target.setResId("tf1");
        target.setContents("hello world");
        target.setState(ContentState.Approved);
        target.setTranslator(new Person("root@localhost", "Admin user"));
        entity.getTextFlowTargets().add(target);

        LocaleId de_DE = new LocaleId("de");
        ClientResponse<String> response =
                transResource.putTranslations("my.txt", de_DE, entity, null);

        assertThat(response.getResponseStatus(), is(Status.OK));

        ClientResponse<TranslationsResource> getResponse =
                transResource.getTranslations("my.txt", de_DE, null);
        assertThat(getResponse.getResponseStatus(), is(Status.OK));
        TranslationsResource entity2 = getResponse.getEntity();
        assertThat(entity2.getTextFlowTargets().size(), is(entity
                .getTextFlowTargets().size()));

        entity.getTextFlowTargets().clear();
        // push an empty document
        response =
                transResource.putTranslations("my.txt", de_DE, entity, null,
                        MergeType.IMPORT.toString());
        assertThat(response.getResponseStatus(), is(Status.OK));

        getResponse =
                transResource.getTranslations("my.txt", de_DE, null, false,
                        null);
        assertThat(getResponse.getResponseStatus(), is(Status.OK));
    }

    @Test
    public void getDocumentThatDoesntExist() {
        ClientResponse<Resource> clientResponse =
                sourceDocResource
                        .getResource("my,doc,does,not,exist.txt", null);
        assertThat(clientResponse.getResponseStatus(), is(Status.NOT_FOUND));
    }

    @Test
    public void getDocument() throws Exception {
        String docName = "my/path/document.txt";
        String docUri = RestUtil.convertToDocumentURIId(docName);
        Resource resource = createSourceDoc(docName, false);
        sourceDocResource.putResource(docUri, resource, null);

        ClientResponse<ResourceMeta> response =
                sourceDocResource.getResourceMeta(docUri, null);
        assertThat(response.getResponseStatus(), is(Status.OK));
        ResourceMeta doc = response.getEntity();
        assertThat(doc.getName(), is(docName));
        assertThat(doc.getContentType(), is(ContentType.TextPlain));
        assertThat(doc.getLang(), is(LocaleId.EN_US));
        assertThat(doc.getRevision(), is(1));

        /*
         * Link link = doc.getLinks().findLinkByRel(Relationships.SELF);
         * assertThat( link, notNullValue() ); assertThat(
         * URIUtil.decode(link.getHref().toString()), endsWith(url+docUri) );
         *
         * link =
         * doc.getLinks().findLinkByRel(Relationships.DOCUMENT_CONTAINER);
         * assertThat( link, notNullValue() ); assertThat(
         * link.getHref().toString(), endsWith("iterations/i/1.0") );
         */
    }

    @Test
    public void getDocumentWithResources() throws Exception {
        LocaleId nbLocale = new LocaleId("de");
        String docName = "my/path/document.txt";
        String docUri = RestUtil.convertToDocumentURIId(docName);
        Resource resource = createSourceDoc(docName, true);
        sourceDocResource.putResource(docUri, resource, null);
        TranslationsResource trans = createTargetDoc();
        transResource.putTranslations(docUri, nbLocale, trans, null);

        {
            ClientResponse<Resource> response =
                    sourceDocResource.getResource(docUri, null);
            assertThat(response.getResponseStatus(), is(Status.OK));

            Resource doc = response.getEntity();
            assertThat(doc.getTextFlows().size(), is(1));
        }

        ClientResponse<TranslationsResource> response =
                transResource.getTranslations(docUri, nbLocale, null);
        assertThat(response.getResponseStatus(), is(Status.OK));

        TranslationsResource doc = response.getEntity();
        assertThat("should have one textFlow", doc.getTextFlowTargets().size(),
                is(1));
        TextFlowTarget tft = doc.getTextFlowTargets().get(0);

        assertThat(tft, notNullValue());
        assertThat("should have a textflow with this id", tft.getResId(),
                is("tf1"));

        assertThat("expected de target", tft, notNullValue());
        assertThat("expected translation for de", tft.getContents(),
                is(asList("hei verden")));
    }

    @Test
    public void putNewDocument() {
        String docName = "my/fancy/document.txt";
        String docUrl = RestUtil.convertToDocumentURIId(docName);
        Resource doc = createSourceDoc(docName, false);
        Response response = sourceDocResource.putResource(docUrl, doc, null);

        assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        assertThat(response.getMetadata().getFirst("Location").toString(),
                endsWith(RESOURCE_PATH + docUrl));

        ClientResponse<Resource> documentResponse =
                sourceDocResource.getResource(docUrl, null);

        assertThat(documentResponse.getResponseStatus(), is(Status.OK));

        doc = documentResponse.getEntity();
        assertThat(doc.getRevision(), is(1));

        /*
         * Link link = doc.getLinks().findLinkByRel(Relationships.SELF);
         * assertThat(link, notNullValue());
         * assertThat(link.getHref().toString(), endsWith(url + docUrl));
         *
         * link =
         * doc.getLinks().findLinkByRel(Relationships.DOCUMENT_CONTAINER);
         * assertThat(link, notNullValue()); assertThat(link.getType(),
         * is(MediaTypes.APPLICATION_ZANATA_PROJECT_ITERATION_XML));
         */
    }

    @Test
    public void putDocWithDuplicateTextFlowIds() throws Exception {
        String docName = "testDoc";
        String docUrl = RestUtil.convertToDocumentURIId(docName);
        Resource doc = createSourceDoc(docName, false);
        List<TextFlow> textFlows = doc.getTextFlows();

        for (int i = 0; i < 2; i++) {
            TextFlow textFlow = new TextFlow("tf1");
            textFlow.setContents("hello world!");
            textFlows.add(textFlow);
        }
        ClientResponse<?> response =
                sourceDocResource.putResource(docUrl, doc, null);
        assertThat(response.getStatus(), is(Status.BAD_REQUEST.getStatusCode()));
        String message = (String) response.getEntity();
        assertThat(message, containsString("tf1"));
    }

    @Test
    public void putNewDocumentWithResources() throws Exception {
        String docName = "my/fancy/document.txt";
        String docUrl = RestUtil.convertToDocumentURIId(docName);
        Resource doc = createSourceDoc(docName, false);

        List<TextFlow> textFlows = doc.getTextFlows();
        textFlows.clear();

        TextFlow textFlow = new TextFlow("tf1");
        textFlow.setContents("hello world!");
        textFlows.add(textFlow);

        TextFlow tf3 = new TextFlow("tf3");
        tf3.setContents("more text");
        textFlows.add(tf3);

        // Marshaller m = null;
        // JAXBContext jc = JAXBContext.newInstance(Resource.class);
        // m = jc.createMarshaller();
        // m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        // m.marshal(doc, System.out);

        Response response = sourceDocResource.putResource(docUrl, doc, null);

        assertThat(response.getStatus(), is(Status.CREATED.getStatusCode()));
        assertThat(response.getMetadata().getFirst("Location").toString(),
                endsWith(RESOURCE_PATH + docUrl));

        ClientResponse<Resource> documentResponse =
                sourceDocResource.getResource(docUrl, null);

        assertThat(documentResponse.getResponseStatus(), is(Status.OK));

        doc = documentResponse.getEntity();

        assertThat(doc.getRevision(), is(1));

        assertThat("Should have textFlows", doc.getTextFlows(), notNullValue());
        assertThat("Should have 2 textFlows", doc.getTextFlows().size(), is(2));
        assertThat("Should have tf1 textFlow", doc.getTextFlows().get(0)
                .getId(), is("tf1"));
        assertThat("Container1 should have tf3 textFlow", doc.getTextFlows()
                .get(1).getId(), is(tf3.getId()));

        textFlow = doc.getTextFlows().get(0);
        textFlow.setId("tf2");

        response = sourceDocResource.putResource(docUrl, doc, null);

        // this WAS testing for status 205
        assertThat(response.getStatus(), is(200));

        documentResponse = sourceDocResource.getResource(docUrl, null);
        assertThat(documentResponse.getResponseStatus(), is(Status.OK));
        doc = documentResponse.getEntity();

        assertThat(doc.getRevision(), is(2));

        assertThat("Should have textFlows", doc.getTextFlows(), notNullValue());
        assertThat("Should have two textFlows", doc.getTextFlows().size(),
                is(2));
        assertThat("should have same id", doc.getTextFlows().get(0).getId(),
                is("tf2"));
    }

    @Test
    public void getZero() throws Exception {
        expectDocs(true, false);
    }

    @Test
    public void put1Get() throws Exception {
        getZero();
        Resource doc1 = putDoc1(false);
        doc1.setRevision(1);
        TextFlow tf1 = doc1.getTextFlows().get(0);
        tf1.setRevision(1);
        TranslationsResource target1 = putTarget1();
        TextFlowTarget tft1 = target1.getTextFlowTargets().get(0);
        tft1.setTextFlowRevision(1);
        tft1.setRevision(1);
        expectDocs(true, false, doc1);
        expectTarget1(target1);
    }

    @Test
    public void put1Post2Get() throws Exception {
        getZero();
        Resource doc1 = putDoc1(false);
        doc1.setRevision(1);
        TextFlow tf1 = doc1.getTextFlows().get(0);
        tf1.setRevision(1);
        TranslationsResource target1 = putTarget1();
        TextFlowTarget tft1 = target1.getTextFlowTargets().get(0);
        tft1.setTextFlowRevision(1);
        tft1.setRevision(1);
        expectDocs(true, false, doc1);
        expectTarget1(target1);
        Resource doc2 = postDoc2(false);
        doc2.setRevision(1);
        TextFlow tf2 = doc2.getTextFlows().get(0);
        tf2.setRevision(1);
        TranslationsResource target2 = putTarget2();
        TextFlowTarget tft2 = target2.getTextFlowTargets().get(0);
        tft2.setTextFlowRevision(1);
        tft2.setRevision(1);
        expectDocs(true, false, doc1, doc2);
        expectTarget1(target1);
        expectTarget2(target2);
    }

    @Test
    public void put1Post2Put1() throws Exception {
        getZero();
        Resource doc1 = putDoc1(false);
        doc1.setRevision(1);
        TextFlow tf1 = doc1.getTextFlows().get(0);
        tf1.setRevision(1);
        TranslationsResource target1 = putTarget1();
        TextFlowTarget tft1 = target1.getTextFlowTargets().get(0);
        tft1.setTextFlowRevision(1);
        Resource doc2 = postDoc2(false);
        doc2.setRevision(1);
        TextFlow tf2 = doc2.getTextFlows().get(0);
        tf2.setRevision(1);
        TranslationsResource target2 = putTarget2();
        TextFlowTarget tft2 = target2.getTextFlowTargets().get(0);
        tft2.setTextFlowRevision(1);
        expectDocs(true, false, doc1, doc2);
        tft1.setRevision(1);
        tft2.setRevision(1);
        expectTarget1(target1);
        expectTarget2(target2);
        // this put should have the effect of deleting doc2
        putDoc1(false);
        deleteDoc2();
        // should be identical to doc1 from before, including revisions
        expectDocs(true, false, doc1);
        expectTarget1(target1);
        dontExpectTarget2();
        // expectTargets(true, FR, target2);
        // use dao to check that doc2 is marked obsolete
        verifyObsoleteDocument(doc2.getName());
    }

    @Test
    public void put1Delete1Put1() throws Exception {
        getZero();
        Resource doc1 = putDoc1(false);
        doc1.setRevision(1);
        TextFlow tf1 = doc1.getTextFlows().get(0);
        tf1.setRevision(1);
        TranslationsResource target1 = putTarget1();
        TextFlowTarget tft1 = target1.getTextFlowTargets().get(0);
        tft1.setTextFlowRevision(1);
        expectDocs(true, false, doc1);
        tft1.setRevision(1);
        expectTarget1(target1);
        deleteDoc1(); // doc1 becomes obsolete
        getZero();
        dontExpectTarget1();
        putDoc1(false); // doc1 resurrected, rev 1
        doc1.setRevision(1);
        tf1.setRevision(1);
        tft1.setTextFlowRevision(1);
        expectDocs(true, false, doc1);
        expectTarget1(target1);
    }

    @Test
    public void put1Put1Again() throws Exception {
        getZero();
        Resource doc1 = putDoc1(false);
        doc1.setRevision(1);
        TextFlow tf1 = doc1.getTextFlows().get(0);
        tf1.setRevision(1);
        TranslationsResource target1 = putTarget1();
        TextFlowTarget tft1 = target1.getTextFlowTargets().get(0);
        tft1.setTextFlowRevision(1);
        tft1.setRevision(1);
        expectDocs(true, false, doc1);
        expectTarget1(target1);
        putDoc1(false); // docRev still 1
        doc1.setRevision(1);
        tf1.setRevision(1);
        tft1.setTextFlowRevision(1);
        expectDocs(true, false, doc1);
        expectTarget1(target1);
    }

    public void put1Put1WithAnotherTextFlow() {
        // TODO make sure tft1 is still there even though the doc rev goes up
    }

    @Test
    public void put1Delete1Put1a() throws Exception {
        getZero();
        Resource doc1 = putDoc1(false);
        doc1.setRevision(1);
        TextFlow tf1 = doc1.getTextFlows().get(0);
        tf1.setRevision(1);
        TranslationsResource target1 = putTarget1();
        TextFlowTarget tft1 = target1.getTextFlowTargets().get(0);
        tft1.setTextFlowRevision(1);
        expectDocs(true, false, doc1);
        tft1.setRevision(1);
        expectTarget1(target1);
        deleteDoc1(); // doc1 becomes obsolete
        getZero();
        dontExpectTarget1();
        Resource doc1a = putDoc1a(false); // doc1 resurrected, rev 2
        doc1a.setRevision(2);
        TextFlow tf1a = doc1a.getTextFlows().get(0);
        tf1a.setRevision(doc1a.getRevision());
        TranslationsResource target1a = putTarget1a();
        TextFlowTarget tft1a = target1a.getTextFlowTargets().get(0);
        tft1a.setTextFlowRevision(tf1a.getRevision());
        tft1a.setRevision(1);
        expectDocs(true, false, doc1a);
        dontExpectTarget1();
        expectTarget1a(target1a);
    }

    @Test
    public void putPoPotGet() throws Exception {
        getZero();
        Resource po1 = putPo1();
        expectDocs(false, false, po1);
        TranslationsResource poTarget1 = putPoTarget1();
        expectTarget(false, po1.getName(), DE, poTarget1);
    }

    @Test
    public void put1Put1aPut1() throws Exception {
        getZero();
        Resource doc1 = putDoc1(false);
        doc1.setRevision(1);
        TextFlow tf1 = doc1.getTextFlows().get(0);
        tf1.setRevision(1);
        TranslationsResource target1 = putTarget1();
        TextFlowTarget tft1 = target1.getTextFlowTargets().get(0);
        tft1.setTextFlowRevision(1);
        tft1.setRevision(1);
        expectDocs(true, false, doc1);
        expectTarget1(target1);
        // this should completely replace doc1's textflow FOOD with HELLO
        Resource doc1a = putDoc1a(false);
        doc1a.setRevision(2);
        TextFlow tf1a = doc1a.getTextFlows().get(0);
        tf1a.setRevision(2);
        TranslationsResource target1a = putTarget1a();
        TextFlowTarget tft1a = target1a.getTextFlowTargets().get(0);
        tft1a.setTextFlowRevision(2);
        tft1a.setRevision(1);
        expectDocs(true, false, doc1a);
        dontExpectTarget1();
        expectTarget1a(target1a);
        // use dao to check that the HTextFlow FOOD (from doc1) is marked
        // obsolete
        verifyObsoleteResource(doc1.getName(), "FOOD");
        putDoc1(false); // same as original doc1, but different doc rev
        doc1.setRevision(3);
        expectDocs(true, false, doc1);
        // target 1 should be resurrected
        expectTarget1(target1);
        dontExpectTarget1a();
    }

    @Test
    public void generatedPoHeaders() throws Exception {
        LocaleId de_DE = new LocaleId("de");
        getZero();
        publishTranslations(); // push some translations (with no headers)
        // Get the translations with PO headers
        ClientResponse<TranslationsResource> response =
                transResource.getTranslations("my.txt", de_DE, new StringSet(
                        "gettext"), true, null);

        TranslationsResource translations = response.getEntity();
        assertThat(translations.getExtensions().size(), greaterThan(0));

        // List of custom Zanata headers that should be present
        final String[] requiredHeaders =
                new String[] { HeaderFields.KEY_LastTranslator,
                        HeaderFields.KEY_PoRevisionDate,
                        HeaderFields.KEY_LanguageTeam, "X-Generator",
                        HeaderFields.KEY_Language };

        for (String reqHeader : requiredHeaders) {
            boolean headerFound = false;
            for (HeaderEntry entry : translations.getExtensions()
                    .findByType(PoTargetHeader.class).getEntries()) {
                if (entry.getKey().equals(reqHeader)) {
                    headerFound = true;
                }
            }

            assertThat("PO Target Header '" + reqHeader
                    + "' was not present when pulling translations.",
                    headerFound, is(true));
        }

        // Since it is a first push with no headers, the Last Translator and
        // Last Revision Date header should be empty
        for (HeaderEntry entry : translations.getExtensions()
                .findByType(PoTargetHeader.class).getEntries()) {
            if (entry.getKey().equals(HeaderFields.KEY_LastTranslator)
                    || entry.getKey().equals(HeaderFields.KEY_PoRevisionDate)) {
                assertThat(entry.getValue().trim(), is(""));
            }
        }
    }

    @Test
    public void headersBeforeTranslating() throws Exception {
        LocaleId de_DE = new LocaleId("de");
        getZero();

        // Push a document with no translations
        createResourceWithContentUsingPut();

        // Get the translations with PO headers
        ClientResponse<TranslationsResource> response =
                transResource.getTranslations("my.txt", de_DE, new StringSet(
                        "gettext"), true, null);

        TranslationsResource translations = response.getEntity();
        assertThat(translations.getTextFlowTargets().size(), is(0)); // Expecting
                                                                     // no
                                                                     // translations

        // Make sure the headers are populated
        PoTargetHeader header =
                translations.getExtensions(true).findByType(
                        PoTargetHeader.class);
        assertThat(header, notNullValue());
        assertThat(header.getEntries().size(), greaterThan(0));

        // Make sure the header values are empty since the system does not have
        // any information for them
        for (HeaderEntry entry : header.getEntries()) {
            if (entry.getKey().equals(HeaderFields.KEY_LastTranslator)) {
                assertThat(entry.getValue().trim(), is(""));
            } else if (entry.getKey().equals(HeaderFields.KEY_PoRevisionDate)) {
                assertThat(entry.getValue().trim(), is(""));
            }
        }
    }

    @Test
    public void headersFromOriginalPush() throws Exception {
        LocaleId de_DE = new LocaleId("de");
        getZero();

        // Push a document and its translations
        createResourceWithContentUsingPut();

        TranslationsResource entity = new TranslationsResource();
        TextFlowTarget target = new TextFlowTarget();
        target.setResId("tf1");
        target.setContents("hello world");
        target.setState(ContentState.Approved);
        target.setTranslator(new Person("root@localhost", "Admin user"));
        entity.getTextFlowTargets().add(target);

        // Future Date for the PO Revision Date Header
        Calendar poRevDate = Calendar.getInstance();
        poRevDate.add(Calendar.YEAR, 1); // 1 year in the future
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mmZ");

        // Add initial headers to the translations
        PoTargetHeader transHeader = new PoTargetHeader();
        transHeader.getEntries().add(
                new HeaderEntry(HeaderFields.KEY_LastTranslator,
                        "Test User <test@zanata.org>"));
        transHeader.getEntries().add(
                new HeaderEntry(HeaderFields.KEY_PoRevisionDate, dateFormat
                        .format(poRevDate.getTime()))); // Date in the future
        entity.getExtensions(true).add(transHeader);

        // Push the translations
        ClientResponse<String> putResponse =
                transResource.putTranslations("my.txt", de_DE, entity,
                        new StringSet("gettext"));
        assertThat(putResponse.getResponseStatus(), is(Status.OK));
        getEm().flush();
        getEm().clear();

        // Get the translations with PO headers
        ClientResponse<TranslationsResource> transResponse =
                transResource.getTranslations("my.txt", de_DE, new StringSet(
                        "gettext"));
        TranslationsResource translations = transResponse.getEntity();

        // Make sure the headers are populated
        PoTargetHeader header =
                translations.getExtensions(true).findByType(
                        PoTargetHeader.class);
        assertThat(header, notNullValue());
        assertThat(header.getEntries().size(), greaterThan(0));

        // Make sure the header values are the same as the ones pushed with the
        // document
        for (HeaderEntry entry : header.getEntries()) {
            if (entry.getKey().equals(HeaderFields.KEY_LastTranslator)) {
                assertThat(entry.getValue().trim(),
                        is("Test User <test@zanata.org>"));
            } else if (entry.getKey().equals(HeaderFields.KEY_PoRevisionDate)) {
                assertThat(entry.getValue().trim(),
                        is(dateFormat.format(poRevDate.getTime())));
            }
        }
    }

    @Test
    public void headersAfterTranslating() throws Exception {
        LocaleId de_DE = new LocaleId("de");
        getZero();

        // Push a document and its translations
        createResourceWithContentUsingPut();

        TranslationsResource entity = new TranslationsResource();
        TextFlowTarget target = new TextFlowTarget();
        target.setResId("tf1");
        target.setContents("hello world");
        target.setState(ContentState.Approved);
        target.setTranslator(new Person("root@localhost", "Admin user"));
        entity.getTextFlowTargets().add(target);

        ClientResponse<String> putResponse =
                transResource.putTranslations("my.txt", de_DE, entity, null);
        assertThat(putResponse.getResponseStatus(), is(Status.OK));

        // Get the translations with PO headers
        ClientResponse<TranslationsResource> response =
                transResource.getTranslations("my.txt", de_DE, new StringSet(
                        "gettext"));

        TranslationsResource translations = response.getEntity();
        assertThat(translations.getTextFlowTargets().size(), greaterThan(0));

        // Now translate and push them again
        for (TextFlowTarget tft : translations.getTextFlowTargets()) {
            tft.setContents("Translated");
            tft.setState(ContentState.Approved);
        }

        putResponse =
                transResource.putTranslations("my.txt", de_DE, translations,
                        null);
        assertThat(putResponse.getStatus(),
                is(Response.Status.OK.getStatusCode()));

        // Fetch the translations again
        response =
                transResource.getTranslations("my.txt", de_DE, new StringSet(
                        "gettext"));

        translations = response.getEntity();
        assertThat(translations.getTextFlowTargets().size(), greaterThan(0));

        // Make sure the headers are now populated
        PoTargetHeader header =
                translations.getExtensions(true).findByType(
                        PoTargetHeader.class);
        assertThat(header, notNullValue());
        assertThat(header.getEntries().size(), greaterThan(0));

        // Make sure the headers have the correct value
        for (HeaderEntry entry : header.getEntries()) {
            if (entry.getKey().equals(HeaderFields.KEY_LastTranslator)) {
                assertThat(entry.getValue().trim(),
                        is("Administrator <root@localhost>"));
            } else if (entry.getKey().equals(HeaderFields.KEY_PoRevisionDate)) {
                assertThat(entry.getValue().trim().length(), greaterThan(0));
            }
        }
    }

    // TODO This test is randomly failing. It should be replaced with an
    // Arquillian integration test where
    // multiple transactions can be reliably tested.
    @Test(enabled = false)
    public void headersAfterWebtransEdit() throws Exception {
        LocaleId de_de = new LocaleId("de");
        getZero();

        // Push a document with no translations
        createResourceWithContentUsingPut();

        super.newSession();
        this.prepareResources(); // Reset Seam as part of new transaction
                                 // simulation

        AccountDAO accountDAO = getSeamAutowire().autowire(AccountDAO.class);

        // Translator
        HAccount translator = accountDAO.getByUsername("demo");

        // Translate using the web editor
        this.simulateWebEditorTranslation("sample-project", "1.0", "my.txt",
                "tf1", translator, de_de, "Translated", ContentState.Approved);

        super.newSession();
        this.prepareResources(); // Reset Seam as part of new transaction
                                 // simulation

        // Fetch the translations again
        ClientResponse<TranslationsResource> response =
                transResource.getTranslations("my.txt", de_de, new StringSet(
                        "gettext"));

        TranslationsResource translations = response.getEntity();
        assertThat(translations.getTextFlowTargets().size(), greaterThan(0));

        // Make sure the headers are present
        PoTargetHeader header =
                translations.getExtensions(true).findByType(
                        PoTargetHeader.class);
        assertThat(header, notNullValue());
        assertThat(header.getEntries().size(), greaterThan(0));

        // Make sure the headers have the correct value
        for (HeaderEntry entry : header.getEntries()) {
            if (entry.getKey().equals(HeaderFields.KEY_LastTranslator)) {
                assertThat(entry.getValue().trim(),
                        is("Sample User <user1@localhost>"));
            } else if (entry.getKey().equals(HeaderFields.KEY_PoRevisionDate)) {
                assertThat(entry.getValue().trim().length(), greaterThan(0));
            }
        }
    }

    @Test
    public void getBadProject() throws Exception {
        ISourceDocResource badSourceResource =
                getClientRequestFactory().createProxy(ISourceDocResource.class,
                        createBaseURI(BAD_RESOURCE_PATH));
        ClientResponse<List<ResourceMeta>> response =
                badSourceResource.get(null);
        assertThat(response.getStatus(), is(404));
    }

    // END of tests

    /**
     * Simulates the translation of a Unit using the web editor. Only suitable
     * for non-plural translations.
     *
     * @param projectSlug
     *            Project
     * @param iterationSlug
     *            Project Iteration
     * @param docId
     *            Document Id
     * @param textFlowContent
     *            Text Flow's content
     * @param translator
     *            The user that does the translation
     * @param localeId
     *            Locale for the translation
     * @param translation
     *            The translated text
     * @param translationState
     *            The new state for the translation (can be null if no changes
     *            are needed)
     */
    private void simulateWebEditorTranslation(String projectSlug,
            String iterationSlug, String docId, String textFlowContent,
            HAccount translator, LocaleId localeId, String translation,
            ContentState translationState) throws Exception {
        SeamAutowire seamAutowire = getSeamAutowire();

        // Mock certain objects
        TranslationWorkspace transWorkspace = mock(TranslationWorkspace.class);

        WorkspaceId workspaceId =
                new WorkspaceId(new ProjectIterationId(projectSlug,
                        iterationSlug, ProjectType.Podir), localeId);

        // Set mock expectations
        when(
                transWorspaceManager
                        .getOrRegisterWorkspace(any(WorkspaceId.class)))
                .thenReturn(transWorkspace);

        // use a given authenticated account
        seamAutowire.use(JpaIdentityStore.AUTHENTICATED_USER, translator);

        // TODO because translationService component has already been created
        // in prepareResource and has a wrong HAccount injected
        seamAutowire.use("translationServiceImpl",
                seamAutowire.autowire(TranslationServiceImpl.class));

        UpdateTransUnitHandler transUnitHandler =
                seamAutowire.autowire(UpdateTransUnitHandler.class);

        // Translation unit id to update
        HTextFlow hTextFlow =
                (HTextFlow) getSession()
                        .createQuery(
                                "select tf from HTextFlow tf where tf.contentHash = ? and "
                                        + "tf.document.docId = ? and "
                                        + "tf.document.projectIteration.slug = ? and "
                                        + "tf.document.projectIteration.project.slug = ?")
                        .setString(0, HashUtil.generateHash(textFlowContent))
                        .setString(1, docId).setString(2, iterationSlug)
                        .setString(3, projectSlug).uniqueResult();
        Long textFlowId = hTextFlow.getId();
        // no previous translation
        int versionNum = 0;

        // Translate using webtrans
        UpdateType updateType = UpdateType.WebEditorSave;
        UpdateTransUnit action =
                new UpdateTransUnit(new TransUnitUpdateRequest(new TransUnitId(
                        textFlowId), Lists.newArrayList(translation),
                        translationState, versionNum), updateType);
        action.setWorkspaceId(workspaceId);

        UpdateTransUnitResult result = transUnitHandler.execute(action, null);

        assertThat(result.isSingleSuccess(), is(true));
        verify(mockIdentity).checkLoggedIn();
        verify(mockIdentity, atLeastOnce()).checkPermission(anyString(),
                any(HLocale.class), any(HProject.class));
        verify(transWorkspace).publish(any(SessionEventData.class));
    }

    private void expectDocs(boolean checkRevs, boolean checkLinksIgnored,
            Resource... docs) {
        expectResourceMetas(checkRevs, docs);
        expectResources(checkRevs, docs);
    }

    private void expectResourceMetas(boolean checkRevs,
            AbstractResourceMeta... docs) {
        ClientResponse<List<ResourceMeta>> response =
                sourceDocResource.get(null);

        assertThat(response.getStatus(), is(200));
        List<ResourceMeta> actualDocs = response.getEntity();
        assertThat(actualDocs, notNullValue());
        Map<String, AbstractResourceMeta> expectedDocs =
                new HashMap<String, AbstractResourceMeta>();
        for (AbstractResourceMeta doc : docs) {
            expectedDocs.put(doc.getName(), doc);
        }
        // Set<String> actualDocVals = new TreeSet<String>();
        Map<String, AbstractResourceMeta> actualDocsMap =
                new HashMap<String, AbstractResourceMeta>();
        for (ResourceMeta doc : actualDocs) {
            actualDocsMap.put(doc.getName(), doc);
            log.debug("actual doc: " + doc.toString());
            AbstractResourceMeta expectedDoc = expectedDocs.get(doc.getName());
            if (checkRevs)
                assertThat(doc.getRevision(), is(expectedDoc.getRevision()));
        }
        assertThat(actualDocsMap.keySet(), is(expectedDocs.keySet()));
    }

    private void expectResources(boolean checkRevs, Resource... docs) {
        for (Resource expectedDoc : docs) {
            if (!checkRevs)
                ResourceTestUtil.clearRevs(expectedDoc);
            ClientResponse<Resource> response =
                    sourceDocResource.getResource(expectedDoc.getName(),
                            extGettextComment);
            assertThat(response.getStatus(), is(200));
            Resource actualDoc = response.getEntity();
            if (!checkRevs)
                ResourceTestUtil.clearRevs(actualDoc);
            createExtensionSets(expectedDoc);
            createExtensionSets(actualDoc);
            assertThat(actualDoc, Matchers.equalTo(expectedDoc));
        }
    }

    protected void createExtensionSets(Resource resource) {
        resource.getExtensions(true);
        for (TextFlow tf : resource.getTextFlows()) {
            tf.getExtensions(true);
        }
    }

    private void dontExpectTarget(String id, LocaleId locale) {
        ClientResponse<TranslationsResource> response =
                transResource.getTranslations(id, locale, null, false, null);
        assertThat(response.getStatus(), is(404));
    }

    private void expectTarget(boolean checkRevs, String id, LocaleId locale,
            TranslationsResource expectedDoc) {
        ClientResponse<TranslationsResource> response =
                transResource.getTranslations(id, locale, extGettextComment);
        assertThat(response.getStatus(), is(200));
        TranslationsResource actualDoc = response.getEntity();
        actualDoc.getLinks(true).clear();
        actualDoc.getExtensions(true);

        for (TextFlowTarget tft : expectedDoc.getTextFlowTargets()) {
            tft.getExtensions(true);
        }

        for (TextFlowTarget tft : actualDoc.getTextFlowTargets()) {
            tft.getExtensions(true);
        }

        expectedDoc.getLinks(true).clear();
        expectedDoc.getExtensions(true);
        if (!checkRevs) {
            ResourceTestUtil.clearRevs(actualDoc);
            ResourceTestUtil.clearRevs(expectedDoc);
        }

        // Clear Po Headers since Zanata will generate custom ones
        ResourceTestUtil.clearPoTargetHeaders(actualDoc, expectedDoc);

        assertThat(RawRestTestUtils.jaxbMarhsal(actualDoc),
                Matchers.equalTo(RawRestTestUtils.jaxbMarhsal(expectedDoc)));
    }

    private Resource createSourceDoc(String name, boolean withTextFlow) {
        Resource resource = new Resource();
        resource.setContentType(ContentType.TextPlain);
        resource.setLang(LocaleId.EN_US);
        resource.setName(name);
        resource.setType(ResourceType.DOCUMENT);

        if (withTextFlow)
            resource.getTextFlows().add(
                    new TextFlow("tf1", LocaleId.EN_US, "hello world"));
        return resource;
    }

    private TranslationsResource createTargetDoc() {
        TranslationsResource trans = new TranslationsResource();
        TextFlowTarget target = new TextFlowTarget();
        target.setContents("hei verden");
        target.setDescription("translation of hello world");
        target.setResId("tf1");
        target.setState(ContentState.Approved);
        Person person = new Person("email@example.com", "Translator Name");
        target.setTranslator(person);
        trans.getTextFlowTargets().add(target);
        return trans;
    }

    private Resource createSourceResource(String name) {
        Resource sr = new Resource(name);
        sr.setContentType(ContentType.TextPlain);
        sr.setLang(LocaleId.EN);
        sr.setType(ResourceType.FILE);
        return sr;
    }

    private void doGetandAssertThatResourceListContainsNItems(int n) {
        ClientResponse<List<ResourceMeta>> resources =
                sourceDocResource.get(null);
        assertThat(resources.getResponseStatus(), is(Status.OK));

        assertThat(resources.getEntity().size(), is(n));
    }

    private Resource newDoc(String id, TextFlow... textFlows) {
        Resource doc = new Resource(id);
        doc.setLang(LocaleId.EN);
        doc.setContentType(ContentType.TextPlain);
        doc.setType(ResourceType.FILE);
        doc.setRevision(null);
        for (TextFlow textFlow : textFlows) {
            doc.getTextFlows().add(textFlow);
        }
        return doc;
    }

    private TextFlow newTextFlow(String id, String sourceContent,
            String sourceComment) {
        TextFlow textFlow = new TextFlow(id, LocaleId.EN);
        textFlow.setContents(sourceContent);
        if (sourceComment != null)
            getOrAddComment(textFlow).setValue(sourceComment);
        return textFlow;
    }

    private TextFlowTarget newTextFlowTarget(String id, String targetContent,
            String targetComment) {
        TextFlowTarget target = new TextFlowTarget();
        target.setResId(id);
        target.setState(ContentState.Approved);
        target.setContents(targetContent);
        if (targetComment != null)
            getOrAddComment(target).setValue(targetComment);
        return target;
    }

    SimpleComment getOrAddComment(TextFlow tf) {
        return tf.getExtensions(true).findOrAddByType(SimpleComment.class);
    }

    SimpleComment getOrAddComment(TextFlowTarget tft) {
        return tft.getExtensions(true).findOrAddByType(SimpleComment.class);
    }

    private Resource putPo1() {
        String id = "foo.pot";
        TextFlow textflow = newTextFlow("FOOD", "Slime Mould", "POT comment");
        PotEntryHeader poData =
                textflow.getExtensions(true).findOrAddByType(
                        PotEntryHeader.class);
        poData.setContext("context");
        List<String> flags = poData.getFlags();
        flags.add("no-c-format");
        flags.add("flag2");
        List<String> refs = poData.getReferences();
        refs.add("ref1.xml:7");
        refs.add("ref1.xml:21");

        Resource doc = newDoc(id, textflow);
        PoHeader poHeader = new PoHeader();
        poHeader.setComment("poheader comment");
        List<HeaderEntry> poEntries = poHeader.getEntries();
        poEntries.add(new HeaderEntry("Project-Id-Version", "en"));
        poEntries.add(new HeaderEntry("Content-Type",
                "application/x-publican; charset=UTF-8\n"));
        doc.getExtensions(true).add(poHeader);

        log.debug("{}", doc);
        Response response =
                sourceDocResource.putResource(id, doc, extGettextComment);
        assertThat(response.getStatus(), isOneOf(200, 201));
        return doc;
    }

    private TranslationsResource putPoTarget1() {
        String id = "foo.pot";
        TranslationsResource tr = new TranslationsResource();
        TextFlowTarget target =
                newTextFlowTarget("FOOD", "Sauerkraut", "translator comment");
        tr.getTextFlowTargets().add(target);

        PoTargetHeader targetHeader = new PoTargetHeader();
        targetHeader.setComment("target comment");
        List<HeaderEntry> entries = targetHeader.getEntries();
        entries.add(new HeaderEntry("Project-Id-Version", "ja"));
        tr.getExtensions(true).add(targetHeader);

        transResource.putTranslations(id, DE, tr, extGettextComment);

        return tr;
    }

    private Resource putDoc1(boolean putTarget) {
        String id = DOC1_NAME;
        Resource doc =
                newDoc(id,
                        newTextFlow("FOOD", "Slime Mould",
                                "slime mould comment"));
        Response response = sourceDocResource.putResource(id, doc, extComment);
        assertThat(response.getStatus(), isOneOf(200, 201));

        if (putTarget)
            putTarget1();

        return doc;
    }

    protected TranslationsResource putTarget1() {
        String id = DOC1_NAME;
        TranslationsResource tr = new TranslationsResource();
        TextFlowTarget target = newTextFlowTarget("FOOD", "Sauerkraut", null);
        tr.getTextFlowTargets().add(target);
        transResource.putTranslations(id, DE, tr, extGettextComment);
        return tr;
    }

    private void dontExpectTarget1() {
        String id = DOC1_NAME;
        dontExpectTarget(id, DE);
    }

    private void expectTarget1(TranslationsResource target1) {
        String id = DOC1_NAME;
        expectTarget(true, id, DE, target1);
    }

    private Resource putDoc1a(boolean putTarget) {
        String id = DOC1_NAME;
        Resource doc = newDoc(id, newTextFlow("HELLO", "Hello World", null));
        Response response = sourceDocResource.putResource(id, doc, extComment);
        assertThat(response.getStatus(), isOneOf(200, 201));

        if (putTarget)
            putTarget1a();

        return doc;
    }

    protected TranslationsResource putTarget1a() {
        String id = DOC1_NAME;
        TranslationsResource tr = new TranslationsResource();
        TextFlowTarget target =
                newTextFlowTarget("HELLO", "Bonjour le Monde",
                        "bon jour comment");
        tr.getTextFlowTargets().add(target);
        transResource.putTranslations(id, FR, tr, extGettextComment);
        return tr;
    }

    private void dontExpectTarget1a() {
        String id = DOC1_NAME;
        dontExpectTarget(id, FR);
    }

    private void expectTarget1a(TranslationsResource target1a) {
        String id = DOC1_NAME;
        expectTarget(true, id, FR, target1a);
    }

    private void deleteDoc1() {
        deleteDoc(DOC1_NAME);
    }

    private void deleteDoc2() {
        deleteDoc(DOC2_NAME);
    }

    protected void deleteDoc(String id) {
        Response response = sourceDocResource.deleteResource(id);
        assertThat(response.getStatus(), is(200));
    }

    private Resource postDoc2(boolean putTarget) {
        String id = DOC2_NAME;
        Resource doc =
                newDoc(id, newTextFlow("HELLO", "Hello World", "hello comment"));
        Response response = sourceDocResource.post(doc, extComment, true);
        assertThat(response.getStatus(), is(201));

        if (putTarget)
            putTarget2();

        return doc;
    }

    protected TranslationsResource putTarget2() {
        String id = DOC2_NAME;
        TranslationsResource tr = new TranslationsResource();
        TextFlowTarget target =
                newTextFlowTarget("HELLO", "Bonjour le Monde", null);
        tr.getTextFlowTargets().add(target);
        transResource.putTranslations(id, FR, tr, extGettextComment);
        return tr;
    }

    private void dontExpectTarget2() {
        String id = DOC2_NAME;
        dontExpectTarget(id, FR);
    }

    private void expectTarget2(TranslationsResource target2) {
        String id = DOC2_NAME;
        expectTarget(true, id, FR, target2);
    }

    private void verifyObsoleteDocument(final String docID) throws Exception {
        ProjectIterationDAO projectIterationDAO =
                getSeamAutowire().autowire(ProjectIterationDAO.class);
        HProjectIteration iteration =
                projectIterationDAO.getBySlug(projectSlug, iter);
        Map<String, HDocument> allDocuments = iteration.getAllDocuments();
        HDocument hDocument = allDocuments.get(docID);
        // FIXME hDocument is coming back null
        // Assert.assertNotNull(hDocument);
        // Assert.assertTrue(hDocument.isObsolete());
    }

    private void verifyObsoleteResource(final String docID,
            final String resourceID) throws Exception {
        ProjectIterationDAO projectIterationDAO =
                getSeamAutowire().autowire(ProjectIterationDAO.class);
        HProjectIteration iteration =
                projectIterationDAO.getBySlug(projectSlug, iter);
        Map<String, HDocument> allDocuments = iteration.getAllDocuments();
        HDocument hDocument = allDocuments.get(docID);
        // FIXME hDocument is coming back null
        // HTextFlow hResource = hDocument.getAllTextFlows().get(resourceID);
        // Assert.assertNotNull(hResource);
        // Assert.assertTrue(hResource.isObsolete());
    }
}
