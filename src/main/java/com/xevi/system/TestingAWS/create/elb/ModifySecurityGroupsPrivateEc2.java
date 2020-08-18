package com.xevi.system.TestingAWS.create.elb;

import com.xevi.system.TestingAWS.bean.InfoResourcesBean;
import com.xevi.system.TestingAWS.create.BaseCreateResource;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.IpPermission;
import software.amazon.awssdk.services.ec2.model.UserIdGroupPair;

/**
 * The ELB SecurityGrup is authorized to ingress on port 80 to the private Ec2 Instances (private subnets).
 * @author xevi
 */
public class ModifySecurityGroupsPrivateEc2 extends BaseCreateResource<Ec2Client> 
{
	protected ModifySecurityGroupsPrivateEc2(Ec2Client pClient, InfoResourcesBean pBean) 
	{
		super(pClient, pBean);
	}

	@Override
	public void create() 
	{
		client.authorizeSecurityGroupIngress(AuthorizeSecurityGroupIngressRequest.builder().groupId(bean.securityGroupIdPrivate)
				.ipPermissions(IpPermission.builder().ipProtocol("tcp").fromPort(80).toPort(80)
						.userIdGroupPairs(UserIdGroupPair.builder().groupId(bean.loadBalancerSecurityGroup).vpcId(bean.vpcId).build()).build())
				.build());	
	}
}
