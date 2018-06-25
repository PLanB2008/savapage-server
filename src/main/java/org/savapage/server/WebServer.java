/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.SecuredRedirectHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.savapage.common.ConfigDefaults;
import org.savapage.core.SpException;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.SslCertInfo;
import org.savapage.core.ipp.operation.IppMessageMixin;
import org.savapage.core.util.InetUtils;
import org.savapage.server.feed.AtomFeedLoginService;
import org.savapage.server.xmlrpc.SpXmlRpcServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main class for the Web Server.
 *
 * @author Rijk Ravestein
 *
 */
public final class WebServer {

    /**
     * Redirect all traffic except IPP to SSL.
     */
    private static class MySecuredRedirectHandler
            extends SecuredRedirectHandler {

        @Override
        public void handle(final String target,
                final org.eclipse.jetty.server.Request baseRequest,
                final HttpServletRequest request,
                final HttpServletResponse response)
                throws IOException, ServletException {

            final String contentTypeReq = request.getContentType();

            /*
             * For now, take /xmlrpc as it is, do not redirect. Reason: C++
             * modules are not prepared for SSL yet.
             */
            if (request.getPathInfo()
                    .startsWith(SpXmlRpcServlet.URL_PATTERN_BASE)) {
                return;
            }

            /*
             * Take IPP traffic as it is, do not redirect.
             */
            if (contentTypeReq != null && contentTypeReq
                    .equalsIgnoreCase(IppMessageMixin.CONTENT_TYPE_IPP)) {
                return;
            }

            super.handle(target, baseRequest, request, response);
        }
    }

    /**
     *
     */
    private static class ConnectorConfig {

        private static final int MIN_THREADS = 20;

        private static final int MAX_THREADS_X64 = 8000;

        private static final int MAX_THREADS_I686 = 4000;

        private static final int MAX_IDLE_TIME_MSEC = 30000;

        /**
         *
         * @return
         */
        public static int getMinThreads() {
            return MIN_THREADS;
        }

        public static boolean isX64() {
            return System.getProperty("os.arch").equalsIgnoreCase("amd64");
        }

        /**
         *
         * @return
         */
        public static int getMaxThreads() {

            final int maxThreads;

            if (isX64()) {
                maxThreads = MAX_THREADS_X64;
            } else {
                maxThreads = MAX_THREADS_I686;
            }

            return maxThreads;
        }

        public static int getIdleTimeoutMsec() {
            return MAX_IDLE_TIME_MSEC;
        }
    }

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(WebServer.class);

    /**
     * .
     */
    private static final String PROP_KEY_SERVER_PORT = "server.port";

    /**
     * .
     */
    private static final String PROP_KEY_SERVER_PORT_SSL = "server.ssl.port";

    /**
     * .
     */
    private static final String PROP_KEY_HTML_REDIRECT_SSL =
            "server.html.redirect.ssl";

    /**
     * .
     */
    private static final String PROP_KEY_SSL_KEYSTORE = "server.ssl.keystore";

    /**
     * .
     */
    private static final String PROP_KEY_SSL_KEYSTORE_PW =
            "server.ssl.keystore-password";

    /**
     * .
     */
    private static final String PROP_KEY_SSL_KEY_PW = "server.ssl.key-password";

    /** */
    private static final String PROP_KEY_WEBAPP_CUSTOM_I18N =
            "webapp.custom.i18n";

    /**
     * .
     */
    private static int serverPort;

    /**
     * .
     */
    private static int serverPortSsl;

    /**
     * .
     */
    private static boolean serverSslRedirect;

    /** */
    private static boolean webAppCustomI18n;

    /**
    *
    */
    private WebServer() {
    }

    /**
     *
     * @return {@code true} when custom Web App i18n is to be applied.
     */
    public static boolean isWebAppCustomI18n() {
        return webAppCustomI18n;
    }

    /**
     *
     * @return The server port.
     */
    public static int getServerPort() {
        return serverPort;
    }

    /**
     *
     * @return The server SSL port.
     */
    public static int getServerPortSsl() {
        return serverPortSsl;
    }

    /**
     *
     * @return {@code true} when server access is SSL only.
     */
    public static boolean isSSLOnly() {
        return serverPortSsl > 0 && serverPort == 0;
    }

    /**
     *
     * @return {@code true} when non-SSL port is redirected to SSL port.
     */
    public static boolean isSSLRedirect() {
        return serverSslRedirect;
    }

