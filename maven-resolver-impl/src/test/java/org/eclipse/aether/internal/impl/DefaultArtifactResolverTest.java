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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.RepositoryEvent.EventType;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.UpdateCheckManager;
import org.eclipse.aether.impl.VersionResolver;
import org.eclipse.aether.internal.impl.DefaultArtifactResolver;
import org.eclipse.aether.internal.impl.DefaultUpdateCheckManager;
import org.eclipse.aether.internal.test.util.TestFileProcessor;
import org.eclipse.aether.internal.test.util.TestFileUtils;
import org.eclipse.aether.internal.test.util.TestLocalRepositoryManager;
import org.eclipse.aether.internal.test.util.TestUtils;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.LocalArtifactRegistration;
import org.eclipse.aether.repository.LocalArtifactRequest;
import org.eclipse.aether.repository.LocalArtifactResult;
import org.eclipse.aether.repository.LocalMetadataRegistration;
import org.eclipse.aether.repository.LocalMetadataRequest;
import org.eclipse.aether.repository.LocalMetadataResult;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.repository.WorkspaceReader;
import org.eclipse.aether.repository.WorkspaceRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRequest;
import org.eclipse.aether.resolution.VersionResolutionException;
import org.eclipse.aether.resolution.VersionResult;
import org.eclipse.aether.spi.connector.ArtifactDownload;
import org.eclipse.aether.spi.connector.MetadataDownload;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import org.eclipse.aether.transfer.ArtifactTransferException;
import org.eclipse.aether.util.repository.SimpleResolutionErrorPolicy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 */
public class DefaultArtifactResolverTest
{
    private DefaultArtifactResolver resolver;

    private DefaultRepositorySystemSession session;

    private TestLocalRepositoryManager lrm;

    private StubRepositoryConnectorProvider repositoryConnectorProvider;

    private Artifact artifact;

    private RecordingRepositoryConnector connector;

    @Before
    public void setup()
        throws IOException
    {
        UpdateCheckManager updateCheckManager = new StaticUpdateCheckManager( true );
        repositoryConnectorProvider = new StubRepositoryConnectorProvider();
        VersionResolver versionResolver = new StubVersionResolver();
        session = TestUtils.newSession();
        lrm = (TestLocalRepositoryManager) session.getLocalRepositoryManager();
        resolver = new DefaultArtifactResolver();
        resolver.setFileProcessor( new TestFileProcessor() );
        resolver.setRepositoryEventDispatcher( new StubRepositoryEventDispatcher() );
        resolver.setVersionResolver( versionResolver );
        resolver.setUpdateCheckManager( updateCheckManager );
        resolver.setRepositoryConnectorProvider( repositoryConnectorProvider );
        resolver.setRemoteRepositoryManager( new StubRemoteRepositoryManager() );
        resolver.setSyncContextFactory( new StubSyncContextFactory() );
        resolver.setOfflineController( new DefaultOfflineController() );

        artifact = new DefaultArtifact( "gid", "aid", "", "ext", "ver" );

        connector = new RecordingRepositoryConnector();
        repositoryConnectorProvider.setConnector( connector );
    }

    @After
    public void teardown()
        throws Exception
    {
        if ( session.getLocalRepository() != null )
        {
            TestFileUtils.deleteFile( session.getLocalRepository().getBasedir() );
        }
    }

    @Test
    public void testResolveLocalArtifactSuccessful()
        throws IOException, ArtifactResolutionException
    {
        File tmpFile = TestFileUtils.createTempFile( "tmp" );
        Map<String, String> properties = new HashMap<String, String>();
        properties.put( ArtifactProperties.LOCAL_PATH, tmpFile.getAbsolutePath() );
        artifact = artifact.setProperties( properties );

        ArtifactRequest request = new ArtifactRequest( artifact, null, "" );
        ArtifactResult result = resolver.resolveArtifact( session, request );

        assertThat(result.getExceptions().isEmpty() ).isTrue();

        Artifact resolved = result.getArtifact();
        assertThat(resolved.getFile() ).isNotNull();
        resolved = resolved.setFile( null );

        assertThat(resolved ).isEqualTo(artifact);
    }

