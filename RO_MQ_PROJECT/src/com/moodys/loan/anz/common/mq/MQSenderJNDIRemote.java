/*
 * Copyright (c) 2016 Moody's Analytics, LLC. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of Moody's Analytics, LLC.
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of
 * the license agreement you entered into with Moody's Analytics.
 *  
 * Creat Date : Feb 25, 2016 2:57:39 PM
 */
package com.moodys.loan.anz.common.mq;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsDestination;

/** 
 * ClassName: MQSender <br/> 
 * Function: TODO Add Function for this Class here. <br/> 
 * 
 * Date: Feb 25, 2016 2:57:39 PM <br/> 
 * @author xuxiao
 * @version $Revision:$
 * @change	$Change:$
 * @lastestModifier $Author:$
 */
public class MQSenderJNDIRemote {
    
    private static final Logger logger = LoggerFactory.getLogger(MQSenderJNDIRemote.class);
    
    private String contextFactory;   //need to be injected from Spring context
    
    public MQSenderJNDIRemote(String contextFactory) {
        this.setContextFactory(contextFactory);
    }
    
    public MQStatus send(String text, MQConfiguration config) {
        MQStatus status = new MQStatus(-1, null);
        Connection connection = null;
        Session session = null;
        Destination destination = null;
        MessageProducer producer = null;
        TextMessage message = null;
        JmsConnectionFactory cf = null;
        StringBuffer errMsg = new StringBuffer();
        // Instantiate the initial context
        Hashtable<String, String> environment = new Hashtable<String, String>();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, contextFactory);
        environment.put(Context.PROVIDER_URL, "iiop://10.12.136.114:2809"); //just for jse test
        Context context = null;
        try {
            context = new InitialContext();
        } catch (NamingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            logger.info("Initial context failed");
            status.setStatusCode(-2);
            status.setGateway(null);
            status.setReturnMsg("Initial context failed:"+e1.getMessage());
            status.setTryTimes(0);
            return status;
        }
        logger.info("Initial context found!");
        
        Iterator<HashMap<String, Object>> iter = config.getGateways().iterator();
        int size = config.getGateways().size();
        HashMap<String, Object> gateway;
        int gatewayTag = 0;
        int tryTimes = 1;
        while (iter.hasNext()) {
            gatewayTag++;
            gateway = (HashMap<String, Object>) iter.next();
            try {
                do {
                    try {
                        logger.info("try to send message on gateway" + gatewayTag + " times: " + tryTimes);
                        // Lookup the connection factory
                        cf = (JmsConnectionFactory) context.lookup(gateway.get(MQConfiguration.MQ_CONNECTION_FACTORY_FROM_JNDI).toString());
                        // Lookup the destination
                        destination = (JmsDestination) context.lookup(gateway.get(MQConfiguration.MQ_DESTINATION_FROM_JNDI).toString());
                        // Create JMS objects
                        connection = cf.createConnection();
                        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                        producer = session.createProducer(destination);
                        message = session.createTextMessage(text);
                        // Start the connection
                        connection.start();
                        // And, send the message
                        producer.send(message);
                        break;
                    } catch (Exception ex) {
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
                errMsg.append("Sent message on Gateway " + gatewayTag + "\n" + message);
                status.setStatusCode(0);
                status.setReturnMsg(errMsg.toString());
                status.setGateway("gateway" + gatewayTag);
                status.setTryTimes(tryTimes);
                if (producer != null) {
                    try {
                        producer.close();
                    } catch (JMSException jmsex) {
                        logger.info("Producer could not be closed.");
                        logger.info(jmsex.getMessage());
                    }
                }

                if (session != null) {
                    try {
                        session.close();
                    } catch (JMSException jmsex) {
                        logger.info("Session could not be closed.");
                        logger.info(jmsex.getMessage());
                    }
                }
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (JMSException jmsex) {
                        logger.info("Connection could not be closed.");
                        logger.info(jmsex.getMessage());
                    }
                }
                return status;
            } catch (Exception e) {
                // TODO Auto-generated catch block
                if (e != null) {
                    errMsg.append("Error ocurred on Gateway" + gatewayTag + "\n");
                    errMsg.append("Error message :" + e.getMessage() + "\n");
                    if (e instanceof JMSException) {
                        errMsg.append("Error cause:"+ e.getCause());
                    }
                    logger.info(e.getMessage());
                }
                if (gatewayTag == size) {
                    status.setStatusCode(-1);
                    status.setReturnMsg(errMsg.toString());
                    status.setGateway("gateway" + gatewayTag);
                    status.setTryTimes(tryTimes);
                    if (producer != null) {
                        try {
                            producer.close();
                        } catch (JMSException jmsex) {
                            logger.info("Producer could not be closed.");
                            logger.info(jmsex.getMessage());
                        }
                    }

                    if (session != null) {
                        try {
                            session.close();
                        } catch (JMSException jmsex) {
                            logger.info("Session could not be closed.");
                            logger.info(jmsex.getMessage());
                        }
                    }
                    if (connection != null) {
                        try {
                            connection.close();
                        } catch (JMSException jmsex) {
                            logger.info("Connection could not be closed.");
                            logger.info(jmsex.getMessage());
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
     * @return contextFactory
     */
    public String getContextFactory() {
        return contextFactory;
    }

    /**
     * @param contextFactory the contextFactory to set
     */
    public void setContextFactory(String contextFactory) {
        this.contextFactory = contextFactory;
    } 

}

