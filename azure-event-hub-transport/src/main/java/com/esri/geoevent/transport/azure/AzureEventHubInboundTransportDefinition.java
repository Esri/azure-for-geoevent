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

package com.esri.geoevent.transport.azure;

import com.esri.ges.core.property.PropertyDefinition;
import com.esri.ges.core.property.PropertyException;
import com.esri.ges.core.property.PropertyType;
import com.esri.ges.framework.i18n.BundleLogger;
import com.esri.ges.framework.i18n.BundleLoggerFactory;
import com.esri.ges.transport.TransportDefinitionBase;
import com.esri.ges.transport.TransportType;
import com.microsoft.azure.eventhubs.EventHubClient;

public class AzureEventHubInboundTransportDefinition extends TransportDefinitionBase {
  // logger
  private static final BundleLogger LOGGER = BundleLoggerFactory.getLogger(AzureEventHubInboundTransportDefinition.class);

  // property names
  public static final String EVENT_HUB_NAME_PROPERTY_NAME = "eventHubName";
  public static final String EVENT_HUB_CONSUMER_GROUP_NAME_PROPERTY_NAME = "consumerGroupName";
  public static final String PROVIDE_EVENT_HUB_CONNECTION_STRING_PROPERTY_NAME = "provideEventHubConnectionString";
  public static final String EVENT_HUB_CONNECTION_STRING_PROPERTY_NAME = "eventHubConnectionString";
  public static final String EVENT_HUB_ENDPOINT_PROPERTY_NAME = "eventHubEndpoint";
  public static final String EVENT_HUB_ACCESS_POLICY_PROPERTY_NAME = "eventHubAccessPolicy";
  public static final String EVENT_HUB_ACCESS_KEY_PROPERTY_NAME = "eventHubAccessKey";
  public static final String STORAGE_CONNECTION_STRING_PROPERTY_NAME = "storageConnectionString";

  // defaults
  public static final String DEFAULT_CONSUMER_GROUP_NAME = EventHubClient.DEFAULT_CONSUMER_GROUP_NAME;
  public static final String DEFAULT_EVENT_HUB_ACCESS_POLICY = "service";

  public AzureEventHubInboundTransportDefinition() {
    super(TransportType.INBOUND);
    try {
      propertyDefinitions.put(EVENT_HUB_NAME_PROPERTY_NAME, new PropertyDefinition(EVENT_HUB_NAME_PROPERTY_NAME, PropertyType.String, null, "${com.esri.geoevent.transport.azure-event-hub-transport.EVENT_HUB_PATH_LBL}", "${com.esri.geoevent.transport.azure-event-hub-transport.EVENT_HUB_NAME_DESC}", true, false));
      propertyDefinitions.put(EVENT_HUB_CONSUMER_GROUP_NAME_PROPERTY_NAME, new PropertyDefinition(EVENT_HUB_CONSUMER_GROUP_NAME_PROPERTY_NAME, PropertyType.String, DEFAULT_CONSUMER_GROUP_NAME, "${com.esri.geoevent.transport.azure-event-hub-transport.EVENT_HUB_CONSUMER_GROUP_NAME_LBL}", "${com.esri.geoevent.transport.azure-event-hub-transport.EVENT_HUB_CONSUMER_GROUP_NAME_DESC}", true, false));
      propertyDefinitions.put(PROVIDE_EVENT_HUB_CONNECTION_STRING_PROPERTY_NAME, new PropertyDefinition(PROVIDE_EVENT_HUB_CONNECTION_STRING_PROPERTY_NAME, PropertyType.Boolean, new Boolean(false), "${com.esri.geoevent.transport.azure-event-hub-transportPROVIDE_EVENT_HUB_CONNECTION_STRING_LBL}", "${com.esri.geoevent.transport.azure-event-hub-transport.PROVIDE_EVENT_HUB_CONNECTION_STRING_DESC}", true, false));
      propertyDefinitions.put(EVENT_HUB_CONNECTION_STRING_PROPERTY_NAME, new PropertyDefinition(EVENT_HUB_CONNECTION_STRING_PROPERTY_NAME, PropertyType.String, null, "${com.esri.geoevent.transport.azure-event-hub-transport.EVENT_HUB_CONNECTION_STRING_LBL}", "${com.esri.geoevent.transport.azure-event-hub-transport.EVENT_HUB_CONNECTION_STRING_DESC}", "provideEventHubConnectionString=true", false, false));
      propertyDefinitions.put(EVENT_HUB_ENDPOINT_PROPERTY_NAME, new PropertyDefinition(EVENT_HUB_ENDPOINT_PROPERTY_NAME, PropertyType.String, null, "${com.esri.geoevent.transport.azure-event-hub-transport.EVENT_HUB_ENDPOINT_LBL}", "${com.esri.geoevent.transport.azure-event-hub-transport.EVENT_HUB_ENDPOINT_DESC}", "provideEventHubConnectionString=false", false, false));
      propertyDefinitions.put(EVENT_HUB_ACCESS_POLICY_PROPERTY_NAME, new PropertyDefinition(EVENT_HUB_ACCESS_POLICY_PROPERTY_NAME, PropertyType.String, DEFAULT_EVENT_HUB_ACCESS_POLICY, "${com.esri.geoevent.transport.azure-event-hub-transport.EVENT_HUB_ACCESS_POLICY_LBL}", "${com.esri.geoevent.transport.azure-event-hub-transport.EVENT_HUB_ACCESS_POLICY_DESC}", "provideEventHubConnectionString=false", false, false));
      propertyDefinitions.put(EVENT_HUB_ACCESS_KEY_PROPERTY_NAME, new PropertyDefinition(EVENT_HUB_ACCESS_KEY_PROPERTY_NAME, PropertyType.String, null, "${com.esri.geoevent.transport.azure-event-hub-transport.EVENT_HUB_ACCESS_KEY_LBL}", "${com.esri.geoevent.transport.azure-event-hub-transport.EVENT_HUB_ACCESS_KEY_DESC}", "provideEventHubConnectionString=false", false, false));
      propertyDefinitions.put(STORAGE_CONNECTION_STRING_PROPERTY_NAME, new PropertyDefinition(STORAGE_CONNECTION_STRING_PROPERTY_NAME, PropertyType.String, null, "${com.esri.geoevent.transport.azure-event-hub-transport.STORAGE_CONNECTION_STRING_LBL}", "${com.esri.geoevent.transport.azure-event-hub-transport.STORAGE_CONNECTION_STRING_DESC}", true, false));
    } catch (PropertyException error) {
      LOGGER.error("ERROR_LOADING_TRANSPORT_DEFINITION", error);
      throw new RuntimeException(error);
    }
  }

  @Override
  public String getName() {
    return "azure-event-hub-in";
  }

  @Override
  public String getLabel() {
    return "${com.esri.geoevent.transport.azure-event-hub-transport.TRANSPORT_IN_LABEL}";
  }

  @Override
  public String getDomain() {
    return "com.esri.geoevent.transport.inbound";
  }

  @Override
  public String getDescription() {
    return "${com.esri.geoevent.transport.azure-event-hub-transport.TRANSPORT_IN_DESC}";
  }
}
