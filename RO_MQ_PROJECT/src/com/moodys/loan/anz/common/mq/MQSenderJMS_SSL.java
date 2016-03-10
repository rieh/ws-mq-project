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

import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Iterator;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.mq.jms.MQConnectionFactory;
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
public class MQSenderJMS_SSL {
    private static final Logger logger = LoggerFactory.getLogger(MQSenderFinalJMS.class);

    public MQStatus send(String strMessage, MQConfiguration config) {
        MQStatus status = new MQStatus(-1, "init");
        MQConnectionFactory cf = new MQConnectionFactory();
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
        
        if (config.isSSL()) {
            HashMap<String, String> sslConfig = config.getSSLConfig();
            SSLSocketFactory sslSocketFactory = getSSLSocketFactory(sslConfig.get(MQConfiguration.MQ_KEY_STORE_PATH), 
                    sslConfig.get(MQConfiguration.MQ_KEY_STORE_PWD), sslConfig.get(MQConfiguration.MQ_TRUST_STORE_PATH), sslConfig.get(MQConfiguration.MQ_TRUST_STORE_PWD));
            cf.setSSLSocketFactory(sslSocketFactory);
            cf.setSSLCipherSuite(sslConfig.get(MQConfiguration.MQ_CIPHER_SUITE));
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
                cf.setHostName(hostName);
                cf.setPort(port);
                cf.setChannel(channelName);
                cf.setQueueManager(queueManagerName);
                cf.setTransportType( WMQConstants.WMQ_CM_CLIENT);
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
    
    private SSLSocketFactory getSSLSocketFactory(String keyStorePath, String keyStorePwd, String trustStorePath, String trustStorePwd) {
        logger.info(System.getProperty("java.home"));
        SSLSocketFactory sslSocketFactory = null;
        try {
            Class.forName("com.sun.net.ssl.internal.ssl.Provider");
            logger.info("JSSE is installed correctly!");
            // instantiate a KeyStore with type JKS
            KeyStore ks = KeyStore.getInstance("JKS");
            // load the contents of the KeyStore
            ks.load(new FileInputStream(keyStorePath), keyStorePwd.toCharArray());
            logger.info("Number of keys on JKS: " + Integer.toString(ks.size()));
            // Create a keystore object for the truststore
            KeyStore trustStore = KeyStore.getInstance("JKS");
            // Open our file and read the truststore (no password)
            trustStore.load(new FileInputStream(trustStorePath), null);
            // Create a default trust and key manager
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            // Initialise the managers
            trustManagerFactory.init(trustStore);
            
            keyManagerFactory.init(ks, keyStorePwd.toCharArray());
            // Get an SSL context.
            // Note: not all providers support all CipherSuites. But the
            // "SSL_RSA_WITH_3DES_EDE_CBC_SHA" CipherSuite is supported on both SunJSSE
            // and IBMJSSE2 providers
            // Accessing available algorithm/protocol in the SunJSSE provider
            // see http://java.sun.com/javase/6/docs/technotes/guides/security/SunProviders.html
            SSLContext sslContext = SSLContext.getInstance("TLS");
            
            // SSLContext sslContext = SSLContext.getInstance("SSL_TLS", "IBMJSSE2");
            // Acessing available algorithm/protocol in the IBMJSSE2 provider
            // see http://www.ibm.com/developerworks/java/jdk/security/142/secguides/jsse2docs/JSSE2RefGuide.html
            // SSLContext sslContext = SSLContext.getInstance("SSL_TLS");
            logger.info("SSLContext provider: " + sslContext.getProvider().toString());
            // Initialise our SSL context from the key/trust managers
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
            // Get an SSLSocketFactory to pass to WMQ
            sslSocketFactory = sslContext.getSocketFactory();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return sslSocketFactory;
    }

}
