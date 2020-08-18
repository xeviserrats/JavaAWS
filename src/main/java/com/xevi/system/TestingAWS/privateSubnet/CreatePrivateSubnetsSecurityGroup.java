package com.xevi.system.TestingAWS.privateSubnet;

import com.xevi.system.TestingAWS.bean.InfoResourcesBean;
import com.xevi.system.TestingAWS.create.BaseCreateResource;
import com.xevi.system.TestingAWS.utils.AWSConstants;
import com.xevi.system.TestingAWS.utils.AWSUtils;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupResponse;
import software.amazon.awssdk.services.ec2.model.CreateSubnetRequest;
import software.amazon.awssdk.services.ec2.model.CreateSubnetResponse;
import software.amazon.awssdk.services.ec2.model.IpPermission;
import software.amazon.awssdk.services.ec2.model.UserIdGroupPair;

public class CreatePrivateSubnetsSecurityGroup extends BaseCreateResource<Ec2Client> 
{
	public CreatePrivateSubnetsSecurityGroup(Ec2Client pClient, InfoResourcesBean pBean) 
	{
		super(pClient, pBean);
	}

	@Override
	public void create() 
	{
		createSubnets();
		
		createPrivateSecurityGroups();
	}
	
	private void createSubnets() 
	{
		// aws ec2 create-subnet --vpc-id vpc-2f09a348 --cidr-block 10.0.1.0/24
		CreateSubnetResponse wSubNetPrivOne = client.createSubnet			(
				CreateSubnetRequest.builder().vpcId(bean.vpcId).cidrBlock(AWSConstants.SUBNET_PRIVATE_ONE_CIDR).availabilityZone(AWSConstants.ZONENAME_ONE).build()
			);

		bean.subnetPrivateOneId = wSubNetPrivOne.subnet().subnetId();
		System.out.println("Subnet PrivateOne: " + bean.subnetPrivateOneId);

		AWSUtils.addTag(client, bean.subnetPrivateOneId, "Name", "SubNetPrivateOne");

		// aws ec2 create-subnet --vpc-id vpc-2f09a348 --cidr-block 10.0.2.0/24
		CreateSubnetResponse wSubNetPrivTwo = client.createSubnet			(
				CreateSubnetRequest.builder().vpcId(bean.vpcId).cidrBlock(AWSConstants.SUBNET_PRIVATE_TWO_CIDR).availabilityZoneId(AWSConstants.ZONENAME_ID_ONE).build()
			);

		bean.subnetPrivateTwoId = wSubNetPrivTwo.subnet().subnetId();

		AWSUtils.addTag(client, bean.subnetPrivateTwoId, "Name", "SubNetPrivateTwo");

		System.out.println("Subnet PrivateTwo: " + bean.subnetPrivateTwoId);
	}
	

	private void createPrivateSecurityGroups()
	{
		CreateSecurityGroupResponse wResp = client.createSecurityGroup(CreateSecurityGroupRequest.builder().groupName("PrivateLANAccess_6c").description("Access Instances Private LAN.").vpcId(bean.vpcId).build());
		bean.securityGroupIdPrivate = wResp.groupId();

		AWSUtils.addTag(client, wResp.groupId(), "Name", "sgPrivateLinux");

		// Accept SSH access (port 22) from Bastion
		client.authorizeSecurityGroupIngress(AuthorizeSecurityGroupIngressRequest.builder().groupId(bean.securityGroupIdPrivate)
				.ipPermissions(IpPermission.builder().ipProtocol("tcp").fromPort(22).toPort(22).userIdGroupPairs(UserIdGroupPair.builder().groupId(bean.securityGroupIdPublic).vpcId(bean.vpcId).build()).build())
				.build());
		client.authorizeSecurityGroupIngress(AuthorizeSecurityGroupIngressRequest.builder().groupId(bean.securityGroupIdPrivate)
				.ipPermissions(IpPermission.builder().ipProtocol("tcp").fromPort(80).toPort(80).userIdGroupPairs(UserIdGroupPair.builder().groupId(bean.securityGroupIdPublic).vpcId(bean.vpcId).build()).build())
				.build());
		client.authorizeSecurityGroupIngress(AuthorizeSecurityGroupIngressRequest.builder().groupId(bean.securityGroupIdPrivate)
				.ipPermissions(IpPermission.builder().ipProtocol("tcp").fromPort(443).toPort(443).userIdGroupPairs(UserIdGroupPair.builder().groupId(bean.securityGroupIdPublic).vpcId(bean.vpcId).build()).build())
				.build());
	}
}
