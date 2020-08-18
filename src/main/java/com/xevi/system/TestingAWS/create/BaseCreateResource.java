package com.xevi.system.TestingAWS.create;

import com.xevi.system.TestingAWS.bean.InfoResourcesBean;

import software.amazon.awssdk.core.SdkClient;

public abstract class BaseCreateResource<C extends SdkClient> 
{
	protected C client;
	protected InfoResourcesBean bean;
	
	public BaseCreateResource(C pClient, InfoResourcesBean pBean)
	{
		super();
		
		client = pClient;
		bean = pBean;
	}
	
	public abstract void create();
}
