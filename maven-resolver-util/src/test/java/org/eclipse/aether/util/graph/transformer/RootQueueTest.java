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

import org.eclipse.aether.util.graph.transformer.ConflictIdSorter.ConflictId;
import org.eclipse.aether.util.graph.transformer.ConflictIdSorter.RootQueue;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RootQueueTest
{

    @Test
    public void testIsEmpty()
    {
        ConflictId id = new ConflictId( "a", 0 );
        RootQueue queue = new RootQueue( 10 );
        assertThat( queue.isEmpty() ).isTrue();
        queue.add( id );
        assertThat( queue.isEmpty() ).isFalse();
        assertThat( queue.remove() ).isSameAs( id );
        assertThat( queue.isEmpty() ).isTrue();
    }

    @Test
    public void testAddSortsByDepth()
    {
        ConflictId id1 = new ConflictId( "a", 0 );
        ConflictId id2 = new ConflictId( "b", 1 );
        ConflictId id3 = new ConflictId( "c", 2 );
        ConflictId id4 = new ConflictId( "d", 3 );

        RootQueue queue = new RootQueue( 10 );
        queue.add( id1 );
        queue.add( id2 );
        queue.add( id3 );
        queue.add( id4 );
        assertThat( queue.remove() ).isSameAs( id1 );
        assertThat( queue.remove() ).isSameAs( id2 );
        assertThat( queue.remove() ).isSameAs( id3 );
        assertThat( queue.remove() ).isSameAs( id4 );

        queue = new RootQueue( 10 );
        queue.add( id4 );
        queue.add( id3 );
        queue.add( id2 );
        queue.add( id1 );
        assertThat( queue.remove() ).isSameAs( id1 );
        assertThat( queue.remove() ).isSameAs( id2 );
        assertThat( queue.remove() ).isSameAs( id3 );
        assertThat( queue.remove() ).isSameAs( id4 );
    }

    @Test
    public void testAddWithArrayCompact()
    {
        ConflictId id = new ConflictId( "a", 0 );

        RootQueue queue = new RootQueue( 10 );
        assertThat( queue.isEmpty() ).isTrue();
        queue.add( id );
        assertThat( queue.isEmpty() ).isFalse();
        assertThat( queue.remove() ).isSameAs( id );
        assertThat( queue.isEmpty() ).isTrue();
        queue.add( id );
        assertThat( queue.isEmpty() ).isFalse();
        assertThat( queue.remove() ).isSameAs( id );
        assertThat( queue.isEmpty() ).isTrue();
    }

    @Test
    public void testAddMinimumAfterSomeRemoves()
    {
        ConflictId id1 = new ConflictId( "a", 0 );
        ConflictId id2 = new ConflictId( "b", 1 );
        ConflictId id3 = new ConflictId( "c", 2 );

        RootQueue queue = new RootQueue( 10 );
        queue.add( id2 );
        queue.add( id3 );
        assertThat( queue.remove() ).isSameAs( id2 );
        queue.add( id1 );
        assertThat( queue.remove() ).isSameAs( id1 );
        assertThat( queue.remove() ).isSameAs( id3 );
        assertThat( queue.isEmpty() ).isTrue();
    }
}
