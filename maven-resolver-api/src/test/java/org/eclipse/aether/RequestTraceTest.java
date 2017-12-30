package org.eclipse.aether;

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

import org.junit.Test;

/**
 */
public class RequestTraceTest
{

    @Test
    public void testConstructor()
    {
        RequestTrace trace = new RequestTrace( null );
        assertThat(trace.getData() ).isSameAs(null);

        trace = new RequestTrace( this );
        assertThat(trace.getData() ).isSameAs(this);
    }

    @Test
    public void testParentChaining()
    {
        RequestTrace trace1 = new RequestTrace( null );
        RequestTrace trace2 = trace1.newChild( this );

        assertThat(trace1.getParent() ).isSameAs(null);
        assertThat(trace1.getData() ).isSameAs(null);
        assertThat(trace2.getParent() ).isSameAs(trace1);
        assertThat(trace2.getData() ).isSameAs(this);
    }

    @Test
    public void testNewChildRequestTrace()
    {
        RequestTrace trace = RequestTrace.newChild( null, this );
        assertThat(trace ).isNotNull();
        assertThat(trace.getParent() ).isSameAs(null);
        assertThat(trace.getData() ).isSameAs(this);
    }

}
