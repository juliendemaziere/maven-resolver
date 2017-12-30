package org.eclipse.aether.util.graph.transformer;

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

import java.util.Map;

import org.eclipse.aether.collection.DependencyGraphTransformer;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.internal.test.util.DependencyGraphParser;
import org.eclipse.aether.util.graph.transformer.ConflictMarker;
import org.eclipse.aether.util.graph.transformer.TransformationContextKeys;
import org.junit.Test;

/**
 */
public class ConflictMarkerTest
    extends AbstractDependencyGraphTransformerTest
{

    @Override
    protected DependencyGraphTransformer newTransformer()
    {
        return new ConflictMarker();
    }

    @Override
    protected DependencyGraphParser newParser()
    {
        return new DependencyGraphParser( "transformer/conflict-marker/" );
    }

    @Test
    public void testSimple()
        throws Exception
    {
        DependencyNode root = parseResource( "simple.txt" );

        assertThat(transform( root ) ).isSameAs(root);

        Map<?, ?> ids = (Map<?, ?>) context.get( TransformationContextKeys.CONFLICT_IDS );
        assertThat(ids ).isNotNull();

        assertThat(ids.get( root ) ).isNull();
        assertThat(ids.get( root.getChildren().get( 0 ) ) ).isNotNull();
        assertThat(ids.get( root.getChildren().get( 1 ) ) ).isNotNull();
        assertThat(ids.get( root.getChildren().get( 1 ) ) ).isNotSameAs(ids.get( root.getChildren().get( 0 ) ));
        assertThat(ids.get( root.getChildren().get( 0 ) ).equals( ids.get( root.getChildren().get( 1 ) ) ) ).isFalse();
    }

    @Test
    public void testRelocation1()
        throws Exception
    {
        DependencyNode root = parseResource( "relocation1.txt" );

        assertThat(transform( root ) ).isSameAs(root);

        Map<?, ?> ids = (Map<?, ?>) context.get( TransformationContextKeys.CONFLICT_IDS );
        assertThat(ids ).isNotNull();

        assertThat(ids.get( root ) ).isNull();
        assertThat(ids.get( root.getChildren().get( 0 ) ) ).isNotNull();
        assertThat(ids.get( root.getChildren().get( 1 ) ) ).isNotNull();
        assertThat(ids.get( root.getChildren().get( 1 ) ) ).isSameAs(ids.get( root.getChildren().get( 0 ) ));
    }

    @Test
    public void testRelocation2()
        throws Exception
    {
        DependencyNode root = parseResource( "relocation2.txt" );

        assertThat(transform( root ) ).isSameAs(root);

        Map<?, ?> ids = (Map<?, ?>) context.get( TransformationContextKeys.CONFLICT_IDS );
        assertThat(ids ).isNotNull();

        assertThat(ids.get( root ) ).isNull();
        assertThat(ids.get( root.getChildren().get( 0 ) ) ).isNotNull();
        assertThat(ids.get( root.getChildren().get( 1 ) ) ).isNotNull();
        assertThat(ids.get( root.getChildren().get( 1 ) ) ).isSameAs(ids.get( root.getChildren().get( 0 ) ));
    }

    @Test
    public void testRelocation3()
        throws Exception
    {
        DependencyNode root = parseResource( "relocation3.txt" );

        assertThat(transform( root ) ).isSameAs(root);

        Map<?, ?> ids = (Map<?, ?>) context.get( TransformationContextKeys.CONFLICT_IDS );
        assertThat(ids ).isNotNull();

        assertThat(ids.get( root ) ).isNull();
        assertThat(ids.get( root.getChildren().get( 0 ) ) ).isNotNull();
        assertThat(ids.get( root.getChildren().get( 1 ) ) ).isNotNull();
        assertThat(ids.get( root.getChildren().get( 2 ) ) ).isNotNull();
        assertThat(ids.get( root.getChildren().get( 1 ) ) ).isSameAs(ids.get( root.getChildren().get( 0 ) ));
        assertThat(ids.get( root.getChildren().get( 2 ) ) ).isSameAs(ids.get( root.getChildren().get( 1 ) ));
    }

}
