package org.eclipse.aether.internal.impl.collect;

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

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.junit.Test;

public class DataPoolTest
{

    private DataPool newDataPool()
    {
        return new DataPool( new DefaultRepositorySystemSession() );
    }

    @Test
    public void testArtifactDescriptorCaching()
    {
        ArtifactDescriptorRequest request = new ArtifactDescriptorRequest();
        request.setArtifact( new DefaultArtifact( "gid:aid:1" ) );
        ArtifactDescriptorResult result = new ArtifactDescriptorResult( request );
        result.setArtifact( new DefaultArtifact( "gid:aid:2" ) );
        result.addRelocation( request.getArtifact() );
        result.addDependency( new Dependency( new DefaultArtifact( "gid:dep:3" ), "compile" ) );
        result.addManagedDependency( new Dependency( new DefaultArtifact( "gid:mdep:3" ), "runtime" ) );
        result.addRepository( new RemoteRepository.Builder( "test", "default", "http://localhost" ).build() );
        result.addAlias( new DefaultArtifact( "gid:alias:4" ) );

        DataPool pool = newDataPool();
        Object key = pool.toKey( request );
        pool.putDescriptor( key, result );
        ArtifactDescriptorResult cached = pool.getDescriptor( key, request );
        assertThat( cached ).isNotNull();
        assertThat( cached.getArtifact() ).isEqualTo( result.getArtifact());
        assertThat( cached.getRelocations() ).isEqualTo( result.getRelocations());
        assertThat( cached.getDependencies() ).isEqualTo( result.getDependencies());
        assertThat( cached.getManagedDependencies() ).isEqualTo( result.getManagedDependencies());
        assertThat( cached.getRepositories() ).isEqualTo( result.getRepositories());
        assertThat( cached.getAliases() ).isEqualTo( result.getAliases());
    }

}
