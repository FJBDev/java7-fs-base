/*
 * Copyright (c) 2014, Francis Galiegue (fgaliegue@gmail.com)
 *
 * This software is dual-licensed under:
 *
 * - the Lesser General Public License (LGPL) version 3.0 or, at your option, any
 *   later version;
 * - the Apache Software License (ASL) version 2.0.
 *
 * The text of both licenses is available under the src/resources/ directory of
 * this project (under the names LGPL-3.0.txt and ASL-2.0.txt respectively).
 *
 * Direct link to the sources:
 *
 * - LGPL 3.0: https://www.gnu.org/licenses/lgpl-3.0.txt
 * - ASL 2.0: http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package com.github.fge.filesystem.path;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Abstract factory for {@link PathNames} instances
 *
 * <p>This class is in charge of all the heavy {@link PathNames} operations:
 * creating them from input strings, but also resolving, relativizing and
 * normalizing them.</p>
 *
 * <p>Implementations have to override the necessary methods to extract the root
 * components and name elements from a string, but also telling whether a name
 * element is valid at all, or represents the current or parent directory (in
 * typical filesystems, those would be testing that the name is either of {@code
 * "."} or {@code ".."}).</p>
 *
 * <p>This package provides an implementation for Unix paths.</p>
 */
@ParametersAreNonnullByDefault
public abstract class PathNamesFactory
{
    protected static final String[] NO_NAMES = new String[0];

    private final String rootSeparator;
    private final String separator;

    /**
     * Constructor
     *
     * @param rootSeparator the separator to insert between the root component,
     * if any, and the first name element, if any
     * @param separator the separator to insert between two name elements
     */
    protected PathNamesFactory(final String rootSeparator,
        final String separator)
    {
        this.rootSeparator = rootSeparator;
        this.separator = separator;
    }

    /**
     * Split an input path into the root component and all name elements
     *
     * <p>This method returns a two-element string array, where the first
     * element is the root component and the second element is all name
     * elements.</p>
     *
     * <p>This method also removes all trailing characters from the name
     * elements, if any. If the path has no root, the first element of the
     * returned array must be {@code null}.</p>
     *
     * @param path the path
     * @return see description
     */
    protected abstract String[] rootAndNames(final String path);

    /**
     * Split a names-only input into the individual name components
     *
     * <p>The input is guaranteed to be well-formed (no root component, no
     * trailing characters). The name components must be in their order of
     * appearance in the input.</p>
     *
     * @param namesOnly the input string
     * @return an array of the different name components
     */
    protected abstract String[] splitNames(final String namesOnly);

    /**
     * Check whether a name element is valid for that factory
     *
     * @param name the name to check
     * @return true if the name is valid
     */
    protected abstract boolean isValidName(final String name);

    /**
     * Check whether a name element represents the current directory
     *
     * @param name the name to check
     * @return true if the name represents the current directory
     *
     * @see #normalize(PathNames)
     */
    protected abstract boolean isSelf(final String name);

    /**
     * Check whether a name element represents the parent directory
     *
     * @param name the name to check
     * @return true if the name represents the parent directory
     *
     * @see #normalize(PathNames)
     */
    protected abstract boolean isParent(final String name);

    /**
     * Check whether a {@link PathNames} instance represents an absolute path
     *
     * @param pathNames the instance to check
     * @return true if the instance is an absolute path
     *
     * @see Path#isAbsolute()
     */
    protected abstract boolean isAbsolute(final PathNames pathNames);

    /**
     * Convert an input string into a {@link PathNames} instance
     *
     * @param path the string to convert
     * @return a new {@link PathNames} instance
     * @throws InvalidPathException one name element is wrong
     *
     * @see #rootAndNames(String)
     * @see #isValidName(String)
     */
    @Nonnull
    protected final PathNames toPathNames(final String path)
    {
        final String[] rootAndNames = rootAndNames(path);
        final String root = rootAndNames[0];
        final String namesOnly = rootAndNames[1];

        final String[] names = splitNames(namesOnly);

        for (final String name: names)
            if (!isValidName(name))
                throw new InvalidPathException(path,
                    "invalid path element: " + name);

        return new PathNames(root, names);
    }

    /**
     * Normalize a {@link PathNames} instance
     *
     * @param pathNames the instance to normalize
     * @return a new, normalized instance
     *
     * @see #isSelf(String)
     * @see #isParent(String)
     * @see Path#normalize()
     */
    @Nonnull
    protected final PathNames normalize(final PathNames pathNames)
    {
        final String[] names = pathNames.names;
        final int length = names.length;
        final String[] newNames = new String[length];

        int dstIndex = 0;
        boolean seenRegularName = false;

        for (final String name: names) {
            /*
             * Just skip self names
             */
            if (isSelf(name))
                continue;
            /*
             * Copy over regular names, and say that we have seen such a token
             */
            if (!isParent(name)) {
                newNames[dstIndex++] = name;
                seenRegularName = true;
                continue;
            }
            /*
             * Parent token... If we have seen a regular token already _and_
             * the destination array contains at least one element, decrease
             * the destination index; otherwise copy it into the destination
             * array.
             */
            if (seenRegularName && dstIndex > 0)
                dstIndex--;
            else
                newNames[dstIndex++] = name;
        }

        return new PathNames(pathNames.root,
            dstIndex == 0 ? NO_NAMES : Arrays.copyOf(newNames, dstIndex));
    }

    /*
     * NOTE: throws OperationNotSupportedException if second is not absolute but
     * has a root
     */
    @Nonnull
    protected final PathNames resolve(final PathNames first,
        final PathNames second)
    {
        if (isAbsolute(second))
            return second;

        //noinspection VariableNotUsedInsideIf
        if (second.root != null)
            throw new UnsupportedOperationException();

        final String[] firstNames = first.names;
        final String[] secondNames = second.names;
        final int firstLen = firstNames.length;
        final int secondLen = secondNames.length;

        if (secondLen == 0)
            return first;

        final String[] newNames
            = Arrays.copyOf(firstNames, firstLen + secondLen);
        System.arraycopy(secondNames, 0, newNames, firstLen, secondLen);

        return new PathNames(first.root, newNames);
    }

    @Nonnull
    protected final PathNames resolveSibling(final PathNames first,
        final PathNames second)
    {
        final PathNames firstParent = first.parent();
        return firstParent == null ? second : resolve(firstParent, second);
    }

    @Nonnull
    protected final String toString(final PathNames pathNames)
    {
        final StringBuilder sb = new StringBuilder();
        final boolean hasRoot = pathNames.root != null;
        if (hasRoot)
            sb.append(pathNames.root);

        final String[] names = pathNames.names;
        final int len = names.length;
        if (len == 0)
            return sb.toString();

        if (hasRoot)
            sb.append(rootSeparator);
        sb.append(names[0]);

        for (int i = 1; i < len; i++)
            sb.append(separator).append(names[i]);

        return sb.toString();
    }
}
