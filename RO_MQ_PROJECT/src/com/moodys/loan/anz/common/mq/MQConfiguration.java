/*
 * Copyright (c) 2016 Moody's Analytics, LLC. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of Moody's Analytics, LLC.
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of
 * the license agreement you entered into with Moody's Analytics.
 *  
 * Creat Date : Feb 17, 2016 5:32:46 PM
 */
package com.moodys.loan.anz.common.mq;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/** 
 * ClassName: MQConfiguration <br/> 
 * Function: TODO Add Function for this Class here. <br/> 
 * 
 * Date: Feb 17, 2016 5:32:46 PM <br/> 
 * @author xuxiao
 * @version $Revision:$
 * @change	$Change:$
 * @lastestModifier $Author:$
 */
public class MQConfiguration {
    
    public static final String MQ_HOST = "host";
    public static final String MQ_PORT = "port";
    public static final String MQ_CHANNEL = "channel";
    public static final String MQ_QUEUE_MANAGER_NAME = "queueManagerName";
    public static final String MQ_DESTINATION_NAME = "destinationName";
    public static final String MQ_IS_TOPIC = "isTopic";
    public static final String MQ_CONNECTION_FACTORY_FROM_JNDI = "connectionFactoryFromJndi";
    public static final String MQ_DESTINATION_FROM_JNDI = "destinationFromJndi";
    
    public static final String MQ_KEY_STORE_PATH = "mqKeyStorePath";
    public static final String MQ_KEY_STORE_PWD = "mqKeyStorePwd";
    public static final String MQ_TRUST_STORE_PATH = "mqKeyTrustStorePath";
    public static final String MQ_TRUST_STORE_PWD = "mqTrustStorePwd";
    public static final String MQ_CIPHER_SUITE = "mqCipherSuite";
    
    protected int mqRetryTimes = 3;
    private List<HashMap<String, Object>> gateways;
    protected boolean isSSL = false;
    private HashMap<String, String> SSLConfig;
    

    public MQConfiguration() {
        
        gateways = new LinkedList<HashMap<String, Object>>();
        HashMap<String, Object> gateway = new HashMap<String, Object>();
        
        //10.12.80.58   1418   WAS_SVRCONN   sampleQM   Q1
        gateway.put(MQ_HOST, "10.12.80.58");
        gateway.put(MQ_PORT, 1418);
        gateway.put(MQ_CHANNEL, "WAS_SVRCONN");
        gateway.put(MQ_QUEUE_MANAGER_NAME, "SampleQM");
        gateway.put(MQ_DESTINATION_NAME, "Q1");
        gateway.put(MQ_IS_TOPIC, false);
        
        gateway.put(MQ_CONNECTION_FACTORY_FROM_JNDI, "jms/myCF1");
        gateway.put(MQ_DESTINATION_FROM_JNDI, "jms/myTopic");
        
        HashMap<String, Object> gateway2 = new HashMap<String, Object>();
        gateway2.put(MQ_HOST, "apc-wgroapp613");
        gateway2.put(MQ_PORT, 1416);
        gateway2.put(MQ_CHANNEL, "QM2_SCC");
        gateway2.put(MQ_QUEUE_MANAGER_NAME, "QM2");
        gateway2.put(MQ_DESTINATION_NAME, "A");
        gateway2.put(MQ_IS_TOPIC, false);
        
        gateway2.put(MQ_CONNECTION_FACTORY_FROM_JNDI, "jms/myCF1");
        gateway2.put(MQ_DESTINATION_FROM_JNDI, "jms/myQueue");
        
        gateways.add(gateway);
        gateways.add(gateway2);
        
        SSLConfig = new HashMap<String, String>();
        SSLConfig.put(MQ_KEY_STORE_PATH, "C://Projects/RO_MQ/jmsKeyStore.jks");
        SSLConfig.put(MQ_KEY_STORE_PWD, "123456");
        SSLConfig.put(MQ_TRUST_STORE_PATH, "C://Projects/RO_MQ/jmsTrustStore.jks");
        SSLConfig.put(MQ_TRUST_STORE_PWD, "123456");
        SSLConfig.put(MQ_CIPHER_SUITE, "SSL_RSA_WITH_3DES_EDE_CBC_SHA");
        
        
    }

    /**
     * @return gateways
     */
    public List<HashMap<String, Object>> getGateways() {
        return gateways;
    }

    /**
     * @param gateways the gateways to set
     */
    public void setGateways(List<HashMap<String, Object>> gateways) {
        this.gateways = gateways;
    }
    
    public int getMqRetryTimes() {
        return mqRetryTimes;
    }

    public void setMqRetryTimes(int mqRetryTimes) {
        this.mqRetryTimes = mqRetryTimes;
    }
    
    /**
     * @return SSLConfig
     */
    public HashMap<String, String> getSSLConfig() {
        return SSLConfig;
    }

    /**
     * @param sSLConfig the sSLConfig to set
     */
    public void setSSLConfig(HashMap<String, String> sSLConfig) {
        SSLConfig = sSLConfig;
    }

    /**
     * @return isSSL
     */
    public boolean isSSL() {
        return isSSL;
    }

    /**
     * @param isSSL the isSSL to set
     */
    public void setSSL(boolean isSSL) {
        this.isSSL = isSSL;
    }

    /**
     * 
     */
    

}

