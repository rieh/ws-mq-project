/*
 * Copyright (c) 2016 Moody's Analytics, LLC. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of Moody's Analytics, LLC.
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of
 * the license agreement you entered into with Moody's Analytics.
 *  
 * Creat Date : Mar 2, 2016 7:40:36 PM
 */
package com.moodys.loan.anz.common.mq;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.Security;

import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;

public class MQSenderSSL {

   public static void main(String[] args) {
      System.out.println(System.getProperty("java.home"));

      String HOSTNAME = "10.12.80.58";
      String QMGRNAME = "SampleQM";
      String CHANNEL = "WAS_SVRCONN";
      //TLS_RSA_WITH_AES_256_CBC_SHA   N
      //SSL_RSA_WITH_AES_256_CBC_SHA   N
      //SSL_RSA_WITH_NULL_MD5    Y
      //SSL_RSA_WITH_NULL_SHA    Y
      //SSL_RSA_WITH_DES_CBC_SHA  N
      String SSLCIPHERSUITE = "TLS_RSA_WITH_AES_128_CBC_SHA";  

      try {
         //Security.addProvider(new com.ibm.jsse2.IBMJSSEProvider2());
         //System.setProperty("com.ibm.mq.cfg.useIBMCipherMappings", "false");
          
         Class.forName("com.sun.net.ssl.internal.ssl.Provider");

         System.out.println("JSSE is installed correctly!");

         String pwd = "123456";

         // instantiate a KeyStore with type JKS
         KeyStore ks = KeyStore.getInstance("JKS");
         // load the contents of the KeyStore
         ks.load(new FileInputStream("C://IBM/SSL/jmsKeyStore.jks"), pwd.toCharArray());
         System.out.println("Number of keys on JKS: " + Integer.toString(ks.size()));

         // Create a keystore object for the truststore
         KeyStore trustStore = KeyStore.getInstance("JKS");
         // Open our file and read the truststore (no password)
         trustStore.load(new FileInputStream("C://Projects/RO_MQ/jmsTrustStore.jks"), null);

         // Create a default trust and key manager
         TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
         KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

         // Initialise the managers
         trustManagerFactory.init(trustStore);
         keyManagerFactory.init(ks,pwd.toCharArray());

         // Get an SSL context.
         // Note: not all providers support all CipherSuites. But the
         // "SSL_RSA_WITH_3DES_EDE_CBC_SHA" CipherSuite is supported on both SunJSSE
         // and IBMJSSE2 providers

         // Accessing available algorithm/protocol in the SunJSSE provider
         // see http://java.sun.com/javase/6/docs/technotes/guides/security/SunProviders.html
         SSLContext sslContext = SSLContext.getInstance("TLS");

         // Acessing available algorithm/protocol in the IBMJSSE2 provider
         // see http://www.ibm.com/developerworks/java/jdk/security/142/secguides/jsse2docs/JSSE2RefGuide.html
         // SSLContext sslContext = SSLContext.getInstance("SSL_TLS");
          System.out.println("SSLContext provider: " + sslContext.getProvider().toString());

         // Initialise our SSL context from the key/trust managers
         sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

         // Get an SSLSocketFactory to pass to WMQ
         SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
         String[] cipherSuite = sslSocketFactory.getSupportedCipherSuites();
         
         for(int i=0; i < cipherSuite.length; i++) {
             System.out.println(cipherSuite[i]);
         }
         

         // Create default MQ connection factory
         MQQueueConnectionFactory factory = new MQQueueConnectionFactory();

         // Customize the factory
         factory.setSSLSocketFactory(sslSocketFactory);
         // Use javac SSLTest.java -Xlint:deprecation
         factory.setTransportType(WMQConstants.WMQ_CM_CLIENT);
         factory.setQueueManager(QMGRNAME);
         factory.setHostName("10.12.80.58");
         factory.setChannel(CHANNEL);
         factory.setPort(1418);
         //factory.setSSLFipsRequired(false);
         factory.setSSLCipherSuite(SSLCIPHERSUITE);

         QueueConnection connection = null;
         connection = factory.createQueueConnection("",""); //empty user, pass to avoid MQJMS2013 messages
         connection.start();
         System.out.println("JMS SSL client connection started!");
         connection.close();

      } catch (JMSException ex) {
         ex.printStackTrace();
      } catch (Exception ex){
         ex.printStackTrace();
      }
   }
}

