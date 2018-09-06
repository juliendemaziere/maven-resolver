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

import org.eclipse.aether.spi.connector.checksum.ChecksumPolicy;
import org.eclipse.aether.transfer.ChecksumFailureException;
import org.eclipse.aether.transfer.TransferResource;
import org.junit.Before;
import org.junit.Test;

public class FailChecksumPolicyTest
{

    private FailChecksumPolicy policy;

    private ChecksumFailureException exception;

    @Before
    public void setup()
    {
        policy = new FailChecksumPolicy( new TransferResource( "null", "file:/dev/null", "file.txt", null, null ) );
        exception = new ChecksumFailureException( "test" );
    }

    @Test
    public void testOnTransferChecksumFailure()
    {
        assertThat(policy.onTransferChecksumFailure( exception ) ).isFalse();
    }

    @Test
    public void testOnChecksumMatch()
    {
        assertThat(policy.onChecksumMatch( "SHA-1", 0 ) ).isTrue();
        assertThat(policy.onChecksumMatch( "SHA-1", ChecksumPolicy.KIND_UNOFFICIAL ) ).isTrue();
    }

    @Test
    public void testOnChecksumMismatch()
        throws Exception
    {
        try
        {
            policy.onChecksumMismatch( "SHA-1", 0, exception );
            fail( "No exception" );
        }
        catch ( ChecksumFailureException e )
        {
            assertThat(e ).isSameAs(exception);
        }
        policy.onChecksumMismatch( "SHA-1", ChecksumPolicy.KIND_UNOFFICIAL, exception );
    }

    @Test
    public void testOnChecksumError()
        throws Exception
    {
        policy.onChecksumError( "SHA-1", 0, exception );
    }

    @Test
    public void testOnNoMoreChecksums()
    {
        try
        {
            policy.onNoMoreChecksums();
            fail( "No exception" );
        }
        catch ( ChecksumFailureException e )
        {
            assertThat(e.getMessage().contains( "no checksums available" ) ).isTrue();
        }
    }

}