    @Test
    public void testResolveLocalArtifactUnsuccessful()
        throws IOException, ArtifactResolutionException
    {
        File tmpFile = TestFileUtils.createTempFile( "tmp" );
        Map<String, String> properties = new HashMap<String, String>();
        properties.put( ArtifactProperties.LOCAL_PATH, tmpFile.getAbsolutePath() );
        artifact = artifact.setProperties( properties );

        tmpFile.delete();

        ArtifactRequest request = new ArtifactRequest( artifact, null, "" );

        try
        {
            resolver.resolveArtifact( session, request );
            fail( "expected exception" );
        }
        catch ( ArtifactResolutionException e )
        {
            assertThat(e.getResults() ).isNotNull();
            assertThat(e.getResults().size() ).isEqualTo(1);

            ArtifactResult result = e.getResults().get( 0 );

            assertThat(result.getRequest() ).isSameAs(request);

            assertThat(result.getExceptions().isEmpty() ).isFalse();
            assertThat(result.getExceptions().get( 0 ) instanceof ArtifactNotFoundException ).isTrue();

            Artifact resolved = result.getArtifact();
            assertThat(resolved ).isNull();
        }

    }

    @Test
    public void testResolveRemoteArtifact()
        throws IOException, ArtifactResolutionException
    {
        connector.setExpectGet( artifact );

        ArtifactRequest request = new ArtifactRequest( artifact, null, "" );
        request.addRepository( new RemoteRepository.Builder( "id", "default", "file:///" ).build() );

        ArtifactResult result = resolver.resolveArtifact( session, request );

        assertThat(result.getExceptions().isEmpty() ).isTrue();

        Artifact resolved = result.getArtifact();
        assertThat(resolved.getFile() ).isNotNull();

        resolved = resolved.setFile( null );
        assertThat(resolved ).isEqualTo(artifact);

        connector.assertSeenExpected();
    }

    @Test
    public void testResolveRemoteArtifactUnsuccessful()
        throws IOException, ArtifactResolutionException
    {
        RecordingRepositoryConnector connector = new RecordingRepositoryConnector()
        {

            @Override
            public void get( Collection<? extends ArtifactDownload> artifactDownloads,
                             Collection<? extends MetadataDownload> metadataDownloads )
            {
                super.get( artifactDownloads, metadataDownloads );
                ArtifactDownload download = artifactDownloads.iterator().next();
                ArtifactTransferException exception =
                    new ArtifactNotFoundException( download.getArtifact(), null, "not found" );
                download.setException( exception );
            }

        };

        connector.setExpectGet( artifact );
        repositoryConnectorProvider.setConnector( connector );

        ArtifactRequest request = new ArtifactRequest( artifact, null, "" );
        request.addRepository( new RemoteRepository.Builder( "id", "default", "file:///" ).build() );

        try
        {
            resolver.resolveArtifact( session, request );
            fail( "expected exception" );
        }
        catch ( ArtifactResolutionException e )
        {
            connector.assertSeenExpected();
            assertThat(e.getResults() ).isNotNull();
            assertThat(e.getResults().size() ).isEqualTo(1);

            ArtifactResult result = e.getResults().get( 0 );

            assertThat(result.getRequest() ).isSameAs(request);

            assertThat(result.getExceptions().isEmpty() ).isFalse();
            assertThat(result.getExceptions().get( 0 ) instanceof ArtifactNotFoundException ).isTrue();

            Artifact resolved = result.getArtifact();
            assertThat(resolved ).isNull();
        }

    }

