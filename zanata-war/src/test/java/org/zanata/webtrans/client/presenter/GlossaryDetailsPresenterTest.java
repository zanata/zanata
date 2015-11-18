/*
 * Copyright 2012, Red Hat, Inc. and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.zanata.webtrans.client.presenter;

import java.util.List;

import static org.hamcrest.MatcherAssert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import net.customware.gwt.presenter.client.EventBus;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.zanata.common.LocaleId;
import org.zanata.webtrans.client.events.NotificationEvent;
import org.zanata.webtrans.client.resources.UiMessages;
import org.zanata.webtrans.client.rpc.CachingDispatchAsync;
import org.zanata.webtrans.client.view.GlossaryDetailsDisplay;
import org.zanata.webtrans.client.view.GlossaryDisplay;
import org.zanata.webtrans.shared.model.GlossaryDetails;
import org.zanata.webtrans.shared.model.GlossaryResultItem;
import org.zanata.webtrans.shared.model.UserWorkspaceContext;
import org.zanata.webtrans.shared.rpc.GetGlossaryDetailsAction;
import org.zanata.webtrans.shared.rpc.GetGlossaryDetailsResult;
import org.zanata.webtrans.shared.rpc.UpdateGlossaryTermAction;
import org.zanata.webtrans.shared.rpc.UpdateGlossaryTermResult;

import com.google.common.collect.Lists;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasText;

/**
 *
 * @author Alex Eng <a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 *
 */
public class GlossaryDetailsPresenterTest {
    private GlossaryDetailsPresenter glossaryDetailsPresenter;

    @Mock
    private GlossaryDetailsDisplay display;
    @Mock
    private EventBus mockEventBus;
    @Mock
    private CachingDispatchAsync mockDispatcher;
    @Mock
    private UiMessages messages;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private UserWorkspaceContext mockUserWorkspaceContext;
    @Mock
    private HasText targetText;
    @Mock
    private HasText targetComment;
    @Mock
    private HasText pos;
    @Mock
    private HasText description;
    @Mock
    private HasText targetCommentText;

    @Captor
    private ArgumentCaptor<UpdateGlossaryTermAction> updateGlossaryTermCaptor;

    @Captor
    private ArgumentCaptor<GetGlossaryDetailsAction> getGlossaryDetailsCaptor;

    @Captor
    private ArgumentCaptor<AsyncCallback<UpdateGlossaryTermResult>> updateGlossarycallbackCaptor;

    @Captor
    private ArgumentCaptor<AsyncCallback<GetGlossaryDetailsResult>> getGlossarycallbackCaptor;
    @Mock
    private GlossaryDisplay.Listener glossaryListener;
    @Mock
    private HasText srcRef;
    @Mock
    private HasText lastModified;
    @Mock
    private HasText sourceLabel;
    @Mock
    private HasText targetLabel;

    @Before
    public void beforeMethod() {
        MockitoAnnotations.initMocks(this);
        glossaryDetailsPresenter =
                new GlossaryDetailsPresenter(display, mockEventBus, messages,
                        mockDispatcher, mockUserWorkspaceContext);
        glossaryDetailsPresenter.setGlossaryListener(glossaryListener);
    }

    @Test
    public void onBind() {
        boolean hasAccess = true;

        when(
                mockUserWorkspaceContext.getWorkspaceRestrictions()
                        .isHasGlossaryUpdateAccess()).thenReturn(hasAccess);

        glossaryDetailsPresenter.bind();

        verify(display).setListener(glossaryDetailsPresenter);
        verify(display).setHasUpdateAccess(hasAccess);
    }

    @Test
    public void onSaveClick() {
        String targetText = "target Text";
        String newTargetText = "new target Text";
        String targetComment = "new comment";

        GlossaryDetails glossaryDetails = mock(GlossaryDetails.class);
        when(
            mockUserWorkspaceContext.getWorkspaceRestrictions()
                .isHasGlossaryUpdateAccess()).thenReturn(true);
        when(display.getTargetText()).thenReturn(this.targetText);
        when(this.targetText.getText()).thenReturn(newTargetText);
        when(glossaryDetails.getTarget()).thenReturn(targetText);
        when(display.getTargetComment()).thenReturn(targetCommentText);
        when(targetCommentText.getText()).thenReturn(targetComment);
        when(display.getPos()).thenReturn(pos);
        when(pos.getText()).thenReturn("new part of speech");

        when(display.getDescription()).thenReturn(description);
        when(description.getText()).thenReturn("new description");
        glossaryDetailsPresenter.setStatesForTest(glossaryDetails);

        glossaryDetailsPresenter.onSaveClick();

        verify(display).showLoading(true);
        verify(mockDispatcher).execute(updateGlossaryTermCaptor.capture(),
            updateGlossarycallbackCaptor.capture());
        UpdateGlossaryTermAction glossaryTermAction =
                updateGlossaryTermCaptor.getValue();
        assertThat(glossaryTermAction.getNewTargetComment(),
            Matchers.equalTo(targetComment));
        assertThat(glossaryTermAction.getNewTargetTerm(),
            Matchers.equalTo(newTargetText));
        assertThat(glossaryTermAction.getSelectedDetailEntry(),
            Matchers.equalTo(glossaryDetails));
    }

