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

import com.esri.ges.core.property.{PropertyDefinition, PropertyException, PropertyType}
import com.esri.ges.framework.i18n.{BundleLogger, BundleLoggerFactory}
import com.esri.ges.transport.{TransportDefinitionBase, TransportType}
import com.microsoft.azure.eventhubs.EventHubClient

class AzureIoTHubInboundTransportDefinition extends TransportDefinitionBase(TransportType.INBOUND) {

  // logger
  private val LOGGER: BundleLogger = BundleLoggerFactory.getLogger(classOf[AzureIoTHubInboundTransportDefinition])

  try {
    propertyDefinitions.put(AzureIoTHubInboundTransportDefinition.HUB_NAME, new PropertyDefinition(AzureIoTHubInboundTransportDefinition.HUB_NAME, PropertyType.String, null, "${com.esri.geoevent.transport.azure-iot-hub-transport.HUB_NAME_LBL}", "${com.esri.geoevent.transport.azure-iot-hub-transport.HUB_NAME_DESC}", true, false))
    propertyDefinitions.put(AzureIoTHubInboundTransportDefinition.HUB_ENDPOINT, new PropertyDefinition(AzureIoTHubInboundTransportDefinition.HUB_ENDPOINT, PropertyType.String, null, "${com.esri.geoevent.transport.azure-iot-hub-transport.HUB_ENDPOINT_LBL}", "${com.esri.geoevent.transport.azure-iot-hub-transport.HUB_ENDPOINT_DESC}", true, false))
    propertyDefinitions.put(AzureIoTHubInboundTransportDefinition.HUB_PARTITIONS, new PropertyDefinition(AzureIoTHubInboundTransportDefinition.HUB_PARTITIONS, PropertyType.Integer, null, "${com.esri.geoevent.transport.azure-iot-hub-transport.HUB_PARTITIONS_LBL}", "${com.esri.geoevent.transport.azure-iot-hub-transport.HUB_PARTITIONS_DESC}", true, false))
    propertyDefinitions.put(AzureIoTHubInboundTransportDefinition.ACCESS_POLICY, new PropertyDefinition(AzureIoTHubInboundTransportDefinition.ACCESS_POLICY, PropertyType.String, null, "${com.esri.geoevent.transport.azure-iot-hub-transport.ACCESS_POLICY_LBL}", "${com.esri.geoevent.transport.azure-iot-hub-transport.ACCESS_POLICY_DESC}", true, false))
    propertyDefinitions.put(AzureIoTHubInboundTransportDefinition.ACCESS_KEY, new PropertyDefinition(AzureIoTHubInboundTransportDefinition.ACCESS_KEY, PropertyType.String, null, "${com.esri.geoevent.transport.azure-iot-hub-transport.ACCESS_KEY_LBL}", "${com.esri.geoevent.transport.azure-iot-hub-transport.ACCESS_KEY_DESC}", true, false))
  } catch {
    case error: PropertyException =>
      LOGGER.error("ERROR_LOADING_TRANSPORT_DEFINITION", error)
      throw new RuntimeException(error)
  }

  override def getName(): String = "azure-iot-hub-in"

  override def getLabel(): String = "${com.esri.geoevent.transport.azure-iot-hub-transport.TRANSPORT_IN_LABEL}"

  override def getDomain(): String = "com.esri.geoevent.transport.inbound"

  override def getDescription(): String = "${com.esri.geoevent.transport.azure-iot-hub-transport.TRANSPORT_IN_DESC}"

}

object AzureIoTHubInboundTransportDefinition {
  // property names
  val HUB_NAME = "hubName"
  val HUB_ENDPOINT = "hubEndpoint"
  val HUB_PARTITIONS = "hubPartitions"
  val ACCESS_POLICY = "accessPolicy"
  val ACCESS_KEY = "accessKey"
}