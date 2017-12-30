package org.eclipse.aether.transport.http;

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
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.http.client.HttpResponseException;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.pool.ConnPoolControl;
import org.apache.http.pool.PoolStats;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.internal.test.util.TestFileUtils;
import org.eclipse.aether.internal.test.util.TestUtils;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.transport.GetTask;
import org.eclipse.aether.spi.connector.transport.PeekTask;
import org.eclipse.aether.spi.connector.transport.PutTask;
import org.eclipse.aether.spi.connector.transport.Transporter;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transfer.NoTransporterException;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

/**
 */
public class HttpTransporterTest
{

    static
    {
        System.setProperty( "javax.net.ssl.trustStore",
                            new File( "src/test/resources/ssl/server-store" ).getAbsolutePath() );
        System.setProperty( "javax.net.ssl.trustStorePassword", "server-pwd" );
        System.setProperty( "javax.net.ssl.keyStore",
                            new File( "src/test/resources/ssl/client-store" ).getAbsolutePath() );
        System.setProperty( "javax.net.ssl.keyStorePassword", "client-pwd" );
    }

    @Rule
    public TestName testName = new TestName();

    private DefaultRepositorySystemSession session;

    private TransporterFactory factory;

    private Transporter transporter;

    private File repoDir;

    private HttpServer httpServer;

    private Authentication auth;

    private Proxy proxy;

    private RemoteRepository newRepo( String url )
    {
        return new RemoteRepository.Builder( "test", "default", url ).setAuthentication( auth ).setProxy( proxy ).build();
    }

    private void newTransporter( String url )
        throws Exception
    {
        if ( transporter != null )
        {
            transporter.close();
            transporter = null;
        }
        transporter = factory.newInstance( session, newRepo( url ) );
    }

    @Before
    public void setUp()
        throws Exception
    {
        System.out.println( "=== " + testName.getMethodName() + " ===" );
        session = TestUtils.newSession();
        factory = new HttpTransporterFactory( );
        repoDir = TestFileUtils.createTempDir();
        TestFileUtils.writeString( new File( repoDir, "file.txt" ), "test" );
        TestFileUtils.writeString( new File( repoDir, "dir/file.txt" ), "test" );
        TestFileUtils.writeString( new File( repoDir, "empty.txt" ), "" );
        TestFileUtils.writeString( new File( repoDir, "some space.txt" ), "space" );
        File resumable = new File( repoDir, "resume.txt" );
        TestFileUtils.writeString( resumable, "resumable" );
        resumable.setLastModified( System.currentTimeMillis() - 90 * 1000 );
        httpServer = new HttpServer().setRepoDir( repoDir ).start();
        newTransporter( httpServer.getHttpUrl() );
    }

    @After
    public void tearDown()
        throws Exception
    {
        if ( transporter != null )
        {
            transporter.close();
            transporter = null;
        }
        if ( httpServer != null )
        {
            httpServer.stop();
            httpServer = null;
        }
        factory = null;
        session = null;
    }

    @Test
    public void testClassify()
    {
        assertThat( transporter.classify( new FileNotFoundException() ) ).isEqualTo( Transporter.ERROR_OTHER );
        assertThat( transporter.classify( new HttpResponseException( 403, "Forbidden" ) ) ).isEqualTo( Transporter.ERROR_OTHER );
        assertThat( transporter.classify( new HttpResponseException( 404, "Not Found" ) ) ).isEqualTo( Transporter.ERROR_NOT_FOUND );
    }

    @Test
    public void testPeek()
        throws Exception
    {
        transporter.peek( new PeekTask( URI.create( "repo/file.txt" ) ) );
    }

    @Test
    public void testPeek_NotFound()
        throws Exception
    {
        try
        {
            transporter.peek( new PeekTask( URI.create( "repo/missing.txt" ) ) );
            fail( "Expected error" );
        }
        catch ( HttpResponseException e )
        {
            assertThat(e.getStatusCode() ).isEqualTo(404);
            assertThat(transporter.classify( e ) ).isEqualTo(Transporter.ERROR_NOT_FOUND);
        }
    }

    @Test
    public void testPeek_Closed()
        throws Exception
    {
        transporter.close();
        try
        {
            transporter.peek( new PeekTask( URI.create( "repo/missing.txt" ) ) );
            fail( "Expected error" );
        }
        catch ( IllegalStateException e )
        {
            assertThat(transporter.classify( e ) ).isEqualTo(Transporter.ERROR_OTHER);
        }
    }

    @Test
    public void testPeek_Authenticated()
        throws Exception
    {
        httpServer.setAuthentication( "testuser", "testpass" );
        auth = new AuthenticationBuilder().addUsername( "testuser" ).addPassword( "testpass" ).build();
        newTransporter( httpServer.getHttpUrl() );
        transporter.peek( new PeekTask( URI.create( "repo/file.txt" ) ) );
    }

    @Test
    public void testPeek_Unauthenticated()
        throws Exception
    {
        httpServer.setAuthentication( "testuser", "testpass" );
        try
        {
            transporter.peek( new PeekTask( URI.create( "repo/file.txt" ) ) );
            fail( "Expected error" );
        }
        catch ( HttpResponseException e )
        {
            assertThat(e.getStatusCode() ).isEqualTo(401);
            assertThat(transporter.classify( e ) ).isEqualTo(Transporter.ERROR_OTHER);
        }
    }

    @Test
    public void testPeek_ProxyAuthenticated()
        throws Exception
    {
        httpServer.setProxyAuthentication( "testuser", "testpass" );
        auth = new AuthenticationBuilder().addUsername( "testuser" ).addPassword( "testpass" ).build();
        proxy = new Proxy( Proxy.TYPE_HTTP, httpServer.getHost(), httpServer.getHttpPort(), auth );
        newTransporter( "http://bad.localhost:1/" );
        transporter.peek( new PeekTask( URI.create( "repo/file.txt" ) ) );
    }

