/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
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
package org.xwiki.extension.repository.internal.local;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.AbstractExtensionRepository;
import org.xwiki.extension.repository.internal.RepositoryUtils;
import org.xwiki.extension.repository.result.CollectionIterableResult;
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.extension.repository.search.SearchException;
import org.xwiki.extension.repository.search.Searchable;
import org.xwiki.extension.version.Version;

/**
 * Base class for {@link org.xwiki.extension.repository.ExtensionRepository} implementations maintaining a cache of all
 * extensions.
 * 
 * @param <E> the type of the extension
 * @version $Id$
 * @since 4.3
 */
public abstract class AbstractCachedExtensionRepository<E extends Extension> extends AbstractExtensionRepository
    implements Searchable
{
    /**
     * The local extensions.
     */
    protected transient Map<ExtensionId, E> extensions = new ConcurrentHashMap<ExtensionId, E>();

    /**
     * The local extensions grouped by ids and ordered by version DESC.
     * <p>
     * <extension id, extensions>
     */
    protected Map<String, List<E>> extensionsVersions = new ConcurrentHashMap<String, List<E>>();

    /**
     * Register a new local extension.
     * 
     * @param extension the new local extension
     */
    protected void addCachedExtension(E extension)
    {
        // extensions
        this.extensions.put(extension.getId(), extension);

        // versions
        addCachedExtensionVersion(extension.getId().getId(), extension);
        for (String feature : extension.getFeatures()) {
            addCachedExtensionVersion(feature, extension);
        }
    }

    /**
     * Register extension in all caches.
     * 
     * @param feature the feature
     * @param extension the extension
     */
    protected void addCachedExtensionVersion(String feature, E extension)
    {
        // versions
        List<E> versions = this.extensionsVersions.get(feature);

        if (versions == null) {
            versions = new ArrayList<E>();
            this.extensionsVersions.put(feature, versions);

            versions.add(extension);
        } else {
            int index = 0;
            while (index < versions.size()
                && extension.getId().getVersion().compareTo(versions.get(index).getId().getVersion()) < 0) {
                ++index;
            }

            versions.add(index, extension);
        }
    }

    /**
     * Remove extension from all caches.
     * 
     * @param extension the extension
     */
    protected void removeCachedExtension(E extension)
    {
        // Remove the extension from the memory.
        this.extensions.remove(extension.getId());
        List<E> localExtensionVersions = this.extensionsVersions.get(extension.getId().getId());
        localExtensionVersions.remove(extension);
        if (localExtensionVersions.isEmpty()) {
            this.extensionsVersions.remove(extension.getId().getId());
        }
    }

    // ExtensionRepository

    @Override
    public E resolve(ExtensionId extensionId) throws ResolveException
    {
        E localExtension = this.extensions.get(extensionId);

        if (localExtension == null) {
            throw new ResolveException("Can't find extension [" + extensionId + "]");
        }

        return localExtension;
    }

    @Override
    public E resolve(ExtensionDependency extensionDependency) throws ResolveException
    {
        List<E> versions = this.extensionsVersions.get(extensionDependency.getId());

        if (versions != null) {
            for (E extension : versions) {
                if (extensionDependency.getVersionConstraint().containsVersion(extension.getId().getVersion())) {
                    // Return the higher version which satisfy the version constraint
                    return extension;
                }
            }
        }

        throw new ResolveException("Can't find extension dependency [" + extensionDependency + "]");
    }

    @Override
    public boolean exists(ExtensionId extensionId)
    {
        return this.extensions.containsKey(extensionId);
    }

    @Override
    public IterableResult<Version> resolveVersions(String id, int offset, int nb) throws ResolveException
    {
        List<E> versions = this.extensionsVersions.get(id);

        if (versions == null) {
            throw new ResolveException("Can't find extension with id [" + id + "]");
        }

        if (nb == 0 || offset >= versions.size()) {
            return new CollectionIterableResult<Version>(versions.size(), offset, Collections.<Version> emptyList());
        }

        int fromId = offset < 0 ? 0 : offset;
        int toId = offset + nb > versions.size() || nb < 0 ? versions.size() - 1 : offset + nb;

        List<Version> result = new ArrayList<Version>(toId - fromId);

        // Invert to sort in ascendent order
        for (int i = toId - 1; i >= fromId; --i) {
            result.add(versions.get(i).getId().getVersion());
        }

        return new CollectionIterableResult<Version>(versions.size(), offset, result);
    }

    // Searchable

    @Override
    public IterableResult<Extension> search(String pattern, int offset, int nb) throws SearchException
    {
        Pattern patternMatcher =
            StringUtils.isEmpty(pattern) ? null : Pattern.compile(RepositoryUtils.SEARCH_PATTERN_SUFFIXNPREFIX
                + pattern + RepositoryUtils.SEARCH_PATTERN_SUFFIXNPREFIX);

        Set<Extension> set = new HashSet<Extension>();
        List<Extension> result = new ArrayList<Extension>(this.extensionsVersions.size());

        for (List<E> versions : this.extensionsVersions.values()) {
            E extension = versions.get(0);

            if ((patternMatcher == null || RepositoryUtils.matches(patternMatcher, extension))
                && !set.contains(extension)) {
                result.add(extension);
                set.add(extension);
            }
        }

        return RepositoryUtils.searchInCollection(offset, nb, result);
    }
}
