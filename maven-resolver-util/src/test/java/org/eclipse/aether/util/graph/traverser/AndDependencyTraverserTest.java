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

import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencyTraverser;
import org.eclipse.aether.graph.Dependency;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AndDependencyTraverserTest
{

    static class DummyDependencyTraverser
        implements DependencyTraverser
    {

        private final boolean traverse;

        private final DependencyTraverser child;

        public DummyDependencyTraverser()
        {
            this( true );
        }

        public DummyDependencyTraverser( boolean traverse )
        {
            this.traverse = traverse;
            this.child = this;
        }

        public DummyDependencyTraverser( boolean traverse, DependencyTraverser child )
        {
            this.traverse = traverse;
            this.child = child;
        }

        public boolean traverseDependency( Dependency dependency )
        {
            return traverse;
        }

        public DependencyTraverser deriveChildTraverser( DependencyCollectionContext context )
        {
            return child;
        }

    }

    @Test
    public void testNewInstance()
    {
        assertThat( AndDependencyTraverser.newInstance( null, null ) ).isNull();
        DependencyTraverser traverser = new DummyDependencyTraverser();
        assertThat( AndDependencyTraverser.newInstance( traverser, null ) ).isSameAs( traverser );
        assertThat( AndDependencyTraverser.newInstance( null, traverser ) ).isSameAs( traverser );
        assertThat( AndDependencyTraverser.newInstance( traverser, traverser ) ).isSameAs( traverser );
        assertThat( AndDependencyTraverser.newInstance( traverser, new DummyDependencyTraverser() ) ).isNotNull();
    }

    @Test
    public void testTraverseDependency()
    {
        Dependency dependency = new Dependency( new DefaultArtifact( "g:a:v:1" ), "runtime" );

        DependencyTraverser traverser = new AndDependencyTraverser();
        assertThat(traverser.traverseDependency( dependency ) ).isTrue();

        traverser =
            new AndDependencyTraverser( new DummyDependencyTraverser( false ), new DummyDependencyTraverser( false ) );
        assertThat(traverser.traverseDependency( dependency ) ).isFalse();

        traverser =
            new AndDependencyTraverser( new DummyDependencyTraverser( true ), new DummyDependencyTraverser( false ) );
        assertThat(traverser.traverseDependency( dependency ) ).isFalse();

        traverser =
            new AndDependencyTraverser( new DummyDependencyTraverser( true ), new DummyDependencyTraverser( true ) );
        assertThat(traverser.traverseDependency( dependency ) ).isTrue();
    }

    @Test
    public void testDeriveChildTraverser_Unchanged()
    {
        DependencyTraverser other1 = new DummyDependencyTraverser( true );
        DependencyTraverser other2 = new DummyDependencyTraverser( false );
        DependencyTraverser traverser = new AndDependencyTraverser( other1, other2 );
        assertThat( traverser.deriveChildTraverser( null ) ).isSameAs( traverser );
    }

    @Test
    public void testDeriveChildTraverser_OneRemaining()
    {
        DependencyTraverser other1 = new DummyDependencyTraverser( true );
        DependencyTraverser other2 = new DummyDependencyTraverser( false, null );
        DependencyTraverser traverser = new AndDependencyTraverser( other1, other2 );
        assertThat( traverser.deriveChildTraverser( null ) ).isSameAs(other1);
    }

    @Test
    public void testDeriveChildTraverser_ZeroRemaining()
    {
        DependencyTraverser other1 = new DummyDependencyTraverser( true, null );
        DependencyTraverser other2 = new DummyDependencyTraverser( false, null );
        DependencyTraverser traverser = new AndDependencyTraverser( other1, other2 );
        assertThat( traverser.deriveChildTraverser( null ) ).isNull();
    }

    @Test
    public void testEquals()
    {
        DependencyTraverser other1 = new DummyDependencyTraverser( true );
        DependencyTraverser other2 = new DummyDependencyTraverser( false );
        DependencyTraverser traverser1 = new AndDependencyTraverser( other1, other2 );
        DependencyTraverser traverser2 = new AndDependencyTraverser( other2, other1 );
        DependencyTraverser traverser3 = new AndDependencyTraverser( other1 );
        assertThat( traverser1 ).isEqualTo(traverser1);
        assertThat( traverser2 ).isEqualTo(traverser1);
        assertThat( traverser1 ).isNotEqualTo( traverser3 );
        assertThat( traverser1 ).isNotEqualTo( this );
        assertThat( traverser1 ).isNotNull();
    }

    @Test
    public void testHashCode()
    {
        DependencyTraverser other1 = new DummyDependencyTraverser( true );
        DependencyTraverser other2 = new DummyDependencyTraverser( false );
        DependencyTraverser traverser1 = new AndDependencyTraverser( other1, other2 );
        DependencyTraverser traverser2 = new AndDependencyTraverser( other2, other1 );
        assertThat( traverser2.hashCode() ).isEqualTo( traverser1.hashCode() );
    }

}
