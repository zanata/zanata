package org.zanata.webtrans.client.presenter;

import static org.easymock.EasyMock.and;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import java.util.ArrayList;

import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.presenter.client.EventBus;

import org.easymock.Capture;
import org.easymock.IAnswer;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.zanata.webtrans.client.events.EnterWorkspaceEvent;
import org.zanata.webtrans.client.events.EnterWorkspaceEventHandler;
import org.zanata.webtrans.client.events.ExitWorkspaceEvent;
import org.zanata.webtrans.client.events.ExitWorkspaceEventHandler;
import org.zanata.webtrans.client.events.NativeEvent;
import org.zanata.webtrans.client.presenter.TranslationPresenter.Display;
import org.zanata.webtrans.client.resources.WebTransMessages;
import org.zanata.webtrans.client.rpc.CachingDispatchAsync;
import org.zanata.webtrans.shared.model.Person;
import org.zanata.webtrans.shared.model.PersonId;
import org.zanata.webtrans.shared.model.TransUnit;
import org.zanata.webtrans.shared.rpc.GetTranslatorList;
import org.zanata.webtrans.shared.rpc.GetTranslatorListResult;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasValue;


@Test(groups = { "unit-tests" })
public class TranslationPresenterTest
{

   private static final String TEST_USERS_ONLINE_MESSAGE = "some users online";
   private static final String TEST_SHOW_OPTIONS_TOOLTIP = "tooltip to show options";
   private static final String TEST_HIDE_OPTIONS_TOOLTIP = "tooltip to hide options";

   // object under test
   private TranslationPresenter translationPresenter;

   // mock injected entities
   private CachingDispatchAsync mockDispatcher;
   private Display mockDisplay;
   private EventBus mockEventBus;
   private GlossaryPresenter mockGlossaryPresenter;
   private WebTransMessages mockMessages;
   private NativeEvent mockNativeEvent;
   private SidePanelPresenter mockSidePanelPresenter;
   private TranslationEditorPresenter mockTranslationEditorPresenter;
   private TransMemoryPresenter mockTransMemoryPresenter;
   private WorkspaceUsersPresenter mockWorkspaceUsersPresenter;

   // mock view components
   private HasValue<Boolean> mockOptionsToggle;
   private HasValue<Boolean> mockSouthPanelToggle;


   private Capture<EnterWorkspaceEventHandler> capturedEnterWorkspaceEventHandler;
   private Capture<ExitWorkspaceEventHandler> capturedExitWorkspaceEventHandler;
   private Capture<GetTranslatorList> capturedTranslatorListRequest;
   private Capture<AsyncCallback<GetTranslatorListResult>> capturedTranslatorListRequestCallback;
   private Capture<ValueChangeHandler<Boolean>> capturedOptionsToggleValueChangeHandler;
   private Capture<ValueChangeHandler<Boolean>> capturedSouthPanelToggleValueChangeHandler;

   private Capture<NativePreviewHandler> capturedKeyShortcutHandler;

   @SuppressWarnings("unchecked")
   @BeforeClass
   public void createMocks()
   {
      mockDispatcher = createMock(CachingDispatchAsync.class);
      mockDisplay = createMock(TranslationPresenter.Display.class);
      mockEventBus = createMock(EventBus.class);
      mockGlossaryPresenter = createMock(GlossaryPresenter.class);
      mockMessages = createMock(WebTransMessages.class);
      mockNativeEvent = createMock(NativeEvent.class);
      mockSidePanelPresenter = createMock(SidePanelPresenter.class);
      mockTranslationEditorPresenter = createMock(TranslationEditorPresenter.class);
      mockTransMemoryPresenter = createMock(TransMemoryPresenter.class);
      mockWorkspaceUsersPresenter = createMock(WorkspaceUsersPresenter.class);

      mockOptionsToggle = createMock(HasValue.class);
      mockSouthPanelToggle = createMock(HasValue.class);
   }

