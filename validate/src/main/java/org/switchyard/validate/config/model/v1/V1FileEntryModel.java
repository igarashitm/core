/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.switchyard.validate.config.model.v1;

import org.switchyard.config.Configuration;
import org.switchyard.config.model.BaseModel;
import org.switchyard.config.model.Descriptor;
import org.switchyard.config.model.validate.ValidateModel;
import org.switchyard.validate.config.model.FileEntryModel;

import javax.xml.namespace.QName;

/**
 * A version 1 FileEntryModel.
 */
public class V1FileEntryModel extends BaseModel implements FileEntryModel {

    /**
     * Constructs a new V1SchemaCatalogsModel.
     */
    public V1FileEntryModel() {
        super(new QName(ValidateModel.DEFAULT_NAMESPACE, FileEntryModel.ENTRY));
    }

    /**
     * Constructs a new V1FileEntryModel with the specified Configuration and Descriptor.
     * @param config the Configuration
     * @param desc the Descriptor
     */
    public V1FileEntryModel(Configuration config, Descriptor desc) {
        super(config, desc);
    }

    @Override
    public String getFile() {
        return getModelAttribute(FileEntryModel.FILE);
    }

    @Override
    public FileEntryModel setFile(String file) {
        setModelAttribute(FileEntryModel.FILE, file);
        return this;
    }
}
