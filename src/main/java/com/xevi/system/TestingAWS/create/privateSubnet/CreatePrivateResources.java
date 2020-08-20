package com.xevi.system.TestingAWS.create.privateSubnet;

import com.xevi.system.TestingAWS.bean.InfoResourcesBean;
import com.xevi.system.TestingAWS.create.BaseCreateResource;

import software.amazon.awssdk.services.ec2.Ec2Client;

/**
 * 
 * 
 * @author xevi
 */
public class CreatePrivateResources extends BaseCreateResource<Ec2Client>
{
	public CreatePrivateResources(Ec2Client pClient, InfoResourcesBean pBean) 
	{
		super(pClient, pBean);
	}

	public void create()
	{
		new CreatePrivateSubnetsSecurityGroup(client, bean).create();

		new LaunchPrivateEc2Instances(client, bean).create();

		new ConfigurePrivateSubnetConnectivity(client, bean).create();
	}
}