    @Test
    public void testArtifactNotFoundCache()
        throws Exception
    {
        RecordingRepositoryConnector connector = new RecordingRepositoryConnector()
        {
            @Override
            public void get( Collection<? extends ArtifactDownload> artifactDownloads,
                             Collection<? extends MetadataDownload> metadataDownloads )
            {
                super.get( artifactDownloads, metadataDownloads );
                for ( ArtifactDownload download : artifactDownloads )
                {
                    download.getFile().delete();
                    ArtifactTransferException exception =
                        new ArtifactNotFoundException( download.getArtifact(), null, "not found" );
                    download.setException( exception );
                }
            }
        };

        repositoryConnectorProvider.setConnector( connector );
        resolver.setUpdateCheckManager( new DefaultUpdateCheckManager().setUpdatePolicyAnalyzer( new DefaultUpdatePolicyAnalyzer() ) );

        session.setResolutionErrorPolicy( new SimpleResolutionErrorPolicy( true, false ) );
        session.setUpdatePolicy( RepositoryPolicy.UPDATE_POLICY_NEVER );

        RemoteRepository remoteRepo = new RemoteRepository.Builder( "id", "default", "file:///" ).build();

        Artifact artifact1 = artifact;
        Artifact artifact2 = artifact.setVersion( "ver2" );

        ArtifactRequest request1 = new ArtifactRequest( artifact1, Arrays.asList( remoteRepo ), "" );
        ArtifactRequest request2 = new ArtifactRequest( artifact2, Arrays.asList( remoteRepo ), "" );

        connector.setExpectGet( new Artifact[] { artifact1, artifact2 } );
        try
        {
            resolver.resolveArtifacts( session, Arrays.asList( request1, request2 ) );
            fail( "expected exception" );
        }
        catch ( ArtifactResolutionException e )
        {
            connector.assertSeenExpected();
        }

        TestFileUtils.writeString( new File( lrm.getRepository().getBasedir(), lrm.getPathForLocalArtifact( artifact2 ) ),
                             "artifact" );
        lrm.setArtifactAvailability( artifact2, false );

        DefaultUpdateCheckManagerTest.resetSessionData( session );
        connector.resetActual();
        connector.setExpectGet( new Artifact[0] );
        try
        {
            resolver.resolveArtifacts( session, Arrays.asList( request1, request2 ) );
            fail( "expected exception" );
        }
        catch ( ArtifactResolutionException e )
        {
            connector.assertSeenExpected();
            for ( ArtifactResult result : e.getResults() )
            {
                Throwable t = result.getExceptions().get( 0 );
                assertThat( t ).isInstanceOf( ArtifactNotFoundException.class );
                assertThat( t ).hasMessage( "cached" );
            }
        }
    }

    @Test
    public void testResolveFromWorkspace()
        throws IOException, ArtifactResolutionException
    {
        WorkspaceReader workspace = new WorkspaceReader()
        {

            public WorkspaceRepository getRepository()
            {
                return new WorkspaceRepository( "default" );
            }

            public List<String> findVersions( Artifact artifact )
            {
                return Arrays.asList( artifact.getVersion() );
            }

            public File findArtifact( Artifact artifact )
            {
                try
                {
                    return TestFileUtils.createTempFile( artifact.toString() );
                }
                catch ( IOException e )
                {
                    throw new RuntimeException( e.getMessage(), e );
                }
            }
        };
        session.setWorkspaceReader( workspace );

        ArtifactRequest request = new ArtifactRequest( artifact, null, "" );
        request.addRepository( new RemoteRepository.Builder( "id", "default", "file:///" ).build() );

        ArtifactResult result = resolver.resolveArtifact( session, request );

        assertThat(result.getExceptions().isEmpty() ).isTrue();

        Artifact resolved = result.getArtifact();
        assertThat(resolved.getFile() ).isNotNull();

        assertThat(TestFileUtils.readString( resolved.getFile() ) ).isEqualTo(resolved.toString());

        resolved = resolved.setFile( null );
        assertThat(resolved ).isEqualTo(artifact);

        connector.assertSeenExpected();
    }

