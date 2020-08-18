package com.xevi.system.TestingAWS.create.elb;

import com.xevi.system.TestingAWS.bean.InfoResourcesBean;
import com.xevi.system.TestingAWS.create.BaseCreateResource;
import com.xevi.system.TestingAWS.utils.AWSUtils;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupResponse;

/**
 * Elastoic Load Balancer Security Group. Accept request (to ports 80 && 443) from all IP's (0.0.0.0/0).
 * @author xevi
 */
public class CreateELBSecurityGroup extends BaseCreateResource<Ec2Client> 
{
	protected CreateELBSecurityGroup(Ec2Client pClient, InfoResourcesBean pBean) 
	{
		super(pClient, pBean);
	}

	@Override
	public void create() 
	{
		CreateSecurityGroupResponse wResp = client.createSecurityGroup(CreateSecurityGroupRequest.builder().groupName("sgLoadBalancer").description("Security Group Load Balancer").vpcId(bean.vpcId).build());
		bean.loadBalancerSecurityGroup = wResp.groupId();

		System.out.println("SecurityGroup Load Balancer: " + wResp.groupId());
		
		AWSUtils.addTag(client, bean.loadBalancerSecurityGroup, "Name", "sgLoadBalancer");

		client.authorizeSecurityGroupIngress(AuthorizeSecurityGroupIngressRequest.builder().groupId(bean.loadBalancerSecurityGroup).ipProtocol("tcp").fromPort(80).toPort(80).cidrIp("0.0.0.0/0").build());
		client.authorizeSecurityGroupIngress(AuthorizeSecurityGroupIngressRequest.builder().groupId(bean.loadBalancerSecurityGroup).ipProtocol("tcp").fromPort(443).toPort(443).cidrIp("0.0.0.0/0").build());
	}
}
