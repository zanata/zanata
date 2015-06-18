package org.zanata.rest.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.ws.rs.core.MediaType;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.jboss.resteasy.core.Headers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zanata.ZanataTest;
import org.zanata.rest.dto.DTOUtil;
import org.zanata.rest.dto.VersionInfo;
import org.zanata.rest.dto.resource.Resource;
import org.zanata.rest.dto.resource.ResourceMeta;
import org.zanata.rest.dto.resource.TranslationsResource;
import org.zanata.seam.SeamAutowire;
import com.allen_sauer.gwt.log.client.Log;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(DataProviderRunner.class)
public class RestUtilsTest extends ZanataTest {
    RestUtils restUtils;

    private static ResourceTestObjectFactory resourceTestFactory =
            new ResourceTestObjectFactory();
    private static TranslationsResourceTestObjectFactory transTestFactory =
            new TranslationsResourceTestObjectFactory();
    private final Logger log = LoggerFactory.getLogger(RestUtilsTest.class);

    @Before
    public void prepareSeam() {
        ValidatorFactory validatorFactory =
                Validation.buildDefaultValidatorFactory();

        SeamAutowire seam = SeamAutowire.instance();
        restUtils =
                seam.reset().use("validatorFactory", validatorFactory)
                        .use("validator", validatorFactory.getValidator())
                        .autowire(RestUtils.class);
    }

    @DataProvider
    public static Object[][] resourceTestData() {
        return new Object[][] {
                new Object[] { "getPoHeaderTest",
                        resourceTestFactory.getPoHeaderTest() },
                new Object[] { "getPotEntryHeaderTest",
                        resourceTestFactory.getPotEntryHeaderTest() },
                new Object[] { "getTextFlowCommentTest",
                        resourceTestFactory.getTextFlowCommentTest() },
                new Object[] { "getTextFlowTest2",
                        resourceTestFactory.getTextFlowTest2() } };
    }

    @DataProvider
    public static Object[][] resourceMetaTestData() {
        return new Object[][] {
                new Object[] { "getResourceMeta",
                        resourceTestFactory.getResourceMeta() },
                new Object[] { "getPoHeaderResourceMeta",
                        resourceTestFactory.getPoHeaderResourceMeta() } };
    }

    @DataProvider
    public static Object[][] translationTestData() {
        return new Object[][] {
                new Object[] { "getPoTargetHeaderTextFlowTargetTest",
                        transTestFactory.getPoTargetHeaderTextFlowTargetTest() },
                new Object[] { "getTestObject",
                        transTestFactory.getTestObject() },
                new Object[] { "getTestObject2",
                        transTestFactory.getTestObject2() },
                new Object[] { "getTextFlowTargetCommentTest",
                        transTestFactory.getTextFlowTargetCommentTest() } };
    }

    @Test
    @UseDataProvider("resourceTestData")
    public void testUnmarshallResource(String desc, Resource res)
            throws UnsupportedEncodingException {
        // SeamMockClientExecutor test = new SeamMockClientExecutor();
        // ClientRequest client = test.createRequest("http://example.com/");
        // MultivaluedMap<String, String> header = client.getHeaders();
        testRestUtilUnmarshall(res, Resource.class);
    }

    private <T extends Serializable> void testRestUtilUnmarshall(T entity,
            Class<T> type) throws UnsupportedEncodingException {
        InputStream messageBody = null;
        try {
            String testStr = DTOUtil.toXML(entity);
            log.info("expect:" + testStr);

            messageBody = new ByteArrayInputStream(testStr.getBytes("UTF-8"));
            T unmarshall =
                    restUtils.unmarshall(type, messageBody,
                            MediaType.APPLICATION_XML_TYPE,
                            new Headers<String>());
            Log.info("got:" + DTOUtil.toXML(unmarshall));
            assertThat(DTOUtil.toXML(entity), is(testStr));
        } finally {
            if (messageBody != null) {
                try {
                    messageBody.close();
                } catch (IOException e) {
                }
            }
        }
    }

    @Test
    public void testUnmarshallJasonTranslationsResource() {
        log.info("start jason");
        InputStream messageBody = null;
        try {
            // String testStr =
            // "{\"resId\":\"782f49c4e93c32403ba0b51821b38b90\",\"state\":\"Approved\",\"translator\":{},\"content\":\"title: ttff\",\"extensions\":[{\"object-type\": \"comment\",\"value\": \"testcomment\", \"space\": \"preserve\"}]}";
            String testStr =
                    "{\"textFlowTargets\":["
                            + "{\"resId\":\"rest1\","
                            + "\"state\":\"Approved\","
                            + "\"translator\":{"
                            + "\"email\":\"root@localhost\","
                            + "\"name\":\"Admin user\"},"
                            + "\"content\": \"<title>\u8bbf\u95ee\u5b58\u53d6\u63a7\u5236\u5217\u8868</title>\"},"
                            + "{\"resId\":\"rest2\","
                            + "\"state\":\"Approved\"," + "\"translator\":{"
                            + "\"email\":\"root@localhost\","
                            + "\"name\":\"Admin user\"},"
                            + "\"content\":\"hello world\"}]}";

            messageBody = new ByteArrayInputStream(testStr.getBytes("UTF-8"));
            TranslationsResource unmarshall =
                    restUtils.unmarshall(TranslationsResource.class,
                            messageBody, MediaType.APPLICATION_JSON_TYPE, null);
            log.info("got:" + unmarshall.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } finally {
            if (messageBody != null) {
                try {
                    messageBody.close();
                } catch (IOException e) {
                }
            }
        }
    }

    @Test
    @UseDataProvider("translationTestData")
    public void
            testUnmarshallTranslation(String desc, TranslationsResource res)
                    throws UnsupportedEncodingException {
        testRestUtilUnmarshall(res, TranslationsResource.class);
    }

    @Test
    @UseDataProvider("resourceMetaTestData")
    public void testUnmarshallResourceMeta(String desc, ResourceMeta res)
            throws UnsupportedEncodingException {
        testRestUtilUnmarshall(res, ResourceMeta.class);
    }

    @Test
    public void testVersion() throws UnsupportedEncodingException {
        VersionInfo ver = new VersionInfo(null, null, null);
        testRestUtilUnmarshall(ver, VersionInfo.class);
    }

}