    @Test
    public void testPeek_ProxyUnauthenticated()
        throws Exception
    {
        httpServer.setProxyAuthentication( "testuser", "testpass" );
        proxy = new Proxy( Proxy.TYPE_HTTP, httpServer.getHost(), httpServer.getHttpPort() );
        newTransporter( "http://bad.localhost:1/" );
        try
        {
            transporter.peek( new PeekTask( URI.create( "repo/file.txt" ) ) );
            fail( "Expected error" );
        }
        catch ( HttpResponseException e )
        {
            assertThat(e.getStatusCode() ).isEqualTo(407);
            assertThat(transporter.classify( e ) ).isEqualTo(Transporter.ERROR_OTHER);
        }
    }

    @Test
    public void testPeek_SSL()
        throws Exception
    {
        httpServer.addSslConnector();
        newTransporter( httpServer.getHttpsUrl() );
        transporter.peek( new PeekTask( URI.create( "repo/file.txt" ) ) );
    }

    @Test
    public void testPeek_Redirect()
        throws Exception
    {
        httpServer.addSslConnector();
        transporter.peek( new PeekTask( URI.create( "redirect/file.txt" ) ) );
        transporter.peek( new PeekTask( URI.create( "redirect/file.txt?scheme=https" ) ) );
    }

    @Test
    public void testGet_ToMemory()
        throws Exception
    {
        RecordingTransportListener listener = new RecordingTransportListener();
        GetTask task = new GetTask( URI.create( "repo/file.txt" ) ).setListener( listener );
        transporter.get( task );
        assertThat(task.getDataString() ).isEqualTo("test");
        assertThat(listener.dataOffset ).isEqualTo(0L);
        assertThat(listener.dataLength ).isEqualTo(4L);
        assertThat(listener.startedCount ).isEqualTo(1);
        assertThat(listener.progressedCount).as("Count: " + listener.progressedCount).isGreaterThan( 0 );
        assertThat(new String( listener.baos.toByteArray(), StandardCharsets.UTF_8)).isEqualTo( task.getDataString() );
    }

    @Test
    public void testGet_ToFile()
        throws Exception
    {
        File file = TestFileUtils.createTempFile( "failure" );
        RecordingTransportListener listener = new RecordingTransportListener();
        GetTask task = new GetTask( URI.create( "repo/file.txt" ) ).setDataFile( file ).setListener( listener );
        transporter.get( task );
        assertThat(TestFileUtils.readString( file ) ).isEqualTo("test");
        assertThat(listener.dataOffset ).isEqualTo(0L);
        assertThat(listener.dataLength ).isEqualTo(4L);
        assertThat(listener.startedCount ).isEqualTo(1);
        assertThat(listener.progressedCount).as("Count: " + listener.progressedCount).isGreaterThan( 0 );
        assertThat(new String( listener.baos.toByteArray(), StandardCharsets.UTF_8)).isEqualTo( "test" );
    }

    @Test
    public void testGet_EmptyResource()
        throws Exception
    {
        File file = TestFileUtils.createTempFile( "failure" );
        RecordingTransportListener listener = new RecordingTransportListener();
        GetTask task = new GetTask( URI.create( "repo/empty.txt" ) ).setDataFile( file ).setListener( listener );
        transporter.get( task );
        assertThat(TestFileUtils.readString( file ) ).isEqualTo("");
        assertThat(listener.dataOffset ).isEqualTo(0L);
        assertThat(listener.dataLength ).isEqualTo(0L);
        assertThat(listener.startedCount ).isEqualTo(1);
        assertThat(listener.progressedCount ).isEqualTo(0);
        assertThat(new String( listener.baos.toByteArray(), StandardCharsets.UTF_8)).isEqualTo( "" );
    }

    @Test
    public void testGet_EncodedResourcePath()
        throws Exception
    {
        GetTask task = new GetTask( URI.create( "repo/some%20space.txt" ) );
        transporter.get( task );
        assertThat(task.getDataString() ).isEqualTo("space");
    }

    @Test
    public void testGet_Authenticated()
        throws Exception
    {
        httpServer.setAuthentication( "testuser", "testpass" );
        auth = new AuthenticationBuilder().addUsername( "testuser" ).addPassword( "testpass" ).build();
        newTransporter( httpServer.getHttpUrl() );
        RecordingTransportListener listener = new RecordingTransportListener();
        GetTask task = new GetTask( URI.create( "repo/file.txt" ) ).setListener( listener );
        transporter.get( task );
        assertThat(task.getDataString() ).isEqualTo("test");
        assertThat(listener.dataOffset ).isEqualTo(0L);
        assertThat(listener.dataLength ).isEqualTo(4L);
        assertThat(listener.startedCount ).isEqualTo(1);
        assertThat(listener.progressedCount).as("Count: " + listener.progressedCount).isGreaterThan( 0 );
        assertThat(new String( listener.baos.toByteArray(), StandardCharsets.UTF_8)).isEqualTo( task.getDataString() );
    }

    @Test
    public void testGet_Unauthenticated()
        throws Exception
    {
        httpServer.setAuthentication( "testuser", "testpass" );
        try
        {
            transporter.get( new GetTask( URI.create( "repo/file.txt" ) ) );
            fail( "Expected error" );
        }
        catch ( HttpResponseException e )
        {
            assertThat(e.getStatusCode() ).isEqualTo(401);
            assertThat(transporter.classify( e ) ).isEqualTo(Transporter.ERROR_OTHER);
        }
    }

