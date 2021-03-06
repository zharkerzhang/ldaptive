/* See LICENSE for licensing and NOTICE for copyright. */
package org.ldaptive.provider.jndi;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import org.ldaptive.ConnectionStrategy;
import org.ldaptive.LdapException;
import org.ldaptive.provider.AbstractProviderConnectionFactory;
import org.ldaptive.provider.ConnectionException;
import org.ldaptive.ssl.SslConfig;
import org.ldaptive.ssl.ThreadLocalTLSSocketFactory;

/**
 * Creates connections using the JNDI {@link InitialLdapContext} class.
 *
 * @author  Middleware Services
 */
public class JndiConnectionFactory extends AbstractProviderConnectionFactory<JndiProviderConfig>
{

  /** Environment properties. */
  private final Map<String, Object> environment;

  /** Context class loader to use when instantiating {@link InitialLdapContext}. */
  private final ClassLoader classLoader;

  /** Thread local SslConfig, if one exists. */
  private SslConfig threadLocalSslConfig;


  /**
   * Creates a new jndi connection factory.
   *
   * @param  url  of the ldap to connect to
   * @param  strategy  connection strategy
   * @param  config  provider configuration
   * @param  env  jndi context environment
   * @param  cl  class loader
   */
  public JndiConnectionFactory(
    final String url,
    final ConnectionStrategy strategy,
    final JndiProviderConfig config,
    final Map<String, Object> env,
    final ClassLoader cl)
  {
    super(url, strategy, config);
    environment = Collections.unmodifiableMap(env);
    classLoader = cl;
    if (ThreadLocalTLSSocketFactory.class.getName().equals(environment.get(JndiProvider.SOCKET_FACTORY))) {
      threadLocalSslConfig = new ThreadLocalTLSSocketFactory().getSslConfig();
    }
  }


  @Override
  protected JndiConnection createInternal(final String url)
    throws LdapException
  {
    if (
      threadLocalSslConfig != null &&
        ThreadLocalTLSSocketFactory.class.getName().equals(environment.get(JndiProvider.SOCKET_FACTORY))) {
      final ThreadLocalTLSSocketFactory sf = new ThreadLocalTLSSocketFactory();
      sf.setSslConfig(threadLocalSslConfig);
    }

    // CheckStyle:IllegalType OFF
    // the JNDI API requires the Hashtable type
    final Hashtable<String, Object> env = new Hashtable<>(environment);
    // CheckStyle:IllegalType ON
    env.put(JndiProvider.PROVIDER_URL, url);

    JndiConnection conn;
    try {
      if (classLoader != null) {
        final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        try {
          Thread.currentThread().setContextClassLoader(classLoader);
          conn = new JndiConnection(new InitialLdapContext(env, null), getProviderConfig());
        } finally {
          Thread.currentThread().setContextClassLoader(currentClassLoader);
        }
      } else {
        conn = new JndiConnection(new InitialLdapContext(env, null), getProviderConfig());
      }
    } catch (NamingException e) {
      throw new ConnectionException(e, NamingExceptionUtils.getResultCode(e.getClass()));
    }
    return conn;
  }


  @Override
  public String toString()
  {
    return
      String.format(
        "[%s@%d::metadata=%s, environment=%s, classLoader=%s, providerConfig=%s]",
        getClass().getName(),
        hashCode(),
        getMetadata(),
        environment,
        classLoader,
        getProviderConfig());
  }
}
