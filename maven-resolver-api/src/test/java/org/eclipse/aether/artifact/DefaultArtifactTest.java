package org.eclipse.aether.artifact;

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

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.Test;

/**
 */
public class DefaultArtifactTest
{

    @Test
    public void testDefaultArtifactString()
    {
        Artifact a;

        a = new DefaultArtifact( "gid:aid:ver" );
        assertThat( a.getGroupId() ).isEqualTo("gid");
        assertThat( a.getArtifactId() ).isEqualTo("aid");
        assertThat( a.getVersion() ).isEqualTo("ver");
        assertThat( a.getBaseVersion() ).isEqualTo("ver");
        assertThat( a.getExtension() ).isEqualTo("jar");
        assertThat( a.getClassifier() ).isEqualTo("");

        a = new DefaultArtifact( "gid:aid:ext:ver" );
        assertThat( a.getGroupId() ).isEqualTo("gid");
        assertThat( a.getArtifactId() ).isEqualTo("aid");
        assertThat( a.getVersion() ).isEqualTo("ver");
        assertThat( a.getBaseVersion() ).isEqualTo("ver");
        assertThat( a.getExtension() ).isEqualTo("ext");
        assertThat( a.getClassifier() ).isEqualTo("");

        a = new DefaultArtifact( "org.gid:foo-bar:jar:1.1-20101116.150650-3" );
        assertThat( a.getGroupId() ).isEqualTo("org.gid");
        assertThat( a.getArtifactId() ).isEqualTo("foo-bar");
        assertThat( a.getVersion() ).isEqualTo("1.1-20101116.150650-3");
        assertThat( a.getBaseVersion() ).isEqualTo("1.1-SNAPSHOT");
        assertThat( a.getExtension() ).isEqualTo("jar");
        assertThat( a.getClassifier() ).isEqualTo("");

        a = new DefaultArtifact( "gid:aid:ext:cls:ver" );
        assertThat( a.getGroupId() ).isEqualTo("gid");
        assertThat( a.getArtifactId() ).isEqualTo("aid");
        assertThat( a.getVersion() ).isEqualTo("ver");
        assertThat( a.getBaseVersion() ).isEqualTo("ver");
        assertThat( a.getExtension() ).isEqualTo("ext");
        assertThat( a.getClassifier() ).isEqualTo("cls");

        a = new DefaultArtifact( "gid:aid::cls:ver" );
        assertThat( a.getGroupId() ).isEqualTo("gid");
        assertThat( a.getArtifactId() ).isEqualTo("aid");
        assertThat( a.getVersion() ).isEqualTo("ver");
        assertThat( a.getBaseVersion() ).isEqualTo("ver");
        assertThat( a.getExtension() ).isEqualTo("jar");
        assertThat( a.getClassifier() ).isEqualTo("cls");

        a = new DefaultArtifact( new DefaultArtifact( "gid:aid:ext:cls:ver" ).toString() );
        assertThat( a.getGroupId() ).isEqualTo("gid");
        assertThat( a.getArtifactId() ).isEqualTo("aid");
        assertThat( a.getVersion() ).isEqualTo("ver");
        assertThat( a.getBaseVersion() ).isEqualTo("ver");
        assertThat( a.getExtension() ).isEqualTo("ext");
        assertThat( a.getClassifier() ).isEqualTo("cls");
    }

    @Test( expected = IllegalArgumentException.class )
    public void testDefaultArtifactBadString()
    {
        new DefaultArtifact( "gid:aid" );
    }

    @Test
    public void testImmutability()
    {
        Artifact a = new DefaultArtifact( "gid:aid:ext:cls:ver" );
        assertThat( a.setFile( new File( "file" ) ) ).isNotSameAs( a );
        assertThat( a.setVersion( "otherVersion" ) ).isNotSameAs( a );
        assertThat( a.setProperties( Collections.singletonMap( "key", "value"))).isNotSameAs( a );
    }