    @Test
    public void testGet_ProxyAuthenticated()
        throws Exception
    {
        httpServer.setProxyAuthentication( "testuser", "testpass" );
        Authentication auth = new AuthenticationBuilder().addUsername( "testuser" ).addPassword( "testpass" ).build();
        proxy = new Proxy( Proxy.TYPE_HTTP, httpServer.getHost(), httpServer.getHttpPort(), auth );
        newTransporter( "http://bad.localhost:1/" );
        RecordingTransportListener listener = new RecordingTransportListener();
        GetTask task = new GetTask( URI.create( "repo/file.txt" ) ).setListener( listener );
        transporter.get( task );
        assertThat(task.getDataString() ).isEqualTo("test");
        assertThat(listener.dataOffset ).isEqualTo(0L);
        assertThat(listener.dataLength ).isEqualTo(4L);
        assertThat(listener.startedCount ).isEqualTo(1);
        assertThat(listener.progressedCount).as("Count: " + listener.progressedCount).isGreaterThan( 0 );
        assertThat(new String( listener.baos.toByteArray(), StandardCharsets.UTF_8)).isEqualTo( task.getDataString() );
    }

    @Test
    public void testGet_ProxyUnauthenticated()
        throws Exception
    {
        httpServer.setProxyAuthentication( "testuser", "testpass" );
        proxy = new Proxy( Proxy.TYPE_HTTP, httpServer.getHost(), httpServer.getHttpPort() );
        newTransporter( "http://bad.localhost:1/" );
        try
        {
            transporter.get( new GetTask( URI.create( "repo/file.txt" ) ) );
            fail( "Expected error" );
        }
        catch ( HttpResponseException e )
        {
            assertThat(e.getStatusCode() ).isEqualTo(407);
            assertThat(transporter.classify( e ) ).isEqualTo(Transporter.ERROR_OTHER);
        }
    }

    @Test
    public void testGet_SSL()
        throws Exception
    {
        httpServer.addSslConnector();
        newTransporter( httpServer.getHttpsUrl() );
        RecordingTransportListener listener = new RecordingTransportListener();
        GetTask task = new GetTask( URI.create( "repo/file.txt" ) ).setListener( listener );
        transporter.get( task );
        assertThat(task.getDataString() ).isEqualTo("test");
        assertThat(listener.dataOffset ).isEqualTo(0L);
        assertThat(listener.dataLength ).isEqualTo(4L);
        assertThat(listener.startedCount ).isEqualTo(1);
        assertThat(listener.progressedCount).as("Count: " + listener.progressedCount).isGreaterThan( 0 );
        assertThat(new String( listener.baos.toByteArray(), StandardCharsets.UTF_8)).isEqualTo( task.getDataString() );
    }

    @Test
    public void testGet_WebDav()
        throws Exception
    {
        httpServer.setWebDav( true );
        RecordingTransportListener listener = new RecordingTransportListener();
        GetTask task = new GetTask( URI.create( "repo/dir/file.txt" ) ).setListener( listener );
        ( (HttpTransporter) transporter ).getState().setWebDav( true );
        transporter.get( task );
        assertThat(task.getDataString() ).isEqualTo("test");
        assertThat(listener.dataOffset ).isEqualTo(0L);
        assertThat(listener.dataLength ).isEqualTo(4L);
        assertThat(listener.startedCount ).isEqualTo(1);
        assertThat(listener.progressedCount).as("Count: " + listener.progressedCount).isGreaterThan( 0 );
        assertThat(new String( listener.baos.toByteArray(), StandardCharsets.UTF_8)).isEqualTo( task.getDataString() );
        assertThat( httpServer.getLogEntries() ).as(httpServer.getLogEntries().toString()).hasSize( 1 );
    }

    @Test
    public void testGet_Redirect()
        throws Exception
    {
        httpServer.addSslConnector();
        RecordingTransportListener listener = new RecordingTransportListener();
        GetTask task = new GetTask( URI.create( "redirect/file.txt?scheme=https" ) ).setListener( listener );
        transporter.get( task );
        assertThat(task.getDataString() ).isEqualTo("test");
        assertThat(listener.dataOffset ).isEqualTo(0L);
        assertThat(listener.dataLength ).isEqualTo(4L);
        assertThat(listener.startedCount ).isEqualTo(1);
        assertThat(listener.progressedCount).as("Count: " + listener.progressedCount).isGreaterThan( 0 );
        assertThat(new String( listener.baos.toByteArray(), StandardCharsets.UTF_8)).isEqualTo( task.getDataString() );
    }

    @Test
    public void testGet_Resume()
        throws Exception
    {
        File file = TestFileUtils.createTempFile( "re" );
        RecordingTransportListener listener = new RecordingTransportListener();
        GetTask task = new GetTask( URI.create( "repo/resume.txt" ) ).setDataFile( file, true ).setListener( listener );
        transporter.get( task );
        assertThat(TestFileUtils.readString( file ) ).isEqualTo("resumable");
        assertThat(listener.startedCount ).isEqualTo(1L);
        assertThat(listener.dataOffset ).isEqualTo(2L);
        assertThat(listener.dataLength ).isEqualTo(9);
        assertThat(listener.progressedCount).as("Count: " + listener.progressedCount).isGreaterThan( 0 );
        assertThat(new String( listener.baos.toByteArray(), StandardCharsets.UTF_8)).isEqualTo( "sumable" );
    }

    @Test
    public void testGet_ResumeLocalContentsOutdated()
        throws Exception
    {
        File file = TestFileUtils.createTempFile( "re" );
        file.setLastModified( System.currentTimeMillis() - 5 * 60 * 1000 );
        RecordingTransportListener listener = new RecordingTransportListener();
        GetTask task = new GetTask( URI.create( "repo/resume.txt" ) ).setDataFile( file, true ).setListener( listener );
        transporter.get( task );
        assertThat(TestFileUtils.readString( file ) ).isEqualTo("resumable");
        assertThat(listener.startedCount ).isEqualTo(1L);
        assertThat(listener.dataOffset ).isEqualTo(0L);
        assertThat(listener.dataLength ).isEqualTo(9);
        assertThat(listener.progressedCount).as("Count: " + listener.progressedCount).isGreaterThan( 0 );
        assertThat(new String( listener.baos.toByteArray(), StandardCharsets.UTF_8)).isEqualTo( "resumable" );
    }

