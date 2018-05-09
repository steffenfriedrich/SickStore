/*
 * Copyright (C) 2014 Brett Wooldridge
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.unihamburg.sickstore.database.hikari;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import de.unihamburg.sickstore.database.client.Connection;
import de.unihamburg.sickstore.database.client.SickConnection;

import java.sql.SQLException;
import java.util.Properties;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

/**
 * Reimplemtation of HikariCP's {@link com.zaxxer.hikari.metrics.dropwizard.CodahaleHealthChecker}  for the use with SickStore
 * @author Steffen Friedrich
 */
public final class CodahaleHealthChecker
{
   /**
    * Register Dropwizard health checks.
    *
    * @param pool the pool to register health checks for
    * @param hikariConfig the pool configuration
    * @param registry the HealthCheckRegistry into which checks will be registered
    */
   public static void registerHealthChecks(final HikariPool pool, final HikariConfig hikariConfig, final HealthCheckRegistry registry)
   {
      final Properties healthCheckProperties = hikariConfig.getHealthCheckProperties();
      final MetricRegistry metricRegistry = (MetricRegistry) hikariConfig.getMetricRegistry();

      final long checkTimeoutMs = Long.parseLong(healthCheckProperties.getProperty("connectivityCheckTimeoutMs", String.valueOf(hikariConfig.getConnectionTimeout())));
      registry.register(MetricRegistry.name(hikariConfig.getPoolName(), "pool", "ConnectivityCheck"), new ConnectivityHealthCheck(pool, checkTimeoutMs));

      final long expected99thPercentile = Long.parseLong(healthCheckProperties.getProperty("expected99thPercentileMs", "0"));
      if (metricRegistry != null && expected99thPercentile > 0) {
         SortedMap<String,Timer> timers = metricRegistry.getTimers(new MetricFilter() {
            @Override
            public boolean matches(String name, Metric metric)
            {
               return name.equals(MetricRegistry.name(hikariConfig.getPoolName(), "pool", "Wait"));
            }
         });

         if (!timers.isEmpty()) {
            final Timer timer = timers.entrySet().iterator().next().getValue();
            registry.register(MetricRegistry.name(hikariConfig.getPoolName(), "pool", "Connection99Percent"), new Connection99Percent(timer, expected99thPercentile));
         }
      }
   }

   private CodahaleHealthChecker()
   {
      // private constructor
   }

   private static class ConnectivityHealthCheck extends HealthCheck
   {
      private final HikariPool pool;
      private final long checkTimeoutMs;

      ConnectivityHealthCheck(final HikariPool pool, final long checkTimeoutMs)
      {
         this.pool = pool;
         this.checkTimeoutMs = (checkTimeoutMs > 0 && checkTimeoutMs != Integer.MAX_VALUE ? checkTimeoutMs : TimeUnit.SECONDS.toMillis(10));
      }

      /** {@inheritDoc} */
      @Override
      protected Result check() throws Exception
      {
         try (Connection connection = pool.getConnection(checkTimeoutMs)) {
            return Result.healthy();
         }
         catch (SQLException e) {
            return Result.unhealthy(e);
         }
      }
   }

   private static class Connection99Percent extends HealthCheck
   {
      private final Timer waitTimer;
      private final long expected99thPercentile;

      Connection99Percent(final Timer waitTimer, final long expected99thPercentile)
      {
         this.waitTimer = waitTimer;
         this.expected99thPercentile = expected99thPercentile;
      }

      /** {@inheritDoc} */
      @Override
      protected Result check() throws Exception
      {
         final long the99thPercentile = TimeUnit.NANOSECONDS.toMillis(Math.round(waitTimer.getSnapshot().get99thPercentile()));
         return the99thPercentile <= expected99thPercentile ? Result.healthy() : Result.unhealthy("99th percentile connection wait time of %dms exceeds the threshold %dms", the99thPercentile, expected99thPercentile);
      }
   }
}
