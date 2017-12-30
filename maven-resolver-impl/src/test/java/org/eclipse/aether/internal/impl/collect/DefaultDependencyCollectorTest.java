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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.ArtifactProperties;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.collection.DependencyManagement;
import org.eclipse.aether.collection.DependencyManager;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyCycle;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.eclipse.aether.internal.impl.IniArtifactDescriptorReader;
import org.eclipse.aether.internal.impl.StubRemoteRepositoryManager;
import org.eclipse.aether.internal.impl.StubVersionRangeResolver;
import org.eclipse.aether.internal.test.util.DependencyGraphParser;
import org.eclipse.aether.internal.test.util.TestUtils;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.eclipse.aether.util.graph.manager.ClassicDependencyManager;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.version.HighestVersionFilter;
import org.junit.Before;
import org.junit.Test;

/**
 */
public class DefaultDependencyCollectorTest
{

    private DefaultDependencyCollector collector;

    private DefaultRepositorySystemSession session;

    private DependencyGraphParser parser;

    private RemoteRepository repository;

    private IniArtifactDescriptorReader newReader( String prefix )
    {
        return new IniArtifactDescriptorReader( "artifact-descriptions/" + prefix );
    }

    private Dependency newDep( String coords )
    {
        return newDep( coords, "" );
    }

    private Dependency newDep( String coords, String scope )
    {
        return new Dependency( new DefaultArtifact( coords ), scope );
    }

    @Before
    public void setup()
        throws IOException
    {
        session = TestUtils.newSession();

        collector = new DefaultDependencyCollector();
        collector.setArtifactDescriptorReader( newReader( "" ) );
        collector.setVersionRangeResolver( new StubVersionRangeResolver() );
        collector.setRemoteRepositoryManager( new StubRemoteRepositoryManager() );

        parser = new DependencyGraphParser( "artifact-descriptions/" );

        repository = new RemoteRepository.Builder( "id", "default", "file:///" ).build();
    }

    private static void assertEqualSubtree( DependencyNode expected, DependencyNode actual )
    {
        assertEqualSubtree( expected, actual, new LinkedList<DependencyNode>() );
    }

    private static void assertEqualSubtree( DependencyNode expected, DependencyNode actual,
                                            LinkedList<DependencyNode> parents )
    {
        assertThat( actual.getDependency() ).isEqualTo( expected.getDependency());

        if ( actual.getDependency() != null )
        {
            Artifact artifact = actual.getDependency().getArtifact();
            for ( DependencyNode parent : parents )
            {
                if ( parent.getDependency() != null && artifact.equals( parent.getDependency().getArtifact() ) )
                {
                    return;
                }
            }
        }

        parents.addLast( expected );

        assertThat(expected.getChildren().size() )
                .as( "path: " + parents + ", expected: " + expected.getChildren() + ", actual: "
                        + actual.getChildren())
                .isEqualTo( actual.getChildren().size() );

        Iterator<DependencyNode> iterator1 = expected.getChildren().iterator();
        Iterator<DependencyNode> iterator2 = actual.getChildren().iterator();

        while ( iterator1.hasNext() )
        {
            assertEqualSubtree( iterator1.next(), iterator2.next(), parents );
        }

        parents.removeLast();
    }

    private Dependency dep( DependencyNode root, int... coords )
    {
        return path( root, coords ).getDependency();
    }

    private DependencyNode path( DependencyNode root, int... coords )
    {
        try
        {
            DependencyNode node = root;
            for ( int coord : coords )
            {
                node = node.getChildren().get( coord );
            }

            return node;
        }
        catch ( IndexOutOfBoundsException | NullPointerException e )
        {
            throw new IllegalArgumentException( "illegal coordinates for child", e );
        }
    }

