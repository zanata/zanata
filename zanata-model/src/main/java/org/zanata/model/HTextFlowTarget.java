/*
 * Copyright 2010, Red Hat, Inc. and individual contributors as indicated by the
 * @author tags. See the copyright.txt file in the distribution for a full
 * listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package org.zanata.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.AnalyzerDiscriminator;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Parameter;
import org.zanata.common.ContentState;
import org.zanata.common.HasContents;
import org.zanata.common.LocaleId;
import org.zanata.hibernate.search.ContentStateBridge;
import org.zanata.hibernate.search.IndexFieldLabels;
import org.zanata.hibernate.search.LocaleIdBridge;
import org.zanata.hibernate.search.StringListBridge;
import org.zanata.hibernate.search.TextContainerAnalyzerDiscriminator;
import org.zanata.model.type.EntityType;
import org.zanata.model.type.EntityTypeType;
import org.zanata.model.type.TranslationSourceType;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import org.zanata.model.type.TranslationSourceTypeType;

/**
 * Represents a flow of translated text that should be processed as a
 * stand-alone structural unit.
 *
 * @see org.zanata.rest.dto.resource.TextFlowTarget
 * @author Asgeir Frimannsson <asgeirf@redhat.com>
 *
 */
@Entity
@EntityListeners({ HTextFlowTarget.EntityListener.class })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@TypeDefs({
    @TypeDef(name = "sourceType", typeClass = TranslationSourceTypeType.class),
    @TypeDef(name = "entityType", typeClass = EntityTypeType.class)
})
@Indexed
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class HTextFlowTarget extends ModelEntityBase implements HasContents,
        HasSimpleComment, ITextFlowTargetHistory, Serializable,
        ITextFlowTarget, IsEntityWithType {

    private static final long serialVersionUID = 302308010797605435L;

    private HTextFlow textFlow;
    private @Nonnull
    HLocale locale;

    private String content0;
    private String content1;
    private String content2;
    private String content3;
    private String content4;
    private String content5;

    private ContentState state = ContentState.New;
    private Integer textFlowRevision;
    private HPerson lastModifiedBy;

    private HPerson translator;
    private HPerson reviewer;

    private HSimpleComment comment;

    private Map<Integer, HTextFlowTargetHistory> history;

    private List<HTextFlowTargetReviewComment> reviewComments;

    @Getter
    private String revisionComment;

    private EntityType copiedEntityType;

    @Getter
    private Long copiedEntityId;

    private TranslationSourceType sourceType;

    @Getter
    @Setter(AccessLevel.PRIVATE)
    private Boolean automatedEntry;

    private boolean revisionCommentSet = false;

    // Only for internal use (persistence transient)
    @Setter(AccessLevel.PRIVATE)
    private Integer oldVersionNum;

    @Type(type = "sourceType")
    public TranslationSourceType getSourceType() {
        return sourceType;
    }

    public void setRevisionComment(String revisionComment) {
        this.revisionComment = revisionComment;
        revisionCommentSet = true;
    }

    @Transient
    boolean isRevisionCommentSet() {
        return revisionCommentSet;
    }

    // Only for internal use (persistence transient)
    @Setter(AccessLevel.PRIVATE)
    private HTextFlowTargetHistory initialState;

    public HTextFlowTarget(HTextFlow textFlow, @Nonnull HLocale locale) {
        this.locale = locale;
        this.textFlow = textFlow;
        this.textFlowRevision = textFlow.getRevision();
    }

    // TODO PERF @NaturalId(mutable=false) for better criteria caching
    @NaturalId
    @ManyToOne(optional = false)
    @JoinColumn(name = "locale", nullable = false, updatable = false)
    @Field(analyze = Analyze.NO)
    @FieldBridge(impl = LocaleIdBridge.class)
    public @Nonnull
    HLocale getLocale() {
        return locale;
    }

    @Transient
    @Override
    public @Nonnull
    LocaleId getLocaleId() {
        return locale.getLocaleId();
    }

    @NotNull
    @Field(analyze = Analyze.NO)
    @FieldBridge(impl = ContentStateBridge.class)
    @Override
    public @Nonnull
    ContentState getState() {
        return state;
    }

    public void setState(@Nonnull ContentState newState) {
        state = newState;
    }

    @NotNull
    @Column(name = "tf_revision")
    @Override
    public Integer getTextFlowRevision() {
        return textFlowRevision;
    }

    @ManyToOne(cascade = { CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id", nullable = true)
    @Override
    public HPerson getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(HPerson date) {
        lastModifiedBy = date;
    }

    @Override
    @ManyToOne(cascade = { CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "translated_by_id", nullable = true)
    public HPerson getTranslator() {
        return translator;
    }

    @Override
    @ManyToOne(cascade = { CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id", nullable = true)
    public HPerson getReviewer() {
        return reviewer;
    }

    public boolean hasReviewer() {
        return reviewer != null;
    }

    // TODO PERF @NaturalId(mutable=false) for better criteria caching
    @NaturalId
    @ManyToOne
    @JoinColumn(name = "tf_id")
    // @Field(index = Index.UN_TOKENIZED)
    // @FieldBridge(impl = ContainingWorkspaceBridge.class)
            @IndexedEmbedded
            public
            HTextFlow getTextFlow() {
        return textFlow;
    }

    /**
     * As of release 1.6, replaced by {@link #getContents()}
     *
     * @return
     */
    @Deprecated
    @Transient
    public String getContent() {
        if (this.getContents().size() > 0) {
            return this.getContents().get(0);
        }
        return null;
    }

    @Deprecated
    @Transient
    public void setContent(String content) {
        this.setContents(Arrays.asList(content));
    }

    @Type(type = "entityType")
    public EntityType getCopiedEntityType() {
        return copiedEntityType;
    }

    @Override
    @Transient
    // TODO extend HTextContainer and remove this
    @Field(name = IndexFieldLabels.CONTENT, bridge = @FieldBridge(
            impl = StringListBridge.class, params = {
                    @Parameter(name = "case", value = "fold"),
                    @Parameter(name = "ngrams", value = "multisize") }))
    @AnalyzerDiscriminator(impl = TextContainerAnalyzerDiscriminator.class)
    public
            List<String> getContents() {
        List<String> contents = new ArrayList<String>();
        boolean populating = false;
        for (int i = MAX_PLURALS - 1; i >= 0; i--) {
            String c = this.getContent(i);
            if (c != null) {
                populating = true;
            }

            if (populating) {
                contents.add(0, c);
            }
        }
        return contents;
    }

    public void setContents(List<String> contents) {
        if (!Objects.equal(contents, this.getContents())) {
            for (int i = 0; i < contents.size(); i++) {
                this.setContent(i, contents.get(i));
            }
        }
    }

    private String getContent(int idx) {
        switch (idx) {
        case 0:
            return content0;

        case 1:
            return content1;

        case 2:
            return content2;

        case 3:
            return content3;

        case 4:
            return content4;

        case 5:
            return content5;

        default:
            throw new RuntimeException("Invalid Content index: " + idx);
        }
    }

    private void setContent(int idx, String content) {
        switch (idx) {
        case 0:
            content0 = content;
            break;

        case 1:
            content1 = content;
            break;

        case 2:
            content2 = content;
            break;

        case 3:
            content3 = content;
            break;

        case 4:
            content4 = content;
            break;

        case 5:
            content5 = content;
            break;

        default:
            throw new RuntimeException("Invalid Content index: " + idx);
        }
    }

    protected String getContent0() {
        return content0;
    }

    protected String getContent1() {
        return content1;
    }

    protected String getContent2() {
        return content2;
    }

    protected String getContent3() {
        return content3;
    }

    protected String getContent4() {
        return content4;
    }

    protected String getContent5() {
        return content5;
    }

    public void setContents(String... contents) {
        this.setContents(Arrays.asList(contents));
    }

    @OneToOne(optional = true, fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "comment_id")
    public HSimpleComment getComment() {
        return comment;
    }

    @OneToMany(cascade = { CascadeType.REMOVE, CascadeType.MERGE,
            CascadeType.PERSIST }, mappedBy = "textFlowTarget")
    @MapKey(name = "versionNum")
    public Map<Integer, HTextFlowTargetHistory> getHistory() {
        if (this.history == null) {
            this.history = new HashMap<Integer, HTextFlowTargetHistory>();
        }
        return history;
    }

    @OneToMany(cascade = { CascadeType.REMOVE, CascadeType.MERGE,
            CascadeType.PERSIST }, mappedBy = "textFlowTarget")
    public List<HTextFlowTargetReviewComment> getReviewComments() {
        if (reviewComments == null) {
            reviewComments = Lists.newArrayList();
        }
        return reviewComments;
    }

    public HTextFlowTargetReviewComment addReviewComment(String comment,
            HPerson commenter) {
        HTextFlowTargetReviewComment reviewComment =
                new HTextFlowTargetReviewComment(this, comment, commenter);
        getReviewComments().add(reviewComment);
        return reviewComment;
    }

    @Override
    public String toString() {
        MoreObjects.ToStringHelper helper =
                MoreObjects.toStringHelper(this)
                        .add("contents", getContents())
                        .add("locale", getLocale())
                        .add("state", getState())
                        .add("comment", getComment());
        if (getTextFlow() == null) {
            return helper.toString();
        }
        return helper
                .add("textFlow", getTextFlow().getContents()).toString();
    }

    @Transient
    public void clear() {
        setContents(null, null, null, null, null, null);
        setState(ContentState.New);
        setComment(null);
        setLastModifiedBy(null);
        setTranslator(null);
        setReviewer(null);
        setRevisionComment(null);
        setSourceType(null);
        setCopiedEntityId(null);
        setCopiedEntityType(null);
    }

    protected boolean logPersistence() {
        return false;
    }

    @Override
    @Transient
    public EntityType getEntityType() {
        return EntityType.HTexFlowTarget;
    }

    public static class EntityListener {
        @PreUpdate
        private void preUpdate(HTextFlowTarget tft) {
            // insert history if this has changed from its initial state
            if (tft.initialState != null && tft.initialState.hasChanged(tft)) {
                if (tft.initialState.getSourceType() == null) {
                    tft.initialState.setSourceType(TranslationSourceType.UNKNOWN);
                }
                tft.initialState.setAutomatedEntry(tft.initialState
                        .getSourceType().isAutomatedEntry());
                tft.getHistory().put(tft.oldVersionNum, tft.initialState);
                if (!tft.isRevisionCommentSet()) {
                    tft.setRevisionComment(null);
                }
            }
            setAutomatedEntry(tft);
        }

        @PrePersist
        private void prePersist(HTextFlowTarget tft) {
            setAutomatedEntry(tft);
        }

        private void setAutomatedEntry(HTextFlowTarget tft) {
            if (tft.getSourceType() == null) {
                tft.setSourceType(TranslationSourceType.UNKNOWN);
            }
            tft.setAutomatedEntry(tft.getSourceType().isAutomatedEntry());
        }

        @PostUpdate
        @PostPersist
        @PostLoad
        private void updateInternalHistory(HTextFlowTarget tft) {
            tft.oldVersionNum = tft.getVersionNum();
            tft.initialState = new HTextFlowTargetHistory(tft);
        }

    }
}