    /**
     * Creates the {@link SslCertInfo}.
     *
     * @param ksLocation
     *            The keystore location.
     * @param ksPassword
     *            The keystore password.
     * @return The {@link SslCertInfo}, or {@code null}. when alias is not
     *         found.
     */
    private static SslCertInfo createSslCertInfo(final String ksLocation,
            final String ksPassword) {

        final File file = new File(ksLocation);

        SslCertInfo certInfo = null;

        try (FileInputStream is = new FileInputStream(file);) {

            final KeyStore keystore =
                    KeyStore.getInstance(KeyStore.getDefaultType());

            keystore.load(is, ksPassword.toCharArray());

            final Enumeration<String> aliases = keystore.aliases();

            /*
             * Get X509 cert and alias with most recent "not after".
             */
            long minNotAfter = Long.MAX_VALUE;
            java.security.cert.X509Certificate minCertX509 = null;
            String minAlias = null;
            int nAliases = 0;

            while (aliases.hasMoreElements()) {

                final String alias = aliases.nextElement();

                final java.security.cert.Certificate cert =
                        keystore.getCertificate(alias);

                if (cert instanceof java.security.cert.X509Certificate) {

                    java.security.cert.X509Certificate certX509 =
                            (java.security.cert.X509Certificate) cert;

                    final long notAfter = certX509.getNotAfter().getTime();
                    if (notAfter < minNotAfter) {
                        minCertX509 = certX509;
                        minAlias = alias;
                    }

                    nAliases++;
                }
            }

            if (minCertX509 != null) {

                final Date creationDate = keystore.getCreationDate(minAlias);
                final Date notAfter = minCertX509.getNotAfter();

                final LdapName ln =
                        new LdapName(minCertX509.getIssuerDN().getName());

                for (final Rdn rdn : ln.getRdns()) {

                    if (rdn.getType().equalsIgnoreCase("CN")) {

                        final String issuerCN = rdn.getValue().toString();

                        certInfo = new SslCertInfo(issuerCN, creationDate,
                                notAfter, nAliases == 1);
                        break;
                    }
                }
            }

        } catch (KeyStoreException | NoSuchAlgorithmException
                | CertificateException | IOException | InvalidNameException e) {
            LOGGER.error(e.getMessage(), e);
            throw new SpException(e.getMessage(), e);
        }

        return certInfo;
    }

    /**
     * @return {@code true} when Java 8 runtime.
     */
    private static boolean checkJava8() {
        try {
            // Pick a class that was introduced in Java 8.
            final String java8ClassCheck = "java.time.Duration";
            Class.forName(java8ClassCheck);
            return true;
        } catch (ClassNotFoundException e) {
            // no code intended.
        }

        final String msg =
                "\n+=================================================+"
                        + "\n| SavaPage NOT started: "
                        + "Java 8 MUST be installed. |"
                        + "\n+========================"
                        + "=========================+";
        System.err.println(new Date().toString() + " : " + msg);
        LOGGER.error(msg);
        return false;
    }