    @Test
    public void testSimpleCollection()
        throws DependencyCollectionException
    {
        Dependency dependency = newDep( "gid:aid:ext:ver", "compile" );
        CollectRequest request = new CollectRequest( dependency, Arrays.asList( repository ) );
        CollectResult result = collector.collectDependencies( session, request );

        assertThat(result.getExceptions().size() ).isEqualTo(0);

        DependencyNode root = result.getRoot();
        Dependency newDependency = root.getDependency();

        assertThat(newDependency ).isEqualTo(dependency);
        assertThat(newDependency.getArtifact() ).isEqualTo(dependency.getArtifact());

        assertThat(root.getChildren().size() ).isEqualTo(1);

        Dependency expect = newDep( "gid:aid2:ext:ver", "compile" );
        assertThat(root.getChildren().get( 0 ).getDependency() ).isEqualTo(expect);
    }

    @Test
    public void testMissingDependencyDescription()
    {
        CollectRequest request =
            new CollectRequest( newDep( "missing:description:ext:ver" ), Arrays.asList( repository ) );
        try
        {
            collector.collectDependencies( session, request );
            fail( "expected exception" );
        }
        catch ( DependencyCollectionException e )
        {
            CollectResult result = e.getResult();
            assertThat(result.getRequest() ).isSameAs(request);
            assertThat(result.getExceptions() ).isNotNull();
            assertThat(result.getExceptions().size() ).isEqualTo(1);

            assertThat(result.getExceptions().get( 0 ) instanceof ArtifactDescriptorException ).isTrue();

            assertThat(result.getRoot().getDependency() ).isEqualTo(request.getRoot());
        }
    }

    @Test
    public void testDuplicates()
        throws IOException, DependencyCollectionException
    {
        Dependency dependency = newDep( "duplicate:transitive:ext:dependency" );
        CollectRequest request = new CollectRequest( dependency, Arrays.asList( repository ) );

        CollectResult result = collector.collectDependencies( session, request );

        assertThat(result.getExceptions().size() ).isEqualTo(0);

        DependencyNode root = result.getRoot();
        Dependency newDependency = root.getDependency();

        assertThat(newDependency ).isEqualTo(dependency);
        assertThat(newDependency.getArtifact() ).isEqualTo(dependency.getArtifact());

        assertThat(root.getChildren().size() ).isEqualTo(2);

        Dependency dep = newDep( "gid:aid:ext:ver", "compile" );
        assertThat(dep( root)).isEqualTo( dep );

        dep = newDep( "gid:aid2:ext:ver", "compile" );
        assertThat(dep( root, 1 ) ).isEqualTo( dep );
        assertThat(dep( root, 0, 0 ) ).isEqualTo( dep );
        assertThat(dep( root, 0, 0 )).isEqualTo( dep );
    }

    @Test
    public void testEqualSubtree()
        throws IOException, DependencyCollectionException
    {
        DependencyNode root = parser.parseResource( "expectedSubtreeComparisonResult.txt" );
        Dependency dependency = root.getDependency();
        CollectRequest request = new CollectRequest( dependency, Arrays.asList( repository ) );

        CollectResult result = collector.collectDependencies( session, request );
        assertEqualSubtree( root, result.getRoot() );
    }

    @Test
    public void testCyclicDependencies()
        throws Exception
    {
        DependencyNode root = parser.parseResource( "cycle.txt" );
        CollectRequest request = new CollectRequest( root.getDependency(), Arrays.asList( repository ) );
        CollectResult result = collector.collectDependencies( session, request );
        assertEqualSubtree( root, result.getRoot() );
    }

    @Test
    public void testCyclicDependenciesBig()
        throws Exception
    {
        CollectRequest request = new CollectRequest( newDep( "1:2:pom:5.50-SNAPSHOT" ), Arrays.asList( repository ) );
        collector.setArtifactDescriptorReader( newReader( "cycle-big/" ) );
        CollectResult result = collector.collectDependencies( session, request );
        assertThat(result.getRoot() ).isNotNull();
        // we only care about the performance here, this test must not hang or run out of mem
    }

