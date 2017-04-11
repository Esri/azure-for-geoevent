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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import com.esri.ges.core.component.ComponentException;
import com.esri.ges.core.component.RunningState;
import com.esri.ges.core.geoevent.GeoEvent;
import com.esri.ges.framework.i18n.BundleLogger;
import com.esri.ges.framework.i18n.BundleLoggerFactory;
import com.esri.ges.transport.GeoEventAwareTransport;
import com.esri.ges.transport.OutboundTransportBase;
import com.esri.ges.transport.TransportDefinition;
import com.microsoft.azure.eventhubs.EventData;
import com.microsoft.azure.eventhubs.EventHubClient;

public class AzureEventHubOutboundTransport extends OutboundTransportBase implements GeoEventAwareTransport
{
  // logger
  private static final BundleLogger LOGGER                 = BundleLoggerFactory.getLogger(AzureEventHubInboundTransport.class);

  // connection properties
  private String                    connectionString       = "";

  private volatile boolean          propertiesNeedUpdating = false;

  // event hub client
  EventHubClient                    ehClient               = null;

  public AzureEventHubOutboundTransport(TransportDefinition definition) throws ComponentException
  {
    super(definition);
  }

  @Override
  public synchronized void start()
  {
    switch (getRunningState())
    {
      case STARTING:
      case STARTED:
        return;
      default:
    }

    setRunningState(RunningState.STARTING);
    setup();
  }

  public void readProperties()
  {
    try
    {
      boolean somethingChanged = false;

      // Connection String
      if (hasProperty(AzureEventHubOutboundTransportDefinition.CONNECTION_STRING_PROPERTY_NAME))
      {
        String newConnectionString = getProperty(AzureEventHubOutboundTransportDefinition.CONNECTION_STRING_PROPERTY_NAME).getValueAsString();
        if (!connectionString.equals(newConnectionString))
        {
          connectionString = newConnectionString;
          somethingChanged = true;
        }
      }
      propertiesNeedUpdating = somethingChanged;
    }
    catch (Exception ex)
    {
      LOGGER.error("INIT_ERROR", ex.getMessage());
      LOGGER.info(ex.getMessage(), ex);
      setErrorMessage(ex.getMessage());
      setRunningState(RunningState.ERROR);
    }
  }

  public synchronized void setup()
  {
    String errorMessage = null;
    RunningState runningState = RunningState.STARTED;

    try
    {
      readProperties();
      if (propertiesNeedUpdating)
      {
        cleanup();
        propertiesNeedUpdating = false;
      }

      // setup Event Hub
      ehClient = EventHubClient.createFromConnectionStringSync(connectionString);
      if (ehClient == null)
      {
        runningState = RunningState.ERROR;
        errorMessage = LOGGER.translate("FAILED_TO_CREATE_EH_CLIENT", connectionString);
        LOGGER.error(errorMessage);
      }

      setErrorMessage(errorMessage);
      setRunningState(runningState);
    }
    catch (Exception ex)
    {
      LOGGER.error("INIT_ERROR", ex.getMessage());
      LOGGER.info(ex.getMessage(), ex);
      setErrorMessage(ex.getMessage());
      setRunningState(RunningState.ERROR);
    }
  }

  protected void cleanup()
  {
    // clean up the event hub client
    if (ehClient != null)
    {
      try
      {
        ehClient.close();
      }
      catch (Exception error)
      {
        ;
      }
    }
  }

  @Override
  public void receive(ByteBuffer buffer, String channelId)
  {
    receive(buffer, channelId, null);
  }

  @Override
  public void receive(ByteBuffer buffer, String channelId, GeoEvent geoEvent)
  {
    if (isRunning())
    {
      if (geoEvent == null)
        return;

      try
      {
        // Send Event to an Event Hub
        String message = new String(buffer.array(), StandardCharsets.UTF_8);
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8); // "UTF_8"
        // bytes = buffer.array();
        EventData sendEvent = new EventData(bytes);

        if (ehClient != null)
        {
          ehClient.sendSync(sendEvent);
        }
        else
        {
          LOGGER.warn("FAILED_TO_SEND_INVALID_EH_CONNECTION", connectionString);
        }
      }
      catch (Exception e)
      {
        // streamClient.stop();
        setErrorMessage(e.getMessage());
        LOGGER.error(e.getMessage(), e);
        setRunningState(RunningState.ERROR);
      }
    }
    else
    {
      LOGGER.debug("RECEIVED_BUFFER_WHEN_STOPPED", "");
    }
  }

}
