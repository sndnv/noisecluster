/**
  * Copyright 2017 https://github.com/sndnv
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

import java.io.File

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.util.Timeout
import com.google.inject.{AbstractModule, Provides, Singleton}
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import core3.http.filters.{CompressionFilter, MaintenanceModeFilter, MetricsFilter, TraceFilter}
import io.aeron.driver.MediaDriver
import net.sf.jni4net.Bridge
import noisecluster.jvm.control.cluster
import noisecluster.jvm.transport.aeron.Contexts
import noisecluster.win.interop
import play.api.inject.ApplicationLifecycle
import vili.ApplicationService

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class Module extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[ControlStart]).to(classOf[ControlStartImpl]).asEagerSingleton()
  }

  @Provides
  def provideCompressionFilter(implicit mat: Materializer): CompressionFilter = {
    new CompressionFilter(Vector("text/html", "application/json"), 1400)
  }

  @Provides
  def provideMetricsFilter(implicit mat: Materializer, ec: ExecutionContext): MetricsFilter = {
    new MetricsFilter()
  }

  @Provides
  def provideTraceFilter(implicit mat: Materializer, ec: ExecutionContext): TraceFilter = {
    new TraceFilter()
  }

  @Provides
  def provideMaintenanceFilter(implicit mat: Materializer, ec: ExecutionContext): MaintenanceModeFilter = {
    new MaintenanceModeFilter(Vector("/favicon.ico"))
  }

  @Provides
  @Singleton
  def provideMediaDriver(): Option[MediaDriver] = {
    val appConfig = ConfigFactory.load.getConfig("noisecluster.vili")

    appConfig.getString("transport.provider") match {
      case "aeron" => Some(MediaDriver.launch(Contexts.Driver.lowLatency))
      case "udp" => None
    }
  }

  @Provides
  @Singleton
  def provideInteropService(
    lifecycle: ApplicationLifecycle,
    driver: Option[MediaDriver]
  )(implicit ec: ExecutionContext): interop.SourceService = {
    val baseConfig = ConfigFactory.load()
    val appConfig = baseConfig.getConfig("noisecluster.vili")

    Bridge.setVerbose(true)
    Bridge.init()
    Bridge.LoadAndRegisterAssemblyFrom(new File(appConfig.getString("interopDllPath")))

    val service = appConfig.getString("transport.provider") match {
      case "aeron" =>
        new interop.SourceService(
          appConfig.getInt("transport.aeron.stream"),
          appConfig.getString("transport.aeron.address"),
          appConfig.getInt("transport.aeron.port"),
          if (appConfig.hasPath("transport.aeron.interface")) appConfig.getString("transport.aeron.interface") else null,
          if (appConfig.hasPath("transport.debuggingEnabled")) appConfig.getBoolean("transport.debuggingEnabled") else false
        )

      case "udp" =>
        new interop.SourceService(
          appConfig.getString("transport.udp.address"),
          appConfig.getInt("transport.udp.port"),
          if (appConfig.hasPath("transport.debuggingEnabled")) appConfig.getBoolean("transport.debuggingEnabled") else false
        )
    }

    lifecycle.addStopHook { () =>
      Future {
        try {
          service.Dispose()
        } catch {
          case NonFatal(e) => e.printStackTrace()
        }

        try {
          driver.foreach(_.close())
        } catch {
          case NonFatal(e) => e.printStackTrace()
        }
      }
    }

    service
  }

  @Provides
  @Singleton
  def provideApplicationService(
    lifecycle: ApplicationLifecycle,
    interopService: interop.SourceService
  )(implicit ec: ExecutionContext, system: ActorSystem): ApplicationService = {
    val appConfig = ConfigFactory.load().getConfig("noisecluster.vili")
    val service = new ApplicationService(appConfig, interopService)
    lifecycle.addStopHook { () => Future.successful(service.shutdown()) }
    service
  }

  @Provides
  @Singleton
  def provideControlService(appService: ApplicationService)(implicit ec: ExecutionContext): cluster.SourceService = {
    val baseConfig = ConfigFactory.load()
    val appConfig = baseConfig.getConfig("noisecluster.vili")

    val clusterSystemName = appConfig.getString("control.systemName")
    val clusterHost = appConfig.getString("control.cluster.host")
    val clusterPort = appConfig.getInt("control.cluster.port")
    val clusterAddress = s"akka.tcp://$clusterSystemName@$clusterHost:$clusterPort"

    val localHost = appConfig.getString("control.local.host")
    val localPort = appConfig.getInt("control.local.port")

    val clusterConfig = baseConfig
      .withValue("akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef(localPort))
      .withValue("akka.remote.netty.tcp.hostname", ConfigValueFactory.fromAnyRef(localHost))
      .withValue("akka.cluster.seed-nodes", ConfigValueFactory.fromIterable(Seq(clusterAddress).asJava))
      .withValue("akka.cluster.roles", ConfigValueFactory.fromIterable(Seq("source").asJava))

    implicit val timeout = Timeout(appConfig.getInt("actionTimeout").seconds)

    new cluster.SourceService(
      systemName = appConfig.getString("control.systemName"),
      messengerName = appConfig.getString("control.messengerName"),
      pingInterval = appConfig.getInt("control.pingInterval").seconds,
      appService.localHandlers,
      Some(clusterConfig)
    )
  }
}