    @Test
    public void testCyclicProjects()
        throws Exception
    {
        CollectRequest request = new CollectRequest( newDep( "test:a:2" ), Arrays.asList( repository ) );
        collector.setArtifactDescriptorReader( newReader( "versionless-cycle/" ) );
        CollectResult result = collector.collectDependencies( session, request );
        DependencyNode root = result.getRoot();
        DependencyNode a1 = path( root, 0, 0 );
        assertThat(a1.getArtifact().getArtifactId() ).isEqualTo("a");
        assertThat(a1.getArtifact().getVersion() ).isEqualTo("1");
        for ( DependencyNode child : a1.getChildren() )
        {
            assertThat( child.getArtifact().getVersion() ).isNotEqualTo( "1" );
        }

        assertThat(result.getCycles().size() ).isEqualTo(1);
        DependencyCycle cycle = result.getCycles().get( 0 );
        assertThat(cycle.getPrecedingDependencies() ).isEmpty();
        assertThat( Arrays.asList( root.getDependency(), path( root, 0 ).getDependency()))
                .isEqualTo( cycle.getCyclicDependencies() );
    }

    @Test
    public void testCyclicProjects_ConsiderLabelOfRootlessGraph()
        throws Exception
    {
        Dependency dep = newDep( "gid:aid:ver", "compile" );
        CollectRequest request =
            new CollectRequest().addDependency( dep ).addRepository( repository ).setRootArtifact( dep.getArtifact() );
        CollectResult result = collector.collectDependencies( session, request );
        DependencyNode root = result.getRoot();
        DependencyNode a1 = root.getChildren().get( 0 );
        assertThat(a1.getArtifact().getArtifactId() ).isEqualTo("aid");
        assertThat(a1.getArtifact().getVersion() ).isEqualTo("ver");
        DependencyNode a2 = a1.getChildren().get( 0 );
        assertThat(a2.getArtifact().getArtifactId() ).isEqualTo("aid2");
        assertThat(a2.getArtifact().getVersion() ).isEqualTo("ver");

        assertThat(result.getCycles().size() ).isEqualTo(1);
        DependencyCycle cycle = result.getCycles().get( 0 );
        assertThat( cycle.getPrecedingDependencies() ).isEmpty();
        assertThat( Arrays.asList( new Dependency( dep.getArtifact(), null ), a1.getDependency()))
                .isEqualTo( cycle.getCyclicDependencies() );
    }

    @Test
    public void testPartialResultOnError()
        throws IOException
    {
        DependencyNode root = parser.parseResource( "expectedPartialSubtreeOnError.txt" );

        Dependency dependency = root.getDependency();
        CollectRequest request = new CollectRequest( dependency, Arrays.asList( repository ) );

        CollectResult result;
        try
        {
            result = collector.collectDependencies( session, request );
            fail( "expected exception " );
        }
        catch ( DependencyCollectionException e )
        {
            result = e.getResult();

            assertThat(result.getRequest() ).isSameAs(request);
            assertThat(result.getExceptions() ).isNotNull();
            assertThat(result.getExceptions().size() ).isEqualTo(1);

            assertThat(result.getExceptions().get( 0 ) instanceof ArtifactDescriptorException ).isTrue();

            assertEqualSubtree( root, result.getRoot() );
        }
    }

    @Test
    public void testCollectMultipleDependencies()
        throws DependencyCollectionException
    {
        Dependency root1 = newDep( "gid:aid:ext:ver", "compile" );
        Dependency root2 = newDep( "gid:aid2:ext:ver", "compile" );
        List<Dependency> dependencies = Arrays.asList( root1, root2 );
        CollectRequest request = new CollectRequest( dependencies, null, Arrays.asList( repository ) );
        CollectResult result = collector.collectDependencies( session, request );

        assertThat( result.getExceptions() ).hasSize( 0 );
        assertThat( result.getRoot().getChildren() ).hasSize( 2 );
        assertThat( dep( result.getRoot(), 0 )).isEqualTo( root1 );

        assertThat( path( result.getRoot(), 0 ).getChildren()).hasSize( 1 );
        assertThat( dep( result.getRoot(), 0, 0 )).isEqualTo( root2 );

        assertThat( path( result.getRoot(), 1 ).getChildren() ).hasSize( 0 );
        assertThat( dep( result.getRoot(), 1 )).isEqualTo( root2 );
    }

