/*
  Copyright 1995-2017 Esri

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

  For additional information, contact:
  Environmental Systems Research Institute, Inc.
  Attn: Contracts Dept
  380 New York Street
  Redlands, California, USA 92373

  email: contracts@esri.com
 */

package com.esri.geoevent.transport.azure


import java.nio.{BufferOverflowException, ByteBuffer}

//import akka.actor.ActorSystem
//import akka.stream.ActorMaterializer
import com.esri.ges.core.component.RunningState
import com.esri.ges.framework.i18n.{BundleLogger, BundleLoggerFactory}
import com.esri.ges.transport.{InboundTransportBase, TransportDefinition}
import com.microsoft.azure.iot.iothubreact.scaladsl.IoTHub

//import akka.stream.scaladsl.Sink
import com.microsoft.azure.iot.iothubreact.ResumeOnError._

//import com.microsoft.azure.iot.iothubreact.scaladsl._

class AzureIoTHubInboundTransport(definition: TransportDefinition)
    extends InboundTransportBase(definition) {

  // logger
  private val LOGGER: BundleLogger = BundleLoggerFactory.getLogger(classOf[AzureIoTHubInboundTransport])

  // data members
  private var errorMessage: String = ""

  override def start(): Unit = {
    //TODO -  sync this
    val runningState = getRunningState
    runningState match {
      case RunningState.STARTING | RunningState.STARTED | RunningState.ERROR =>

      case _ =>
        // start
        setRunningState(RunningState.STARTING)

        applyProperties()

        //val cl = Thread.currentThread.getContextClassLoader
        //Thread.currentThread.setContextClassLoader(classOf[IoTHub].getClassLoader)

        val now = java.time.Instant.now()
        IoTHub().source(now)
            //.map(m => m.contentAsString)
            .runForeach(event => receive(event.content))

      //Thread.currentThread.setContextClassLoader(cl)
    }
  }

  override def stop(): Unit = {
    //TODO -  sync this
    if (getRunningState == RunningState.STOPPING)
      return

    errorMessage = null
    setRunningState(RunningState.STOPPING)
    cleanup()
    // setErrorMessage(null)
    setRunningState(RunningState.STOPPED)
  }

  protected def cleanup() = {
    // TODO...
    LOGGER.debug("CLEANUP_COMPLETE")
  }

  override def validate(): Unit = {
    // TODO: Validate
  }

  override def isRunning(): Boolean = {
    //TODO -  sync this
    getRunningState == RunningState.STARTED
  }

  def applyProperties() = {
    try {
      val hubName = getProperty(AzureIoTHubInboundTransportDefinition.HUB_NAME).getValueAsString
      val hubEndpoint = getProperty(AzureIoTHubInboundTransportDefinition.HUB_ENDPOINT).getValueAsString
      val hubPartitions = getProperty(AzureIoTHubInboundTransportDefinition.HUB_PARTITIONS).getValueAsString
      val accessPolicy = getProperty(AzureIoTHubInboundTransportDefinition.ACCESS_POLICY).getValueAsString
      val accessKey = getProperty(AzureIoTHubInboundTransportDefinition.ACCESS_KEY).getValueAsString

      System.setProperty("IOTHUB_EVENTHUB_NAME", hubName)
      System.setProperty("IOTHUB_EVENTHUB_ENDPOINT", hubEndpoint)
      System.setProperty("IOTHUB_EVENTHUB_PARTITIONS", hubPartitions)
      System.setProperty("IOTHUB_ACCESS_POLICY", accessPolicy)
      System.setProperty("IOTHUB_ACCESS_KEY", accessKey)
    } catch {
      case error: Exception =>
        errorMessage = LOGGER.translate("ERROR_READING_PROPS")
        LOGGER.error("ERROR_READING_PROPS", error)
    }
  }

  private def receive(bytes: Array[Byte]) = {
    if (bytes != null && bytes.length > 0) {
      val str = new String(bytes) + '\n'
      val newBytes = str.getBytes()

      val bb = ByteBuffer.allocate(newBytes.length)
      try {
        bb.put(newBytes)
        bb.flip()
        byteListener.receive(bb, "")
        bb.clear()
      } catch {
        case boe: BufferOverflowException =>
          LOGGER.error("BUFFER_OVERFLOW_ERROR", boe)
          bb.clear()
          setRunningState(RunningState.ERROR)
        case error: Exception =>
          LOGGER.error("UNEXPECTED_ERROR", error)
          stop()
          setRunningState(RunningState.ERROR)
      }
    }
  }

  override def getStatusDetails(): String = {
    errorMessage
  }

  override def isClusterable(): Boolean = {
    false
  }

}
