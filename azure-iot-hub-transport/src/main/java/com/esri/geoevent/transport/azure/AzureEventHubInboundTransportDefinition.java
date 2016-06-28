/*
  Copyright 1995-2016 Esri

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

package com.esri.geoevent.transport.azure;

import com.esri.ges.core.property.PropertyDefinition;
import com.esri.ges.core.property.PropertyException;
import com.esri.ges.core.property.PropertyType;
import com.esri.ges.framework.i18n.BundleLogger;
import com.esri.ges.framework.i18n.BundleLoggerFactory;
import com.esri.ges.transport.TransportDefinitionBase;
import com.esri.ges.transport.TransportType;

public class AzureEventHubInboundTransportDefinition extends TransportDefinitionBase
{
  // logger
  private static final BundleLogger LOGGER                                      = BundleLoggerFactory.getLogger(AzureEventHubInboundTransportDefinition.class);

  // property names
  public static final String        EVENT_HUB_CONNECTION_STRING_PROPERTY_NAME   = "eventHubConnectionString";
  public static final String        EVENT_HUB_NUMBER_OF_PARTITION_PROPERTY_NAME = "eventHubNumberOfPartitions";

  public AzureEventHubInboundTransportDefinition()
  {
    super(TransportType.INBOUND);
    try
    {
      propertyDefinitions.put(EVENT_HUB_CONNECTION_STRING_PROPERTY_NAME, new PropertyDefinition(EVENT_HUB_CONNECTION_STRING_PROPERTY_NAME, PropertyType.String, null, "${com.esri.geoevent.transport.azure-iot-hub-transport.EVENT_HUB_CONNECTION_STRING_LBL}", "${com.esri.geoevent.transport.azure-iot-hub-transport.EVENT_HUB_CONNECTION_STRING_DESC}", true, false));
      propertyDefinitions.put(EVENT_HUB_NUMBER_OF_PARTITION_PROPERTY_NAME, new PropertyDefinition(EVENT_HUB_NUMBER_OF_PARTITION_PROPERTY_NAME, PropertyType.Integer, 4, "${com.esri.geoevent.transport.azure-iot-hub-transport.NUMBER_OF_PARTITIONS_LBL}", "${com.esri.geoevent.transport.azure-iot-hub-transport.NUMBER_OF_PARTITIONS_DESC}", true, false));
    }
    catch (PropertyException error)
    {
      LOGGER.error("ERROR_LOADING_TRANSPORT_DEFINITION", error);
      throw new RuntimeException(error);
    }
  }

  @Override
  public String getName()
  {
    return "AzureIoTHub";
  }

  @Override
  public String getLabel()
  {
    return "${com.esri.geoevent.transport.azure-iot-hub-transport.TRANSPORT_IN_LABEL}";
  }

  @Override
  public String getDomain()
  {
    return "com.esri.geoevent.transport.inbound";
  }

  @Override
  public String getDescription()
  {
    return "${com.esri.geoevent.transport.azure-iot-hub-transport.TRANSPORT_IN_DESC}";
  }
}
