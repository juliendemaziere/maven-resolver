package org.eclipse.aether.util.graph.versions;

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

import org.eclipse.aether.collection.VersionFilter.VersionFilterContext;
import org.eclipse.aether.util.graph.version.HighestVersionFilter;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HighestVersionFilterTest
    extends AbstractVersionFilterTest
{

    @Test
    public void testFilterVersions()
    {
        HighestVersionFilter filter = new HighestVersionFilter();
        VersionFilterContext ctx = newContext( "g:a:[1,9]", "1", "2", "3", "4", "5", "6", "7", "8", "9" );
        filter.filterVersions( ctx );
        assertVersions( ctx, "9" );
    }

    @Test
    public void testDeriveChildFilter()
    {
        HighestVersionFilter filter = new HighestVersionFilter();
        assertThat( derive( filter, "g:a:1")).isSameAs( filter );
    }

    @Test
    public void testEquals()
    {
        HighestVersionFilter filter = new HighestVersionFilter();
        assertThat( filter.equals( null ) ).isFalse();
        assertThat( filter.equals( filter ) ).isTrue();
        assertThat( filter.equals( new HighestVersionFilter() ) ).isTrue();
    }

}
