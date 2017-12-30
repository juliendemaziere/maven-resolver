package org.eclipse.aether.util.version;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static org.assertj.core.api.Assertions.*;

import org.eclipse.aether.util.version.GenericVersion;
import org.eclipse.aether.util.version.GenericVersionRange;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionRange;
import org.junit.Test;

public class GenericVersionRangeTest
{

    private Version newVersion( String version )
    {
        return new GenericVersion( version );
    }

    private VersionRange parseValid( String range )
    {
        try
        {
            return new GenericVersionRange( range );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            AssertionError error =
                new AssertionError( range + " should be valid but failed to parse due to: " + e.getMessage() );
            error.initCause( e );
            throw error;
        }
    }

    private void parseInvalid( String range )
    {
        try
        {
            new GenericVersionRange( range );
            fail( range + " should be invalid" );
        }
        catch ( InvalidVersionSpecificationException e )
        {
            assertThat(true ).isTrue();
        }
    }

    private void assertContains( VersionRange range, String version )
    {
        assertThat( range.containsVersion( newVersion( version ) ) ).as( range + " should contain " + version ).isTrue();
    }

    private void assertNotContains( VersionRange range, String version )
    {
        assertThat( range.containsVersion( newVersion( version ) ) ).as( range + " should not contain " + version ).isFalse();
    }

    @Test
    public void testLowerBoundInclusiveUpperBoundInclusive()
    {
        VersionRange range = parseValid( "[1,2]" );
        assertContains( range, "1" );
        assertContains( range, "1.1-SNAPSHOT" );
        assertContains( range, "2" );
        assertThat(parseValid( range.toString() ) ).isEqualTo(range);
    }

    @Test
    public void testLowerBoundInclusiveUpperBoundExclusive()
    {
        VersionRange range = parseValid( "[1.2.3.4.5,1.2.3.4.6)" );
        assertContains( range, "1.2.3.4.5" );
        assertNotContains( range, "1.2.3.4.6" );
        assertThat(parseValid( range.toString() ) ).isEqualTo(range);
    }

    @Test
    public void testLowerBoundExclusiveUpperBoundInclusive()
    {
        VersionRange range = parseValid( "(1a,1b]" );
        assertNotContains( range, "1a" );
        assertContains( range, "1b" );
        assertThat(parseValid( range.toString() ) ).isEqualTo(range);
    }

    @Test
    public void testLowerBoundExclusiveUpperBoundExclusive()
    {
        VersionRange range = parseValid( "(1,3)" );
        assertNotContains( range, "1" );
        assertContains( range, "2-SNAPSHOT" );
        assertNotContains( range, "3" );
        assertThat(parseValid( range.toString() ) ).isEqualTo(range);
    }

    @Test
    public void testSingleVersion()
    {
        VersionRange range = parseValid( "[1]" );
        assertContains( range, "1" );
        assertThat(parseValid( range.toString() ) ).isEqualTo(range);

        range = parseValid( "[1,1]" );
        assertContains( range, "1" );
        assertThat(parseValid( range.toString() ) ).isEqualTo(range);
    }

    @Test
    public void testSingleWildcardVersion()
    {
        VersionRange range = parseValid( "[1.2.*]" );
        assertContains( range, "1.2-alpha-1" );
        assertContains( range, "1.2-SNAPSHOT" );
        assertContains( range, "1.2" );
        assertContains( range, "1.2.9999999" );
        assertNotContains( range, "1.3-rc-1" );
        assertThat(parseValid( range.toString() ) ).isEqualTo(range);
    }

    @Test
    public void testMissingOpenCloseDelimiter()
    {
        parseInvalid( "1.0" );
    }

    @Test
    public void testMissingOpenDelimiter()
    {
        parseInvalid( "1.0]" );
        parseInvalid( "1.0)" );
    }

    @Test
    public void testMissingCloseDelimiter()
    {
        parseInvalid( "[1.0" );
        parseInvalid( "(1.0" );
    }

    @Test
    public void testTooManyVersions()
    {
        parseInvalid( "[1,2,3]" );
        parseInvalid( "(1,2,3)" );
        parseInvalid( "[1,2,3)" );
    }

}