    /**
     * Starts the Web Server.
     * <p>
     * References:
     * </p>
     * <ul>
     * <li>Jetty: <a href="See:
     * https://www.eclipse.org/jetty/documentation/current/using-annotations
     * .html">Working with Annotations</a></li>
     * </ul>
     *
     * @param args
     *            The arguments.
     * @throws Exception
     *             When unexpected things happen.
     */
    public static void main(final String[] args) throws Exception {

        if (!checkJava8()) {
            return;
        }

        ConfigManager.setDefaultServerPort(ConfigDefaults.SERVER_PORT);
        ConfigManager.setDefaultServerSslPort(ConfigDefaults.SERVER_SSL_PORT);

        /*
         * Passed as -Dserver.home to JVM
         */
        final Properties propsServer = ConfigManager.loadServerProperties();

        /*
         * Notify central WebApp.
         */
        WebApp.setServerProps(propsServer);
        WebApp.loadWebProperties();

        /*
         * Server Ports.
         */
        serverPort = Integer.parseInt(propsServer
                .getProperty(PROP_KEY_SERVER_PORT, ConfigDefaults.SERVER_PORT));

        serverPortSsl = Integer.parseInt(propsServer.getProperty(
                PROP_KEY_SERVER_PORT_SSL, ConfigDefaults.SERVER_SSL_PORT));

        /*
         * Check if ports are in use.
         */
        boolean portsInUse = false;

        for (final int port : new int[] { serverPort, serverPortSsl }) {
            if (InetUtils.isPortInUse(port)) {
                portsInUse = true;
                System.err.println(String.format("Port [%d] is in use.", port));
            }
        }
        if (portsInUse) {
            System.err.println(String.format("%s not started.",
                    CommunityDictEnum.SAVAPAGE.getWord()));
            System.exit(-1);
            return;
        }

        //
        serverSslRedirect = !isSSLOnly() && BooleanUtils.toBooleanDefaultIfNull(
                BooleanUtils.toBooleanObject(
                        propsServer.getProperty(PROP_KEY_HTML_REDIRECT_SSL)),
                false);

        webAppCustomI18n = BooleanUtils.toBooleanDefaultIfNull(
                BooleanUtils.toBooleanObject(
                        propsServer.getProperty(PROP_KEY_WEBAPP_CUSTOM_I18N)),
                false);
        //
        final QueuedThreadPool threadPool = new QueuedThreadPool();
        final String poolName = "jetty-threadpool";

        threadPool.setName(poolName);
        threadPool.setMinThreads(ConnectorConfig.getMinThreads());
        threadPool.setMaxThreads(ConnectorConfig.getMaxThreads());
        threadPool.setIdleTimeout(ConnectorConfig.getIdleTimeoutMsec());

        final Server server = new Server(threadPool);

        /*
         * This is needed to enable the Jetty annotations.
         */
        org.eclipse.jetty.webapp.Configuration.ClassList classlist =
                org.eclipse.jetty.webapp.Configuration.ClassList
                        .setServerDefault(server);
        classlist.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
                "org.eclipse.jetty.annotations.AnnotationConfiguration");

        /*
         * HttpConfiguration is a collection of configuration information
         * appropriate for http and https.
         *
         * The default scheme for http is <code>http</code> of course, as the
         * default for secured http is <code>https</code> but we show setting
         * the scheme to show it can be done.
         *
         * The port for secured communication is also set here.
         */
        final HttpConfiguration httpConfig = new HttpConfiguration();

        httpConfig.setSecureScheme("https");
        httpConfig.setSecurePort(serverPortSsl);

        if (!isSSLOnly()) {
            /*
             * The server connector we create is the one for http, passing in
             * the http configuration we configured above so it can get things
             * like the output buffer size, etc. We also set the port and
             * configure an idle timeout.
             */
            final ServerConnector http = new ServerConnector(server,
                    new HttpConnectionFactory(httpConfig));

            http.setPort(serverPort);
            http.setIdleTimeout(ConnectorConfig.getIdleTimeoutMsec());

            server.addConnector(http);
        }

        /*
         * SSL Context Factory for HTTPS and SPDY.
         *
         * SSL requires a certificate so we configure a factory for ssl contents
         * with information pointing to what keystore the ssl connection needs
         * to know about.
         *
         * Much more configuration is available the ssl context, including
         * things like choosing the particular certificate out of a keystore to
         * be used.
         */

        final SslContextFactory sslContextFactory = new SslContextFactory();

        // Mantis #562
        sslContextFactory.addExcludeCipherSuites(
                //
                // weak
                "TLS_RSA_WITH_RC4_128_MD5",
                // weak
                "TLS_RSA_WITH_RC4_128_SHA",
                // insecure
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
                // weak
                "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
                // insecure
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                // insecure
                "TLS_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
                // insecure
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
                // insecure
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256"
        //
        );

        final String serverHome = ConfigManager.getServerHome();

        final String ksLocation;
        final String ksPassword;

        if (propsServer.getProperty(PROP_KEY_SSL_KEYSTORE) == null) {

            InputStream istr;

            /**
             *
             */
            final Properties propsPw = new Properties();

            istr = new java.io.FileInputStream(
                    serverHome + "/data/default-ssl-keystore.pw");

            propsPw.load(istr);
            ksPassword = propsPw.getProperty("password");
            istr.close();

            /**
             *
             */
            ksLocation = serverHome + "/data/default-ssl-keystore";

            istr = new java.io.FileInputStream(ksLocation);
            final KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(istr, ksPassword.toCharArray());
            istr.close();

            /**
             *
             */
            sslContextFactory.setKeyStore(ks);
            sslContextFactory.setKeyManagerPassword(ksPassword);

        } else {

            ksLocation = String.format("%s%c%s", serverHome, File.separatorChar,
                    propsServer.getProperty(PROP_KEY_SSL_KEYSTORE));

            ksPassword = propsServer.getProperty(PROP_KEY_SSL_KEYSTORE_PW);

            final Resource keystore = Resource.newResource(ksLocation);

            sslContextFactory.setKeyStoreResource(keystore);

            sslContextFactory.setKeyStorePassword(ksPassword);

            sslContextFactory.setKeyManagerPassword(
                    propsServer.getProperty(PROP_KEY_SSL_KEY_PW));
        }

