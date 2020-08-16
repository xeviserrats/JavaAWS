package com.xevi.system.TestingAWS;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupResponse;
import software.amazon.awssdk.services.ec2.model.CreateSubnetRequest;
import software.amazon.awssdk.services.ec2.model.CreateSubnetResponse;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.IpPermission;
import software.amazon.awssdk.services.ec2.model.Placement;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.UserIdGroupPair;

/**
 * We need two subnets (in differents AZ) to user Elastic Load Balancer
 * @author xevi
 *
 */
public class CreatePrivateResources 
{
	private Ec2Client client;
	private InfoElementsBean bean = null;


	
	public CreatePrivateResources(Ec2Client pClient, InfoElementsBean pInfoElementsBean)
	{
		super();
		
		client = pClient;
		bean = pInfoElementsBean;
	}

	public void create() throws Exception
	{
		createSubnets();
		
		createPrivateSecurityGroups();

		launchInstanceSubnetOne();
		
		launchInstanceSubnetTwo();

		AddAccessInternetPrivateSubnets.addConnectibityToPrivateSubnets(client, bean);
	}
	
	private void createSubnets() throws Exception
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

	private void createPrivateSecurityGroups() throws Exception
	{
		try
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
		catch(Exception e)
		{
			throw e;
		}
	}

	private void launchInstanceSubnetOne() throws Exception
	{
		DescribeSubnetsResponse wRespSubnet = client.describeSubnets(DescribeSubnetsRequest.builder().subnetIds(bean.subnetPrivateOneId).build());
		
		//aws ec2 run-instances --image-id ami-a4827dc9 --count 1 --instance-type t2.micro --key-name MyKeyPair --security-group-ids sg-e1fb8c9a --subnet-id subnet-b46032ec
		RunInstancesResponse wResp = client.runInstances(RunInstancesRequest.builder().imageId(AWSConstants.EC2_INSTANCE_LINUX_AMAZON2_AMIID).minCount(1).maxCount(1)
				.instanceType(AWSConstants.EC2_INSTANCE_TYPE)
				.placement(Placement.builder().availabilityZone(wRespSubnet.subnets().get(0).availabilityZone()).build())
				.keyName(bean.keyPairName).securityGroupIds(bean.securityGroupIdPrivate)
				.privateIpAddress("10.0.1.100")
				.subnetId(bean.subnetPrivateOneId).build());
		
		Instance wInstancia = wResp.instances().get(0);
		bean.instanceIdPrivateOne = wInstancia.instanceId();

		AWSUtils.addTag(client, bean.instanceIdPrivateOne, "Name", "LinuxPrivateOne");
		
		System.out.println("ID_INSTANCIA PRIVATE ONE: " + bean.instanceIdPrivateOne);
		System.out.println("IP PUBLIC: '"+wInstancia.publicIpAddress()+"' PRIVATE IP: '"+wInstancia.privateIpAddress()+"' DNS PUBLIC: '"+wInstancia.publicDnsName()+"' DNS PRIVATE: '"+wInstancia.privateDnsName()+"'.");
	}

	private void launchInstanceSubnetTwo() throws Exception
	{
		DescribeSubnetsResponse wRespSubnet = client.describeSubnets(DescribeSubnetsRequest.builder().subnetIds(bean.subnetPrivateTwoId).build());

		//aws ec2 run-instances --image-id ami-a4827dc9 --count 1 --instance-type t2.micro --key-name MyKeyPair --security-group-ids sg-e1fb8c9a --subnet-id subnet-b46032ec
		RunInstancesResponse wResp = client.runInstances(RunInstancesRequest.builder().imageId(AWSConstants.EC2_INSTANCE_LINUX_AMAZON2_AMIID).minCount(1).maxCount(1)
				.instanceType(AWSConstants.EC2_INSTANCE_TYPE)
				.placement(Placement.builder().availabilityZone(wRespSubnet.subnets().get(0).availabilityZone()).build())
				.keyName(bean.keyPairName).securityGroupIds(bean.securityGroupIdPrivate)
				.privateIpAddress("10.0.2.100")
				.subnetId(bean.subnetPrivateTwoId).build());

		Instance wInstancia = wResp.instances().get(0);
		bean.instanceIdPrivateTwo = wInstancia.instanceId();

		AWSUtils.addTag(client, bean.instanceIdPrivateTwo, "Name", "LinuxPrivateTwo");
		
		System.out.println("ID_INSTANCIA PRIVATE TWO: " + bean.instanceIdPrivateTwo);
		System.out.println("IP PUBLIC: '"+wInstancia.publicIpAddress()+"' PRIVATE IP: '"+wInstancia.privateIpAddress()+"' DNS PUBLIC: '"+wInstancia.publicDnsName()+"' DNS PRIVATE: '"+wInstancia.privateDnsName()+"'.");
	}
}
