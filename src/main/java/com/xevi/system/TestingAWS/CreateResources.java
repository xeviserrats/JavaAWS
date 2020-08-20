package com.xevi.system.TestingAWS;

import com.xevi.system.TestingAWS.bean.InfoResourcesBean;
import com.xevi.system.TestingAWS.create.ConfigurePublicSubnetConnectivity;
import com.xevi.system.TestingAWS.create.CreateEc2InstanceBastion;
import com.xevi.system.TestingAWS.create.CreateKeyPair;
import com.xevi.system.TestingAWS.create.CreateVPC_PublicSubnets;
import com.xevi.system.TestingAWS.create.elb.CreateElasticLoadBalancer;
import com.xevi.system.TestingAWS.create.privateSubnet.CreatePrivateResources;
import com.xevi.system.TestingAWS.utils.AWSUtils;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;

/**
 * Creates two EC2 instances in a private subnet and public facing Elastic Load Balancer on front of them.<br>
 * A Linux Bastion (small Ec2 instane) is used to connect to the other Ec2 instances.<br>
 * <br>
 * The credentials (aws access key and aws secret key) are read from Standard Input. For security, those keys are not stored in a file.
 * @author xevi
 */
public class CreateResources 
{
	public static void main(String[] args) throws Exception
	{
		createResources(args);
	}

	private static void createResources(String[] args) 
	{
		InfoResourcesBean bean = new InfoResourcesBean();

		AWSUtils.readCredentials();

		Ec2Client client = Ec2Client.builder().region(Region.EU_WEST_1).credentialsProvider(AWSUtils.createCredentialsProvider()).build();

		new CreateKeyPair(client, bean).create();;

		createPublicResources(client, bean);

		new CreatePrivateResources(client, bean).create();

		new CreateElasticLoadBalancer(client, bean).create();
	}
	
	/**
	 * Create the resources in the public subnet
	 * @param pClient
	 * @param pBean
	 */
	private static void createPublicResources(Ec2Client pClient, InfoResourcesBean pBean) 
	{
		new CreateVPC_PublicSubnets(pClient, pBean).create();

		new ConfigurePublicSubnetConnectivity(pClient, pBean).create();

		new CreateEc2InstanceBastion(pClient, pBean).create();
	}
}

