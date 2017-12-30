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

import java.util.List;

import org.eclipse.aether.collection.UnsolvableVersionConflictException;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.internal.test.util.DependencyGraphParser;
import org.junit.Test;

/**
 */
public class NearestVersionSelectorTest
    extends AbstractDependencyGraphTransformerTest
{

    @Override
    protected ConflictResolver newTransformer()
    {
        return new ConflictResolver( new NearestVersionSelector(), new JavaScopeSelector(),
                                     new SimpleOptionalitySelector(), new JavaScopeDeriver() );
    }

    @Override
    protected DependencyGraphParser newParser()
    {
        return new DependencyGraphParser( "transformer/version-resolver/" );
    }

    @Test
    public void testSelectHighestVersionFromMultipleVersionsAtSameLevel()
        throws Exception
    {
        DependencyNode root = parseResource( "sibling-versions.txt" );
        assertThat(transform( root ) ).isSameAs(root);

        assertThat(root.getChildren().size() ).isEqualTo(1);
        assertThat(root.getChildren().get( 0 ).getArtifact().getVersion() ).isEqualTo("3");
    }

    @Test
    public void testSelectedVersionAtDeeperLevelThanOriginallySeen()
        throws Exception
    {
        DependencyNode root = parseResource( "nearest-underneath-loser-a.txt" );

        assertThat(transform( root ) ).isSameAs(root);

        List<DependencyNode> trail = find( root, "j" );
        assertThat(trail.size() ).isEqualTo(5);
    }

    @Test
    public void testNearestDirtyVersionUnderneathRemovedNode()
        throws Exception
    {
        DependencyNode root = parseResource( "nearest-underneath-loser-b.txt" );

        assertThat(transform( root ) ).isSameAs(root);

        List<DependencyNode> trail = find( root, "j" );
        assertThat(trail.size() ).isEqualTo(5);
    }

    @Test
    public void testViolationOfHardConstraintFallsBackToNearestSeenNotFirstSeen()
        throws Exception
    {
        DependencyNode root = parseResource( "range-backtracking.txt" );

        assertThat(transform( root ) ).isSameAs(root);

        List<DependencyNode> trail = find( root, "x" );
        assertThat(trail.size() ).isEqualTo(3);
        assertThat(trail.get( 0 ).getArtifact().getVersion() ).isEqualTo("2");
    }

    @Test
    public void testCyclicConflictIdGraph()
        throws Exception
    {
        DependencyNode root = parseResource( "conflict-id-cycle.txt" );

        assertThat(transform( root ) ).isSameAs(root);

        assertThat(root.getChildren().size() ).isEqualTo(2);
        assertThat(root.getChildren().get( 0 ).getArtifact().getArtifactId() ).isEqualTo("a");
        assertThat(root.getChildren().get( 1 ).getArtifact().getArtifactId() ).isEqualTo("b");
        assertThat(root.getChildren().get( 0 ).getChildren().isEmpty() ).isTrue();
        assertThat(root.getChildren().get( 1 ).getChildren().isEmpty() ).isTrue();
    }

    @Test( expected = UnsolvableVersionConflictException.class )
    public void testUnsolvableRangeConflictBetweenHardConstraints()
        throws Exception
    {
        DependencyNode root = parseResource( "unsolvable.txt" );

        assertThat(transform( root ) ).isSameAs(root);
    }

    @Test( expected = UnsolvableVersionConflictException.class )
    public void testUnsolvableRangeConflictWithUnrelatedCycle()
        throws Exception
    {
        DependencyNode root = parseResource( "unsolvable-with-cycle.txt" );

        transform( root );
    }

    @Test
    public void testSolvableConflictBetweenHardConstraints()
        throws Exception
    {
        DependencyNode root = parseResource( "ranges.txt" );

        assertThat(transform( root ) ).isSameAs(root);
    }

    @Test
    public void testConflictGroupCompletelyDroppedFromResolvedTree()
        throws Exception
    {
        DependencyNode root = parseResource( "dead-conflict-group.txt" );

        assertThat(transform( root ) ).isSameAs(root);

        assertThat(root.getChildren().size() ).isEqualTo(2);
        assertThat(root.getChildren().get( 0 ).getArtifact().getArtifactId() ).isEqualTo("a");
        assertThat(root.getChildren().get( 1 ).getArtifact().getArtifactId() ).isEqualTo("b");
        assertThat(root.getChildren().get( 0 ).getChildren().isEmpty() ).isTrue();
        assertThat(root.getChildren().get( 1 ).getChildren().isEmpty() ).isTrue();
    }

    @Test
    public void testNearestSoftVersionPrunedByFartherRange()
        throws Exception
    {
        DependencyNode root = parseResource( "soft-vs-range.txt" );

        assertThat(transform( root ) ).isSameAs(root);

        assertThat(root.getChildren().size() ).isEqualTo(2);
        assertThat(root.getChildren().get( 0 ).getArtifact().getArtifactId() ).isEqualTo("a");
        assertThat(root.getChildren().get( 0 ).getChildren().size() ).isEqualTo(0);
        assertThat(root.getChildren().get( 1 ).getArtifact().getArtifactId() ).isEqualTo("b");
        assertThat(root.getChildren().get( 1 ).getChildren().size() ).isEqualTo(1);
    }

    @Test
    public void testCyclicGraph()
        throws Exception
    {
        DependencyNode root = parseResource( "cycle.txt" );

        assertThat(transform( root ) ).isSameAs(root);

        assertThat(root.getChildren().size() ).isEqualTo(2);
        assertThat(root.getChildren().get( 0 ).getChildren().size() ).isEqualTo(1);
        assertThat(root.getChildren().get( 0 ).getChildren().get( 0 ).getChildren().size() ).isEqualTo(0);
        assertThat(root.getChildren().get( 1 ).getChildren().size() ).isEqualTo(0);
    }

    @Test
    public void testLoop()
        throws Exception
    {
        DependencyNode root = parseResource( "loop.txt" );

        assertThat(transform( root ) ).isSameAs(root);

        assertThat(root.getChildren().size() ).isEqualTo(0);
    }

    @Test
    public void testOverlappingCycles()
        throws Exception
    {
        DependencyNode root = parseResource( "overlapping-cycles.txt" );

        assertThat(transform( root ) ).isSameAs(root);

        assertThat(root.getChildren().size() ).isEqualTo(2);
    }

    @Test
    public void testScopeDerivationAndConflictResolutionCantHappenForAllNodesBeforeVersionSelection()
        throws Exception
    {
        DependencyNode root = parseResource( "scope-vs-version.txt" );

        assertThat(transform( root ) ).isSameAs(root);

        DependencyNode[] nodes = find( root, "y" ).toArray( new DependencyNode[0] );
        assertThat(nodes.length ).isEqualTo(3);
        assertThat(nodes[1].getDependency().getScope() ).isEqualTo("test");
        assertThat(nodes[0].getDependency().getScope() ).isEqualTo("test");
    }

    @Test
    public void testVerboseMode()
        throws Exception
    {
        DependencyNode root = parseResource( "verbose.txt" );

        session.setConfigProperty( ConflictResolver.CONFIG_PROP_VERBOSE, Boolean.TRUE );
        assertThat(transform( root ) ).isSameAs(root);

        assertThat(root.getChildren().size() ).isEqualTo(2);
        assertThat(root.getChildren().get( 0 ).getChildren().size() ).isEqualTo(1);
        DependencyNode winner = root.getChildren().get( 0 ).getChildren().get( 0 );
        assertThat(winner.getDependency().getScope() ).isEqualTo("test");
        assertThat(winner.getData().get( ConflictResolver.NODE_DATA_ORIGINAL_SCOPE ) ).isEqualTo("compile");
        assertThat(winner.getData().get( ConflictResolver.NODE_DATA_ORIGINAL_OPTIONALITY) ).isEqualTo(false);
        assertThat(root.getChildren().get( 1 ).getChildren().size() ).isEqualTo(1);
        DependencyNode loser = root.getChildren().get( 1 ).getChildren().get( 0 );
        assertThat(loser.getDependency().getScope() ).isEqualTo("test");
        assertThat(loser.getChildren().size() ).isEqualTo(0);
        assertThat(loser.getData().get( ConflictResolver.NODE_DATA_WINNER ) ).isSameAs(winner);
        assertThat(loser.getData().get( ConflictResolver.NODE_DATA_ORIGINAL_SCOPE ) ).isEqualTo("compile");
        assertThat(loser.getData().get( ConflictResolver.NODE_DATA_ORIGINAL_OPTIONALITY ) ).isEqualTo(false);
    }

}