    @Test
    public void testResolveFromWorkspaceFallbackToRepository()
        throws ArtifactResolutionException
    {
        WorkspaceReader workspace = new WorkspaceReader()
        {

            public WorkspaceRepository getRepository()
            {
                return new WorkspaceRepository( "default" );
            }

            public List<String> findVersions( Artifact artifact )
            {
                return Arrays.asList( artifact.getVersion() );
            }

            public File findArtifact( Artifact artifact )
            {
                return null;
            }
        };
        session.setWorkspaceReader( workspace );

        connector.setExpectGet( artifact );
        repositoryConnectorProvider.setConnector( connector );

        ArtifactRequest request = new ArtifactRequest( artifact, null, "" );
        request.addRepository( new RemoteRepository.Builder( "id", "default", "file:///" ).build() );

        ArtifactResult result = resolver.resolveArtifact( session, request );

        assertThat( result.getExceptions().isEmpty() ).isTrue();

        Artifact resolved = result.getArtifact();
        assertThat(resolved.getFile() ).isNotNull();

        resolved = resolved.setFile( null );
        assertThat(resolved ).isEqualTo(artifact);

        connector.assertSeenExpected();
    }

    @Test
    public void testRepositoryEventsSuccessfulLocal()
        throws ArtifactResolutionException, IOException
    {
        RecordingRepositoryListener listener = new RecordingRepositoryListener();
        session.setRepositoryListener( listener );

        File tmpFile = TestFileUtils.createTempFile( "tmp" );
        Map<String, String> properties = new HashMap<String, String>();
        properties.put( ArtifactProperties.LOCAL_PATH, tmpFile.getAbsolutePath() );
        artifact = artifact.setProperties( properties );

        ArtifactRequest request = new ArtifactRequest( artifact, null, "" );
        resolver.resolveArtifact( session, request );

        List<RepositoryEvent> events = listener.getEvents();
        assertThat(events.size() ).isEqualTo(2);
        RepositoryEvent event = events.get( 0 );
        assertThat(event.getType() ).isEqualTo(EventType.ARTIFACT_RESOLVING);
        assertThat(event.getException() ).isNull();
        assertThat(event.getArtifact() ).isEqualTo(artifact);

        event = events.get( 1 );
        assertThat(event.getType() ).isEqualTo(EventType.ARTIFACT_RESOLVED);
        assertThat(event.getException() ).isNull();
        assertThat(event.getArtifact().setFile( null ) ).isEqualTo(artifact);
    }

    @Test
    public void testRepositoryEventsUnsuccessfulLocal()
        throws IOException
    {
        RecordingRepositoryListener listener = new RecordingRepositoryListener();
        session.setRepositoryListener( listener );

        Map<String, String> properties = new HashMap<String, String>();
        properties.put( ArtifactProperties.LOCAL_PATH, "doesnotexist" );
        artifact = artifact.setProperties( properties );

        ArtifactRequest request = new ArtifactRequest( artifact, null, "" );
        try
        {
            resolver.resolveArtifact( session, request );
            fail( "expected exception" );
        }
        catch ( ArtifactResolutionException e )
        {
        }

        List<RepositoryEvent> events = listener.getEvents();
        assertThat(events.size() ).isEqualTo(2);

        RepositoryEvent event = events.get( 0 );
        assertThat(event.getArtifact() ).isEqualTo(artifact);
        assertThat(event.getType() ).isEqualTo(EventType.ARTIFACT_RESOLVING);

        event = events.get( 1 );
        assertThat(event.getArtifact() ).isEqualTo(artifact);
        assertThat(event.getType() ).isEqualTo(EventType.ARTIFACT_RESOLVED);
        assertThat(event.getException() ).isNotNull();
        assertThat(event.getExceptions().size() ).isEqualTo(1);

    }

