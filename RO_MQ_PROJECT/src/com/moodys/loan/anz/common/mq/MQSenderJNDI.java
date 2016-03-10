package com.moodys.loan.anz.common.mq;

import java.util.Hashtable;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;

import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsDestination;

public class MQSenderJNDI {

  private static String initialContextUrl = null;
  private static String connectionFactoryFromJndi = null;
  private static String destinationFromJndi = null;

  private static int status = 1;

  public static void main(String[] args) {
    // Parse the arguments
    initialContextUrl = "";
    connectionFactoryFromJndi = "";
    destinationFromJndi = "";

    // Variables
    Connection connection = null;
    Session session = null;
    Destination destination = null;
    MessageProducer producer = null;

    try {
      // Instantiate the initial context
      String contextFactory = "com.sun.jndi.fscontext.RefFSContextFactory";
      Hashtable environment = new Hashtable();
      environment.put(Context.INITIAL_CONTEXT_FACTORY, contextFactory);
      environment.put(Context.PROVIDER_URL, initialContextUrl);
      Context context = new InitialDirContext(environment);
      System.out.println("Initial context found!");

      // Lookup the connection factory
      JmsConnectionFactory cf = (JmsConnectionFactory) context.lookup(connectionFactoryFromJndi);

      // Lookup the destination
      destination = (JmsDestination) context.lookup(destinationFromJndi);

      // Create JMS objects
      connection = cf.createConnection();
      session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
      producer = session.createProducer(destination);

      long uniqueNumber = System.currentTimeMillis() % 1000;
      TextMessage message = session
          .createTextMessage("JmsJndiProducer: Your lucky number today is " + uniqueNumber);

      // Start the connection
      connection.start();

      // And, send the message
      producer.send(message);
      System.out.println("Sent message:\n" + message);

      recordSuccess();
    }
    catch (JMSException jmsex) {
      recordFailure(jmsex);
    }
    catch (NamingException ne) {
      System.out.println("The initial context could not be instantiated, or the lookup failed.");
      recordFailure(ne);
    }
    finally {
      if (producer != null) {
        try {
          producer.close();
        }
        catch (JMSException jmsex) {
          System.out.println("Producer could not be closed.");
          recordFailure(jmsex);
        }
      }

      if (session != null) {
        try {
          session.close();
        }
        catch (JMSException jmsex) {
          System.out.println("Session could not be closed.");
          recordFailure(jmsex);
        }
      }

      if (connection != null) {
        try {
          connection.close();
        }
        catch (JMSException jmsex) {
          System.out.println("Connection could not be closed.");
          recordFailure(jmsex);
        }
      }
    }
    System.exit(status);
    return;
  } // end main()

  /**
   * Process a JMSException and any associated inner exceptions.
   * 
   * @param jmsex
   */
  private static void processJMSException(JMSException jmsex) {
    System.out.println(jmsex);
    Throwable innerException = jmsex.getLinkedException();
    if (innerException != null) {
      System.out.println("Inner exception(s):");
    }
    while (innerException != null) {
      System.out.println(innerException);
      innerException = innerException.getCause();
    }
    return;
  }

  /**
   * Record this run as successful.
   */
  private static void recordSuccess() {
    System.out.println("SUCCESS");
    status = 0;
    return;
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
      }
      else {
        System.out.println(ex);
      }
    }
    System.out.println("FAILURE");
    status = -1;
    return;
  }

  /**
   * Parse user supplied arguments.
   * 
   * @param args
   */
  private static void parseArgs(String[] args) {
    try {
      int length = args.length;
      if (length == 0) {
        throw new IllegalArgumentException("No arguments! Mandatory arguments must be specified.");
      }
      if ((length % 2) != 0) {
        throw new IllegalArgumentException("Incorrect number of arguments!");
      }

      int i = 0;

      while (i < length) {
        if ((args[i]).charAt(0) != '-') {
          throw new IllegalArgumentException("Expected a '-' character next: " + args[i]);
        }

        char opt = (args[i]).toLowerCase().charAt(1);

        switch (opt) {
          case 'i' :
            initialContextUrl = args[++i];
            break;
          case 'c' :
            connectionFactoryFromJndi = args[++i];
            break;
          case 'd' :
            destinationFromJndi = args[++i];
            break;
          default : {
            throw new IllegalArgumentException("Unknown argument: " + opt);
          }
        }

        ++i;
      }

      if (initialContextUrl == null) {
        throw new IllegalArgumentException("An initial context must be specified.");
      }

      if (connectionFactoryFromJndi == null) {
        throw new IllegalArgumentException(
            "A connection factory to lookup in the initial context must be specified.");
      }

      if (destinationFromJndi == null) {
        throw new IllegalArgumentException(
            "A destination to lookup in the initial context must be specified.");
      }
    }
    catch (Exception e) {
      System.out.println(e.getMessage());
      printUsage();
      System.exit(-1);
    }
    return;
  }

  /**
   * Display usage help.
   */
  private static void printUsage() {
    System.out.println("\nUsage:");
    System.out.println("JmsJndiProducer -i initialContext -c connectionFactory -d destination");
    return;
  }

} // end class
