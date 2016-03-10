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
public class MQSender3 {
    
    public MQStatus send(String text, MQConfiguration config) {
        
        MQStatus status = new MQStatus(-1, null);
        StringBuffer errMsg = new StringBuffer();
        
      //Assign trustStore and KeyStore to application
        System.setProperty("javax.net.ssl.trustStore", "C://Projects/RO_MQ/jmsTrustStore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "123456");
        System.setProperty("javax.net.ssl.keyStore", "C://Projects/RO_MQ/jmsKeyStore.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");
        
        try {

            boolean isTopic = false;
            MQEnvironment.hostname = "10.12.80.58";   //10.12.80.58
            MQEnvironment.port = 1418;
            MQEnvironment.channel = "WAS_SVRCONN";
            MQEnvironment.sslCipherSuite = "TLS_RSA_WITH_AES_128_CBC_SHA";
            MQQueueManager qm = new MQQueueManager("SampleQM");
            
            MQMessage message = new MQMessage();
            message.writeString(text);
            MQPutMessageOptions pmo = new MQPutMessageOptions();
            MQDestination destination = null;
            
            if(isTopic) {
                destination = qm.accessTopic("", "TOPIC_TEST", MQConstants.MQTOPIC_OPEN_AS_PUBLICATION, MQConstants.MQOO_OUTPUT);
            }else {
                destination = qm.accessQueue("Q1", MQConstants.MQOO_INPUT_AS_Q_DEF | MQConstants.MQOO_OUTPUT);
            }
            destination.put(message, pmo);
            destination.close();
            qm.disconnect();
            status.setStatusCode(0);
            status.setReturnMsg("success");
            status.setGateway("gateway1");
            status.setTryTimes(1);
        }catch(MQException e) {
            //System.out.println(e);
            e.printStackTrace();
            status.setStatusCode(-1);
            errMsg.append("error code : "+e.getErrorCode()+"\n");
            errMsg.append("reason code : "+e.getReason()+"\n");
            errMsg.append("completion code : "+e.getCompCode()+"\n");
            errMsg.append("error message : "+e.getMessage()+"\n");
            errMsg.append("error cause : "+e.getCause());
            status.setReturnMsg(errMsg.toString());
            status.setGateway("gateway 1");
            status.setTryTimes(1);
            return status;
        }catch(IOException e) {
            System.out.println(e);
        }
        return status;
        
    }
}

