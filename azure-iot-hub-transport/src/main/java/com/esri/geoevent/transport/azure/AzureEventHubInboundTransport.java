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

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.concurrent.ExecutionException;

import com.esri.ges.core.component.ComponentException;
import com.esri.ges.core.component.RunningState;
import com.esri.ges.framework.i18n.BundleLogger;
import com.esri.ges.framework.i18n.BundleLoggerFactory;
import com.esri.ges.transport.InboundTransportBase;
import com.esri.ges.transport.TransportDefinition;
import com.microsoft.azure.eventhubs.EventData;
import com.microsoft.azure.eventhubs.EventHubClient;
import com.microsoft.azure.eventhubs.PartitionReceiveHandler;
import com.microsoft.azure.eventhubs.PartitionReceiver;
import com.microsoft.azure.servicebus.ServiceBusException;

public class AzureEventHubInboundTransport extends InboundTransportBase
{
  // logger
  private static final BundleLogger LOGGER                     = BundleLoggerFactory.getLogger(AzureEventHubInboundTransport.class);
  private static final int          MAX_EVENT_COUNT            = 990;

  private String                    eventHubConnectionString   = "";
  private int                       eventHubNumberOfPartitions = 4;
  private PartitionReceiver[]       receivers;
  private EventHubClient[]          ehClients;
  private String                    errorMessage;

  public AzureEventHubInboundTransport(TransportDefinition definition) throws ComponentException
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
      case ERROR:
        return;
      default:
    }
    setRunningState(RunningState.STARTING);
    createReceivers();
  }

  @Override
  public synchronized void stop()
  {
    if (getRunningState() == RunningState.STOPPING)
      return;
    errorMessage = null;
    setRunningState(RunningState.STOPPING);
    cleanup();
    // setErrorMessage(null);
    setRunningState(RunningState.STOPPED);
  }

  protected void cleanup()
  {
    for (PartitionReceiver receiver : receivers)
    {
      if (receiver != null)
      {
        try
        {
          receiver.closeSync();
        }
        catch (Exception e)
        {
          LOGGER.warn("CLEANUP_ERROR", e);
        }
        receiver = null;
      }
    }
    for (EventHubClient ehClient : ehClients)
    {
      if (ehClient != null)
      {
        try
        {
          ehClient.closeSync();
        }
        catch (ServiceBusException e)
        {
          LOGGER.warn("CLEANUP_ERROR", e);
        }
        ehClient = null;
      }
    }
    LOGGER.debug("CLEANUP_COMPLETE");
  }

  @Override
  public void validate()
  {
    // TODO: Validate
  }

  @Override
  public synchronized boolean isRunning()
  {
    return (getRunningState() == RunningState.STARTED);
  }

  public void readProperties() throws Exception
  {
    try
    {
      eventHubConnectionString = getProperty(AzureEventHubInboundTransportDefinition.EVENT_HUB_CONNECTION_STRING_PROPERTY_NAME).getValueAsString();
      eventHubNumberOfPartitions = Integer.parseInt(getProperty(AzureEventHubInboundTransportDefinition.EVENT_HUB_NUMBER_OF_PARTITION_PROPERTY_NAME).getValueAsString());
    }
    catch (Exception e)
    {
      errorMessage = LOGGER.translate("ERROR_READING_PROPS");
      LOGGER.error("ERROR_READING_PROPS", e);
    }
  }

  private void createReceivers()
  {
    try
    {
      errorMessage = null;
      readProperties();

      receivers = new PartitionReceiver[eventHubNumberOfPartitions];
      ehClients = new EventHubClient[eventHubNumberOfPartitions];

      for (int partitionId = 0; partitionId < eventHubNumberOfPartitions; partitionId++)
      {
        createEventHubClient(partitionId);
        createPartitionReceiver(partitionId);
      }

      LOGGER.debug("RECEVERS_CREATED");
      setRunningState(RunningState.STARTED);
    }
    catch (Exception error)
    {
      errorMessage = LOGGER.translate("CREATE_EVENT_HUB_RECEIVER_ERROR", error.getMessage());
      LOGGER.error(errorMessage, error);
      setRunningState(RunningState.ERROR);
      cleanup();
    }
  }

  private void createEventHubClient(int partitionId) throws InterruptedException, ExecutionException, ServiceBusException, IOException
  {
    EventHubClient ehClient = EventHubClient.createFromConnectionString(eventHubConnectionString).get();
    if (ehClient != null)
    {
      ehClients[partitionId] = ehClient;
      LOGGER.debug("CREATED_EVENT_HUB_CLIENT", partitionId);
    }
  }

  private void createPartitionReceiver(int partitionId) throws Exception
  {
    EventHubClient ehClient = ehClients[partitionId];
    PartitionReceiver receiver = ehClient.createReceiver(EventHubClient.DEFAULT_CONSUMER_GROUP_NAME, Integer.toString(partitionId), Instant.now()).get();
    if (receiver != null)
    {
      receiver.setReceiveHandler(new EventHandler(MAX_EVENT_COUNT, partitionId));
      receivers[partitionId] = receiver;
      LOGGER.debug("CREATED_CLIENT_RECEIVER", partitionId);
    }
    else
    {
      throw new Exception(LOGGER.translate("UNABLE_TO_CREATE_RECEIVER", partitionId));
    }
  }

  private void receive(byte[] bytes)
  {
    if (bytes != null && bytes.length > 0)
    {
      String str = new String(bytes);
      str = str + '\n';
      byte[] newBytes = str.getBytes();

      ByteBuffer bb = ByteBuffer.allocate(newBytes.length);
      try
      {
        bb.put(newBytes);
        bb.flip();
        byteListener.receive(bb, "");
        bb.clear();
      }
      catch (BufferOverflowException boe)
      {
        LOGGER.error("BUFFER_OVERFLOW_ERROR", boe);
        bb.clear();
        setRunningState(RunningState.ERROR);
      }
      catch (Exception e)
      {
        LOGGER.error("UNEXPECTED_ERROR", e);
        stop();
        setRunningState(RunningState.ERROR);
      }
    }
  }

  public final class EventHandler extends PartitionReceiveHandler
  {
    private int partitionId;

    public EventHandler(final int maxEventCount, final int partitionId)
    {
      super(maxEventCount);
      this.partitionId = partitionId;
    }

    @Override
    public void onReceive(Iterable<EventData> events)
    {
      for (EventData event : events)
      {
        // String message = new String(event.getBody(),
        // Charset.defaultCharset());
        receive(event.getBody());
      }
    }

    @Override
    public void onError(Throwable error)
    {
      LOGGER.warn("EVENT_HUB_RECEIVER_ERROR", error);
      try
      {
        // TODO - recreate partition receiver with partitionId?
        //createPartitionReceiver(partitionId);
      }
      catch (Exception ignored)
      {
      }
    }
  }

  @Override
  public String getStatusDetails()
  {
    return errorMessage;
  }

  @Override
  public boolean isClusterable()
  {
    return false;
  }

}
