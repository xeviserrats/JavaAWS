package com.xevi.system.TestingAWS.create.privateSubnet;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

import com.xevi.system.TestingAWS.bean.InfoResourcesBean;
import com.xevi.system.TestingAWS.create.BaseCreateResource;
import com.xevi.system.TestingAWS.utils.AWSConstants;
import com.xevi.system.TestingAWS.utils.AWSUtils;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Placement;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;

/**
 * Launch two Ec2 Instance. Each in one different private subnet.
 * 
 * https://s3-eu-west-1.amazonaws.com/com.testaws.public/SpringBootRest-0.0.1-SNAPSHOT.war
 * 
 * @author xevi
 */
public class LaunchPrivateEc2Instances extends BaseCreateResource<Ec2Client> 
{
	public LaunchPrivateEc2Instances(Ec2Client pClient, InfoResourcesBean pBean) 
	{
		super(pClient, pBean);
	}

	@Override
	public void create() 
	{
		launchInstanceSubnetOne();

		launchInstanceSubnetTwo();
	}
	
	/**
	 * Launch an instance and associate 'user data' to launch an SpringBoot App.
	 */
	private void launchInstanceSubnetOne()
	{
		DescribeSubnetsResponse wRespSubnet = client.describeSubnets(DescribeSubnetsRequest.builder().subnetIds(bean.subnetPrivateOneId).build());
		
		//aws ec2 run-instances --image-id ami-a4827dc9 --count 1 --instance-type t2.micro --key-name MyKeyPair --security-group-ids sg-e1fb8c9a --subnet-id subnet-b46032ec
		RunInstancesResponse wResp = client.runInstances(RunInstancesRequest.builder().imageId(AWSConstants.EC2_INSTANCE_LINUX_AMAZON2_AMIID).minCount(1).maxCount(1)
				.instanceType(AWSConstants.EC2_INSTANCE_TYPE)
				.placement(Placement.builder().availabilityZone(wRespSubnet.subnets().get(0).availabilityZone()).build())
				.keyName(bean.keyPairName).securityGroupIds(bean.securityGroupIdPrivate)
				.privateIpAddress(AWSConstants.EC2_PRIVATE_IP_SUBNET_ONE)
				.subnetId(bean.subnetPrivateOneId)
				.userData(getUserData())
				.build());
		
		Instance wInstancia = wResp.instances().get(0);
		bean.instanceIdPrivateOne = wInstancia.instanceId();

		AWSUtils.addTag(client, bean.instanceIdPrivateOne, "Name", "LinuxPrivateOne");
		
		System.out.println("ID_INSTANCIA PRIVATE ONE: " + bean.instanceIdPrivateOne);
		System.out.println("IP PUBLIC: '"+wInstancia.publicIpAddress()+"' PRIVATE IP: '"+wInstancia.privateIpAddress()+"' DNS PUBLIC: '"+wInstancia.publicDnsName()+"' DNS PRIVATE: '"+wInstancia.privateDnsName()+"'.");
	}

	/**
	 * Launch an instance and associate 'user data' to launch an SpringBoot App.
	 */
	private void launchInstanceSubnetTwo()
	{
		DescribeSubnetsResponse wRespSubnet = client.describeSubnets(DescribeSubnetsRequest.builder().subnetIds(bean.subnetPrivateTwoId).build());

		//aws ec2 run-instances --image-id ami-a4827dc9 --count 1 --instance-type t2.micro --key-name MyKeyPair --security-group-ids sg-e1fb8c9a --subnet-id subnet-b46032ec
		RunInstancesResponse wResp = client.runInstances(RunInstancesRequest.builder().imageId(AWSConstants.EC2_INSTANCE_LINUX_AMAZON2_AMIID).minCount(1).maxCount(1)
				.instanceType(AWSConstants.EC2_INSTANCE_TYPE)
				.placement(Placement.builder().availabilityZone(wRespSubnet.subnets().get(0).availabilityZone()).build())
				.keyName(bean.keyPairName).securityGroupIds(bean.securityGroupIdPrivate)
				.privateIpAddress(AWSConstants.EC2_PRIVATE_IP_SUBNET_TWO)
				.subnetId(bean.subnetPrivateTwoId)
				.userData(getUserData())
				.build());

		Instance wInstancia = wResp.instances().get(0);
		bean.instanceIdPrivateTwo = wInstancia.instanceId();

		AWSUtils.addTag(client, bean.instanceIdPrivateTwo, "Name", "LinuxPrivateTwo");
		
		System.out.println("ID_INSTANCIA PRIVATE TWO: " + bean.instanceIdPrivateTwo);
		System.out.println("IP PUBLIC: '"+wInstancia.publicIpAddress()+"' PRIVATE IP: '"+wInstancia.privateIpAddress()+"' DNS PUBLIC: '"+wInstancia.publicDnsName()+"' DNS PRIVATE: '"+wInstancia.privateDnsName()+"'.");
	}

	/**
	 * Install JAVA && eecute spring boot application
	 * @return
	 */
	private static String getUserData()
	{
		try
		{
			byte[] wBytes = Files.readAllBytes(Paths.get(AWSConstants.class.getResource("/com/xevi/system/TestingAWS/resources/LinuxEc2Private_UserData.txt").toURI()));

			return new String(Base64.getEncoder().encode(wBytes));
		}
		catch(Exception e)
		{
			throw new IllegalStateException(e);
		}
	}
}
