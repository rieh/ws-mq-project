/*
 * Copyright (c) 2016 Moody's Analytics, LLC. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of Moody's Analytics, LLC.
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of
 * the license agreement you entered into with Moody's Analytics.
 *  
 * Creat Date : Feb 17, 2016 5:47:47 PM
 */
package com.moodys.loan.anz.common.mq;


/** 
 * ClassName: MQTest <br/> 
 * Function: TODO Add Function for this Class here. <br/> 
 * 
 * Date: Feb 17, 2016 5:47:47 PM <br/> 
 * @author xuxiao
 * @version $Revision:$
 * @change	$Change:$
 * @lastestModifier $Author:$
 */
public class MQTest {

    /** 
     * TODO Describe the functionality of this method Here<br/> 
     * TODO Describe how to use this method here (Optional).<br/> 
     * 
     * @author xuxiao 
     * @param args 
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        MQConfiguration config = new MQConfiguration();
        MQSender2 sender = new MQSender2();
        MQStatus status = sender.send("This is the test Message XXXXXX from Jerry.....", config);
        
        System.out.println(status.getReturnMsg());
        System.out.println(status.getGateway());
        System.out.println(status.getStatusCode());
        System.out.println(status.getTryTimes());
    }

}