    @Test
    public void testGet_ResumeRangesNotSupportedByServer()
        throws Exception
    {
        httpServer.setRangeSupport( false );
        File file = TestFileUtils.createTempFile( "re" );
        RecordingTransportListener listener = new RecordingTransportListener();
        GetTask task = new GetTask( URI.create( "repo/resume.txt" ) ).setDataFile( file, true ).setListener( listener );
        transporter.get( task );
        assertThat(TestFileUtils.readString( file ) ).isEqualTo("resumable");
        assertThat(listener.startedCount ).isEqualTo(1L);
        assertThat(listener.dataOffset ).isEqualTo(0L);
        assertThat(listener.dataLength ).isEqualTo(9);
        assertThat(listener.progressedCount).as("Count: " + listener.progressedCount).isGreaterThan( 0 );
        assertThat(new String( listener.baos.toByteArray(), StandardCharsets.UTF_8)).isEqualTo( "resumable" );
    }

    @Test
    public void testGet_Checksums_Nexus()
        throws Exception
    {
        httpServer.setChecksumHeader( HttpServer.ChecksumHeader.NEXUS );
        GetTask task = new GetTask( URI.create( "repo/file.txt" ) );
        transporter.get( task );
        assertThat(task.getDataString() ).isEqualTo("test");
        assertThat(task.getChecksums().get( "SHA-1" ) ).isEqualTo("a94a8fe5ccb19ba61c4c0873d391e987982fbbd3");
    }

    @Test
    public void testGet_FileHandleLeak()
        throws Exception
    {
        for ( int i = 0; i < 100; i++ )
        {
            File file = TestFileUtils.createTempFile( "failure" );
            transporter.get( new GetTask( URI.create( "repo/file.txt" ) ).setDataFile( file ) );
            assertThat( file.delete() ).as(i + ", " + file.getAbsolutePath()).isTrue();
        }
    }

    @Test
    public void testGet_NotFound()
        throws Exception
    {
        try
        {
            transporter.get( new GetTask( URI.create( "repo/missing.txt" ) ) );
            fail( "Expected error" );
        }
        catch ( HttpResponseException e )
        {
            assertThat(e.getStatusCode() ).isEqualTo(404);
            assertThat(transporter.classify( e ) ).isEqualTo(Transporter.ERROR_NOT_FOUND);
        }
    }

    @Test
    public void testGet_Closed()
        throws Exception
    {
        transporter.close();
        try
        {
            transporter.get( new GetTask( URI.create( "repo/file.txt" ) ) );
            fail( "Expected error" );
        }
        catch ( IllegalStateException e )
        {
            assertThat(transporter.classify( e ) ).isEqualTo(Transporter.ERROR_OTHER);
        }
    }

    @Test
    public void testGet_StartCancelled()
        throws Exception
    {
        RecordingTransportListener listener = new RecordingTransportListener();
        listener.cancelStart = true;
        GetTask task = new GetTask( URI.create( "repo/file.txt" ) ).setListener( listener );
        try
        {
            transporter.get( task );
            fail( "Expected error" );
        }
        catch ( TransferCancelledException e )
        {
            assertThat(transporter.classify( e ) ).isEqualTo(Transporter.ERROR_OTHER);
        }
        assertThat(listener.dataOffset ).isEqualTo(0L);
        assertThat(listener.dataLength ).isEqualTo(4L);
        assertThat(listener.startedCount ).isEqualTo(1);
        assertThat(listener.progressedCount ).isEqualTo(0);
    }

    @Test
    public void testGet_ProgressCancelled()
        throws Exception
    {
        RecordingTransportListener listener = new RecordingTransportListener();
        listener.cancelProgress = true;
        GetTask task = new GetTask( URI.create( "repo/file.txt" ) ).setListener( listener );
        try
        {
            transporter.get( task );
            fail( "Expected error" );
        }
        catch ( TransferCancelledException e )
        {
            assertThat(transporter.classify( e ) ).isEqualTo(Transporter.ERROR_OTHER);
        }
        assertThat(listener.dataOffset ).isEqualTo(0L);
        assertThat(listener.dataLength ).isEqualTo(4L);
        assertThat(listener.startedCount ).isEqualTo(1);
        assertThat(listener.progressedCount ).isEqualTo(1);
    }

    @Test
    public void testPut_FromMemory()
        throws Exception
    {
        RecordingTransportListener listener = new RecordingTransportListener();
        PutTask task = new PutTask( URI.create( "repo/file.txt" ) ).setListener( listener ).setDataString( "upload" );
        transporter.put( task );
        assertThat(listener.dataOffset ).isEqualTo(0L);
        assertThat(listener.dataLength ).isEqualTo(6L);
        assertThat(listener.startedCount ).isEqualTo(1);
        assertThat(listener.progressedCount).as("Count: " + listener.progressedCount).isGreaterThan( 0 );
        assertThat( TestFileUtils.readString( new File( repoDir, "file.txt" ) ) ).isEqualTo("upload");
    }

    @Test
    public void testPut_FromFile()
        throws Exception
    {
        File file = TestFileUtils.createTempFile( "upload" );
        RecordingTransportListener listener = new RecordingTransportListener();
        PutTask task = new PutTask( URI.create( "repo/file.txt" ) ).setListener( listener ).setDataFile( file );
        transporter.put( task );
        assertThat(listener.dataOffset ).isEqualTo(0L);
        assertThat(listener.dataLength ).isEqualTo(6L);
        assertThat(listener.startedCount ).isEqualTo(1);
        assertThat(listener.progressedCount).as("Count: " + listener.progressedCount).isGreaterThan( 0 );
        assertThat( TestFileUtils.readString( new File( repoDir, "file.txt" ) ) ).isEqualTo("upload");
    }

