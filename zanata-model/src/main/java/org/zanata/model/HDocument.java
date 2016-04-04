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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.enterprise.util.AnnotationLiteral;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PreUpdate;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import lombok.extern.slf4j.Slf4j;
import org.apache.deltaspike.core.api.provider.BeanProvider;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.IndexColumn;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Where;
import org.hibernate.validator.constraints.NotEmpty;
import org.zanata.common.ContentType;
import org.zanata.common.LocaleId;
import org.zanata.model.po.HPoHeader;
import org.zanata.model.po.HPoTargetHeader;
import org.zanata.model.type.ContentTypeType;
import org.zanata.model.type.EntityType;
import org.zanata.rest.dto.resource.AbstractResourceMeta;
import org.zanata.rest.dto.resource.Resource;
import org.zanata.rest.dto.resource.ResourceMeta;
import org.zanata.rest.dto.resource.TranslationsResource;

import com.google.common.collect.ImmutableList;
import org.zanata.security.annotations.Authenticated;
import org.zanata.util.Contexts;

/**
 * @see AbstractResourceMeta
 * @see Resource
 * @see ResourceMeta
 * @see TranslationsResource
 *
 */
@Entity
@EntityListeners({HDocument.EntityListener.class})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@TypeDef(name = "contentType", typeClass = ContentTypeType.class)
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, of = {})
@ToString(of = { "name", "path", "docId", "locale", "revision" })
@Slf4j
public class HDocument extends ModelEntityBase implements DocumentWithId,
        IDocumentHistory, Serializable, Iterable<ITextFlow>, IsEntityWithType {
    private static final long serialVersionUID = 5129552589912687504L;
    private String docId;
    private String name;
    private String path;
    private ContentType contentType;
    private Integer revision = 1;
    private HLocale locale;
    private HPerson lastModifiedBy;

    private HProjectIteration projectIteration;

    private Map<String, HTextFlow> allTextFlows;
    /**
     * NB: Any elements which are removed from this list must have obsolete set
     * to true, and any elements which are added to this list must have obsolete
     * set to false.
     */
    private List<HTextFlow> textFlows;
    private boolean obsolete = false;
    private HPoHeader poHeader;
    private Map<HLocale, HPoTargetHeader> poTargetHeaders;

    private HRawDocument rawDocument;

    public HDocument(String fullPath, ContentType contentType, HLocale locale) {
        this.contentType = contentType;
        this.locale = locale;
        setFullPath(fullPath);
    }

    public HDocument(String docId, String name, String path,
            ContentType contentType, HLocale locale) {
        this.docId = docId;
        this.name = name;
        this.path = path;
        this.contentType = contentType;
        this.locale = locale;
    }

    public void setFullPath(String fullPath) {
        this.docId = fullPath;
        int lastSepChar = fullPath.lastIndexOf('/');
        switch (lastSepChar) {
        case -1:
            this.path = "";
            this.name = fullPath;
            break;
        case 0:
            this.path = "/";
            this.name = fullPath.substring(1);
            break;
        default:
            this.path = fullPath.substring(0, lastSepChar + 1);
            this.name = fullPath.substring(lastSepChar + 1);
        }
    }

    // TODO make this case sensitive
    @NaturalId
    @Size(max = 255)
    @NotEmpty
    public String getDocId() {
        return docId;
    }

    @NotEmpty
    public String getName() {
        return name;
    }

    @Transient
    @Override
    public String getQualifiedDocId() {
        HProjectIteration iter = getProjectIteration();
        HProject proj = iter.getProject();
        return proj.getSlug() + ":" + iter.getSlug() + ":" + getDocId();
    }

    @NotNull
    public String getPath() {
        return path;
    }

    @SuppressWarnings("null")
    @ManyToOne
    @JoinColumn(name = "locale", nullable = false)
    @Override
    public @Nonnull
    HLocale getLocale() {
        return this.locale;
    }

    @Transient
    @Override
    public @Nonnull
    LocaleId getSourceLocaleId() {
        return locale.getLocaleId();
    }

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "project_iteration_id", nullable = false)
    @NaturalId
    public HProjectIteration getProjectIteration() {
        return projectIteration;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id", nullable = true)
    @Override
    public HPerson getLastModifiedBy() {
        return lastModifiedBy;
    }

    protected void setLastModifiedBy(HPerson lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    @NotNull
    public Integer getRevision() {
        return revision;
    }

    @Transient
    public void incrementRevision() {
        revision++;
    }

    @Type(type = "contentType")
    @NotNull
    public ContentType getContentType() {
        return contentType;
    }

    @OneToMany
    @JoinColumn(name = "document_id", insertable = false, updatable = false)
    // , nullable = true
    @MapKey(name = "resId")
    /**
     * NB Don't modify this collection.  Add to the TextFlows list instead.
     * TODO get ImmutableMap working here.
     */
    public Map<String, HTextFlow> getAllTextFlows() {
        if (allTextFlows == null) {
            allTextFlows = new HashMap<String, HTextFlow>();
        }
        return allTextFlows;
    }

    @SuppressWarnings("unused")
    // used only by Hibernate
            private
            void setAllTextFlows(Map<String, HTextFlow> allTextFlows) {
        this.allTextFlows = allTextFlows;
    }

    @OneToMany(cascade = CascadeType.ALL)
    @Where(clause = "obsolete=0")
    @IndexColumn(name = "pos", base = 0, nullable = false)
    @JoinColumn(name = "document_id", nullable = false)
    /**
     * NB: Any elements which are removed from this list must have obsolete set
     * to true, and any elements which are added to this list must have obsolete
     * set to false.
     */
    public List<HTextFlow> getTextFlows() {
        if (textFlows == null) {
            textFlows = new ArrayList<HTextFlow>();
        }
        return textFlows;
    }

    @Override
    public Iterator<ITextFlow> iterator() {
        return ImmutableList.<ITextFlow> copyOf(getTextFlows()).iterator();
    }

    public boolean isObsolete() {
        return obsolete;
    }

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY,
            optional = true)
    public HPoHeader getPoHeader() {
        return poHeader;
    }

    // private setter for Hibernate
    @SuppressWarnings("unused")
    private void setPoTargetHeaders(
            Map<HLocale, HPoTargetHeader> poTargetHeaders) {
        this.poTargetHeaders = poTargetHeaders;
    }

    // TODO use orphanRemoval=true: requires JPA 2.0
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY,
            mappedBy = "document", orphanRemoval = true)
    @MapKey(name = "targetLanguage")
    public Map<HLocale, HPoTargetHeader> getPoTargetHeaders() {
        if (poTargetHeaders == null) {
            poTargetHeaders = new HashMap<HLocale, HPoTargetHeader>();
        }
        return poTargetHeaders;
    }

    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinTable(name = "HDocument_RawDocument", joinColumns = @JoinColumn(
            name = "documentId"), inverseJoinColumns = @JoinColumn(
            name = "rawDocumentId"))
    public HRawDocument getRawDocument() {
        return rawDocument;
    }

    @Override
    @Transient
    public EntityType getEntityType() {
        return EntityType.HDocument;
    }

    public static class EntityListener {

        @PreUpdate
        private void onUpdate(HDocument doc) {
            if (Contexts.isSessionContextActive()) {
                HAccount account;
                try {
                    account = BeanProvider
                            .getContextualReference(HAccount.class,
                                    true,
                                    new AnnotationLiteral<Authenticated>() {
                                    });
                } catch (IllegalStateException e) {
                    account = null;
                }
                if (account != null) {
                    doc.setLastModifiedBy(account.getPerson());
                }
            }
        }
    }

}