    @Test
    public void testRepositoryEventsSuccessfulRemote()
        throws ArtifactResolutionException
    {
        RecordingRepositoryListener listener = new RecordingRepositoryListener();
        session.setRepositoryListener( listener );

        ArtifactRequest request = new ArtifactRequest( artifact, null, "" );
        request.addRepository( new RemoteRepository.Builder( "id", "default", "file:///" ).build() );

        resolver.resolveArtifact( session, request );

        List<RepositoryEvent> events = listener.getEvents();
        assertThat(events ).hasSize( 4 );
        RepositoryEvent event = events.get( 0 );
        assertThat(event.getType() ).isEqualTo(EventType.ARTIFACT_RESOLVING);
        assertThat(event.getException() ).isNull();
        assertThat(event.getArtifact() ).isEqualTo(artifact);

        event = events.get( 1 );
        assertThat(event.getType() ).isEqualTo(EventType.ARTIFACT_DOWNLOADING);
        assertThat(event.getException() ).isNull();
        assertThat(event.getArtifact().setFile( null ) ).isEqualTo(artifact);

        event = events.get( 2 );
        assertThat(event.getType() ).isEqualTo(EventType.ARTIFACT_DOWNLOADED);
        assertThat(event.getException() ).isNull();
        assertThat(event.getArtifact().setFile( null ) ).isEqualTo(artifact);

        event = events.get( 3 );
        assertThat(event.getType() ).isEqualTo(EventType.ARTIFACT_RESOLVED);
        assertThat(event.getException() ).isNull();
        assertThat(event.getArtifact().setFile( null ) ).isEqualTo(artifact);
    }

    @Test
    public void testRepositoryEventsUnsuccessfulRemote()
        throws IOException, ArtifactResolutionException
    {
        RecordingRepositoryConnector connector = new RecordingRepositoryConnector()
        {

            @Override
            public void get( Collection<? extends ArtifactDownload> artifactDownloads,
                             Collection<? extends MetadataDownload> metadataDownloads )
            {
                super.get( artifactDownloads, metadataDownloads );
                ArtifactDownload download = artifactDownloads.iterator().next();
                ArtifactTransferException exception =
                    new ArtifactNotFoundException( download.getArtifact(), null, "not found" );
                download.setException( exception );
            }

        };
        repositoryConnectorProvider.setConnector( connector );

        RecordingRepositoryListener listener = new RecordingRepositoryListener();
        session.setRepositoryListener( listener );

        ArtifactRequest request = new ArtifactRequest( artifact, null, "" );
        request.addRepository( new RemoteRepository.Builder( "id", "default", "file:///" ).build() );

        try
        {
            resolver.resolveArtifact( session, request );
            fail( "expected exception" );
        }
        catch ( ArtifactResolutionException e )
        {
        }

        List<RepositoryEvent> events = listener.getEvents();
        assertThat( events ).hasSize( 4 );

        RepositoryEvent event = events.get( 0 );
        assertThat(event.getArtifact() ).isEqualTo(artifact);
        assertThat(event.getType() ).isEqualTo(EventType.ARTIFACT_RESOLVING);

        event = events.get( 1 );
        assertThat(event.getArtifact() ).isEqualTo(artifact);
        assertThat(event.getType() ).isEqualTo(EventType.ARTIFACT_DOWNLOADING);

        event = events.get( 2 );
        assertThat(event.getArtifact() ).isEqualTo(artifact);
        assertThat(event.getType() ).isEqualTo(EventType.ARTIFACT_DOWNLOADED);
        assertThat(event.getException() ).isNotNull();
        assertThat(event.getExceptions().size() ).isEqualTo(1);

        event = events.get( 3 );
        assertThat(event.getArtifact() ).isEqualTo(artifact);
        assertThat(event.getType() ).isEqualTo(EventType.ARTIFACT_RESOLVED);
        assertThat(event.getException() ).isNotNull();
        assertThat(event.getExceptions().size() ).isEqualTo(1);
    }

    @Test
    public void testVersionResolverFails()
    {
        resolver.setVersionResolver( new VersionResolver()
        {

            public VersionResult resolveVersion( RepositorySystemSession session, VersionRequest request )
                throws VersionResolutionException
            {
                throw new VersionResolutionException( new VersionResult( request ) );
            }
        } );

        ArtifactRequest request = new ArtifactRequest( artifact, null, "" );
        try
        {
            resolver.resolveArtifact( session, request );
            fail( "expected exception" );
        }
        catch ( ArtifactResolutionException e )
        {
            connector.assertSeenExpected();
            assertThat(e.getResults() ).isNotNull();
            assertThat(e.getResults().size() ).isEqualTo(1);

            ArtifactResult result = e.getResults().get( 0 );

            assertThat(result.getRequest() ).isSameAs(request);

            assertThat(result.getExceptions().isEmpty() ).isFalse();
            assertThat(result.getExceptions().get( 0 ) instanceof VersionResolutionException ).isTrue();

            Artifact resolved = result.getArtifact();
            assertThat(resolved ).isNull();
        }
    }