    @Test
    public void testPut_EmptyResource()
        throws Exception
    {
        RecordingTransportListener listener = new RecordingTransportListener();
        PutTask task = new PutTask( URI.create( "repo/file.txt" ) ).setListener( listener );
        transporter.put( task );
        assertThat(listener.dataOffset ).isEqualTo(0L);
        assertThat(listener.dataLength ).isEqualTo(0L);
        assertThat(listener.startedCount ).isEqualTo(1);
        assertThat(listener.progressedCount).as("Count: " + listener.progressedCount).isGreaterThan( 0 );
        assertThat( TestFileUtils.readString( new File( repoDir, "file.txt" ) ) ).isEqualTo( "" );
    }

    @Test
    public void testPut_EncodedResourcePath()
        throws Exception
    {
        RecordingTransportListener listener = new RecordingTransportListener();
        PutTask task =
            new PutTask( URI.create( "repo/some%20space.txt" ) ).setListener( listener ).setDataString( "OK" );
        transporter.put( task );
        assertThat(listener.dataOffset ).isEqualTo(0L);
        assertThat(listener.dataLength ).isEqualTo(2L);
        assertThat(listener.startedCount ).isEqualTo(1);
        assertThat(listener.progressedCount).as("Count: " + listener.progressedCount).isGreaterThan( 0 );
        assertThat( TestFileUtils.readString( new File( repoDir, "some space.txt" ) ) ).isEqualTo( "OK" );
    }

    @Test
    public void testPut_Authenticated_ExpectContinue()
        throws Exception
    {
        httpServer.setAuthentication( "testuser", "testpass" );
        auth = new AuthenticationBuilder().addUsername( "testuser" ).addPassword( "testpass" ).build();
        newTransporter( httpServer.getHttpUrl() );
        RecordingTransportListener listener = new RecordingTransportListener();
        PutTask task = new PutTask( URI.create( "repo/file.txt" ) ).setListener( listener ).setDataString( "upload" );
        transporter.put( task );
        assertThat(listener.dataOffset ).isEqualTo(0L);
        assertThat(listener.dataLength ).isEqualTo(6L);
        assertThat(listener.startedCount ).isEqualTo(1);
        assertThat(listener.progressedCount).as("Count: " + listener.progressedCount).isGreaterThan( 0 );
        assertThat( TestFileUtils.readString( new File( repoDir, "file.txt" ) ) ).isEqualTo( "upload" );
    }

    @Test
    public void testPut_Authenticated_ExpectContinueBroken()
        throws Exception
    {
        httpServer.setAuthentication( "testuser", "testpass" );
        httpServer.setExpectSupport( HttpServer.ExpectContinue.BROKEN );
        auth = new AuthenticationBuilder().addUsername( "testuser" ).addPassword( "testpass" ).build();
        newTransporter( httpServer.getHttpUrl() );
        RecordingTransportListener listener = new RecordingTransportListener();
        PutTask task = new PutTask( URI.create( "repo/file.txt" ) ).setListener( listener ).setDataString( "upload" );
        transporter.put( task );
        assertThat(listener.dataOffset ).isEqualTo(0L);
        assertThat(listener.dataLength ).isEqualTo(6L);
        assertThat(listener.startedCount ).isEqualTo(1);
        assertThat(listener.progressedCount).as("Count: " + listener.progressedCount).isGreaterThan( 0 );
        assertThat( TestFileUtils.readString( new File( repoDir, "file.txt" ) ) ).isEqualTo( "upload" );
    }

    @Test
    public void testPut_Authenticated_ExpectContinueRejected()
        throws Exception
    {
        httpServer.setAuthentication( "testuser", "testpass" );
        httpServer.setExpectSupport( HttpServer.ExpectContinue.FAIL );
        auth = new AuthenticationBuilder().addUsername( "testuser" ).addPassword( "testpass" ).build();
        newTransporter( httpServer.getHttpUrl() );
        RecordingTransportListener listener = new RecordingTransportListener();
        PutTask task = new PutTask( URI.create( "repo/file.txt" ) ).setListener( listener ).setDataString( "upload" );
        transporter.put( task );
        assertThat(listener.dataOffset ).isEqualTo(0L);
        assertThat(listener.dataLength ).isEqualTo(6L);
        assertThat(listener.startedCount ).isEqualTo(1);
        assertThat(listener.progressedCount).as("Count: " + listener.progressedCount).isGreaterThan( 0 );
        assertThat( TestFileUtils.readString( new File( repoDir, "file.txt" ) ) ).isEqualTo( "upload" );
    }

    @Test
    public void testPut_Authenticated_ExpectContinueRejected_ExplicitlyConfiguredHeader()
        throws Exception
    {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put( "Expect", "100-continue" );
        session.setConfigProperty( ConfigurationProperties.HTTP_HEADERS + ".test", headers );
        httpServer.setAuthentication( "testuser", "testpass" );
        httpServer.setExpectSupport( HttpServer.ExpectContinue.FAIL );
        auth = new AuthenticationBuilder().addUsername( "testuser" ).addPassword( "testpass" ).build();
        newTransporter( httpServer.getHttpUrl() );
        RecordingTransportListener listener = new RecordingTransportListener();
        PutTask task = new PutTask( URI.create( "repo/file.txt" ) ).setListener( listener ).setDataString( "upload" );
        transporter.put( task );
        assertThat(listener.dataOffset ).isEqualTo(0L);
        assertThat(listener.dataLength ).isEqualTo(6L);
        assertThat(listener.startedCount ).isEqualTo(1);
        assertThat(listener.progressedCount).as("Count: " + listener.progressedCount).isGreaterThan( 0 );
        assertThat( TestFileUtils.readString( new File( repoDir, "file.txt" ) ) ).isEqualTo( "upload" );
    }

