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

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class SubArtifactTest
{

    private Artifact newMainArtifact( String coords )
    {
        return new DefaultArtifact( coords );
    }

    @Test
    public void testMainArtifactFileNotRetained()
    {
        Artifact a = newMainArtifact( "gid:aid:ver" ).setFile( new File( "" ) );
        assertThat(a.getFile() ).isNotNull();
        a = new SubArtifact( a, "", "pom" );
        assertThat(a.getFile() ).isNull();
    }

    @Test
    public void testMainArtifactPropertiesNotRetained()
    {
        Artifact a = newMainArtifact( "gid:aid:ver" ).setProperties( Collections.singletonMap( "key", "value" ) );
        assertThat(a.getProperties().size() ).isEqualTo(1);
        a = new SubArtifact( a, "", "pom" );
        assertThat(a.getProperties().size() ).isEqualTo(0);
        assertThat(a.getProperty( "key", null )).isNull();
    }

    @Test( expected = NullPointerException.class )
    public void testMainArtifactMissing()
    {
        new SubArtifact( null, "", "pom" );
    }

    @Test
    public void testEmptyClassifier()
    {
        Artifact main = newMainArtifact( "gid:aid:ext:cls:ver" );
        Artifact sub = new SubArtifact( main, "", "pom" );
        assertThat(sub.getClassifier() ).isEqualTo("");
        sub = new SubArtifact( main, null, "pom" );
        assertThat(sub.getClassifier() ).isEqualTo("");
    }

    @Test
    public void testEmptyExtension()
    {
        Artifact main = newMainArtifact( "gid:aid:ext:cls:ver" );
        Artifact sub = new SubArtifact( main, "tests", "" );
        assertThat(sub.getExtension() ).isEqualTo("");
        sub = new SubArtifact( main, "tests", null );
        assertThat(sub.getExtension() ).isEqualTo("");
    }

    @Test
    public void testSameClassifier()
    {
        Artifact main = newMainArtifact( "gid:aid:ext:cls:ver" );
        Artifact sub = new SubArtifact( main, "*", "pom" );
        assertThat(sub.getClassifier() ).isEqualTo("cls");
    }

    @Test
    public void testSameExtension()
    {
        Artifact main = newMainArtifact( "gid:aid:ext:cls:ver" );
        Artifact sub = new SubArtifact( main, "tests", "*" );
        assertThat(sub.getExtension() ).isEqualTo("ext");
    }

    @Test
    public void testDerivedClassifier()
    {
        Artifact main = newMainArtifact( "gid:aid:ext:cls:ver" );
        Artifact sub = new SubArtifact( main, "*-tests", "pom" );
        assertThat(sub.getClassifier() ).isEqualTo("cls-tests");
        sub = new SubArtifact( main, "tests-*", "pom" );
        assertThat(sub.getClassifier() ).isEqualTo("tests-cls");

        main = newMainArtifact( "gid:aid:ext:ver" );
        sub = new SubArtifact( main, "*-tests", "pom" );
        assertThat(sub.getClassifier() ).isEqualTo("tests");
        sub = new SubArtifact( main, "tests-*", "pom" );
        assertThat(sub.getClassifier() ).isEqualTo("tests");
    }

    @Test
    public void testDerivedExtension()
    {
        Artifact main = newMainArtifact( "gid:aid:ext:cls:ver" );
        Artifact sub = new SubArtifact( main, "", "*.asc" );
        assertThat(sub.getExtension() ).isEqualTo("ext.asc");
        sub = new SubArtifact( main, "", "asc.*" );
        assertThat(sub.getExtension() ).isEqualTo("asc.ext");
    }

    @Test
    public void testImmutability()
    {
        Artifact a = new SubArtifact( newMainArtifact( "gid:aid:ver" ), "", "pom" );
        assertThat( a.setFile( new File( "file" ) ) ).isNotSameAs( a );
        assertThat( a.setVersion( "otherVersion" ) ).isNotSameAs( a );
        assertThat( a.setProperties( Collections.singletonMap( "key", "value"))).isNotSameAs( a );;
    }

    @Test
    public void testPropertiesCopied()
    {
        Map<String, String> props = new HashMap<String, String>();
        props.put( "key", "value1" );

        Artifact a = new SubArtifact( newMainArtifact( "gid:aid:ver" ), "", "pom", props, null );
        assertThat( a.getProperty( "key", null)).isEqualTo( "value1" );
        props.clear();
        assertThat( a.getProperty( "key", null)).isEqualTo( "value1" );

        props.put( "key", "value2" );
        a = a.setProperties( props );
        assertThat( a.getProperty( "key", null)).isEqualTo( "value2" );
        props.clear();
        assertThat( a.getProperty( "key", null)).isEqualTo( "value2" );
    }

}
