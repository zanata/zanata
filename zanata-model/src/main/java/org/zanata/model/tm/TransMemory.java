/*
 * Copyright 2013, Red Hat, Inc. and individual contributors as indicated by the
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
package org.zanata.model.tm;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyEnumerated;
import javax.persistence.OneToMany;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;

import org.zanata.model.SlugEntityBase;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * A translation Memory representation.
 *
 * @author Carlos Munoz <a
 *         href="mailto:camunoz@redhat.com">camunoz@redhat.com</a>
 */
@Entity
@EqualsAndHashCode(callSuper = true, of = { "description" })
@ToString(exclude = "translationUnits")
@Data
@Access(AccessType.FIELD)
public class TransMemory extends SlugEntityBase implements HasTMMetadata {
    private static final long serialVersionUID = 1L;

    private String description;

    // This is the BCP-47 language code. Null means any source language (*all*
    // in TMX)
    @Column(name = "source_language", nullable = true)
    private String sourceLanguage;

    public static TransMemory tm(String slug) {
        TransMemory tm = new TransMemory();
        tm.setSlug(slug);
        return tm;
    }

    @Setter(AccessLevel.PROTECTED)
    @OneToMany(mappedBy = "translationMemory", orphanRemoval = true)
    private Set<TransMemoryUnit> translationUnits = Sets.newHashSet();

    /**
     * Map values are Json strings containing metadata for the particular type
     * of translation memory
     */
    @ElementCollection
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "metadata_type")
    @JoinTable(name = "TransMemory_Metadata", joinColumns = { @JoinColumn(
            name = "trans_memory_id") })
    @Column(name = "metadata", length = Integer.MAX_VALUE)
    private Map<TMMetadataType, String> metadata = Maps.newHashMap();

    @Override
    public String getMetadata(TMMetadataType tmType) {
        return metadata.get(tmType);
    }

    @Override
    public void setMetadata(@Nonnull TMMetadataType tmType, String metadata) {
        this.metadata.put(tmType, metadata);
    }
}