   private TranslationPresenter newTranslationPresenter()
   {
      return new TranslationPresenter(mockDisplay, mockEventBus, mockDispatcher, mockWorkspaceUsersPresenter, mockTranslationEditorPresenter, mockSidePanelPresenter, mockTransMemoryPresenter, mockGlossaryPresenter, mockMessages, mockNativeEvent);
   }


   @Test
   public void performsRequiredActionsOnBind()
   {
      setupAndBindPresenter();
      verifyAllMocks();
   }

   private void setupAndBindPresenter()
   {
      resetAllMocks();
      setupDefaultMockExpectations();
      // default mock expectations include:
      // - bind 5 sub-presenters
      // - request & update participant list

      replayAllMocks();
      translationPresenter = newTranslationPresenter();
      translationPresenter.bind();
   }


   @Test
   public void hidesOptionsPanel()
   {
      setupAndBindPresenter();

      reset(mockDisplay);
      mockDisplay.setOptionsToggleTooltip(TEST_SHOW_OPTIONS_TOOLTIP);
      expectLastCall().once();
      mockDisplay.setSidePanelVisible(false);
      expectLastCall().once();
      replay(mockDisplay);

      // simulate options toggle released
      @SuppressWarnings("unchecked")
      ValueChangeEvent<Boolean> optionsToggleDeactivated = createMock(ValueChangeEvent.class);
      expect(optionsToggleDeactivated.getValue()).andReturn(false).anyTimes();
      replay(optionsToggleDeactivated);
      capturedOptionsToggleValueChangeHandler.getValue().onValueChange(optionsToggleDeactivated);

      verify(mockDisplay);
   }

   @Test
   public void showsOptionsPanel()
   {
      setupAndBindPresenter();

      reset(mockDisplay);
      // simulate options panel hidden
      mockDisplay.setOptionsToggleTooltip(TEST_HIDE_OPTIONS_TOOLTIP);
      expectLastCall().once();
      mockDisplay.setSidePanelVisible(true);
      expectLastCall().once();
      replay(mockDisplay);

      // simulate options toggle depressed
      @SuppressWarnings("unchecked")
      ValueChangeEvent<Boolean> optionsToggleActivated = createMock(ValueChangeEvent.class);
      expect(optionsToggleActivated.getValue()).andReturn(true).anyTimes();
      replay(optionsToggleActivated);
      capturedOptionsToggleValueChangeHandler.getValue().onValueChange(optionsToggleActivated);

      verify(mockDisplay);
   }

   @Test
   public void hidesSouthPanel()
   {
      setupAndBindPresenter();

      reset(mockDisplay, mockTransMemoryPresenter, mockGlossaryPresenter, mockWorkspaceUsersPresenter);
      // doesn't set tooltip like options toggle
      mockDisplay.setSouthPanelVisible(false);
      expectLastCall().once();

      // should unbind sub-presenters when hiding
      mockTransMemoryPresenter.unbind();
      expectLastCall().once();
      mockGlossaryPresenter.unbind();
      expectLastCall().once();
      mockWorkspaceUsersPresenter.unbind();
      expectLastCall().once();

      replay(mockDisplay, mockTransMemoryPresenter, mockGlossaryPresenter, mockWorkspaceUsersPresenter);

      // simulate south panel toggle released
      @SuppressWarnings("unchecked")
      ValueChangeEvent<Boolean> southPanelToggleDeactivated = createMock(ValueChangeEvent.class);
      expect(southPanelToggleDeactivated.getValue()).andReturn(false).anyTimes();
      replay(southPanelToggleDeactivated);
      capturedSouthPanelToggleValueChangeHandler.getValue().onValueChange(southPanelToggleDeactivated);

      verify(mockDisplay, mockTransMemoryPresenter, mockGlossaryPresenter, mockWorkspaceUsersPresenter);
   }