    @Test
    public void testPut_Unauthenticated()
        throws Exception
    {
        httpServer.setAuthentication( "testuser", "testpass" );
        RecordingTransportListener listener = new RecordingTransportListener();
        PutTask task = new PutTask( URI.create( "repo/file.txt" ) ).setListener( listener ).setDataString( "upload" );
        try
        {
            transporter.put( task );
            fail( "Expected error" );
        }
        catch ( HttpResponseException e )
        {
            assertThat(e.getStatusCode() ).isEqualTo(401);
            assertThat(transporter.classify( e ) ).isEqualTo(Transporter.ERROR_OTHER);
        }
        assertThat(listener.startedCount ).isEqualTo(0);
        assertThat(listener.progressedCount ).isEqualTo(0);
    }

    @Test
    public void testPut_ProxyAuthenticated()
        throws Exception
    {
        httpServer.setProxyAuthentication( "testuser", "testpass" );
        Authentication auth = new AuthenticationBuilder().addUsername( "testuser" ).addPassword( "testpass" ).build();
        proxy = new Proxy( Proxy.TYPE_HTTP, httpServer.getHost(), httpServer.getHttpPort(), auth );
        newTransporter( "http://bad.localhost:1/" );
        RecordingTransportListener listener = new RecordingTransportListener();
        PutTask task = new PutTask( URI.create( "repo/file.txt" ) ).setListener( listener ).setDataString( "upload" );
        transporter.put( task );
        assertThat(listener.dataOffset ).isEqualTo(0L);
        assertThat(listener.dataLength ).isEqualTo(6L);
        assertThat(listener.startedCount ).isEqualTo(1);
        assertThat(listener.progressedCount).as("Count: " + listener.progressedCount).isGreaterThan( 0 );
        assertThat( TestFileUtils.readString( new File( repoDir, "file.txt" ) ) ).isEqualTo( "upload" );
    }

    @Test
    public void testPut_ProxyUnauthenticated()
        throws Exception
    {
        httpServer.setProxyAuthentication( "testuser", "testpass" );
        proxy = new Proxy( Proxy.TYPE_HTTP, httpServer.getHost(), httpServer.getHttpPort() );
        newTransporter( "http://bad.localhost:1/" );
        RecordingTransportListener listener = new RecordingTransportListener();
        PutTask task = new PutTask( URI.create( "repo/file.txt" ) ).setListener( listener ).setDataString( "upload" );
        try
        {
            transporter.put( task );
            fail( "Expected error" );
        }
        catch ( HttpResponseException e )
        {
            assertThat(e.getStatusCode() ).isEqualTo(407);
            assertThat(transporter.classify( e ) ).isEqualTo(Transporter.ERROR_OTHER);
        }
        assertThat(listener.startedCount ).isEqualTo(0);
        assertThat(listener.progressedCount ).isEqualTo(0);
    }

    @Test
    public void testPut_SSL()
        throws Exception
    {
        httpServer.addSslConnector();
        httpServer.setAuthentication( "testuser", "testpass" );
        auth = new AuthenticationBuilder().addUsername( "testuser" ).addPassword( "testpass" ).build();
        newTransporter( httpServer.getHttpsUrl() );
        RecordingTransportListener listener = new RecordingTransportListener();
        PutTask task = new PutTask( URI.create( "repo/file.txt" ) ).setListener( listener ).setDataString( "upload" );
        transporter.put( task );
        assertThat(listener.dataOffset ).isEqualTo(0L);
        assertThat(listener.dataLength ).isEqualTo(6L);
        assertThat(listener.startedCount ).isEqualTo(1);
        assertThat(listener.progressedCount).as("Count: " + listener.progressedCount).isGreaterThan( 0 );
        assertThat( TestFileUtils.readString( new File( repoDir, "file.txt" ) ) ).isEqualTo( "upload" );
    }

    @Test
    public void testPut_WebDav()
        throws Exception
    {
        httpServer.setWebDav( true );
        RecordingTransportListener listener = new RecordingTransportListener();
        PutTask task =
            new PutTask( URI.create( "repo/dir1/dir2/file.txt" ) ).setListener( listener ).setDataString( "upload" );
        transporter.put( task );
        assertThat(listener.dataOffset ).isEqualTo(0L);
        assertThat(listener.dataLength ).isEqualTo(6L);
        assertThat(listener.startedCount ).isEqualTo(1);
        assertThat(listener.progressedCount).as("Count: " + listener.progressedCount).isGreaterThan( 0 );
        assertThat( TestFileUtils.readString( new File( repoDir, "dir1/dir2/file.txt" ) ) ).isEqualTo( "upload" );

        assertThat(httpServer.getLogEntries() ).hasSize( 5 );
        assertThat(httpServer.getLogEntries().get( 0 ).method ).isEqualTo("OPTIONS");
        assertThat(httpServer.getLogEntries().get( 1 ).method ).isEqualTo("MKCOL");
        assertThat(httpServer.getLogEntries().get( 1 ).path ).isEqualTo("/repo/dir1/dir2/");
        assertThat(httpServer.getLogEntries().get( 2 ).method ).isEqualTo("MKCOL");
        assertThat(httpServer.getLogEntries().get( 2 ).path ).isEqualTo("/repo/dir1/");
        assertThat(httpServer.getLogEntries().get( 3 ).method ).isEqualTo("MKCOL");
        assertThat(httpServer.getLogEntries().get( 3 ).path ).isEqualTo("/repo/dir1/dir2/");
        assertThat(httpServer.getLogEntries().get( 4 ).method ).isEqualTo("PUT");
    }

