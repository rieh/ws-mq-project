/*
 * Copyright (c) 2016 Moody's Analytics, LLC. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of Moody's Analytics, LLC.
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of
 * the license agreement you entered into with Moody's Analytics.
 *  
 * Creat Date : Feb 22, 2016 10:28:10 AM
 */
package com.moodys.loan.anz.common.mq;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.mq.MQDestination;
import com.ibm.mq.MQEnvironment;
import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQPutMessageOptions;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.MQConstants;

/** 
 * ClassName: MQSender3 <br/> 
 * Function: TODO Add Function for this Class here. <br/> 
 * 
 * Date: Feb 22, 2016 10:28:10 AM <br/> 
 * @author xuxiao
 * @version $Revision:$
 * @change	$Change:$
 * @lastestModifier $Author:$
 */
public class MQSenderMQI {
    private static final Logger logger = LoggerFactory.getLogger(MQSenderMQI.class);
    private boolean isTopic;
    private MQQueueManager qm;
    private MQDestination destination;
    private MQPutMessageOptions pmo;
    private MQMessage message;
    
    public MQStatus send(String text, MQConfiguration config) {
        
        MQStatus status = new MQStatus(-1, "Init");
        pmo = new MQPutMessageOptions();
        message = new MQMessage();
        try {
            message.writeString(text);
        } catch (IOException ioe) {
            logger.info(ioe.getMessage());
        }
        StringBuffer errMsg = new StringBuffer();
        
        Iterator<HashMap<String, Object>> iter = config.getGateways().iterator();
        int size = config.getGateways().size();
        HashMap<String, Object> gateway;
        int gatewayTag = 0;
        int tryTimes = 1;
        while(iter.hasNext()) {
            gatewayTag++;
            gateway = (HashMap<String, Object>) iter.next();
            try {
                MQEnvironment.hostname = gateway.get(MQConfiguration.MQ_HOST).toString();
                MQEnvironment.port = (Integer) gateway.get(MQConfiguration.MQ_PORT);
                MQEnvironment.channel = gateway.get(MQConfiguration.MQ_CHANNEL).toString();
                isTopic = (Boolean) gateway.get(MQConfiguration.MQ_IS_TOPIC);
                String queueManagerName = gateway.get(MQConfiguration.MQ_QUEUE_MANAGER_NAME).toString();
                String destinationName = gateway.get(MQConfiguration.MQ_DESTINATION_NAME).toString();
                do {
                    try {
                        logger.info("Try to send message on gateway" + gatewayTag + " times: " + tryTimes);
                        qm = new MQQueueManager(queueManagerName);
                        if(isTopic) {
                            destination = qm.accessTopic("", destinationName, MQConstants.MQTOPIC_OPEN_AS_PUBLICATION, MQConstants.MQOO_OUTPUT);
                        }else {
                            destination = qm.accessQueue(destinationName, MQConstants.MQOO_INPUT_AS_Q_DEF | MQConstants.MQOO_OUTPUT);
                        }
                        destination.put(message, pmo);
                        break;
                    } catch (MQException ex) {
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
                errMsg.append("Sent message on gateway" + gatewayTag + ": success");
                status.setStatusCode(0);
                status.setReturnMsg(errMsg.toString());
                status.setGateway("gateway" + gatewayTag);
                status.setTryTimes(tryTimes);
                if (destination != null) {
                    try {
                        destination.close();
                    } catch (MQException mqe) {
                        logger.error("Destination could not be closed.");
                    }
                }
                if (qm != null) {
                    try {
                        qm.close();
                    } catch (MQException mqe) {
                        logger.error("Queue Manager could not be closed.");
                    }
                }
                return status;
            }catch (MQException e) {
                if (e != null) {
                    errMsg.append("Error occurred on Gateway" + gatewayTag + "\n");
                    errMsg.append("Error code : "+e.getErrorCode()+"\n");
                    errMsg.append("Reason code : "+e.getReason()+"\n");
                    errMsg.append("Completion code : "+e.getCompCode()+"\n");
                    errMsg.append("Error message : "+e.getMessage()+"\n");
                    errMsg.append("Error cause : "+e.getCause());
                    logger.info(e.getMessage());
                }
                if (gatewayTag == size) {
                    status.setStatusCode(-1);
                    status.setReturnMsg(errMsg.toString());
                    status.setGateway("gateway" + gatewayTag);
                    status.setTryTimes(tryTimes);
                    if (destination != null) {
                        try {
                            destination.close();
                        } catch (MQException mqe) {
                            logger.error("Destination could not be closed.");
                        }
                    }
                    if (qm != null) {
                        try {
                            qm.close();
                        } catch (MQException mqe) {
                            logger.error("Queue Manager could not be closed.");
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
}

