//=============================================================================
//===	Copyright (C) 2001-2011 Food and Agriculture Organization of the
//===	United Nations (FAO-UN), United Nations World Food Programme (WFP)
//===	and United Nations Environment Programme (UNEP)
//===
//===	This program is free software; you can redistribute it and/or modify
//===	it under the terms of the GNU General Public License as published by
//===	the Free Software Foundation; either version 2 of the License, or (at
//===	your option) any later version.
//===
//===	This program is distributed in the hope that it will be useful, but
//===	WITHOUT ANY WARRANTY; without even the implied warranty of
//===	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
//===	General Public License for more details.
//===
//===	You should have received a copy of the GNU General Public License
//===	along with this program; if not, write to the Free Software
//===	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
//===
//===	Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
//===	Rome - Italy. email: geonetwork@osgeo.org
//==============================================================================

package org.fao.geonet.kernel.datamanager.base;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import org.fao.geonet.domain.AbstractMetadata;
import org.fao.geonet.domain.Metadata;
import org.fao.geonet.domain.MetadataCategory;
import org.fao.geonet.kernel.SvnManager;
import org.fao.geonet.kernel.datamanager.IMetadataCategory;
import org.fao.geonet.kernel.datamanager.IMetadataManager;
import org.fao.geonet.kernel.datamanager.IMetadataUtils;
import org.fao.geonet.repository.MetadataCategoryRepository;
import org.fao.geonet.repository.MetadataRepository;
import org.fao.geonet.repository.Updater;
import org.springframework.beans.factory.annotation.Autowired;

import jeeves.server.context.ServiceContext;

public class BaseMetadataCategory implements IMetadataCategory {

    @Autowired
    private IMetadataUtils metadataUtils;
    @Autowired
    private MetadataRepository metadataRepository;
    @Autowired(required = false)
    private SvnManager svnManager;
    @Autowired
    private MetadataCategoryRepository metadataCategoryRepository;


    /**
     * Adds a category to a metadata. Metadata is not reindexed.
     *
     * @return if the category was assigned
     */
    @Override
    public boolean setCategory(ServiceContext context, String mdId, String categId) throws Exception {

        if (!getMetadataRepository().existsById(Integer.valueOf(mdId))) {
            return false;
        }

        final Optional<MetadataCategory> newCategory = metadataCategoryRepository.findById(Integer.valueOf(categId));
        if (!newCategory.isPresent()) {
            return false;
        }

        final boolean[] changed = new boolean[1];
        getMetadataRepository().update(Integer.valueOf(mdId), new Updater<Metadata>() {
            @Override
            public void apply(@Nonnull Metadata entity) {
                changed[0] = !entity.getMetadataCategories().contains(newCategory.get());
                entity.getMetadataCategories().add(newCategory.get());
            }
        });

        if (changed[0]) {
            if (getSvnManager() != null) {
                getSvnManager().setHistory(mdId, context);
            }

            return true;
        }
        return false;
    }

    /**
     * @param mdId
     * @param categId
     * @return
     * @throws Exception
     */
    @Override
    public boolean isCategorySet(final String mdId, final int categId) throws Exception {
        Set<MetadataCategory> categories = getMetadataUtils().findOne(mdId).getCategories();
        for (MetadataCategory category : categories) {
            if (category.getId() == categId) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param mdId
     * @param categId
     * @return if the category was deassigned
     * @throws Exception
     */
    @Override
    public boolean unsetCategory(final ServiceContext context, final String mdId, final int categId) throws Exception {
        AbstractMetadata metadata = getMetadataUtils().findOne(mdId);

        if (metadata == null) {
            return false;
        }
        boolean changed = false;
        for (MetadataCategory category : metadata.getCategories()) {
            if (category.getId() == categId) {
                changed = true;
                metadata.getCategories().remove(category);
                break;
            }
        }

        if (changed) {
            context.getBean(IMetadataManager.class).save(metadata);
            if (getSvnManager() != null) {
                getSvnManager().setHistory(mdId + "", context);
            }
        }

        return changed;
    }

    /**
     * @param mdId
     * @return
     * @throws Exception
     */
    @Override
    public Collection<MetadataCategory> getCategories(final String mdId) throws Exception {
        AbstractMetadata metadata = getMetadataUtils().findOne(mdId);
        if (metadata == null) {
            throw new IllegalArgumentException("No metadata found with id: " + mdId);
        }

        return metadata.getCategories();
    }

    protected SvnManager getSvnManager() {
        return svnManager;

    }

    protected IMetadataUtils getMetadataUtils() {
        return metadataUtils;

    }

    protected MetadataCategoryRepository getMetadataCategoryRepository() {
        return metadataCategoryRepository;
    }

    protected MetadataRepository getMetadataRepository() {
        return metadataRepository;
    }
}