   @Test
   public void showsSouthPanel()
   {
      setupAndBindPresenter();

      reset(mockDisplay, mockTransMemoryPresenter, mockGlossaryPresenter, mockWorkspaceUsersPresenter, mockTranslationEditorPresenter);
      // doesn't set tooltip like options toggle
      mockDisplay.setSouthPanelVisible(true);
      expectLastCall().once();

      // should re-bind sub-presenters when showing
      mockTransMemoryPresenter.bind();
      expectLastCall().once();
      mockGlossaryPresenter.bind();
      expectLastCall().once();
      mockWorkspaceUsersPresenter.bind();
      expectLastCall().once();

      // simulate no TU selected (ideally this would be the responsibility of
      // TransMemoryPresenter, not the class under test)
      expect(mockTranslationEditorPresenter.getSelectedTransUnit()).andReturn(null);
      // should not call this for null TU selected:
      // mockTransMemoryPresenter.showResultsFor(null);

      replay(mockDisplay, mockTransMemoryPresenter, mockGlossaryPresenter, mockWorkspaceUsersPresenter);

      // simulate south panel toggle depressed
      @SuppressWarnings("unchecked")
      ValueChangeEvent<Boolean> southPanelToggleDeactivated = createMock(ValueChangeEvent.class);
      expect(southPanelToggleDeactivated.getValue()).andReturn(true).anyTimes();
      replay(southPanelToggleDeactivated);
      capturedSouthPanelToggleValueChangeHandler.getValue().onValueChange(southPanelToggleDeactivated);

      verify(mockDisplay, mockTransMemoryPresenter, mockGlossaryPresenter, mockWorkspaceUsersPresenter);
   }

   /**
    * similar to showsSouthPanel() but with non-null selected TU
    */
   @Test
   public void fireTMSearchOnShowSouthPanel()
   {
      setupAndBindPresenter();

      reset(mockDisplay, mockTransMemoryPresenter, mockGlossaryPresenter, mockWorkspaceUsersPresenter, mockTranslationEditorPresenter);
      // doesn't set tooltip like options toggle
      mockDisplay.setSouthPanelVisible(true);
      expectLastCall().once();

      // should re-bind sub-presenters when showing
      mockTransMemoryPresenter.bind();
      expectLastCall().once();
      mockGlossaryPresenter.bind();
      expectLastCall().once();
      mockWorkspaceUsersPresenter.bind();
      expectLastCall().once();

      // simulate some TU currently selected (ideally this would be the
      // responsibility of TransMemoryPresenter, not the class under test)
      TransUnit mockTU = createMock(TransUnit.class);
      expect(mockTranslationEditorPresenter.getSelectedTransUnit()).andReturn(mockTU);
      // should not call this for null TU selected:
      mockTransMemoryPresenter.showResultsFor(mockTU);
      expectLastCall().once();

      replay(mockDisplay, mockTransMemoryPresenter, mockGlossaryPresenter, mockWorkspaceUsersPresenter, mockTranslationEditorPresenter);

      // simulate south panel toggle depressed
      @SuppressWarnings("unchecked")
      ValueChangeEvent<Boolean> southPanelToggleDeactivated = createMock(ValueChangeEvent.class);
      expect(southPanelToggleDeactivated.getValue()).andReturn(true).anyTimes();
      replay(southPanelToggleDeactivated);
      capturedSouthPanelToggleValueChangeHandler.getValue().onValueChange(southPanelToggleDeactivated);

      verify(mockDisplay, mockTransMemoryPresenter, mockGlossaryPresenter, mockWorkspaceUsersPresenter, mockTranslationEditorPresenter);
   }

