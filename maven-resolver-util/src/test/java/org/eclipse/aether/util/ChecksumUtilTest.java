package org.eclipse.aether.util;

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

import static org.eclipse.aether.internal.test.util.TestFileUtils.*;
import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ChecksumUtilTest
{
    private File emptyFile;

    private File patternFile;

    private File textFile;

    private static Map<String, String> emptyFileChecksums = new HashMap<>();

    private static Map<String, String> patternFileChecksums = new HashMap<>();

    private static Map<String, String> textFileChecksums = new HashMap<>();

    private Map<File, Map<String, String>> sums = new HashMap<>();

    @BeforeClass
    public static void beforeClass()
    {
        emptyFileChecksums.put( "MD5", "d41d8cd98f00b204e9800998ecf8427e" );
        emptyFileChecksums.put( "SHA-1", "da39a3ee5e6b4b0d3255bfef95601890afd80709" );
        patternFileChecksums.put( "MD5", "14f01d6c7de7d4cf0a4887baa3528b5a" );
        patternFileChecksums.put( "SHA-1", "feeeda19f626f9b0ef6cbf5948c1ec9531694295" );
        textFileChecksums.put( "MD5", "12582d1a662cefe3385f2113998e43ed" );
        textFileChecksums.put( "SHA-1", "a8ae272db549850eef2ff54376f8cac2770745ee" );
    }

    @Before
    public void before()
        throws IOException
    {
        sums.clear();

        emptyFile = createTempFile( new byte[] {}, 0 );
        sums.put( emptyFile, emptyFileChecksums );

        patternFile =
            createTempFile( new byte[] { 0, 1, 2, 4, 8, 16, 32, 64, 127, -1, -2, -4, -8, -16, -32, -64, -127 }, 1000 );
        sums.put( patternFile, patternFileChecksums );

        textFile = createTempFile( "the quick brown fox jumps over the lazy dog\n".getBytes( StandardCharsets.UTF_8 ), 500 );
        sums.put( textFile, textFileChecksums );

    }

    @Test
    public void testEquality()
        throws Throwable
    {
        Map<String, Object> checksums = null;

        for ( File file : new File[] { emptyFile, patternFile, textFile } )
        {

            checksums = ChecksumUtils.calc( file, Arrays.asList( "SHA-1", "MD5" ) );

            for ( Entry<String, Object> entry : checksums.entrySet() )
            {
                if ( entry.getValue() instanceof Throwable )
                {
                    throw (Throwable) entry.getValue();
                }
                String actual = entry.getValue().toString();
                String expected = sums.get( file ).get( entry.getKey() );
                assertThat( actual )
                        .as( "checksums do not match for '%s', algorithm '%s'", entry.getKey() , actual )
                .isEqualTo( expected );
            }
            assertThat( file.delete() ).as("Could not delete file").isTrue();
        }
    }

    @Test
    public void testFileHandleLeakage()
        throws IOException
    {
        for ( File file : new File[] { emptyFile, patternFile, textFile } )
        {
            for ( int i = 0; i < 150; i++ )
            {
                ChecksumUtils.calc( file, Arrays.asList( "SHA-1", "MD5" ) );
            }
            assertThat( file.delete() ).as("Could not delete file").isTrue();        }

    }

    @Test
    public void testRead()
        throws IOException
    {
        for ( Map<String, String> checksums : sums.values() )
        {
            String sha1 = checksums.get( "SHA-1" );
            String md5 = checksums.get( "MD5" );

            File sha1File = createTempFile( sha1 );
            File md5File = createTempFile( md5 );

            assertThat(ChecksumUtils.read( sha1File ) ).isEqualTo(sha1);
            assertThat(ChecksumUtils.read( md5File ) ).isEqualTo(md5);

            assertThat( sha1File.delete() ).as( "ChecksumUtils leaks file handles (cannot delete checksums.sha1)")
                    .isTrue();
            assertThat( md5File.delete() ).as( "ChecksumUtils leaks file handles (cannot delete checksums.md5)")
                    .isTrue();
        }
    }

    @Test
    public void testReadSpaces()
        throws IOException
    {
        for ( Map<String, String> checksums : sums.values() )
        {
            String sha1 = checksums.get( "SHA-1" );
            String md5 = checksums.get( "MD5" );

            File sha1File = createTempFile( "sha1-checksum = " + sha1 );
            File md5File = createTempFile( md5 + " test" );

            assertThat(ChecksumUtils.read( sha1File ) ).isEqualTo(sha1);
            assertThat(ChecksumUtils.read( md5File ) ).isEqualTo(md5);

            assertThat( sha1File.delete() ).as( "ChecksumUtils leaks file handles (cannot delete checksums.sha1)")
                    .isTrue();
            assertThat( md5File.delete() ).as( "ChecksumUtils leaks file handles (cannot delete checksums.md5)")
                    .isTrue();
        }
    }

    @Test
    public void testReadEmptyFile()
        throws IOException
    {
        File file = createTempFile( "" );

        assertThat(ChecksumUtils.read( file ) ).isEqualTo("");

        assertThat(file.delete() ).as("ChecksumUtils leaks file handles (cannot delete checksum.empty)")
                .isTrue();
    }

    @Test
    public void testToHexString()
    {
        assertThat( ChecksumUtils.toHexString( null ) ).isNull();
        assertThat( ChecksumUtils.toHexString( new byte[] {} ) ).isEqualTo("");
        assertThat( ChecksumUtils.toHexString( new byte[] { 0 } ) ).isEqualTo("00");
        assertThat( ChecksumUtils.toHexString( new byte[] { -1 } ) ).isEqualTo("ff");
        assertThat( ChecksumUtils.toHexString( new byte[] { 0, 1, 127 } ) ).isEqualTo("00017f");
    }

}
