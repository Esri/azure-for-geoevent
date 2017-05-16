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
import com.esri.ges.framework.i18n.BundleLogger;
import com.esri.ges.framework.i18n.BundleLoggerFactory;
import com.esri.ges.transport.InboundTransportBase;
import com.esri.ges.transport.TransportDefinition;
import com.microsoft.azure.eventhubs.EventData;
import com.microsoft.azure.eventhubs.EventHubClient;
import com.microsoft.azure.eventprocessorhost.*;
import com.microsoft.azure.servicebus.ConnectionStringBuilder;

import java.net.URI;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class AzureEventHubInboundTransport extends InboundTransportBase {
  // based on the microsoft's azure-eventhubs-eph API:
  // https://azure.microsoft.com/en-us/documentation/articles/event-hubs-java-ephjava-getstarted/#receive-messages-with-eventprocessorhost-in-java
  // https://github.com/Azure/azure-event-hubs-java/tree/e6413d059471d75224a62df89251c5dfd331f5e2/azure-eventhubs-eph
  // https://github.com/Azure/azure-event-hubs/tree/master/java

  // logger
  private static final BundleLogger LOGGER = BundleLoggerFactory.getLogger(AzureEventHubInboundTransport.class);

  private String eventHubName = ""; // e.g. "hkiot1"
  private String eventHubConsumerGroupName = EventHubClient.DEFAULT_CONSUMER_GROUP_NAME;
  private boolean provideEventHubConnectionString = false;
  private String eventHubConnectionString = "";
  private String eventHubEndpoint = "";  // e.g. // "sb://iothub-ns-hkiot1-144429-38bfd49a46.servicebus.windows.net/";
  private String eventHubAccessPolicy = "";  // sasKeyName
  private String eventHubAccessKey = "";  // sasKey

  private String storageConnectionString = "";

  private EventProcessor eventProcessor = null;
  private EventProcessorFactory eventProcessorFactory = null;
  private EventProcessorHost host = null;
  private String errorMessage = null;

  public AzureEventHubInboundTransport(TransportDefinition definition) throws ComponentException {
    super(definition);
    eventProcessor = new EventProcessor();
    eventProcessorFactory = new EventProcessorFactory();
  }

  @Override
  public synchronized void start() {
    switch (getRunningState()) {
      case STARTING:
      case STARTED:
      case ERROR:
        return;
      default:
    }
    setRunningState(RunningState.STARTING);
    createNewHost();
  }

  @Override
  public synchronized void stop() {
    if (getRunningState() == RunningState.STOPPING)
      return;

    errorMessage = null;
    setRunningState(RunningState.STOPPING);
    cleanup(false);
    // setErrorMessage(null);
    setRunningState(RunningState.STOPPED);
  }

  protected void cleanup(boolean completeProcessShutDown) {
    if (host != null) {
      try {
        host.unregisterEventProcessor();
      } catch (Exception ignored) {
      }
      host = null;

      if (completeProcessShutDown) {
        try {
          EventProcessorHost.forceExecutorShutdown(120);
        } catch (Exception ignored) {
        }
      }
    }
  }

  @Override
  public void validate() {
    // TODO: Validate
  }

  @Override
  public synchronized boolean isRunning() {
    return (getRunningState() == RunningState.STARTED);
  }

  public void readProperties() throws Exception {
    try {
      eventHubName = getProperty(AzureEventHubInboundTransportDefinition.EVENT_HUB_NAME_PROPERTY_NAME).getValueAsString();
      eventHubConsumerGroupName = getProperty(AzureEventHubInboundTransportDefinition.EVENT_HUB_CONSUMER_GROUP_NAME_PROPERTY_NAME).getValueAsString();
      provideEventHubConnectionString = ((Boolean) (getProperty(AzureEventHubInboundTransportDefinition.PROVIDE_EVENT_HUB_CONNECTION_STRING_PROPERTY_NAME).getValue()));
      eventHubConnectionString = getProperty(AzureEventHubInboundTransportDefinition.EVENT_HUB_CONNECTION_STRING_PROPERTY_NAME).getValueAsString();
      eventHubEndpoint = getProperty(AzureEventHubInboundTransportDefinition.EVENT_HUB_ENDPOINT_PROPERTY_NAME).getValueAsString();
      eventHubAccessPolicy = getProperty(AzureEventHubInboundTransportDefinition.EVENT_HUB_ACCESS_POLICY_PROPERTY_NAME).getValueAsString();
      eventHubAccessKey = getProperty(AzureEventHubInboundTransportDefinition.EVENT_HUB_ACCESS_KEY_PROPERTY_NAME).getValueAsString();
      storageConnectionString = getProperty(AzureEventHubInboundTransportDefinition.STORAGE_CONNECTION_STRING_PROPERTY_NAME).getValueAsString();
    } catch (Exception e) {
      errorMessage = LOGGER.translate("ERROR_READING_PROPS");
      LOGGER.error("ERROR_READING_PROPS", e);
    }
  }

  private void createNewHost() {
    try {
      cleanup(false);

      errorMessage = null;
      readProperties();

      if (!provideEventHubConnectionString) {
        URI eventHubEndpointUri = new URI(eventHubEndpoint);
        ConnectionStringBuilder builder = new ConnectionStringBuilder(eventHubEndpointUri, eventHubName, eventHubAccessPolicy, eventHubAccessKey);
        eventHubConnectionString = builder.toString();
      }

      host = new EventProcessorHost(eventHubName, eventHubConsumerGroupName, eventHubConnectionString, storageConnectionString);
      EventProcessorOptions options = EventProcessorOptions.getDefaultOptions();
      options.setExceptionNotification(new ErrorNotificationHandler());
      options.setInitialOffsetProvider((partitionId) -> {
        return Instant.now();
      });
      host.registerEventProcessorFactory(eventProcessorFactory, options).get();

      setRunningState(RunningState.STARTED);
    } catch (Exception error) {
      String errorMsg = "";
      // System.out.print("Failure while registering: ");
      if (error instanceof ExecutionException) {
        Throwable inner = error.getCause();
        errorMsg = inner.toString();
      } else {
        errorMsg = error.toString(); // error.getMessage()
      }
      this.errorMessage = LOGGER.translate("CREATE_EVENT_HUB_RECEIVER_ERROR", errorMsg);
      setRunningState(RunningState.ERROR);
    }
  }

  private String buildConnectionStringFromNamespace(URI eventHubEndpointUri) {
    // build the eventHubNamespaceName
    String eventHubNamespaceName = eventHubEndpointUri.getHost();
    eventHubNamespaceName = eventHubNamespaceName.substring(0, eventHubNamespaceName.lastIndexOf(".servicebus.windows.net"));

    ConnectionStringBuilder builder = new ConnectionStringBuilder(eventHubNamespaceName, eventHubName, eventHubAccessPolicy, eventHubAccessKey);
    return builder.toString();
  }

  private void receive(byte[] bytes) {
    if (bytes != null && bytes.length > 0) {
      String str = new String(bytes);
      str = str + '\n';
      byte[] newBytes = str.getBytes();

      ByteBuffer bb = ByteBuffer.allocate(newBytes.length);
      try {
        bb.put(newBytes);
        bb.flip();
        byteListener.receive(bb, "");
        bb.clear();
      } catch (BufferOverflowException boe) {
        LOGGER.error("BUFFER_OVERFLOW_ERROR", boe);
        bb.clear();
        setRunningState(RunningState.ERROR);
      } catch (Exception e) {
        LOGGER.error("UNEXPECTED_ERROR", e);
        stop();
        setRunningState(RunningState.ERROR);
      }
    }
  }

  @Override
  public String getStatusDetails() {
    return errorMessage;
  }

  @Override
  public boolean isClusterable() {
    return false;
  }

  public final class EventProcessor implements IEventProcessor {
    @Override
    public void onOpen(PartitionContext context) throws Exception {
      String message = "Partition " + context.getPartitionId() + " is opening";
      // System.out.println(message);
      // TODO - localize
      LOGGER.info(message);
    }

    @Override
    public void onClose(PartitionContext context, CloseReason reason) throws Exception {
      String message = "Partition " + context.getPartitionId() + " is closing for reason " + reason.toString();
      // System.out.println(message);
      // TODO - localize
      LOGGER.info(message);
    }

    @Override
    public void onError(PartitionContext context, Throwable error) {
      // String message = "Partition " + context.getPartitionId() + " got error
      // " + error.toString();
      // System.out.println(message);
      LOGGER.warn("EVENT_HUB_RECEIVER_ERROR", error);
      LOGGER.error(errorMessage, error);
      // setRunningState(RunningState.ERROR);
      // cleanup(false);
    }

    @Override
    public void onEvents(PartitionContext context, Iterable<EventData> events) throws Exception {
      if (events == null)
        return;

      for (EventData event : events) {
        // String message = new String(event.getBytes(),
        receive(event.getBytes());
        // context.checkpoint(event);
      }
    }
  }

  public final class EventProcessorFactory implements IEventProcessorFactory<EventProcessor> {
    @Override
    public EventProcessor createEventProcessor(PartitionContext context) throws Exception {
      return eventProcessor;
    }
  }

  public final class ErrorNotificationHandler implements Consumer<ExceptionReceivedEventArgs> {
    @Override
    public void accept(ExceptionReceivedEventArgs error) {
      LOGGER.warn("EVENT_HUB_RECEIVER_ERROR", error);
      LOGGER.error(errorMessage, error);
    }
  }

}
