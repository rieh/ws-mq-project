/*
 * Copyright (c) 2016 Moody's Analytics, LLC. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of Moody's Analytics, LLC.
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of
 * the license agreement you entered into with Moody's Analytics.
 *  
 * Creat Date : Feb 23, 2016 4:27:56 PM
 */
package com.moodys.loan.anz.common.mq;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

/** 
 * ClassName: JNDITest <br/> 
 * Function: TODO Add Function for this Class here. <br/> 
 * 
 * Date: Feb 23, 2016 4:27:56 PM <br/> 
 * @author xuxiao
 * @version $Revision:$
 * @change	$Change:$
 * @lastestModifier $Author:$
 */
public class JNDITest {

    /** 
     * TODO Describe the functionality of this method Here<br/> 
     * TODO Describe how to use this method here (Optional).<br/> 
     * 
     * @author xuxiao 
     * @param args 
     * @throws NamingException 
     * @throws IOException 
     * @throws FileNotFoundException 
     */
    public static void main(String[] args) throws NamingException, FileNotFoundException, IOException {
        // TODO Auto-generated method stub
        //使用文件系统作为JNDI服务器
        //System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.fscontext.RefFSContextFactory");
        //System.setProperty(Context.PROVIDER_URL, "/");
        
        
        Properties prop = new Properties();
        //prop.load(new FileInputStream("fileSystemService.properties"));
        prop.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.fscontext.RefFSContextFactory");
        prop.put(Context.PROVIDER_URL, "file:///c:/");
        Context context = new InitialContext(prop);
        //context.createSubcontext("haukey2/port2");
        Reference ref = new Reference("MQ");
        ref.add(new StringRefAddr("PORT", "1111"));
        context.rebind("haukey1/port/a", ref);
        System.out.println(context.lookup("haukey1/port/a"));
        context.close();
        //context.bind("MQ_HOST", "10.12.80.58");
        
        //System.out.println(context.lookup("MQ_HOST"));

    }

}