   @Test
   public void updateParticipantsOnEnterWorkspace()
   {
      setupAndBindPresenter();

      reset(mockDispatcher, mockDisplay, mockMessages, mockWorkspaceUsersPresenter);

      // expect lookup translator list
      ArrayList<Person> participants = new ArrayList<Person>();
      participants.add(new Person(new PersonId("bob"), "Bob Smith"));
      participants.add(new Person(new PersonId("smith"), "Smith Bob"));
      setupUserListRequestResponse(participants);

      replay(mockDispatcher, mockDisplay, mockMessages, mockWorkspaceUsersPresenter);

      // simulate enter workspace event
      EnterWorkspaceEvent event = createMock(EnterWorkspaceEvent.class);
      capturedEnterWorkspaceEventHandler.getValue().onEnterWorkspace(event);

      verify(mockDispatcher, mockDisplay, mockMessages, mockWorkspaceUsersPresenter);
   }

   @Test
   public void updateParticipantsOnExitWorkspace()
   {
      setupAndBindPresenter();

      reset(mockDispatcher, mockDisplay, mockMessages, mockWorkspaceUsersPresenter);

      // expect lookup translator list
      ArrayList<Person> participants = new ArrayList<Person>();
      participants.add(new Person(new PersonId("john"), "John Jones"));
      participants.add(new Person(new PersonId("jones"), "Jones John"));
      participants.add(new Person(new PersonId("jim"), "Jim Jones"));
      setupUserListRequestResponse(participants);

      replay(mockDispatcher, mockDisplay, mockMessages, mockWorkspaceUsersPresenter);

      // simulate enter workspace event
      ExitWorkspaceEvent event = createMock(ExitWorkspaceEvent.class);
      capturedExitWorkspaceEventHandler.getValue().onExitWorkspace(event);

      verify(mockDispatcher, mockDisplay, mockMessages, mockWorkspaceUsersPresenter);
   }

   // TODO test failed participants list request (what behaviour is desired
   // here?)

   // TODO test key bindings

   private void setupDefaultMockExpectations()
   {
      ArrayList<Person> people = new ArrayList<Person>();
      people.add(new Person(new PersonId("jones"), "Joey Jones"));

      setupDefaultMockExpectations(people);
   }

   @SuppressWarnings("unchecked")
   private void setupDefaultMockExpectations(ArrayList<Person> initialParticipants)
   {
      mockTransMemoryPresenter.bind();
      expectLastCall().once();
      mockWorkspaceUsersPresenter.bind();
      expectLastCall().once();
      mockGlossaryPresenter.bind();
      expectLastCall().once();
      mockTranslationEditorPresenter.bind();
      expectLastCall().once();
      mockSidePanelPresenter.bind();
      expectLastCall().once();

      expect(mockMessages.showEditorOptions()).andReturn(TEST_SHOW_OPTIONS_TOOLTIP).anyTimes();
      expect(mockMessages.hideEditorOptions()).andReturn(TEST_HIDE_OPTIONS_TOOLTIP).anyTimes();

      capturedEnterWorkspaceEventHandler = new Capture<EnterWorkspaceEventHandler>();
      expect(mockEventBus.addHandler(eq(EnterWorkspaceEvent.getType()), and(capture(capturedEnterWorkspaceEventHandler), isA(EnterWorkspaceEventHandler.class)))).andReturn(createMock(HandlerRegistration.class)).once();
      capturedExitWorkspaceEventHandler = new Capture<ExitWorkspaceEventHandler>();
      expect(mockEventBus.addHandler(eq(ExitWorkspaceEvent.getType()), and(capture(capturedExitWorkspaceEventHandler), isA(ExitWorkspaceEventHandler.class)))).andReturn(createMock(HandlerRegistration.class)).once();

      setupUserListRequestResponse(initialParticipants);
      
      expect(mockDisplay.getOptionsToggle()).andReturn(mockOptionsToggle).anyTimes();
      capturedOptionsToggleValueChangeHandler = new Capture<ValueChangeHandler<Boolean>>();
      expect(mockOptionsToggle.addValueChangeHandler(and(capture(capturedOptionsToggleValueChangeHandler), isA(ValueChangeHandler.class)))).andReturn(createMock(HandlerRegistration.class)).once();

      expect(mockDisplay.getSouthPanelToggle()).andReturn(mockSouthPanelToggle).anyTimes();
      capturedSouthPanelToggleValueChangeHandler = new Capture<ValueChangeHandler<Boolean>>();
      expect(mockSouthPanelToggle.addValueChangeHandler(and(capture(capturedSouthPanelToggleValueChangeHandler), isA(ValueChangeHandler.class)))).andReturn(createMock(HandlerRegistration.class)).once();

      capturedKeyShortcutHandler = new Capture<NativePreviewHandler>();
      expect(mockNativeEvent.addNativePreviewHandler(and(capture(capturedKeyShortcutHandler), isA(NativePreviewHandler.class)))).andReturn(createMock(HandlerRegistration.class)).once();
   }