    @Test
    public void testRepositoryEventsOnVersionResolverFail()
    {
        resolver.setVersionResolver( new VersionResolver()
        {

            public VersionResult resolveVersion( RepositorySystemSession session, VersionRequest request )
                throws VersionResolutionException
            {
                throw new VersionResolutionException( new VersionResult( request ) );
            }
        } );

        RecordingRepositoryListener listener = new RecordingRepositoryListener();
        session.setRepositoryListener( listener );

        ArtifactRequest request = new ArtifactRequest( artifact, null, "" );
        try
        {
            resolver.resolveArtifact( session, request );
            fail( "expected exception" );
        }
        catch ( ArtifactResolutionException e )
        {
        }

        List<RepositoryEvent> events = listener.getEvents();
        assertThat(events.size() ).isEqualTo(2);

        RepositoryEvent event = events.get( 0 );
        assertThat(event.getArtifact() ).isEqualTo(artifact);
        assertThat(event.getType() ).isEqualTo(EventType.ARTIFACT_RESOLVING);

        event = events.get( 1 );
        assertThat(event.getArtifact() ).isEqualTo(artifact);
        assertThat(event.getType() ).isEqualTo(EventType.ARTIFACT_RESOLVED);
        assertThat(event.getException() ).isNotNull();
        assertThat(event.getExceptions().size() ).isEqualTo(1);
    }

    @Test
    public void testLocalArtifactAvailable()
        throws ArtifactResolutionException
    {
        session.setLocalRepositoryManager( new LocalRepositoryManager()
        {

            public LocalRepository getRepository()
            {
                return null;
            }

            public String getPathForRemoteMetadata( Metadata metadata, RemoteRepository repository, String context )
            {
                return null;
            }

            public String getPathForRemoteArtifact( Artifact artifact, RemoteRepository repository, String context )
            {
                return null;
            }

            public String getPathForLocalMetadata( Metadata metadata )
            {
                return null;
            }

            public String getPathForLocalArtifact( Artifact artifact )
            {
                return null;
            }

            public LocalArtifactResult find( RepositorySystemSession session, LocalArtifactRequest request )
            {

                LocalArtifactResult result = new LocalArtifactResult( request );
                result.setAvailable( true );
                try
                {
                    result.setFile( TestFileUtils.createTempFile( "" ) );
                }
                catch ( IOException e )
                {
                    e.printStackTrace();
                }
                return result;
            }

            public void add( RepositorySystemSession session, LocalArtifactRegistration request )
            {
            }

            public LocalMetadataResult find( RepositorySystemSession session, LocalMetadataRequest request )
            {
                LocalMetadataResult result = new LocalMetadataResult( request );
                try
                {
                    result.setFile( TestFileUtils.createTempFile( "" ) );
                }
                catch ( IOException e )
                {
                    e.printStackTrace();
                }
                return result;
            }

            public void add( RepositorySystemSession session, LocalMetadataRegistration request )
            {
            }
        } );

        ArtifactRequest request = new ArtifactRequest( artifact, null, "" );
        request.addRepository( new RemoteRepository.Builder( "id", "default", "file:///" ).build() );

        ArtifactResult result = resolver.resolveArtifact( session, request );

        assertThat(result.getExceptions().isEmpty() ).isTrue();

        Artifact resolved = result.getArtifact();
        assertThat(resolved.getFile() ).isNotNull();

        resolved = resolved.setFile( null );
        assertThat(resolved ).isEqualTo(artifact);

    }

