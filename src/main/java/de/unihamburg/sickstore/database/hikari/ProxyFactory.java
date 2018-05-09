package de.unihamburg.sickstore.database.hikari;

import com.zaxxer.hikari.util.FastList;
import de.unihamburg.sickstore.database.client.SickConnection;
import de.unihamburg.sickstore.database.messages.ClientRequest;

/**
 * A factory class that produces proxies around instances of the standard
 * JDBC interfaces.
 *
 * @author Brett Wooldridge
 */
public final class ProxyFactory
{
   private ProxyFactory()
   {
      // unconstructable
   }

   /**
    * Create a proxy for the specified {@link SickConnection} instance.
    * @param poolEntry the PoolEntry holding pool state
    * @param connection the raw database Connection
    * @param openStatements a reusable list to track open Statement instances
    * @param leakTask the ProxyLeakTask for this connection
    * @param now the current timestamp
    * @param isReadOnly the default readOnly state of the connection
    * @param isAutoCommit the default autoCommit state of the connection
    * @return a proxy that wraps the specified {@link SickConnection}
    */
   static ProxyConnection getProxyConnection(final PoolEntry poolEntry, final SickConnection connection, final FastList<ClientRequest> openStatements, final ProxyLeakTask leakTask, final long now, final boolean isReadOnly, final boolean isAutoCommit)
   {
      return new ProxyConnection(poolEntry, connection, openStatements, leakTask, now, isReadOnly, isAutoCommit);
   }
}