    @Test
    public void testPut_FileHandleLeak()
        throws Exception
    {
        for ( int i = 0; i < 100; i++ )
        {
            File src = TestFileUtils.createTempFile( "upload" );
            File dst = new File( repoDir, "file.txt" );
            transporter.put( new PutTask( URI.create( "repo/file.txt" ) ).setDataFile( src ) );
            assertThat(src.delete() ).as( i + ", " + src.getAbsolutePath() ).isTrue();
            assertThat(dst.delete() ).as( i + ", " + dst.getAbsolutePath() ).isTrue();
        }
    }

    @Test
    public void testPut_Closed()
        throws Exception
    {
        transporter.close();
        try
        {
            transporter.put( new PutTask( URI.create( "repo/missing.txt" ) ) );
            fail( "Expected error" );
        }
        catch ( IllegalStateException e )
        {
            assertThat(transporter.classify( e ) ).isEqualTo( Transporter.ERROR_OTHER );
        }
    }

    @Test
    public void testPut_StartCancelled()
        throws Exception
    {
        RecordingTransportListener listener = new RecordingTransportListener();
        listener.cancelStart = true;
        PutTask task = new PutTask( URI.create( "repo/file.txt" ) ).setListener( listener ).setDataString( "upload" );
        try
        {
            transporter.put( task );
            fail( "Expected error" );
        }
        catch ( TransferCancelledException e )
        {
            assertThat(transporter.classify( e ) ).isEqualTo(Transporter.ERROR_OTHER);
        }
        assertThat(listener.dataOffset ).isEqualTo(0L);
        assertThat(listener.dataLength ).isEqualTo(6L);
        assertThat(listener.startedCount ).isEqualTo(1);
        assertThat(listener.progressedCount ).isEqualTo(0);
    }

    @Test
    public void testPut_ProgressCancelled()
        throws Exception
    {
        RecordingTransportListener listener = new RecordingTransportListener();
        listener.cancelProgress = true;
        PutTask task = new PutTask( URI.create( "repo/file.txt" ) ).setListener( listener ).setDataString( "upload" );
        try
        {
            transporter.put( task );
            fail( "Expected error" );
        }
        catch ( TransferCancelledException e )
        {
            assertThat(transporter.classify( e ) ).isEqualTo(Transporter.ERROR_OTHER);
        }
        assertThat(listener.dataOffset ).isEqualTo(0L);
        assertThat(listener.dataLength ).isEqualTo(6L);
        assertThat(listener.startedCount ).isEqualTo(1);
        assertThat(listener.progressedCount ).isEqualTo(1);
    }

    @Test
    public void testGetPut_AuthCache()
        throws Exception
    {
        httpServer.setAuthentication( "testuser", "testpass" );
        auth = new AuthenticationBuilder().addUsername( "testuser" ).addPassword( "testpass" ).build();
        newTransporter( httpServer.getHttpUrl() );
        GetTask get = new GetTask( URI.create( "repo/file.txt" ) );
        transporter.get( get );
        RecordingTransportListener listener = new RecordingTransportListener();
        PutTask task = new PutTask( URI.create( "repo/file.txt" ) ).setListener( listener ).setDataString( "upload" );
        transporter.put( task );
        assertThat(listener.startedCount ).isEqualTo(1);
    }

    @Test( timeout = 10000L )
    public void testConcurrency()
        throws Exception
    {
        httpServer.setAuthentication( "testuser", "testpass" );
        auth = new AuthenticationBuilder().addUsername( "testuser" ).addPassword( "testpass" ).build();
        newTransporter( httpServer.getHttpUrl() );
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        Thread threads[] = new Thread[20];
        for ( int i = 0; i < threads.length; i++ )
        {
            final String path = "repo/file.txt?i=" + i;
            threads[i] = new Thread()
            {
                @Override
                public void run()
                {
                    try
                    {
                        for ( int j = 0; j < 100; j++ )
                        {
                            GetTask task = new GetTask( URI.create( path ) );
                            transporter.get( task );
                            assertThat( task.getDataString() ).isEqualTo( "test" );
                        }
                    }
                    catch ( Throwable t )
                    {
                        error.compareAndSet( null, t );
                        System.err.println( path );
                        t.printStackTrace();
                    }
                }
            };
            threads[i].setName( "Task-" + i );
        }
        for ( Thread thread : threads )
        {
            thread.start();
        }
        for ( Thread thread : threads )
        {
            thread.join();
        }
        assertThat( error.get() ).isNull();
    }

    @Test( timeout = 1000L )
    public void testConnectTimeout()
        throws Exception
    {
        session.setConfigProperty( ConfigurationProperties.CONNECT_TIMEOUT, 100 );
        int port = 1;
        newTransporter( "http://localhost:" + port );
        try
        {
            transporter.get( new GetTask( URI.create( "repo/file.txt" ) ) );
            fail( "Expected error" );
        }
        catch ( ConnectTimeoutException | ConnectException e )
        {
            assertThat(transporter.classify( e ) ).isEqualTo(Transporter.ERROR_OTHER);
        }
    }

    @Test( timeout = 1000L )
    public void testRequestTimeout()
        throws Exception
    {
        session.setConfigProperty( ConfigurationProperties.REQUEST_TIMEOUT, 100 );
        ServerSocket server = new ServerSocket( 0 );
        newTransporter( "http://localhost:" + server.getLocalPort() );
        try
        {
            try
            {
                transporter.get( new GetTask( URI.create( "repo/file.txt" ) ) );
                fail( "Expected error" );
            }
            catch ( SocketTimeoutException e )
            {
                assertThat(transporter.classify( e ) ).isEqualTo(Transporter.ERROR_OTHER);
            }
        }
        finally
        {
            server.close();
        }
    }

