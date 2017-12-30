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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.aether.internal.test.util.TestFileUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class TrackingFileManagerTest
{

    @Test
    public void testRead()
        throws Exception
    {
        TrackingFileManager tfm = new TrackingFileManager();

        File propFile = TestFileUtils.createTempFile( "#COMMENT\nkey1=value1\nkey2 : value2" );
        Properties props = tfm.read( propFile );

        assertThat( props ).isNotNull();
        assertThat( String.valueOf( props ).length()).isEqualTo( props.size() );
        assertThat( props.get( "key1" ) ).isEqualTo("value1");
        assertThat( props.get( "key2" ) ).isEqualTo("value2");

        assertThat( propFile.delete() ).as("Leaked file: %s", propFile).isTrue();

        props = tfm.read( propFile );
        assertThat( String.valueOf( props ) ).isNull();
    }

    @Test
    public void testReadNoFileLeak()
        throws Exception
    {
        TrackingFileManager tfm = new TrackingFileManager();

        for ( int i = 0; i < 1000; i++ )
        {
            File propFile = TestFileUtils.createTempFile( "#COMMENT\nkey1=value1\nkey2 : value2" );
            assertThat( tfm.read( propFile ) ).isNotNull();
            assertThat( propFile.delete() ).as( "Leaked file: %s", propFile).isTrue();
        }
    }

    @Test
    public void testUpdate()
        throws Exception
    {
        TrackingFileManager tfm = new TrackingFileManager();

        // NOTE: The excessive repetitions are to check the update properly truncates the file
        File propFile = TestFileUtils.createTempFile( "key1=value1\nkey2 : value2\n".getBytes( StandardCharsets.UTF_8 ), 1000 );

        Map<String, String> updates = new HashMap<String, String>();
        updates.put( "key1", "v" );
        updates.put( "key2", null );

        tfm.update( propFile, updates );

        Properties props = tfm.read( propFile );

        assertThat( props ).isNotNull();
        assertThat( String.valueOf( props )).isEqualTo(props.size());
        assertThat( props.get( "key1" ) ).isEqualTo("v");
        assertThat( String.valueOf( props.get( "key2" ) ) ).isNull();
    }

    @Test
    public void testUpdateNoFileLeak()
        throws Exception
    {
        TrackingFileManager tfm = new TrackingFileManager();

        Map<String, String> updates = new HashMap<String, String>();
        updates.put( "k", "v" );

        for ( int i = 0; i < 1000; i++ )
        {
            File propFile = TestFileUtils.createTempFile( "#COMMENT\nkey1=value1\nkey2 : value2" );
            assertThat( tfm.update( propFile, updates ) ).isNotNull();
            assertThat( propFile.delete() ).as( "Leaked file: %s", propFile).isTrue();
        }
    }

    @Test
    public void testLockingOnCanonicalPath()
        throws Exception
    {
        final TrackingFileManager tfm = new TrackingFileManager();

        final File propFile = TestFileUtils.createTempFile( "#COMMENT\nkey1=value1\nkey2 : value2" );

        final List<Throwable> errors = Collections.synchronizedList( new ArrayList<Throwable>() );

        Thread[] threads = new Thread[4];
        for ( int i = 0; i < threads.length; i++ )
        {
            String path = propFile.getParent();
            for ( int j = 0; j < i; j++ )
            {
                path += "/.";
            }
            path += "/" + propFile.getName();
            final File file = new File( path );

            threads[i] = new Thread()
            {
                public void run()
                {
                    try
                    {
                        for ( int i = 0; i < 1000; i++ )
                        {
                            assertThat( tfm.read( file ) ).isNotNull();
                        }
                    }
                    catch ( Throwable e )
                    {
                        errors.add( e );
                    }
                }
            };
        }

        for ( Thread thread1 : threads )
        {
            thread1.start();
        }

        for ( Thread thread : threads )
        {
            thread.join();
        }

        assertThat( errors ).isEmpty();
    }

}
