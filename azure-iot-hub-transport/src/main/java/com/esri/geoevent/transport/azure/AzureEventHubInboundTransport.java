/*
  Copyright 1995-2015 Esri

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
import java.util.ArrayList;
import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.apache.qpid.amqp_1_0.jms.BytesMessage;
import org.apache.qpid.amqp_1_0.jms.impl.ConnectionFactoryImpl;

import com.esri.ges.core.component.ComponentException;
import com.esri.ges.core.component.RunningState;
import com.esri.ges.framework.i18n.BundleLogger;
import com.esri.ges.framework.i18n.BundleLoggerFactory;
import com.esri.ges.transport.InboundTransportBase;
import com.esri.ges.transport.TransportDefinition;

public class AzureEventHubInboundTransport extends InboundTransportBase implements MessageListener
{
	// logger
	private static final BundleLogger LOGGER = BundleLoggerFactory.getLogger(AzureEventHubInboundTransport.class);

	// connection properties
	private String	connectionUri				= "";
	private String	eventHubName				= "";
	private int			numberOfPartitions	= 4;

	private volatile boolean			propertiesNeedUpdating	= false;
	private String								errorMessage;
	private Connection						connection							= null;
	private Session								session									= null;
	private List<MessageConsumer>	consumers								= new ArrayList<MessageConsumer>();

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
				return;
			default:
		}
		setup();
	}

	@Override
	public synchronized void stop()
	{
		if (getRunningState() == RunningState.STOPPING)
			return;

		cleanup();
		setErrorMessage(null);
		errorMessage = null;
		setRunningState(RunningState.STOPPED);
	}

	public void readProperties()
	{
		try
		{
			// get the properties from the connector properties
			boolean somethingChanged = false;
			if (hasProperty(AzureEventHubInboundTransportDefinition.CONNECTION_URI_PROPERTY_NAME))
			{
				String newConnectionUri = getProperty(AzureEventHubInboundTransportDefinition.CONNECTION_URI_PROPERTY_NAME).getValueAsString();
				if (!connectionUri.equals(newConnectionUri))
				{
					connectionUri = newConnectionUri;
					somethingChanged = true;
				}
			}
			if (hasProperty(AzureEventHubInboundTransportDefinition.EVENT_HUB_NAME_PROPERTY_NAME))
			{
				String newQueueName = getProperty(AzureEventHubInboundTransportDefinition.EVENT_HUB_NAME_PROPERTY_NAME).getValueAsString();
				if (!eventHubName.equals(newQueueName))
				{
					eventHubName = newQueueName;
					somethingChanged = true;
				}
			}
			if (hasProperty(AzureEventHubInboundTransportDefinition.NUMBER_OF_PARTITIONS_PROPERTY_NAME))
			{
				String numberOfPartitionsStr = getProperty(AzureEventHubInboundTransportDefinition.NUMBER_OF_PARTITIONS_PROPERTY_NAME).getValueAsString();
				int newNumberOfPartitions = 4;
				try
				{
					newNumberOfPartitions = Integer.parseInt(numberOfPartitionsStr);
				}
				catch (Exception error)
				{
				}

				if (numberOfPartitions != newNumberOfPartitions && newNumberOfPartitions > 0)
				{
					numberOfPartitions = newNumberOfPartitions;
					somethingChanged = true;
				}
			}
			propertiesNeedUpdating = somethingChanged;
		}
		catch (Exception ex)
		{
			LOGGER.error("READ_PROPERTIES_ERROR", ex.getMessage());
			LOGGER.info(ex.getMessage(), ex);
			errorMessage = ex.getMessage();
			setErrorMessage(ex.getMessage());
			setRunningState(RunningState.ERROR);
		}
	}

	public boolean setup()
	{
		// read the properties
		readProperties();

		ConnectionFactory factory = null;
		try
		{
			factory = ConnectionFactoryImpl.createFromURL(connectionUri);
			connection = factory.createConnection();
			connection.start();

			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			for (int i = 0; i < numberOfPartitions; i++)
			{
				String queueName = eventHubName + i;
				Destination destination = session.createQueue(queueName);
				MessageConsumer consumer = session.createConsumer(destination);
				consumer.setMessageListener(this);
				consumers.add(consumer);
			}

			// reset the error message
			setErrorMessage(null);
			errorMessage = null;
			setRunningState(RunningState.STARTED);
		}
		catch (Exception ex)
		{
			LOGGER.error("SETUP_ERROR", ex.getMessage());
			LOGGER.info(ex.getMessage(), ex);
			setErrorMessage(ex.getMessage());
			errorMessage = ex.getMessage();
			setRunningState(RunningState.ERROR);
			return false;
		}
		return true;
	}

	protected void cleanup()
	{
		for (MessageConsumer consumer : consumers)
		{
			try
			{
				consumer.close();
			}
			catch (Exception ignored)
			{
			}
		}
		if (session != null)
		{
			try
			{
				session.close();
			}
			catch (Exception ignored)
			{
			}
		}
		if (connection != null)
		{
			try
			{
				connection.close();
			}
			catch (Exception ignored)
			{
			}
		}
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

	public void applyProperties() throws Exception
	{
		validate();
		cleanup();
		setup();
	}

	@Override
	public void afterPropertiesSet()
	{
		try
		{
			readProperties();
		}
		catch (Exception e)
		{
			setRunningState(RunningState.ERROR);

			errorMessage = LOGGER.translate("ERROR_APPLYING_PROPS", e);
			setRunningState(RunningState.ERROR);
			setErrorMessage(errorMessage);
			LOGGER.error("ERROR_APPLYING_PROPS", e);
			return;
		}
	}

	@Override
	public String getStatusDetails()
	{
		return errorMessage;
	}

	@Override
	public void onMessage(Message message)
	{
		try
		{
			if (message instanceof BytesMessage)
			{
				// read the message body
				BytesMessage byteMessage = (BytesMessage) message;
				byte[] data = new byte[(int) byteMessage.getBodyLength()];
				byteMessage.readBytes(data);
				byteMessage.reset();

				String deviceId = byteMessage.getStringProperty("iothub-connection-device-id");

				// parse out the message to string
				String messageAsString = new String(data, StandardCharsets.UTF_8);
				LOGGER.debug("MSG_RECEIVED_DEBUG", messageAsString);

				// send the message to adapter
				ByteBuffer buffer = ByteBuffer.wrap(data);
				byteListener.receive(buffer, null);
			}
		}
		catch (Exception error)
		{
			LOGGER.error("ERROR_READING_MSG", error);
		}
	}
}
