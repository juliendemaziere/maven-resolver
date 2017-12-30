package org.eclipse.aether.repository;

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

import org.eclipse.aether.repository.RemoteRepository;
import org.junit.Test;

/**
 */
public class RemoteRepositoryTest
{

    @Test
    public void testGetProtocol()
    {
        RemoteRepository.Builder builder = new RemoteRepository.Builder( "id", "type", "" );
        RemoteRepository repo = builder.build();
        assertThat(repo.getProtocol() ).isEqualTo("");

        repo = builder.setUrl( "http://localhost" ).build();
        assertThat(repo.getProtocol() ).isEqualTo("http");

        repo = builder.setUrl( "HTTP://localhost" ).build();
        assertThat(repo.getProtocol() ).isEqualTo("HTTP");

        repo = builder.setUrl( "dav+http://www.sonatype.org/" ).build();
        assertThat(repo.getProtocol() ).isEqualTo("dav+http");

        repo = builder.setUrl( "dav:http://www.sonatype.org/" ).build();
        assertThat(repo.getProtocol() ).isEqualTo("dav:http");

        repo = builder.setUrl( "file:/path" ).build();
        assertThat(repo.getProtocol() ).isEqualTo("file");

        repo = builder.setUrl( "file:path" ).build();
        assertThat(repo.getProtocol() ).isEqualTo("file");

        repo = builder.setUrl( "file:C:\\dir" ).build();
        assertThat(repo.getProtocol() ).isEqualTo("file");

        repo = builder.setUrl( "file:C:/dir" ).build();
        assertThat(repo.getProtocol() ).isEqualTo("file");
    }

    @Test
    public void testGetHost()
    {
        RemoteRepository.Builder builder = new RemoteRepository.Builder( "id", "type", "" );
        RemoteRepository repo = builder.build();
        assertThat(repo.getHost() ).isEqualTo("");

        repo = builder.setUrl( "http://localhost" ).build();
        assertThat(repo.getHost() ).isEqualTo("localhost");

        repo = builder.setUrl( "http://localhost/" ).build();
        assertThat(repo.getHost() ).isEqualTo("localhost");

        repo = builder.setUrl( "http://localhost:1234/" ).build();
        assertThat(repo.getHost() ).isEqualTo("localhost");

        repo = builder.setUrl( "http://127.0.0.1" ).build();
        assertThat(repo.getHost() ).isEqualTo("127.0.0.1");

        repo = builder.setUrl( "http://127.0.0.1/" ).build();
        assertThat(repo.getHost() ).isEqualTo("127.0.0.1");

        repo = builder.setUrl( "http://user@localhost/path" ).build();
        assertThat(repo.getHost() ).isEqualTo("localhost");

        repo = builder.setUrl( "http://user:pass@localhost/path" ).build();
        assertThat(repo.getHost() ).isEqualTo("localhost");

        repo = builder.setUrl( "http://user:pass@localhost:1234/path" ).build();
        assertThat(repo.getHost() ).isEqualTo("localhost");
    }

}