   /**
    * @param participants
    */
   @SuppressWarnings("unchecked")
   private void setupUserListRequestResponse(ArrayList<Person> participants)
   {
      capturedTranslatorListRequest = new Capture<GetTranslatorList>();
      capturedTranslatorListRequestCallback = new Capture<AsyncCallback<GetTranslatorListResult>>();
      mockDispatcher.execute(and(capture(capturedTranslatorListRequest), isA(Action.class)), and(capture(capturedTranslatorListRequestCallback), isA(AsyncCallback.class)));
      expectLastCall().andAnswer(new TranslatorListSuccessAnswer(participants)).once();

      mockWorkspaceUsersPresenter.setUserList(participants);
      expectLastCall().once(); // once for now

      expect(mockMessages.nUsersOnline(participants.size())).andReturn(TEST_USERS_ONLINE_MESSAGE).anyTimes();
      mockDisplay.setParticipantsTitle(TEST_USERS_ONLINE_MESSAGE);
      expectLastCall().once(); // once for now
   }

   private void resetAllMocks()
   {
      reset(mockDispatcher, mockDisplay, mockEventBus, mockGlossaryPresenter);
      reset(mockMessages, mockNativeEvent, mockSidePanelPresenter, mockTranslationEditorPresenter, mockTransMemoryPresenter);
      reset(mockWorkspaceUsersPresenter);

      reset(mockOptionsToggle, mockSouthPanelToggle);
   }

   private void replayAllMocks()
   {
      replay(mockDispatcher, mockDisplay, mockEventBus, mockGlossaryPresenter);
      replay(mockMessages, mockNativeEvent, mockSidePanelPresenter, mockTranslationEditorPresenter, mockTransMemoryPresenter);
      replay(mockWorkspaceUsersPresenter);

      replay(mockOptionsToggle, mockSouthPanelToggle);
   }

   private void verifyAllMocks()
   {
      verify(mockDispatcher, mockDisplay, mockEventBus, mockGlossaryPresenter);
      verify(mockMessages, mockNativeEvent, mockSidePanelPresenter, mockTranslationEditorPresenter, mockTransMemoryPresenter);
      verify(mockWorkspaceUsersPresenter);

      verify(mockOptionsToggle, mockSouthPanelToggle);
   }

   private class TranslatorListSuccessAnswer implements IAnswer<GetTranslatorListResult>
   {
      private ArrayList<Person> translatorsToReturn;

      public TranslatorListSuccessAnswer(ArrayList<Person> translatorsToReturn)
      {
         this.translatorsToReturn = translatorsToReturn;
      }

      @Override
      public GetTranslatorListResult answer() throws Throwable
      {
         GetTranslatorListResult result = new GetTranslatorListResult(translatorsToReturn);
         capturedTranslatorListRequestCallback.getValue().onSuccess(result);
         return null;
      }
   }

   // TODO test that failed participant list request is handled appropriately

   private class TranslatorListFailAnswer implements IAnswer<GetTranslatorListResult>
   {
      @Override
      public GetTranslatorListResult answer() throws Throwable
      {
         capturedTranslatorListRequestCallback.getValue().onFailure(new Throwable("test"));
         return null;
      }
   }

}
