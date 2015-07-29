package org.zanata.webtrans.server.rpc;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.List;

import org.dbunit.operation.DatabaseOperation;
import org.hamcrest.Matchers;
import org.zanata.seam.security.ZanataJpaIdentityStore;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.zanata.ZanataDbunitJpaTest;
import org.zanata.dao.AccountDAO;
import org.zanata.dao.AccountOptionDAO;
import org.zanata.model.HAccount;
import org.zanata.model.HAccountOption;
import org.zanata.seam.SeamAutowire;
import org.zanata.webtrans.client.presenter.UserConfigHolder;
import org.zanata.webtrans.shared.model.UserOptions;
import org.zanata.webtrans.shared.rpc.LoadOptionsAction;
import org.zanata.webtrans.shared.rpc.LoadOptionsResult;
import org.zanata.webtrans.shared.rpc.NavOption;
import org.zanata.webtrans.shared.rpc.SaveOptionsAction;

/**
 * @author Patrick Huang <a
 *         href="mailto:pahuang@redhat.com">pahuang@redhat.com</a>
 */
public class LoadOptionsHandlerTest extends ZanataDbunitJpaTest {
    private LoadOptionsHandler handler;
    private SaveOptionsHandler saveHandler;

    private final static SeamAutowire seam = SeamAutowire.instance();

    @Override
    protected void prepareDBUnitOperations() {
        beforeTestOperations.add(new DataSetOperation(
                "org/zanata/test/model/AccountData.dbunit.xml",
                DatabaseOperation.CLEAN_INSERT));

        afterTestOperations.add(new DataSetOperation(
                "org/zanata/test/model/ClearAllTables.dbunit.xml",
                DatabaseOperation.DELETE_ALL));
    }

    @Before
    public void setUp() throws Exception {
        AccountDAO accountDAO = new AccountDAO(getSession());
        AccountOptionDAO accountOptionDAO = new AccountOptionDAO(getSession());
        HAccount authenticatedAccount = getEm().find(HAccount.class, 1L);
        // @formatter:off
        handler = seam
            .reset()
            .use(ZanataJpaIdentityStore.AUTHENTICATED_USER, authenticatedAccount)
            .use("accountDAO", accountDAO)
            .autowire(LoadOptionsHandler.class);

        saveHandler = SeamAutowire.instance()
            .reset()
            .use(ZanataJpaIdentityStore.AUTHENTICATED_USER, authenticatedAccount)
            .use("accountDAO", accountDAO)
            .use("accountOptionDAO", accountOptionDAO)
            .autowire(SaveOptionsHandler.class);
        // @formatter:on
    }

    private HashMap<UserOptions, String> generateConfigMap(
            UserConfigHolder configHolder) {
        HashMap<UserOptions, String> configMap =
                new HashMap<UserOptions, String>();
        configMap.put(UserOptions.DisplayButtons,
                Boolean.toString(configHolder.getState().isDisplayButtons()));
        configMap.put(UserOptions.EnterSavesApproved, Boolean
                .toString(configHolder.getState().isEnterSavesApproved()));
        configMap.put(UserOptions.EditorPageSize,
                Integer.toString(configHolder.getState().getEditorPageSize()));
        configMap.put(UserOptions.DocumentListPageSize,
                Integer.toString(configHolder.getState().getEditorPageSize()));

        configMap.put(UserOptions.TranslatedMessageFilter, Boolean
                .toString(configHolder.getState().isFilterByTranslated()));
        configMap.put(UserOptions.FuzzyMessageFilter,
                Boolean.toString(configHolder.getState().isFilterByFuzzy()));
        configMap.put(UserOptions.UntranslatedMessageFilter, Boolean
                .toString(configHolder.getState().isFilterByUntranslated()));
        configMap.put(UserOptions.Navigation, configHolder.getState()
                .getNavOption().toString());

        configMap.put(UserOptions.ShowErrors,
                Boolean.toString(configHolder.getState().isShowError()));
        configMap.put(UserOptions.UseCodeMirrorEditor, Boolean
                .toString(configHolder.getState().isUseCodeMirrorEditor()));
        return configMap;
    }

    @Test
    public void testExecuteWithOptionsInDatabase() throws Exception {
        UserConfigHolder configHolder = new UserConfigHolder();
        configHolder.setShowError(true); // we change one default value
        SaveOptionsAction action =
                new SaveOptionsAction(generateConfigMap(configHolder));
        saveHandler.execute(action, null); // save some options first

        LoadOptionsResult result =
                handler.execute(new LoadOptionsAction(null), null);

        assertThat(result.getConfiguration().isShowError(),
                Matchers.equalTo(true));
        assertThat(result.getConfiguration().getNavOption(),
                Matchers.equalTo(NavOption.FUZZY_UNTRANSLATED));
        assertThat(result.getConfiguration().isDisplayButtons(),
                Matchers.equalTo(true));
    }

    @Test
    public void testExecuteWithNoOptionsInDatabase() throws Exception {
        // clear data result from testExecuteWithOptionsInDatabase()
        getEm().createQuery("Delete from HAccountOption").executeUpdate();

        // given: no options in database
        List<HAccountOption> options =
                getEm().createQuery("from HAccountOption").getResultList();
        assertThat(options, Matchers.<HAccountOption> empty());

        LoadOptionsResult result =
                handler.execute(new LoadOptionsAction(null), null);

        // then: we get back default values
        assertThat(result.getConfiguration().isShowError(),
                Matchers.equalTo(false));
        assertThat(result.getConfiguration().getNavOption(),
                Matchers.equalTo(NavOption.FUZZY_UNTRANSLATED));
        assertThat(result.getConfiguration().getEditorPageSize(),
                Matchers.equalTo(25));
    }

    @Test
    public void testRollback() throws Exception {
        handler.rollback(null, null, null);
    }
}