    @Test
    public void onSaveClickAndCallbackSuccess() {
        GlossaryDetails glossaryDetails = mock(GlossaryDetails.class);
        when(
                mockUserWorkspaceContext.getWorkspaceRestrictions()
                        .isHasGlossaryUpdateAccess()).thenReturn(true);
        when(display.getTargetText()).thenReturn(targetText);
        when(targetText.getText()).thenReturn("new target Text");

        when(display.getTargetComment()).thenReturn(targetComment);
        when(targetComment.getText()).thenReturn("new target comment");

        when(display.getPos()).thenReturn(pos);
        when(pos.getText()).thenReturn("new part of speech");

        when(display.getDescription()).thenReturn(description);
        when(description.getText()).thenReturn("new description");

        when(glossaryDetails.getTarget()).thenReturn("target Text");
        glossaryDetailsPresenter.setStatesForTest(glossaryDetails);

        glossaryDetailsPresenter.onSaveClick();

        verify(display).showLoading(true);
        verify(mockDispatcher).execute(updateGlossaryTermCaptor.capture(),
                updateGlossarycallbackCaptor.capture());
        AsyncCallback<UpdateGlossaryTermResult> callback =
                updateGlossarycallbackCaptor.getValue();
        GlossaryDetails newDetails = mock(GlossaryDetails.class);
        when(display.getSrcRef()).thenReturn(srcRef);
        callback.onSuccess(new UpdateGlossaryTermResult(newDetails));

        verify(glossaryListener).fireSearchEvent();
        verify(srcRef).setText(newDetails.getSourceRef());
        verify(display).setDescription(newDetails.getDescription());
        verify(display).setPos(newDetails.getPos());
        verify(display).setTargetComment(newDetails.getTargetComment());
        verify(display).setLastModifiedDate(newDetails.getLastModifiedDate());
        verify(display).showLoading(false);
    }

    @Test
    public void onSaveClickAndCallbackFailure() {
        GlossaryDetails glossaryDetails = mock(GlossaryDetails.class);
        when(
                mockUserWorkspaceContext.getWorkspaceRestrictions()
                        .isHasGlossaryUpdateAccess()).thenReturn(true);
        when(display.getTargetText()).thenReturn(targetText);
        when(targetText.getText()).thenReturn("new target Text");
        when(glossaryDetails.getTarget()).thenReturn("target Text");

        when(display.getTargetComment()).thenReturn(targetComment);
        when(targetComment.getText()).thenReturn("new target comment");

        when(display.getPos()).thenReturn(pos);
        when(pos.getText()).thenReturn("new part of speech");

        when(display.getDescription()).thenReturn(description);
        when(description.getText()).thenReturn("new description");

        glossaryDetailsPresenter.setStatesForTest(glossaryDetails);

        glossaryDetailsPresenter.onSaveClick();

        verify(display).showLoading(true);
        verify(mockDispatcher).execute(updateGlossaryTermCaptor.capture(),
            updateGlossarycallbackCaptor.capture());
        AsyncCallback<UpdateGlossaryTermResult> callback =
                updateGlossarycallbackCaptor.getValue();
        callback.onFailure(new RuntimeException());

        verify(mockEventBus).fireEvent(isA(NotificationEvent.class));
        verify(display).showLoading(false);
    }

    @Test
    public void onSaveClickNoWriteAccess() {
        GlossaryDetails glossaryDetails = mock(GlossaryDetails.class);
        when(
                mockUserWorkspaceContext.getWorkspaceRestrictions()
                        .isHasGlossaryUpdateAccess()).thenReturn(false);
        when(display.getTargetText()).thenReturn(targetText);
        when(targetText.getText()).thenReturn("new target Text");
        when(glossaryDetails.getTarget()).thenReturn("target Text");
        glossaryDetailsPresenter.setStatesForTest(glossaryDetails);

        glossaryDetailsPresenter.onSaveClick();

        verifyZeroInteractions(mockDispatcher);
    }

    @Test
    public void show() {
        GlossaryResultItem item = new GlossaryResultItem("", "", 0, 0);
        when(
                mockUserWorkspaceContext.getWorkspaceRestrictions()
                        .isHasGlossaryUpdateAccess()).thenReturn(true);

        glossaryDetailsPresenter.show(item);

        verify(mockDispatcher).execute(getGlossaryDetailsCaptor.capture(),
                getGlossarycallbackCaptor.capture());
        assertThat(getGlossaryDetailsCaptor.getValue().getSourceIdList(),
                Matchers.equalTo(item.getSourceIdList()));
        AsyncCallback<GetGlossaryDetailsResult> callback =
                getGlossarycallbackCaptor.getValue();

        // testing success callback
        GlossaryDetails glossaryDetails = mock(GlossaryDetails.class);
        when(glossaryDetails.getSource()).thenReturn("source text");
        when(glossaryDetails.getTarget()).thenReturn("target text");
        when(glossaryDetails.getSrcLocale()).thenReturn(new LocaleId("en-US"));
        when(glossaryDetails.getTargetLocale()).thenReturn(new LocaleId("zh"));
        when(glossaryDetails.getTarget()).thenReturn("source text");
        when(display.getSrcRef()).thenReturn(srcRef);
        when(display.getTargetText()).thenReturn(targetText);
        when(display.getSourceLabel()).thenReturn(sourceLabel);
        when(display.getTargetLabel()).thenReturn(targetLabel);
        when(messages.entriesLabel(1)).thenReturn("1");

        callback.onSuccess(new GetGlossaryDetailsResult(Lists
                .newArrayList(glossaryDetails)));

        verify(display).setSourceText(item.getSource());
        verify(targetText).setText(item.getSource());
        verify(display).clearEntries();
        verify(display).setSourceText(anyString());
        verify(targetLabel).setText(anyString());
        verify(display).addEntry("1");
        verify(display).center();
    }
}
