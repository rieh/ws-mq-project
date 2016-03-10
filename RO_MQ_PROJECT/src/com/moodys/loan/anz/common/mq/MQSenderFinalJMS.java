/*
 * Copyright (c) 2016 Moody's Analytics, LLC. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of Moody's Analytics, LLC.
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of
 * the license agreement you entered into with Moody's Analytics.
 *  
 * Creat Date : Feb 17, 2016 5:15:57 PM
 */
package com.moodys.loan.anz.common.mq;

import java.util.HashMap;
import java.util.Iterator;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.net.ssl.SSLSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;

/**
 * ClassName: MQSender <br/>
 * Function: TODO Add Function for this Class here. <br/>
 * Date: Feb 17, 2016 5:15:57 PM <br/>
 * 
 * @author xuxiao
 * @version $Revision:$
 * @change $Change:$
 * @lastestModifier $Author:$
 */
public class MQSenderFinalJMS {
    private static final Logger logger = LoggerFactory.getLogger(MQSenderFinalJMS.class);

    public MQStatus send(String strMessage, MQConfiguration config) {
        MQStatus status = new MQStatus(-1, "init");
        JmsFactoryFactory ff = null;
        JmsConnectionFactory cf = null;
        Connection connection = null;
        Session session = null;
        Destination destination = null;
        MessageProducer producer = null;
        TextMessage message = null;
        StringBuilder errMsg = new StringBuilder();
        int MAX_TRY_TIMES = config.getMqRetryTimes();
        //Config Parameters
        String hostName = null;
        int port;
        String channelName = null;
        String queueManagerName = null;
        String destinationName = null;
        boolean isTopic = false;
        
      //Assign trustStore and KeyStore to application
        System.setProperty("javax.net.ssl.trustStore", "C://Projects/RO_MQ/jmsTrustStore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "123456");
        System.setProperty("javax.net.ssl.keyStore", "C://Projects/RO_MQ/jmsKeyStore.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");

        try {
            ff = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
            cf = ff.createConnectionFactory();
        } catch (JMSException e1) {
            errMsg.append("Create Connection Factory failed, Error message : " + e1).append("\n");
            logger.error(errMsg.toString());
            status.setStatusCode(-2);
            status.setReturnMsg(errMsg.toString());
            return status;
        }

        Iterator<HashMap<String, Object>> iter = config.getGateways().iterator();
        int size = config.getGateways().size();
        HashMap<String, Object> gateway;
        int gatewayTag = 0;
        String gatewayStr = null;
        while (iter.hasNext()) {
            gatewayTag++;
            gateway = (HashMap<String, Object>) iter.next();
            int tryTimes = 1;
            hostName =  gateway.get(MQConfiguration.MQ_HOST).toString();
            port = (Integer) gateway.get(MQConfiguration.MQ_PORT);
            channelName = gateway.get(MQConfiguration.MQ_CHANNEL).toString();
            queueManagerName = gateway.get(MQConfiguration.MQ_QUEUE_MANAGER_NAME).toString();
            destinationName = gateway.get(MQConfiguration.MQ_DESTINATION_NAME).toString();
            isTopic = (Boolean) gateway.get(MQConfiguration.MQ_IS_TOPIC);
            try {
                // Set the properties
                cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, hostName);
                cf.setIntProperty(WMQConstants.WMQ_PORT, port);
                cf.setStringProperty(WMQConstants.WMQ_CHANNEL, channelName);
                cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, queueManagerName);
                cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT); 
                gatewayStr = hostName + "(" + port + "):\\\\" + channelName + "\\\\" + destinationName;
                do {
                    try {
                        // Start the connection and send the message
                        logger.info(String.format("Begin to send message on gateway %d [%s], trying times %d ... ", gatewayTag, gatewayStr, tryTimes));
                        connection = cf.createConnection();
                        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                        if (isTopic) {
                            destination = session.createTopic(destinationName);
                        } else {
                            destination = session.createQueue(destinationName);
                        }
                        producer = session.createProducer(destination);
                        message = session.createTextMessage(strMessage);
                        connection.start();
                        producer.send(message);
                        break;
                    } catch (JMSException ex) {
                        if (tryTimes == MAX_TRY_TIMES) {
                            String err = String.format("Failed on Gateway %d [%s], try time %d,  cause is : %s, error message is : %s ",
                                    gatewayTag, gatewayStr, tryTimes, ex.getCause(), ex.getMessage());
                            logger.info(err);
                            throw ex;
                        } else {
                            tryTimes++;
                            continue;
                        }
                    }

                } while (tryTimes <= MAX_TRY_TIMES);
                String successMsg = String.format("message sent successfully on gateway %d [%s], message : \n %s ", gatewayTag, gatewayStr, message);
                logger.info(successMsg);
                errMsg.append(successMsg).append("\n");
                
                if (producer != null) {
                    try {
                        producer.close();
                    } catch (JMSException jmsex) {
                        logger.error("Producer could not be closed.");
                        logger.info(jmsex.getMessage());
                        errMsg.append("Producer could not be closed with error : ").append(jmsex.getMessage()).append("\n");
                    }
                }
                if (session != null) {
                    try {
                        session.close();
                    } catch (JMSException jmsex) {
                        logger.error("Session could not be closed.");
                        logger.info(jmsex.getMessage());
                        errMsg.append("Session could not be closed with error : ").append(jmsex.getMessage()).append("\n");
                    }
                }
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (JMSException jmsex) {
                        logger.error("Connection could not be closed.");
                        logger.info(jmsex.getMessage());
                        errMsg.append("Connection could not be closed with error : ").append(jmsex.getMessage()).append("\n");
                    }
                }
                status.setStatusCode(0);
                status.setReturnMsg(errMsg.toString());
                status.setGateway(gatewayStr);
                status.setTryTimes(tryTimes);
                return status;
            } catch (JMSException jmsex) {

                if (jmsex != null) {
                    errMsg.append(String.format("error ocurred on gateway %d [%s], error message is : \n", gatewayTag, gatewayStr))
                            .append(jmsex.getMessage()).append("\n").append("error cause is :" + jmsex.getCause()).append("\n");
                    logger.info(jmsex.getMessage());
                }
                if (gatewayTag == size) {
                    status.setStatusCode(-1);
                    status.setReturnMsg(errMsg.toString());
                    status.setGateway("gateway" + gatewayTag);
                    status.setTryTimes(tryTimes);
                    if (producer != null) {
                        try {
                            producer.close();
                        } catch (JMSException ex) {
                            logger.error("Producer could not be closed.");
                            logger.info(jmsex.getMessage());
                            errMsg.append("Producer could not be closed with error : ").append(jmsex.getMessage()).append("\n");
                        }
                    }

                    if (session != null) {
                        try {
                            session.close();
                        } catch (JMSException ex) {
                            logger.error("Session could not be closed.");
                            logger.info(jmsex.getMessage());
                            errMsg.append("Session could not be closed with error : ").append(jmsex.getMessage()).append("\n");
                        }
                    }

                    if (connection != null) {
                        try {
                            connection.close();
                        } catch (JMSException ex) {
                            logger.error("Connection could not be closed.");
                            logger.info(jmsex.getMessage());
                            errMsg.append("Connection could not be closed with error : ").append(jmsex.getMessage()).append("\n");
                        }
                    }
                    return status;
                } 
            }
        }
        return status;
    }

}
