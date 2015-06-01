package org.zanata.service.impl;

import com.binarytweed.test.Quarantine;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.zanata.common.MergeType;
import org.zanata.model.HTextFlowTarget;
import org.zanata.seam.SeamAutowire;
import org.zanata.service.TranslationMergeService;
import org.zanata.test.QuarantiningRunner;

import static org.hamcrest.MatcherAssert.*;

/**
 * @author Patrick Huang <a
 *         href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
@Quarantine({ "org.jboss.seam" })
@RunWith(QuarantiningRunner.class)
public class TranslationMergeServiceFactoryTest {
    private TranslationMergeServiceFactory factory;

    @Before
    public void before() {
        factory =
                SeamAutowire.instance().reset().ignoreNonResolvable()
                        .autowire(TranslationMergeServiceFactory.class);
    }

    @Test
    public void getMergeServiceWhenServerHasNoTarget() {
        TranslationMergeServiceFactory.MergeContext mergeContext =
                new TranslationMergeServiceFactory.MergeContext(null, null,
                        null, null, 1);
        TranslationMergeService result = factory.getMergeService(mergeContext);

        assertThat(result, Matchers.instanceOf(TranslationMergeFirstTran.class));
    }

    @Test
    public void getMergeServiceWhenServerHasTargetAndMergeTypeIsAuto() {
        TranslationMergeServiceFactory.MergeContext mergeContext =
                new TranslationMergeServiceFactory.MergeContext(MergeType.AUTO,
                        null, null, new HTextFlowTarget(), 1);
        TranslationMergeService result = factory.getMergeService(mergeContext);

        assertThat(result, Matchers.instanceOf(TranslationMergeAuto.class));
    }

    @Test
    public void getMergeServiceWhenServerHasTargetAndMergeTypeIsImport() {
        TranslationMergeServiceFactory.MergeContext mergeContext =
                new TranslationMergeServiceFactory.MergeContext(
                        MergeType.IMPORT, null, null, new HTextFlowTarget(), 1);
        TranslationMergeService result = factory.getMergeService(mergeContext);

        assertThat(result, Matchers.instanceOf(TranslationMergeImport.class));
    }

}
