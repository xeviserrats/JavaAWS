package com.xevi.system.TestingAWS;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.IOUtils;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AssociateRouteTableRequest;
import software.amazon.awssdk.services.ec2.model.AttachInternetGatewayRequest;
import software.amazon.awssdk.services.ec2.model.AttributeBooleanValue;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.CreateInternetGatewayResponse;
import software.amazon.awssdk.services.ec2.model.CreateKeyPairRequest;
import software.amazon.awssdk.services.ec2.model.CreateKeyPairResponse;
import software.amazon.awssdk.services.ec2.model.CreateRouteRequest;
import software.amazon.awssdk.services.ec2.model.CreateRouteTableRequest;
import software.amazon.awssdk.services.ec2.model.CreateRouteTableResponse;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupResponse;
import software.amazon.awssdk.services.ec2.model.CreateSubnetRequest;
import software.amazon.awssdk.services.ec2.model.CreateSubnetResponse;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpcRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpcResponse;
import software.amazon.awssdk.services.ec2.model.DescribeKeyPairsResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.KeyPairInfo;
import software.amazon.awssdk.services.ec2.model.ModifySubnetAttributeRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Tag;

/**
 * Executem les comandes de:
 * https://docs.aws.amazon.com/vpc/latest/userguide/vpc-subnets-commands-example.html
 * 
 * @author xevi
 */
public class CreateResources 
{
	private InfoElementsBean infoElementsBean = null;

	public static void main(String[] args) throws Exception
	{
		try
		{
			new CreateResources().mainImpl(args);
		}
		catch(Exception e)
		{
			e.printStackTrace(System.out);
		}
	}

	private void mainImpl(String[] args) throws Exception
	{
		infoElementsBean = new InfoElementsBean();

		AWSUtils.readCredentials();

		Ec2Client client = Ec2Client.builder().region(Region.EU_WEST_1).credentialsProvider(AWSUtils.createCredentialsProvider()).build();

		createKeyPair(client);

		createVPN_Subnets(client);

		configureConnectivity(client);

		new CreatePublicResources(infoElementsBean).prepareInstanceBastion(client);

		new CreatePrivateResources(client, infoElementsBean).create();
		
		new CreateLoadBalancer(client, infoElementsBean).exec();
	}

	/**
	 * Create an VPC with a public subnet and a private subnet
	 * @param pClient
	 * @throws Exception
	 */
	public void createVPN_Subnets(Ec2Client pClient) throws Exception
	{
		// aws ec2 create-vpc --cidr-block 10.0.0.0/16 
		CreateVpcResponse wVPCResponse = pClient.createVpc(CreateVpcRequest.builder().cidrBlock(AWSConstants.VPC_CIDR). build());

		infoElementsBean.vpcId = wVPCResponse.vpc().vpcId();

		AWSUtils.addTag(pClient, infoElementsBean.vpcId, "Name", "JavaAWSExample");
		
		System.out.println("VPC_ID: "+ infoElementsBean.vpcId);

		CreateSubnetResponse wSubNetResp = pClient.createSubnet			(
				CreateSubnetRequest.builder().vpcId(wVPCResponse.vpc().vpcId()).cidrBlock(AWSConstants.SUBNET_PUBLIC_CIDR).build()
			);

		infoElementsBean.subnetPublicId = wSubNetResp.subnet().subnetId();

		AWSUtils.addTag(pClient, infoElementsBean.subnetPublicId, "Name", "SubNetBastion");

		System.out.println("Subnet Public: " + infoElementsBean.subnetPublicId);
	}
	
	/**
	 * Create Internt Gateway and route table's.
	 * @param pClient
	 * @throws Exception
	 */
	public void configureConnectivity(Ec2Client pClient) throws Exception
	{
		// Create an Internet gateway.
		CreateInternetGatewayResponse wGatewayResponse = pClient.createInternetGateway();
		infoElementsBean.gatewayId = wGatewayResponse.internetGateway().internetGatewayId(); 

		//Using the ID from the previous step, attach the Internet gateway to your VPC.
		pClient.attachInternetGateway(
				AttachInternetGatewayRequest.builder().vpcId(infoElementsBean.vpcId).internetGatewayId(wGatewayResponse.internetGateway().internetGatewayId()).build()
			);

		CreateRouteTableResponse wCreateRouteTableResp = pClient.createRouteTable(CreateRouteTableRequest.builder().vpcId(infoElementsBean.vpcId).build());

		infoElementsBean.routeTableId = wCreateRouteTableResp.routeTable().routeTableId();

		// Create a route in the route table that points all traffic (0.0.0.0/0) to the Internet gateway.
		pClient.createRoute(
				CreateRouteRequest.builder().routeTableId(infoElementsBean.routeTableId).destinationCidrBlock("0.0.0.0/0").gatewayId(infoElementsBean.gatewayId).build()
			);

		pClient.associateRouteTable(AssociateRouteTableRequest.builder().subnetId(infoElementsBean.subnetPublicId).routeTableId(infoElementsBean.routeTableId).build());

		// an instance launched into the subnet automatically receives a public IP address
		pClient.modifySubnetAttribute(ModifySubnetAttributeRequest.builder().subnetId(infoElementsBean.subnetPublicId).mapPublicIpOnLaunch(AttributeBooleanValue.builder().value(true).build()).build());
	}

	/**
	 * If a KEY PAIR with a TAG 'JAVA_KEY' is not found, create one. 
	 * @param pClient
	 * @throws Exception
	 */
	public void createKeyPair(Ec2Client pClient) throws Exception
	{
		infoElementsBean.keyPairName = "JAVA_KEY";
		
		boolean wExistsKey = false;
		// if exists, remove previous
		DescribeKeyPairsResponse wKeysResponse = pClient.describeKeyPairs();
		for (KeyPairInfo wKeyInfo : wKeysResponse.keyPairs())
			if ("JAVA_KEY".equals(wKeyInfo.keyName()))
			{
				wExistsKey = true;
				break;
			}
		
		if (!wExistsKey)
		{
			CreateKeyPairResponse wKeyPairResponse = pClient.createKeyPair(CreateKeyPairRequest.builder().keyName(infoElementsBean.keyPairName).build());
			AWSUtils.addTag(pClient, wKeyPairResponse.keyPairId(), infoElementsBean.keyPairName, infoElementsBean.keyPairName);

			String wPEMPrivateKey = wKeyPairResponse.keyMaterial();

			File wKeyPairPEM = new File(System.getProperty("user.dir") + File.separator + "KeyPair_" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".pem");

			IOUtils.write(wPEMPrivateKey, new FileOutputStream(wKeyPairPEM), Charset.defaultCharset());

			System.out.println("PRIVATE KEY PEM: '" + wKeyPairPEM.getAbsolutePath() + "'.");
		}
	}
}

