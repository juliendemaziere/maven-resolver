package org.eclipse.aether.util.repository;

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

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.AuthenticationContext;
import org.eclipse.aether.repository.AuthenticationDigest;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.repository.RemoteRepository;

/**
 * A proxy selector that uses the {@link java.net.ProxySelector#getDefault() JRE's global proxy selector}. In
 * combination with the system property {@code java.net.useSystemProxies}, this proxy selector can be employed to pick
 * up the proxy configuration from the operating system, see <a
 * href="http://docs.oracle.com/javase/6/docs/technotes/guides/net/proxies.html">Java Networking and Proxies</a> for
 * details. The {@link java.net.Authenticator JRE's global authenticator} is used to look up credentials for a proxy
 * when needed.
 */
public final class JreProxySelector
    implements ProxySelector
{

    /**
     * Creates a new proxy selector that delegates to {@link java.net.ProxySelector#getDefault()}.
     */
    public JreProxySelector()
    {
    }

    @Override
    public Proxy getProxy(RemoteRepository repository) {
        return getProxy(repository.getUrl());
    }

    public Proxy getProxy(final String url) {
        try {
            final java.net.ProxySelector systemSelector = java.net.ProxySelector.getDefault();
            if (systemSelector == null) {
                return null;
            }
            URI uri = new URI( url ).parseServerAuthority();
            final List<java.net.Proxy> selected = systemSelector.select(uri);
            if (selected == null || selected.isEmpty()) {
                return null;
            }
            for (java.net.Proxy proxy : selected) {
                if (proxy.type() == java.net.Proxy.Type.HTTP && isValid(proxy.address())) {
                    final String proxyType = chooseProxyType(uri.getScheme());
                    if (proxyType != null) {
                        final InetSocketAddress addr = (InetSocketAddress)proxy.address();
                        return new Proxy(proxyType, addr.getHostName(), addr.getPort(), JreProxyAuthentication.INSTANCE);
                    }
                }
            }
        }
        catch ( Exception e )
        {
            // URL invalid or not accepted by selector or no selector at all, simply use no proxy
        }
        return null;
    }

    private static String chooseProxyType(final String protocol)
    {
        if (Proxy.TYPE_HTTP.equals(protocol))
        {
            return Proxy.TYPE_HTTP;
        }
        if (Proxy.TYPE_HTTPS.equals(protocol))
        {
            return Proxy.TYPE_HTTPS;
        }
        return null;
    }

    private static boolean isValid( SocketAddress address )
    {
        if ( address instanceof InetSocketAddress )
        {
            /*
             * NOTE: On some platforms with java.net.useSystemProxies=true, unconfigured proxies show up as proxy
             * objects with empty host and port 0.
             */
            InetSocketAddress addr = (InetSocketAddress) address;
            if ( addr.getPort() <= 0 )
            {
                return false;
            }
            if ( addr.getHostName() == null || addr.getHostName().length() <= 0 )
            {
                return false;
            }
            return true;
        }
        return false;
    }

    private static final class JreProxyAuthentication
        implements Authentication
    {

        public static final Authentication INSTANCE = new JreProxyAuthentication();

        public void fill( AuthenticationContext context, String key, Map<String, String> data )
        {
            Proxy proxy = context.getProxy();
            if ( proxy == null )
            {
                return;
            }
            if ( !AuthenticationContext.USERNAME.equals( key ) && !AuthenticationContext.PASSWORD.equals( key ) )
            {
                return;
            }

            try
            {
                URL url;
                String protocol = "http";
               try
               {
                    url = new URL(context.getRepository().getUrl());
                    protocol = url.getProtocol();
                }
                catch ( Exception e )
                {
                    url = null;
                }

                PasswordAuthentication auth =
                    Authenticator.requestPasswordAuthentication( proxy.getHost(), null, proxy.getPort(), protocol,
                            "Credentials for proxy " + proxy, null, url, Authenticator.RequestorType.PROXY );
                if (auth != null)
                {
                    context.put( AuthenticationContext.USERNAME, auth.getUserName() );
                    context.put( AuthenticationContext.PASSWORD, auth.getPassword() );
                }
                else
                {
                    context.put(AuthenticationContext.USERNAME, System.getProperty(protocol + ".proxyUser"));
                    context.put(AuthenticationContext.PASSWORD, System.getProperty(protocol + ".proxyPassword"));
                }
            }
            catch ( SecurityException e )
            {
                // oh well, let's hope the proxy can do without auth
            }
        }

        public void digest( AuthenticationDigest digest )
        {
            // we don't know anything about the JRE's current authenticator, assume the worst (i.e. interactive)
            digest.update( UUID.randomUUID().toString() );
        }

        @Override
        public boolean equals( Object obj )
        {
            return this == obj || ( obj != null && getClass().equals( obj.getClass() ) );
        }

        @Override
        public int hashCode()
        {
            return getClass().hashCode();
        }

    }

}