    @Test
    public void testArtifactType()
    {
        DefaultArtifactType type = new DefaultArtifactType( "typeId", "typeExt", "typeCls", "typeLang", true, true );

        Artifact a = new DefaultArtifact( "gid", "aid", null, null, null, null, type );
        assertThat(a.getExtension() ).isEqualTo("typeExt");
        assertThat(a.getClassifier() ).isEqualTo("typeCls");
        assertThat(a.getProperties().get( ArtifactProperties.LANGUAGE ) ).isEqualTo("typeLang");
        assertThat(a.getProperties().get( ArtifactProperties.TYPE ) ).isEqualTo("typeId");
        assertThat(a.getProperties().get( ArtifactProperties.INCLUDES_DEPENDENCIES ) ).isEqualTo("true");
        assertThat(a.getProperties().get( ArtifactProperties.CONSTITUTES_BUILD_PATH ) ).isEqualTo("true");

        a = new DefaultArtifact( "gid", "aid", "cls", "ext", "ver", null, type );
        assertThat(a.getExtension() ).isEqualTo("ext");
        assertThat(a.getClassifier() ).isEqualTo("cls");
        assertThat(a.getProperties().get( ArtifactProperties.LANGUAGE ) ).isEqualTo("typeLang");
        assertThat(a.getProperties().get( ArtifactProperties.TYPE ) ).isEqualTo("typeId");
        assertThat(a.getProperties().get( ArtifactProperties.INCLUDES_DEPENDENCIES ) ).isEqualTo("true");
        assertThat(a.getProperties().get( ArtifactProperties.CONSTITUTES_BUILD_PATH ) ).isEqualTo("true");

        Map<String, String> props = new HashMap<String, String>();
        props.put( "someNonStandardProperty", "someNonStandardProperty" );
        a = new DefaultArtifact( "gid", "aid", "cls", "ext", "ver", props, type );
        assertThat(a.getExtension() ).isEqualTo("ext");
        assertThat(a.getClassifier() ).isEqualTo("cls");
        assertThat(a.getProperties().get( ArtifactProperties.LANGUAGE ) ).isEqualTo("typeLang");
        assertThat(a.getProperties().get( ArtifactProperties.TYPE ) ).isEqualTo("typeId");
        assertThat(a.getProperties().get( ArtifactProperties.INCLUDES_DEPENDENCIES ) ).isEqualTo("true");
        assertThat(a.getProperties().get( ArtifactProperties.CONSTITUTES_BUILD_PATH ) ).isEqualTo("true");
        assertThat(a.getProperties().get( "someNonStandardProperty" ) ).isEqualTo("someNonStandardProperty");

        props = new HashMap<String, String>();
        props.put( "someNonStandardProperty", "someNonStandardProperty" );
        props.put( ArtifactProperties.CONSTITUTES_BUILD_PATH, "rubbish" );
        props.put( ArtifactProperties.INCLUDES_DEPENDENCIES, "rubbish" );
        a = new DefaultArtifact( "gid", "aid", "cls", "ext", "ver", props, type );
        assertThat(a.getExtension() ).isEqualTo("ext");
        assertThat(a.getClassifier() ).isEqualTo("cls");
        assertThat(a.getProperties().get( ArtifactProperties.LANGUAGE ) ).isEqualTo("typeLang");
        assertThat(a.getProperties().get( ArtifactProperties.TYPE ) ).isEqualTo("typeId");
        assertThat(a.getProperties().get( ArtifactProperties.INCLUDES_DEPENDENCIES ) ).isEqualTo("rubbish");
        assertThat(a.getProperties().get( ArtifactProperties.CONSTITUTES_BUILD_PATH ) ).isEqualTo("rubbish");
        assertThat(a.getProperties().get( "someNonStandardProperty" ) ).isEqualTo("someNonStandardProperty");
    }

    @Test
    public void testPropertiesCopied()
    {
        Map<String, String> props = new HashMap<String, String>();
        props.put( "key", "value1" );

        Artifact a = new DefaultArtifact( "gid:aid:1", props );
        assertThat( a.getProperty( "key", null)).isEqualTo( "value1");
        props.clear();
        assertThat(a.getProperty( "key", null)).isEqualTo( "value1");

        props.put( "key", "value2" );
        a = a.setProperties( props );
        assertThat(a.getProperty( "key", null)).isEqualTo( "value2");
        props.clear();
        assertThat( a.getProperty( "key",null)).isEqualTo( "value2");
    }

    @Test
    public void testIsSnapshot()
    {
        Artifact a = new DefaultArtifact( "gid:aid:ext:cls:1.0" );
        assertThat(a.isSnapshot() ).as( a.getVersion() ).isFalse();

        a = new DefaultArtifact( "gid:aid:ext:cls:1.0-SNAPSHOT" );
        assertThat(a.isSnapshot() ).as( a.getVersion() ).isTrue();

        a = new DefaultArtifact( "gid:aid:ext:cls:1.0-20101116.150650-3" );
        assertThat(a.isSnapshot() ).as( a.getVersion() ).isTrue();

        a = new DefaultArtifact( "gid:aid:ext:cls:1.0-20101116x150650-3" );
        assertThat(a.isSnapshot() ).as( a.getVersion() ).isFalse();
    }

}
