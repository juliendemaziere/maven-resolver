package org.eclipse.aether.util.graph.traverser;

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

import java.util.Collections;
import java.util.Map;

import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.DependencyTraverser;
import org.eclipse.aether.graph.Dependency;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FatArtifactTraverserTest
{

    @Test
    public void testTraverseDependency()
    {
        DependencyTraverser traverser = new FatArtifactTraverser();
        Map<String, String> props = null;
        assertThat( traverser.traverseDependency( new Dependency( new DefaultArtifact( "g:a:v:1", props ), "test" ) ) ).isTrue();
        props = Collections.singletonMap( ArtifactProperties.INCLUDES_DEPENDENCIES, "false" );
        assertThat( traverser.traverseDependency( new Dependency( new DefaultArtifact( "g:a:v:1", props ), "test" ) ) ).isTrue();
        props = Collections.singletonMap( ArtifactProperties.INCLUDES_DEPENDENCIES, "unrecognized" );
        assertThat( traverser.traverseDependency( new Dependency( new DefaultArtifact( "g:a:v:1", props ), "test" ) ) ).isTrue();
        props = Collections.singletonMap( ArtifactProperties.INCLUDES_DEPENDENCIES, "true" );
        assertThat( traverser.traverseDependency( new Dependency( new DefaultArtifact( "g:a:v:1", props ), "test" ) ) ).isFalse();
    }

    @Test
    public void testDeriveChildTraverser()
    {
        DependencyTraverser traverser = new FatArtifactTraverser();
        assertThat( traverser.deriveChildTraverser( null ) ).isSameAs( traverser );
    }

    @Test
    public void testEquals()
    {
        DependencyTraverser traverser1 = new FatArtifactTraverser();
        DependencyTraverser traverser2 = new FatArtifactTraverser();
        assertThat( traverser1 ).isEqualTo( traverser1 );
        assertThat( traverser2 ).isEqualTo( traverser1 );
        assertThat( traverser1 ).isNotEqualTo( this );
        assertThat( traverser1 ).isNotEqualTo( null );
    }

    @Test
    public void testHashCode()
    {
        DependencyTraverser traverser1 = new FatArtifactTraverser();
        DependencyTraverser traverser2 = new FatArtifactTraverser();
        assertThat( traverser2.hashCode() ).isEqualTo( traverser1.hashCode() );
    }

}
