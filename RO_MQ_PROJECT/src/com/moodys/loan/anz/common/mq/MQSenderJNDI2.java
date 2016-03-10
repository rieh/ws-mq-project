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
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ClassName: MQSender <br/>
 * Function: To send JMS message queue/topic. <br/>
 * Date: Feb 25, 2016 2:57:39 PM <br/>
 * 
 * @author xuxiao
 * @version $Revision:$
 * @change $Change:$
 * @lastestModifier $Author:$
 */
public class MQSenderJNDI2 {

    private static final Logger logger = LoggerFactory.getLogger(MQSenderJNDI2.class);
    private String contextFactory = "com.ibm.websphere.naming.WsnInitialContextFactory";
    // private String contextFactory = "javax.naming.spi.InitialContextFactory";
    private Context context = null;

    public MQStatus send(String text, MQConfiguration config) {
        MQStatus status = new MQStatus(-1, null);
        Connection connection = null;
        Session session = null;
        Destination destination = null;
        MessageProducer producer = null;
        TextMessage message = null;
        ConnectionFactory cf = null;
        StringBuilder errMsg = new StringBuilder();
        int MAX_TRY_TIMES = config.getMqRetryTimes();
        // Instantiate the initial context

        try {
            if (context == null) {
                Hashtable<String, String> environment = new Hashtable<String, String>();
                environment.put(Context.PROVIDER_URL, "iiop://10.12.136.114:2809"); // just for jse test
                environment.put(Context.INITIAL_CONTEXT_FACTORY, contextFactory);
//                environment.put("org.omg.CORBA.ORBClass", "com.ibm.CORBA.iiop.ORB");
                // environment.put("org.omg.CORBA.ORBClass", "com.ibm.CORBA.iiop.ORB");
                context = new InitialContext();

            }
        } catch (NamingException ex) {
            errMsg.append("Initial context failed, Error message : " + ex).append("\n");
            logger.error(errMsg.toString());
            status.setStatusCode(-2);
            status.setReturnMsg(errMsg.toString());
            return status;
        }
        logger.info("Initial context found!");

        Iterator<HashMap<String, Object>> iter = config.getGateways().iterator();
        int size = config.getGateways().size();
        HashMap<String, Object> gateway;
        int gatewayTag = 0;
        String gatewayStr = null;
        while (iter.hasNext()) {
            gatewayTag++;
            gateway = (HashMap<String, Object>) iter.next();
            int tryTimes = 1;
            try {
                do {
                    try {
                        String connectionFactoryName = String.valueOf(gateway.get(MQConfiguration.MQ_CONNECTION_FACTORY_FROM_JNDI));
                        String destinationName = String.valueOf(gateway.get(MQConfiguration.MQ_DESTINATION_FROM_JNDI));
                        gatewayStr = connectionFactoryName + " - " + destinationName;
                        logger.info(String.format("Begin to send message on gateway %d (%s), trying times %d ... ", gatewayTag,
                                gatewayStr, tryTimes));

                        Object ss = context.lookup(connectionFactoryName);



                        if (ss != null) {
                            logger.info("ss.getClass().getSuperclass() - {}", ss.getClass().getSuperclass().getName());
                            logger.info("ss.getClass().getGenericInterfaces() - {}", ss.getClass().getGenericInterfaces());
                        } else {
                            logger.info("SS is null !!!!!!!!!!!!! ");
                        }
                        logger.info("SS is : {}", ss);
                        // cf = (com.ibm.ejs.jms.JMSConnectionFactoryHandle) ss;
                        cf = (ConnectionFactory) ss;


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
                        String err = String.format("Failed on Gateway %d (%s), try time %d,  cause is : %s, error message is : %s ",
                                gatewayTag, gatewayStr, tryTimes, ex.getCause(), ex.getMessage());
                        errMsg.append(err).append("\n");
                        status.setReturnMsg(errMsg.toString());
                        if (tryTimes == MAX_TRY_TIMES) {
                            logger.info(err, ex);
                            throw ex;
                        } else {
                            tryTimes++;
                            continue;
                        }
                    }

                } while (tryTimes <= MAX_TRY_TIMES);
                String successMsg = String.format("Message sent successfully on gateway %d (%s), message : \n %s ", gatewayTag,
                        gatewayStr, message);
                logger.info(successMsg);
                errMsg.append(successMsg).append("\n");

                if (producer != null) {
                    try {
                        producer.close();
                    } catch (JMSException jmsex) {
                        logger.info("Producer could not be closed.");
                        logger.info(jmsex.getMessage());
                        errMsg.append("Producer could not be closed with error : ").append(jmsex.getMessage()).append("\n");
                    }
                }

                if (session != null) {
                    try {
                        session.close();
                    } catch (JMSException jmsex) {
                        logger.info("Session could not be closed.");
                        logger.info(jmsex.getMessage());
                        errMsg.append("Session could not be closed with error : ").append(jmsex.getMessage()).append("\n");
                    }
                }

                if (connection != null) {
                    try {
                        connection.close();
                    } catch (JMSException jmsex) {
                        logger.info("Connection could not be closed.");
                        logger.info(jmsex.getMessage());
                        errMsg.append("Connection could not be closed with error : ").append(jmsex.getMessage()).append("\n");
                    }
                }

                status.setStatusCode(0);
                status.setReturnMsg(errMsg.toString());
                status.setGateway(gatewayStr);
                status.setTryTimes(tryTimes);

                return status;
            } catch (Exception e) {
                if (e != null) {
                    errMsg.append(String.format("Error ocurred on Gateway %d (%s), Error message : \n", gatewayTag, gatewayStr))
                            .append(e.getMessage()).append("\n");
                    if (e instanceof JMSException) {
                        errMsg.append("Error cause:" + e.getCause()).append("\n");
                    }
                    logger.info(e.getMessage());
                }
                if (gatewayTag == size) {

                    if (producer != null) {
                        try {
                            producer.close();
                        } catch (JMSException jmsex) {
                            logger.info("Producer could not be closed.");
                            logger.info(jmsex.getMessage());
                            errMsg.append("Producer could not be closed with error : ").append(jmsex.getMessage()).append("\n");
                        }
                    }

                    if (session != null) {
                        try {
                            session.close();
                        } catch (JMSException jmsex) {
                            logger.info("Session could not be closed.");
                            logger.info(jmsex.getMessage());
                            errMsg.append("Session could not be closed with error : ").append(jmsex.getMessage()).append("\n");
                        }
                    }
                    if (connection != null) {
                        try {
                            connection.close();
                        } catch (JMSException jmsex) {
                            logger.info("Connection could not be closed.");
                            logger.info(jmsex.getMessage());
                            errMsg.append("Connection could not be closed with error : ").append(jmsex.getMessage()).append("\n");
                        }
                    }

                    status.setStatusCode(-1);
                    status.setReturnMsg(errMsg.toString());
                    status.setGateway(gatewayStr);
                    status.setTryTimes(tryTimes);

                    return status;
                }
            }
        }
        return status;
    }

}
