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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.mq.jms.MQQueueConnectionFactory;
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
public class MQSender2 {
    private static final Logger logger = LoggerFactory.getLogger(MQSenderFinalJMS.class);
    private boolean isTopic;
    private JmsFactoryFactory ff;
    private JmsConnectionFactory cf;
    private StringBuffer errMsg;

    public MQStatus send(String strMessage, MQConfiguration config) {
        MQStatus status = new MQStatus(-1,null);
        Connection connection = null;
        Session session = null;
        Destination destination = null;
        MessageProducer producer = null;
        errMsg = new StringBuffer();
        TextMessage message = null;
        
        MQConnectionFactory cf;

        cf = new MQQueueConnectionFactory();
        
        Iterator<HashMap<String, Object>> iter = config.getGateways().iterator();
        int size = config.getGateways().size();
        HashMap<String, Object> gateway;
        int gatewayTag = 0;
        int tryTimes = 1;
        while (iter.hasNext()) {
            gatewayTag++;
            gateway = (HashMap<String, Object>) iter.next();
            try {
                // Set the properties
                cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, gateway.get(MQConfiguration.MQ_HOST).toString());
                cf.setIntProperty(WMQConstants.WMQ_PORT, (Integer) gateway.get(MQConfiguration.MQ_PORT));
                cf.setStringProperty(WMQConstants.WMQ_CHANNEL, gateway.get(MQConfiguration.MQ_CHANNEL).toString());
                cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, gateway.get(MQConfiguration.MQ_QUEUE_MANAGER_NAME).toString());
                cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT); // ////////////////MQ
                                                                                                 // Connection Mode
                isTopic = (Boolean) gateway.get(MQConfiguration.MQ_IS_TOPIC);

                // Start the connection and send the message
                do {
                    try {
                        // Create JMS objects
                        logger.info("try to send message on gateway" + gatewayTag + " times: " + tryTimes);
                        connection = cf.createConnection();
                        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                        if (isTopic) {
                            destination = session.createTopic(gateway.get(MQConfiguration.MQ_DESTINATION_NAME).toString());
                        } else {
                            destination = session.createQueue(gateway.get(MQConfiguration.MQ_DESTINATION_NAME).toString());
                        }
                        producer = session.createProducer(destination);
                        message = session.createTextMessage(strMessage);
                        connection.start();
                        producer.send(message);
                        break;
                    } catch (JMSException ex) {
                        if (tryTimes == 3) {
                            logger.info("fails to send message on gateway" + gatewayTag + "try times: " + tryTimes);
                            throw ex;
                        } else {
                            tryTimes++;
                            continue;
                        }
                    }

                } while (tryTimes < 4);

                logger.info("Sent message on Gateway " + gatewayTag);
                logger.info("Sent message:\n" + message);
                errMsg.append("Sent message on Gateway " + gatewayTag + "\n" + message.toString());
                status.setStatusCode(0);
                status.setReturnMsg(errMsg.toString());
                status.setGateway("gateway" + gatewayTag);
                status.setTryTimes(tryTimes);
                if (producer != null) {
                    try {
                        producer.close();
                    } catch (JMSException jmsex) {
                        logger.error("Producer could not be closed.");
                        recordFailure(jmsex);
                    }
                }
                if (session != null) {
                    try {
                        session.close();
                    } catch (JMSException jmsex) {
                        logger.error("Session could not be closed.");
                        recordFailure(jmsex);
                    }
                }
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (JMSException jmsex) {
                        logger.error("Connection could not be closed.");
                        recordFailure(jmsex);
                    }
                }
                return status;
            } catch (JMSException jmsex) {

                if (jmsex != null) {
                    if (jmsex instanceof JMSException) {
                        errMsg.append("Error message on Gateway" + gatewayTag + ":");
                        errMsg.append(processJMSException((JMSException) jmsex));
                    } else {
                        logger.info(jmsex.getMessage());
                        errMsg.append(jmsex.getMessage());
                    }
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
                            recordFailure(ex);
                        }
                    }

                    if (session != null) {
                        try {
                            session.close();
                        } catch (JMSException ex) {
                            logger.error("Session could not be closed.");
                            recordFailure(ex);
                        }
                    }

                    if (connection != null) {
                        try {
                            connection.close();
                        } catch (JMSException ex) {
                            logger.error("Connection could not be closed.");
                            recordFailure(ex);
                        }
                    }
                    return status;
                } else {
                    errMsg.append("\n");
                    tryTimes = 1;
                    continue;
                }
            }
        }
        return status;
    }

    /**
     * Process a JMSException and any associated inner exceptions.
     * 
     * @param jmsex
     */
    private static String processJMSException(JMSException jmsex) {
        StringBuffer msg = new StringBuffer();
        msg.append(jmsex.getMessage());
        msg.append("\n");
        Throwable innerException = jmsex.getLinkedException();
        if (innerException != null) {
            msg.append("Inner exception(s):");
            msg.append("\n");
        }
        while (innerException != null) {
            msg.append(innerException.getMessage());
            innerException = innerException.getCause();
            if (innerException != null) {
                msg.append("\n");
            }
        }
        return msg.toString();
    }

    /**
     * Record this run as failure.
     * 
     * @param ex
     */
    private static void recordFailure(Exception ex) {
        if (ex != null) {
            if (ex instanceof JMSException) {
                processJMSException((JMSException) ex);
            } else {
                logger.info(ex.getMessage());
            }
        }
        logger.info("FAILURE");
        return;
    }

}