        ConfigManager.setSslCertInfo(createSslCertInfo(ksLocation, ksPassword));

        /*
         * HTTPS Configuration
         *
         * A new HttpConfiguration object is needed for the next connector and
         * you can pass the old one as an argument to effectively clone the
         * contents. On this HttpConfiguration object we add a
         * SecureRequestCustomizer which is how a new connector is able to
         * resolve the https connection before handing control over to the Jetty
         * Server.
         */
        final HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
        httpsConfig.addCustomizer(new SecureRequestCustomizer());

        /*
         * HTTPS connector
         *
         * We create a second ServerConnector, passing in the http configuration
         * we just made along with the previously created ssl context factory.
         * Next we set the port and a longer idle timeout.
         */
        final ServerConnector https = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory,
                        HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory(httpsConfig));

        https.setPort(serverPortSsl);
        https.setIdleTimeout(ConnectorConfig.getIdleTimeoutMsec());

        server.addConnector(https);

        /*
         * Set a handler
         */
        final WebAppContext webAppContext = new WebAppContext();

        webAppContext.setServer(server);
        webAppContext.setContextPath("/");

        boolean fDevelopment =
                (System.getProperty("savapage.war.file") == null);

        String pathToWarFile = null;

        if (fDevelopment) {
            pathToWarFile = "src/main/webapp";
        } else {
            pathToWarFile = serverHome + "/lib/"
                    + System.getProperty("savapage.war.file");
        }

        webAppContext.setWar(pathToWarFile);

        /*
         * This is needed for scanning "discoverable" Jetty annotations. The
         * "/classes/.*" scan is needed when running in development (Eclipse).
         * The "/savapage-server-*.jar$" scan in needed for production.
         */
        webAppContext.setAttribute(
                "org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",
                ".*/savapage-server-[^/]*\\.jar$|.*/classes/.*");

        /*
         * Redirect to SSL?
         */
        final Handler[] handlerArray;

        if (serverSslRedirect) {
            handlerArray = new Handler[] { new MySecuredRedirectHandler(),
                    webAppContext };
        } else {
            handlerArray = new Handler[] { webAppContext };
        }

        /*
         * Set cookies to HttpOnly.
         */
        webAppContext.getSessionHandler().getSessionCookieConfig()
                .setHttpOnly(true);

        /*
         * Set the handler(s).
         */
        final HandlerList handlerList = new HandlerList();
        handlerList.setHandlers(handlerArray);

        server.setHandler(handlerList);

        /*
         * BASIC Authentication for Atom Feed.
         */
        server.addBean(new AtomFeedLoginService());

        //
        final String serverStartedFile =
                String.format("%s%clogs%cserver.started.txt", serverHome,
                        File.separatorChar, File.separatorChar);

        int status = 0;

        try (FileWriter writer = new FileWriter(serverStartedFile);) {
            /*
             * Writing the time we started in a file. This file is monitored by
             * the install script to see when the server has started.
             */

            final Date now = new Date();

            writer.write("#");
            writer.write(now.toString());
            writer.write("\n");
            writer.write(String.valueOf(now.getTime()));
            writer.write("\n");

            writer.flush();

            Runtime.getRuntime()
                    .addShutdownHook(new WebServerShutdownHook(server));

            /*
             * Start the server: WebApp is initialized.
             */
            server.start();

            if (WebApp.hasInitializeError()) {
                System.exit(1);
                return;
            }

            if (!fDevelopment) {
                server.join();
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            status = 1;
        }

        if (status == 0) {

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("server [" + server.getState() + "]");
            }

            if (fDevelopment) {
                System.out
                        .println(" \n+========================================"
                                + "====================================+"
                                + "\n| You're running in development mode. "
                                + "Click in this console and press ENTER. |"
                                + "\n| This will call System.exit() so the "
                                + "shutdown routine is executed.          |"
                                + "\n+====================================="
                                + "=======================================+"
                                + "\n");
                try {

                    System.in.read();
                    System.exit(0);

                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }
}
