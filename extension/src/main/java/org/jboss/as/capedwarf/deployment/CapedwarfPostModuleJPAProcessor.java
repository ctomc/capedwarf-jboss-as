/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.capedwarf.deployment;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.jboss.as.jpa.config.Configuration;
import org.jboss.as.jpa.config.PersistenceUnitMetadataHolder;
import org.jboss.as.jpa.spi.PersistenceUnitMetadata;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.module.ResourceRoot;

/**
 * Fix CapeDwarf JPA usage - add metadata scanner.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class CapedwarfPostModuleJPAProcessor extends CapedwarfPersistenceProcessor {
    static final String METADATA_SCANNER_KEY = "datanucleus.metadata.scanner";
    static final String METADATA_SCANNER_CLASS = "org.jboss.capedwarf.datastore.datancleus.JndiMetaDataScanner"; // TODO - configurable?

    protected void modifyPersistenceInfo(DeploymentUnit unit, ResourceRoot resourceRoot, ResourceType type) {
        final PersistenceUnitMetadataHolder holder = resourceRoot.getAttachment(PersistenceUnitMetadataHolder.PERSISTENCE_UNITS);
        if (holder != null) {
            final List<PersistenceUnitMetadata> pus = holder.getPersistenceUnits();
            if (pus != null && pus.isEmpty() == false) {
                final ClassLoader cl = unit.getAttachment(Attachments.MODULE).getClassLoader();
                final Set<String> entities = CapedwarfDeploymentMarker.getEntities(unit);
                for (PersistenceUnitMetadata pumd : pus) {
                    final String providerClass = pumd.getPersistenceProviderClassName();

                    if (Configuration.PROVIDER_CLASS_DATANUCLEUS.equals(providerClass) || Configuration.PROVIDER_CLASS_DATANUCLEUS_GAE.equals(providerClass)) {
                        final Properties properties = pumd.getProperties();
                        if (properties.containsKey(METADATA_SCANNER_KEY) == false) {
                            try {
                                final Constructor ctor = cl.loadClass(METADATA_SCANNER_CLASS).getConstructor(Set.class);
                                final Object scanner = ctor.newInstance(entities);
                                properties.put(METADATA_SCANNER_KEY, scanner);
                            } catch (Exception e) {
                                throw new IllegalArgumentException("Cannot create metadata scanner.", e);
                            }
                        }
                    }
                }
            }
        }
    }

}
