package org.eclipse.aether.util.artifact;

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

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.aether.util.artifact.ArtifactIdUtils.equalsBaseId;
import static org.eclipse.aether.util.artifact.ArtifactIdUtils.equalsId;
import static org.eclipse.aether.util.artifact.ArtifactIdUtils.toBaseId;
import static org.eclipse.aether.util.artifact.ArtifactIdUtils.toId;

/**
 */
public class ArtifactIdUtilsTest
{

    @Test
    public void testToIdArtifact()
    {
        assertThat( toId( null ) ).isSameAs( null );

        Artifact artifact = new DefaultArtifact( "gid", "aid", "ext", "1.0-20110205.132618-23" );
        assertThat( toId( artifact ) ).isEqualTo( "gid:aid:ext:1.0-20110205.132618-23" );

        artifact = new DefaultArtifact( "gid", "aid", "cls", "ext", "1.0-20110205.132618-23" );
        assertThat( toId( artifact ) ).isEqualTo( "gid:aid:ext:cls:1.0-20110205.132618-23" );
    }

    @Test
    public void testToIdStrings()
    {
        assertThat( toId( null, null, null, null, null )).isEqualTo( ":::" );

        assertThat( toId( "gid", "aid", "ext", "", "1" ) ).isEqualTo( "gid:aid:ext:1" );

        assertThat( toId( "gid", "aid", "ext", "cls", "1" ) ).isEqualTo( "gid:aid:ext:cls:1" );
    }

    @Test
    public void testToBaseIdArtifact()
    {
        assertThat( toBaseId( null ) ).isNull();

        Artifact artifact = new DefaultArtifact( "gid", "aid", "ext", "1.0-20110205.132618-23" );
        assertThat( toBaseId( artifact ) ).isEqualTo( "gid:aid:ext:1.0-SNAPSHOT" );

        artifact = new DefaultArtifact( "gid", "aid", "cls", "ext", "1.0-20110205.132618-23" );
        assertThat( toBaseId( artifact ) ).isEqualTo( "gid:aid:ext:cls:1.0-SNAPSHOT" );
    }

    @Test
    public void testToVersionlessIdArtifact()
    {
        assertThat(toId( null ) ).isNull();

        Artifact artifact = new DefaultArtifact( "gid", "aid", "ext", "1" );
        assertThat( "gid:aid:ext" ).isEqualTo( ArtifactIdUtils.toVersionlessId( artifact ) );

        artifact = new DefaultArtifact( "gid", "aid", "cls", "ext", "1" );
        assertThat( "gid:aid:ext:cls" ).isEqualTo( ArtifactIdUtils.toVersionlessId( artifact ) );
    }

    @Test
    public void testToVersionlessIdStrings()
    {
        assertThat( "::" ).isEqualTo( ArtifactIdUtils.toVersionlessId( null, null, null, null ) );

        assertThat( "gid:aid:ext" ).isEqualTo( ArtifactIdUtils.toVersionlessId( "gid", "aid", "ext", "" ) );

        assertThat( "gid:aid:ext:cls" ).isEqualTo( ArtifactIdUtils.toVersionlessId( "gid", "aid", "ext", "cls" ) );
    }

    @Test
    public void testEqualsId()
    {
        Artifact artifact1 = null;
        Artifact artifact2 = null;
        assertThat( equalsId( artifact1, artifact2 ) ).isFalse();
        assertThat( equalsId( artifact2, artifact1 ) ).isFalse();

        artifact1 = new DefaultArtifact( "gid", "aid", "ext", "1.0-20110205.132618-23" );
        assertThat( equalsId( artifact1, artifact2 ) ).isFalse();
        assertThat( equalsId( artifact2, artifact1 ) ).isFalse();

        artifact2 = new DefaultArtifact( "gidX", "aid", "ext", "1.0-20110205.132618-23" );
        assertThat( equalsId( artifact1, artifact2 ) ).isFalse();
        assertThat( equalsId( artifact2, artifact1 ) ).isFalse();

        artifact2 = new DefaultArtifact( "gid", "aidX", "ext", "1.0-20110205.132618-23" );
        assertThat( equalsId( artifact1, artifact2 ) ).isFalse();
        assertThat( equalsId( artifact2, artifact1 ) ).isFalse();

        artifact2 = new DefaultArtifact( "gid", "aid", "extX", "1.0-20110205.132618-23" );
        assertThat( equalsId( artifact1, artifact2 ) ).isFalse();
        assertThat( equalsId( artifact2, artifact1 ) ).isFalse();

        artifact2 = new DefaultArtifact( "gid", "aid", "ext", "1.0-20110205.132618-24" );
        assertThat( equalsId( artifact1, artifact2 ) ).isFalse();
        assertThat( equalsId( artifact2, artifact1 ) ).isFalse();

        artifact2 = new DefaultArtifact( "gid", "aid", "ext", "1.0-20110205.132618-23" );
        assertThat( equalsId( artifact1, artifact2 ) ).isTrue();
        assertThat( equalsId( artifact2, artifact1 ) ).isTrue();

        assertThat( equalsId( artifact1, artifact1 ) ).isTrue();
    }

