package org.eclipse.aether.util.graph.selector;

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

import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;
import org.junit.Test;

public class AndDependencySelectorTest
{

    static class DummyDependencySelector
        implements DependencySelector
    {

        private final boolean select;

        private final DependencySelector child;

        public DummyDependencySelector()
        {
            this( true );
        }

        public DummyDependencySelector( boolean select )
        {
            this.select = select;
            this.child = this;
        }

        public DummyDependencySelector( boolean select, DependencySelector child )
        {
            this.select = select;
            this.child = child;
        }

        public boolean selectDependency( Dependency dependency )
        {
            return select;
        }

        public DependencySelector deriveChildSelector( DependencyCollectionContext context )
        {
            return child;
        }

    }

    @Test
    public void testNewInstance()
    {
        assertThat( AndDependencySelector.newInstance( null, null ) ).isNull();
        DependencySelector selector = new DummyDependencySelector();
        assertThat( AndDependencySelector.newInstance( selector, null)).isSameAs( selector );
        assertThat( AndDependencySelector.newInstance( null, selector)).isSameAs( selector );
        assertThat( AndDependencySelector.newInstance( selector, selector)).isSameAs( selector );
        assertThat( AndDependencySelector.newInstance( selector, new DummyDependencySelector() ) ).isNotNull();
    }

    @Test
    public void testTraverseDependency()
    {
        Dependency dependency = new Dependency( new DefaultArtifact( "g:a:v:1" ), "runtime" );

        DependencySelector selector = new AndDependencySelector();
        assertThat( selector.selectDependency( dependency ) ).isTrue();

        selector =
            new AndDependencySelector( new DummyDependencySelector( false ), new DummyDependencySelector( false ) );
        assertThat( selector.selectDependency( dependency ) ).isFalse();

        selector =
            new AndDependencySelector( new DummyDependencySelector( true ), new DummyDependencySelector( false ) );
        assertThat( selector.selectDependency( dependency ) ).isFalse();

        selector = new AndDependencySelector( new DummyDependencySelector( true ), new DummyDependencySelector( true ) );
        assertThat( selector.selectDependency( dependency ) ).isTrue();
    }

    @Test
    public void testDeriveChildSelector_Unchanged()
    {
        DependencySelector other1 = new DummyDependencySelector( true );
        DependencySelector other2 = new DummyDependencySelector( false );
        DependencySelector selector = new AndDependencySelector( other1, other2 );
        assertThat( selector.deriveChildSelector( null ) ).isSameAs(selector);
    }

    @Test
    public void testDeriveChildSelector_OneRemaining()
    {
        DependencySelector other1 = new DummyDependencySelector( true );
        DependencySelector other2 = new DummyDependencySelector( false, null );
        DependencySelector selector = new AndDependencySelector( other1, other2 );
        assertThat( selector.deriveChildSelector( null ) ).isSameAs(other1);
    }

    @Test
    public void testDeriveChildSelector_ZeroRemaining()
    {
        DependencySelector other1 = new DummyDependencySelector( true, null );
        DependencySelector other2 = new DummyDependencySelector( false, null );
        DependencySelector selector = new AndDependencySelector( other1, other2 );
        assertThat( selector.deriveChildSelector( null ) ).isNull();
    }

    @Test
    public void testEquals()
    {
        DependencySelector other1 = new DummyDependencySelector( true );
        DependencySelector other2 = new DummyDependencySelector( false );
        DependencySelector selector1 = new AndDependencySelector( other1, other2 );
        DependencySelector selector2 = new AndDependencySelector( other2, other1 );
        DependencySelector selector3 = new AndDependencySelector( other1 );
        assertThat( selector1 ).isEqualTo(selector1);
        assertThat( selector2 ).isEqualTo(selector1);
        assertThat( selector1 ).isNotEqualTo( selector3 );
        assertThat( selector1 ).isNotEqualTo( this );
        assertThat( selector1 ).isNotEqualTo( null );
    }

    @Test
    public void testHashCode()
    {
        DependencySelector other1 = new DummyDependencySelector( true );
        DependencySelector other2 = new DummyDependencySelector( false );
        DependencySelector selector1 = new AndDependencySelector( other1, other2 );
        DependencySelector selector2 = new AndDependencySelector( other2, other1 );
        assertThat( selector2.hashCode() ).isEqualTo( selector1.hashCode() );
    }

}