    @Test
    public void testFindInLocalRepositoryWhenVersionWasFoundInLocalRepository()
        throws ArtifactResolutionException
    {
        session.setLocalRepositoryManager( new LocalRepositoryManager()
        {

            public LocalRepository getRepository()
            {
                return null;
            }

            public String getPathForRemoteMetadata( Metadata metadata, RemoteRepository repository, String context )
            {
                return null;
            }

            public String getPathForRemoteArtifact( Artifact artifact, RemoteRepository repository, String context )
            {
                return null;
            }

            public String getPathForLocalMetadata( Metadata metadata )
            {
                return null;
            }

            public String getPathForLocalArtifact( Artifact artifact )
            {
                return null;
            }

            public LocalArtifactResult find( RepositorySystemSession session, LocalArtifactRequest request )
            {

                LocalArtifactResult result = new LocalArtifactResult( request );
                result.setAvailable( false );
                try
                {
                    result.setFile( TestFileUtils.createTempFile( "" ) );
                }
                catch ( IOException e )
                {
                    e.printStackTrace();
                }
                return result;
            }

            public void add( RepositorySystemSession session, LocalArtifactRegistration request )
            {
            }

            public LocalMetadataResult find( RepositorySystemSession session, LocalMetadataRequest request )
            {
                LocalMetadataResult result = new LocalMetadataResult( request );
                return result;
            }

            public void add( RepositorySystemSession session, LocalMetadataRegistration request )
            {
            }
        } );
        ArtifactRequest request = new ArtifactRequest( artifact, null, "" );
        request.addRepository( new RemoteRepository.Builder( "id", "default", "file:///" ).build() );

        resolver.setVersionResolver( new VersionResolver()
        {

            public VersionResult resolveVersion( RepositorySystemSession session, VersionRequest request )
                throws VersionResolutionException
            {
                return new VersionResult( request ).setRepository( new LocalRepository( "id" ) ).setVersion( request.getArtifact().getVersion() );
            }
        } );
        ArtifactResult result = resolver.resolveArtifact( session, request );

        assertThat(result.getExceptions().isEmpty() ).isTrue();

        Artifact resolved = result.getArtifact();
        assertThat(resolved.getFile() ).isNotNull();

        resolved = resolved.setFile( null );
        assertThat(resolved ).isEqualTo(artifact);
    }

    @Test
    public void testFindInLocalRepositoryWhenVersionRangeWasResolvedFromLocalRepository()
        throws ArtifactResolutionException
    {
        session.setLocalRepositoryManager( new LocalRepositoryManager()
        {

            public LocalRepository getRepository()
            {
                return null;
            }

            public String getPathForRemoteMetadata( Metadata metadata, RemoteRepository repository, String context )
            {
                return null;
            }

            public String getPathForRemoteArtifact( Artifact artifact, RemoteRepository repository, String context )
            {
                return null;
            }

            public String getPathForLocalMetadata( Metadata metadata )
            {
                return null;
            }

            public String getPathForLocalArtifact( Artifact artifact )
            {
                return null;
            }

            public LocalArtifactResult find( RepositorySystemSession session, LocalArtifactRequest request )
            {

                LocalArtifactResult result = new LocalArtifactResult( request );
                result.setAvailable( false );
                try
                {
                    result.setFile( TestFileUtils.createTempFile( "" ) );
                }
                catch ( IOException e )
                {
                    e.printStackTrace();
                }
                return result;
            }

            public void add( RepositorySystemSession session, LocalArtifactRegistration request )
            {
            }

            public LocalMetadataResult find( RepositorySystemSession session, LocalMetadataRequest request )
            {
                LocalMetadataResult result = new LocalMetadataResult( request );
                return result;
            }

            public void add( RepositorySystemSession session, LocalMetadataRegistration request )
            {
            }

        } );
        ArtifactRequest request = new ArtifactRequest( artifact, null, "" );

        resolver.setVersionResolver( new VersionResolver()
        {

            public VersionResult resolveVersion( RepositorySystemSession session, VersionRequest request )
                throws VersionResolutionException
            {
                return new VersionResult( request ).setVersion( request.getArtifact().getVersion() );
            }
        } );
        ArtifactResult result = resolver.resolveArtifact( session, request );

        assertThat(result.getExceptions().isEmpty() ).isTrue();

        Artifact resolved = result.getArtifact();
        assertThat(resolved.getFile() ).isNotNull();

        resolved = resolved.setFile( null );
        assertThat(resolved ).isEqualTo(artifact);
    }

}
