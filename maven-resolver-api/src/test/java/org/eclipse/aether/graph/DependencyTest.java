package org.eclipse.aether.graph;

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

import java.util.Arrays;
import java.util.Collections;

import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.junit.Test;

/**
 */
public class DependencyTest
{

    @Test
    public void testSetScope()
    {
        Dependency d1 = new Dependency( new DefaultArtifact( "gid:aid:ver" ), "compile" );

        Dependency d2 = d1.setScope( null );
        assertThat(d1 ).isNotSameAs(d2);
        assertThat(d2.getScope() ).isEqualTo("");

        Dependency d3 = d1.setScope( "test" );
        assertThat(d1 ).isNotSameAs(d3);
        assertThat(d3.getScope() ).isEqualTo("test");
    }

    @Test
    public void testSetExclusions()
    {
        Dependency d1 =
            new Dependency( new DefaultArtifact( "gid:aid:ver" ), "compile", false,
                            Collections.singleton( new Exclusion( "g", "a", "c", "e" ) ) );

        Dependency d2 = d1.setExclusions( null );
        assertThat( d1 ).isNotSameAs(d2);
        assertThat( d2.getExclusions().size() ).isEqualTo(0);

        assertThat( d2.setExclusions( null ) ).isSameAs(d2);
        assertThat( d2.setExclusions( Collections.<Exclusion> emptyList() ) ).isSameAs(d2);
        assertThat( d2.setExclusions( Collections.<Exclusion> emptySet() ) ).isSameAs(d2);
        assertThat( d1.setExclusions( Arrays.asList( new Exclusion( "g", "a", "c", "e" ) ) )).isSameAs( d1 );

        Dependency d3 =
            d1.setExclusions( Arrays.asList( new Exclusion( "g", "a", "c", "e" ), new Exclusion( "g", "a", "c", "f" ) ) );
        assertThat( d1 ).isNotSameAs(d3);
        assertThat( d3.getExclusions() ).hasSize( 2 );
    }

}