    @Test
    public void testArtifactDescriptorResolutionNotRestrictedToRepoHostingSelectedVersion()
        throws Exception
    {
        RemoteRepository repo2 = new RemoteRepository.Builder( "test", "default", "file:///" ).build();

        final List<RemoteRepository> repos = new ArrayList<RemoteRepository>();

        collector.setArtifactDescriptorReader( new ArtifactDescriptorReader()
        {
            public ArtifactDescriptorResult readArtifactDescriptor( RepositorySystemSession session,
                                                                    ArtifactDescriptorRequest request )
                throws ArtifactDescriptorException
            {
                repos.addAll( request.getRepositories() );
                return new ArtifactDescriptorResult( request );
            }
        } );

        List<Dependency> dependencies = Arrays.asList( newDep( "verrange:parent:jar:1[1,)", "compile" ) );
        CollectRequest request = new CollectRequest( dependencies, null, Arrays.asList( repository, repo2 ) );
        CollectResult result = collector.collectDependencies( session, request );

        assertThat( result.getExceptions().size() ).isEqualTo( 0 );
        assertThat( repos.size() ).isEqualTo(2);
        assertThat( repos.get( 0 ).getId() ).isEqualTo( "id" );
        assertThat( repos.get( 1 ).getId() ).isEqualTo( "test" );
    }

    @Test
    public void testManagedVersionScope()
        throws DependencyCollectionException
    {
        Dependency dependency = newDep( "managed:aid:ext:ver" );
        CollectRequest request = new CollectRequest( dependency, Arrays.asList( repository ) );

        session.setDependencyManager( new ClassicDependencyManager() );

        CollectResult result = collector.collectDependencies( session, request );

        assertThat(result.getExceptions().size() ).isEqualTo(0);

        DependencyNode root = result.getRoot();

        assertThat(dep( root ) ).isEqualTo(dependency);
        assertThat(dep( root ).getArtifact() ).isEqualTo(dependency.getArtifact());

        assertThat(root.getChildren().size() ).isEqualTo(1);
        Dependency expect = newDep( "gid:aid:ext:ver", "compile" );
        assertThat( dep( root) ).isEqualTo( expect );

        assertThat( path( root).getChildren() ).hasSize( 1 );
        expect = newDep( "gid:aid2:ext:managedVersion", "managedScope" );
        assertThat( dep( root, 0 , 0) ).isEqualTo( expect );
    }

    @Test
    public void testDependencyManagement()
        throws IOException, DependencyCollectionException
    {
        collector.setArtifactDescriptorReader( newReader( "managed/" ) );

        DependencyNode root = parser.parseResource( "expectedSubtreeComparisonResult.txt" );
        TestDependencyManager depMgmt = new TestDependencyManager();
        depMgmt.add( dep( root, 0 ), "managed", null, null );
        depMgmt.add( dep( root, 0, 1 ), "managed", "managed", null );
        depMgmt.add( dep( root, 1 ), null, null, "managed" );
        session.setDependencyManager( depMgmt );

        // collect result will differ from expectedSubtreeComparisonResult.txt
        // set localPath -> no dependency traversal
        CollectRequest request = new CollectRequest( dep( root ), Arrays.asList( repository ) );
        CollectResult result = collector.collectDependencies( session, request );

        DependencyNode node = result.getRoot();
        assertThat( dep( node, 0, 1 ).getArtifact().getVersion() ).isEqualTo("managed");
        assertThat( dep( node, 0, 1 ).getScope() ).isEqualTo("managed");

        assertThat( dep( node, 1 ).getArtifact().getProperty( ArtifactProperties.LOCAL_PATH, null ) ).isEqualTo("managed" );
        assertThat( dep( node, 0, 0 ).getArtifact().getProperty( ArtifactProperties.LOCAL_PATH, null ) ).isEqualTo("managed" );
    }

