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
package org.xwiki.extension.version.internal;

import junit.framework.Assert;

import org.junit.Test;
import org.xwiki.extension.version.InvalidVersionRangeException;

public class DefaultVersionRangeTest
{
    @Test
    public void testIsCompatible() throws InvalidVersionRangeException
    {
        // Compatible
        Assert.assertTrue(new DefaultVersionRange("[1.0]").isCompatible(new DefaultVersionRange("[1.0]")));
        Assert.assertTrue(new DefaultVersionRange("[1.0,2.0]").isCompatible(new DefaultVersionRange("[1.0]")));
        Assert.assertTrue(new DefaultVersionRange("[1.0,2.0]").isCompatible(new DefaultVersionRange("[1.0,3.0]")));
        Assert.assertTrue(new DefaultVersionRange("[1.0,2.0]").isCompatible(new DefaultVersionRange("[2.0,3.0]")));
        Assert.assertTrue(new DefaultVersionRange("(,2.0]").isCompatible(new DefaultVersionRange("[2.0,)")));
        Assert.assertTrue(new DefaultVersionRange("(,2.0]").isCompatible(new DefaultVersionRange("[1.0,)")));
        Assert.assertTrue(new DefaultVersionRange("(,2.0)").isCompatible(new DefaultVersionRange("(1.0,)")));

        // Not compatible
        Assert.assertFalse(new DefaultVersionRange("[1.0,2.0)").isCompatible(new DefaultVersionRange("(2.0,3.0]")));
        Assert.assertFalse(new DefaultVersionRange("(,2.0)").isCompatible(new DefaultVersionRange("(2.0,)")));
        Assert.assertFalse(new DefaultVersionRange("[1.0]").isCompatible(new DefaultVersionRange("[2.0]")));
    }
}
