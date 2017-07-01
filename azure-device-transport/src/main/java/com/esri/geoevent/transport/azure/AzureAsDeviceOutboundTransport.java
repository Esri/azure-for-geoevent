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

import com.esri.ges.core.component.ComponentException;
import com.esri.ges.core.component.RunningState;
import com.esri.ges.core.geoevent.GeoEvent;
import com.esri.ges.framework.i18n.BundleLogger;
import com.esri.ges.framework.i18n.BundleLoggerFactory;
import com.esri.ges.transport.GeoEventAwareTransport;
import com.esri.ges.transport.OutboundTransportBase;
import com.esri.ges.transport.TransportDefinition;
import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.IotHubEventCallback;
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;
import com.microsoft.azure.sdk.iot.device.Message;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;


public class AzureAsDeviceOutboundTransport extends OutboundTransportBase implements GeoEventAwareTransport, IotHubEventCallback {
  // logger
  private static final BundleLogger LOGGER = BundleLoggerFactory.getLogger(AzureAsDeviceOutboundTransport.class);

  // connection properties
  private String connectionString = "";
  private IotHubClientProtocol connectionProtocol = IotHubClientProtocol.valueOf(AzureAsDeviceOutboundTransportDefinition.DEFAULT_CONNECTION_PROTOCOL);

  private volatile boolean propertiesNeedUpdating = false;

  // device id client and receiver
  private DeviceClient deviceClient = null;

  public AzureAsDeviceOutboundTransport(TransportDefinition definition) throws ComponentException {
    super(definition);
  }

  @Override
  public synchronized void start() {
    switch (getRunningState()) {
      case STARTING:
      case STARTED:
        return;
      default:
    }

    setRunningState(RunningState.STARTING);
    setup();
  }

  @Override
  public void execute(IotHubStatusCode responseStatus, Object callbackContext)
  {
    // IotHubEventCallback - message sent callback with a feedback response status from the IotHub
    // TODO ...
  }

  public void readProperties() {
    try {
      boolean somethingChanged = false;

      // Connection String
      if (hasProperty(AzureAsDeviceOutboundTransportDefinition.CONNECTION_STRING_PROPERTY_NAME)) {
        String newConnectionString = getProperty(AzureAsDeviceOutboundTransportDefinition.CONNECTION_STRING_PROPERTY_NAME).getValueAsString();
        if (!connectionString.equals(newConnectionString)) {
          connectionString = newConnectionString;
          somethingChanged = true;
        }
      }
      // Connection Protocol
      if (hasProperty(AzureAsDeviceOutboundTransportDefinition.CONNECTION_PROTOCOL_PROPERTY_NAME)) {
        String newConnectionProtocolStr = getProperty(AzureAsDeviceOutboundTransportDefinition.CONNECTION_PROTOCOL_PROPERTY_NAME).getValueAsString();
        IotHubClientProtocol newConnectionProtocol = IotHubClientProtocol.valueOf(newConnectionProtocolStr);
        if (!connectionProtocol.equals(newConnectionProtocol)) {
          connectionProtocol = newConnectionProtocol;
          somethingChanged = true;
        }
      }
      propertiesNeedUpdating = somethingChanged;
    } catch (Exception ex) {
      LOGGER.error("INIT_ERROR", ex.getMessage());
      LOGGER.info(ex.getMessage(), ex);
      setErrorMessage(ex.getMessage());
      setRunningState(RunningState.ERROR);
    }
  }

  public synchronized void setup() {
    String errorMessage = null;
    RunningState runningState = RunningState.STARTED;

    try {
      readProperties();
      if (propertiesNeedUpdating) {
        cleanup();
        propertiesNeedUpdating = false;
      }

      createDeviceClient();

      setErrorMessage(errorMessage);
      setRunningState(runningState);
    } catch (Exception error) {
      LOGGER.error("INIT_ERROR", error.getMessage());
      LOGGER.info(error.getMessage(), error);
      setErrorMessage(error.getMessage());
      setRunningState(RunningState.ERROR);
    }
  }

  private void createDeviceClient() throws IOException, URISyntaxException {
    closeDeviceClient();
    deviceClient = new DeviceClient(connectionString, connectionProtocol);
    deviceClient.open();
  }

  private void closeDeviceClient() {
    // clean up the service client
    if (deviceClient != null) {
      try {
        deviceClient.close();
      } catch (Exception error) {
        // ignored
      }
      deviceClient = null;
    }
  }

  protected void cleanup() {
    closeDeviceClient();
  }

  @Override
  public void receive(ByteBuffer buffer, String channelId) {
    receive(buffer, channelId, null);
  }

  @Override
  public void receive(ByteBuffer buffer, String channelId, GeoEvent geoEvent) {
    if (isRunning()) {
      if (geoEvent == null)
        return;

      try {
        // Send Event as a Device
        String messageStr = new String(buffer.array(), StandardCharsets.UTF_8);
        Message message = new Message(messageStr);
        deviceClient.sendEventAsync(message, this, 1);
      } catch (Exception e) {
        //LOGGER.warn("FAILED_TO_SEND_INVALID_DEVICE_ID", deviceIdFieldName);
        setErrorMessage(e.getMessage());
        LOGGER.error(e.getMessage(), e);
        setRunningState(RunningState.ERROR);
      }
    } else {
      LOGGER.debug("RECEIVED_BUFFER_WHEN_STOPPED", "");
    }
  }

}