    @Test
    public void testDependencyManagement_VerboseMode()
        throws Exception
    {
        String depId = "gid:aid2:ext";
        TestDependencyManager depMgmt = new TestDependencyManager();
        depMgmt.version( depId, "managedVersion" );
        depMgmt.scope( depId, "managedScope" );
        depMgmt.optional( depId, Boolean.TRUE );
        depMgmt.path( depId, "managedPath" );
        depMgmt.exclusions( depId, new Exclusion( "gid", "aid", "*", "*" ) );
        session.setDependencyManager( depMgmt );
        session.setConfigProperty( DependencyManagerUtils.CONFIG_PROP_VERBOSE, Boolean.TRUE );

        CollectRequest request = new CollectRequest().setRoot( newDep( "gid:aid:ver" ) );
        CollectResult result = collector.collectDependencies( session, request );
        DependencyNode node = result.getRoot().getChildren().get( 0 );
        assertThat( node.getManagedBits() ).isEqualTo( DependencyNode.MANAGED_VERSION | DependencyNode.MANAGED_SCOPE | DependencyNode.MANAGED_OPTIONAL
                | DependencyNode.MANAGED_PROPERTIES | DependencyNode.MANAGED_EXCLUSIONS );
        assertThat(DependencyManagerUtils.getPremanagedVersion( node ) ).isEqualTo( "ver" );
        assertThat(DependencyManagerUtils.getPremanagedScope( node ) ).isEqualTo( "compile" );
        assertThat(DependencyManagerUtils.getPremanagedOptional( node ) ).isFalse();
    }

    @Test
    public void testVersionFilter()
        throws Exception
    {
        session.setVersionFilter( new HighestVersionFilter() );
        CollectRequest request = new CollectRequest().setRoot( newDep( "gid:aid:1" ) );
        CollectResult result = collector.collectDependencies( session, request );
        assertThat( result.getRoot().getChildren() ).hasSize( 1 );
    }

    static class TestDependencyManager
        implements DependencyManager
    {

        private Map<String, String> versions = new HashMap<>();

        private Map<String, String> scopes = new HashMap<>();

        private Map<String, Boolean> optionals = new HashMap<>();

        private Map<String, String> paths = new HashMap<>();

        private Map<String, Collection<Exclusion>> exclusions = new HashMap<>();

        public void add( Dependency d, String version, String scope, String localPath )
        {
            String id = toKey( d );
            version( id, version );
            scope( id, scope );
            path( id, localPath );
        }

        public void version( String id, String version )
        {
            versions.put( id, version );
        }

        public void scope( String id, String scope )
        {
            scopes.put( id, scope );
        }

        public void optional( String id, Boolean optional )
        {
            optionals.put( id, optional );
        }

        public void path( String id, String path )
        {
            paths.put( id, path );
        }

        public void exclusions( String id, Exclusion... exclusions )
        {
            this.exclusions.put( id, exclusions != null ? Arrays.asList( exclusions ) : null );
        }

        public DependencyManagement manageDependency( Dependency d )
        {
            String id = toKey( d );
            DependencyManagement mgmt = new DependencyManagement();
            mgmt.setVersion( versions.get( id ) );
            mgmt.setScope( scopes.get( id ) );
            mgmt.setOptional( optionals.get( id ) );
            String path = paths.get( id );
            if ( path != null )
            {
                mgmt.setProperties( Collections.singletonMap( ArtifactProperties.LOCAL_PATH, path ) );
            }
            mgmt.setExclusions( exclusions.get( id ) );
            return mgmt;
        }

        private String toKey( Dependency dependency )
        {
            return ArtifactIdUtils.toVersionlessId( dependency.getArtifact() );
        }

        public DependencyManager deriveChildManager( DependencyCollectionContext context )
        {
            return this;
        }

    }

}