    @Test
    public void testUserAgent()
        throws Exception
    {
        session.setConfigProperty( ConfigurationProperties.USER_AGENT, "SomeTest/1.0" );
        newTransporter( httpServer.getHttpUrl() );
        transporter.get( new GetTask( URI.create( "repo/file.txt" ) ) );
        assertThat(httpServer.getLogEntries().size() ).isEqualTo(1);
        for ( HttpServer.LogEntry log : httpServer.getLogEntries() )
        {
            assertThat(log.headers.get( "User-Agent" ) ).isEqualTo("SomeTest/1.0");
        }
    }

    @Test
    public void testCustomHeaders()
        throws Exception
    {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put( "User-Agent", "Custom/1.0" );
        headers.put( "X-CustomHeader", "Custom-Value" );
        session.setConfigProperty( ConfigurationProperties.USER_AGENT, "SomeTest/1.0" );
        session.setConfigProperty( ConfigurationProperties.HTTP_HEADERS + ".test", headers );
        newTransporter( httpServer.getHttpUrl() );
        transporter.get( new GetTask( URI.create( "repo/file.txt" ) ) );
        assertThat(httpServer.getLogEntries().size() ).isEqualTo(1);
        for ( HttpServer.LogEntry log : httpServer.getLogEntries() )
        {
            for ( Map.Entry<String, String> entry : headers.entrySet() )
            {
                assertThat( log.headers.get( entry.getKey() ) ).as( entry.getValue()).isEqualTo(entry.getKey());
            }
        }
    }

    @Test
    public void testServerAuthScope_NotUsedForProxy()
        throws Exception
    {
        String username = "testuser", password = "testpass";
        httpServer.setProxyAuthentication( username, password );
        auth = new AuthenticationBuilder().addUsername( username ).addPassword( password ).build();
        proxy = new Proxy( Proxy.TYPE_HTTP, httpServer.getHost(), httpServer.getHttpPort() );
        newTransporter( "http://" + httpServer.getHost() + ":12/" );
        try
        {
            transporter.get( new GetTask( URI.create( "repo/file.txt" ) ) );
            fail( "Server auth must not be used as proxy auth" );
        }
        catch ( HttpResponseException e )
        {
            assertThat(e.getStatusCode() ).isEqualTo(407);
        }
    }

    @Test
    public void testProxyAuthScope_NotUsedForServer()
        throws Exception
    {
        String username = "testuser", password = "testpass";
        httpServer.setAuthentication( username, password );
        Authentication auth = new AuthenticationBuilder().addUsername( username ).addPassword( password ).build();
        proxy = new Proxy( Proxy.TYPE_HTTP, httpServer.getHost(), httpServer.getHttpPort(), auth );
        newTransporter( "http://" + httpServer.getHost() + ":12/" );
        try
        {
            transporter.get( new GetTask( URI.create( "repo/file.txt" ) ) );
            fail( "Proxy auth must not be used as server auth" );
        }
        catch ( HttpResponseException e )
        {
            assertThat(e.getStatusCode() ).isEqualTo(401);
        }
    }

    @Test
    public void testAuthSchemeReuse()
        throws Exception
    {
        httpServer.setAuthentication( "testuser", "testpass" );
        httpServer.setProxyAuthentication( "proxyuser", "proxypass" );
        session.setCache( new DefaultRepositoryCache() );
        auth = new AuthenticationBuilder().addUsername( "testuser" ).addPassword( "testpass" ).build();
        Authentication auth = new AuthenticationBuilder().addUsername( "proxyuser" ).addPassword( "proxypass" ).build();
        proxy = new Proxy( Proxy.TYPE_HTTP, httpServer.getHost(), httpServer.getHttpPort(), auth );
        newTransporter( "http://bad.localhost:1/" );
        GetTask task = new GetTask( URI.create( "repo/file.txt" ) );
        transporter.get( task );
        assertThat(task.getDataString() ).isEqualTo("test");
        assertThat(httpServer.getLogEntries().size() ).isEqualTo(3);
        httpServer.getLogEntries().clear();
        newTransporter( "http://bad.localhost:1/" );
        task = new GetTask( URI.create( "repo/file.txt" ) );
        transporter.get( task );
        assertThat(task.getDataString() ).isEqualTo("test");
        assertThat(httpServer.getLogEntries().size() ).isEqualTo(1);
        assertThat(httpServer.getLogEntries().get( 0 ).headers.get( "Authorization" ) ).isNotNull();
        assertThat(httpServer.getLogEntries().get( 0 ).headers.get( "Proxy-Authorization" ) ).isNotNull();
    }

    @Test
    public void testConnectionReuse()
        throws Exception
    {
        httpServer.addSslConnector();
        session.setCache( new DefaultRepositoryCache() );
        for ( int i = 0; i < 3; i++ )
        {
            newTransporter( httpServer.getHttpsUrl() );
            GetTask task = new GetTask( URI.create( "repo/file.txt" ) );
            transporter.get( task );
            assertThat( task.getDataString() ).isEqualTo("test");
        }
        PoolStats stats =
            ( (ConnPoolControl<?>) ( (HttpTransporter) transporter ).getState().getConnectionManager() ).getTotalStats();
        assertThat( stats.getAvailable() ).as(stats.toString()).isEqualTo( 1 );
    }

    @Test( expected = NoTransporterException.class )
    public void testInit_BadProtocol()
        throws Exception
    {
        newTransporter( "bad:/void" );
    }

    @Test( expected = NoTransporterException.class )
    public void testInit_BadUrl()
        throws Exception
    {
        newTransporter( "http://localhost:NaN" );
    }

    @Test
    public void testInit_CaseInsensitiveProtocol()
        throws Exception
    {
        newTransporter( "http://localhost" );
        newTransporter( "HTTP://localhost" );
        newTransporter( "Http://localhost" );
        newTransporter( "https://localhost" );
        newTransporter( "HTTPS://localhost" );
        newTransporter( "HttpS://localhost" );
    }

}