    @Test
    public void testEqualsBaseId()
    {
        Artifact artifact1 = null;
        Artifact artifact2 = null;
        assertThat( equalsBaseId( artifact1, artifact2 ) ).isFalse();
        assertThat( equalsBaseId( artifact2, artifact1 ) ).isFalse();

        artifact1 = new DefaultArtifact( "gid", "aid", "ext", "1.0-20110205.132618-23" );
        assertThat( equalsBaseId( artifact1, artifact2 ) ).isFalse();
        assertThat( equalsBaseId( artifact2, artifact1 ) ).isFalse();

        artifact2 = new DefaultArtifact( "gidX", "aid", "ext", "1.0-20110205.132618-23" );
        assertThat( equalsBaseId( artifact1, artifact2 ) ).isFalse();
        assertThat( equalsBaseId( artifact2, artifact1 ) ).isFalse();

        artifact2 = new DefaultArtifact( "gid", "aidX", "ext", "1.0-20110205.132618-23" );
        assertThat( equalsBaseId( artifact1, artifact2 ) ).isFalse();
        assertThat( equalsBaseId( artifact2, artifact1 ) ).isFalse();

        artifact2 = new DefaultArtifact( "gid", "aid", "extX", "1.0-20110205.132618-23" );
        assertThat( equalsBaseId( artifact1, artifact2 ) ).isFalse();
        assertThat( equalsBaseId( artifact2, artifact1 ) ).isFalse();

        artifact2 = new DefaultArtifact( "gid", "aid", "ext", "X.0-20110205.132618-23" );
        assertThat( equalsBaseId( artifact1, artifact2 ) ).isFalse();
        assertThat( equalsBaseId( artifact2, artifact1 ) ).isFalse();

        artifact2 = new DefaultArtifact( "gid", "aid", "ext", "1.0-20110205.132618-24" );
        assertThat( equalsBaseId( artifact1, artifact2 ) ).isTrue();
        assertThat( equalsBaseId( artifact2, artifact1 ) ).isTrue();

        artifact2 = new DefaultArtifact( "gid", "aid", "ext", "1.0-20110205.132618-23" );
        assertThat( equalsBaseId( artifact1, artifact2 ) ).isTrue();
        assertThat( equalsBaseId( artifact2, artifact1 ) ).isTrue();

        assertThat( equalsBaseId( artifact1, artifact1 ) ).isTrue();
    }

    @Test
    public void testEqualsVersionlessId()
    {
        Artifact artifact1 = null;
        Artifact artifact2 = null;
        assertThat( ArtifactIdUtils.equalsVersionlessId( artifact1, artifact2 ) ).isFalse();
        assertThat( ArtifactIdUtils.equalsVersionlessId( artifact2, artifact1 ) ).isFalse();

        artifact1 = new DefaultArtifact( "gid", "aid", "ext", "1.0-20110205.132618-23" );
        assertThat( ArtifactIdUtils.equalsVersionlessId( artifact1, artifact2 ) ).isFalse();
        assertThat( ArtifactIdUtils.equalsVersionlessId( artifact2, artifact1 ) ).isFalse();

        artifact2 = new DefaultArtifact( "gidX", "aid", "ext", "1.0-20110205.132618-23" );
        assertThat( ArtifactIdUtils.equalsVersionlessId( artifact1, artifact2 ) ).isFalse();
        assertThat( ArtifactIdUtils.equalsVersionlessId( artifact2, artifact1 ) ).isFalse();

        artifact2 = new DefaultArtifact( "gid", "aidX", "ext", "1.0-20110205.132618-23" );
        assertThat( ArtifactIdUtils.equalsVersionlessId( artifact1, artifact2 ) ).isFalse();
        assertThat( ArtifactIdUtils.equalsVersionlessId( artifact2, artifact1 ) ).isFalse();

        artifact2 = new DefaultArtifact( "gid", "aid", "extX", "1.0-20110205.132618-23" );
        assertThat( ArtifactIdUtils.equalsVersionlessId( artifact1, artifact2 ) ).isFalse();
        assertThat( ArtifactIdUtils.equalsVersionlessId( artifact2, artifact1 ) ).isFalse();

        artifact2 = new DefaultArtifact( "gid", "aid", "ext", "1.0-20110205.132618-24" );
        assertThat( ArtifactIdUtils.equalsVersionlessId( artifact1, artifact2 ) ).isTrue();
        assertThat( ArtifactIdUtils.equalsVersionlessId( artifact2, artifact1 ) ).isTrue();

        artifact2 = new DefaultArtifact( "gid", "aid", "ext", "1.0-20110205.132618-23" );
        assertThat( ArtifactIdUtils.equalsVersionlessId( artifact1, artifact2 ) ).isTrue();
        assertThat( ArtifactIdUtils.equalsVersionlessId( artifact2, artifact1 ) ).isTrue();

        assertThat( ArtifactIdUtils.equalsVersionlessId( artifact1, artifact1 ) ).isTrue();
    }

}
