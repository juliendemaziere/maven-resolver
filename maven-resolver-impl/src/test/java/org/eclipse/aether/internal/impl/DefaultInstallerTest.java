package org.eclipse.aether.internal.impl;

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
import java.io.IOException;
import java.util.List;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryEvent.EventType;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallResult;
import org.eclipse.aether.installation.InstallationException;
import org.eclipse.aether.internal.impl.DefaultFileProcessor;
import org.eclipse.aether.internal.impl.DefaultInstaller;
import org.eclipse.aether.internal.test.util.TestFileProcessor;
import org.eclipse.aether.internal.test.util.TestFileUtils;
import org.eclipse.aether.internal.test.util.TestLocalRepositoryManager;
import org.eclipse.aether.internal.test.util.TestUtils;
import org.eclipse.aether.metadata.DefaultMetadata;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.metadata.Metadata.Nature;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DefaultInstallerTest
{

    private Artifact artifact;

    private Metadata metadata;

    private DefaultRepositorySystemSession session;

    private String localArtifactPath;

    private String localMetadataPath;

    private DefaultInstaller installer;

    private InstallRequest request;

    private RecordingRepositoryListener listener;

    private File localArtifactFile;

    private TestLocalRepositoryManager lrm;

    @Before
    public void setup()
        throws IOException
    {
        artifact = new DefaultArtifact( "gid", "aid", "jar", "ver" );
        artifact = artifact.setFile( TestFileUtils.createTempFile( "artifact".getBytes(), 1 ) );
        metadata =
            new DefaultMetadata( "gid", "aid", "ver", "type", Nature.RELEASE_OR_SNAPSHOT,
                                 TestFileUtils.createTempFile( "metadata".getBytes(), 1 ) );

        session = TestUtils.newSession();
        localArtifactPath = session.getLocalRepositoryManager().getPathForLocalArtifact( artifact );
        localMetadataPath = session.getLocalRepositoryManager().getPathForLocalMetadata( metadata );

        localArtifactFile = new File( session.getLocalRepository().getBasedir(), localArtifactPath );

        installer = new DefaultInstaller();
        installer.setFileProcessor( new TestFileProcessor() );
        installer.setRepositoryEventDispatcher( new StubRepositoryEventDispatcher() );
        installer.setSyncContextFactory( new StubSyncContextFactory() );
        request = new InstallRequest();
        listener = new RecordingRepositoryListener();
        session.setRepositoryListener( listener );

        lrm = (TestLocalRepositoryManager) session.getLocalRepositoryManager();

        TestFileUtils.deleteFile( session.getLocalRepository().getBasedir() );
    }

    @After
    public void teardown()
        throws Exception
    {
        TestFileUtils.deleteFile( session.getLocalRepository().getBasedir() );
    }

    @Test
    public void testSuccessfulInstall()
        throws InstallationException, IOException
    {
        File artifactFile =
            new File( session.getLocalRepositoryManager().getRepository().getBasedir(), localArtifactPath );
        File metadataFile =
            new File( session.getLocalRepositoryManager().getRepository().getBasedir(), localMetadataPath );

        artifactFile.delete();
        metadataFile.delete();

        request.addArtifact( artifact );
        request.addMetadata( metadata );

        InstallResult result = installer.install( session, request );

        assertThat(artifactFile.exists() ).isTrue();
        assertThat(TestFileUtils.readString( artifactFile ) ).isEqualTo("artifact");

        assertThat(metadataFile.exists() ).isTrue();
        assertThat(TestFileUtils.readString( metadataFile ) ).isEqualTo("metadata");

        assertThat(request ).isEqualTo(result.getRequest());

        assertThat(1 ).isEqualTo(result.getArtifacts().size());
        assertThat(result.getArtifacts().contains( artifact ) ).isTrue();

        assertThat(1 ).isEqualTo(result.getMetadata().size());
        assertThat(result.getMetadata().contains( metadata ) ).isTrue();

        assertThat(lrm.getMetadataRegistration().size() ).isEqualTo(1);
        assertThat(lrm.getMetadataRegistration().contains( metadata ) ).isTrue();
        assertThat(lrm.getArtifactRegistration().size() ).isEqualTo(1);
        assertThat(lrm.getArtifactRegistration().contains( artifact ) ).isTrue();
    }

    @Test( expected = InstallationException.class )
    public void testNullArtifactFile()
        throws InstallationException
    {
        InstallRequest request = new InstallRequest();
        request.addArtifact( artifact.setFile( null ) );

        installer.install( session, request );
    }

    @Test( expected = InstallationException.class )
    public void testNullMetadataFile()
        throws InstallationException
    {
        InstallRequest request = new InstallRequest();
        request.addMetadata( metadata.setFile( null ) );

        installer.install( session, request );
    }

    @Test( expected = InstallationException.class )
    public void testNonExistentArtifactFile()
        throws InstallationException
    {
        InstallRequest request = new InstallRequest();
        request.addArtifact( artifact.setFile( new File( "missing.txt" ) ) );

        installer.install( session, request );
    }

    @Test( expected = InstallationException.class )
    public void testNonExistentMetadataFile()
        throws InstallationException
    {
        InstallRequest request = new InstallRequest();
        request.addMetadata( metadata.setFile( new File( "missing.xml" ) ) );

        installer.install( session, request );
    }

    @Test( expected = InstallationException.class )
    public void testArtifactExistsAsDir()
        throws InstallationException
    {
        String path = session.getLocalRepositoryManager().getPathForLocalArtifact( artifact );
        File file = new File( session.getLocalRepository().getBasedir(), path );
        assertThat( file ).isDirectory();
        assertThat( file ).doesNotExist();
        assertThat(file.mkdirs() || file.isDirectory() )
                .as( "failed to setup test: could not create " + file.getAbsolutePath()).isTrue();

        request.addArtifact( artifact );
        installer.install( session, request );
    }

    @Test( expected = InstallationException.class )
    public void testMetadataExistsAsDir()
        throws InstallationException
    {
        String path = session.getLocalRepositoryManager().getPathForLocalMetadata( metadata );
        assertThat( new File( session.getLocalRepository().getBasedir(), path ).mkdirs() ).isTrue();

        request.addMetadata( metadata );
        installer.install( session, request );
    }

    @Test( expected = InstallationException.class )
    public void testArtifactDestinationEqualsSource()
        throws Exception
    {
        String path = session.getLocalRepositoryManager().getPathForLocalArtifact( artifact );
        File file = new File( session.getLocalRepository().getBasedir(), path );
        artifact = artifact.setFile( file );
        TestFileUtils.writeString( file, "test" );

        request.addArtifact( artifact );
        installer.install( session, request );
    }

    @Test( expected = InstallationException.class )
    public void testMetadataDestinationEqualsSource()
        throws Exception
    {
        String path = session.getLocalRepositoryManager().getPathForLocalMetadata( metadata );
        File file = new File( session.getLocalRepository().getBasedir(), path );
        metadata = metadata.setFile( file );
        TestFileUtils.writeString( file, "test" );

        request.addMetadata( metadata );
        installer.install( session, request );
    }

    @Test
    public void testSuccessfulArtifactEvents()
        throws InstallationException
    {
        InstallRequest request = new InstallRequest();
        request.addArtifact( artifact );

        installer.install( session, request );
        checkEvents( "Repository Event problem", artifact, false );
    }

    @Test
    public void testSuccessfulMetadataEvents()
        throws InstallationException
    {
        InstallRequest request = new InstallRequest();
        request.addMetadata( metadata );

        installer.install( session, request );
        checkEvents( "Repository Event problem", metadata, false );
    }

    @Test
    public void testFailingEventsNullArtifactFile()
    {
        checkFailedEvents( "null artifact file", this.artifact.setFile( null ) );
    }

    @Test
    public void testFailingEventsNullMetadataFile()
    {
        checkFailedEvents( "null metadata file", this.metadata.setFile( null ) );
    }

    @Test
    public void testFailingEventsArtifactExistsAsDir()
    {
        String path = session.getLocalRepositoryManager().getPathForLocalArtifact( artifact );
        assertThat( new File( session.getLocalRepository().getBasedir(), path ).mkdirs() );
        checkFailedEvents( "target exists as dir", artifact );
    }

    @Test
    public void testFailingEventsMetadataExistsAsDir()
    {
        String path = session.getLocalRepositoryManager().getPathForLocalMetadata( metadata );
        assertThat( new File( session.getLocalRepository().getBasedir(), path ).mkdirs() );
        checkFailedEvents( "target exists as dir", metadata );
    }

    private void checkFailedEvents( String msg, Metadata metadata )
    {
        InstallRequest request = new InstallRequest().addMetadata( metadata );
        msg = "Repository events problem (case: " + msg + ")";

        try
        {
            installer.install( session, request );
            fail( "expected exception" );
        }
        catch ( InstallationException e )
        {
            checkEvents( msg, metadata, true );
        }

    }

    private void checkEvents( String msg, Metadata metadata, boolean failed )
    {
        List<RepositoryEvent> events = listener.getEvents();
        assertThat( events ).hasSize( 2 );
        RepositoryEvent event = events.get( 0 );
        assertThat( event.getType()).isEqualTo( EventType.ARTIFACT_INSTALLING );
        assertThat( event.getArtifact() ).isEqualTo( artifact );
        assertThat( event.getException() ).isNull();

        event = events.get( 1 );
        assertThat( event.getType() ).isEqualTo(  EventType.ARTIFACT_INSTALLED );
        assertThat( event.getArtifact() ).isEqualTo( artifact ) ;
        if ( failed )
        {
            assertThat( event.getException() ).isNotNull();
        }
        else
        {
            assertThat( event.getException() ).isNull();
        }
    }

    private void checkFailedEvents( String msg, Artifact artifact )
    {
        InstallRequest request = new InstallRequest().addArtifact( artifact );
        msg = "Repository events problem (case: " + msg + ")";

        try
        {
            installer.install( session, request );
            fail( "expected exception" );
        }
        catch ( InstallationException e )
        {
            checkEvents( msg, artifact, true );
        }
    }

    private void checkEvents( String msg, Artifact artifact, boolean failed )
    {
        List<RepositoryEvent> events = listener.getEvents();
        assertThat( events ).hasSize( 2 );
        RepositoryEvent event = events.get( 0 );
        assertThat( event.getType()).isEqualTo( EventType.ARTIFACT_INSTALLING );
        assertThat( event.getArtifact() ).isEqualTo( artifact );
        assertThat( event.getException() ).isNull();

        event = events.get( 1 );
        assertThat( event.getType() ).isEqualTo(  EventType.ARTIFACT_INSTALLED );
        assertThat( event.getArtifact() ).isEqualTo( artifact ) ;
        if ( failed )
        {
            assertThat( event.getException() ).isNotNull();
        }
        else
        {
            assertThat( event.getException() ).isNull();
        }
    }

    @Test
    public void testDoNotUpdateUnchangedArtifact()
        throws InstallationException
    {
        request.addArtifact( artifact );
        installer.install( session, request );

        installer.setFileProcessor( new DefaultFileProcessor()
        {
            @Override
            public long copy( File src, File target, ProgressListener listener )
                throws IOException
            {
                throw new IOException( "copy called" );
            }
        } );

        request = new InstallRequest();
        request.addArtifact( artifact );
        installer.install( session, request );
    }

    @Test
    public void testSetArtifactTimestamps()
        throws InstallationException
    {
        artifact.getFile().setLastModified( artifact.getFile().lastModified() - 60000 );

        request.addArtifact( artifact );

        installer.install( session, request );

        assertThat( artifact.getFile().lastModified()).isEqualTo(localArtifactFile.lastModified() );

        request = new InstallRequest();

        request.addArtifact( artifact );

        artifact.getFile().setLastModified( artifact.getFile().lastModified() - 60000 );

        installer.install( session, request );

        assertThat( artifact.getFile().lastModified()).isEqualTo( localArtifactFile.lastModified() );
    }
}
