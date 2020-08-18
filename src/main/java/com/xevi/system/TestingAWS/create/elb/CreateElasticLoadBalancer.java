package com.xevi.system.TestingAWS.create.elb;

import com.xevi.system.TestingAWS.bean.InfoResourcesBean;
import com.xevi.system.TestingAWS.create.BaseCreateResource;
import com.xevi.system.TestingAWS.utils.AWSUtils;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;

/**
 * Create the resources needed by Elastic LoadBalancer.
 * @author xevi
 */
public class CreateElasticLoadBalancer extends BaseCreateResource<Ec2Client> 
{
	public CreateElasticLoadBalancer(Ec2Client pClient, InfoResourcesBean pBean) 
	{
		super(pClient, pBean);
	}

	@Override
	public void create() 
	{
		new CreateELBParallelSubnets(client, bean).create();

		new CreateELBSecurityGroup(client, bean).create();

		ElasticLoadBalancingV2Client elbClient = ElasticLoadBalancingV2Client.builder().credentialsProvider(AWSUtils.createCredentialsProvider()).build();

		new CreateELSImpl(elbClient, bean).setEc2Client(client).create();
		
		new ModifySecurityGroupsPrivateEc2(client, bean).create();
	}
}
