package org.eclipse.aether.util.filter;

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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.internal.test.util.NodeBuilder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AndDependencyFilterTest
    extends AbstractDependencyFilterTest
{
    @Test
    public void acceptTest()
    {
        NodeBuilder builder = new NodeBuilder();
        builder.artifactId( "test" );
        List<DependencyNode> parents = new LinkedList<DependencyNode>();

        // Empty AND
        assertThat( new AndDependencyFilter().accept( builder.build(), parents ) ).isTrue();

        // Basic Boolean Input
        assertThat( new AndDependencyFilter( getAcceptFilter() ).accept( builder.build(), parents ) ).isTrue();
        assertThat( new AndDependencyFilter( getDenyFilter() ).accept( builder.build(), parents ) ).isFalse();

        assertThat( new AndDependencyFilter( getDenyFilter(), getDenyFilter() ).accept( builder.build(), parents ) ).isFalse();
        assertThat( new AndDependencyFilter( getDenyFilter(), getAcceptFilter() ).accept( builder.build(), parents ) ).isFalse();
        assertThat( new AndDependencyFilter( getAcceptFilter(), getDenyFilter() ).accept( builder.build(), parents ) ).isFalse();
        assertThat( new AndDependencyFilter( getAcceptFilter(), getAcceptFilter() ).accept( builder.build(), parents ) ).isTrue();

        assertThat( new AndDependencyFilter( getDenyFilter(), getDenyFilter(), getDenyFilter() ).accept( builder.build(),
                                                                                                          parents ) ).isFalse();
        assertThat( new AndDependencyFilter( getAcceptFilter(), getDenyFilter(), getDenyFilter() ).accept( builder.build(),
                                                                                                            parents ) ).isFalse();
        assertThat( new AndDependencyFilter( getAcceptFilter(), getAcceptFilter(), getDenyFilter() ).accept( builder.build(),
                                                                                                              parents ) ).isFalse();
        assertThat( new AndDependencyFilter( getAcceptFilter(), getAcceptFilter(), getAcceptFilter() ).accept( builder.build(),
                                                                                                               parents ) ).isTrue();

        // User another constructor
        Collection<DependencyFilter> filters = new LinkedList<DependencyFilter>();
        filters.add( getDenyFilter() );
        filters.add( getAcceptFilter() );
        assertThat( new AndDependencyFilter( filters ).accept( builder.build(), parents ) ).isFalse();

        filters = new LinkedList<DependencyFilter>();
        filters.add( getDenyFilter() );
        filters.add( getDenyFilter() );
        assertThat( new AndDependencyFilter( filters ).accept( builder.build(), parents ) ).isFalse();

        filters = new LinkedList<DependencyFilter>();
        filters.add( getAcceptFilter() );
        filters.add( getAcceptFilter() );
        assertThat( new AndDependencyFilter( filters ).accept( builder.build(), parents ) ).isTrue();

        // newInstance
        assertThat( AndDependencyFilter.newInstance( getAcceptFilter(), getAcceptFilter() ).accept( builder.build(),
                                                                                                    parents ) ).isTrue();
        assertThat( AndDependencyFilter.newInstance( getAcceptFilter(), getDenyFilter() ).accept( builder.build(),
                                                                                                   parents ) ).isFalse();

        assertThat( AndDependencyFilter.newInstance( getDenyFilter(), null ).accept( builder.build(), parents ) ).isFalse();
        assertThat( AndDependencyFilter.newInstance( getAcceptFilter(), null ).accept( builder.build(), parents ) ).isTrue();
        assertThat( AndDependencyFilter.newInstance( null, null ) ).isNull();
    }

}
