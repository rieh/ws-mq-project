/*
 * Copyright (c) 2016 Moody's Analytics, LLC. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of Moody's Analytics, LLC.
 * You shall not disclose such Confidential Information and shall use it only in accordance with the terms of
 * the license agreement you entered into with Moody's Analytics.
 *  
 * Creat Date : Feb 19, 2016 11:25:48 AM
 */
package com.moodys.loan.anz.common.mq;

/** 
 * ClassName: MQStatus <br/> 
 * Function: TODO Add Function for this Class here. <br/> 
 * 
 * Date: Feb 19, 2016 11:25:48 AM <br/> 
 * @author xuxiao
 * @version $Revision:$
 * @change	$Change:$
 * @lastestModifier $Author:$
 */
public class MQStatus {
    
    private int statusCode;
    private String returnMsg;
    private String gateway;
    private int tryTimes;
    /**
     * @return gateway
     */
    public String getGateway() {
        return gateway;
    }
    /**
     * @param statusCode
     * @param returnMsg
     */
    public MQStatus(int statusCode, String returnMsg) {
        super();
        this.statusCode = statusCode;
        this.returnMsg = returnMsg;
    }
    /**
     * @param gateway the gateway to set
     */
    public void setGateway(String gateway) {
        this.gateway = gateway;
    }
    /**
     * @return tryTimes
     */
    public int getTryTimes() {
        return tryTimes;
    }
    /**
     * @param tryTimes the tryTimes to set
     */
    public void setTryTimes(int tryTimes) {
        this.tryTimes = tryTimes;
    }

    /**
     * @return statusCode
     */
    public int getStatusCode() {
        return statusCode;
    }
    /**
     * @param statusCode the statusCode to set
     */
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
    /**
     * @return returnMsg
     */
    public String getReturnMsg() {
        return returnMsg;
    }
    /**
     * @param returnMsg the returnMsg to set
     */
    public void setReturnMsg(String returnMsg) {
        this.returnMsg = returnMsg;
    }
    
    

}

