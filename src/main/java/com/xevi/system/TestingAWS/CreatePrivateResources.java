package com.xevi.system.TestingAWS;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupResponse;
import software.amazon.awssdk.services.ec2.model.CreateSubnetRequest;
import software.amazon.awssdk.services.ec2.model.CreateSubnetResponse;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.IpPermission;
import software.amazon.awssdk.services.ec2.model.Placement;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Subnet;
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

	public static final String ZONENAME_ONE = "eu-west-1a";
	public static final String ZONENAME_TWO = "eu-west-1b";
	
	public static final String ZONENAME_ID_ONE = "euw1-az1";
	public static final String ZONENAME_ID_TWO = "euw1-az2";

	
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
				CreateSubnetRequest.builder().vpcId(bean.vpcId).cidrBlock(CreateResources.SUBNET_PRIVATE_ONE_CIDR)/*.availabilityZone("eu-west-1a")*/.build()
			);

		bean.subnetPrivateOneId = wSubNetPrivOne.subnet().subnetId();
		System.out.println("Subnet PrivateOne: " + bean.subnetPrivateOneId);

        client.createTags(CreateTagsRequest.builder().resources(bean.subnetPrivateOneId).tags(
        		Tag.builder().key("Name").value("SubNetPrivateOne").build()).build());

		// aws ec2 create-subnet --vpc-id vpc-2f09a348 --cidr-block 10.0.2.0/24
		CreateSubnetResponse wSubNetPrivTwo = client.createSubnet			(
				CreateSubnetRequest.builder().vpcId(bean.vpcId).cidrBlock(CreateResources.SUBNET_PRIVATE_TWO_CIDR)/*.availabilityZoneId("euw1-az3")*/.build()
			);
		
		bean.subnetPrivateTwoId = wSubNetPrivTwo.subnet().subnetId();
		
        client.createTags(CreateTagsRequest.builder().resources(bean.subnetPrivateTwoId).tags(
        		Tag.builder().key("Name").value("SubNetPrivateTwo").build()).build());

		System.out.println("Subnet PrivateTwo: " + bean.subnetPrivateTwoId);
	}

	private void createPrivateSecurityGroups() throws Exception
	{
		try
		{
			CreateSecurityGroupResponse wResp = client.createSecurityGroup(CreateSecurityGroupRequest.builder().groupName("PrivateLANAccess_6c").description("Access Instances Private LAN.").vpcId(bean.vpcId).build());
			bean.securityGroupIdPrivate = wResp.groupId();

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

//			client.authorizeSecurityGroupIngress(AuthorizeSecurityGroupIngressRequest.builder().groupId(bean.securityGroupIdPrivate). ipProtocol("tcp").fromPort(22).toPort(22).sourceSecurityGroupName(bean.securityGroupIdPublic).build());
//			client.authorizeSecurityGroupIngress(AuthorizeSecurityGroupIngressRequest.builder().groupId(bean.securityGroupIdPrivate).ipProtocol("tcp").fromPort(80).toPort(80).sourceSecurityGroupName(bean.securityGroupIdPublic).build());
//			client.authorizeSecurityGroupIngress(AuthorizeSecurityGroupIngressRequest.builder().groupId(bean.securityGroupIdPrivate).ipProtocol("tcp").fromPort(443).toPort(443).sourceSecurityGroupName(bean.securityGroupIdPublic).build());
		}
		catch(Exception e)
		{
			throw e;
		}
	}

	private void launchInstanceSubnetOne() throws Exception
	{
		//aws ec2 run-instances --image-id ami-a4827dc9 --count 1 --instance-type t2.micro --key-name MyKeyPair --security-group-ids sg-e1fb8c9a --subnet-id subnet-b46032ec
		RunInstancesResponse wResp = client.runInstances(RunInstancesRequest.builder().imageId(CreateResources.EC2_INSTANCE_LINUX_AMAZON2_AMIID).minCount(1).maxCount(1)
				.instanceType(CreateResources.EC2_INSTANCE_TYPE)
//				.placement(Placement.builder().availabilityZone(ZONENAME_ONE).build())
				.keyName(bean.keyPairName).securityGroupIds(bean.securityGroupIdPrivate)
				.subnetId(bean.subnetPrivateOneId).build());

		Instance wInstancia = wResp.instances().get(0);
		bean.instanceIdPrivateOne = wInstancia.instanceId();

		System.out.println("ID_INSTANCIA PRIVATE ONE: " + bean.instanceIdPrivateOne);
		System.out.println("IP PUBLIC: '"+wInstancia.publicIpAddress()+"' PRIVATE IP: '"+wInstancia.privateIpAddress()+"' DNS PUBLIC: '"+wInstancia.publicDnsName()+"' DNS PRIVATE: '"+wInstancia.privateDnsName()+"'.");
	}

	private void launchInstanceSubnetTwo() throws Exception
	{
		//aws ec2 run-instances --image-id ami-a4827dc9 --count 1 --instance-type t2.micro --key-name MyKeyPair --security-group-ids sg-e1fb8c9a --subnet-id subnet-b46032ec
		RunInstancesResponse wResp = client.runInstances(RunInstancesRequest.builder().imageId(CreateResources.EC2_INSTANCE_LINUX_AMAZON2_AMIID).minCount(1).maxCount(1)
				.instanceType(CreateResources.EC2_INSTANCE_TYPE)
//				.placement(Placement.builder().availabilityZone(ZONENAME_TWO).build())
				.keyName(bean.keyPairName).securityGroupIds(bean.securityGroupIdPrivate)
				.subnetId(bean.subnetPrivateTwoId).build());

		Instance wInstancia = wResp.instances().get(0);
		bean.instanceIdPrivateTwo = wInstancia.instanceId();

		System.out.println("ID_INSTANCIA PRIVATE TWO: " + bean.instanceIdPrivateTwo);
		System.out.println("IP PUBLIC: '"+wInstancia.publicIpAddress()+"' PRIVATE IP: '"+wInstancia.privateIpAddress()+"' DNS PUBLIC: '"+wInstancia.publicDnsName()+"' DNS PRIVATE: '"+wInstancia.privateDnsName()+"'.");
	}
}
