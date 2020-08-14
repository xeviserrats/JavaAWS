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
import software.amazon.awssdk.services.ec2.model.AvailabilityZone;
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
import software.amazon.awssdk.services.ec2.model.DeleteKeyPairRequest;
import software.amazon.awssdk.services.ec2.model.DescribeAvailabilityZonesResponse;
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
public class LauncInstancePublicSubnet 
{
	private InfoElementsBean infoElementsBean = null;

	public static final String VPC_CIDR 							= "10.0.0.0/16";
	public static final String SUBNET_PUBLIC_CIDR 					= "10.0.0.0/24";
	public static final String SUBNET_PRIVATE_ONE_CIDR 				= "10.0.1.0/24";
	public static final String SUBNET_PRIVATE_TWO_CIDR 				= "10.0.2.0/24";
	
	public static final String EC2_INSTANCE_LINUX_AMAZON2_AMIID = "ami-07d9160fa81ccffb5";
	public static final String EC2_INSTANCE_TYPE 				= "t2.micro";
	
	public static void main(String[] args) throws Exception
	{
		try
		{
			new LauncInstancePublicSubnet().mainImpl(args);
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

		prepareInstanceBastion(client);
		
		new CreatePrivateLAN(client, infoElementsBean).create();
	}

	/**
	 * Create an VPC witha  public subnet and a private subnet
	 * @param pClient
	 * @throws Exception
	 */
	public void createVPN_Subnets(Ec2Client pClient) throws Exception
	{
		// aws ec2 create-vpc --cidr-block 10.0.0.0/16 
		CreateVpcResponse wVPCResponse = pClient.createVpc(CreateVpcRequest.builder().cidrBlock(VPC_CIDR). build());

		infoElementsBean.vpcId = wVPCResponse.vpc().vpcId();

        Tag tag = Tag.builder().key("Name").value("JavaAWSExample").build();

        CreateTagsRequest tagRequest = CreateTagsRequest.builder().resources(infoElementsBean.vpcId).tags(tag).build();
        pClient.createTags(tagRequest);
		
		System.out.println("VPC_ID: "+ infoElementsBean.vpcId);

		CreateSubnetResponse wSubNetResp = pClient.createSubnet			(
				CreateSubnetRequest.builder().vpcId(wVPCResponse.vpc().vpcId()).cidrBlock(SUBNET_PUBLIC_CIDR).build()
			);

		infoElementsBean.subnetPublicId = wSubNetResp.subnet().subnetId();
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
	
	public void prepareInstanceBastion(Ec2Client pClient) throws Exception
	{
		configureSecurityGroupBastion(pClient);

		launchInstanceBastion(pClient);
	}

	public void launchInstanceBastion(Ec2Client pClient) throws Exception
	{
		//aws ec2 run-instances --image-id ami-a4827dc9 --count 1 --instance-type t2.micro --key-name MyKeyPair --security-group-ids sg-e1fb8c9a --subnet-id subnet-b46032ec
		RunInstancesResponse wResp = pClient.runInstances(RunInstancesRequest.builder().imageId(EC2_INSTANCE_LINUX_AMAZON2_AMIID).minCount(1).maxCount(1).instanceType(EC2_INSTANCE_TYPE).keyName(infoElementsBean.keyPairName).securityGroupIds(infoElementsBean.securityGroupIdPublic).subnetId(infoElementsBean.subnetPublicId).build());
		
		Instance wInstancia = wResp.instances().get(0);
		infoElementsBean.instanceIdPublic = wInstancia.instanceId();
		
		System.out.println("ID_INSTANCIA: " + infoElementsBean.instanceIdPublic);
		System.out.println("IP PUBLIC: '"+wInstancia.publicIpAddress()+"' DNS: '"+wInstancia.publicDnsName()+"'.");
	}
	


	public void configureSecurityGroupBastion(Ec2Client pClient) throws Exception
	{
		// aws ec2 create-security-group --group-name SSHAccess --description "Security group for SSH access" --vpc-id vpc-2f09a348
		CreateSecurityGroupResponse wResp = pClient.createSecurityGroup(CreateSecurityGroupRequest.builder().groupName("BastionSSHAccess_2").description("SSH Access to Bastion").vpcId(infoElementsBean.vpcId).build());
		infoElementsBean.securityGroupIdPublic = wResp.groupId();

		String wMyPublicIP = AWSUtils.getMyIPFromAmazon();
		String cidrIp = wMyPublicIP + "/32";
		// aws ec2 authorize-security-group-ingress --group-id sg-e1fb8c9a --protocol tcp --port 22 --cidr 0.0.0.0/0
		pClient.authorizeSecurityGroupIngress(AuthorizeSecurityGroupIngressRequest.builder().groupId(infoElementsBean.securityGroupIdPublic).ipProtocol("tcp").fromPort(22).toPort(22).cidrIp(cidrIp).build());
	}

	/**
	 * 
	 * @param pClient
	 * @throws Exception
	 */
	public void createKeyPair(Ec2Client pClient) throws Exception
	{
		infoElementsBean.keyPairName = "JAVA_KEY";
		
		// if exists, remove previous
		DescribeKeyPairsResponse wKeysResponse = pClient.describeKeyPairs();
		for (KeyPairInfo wKeyInfo : wKeysResponse.keyPairs())
			if ("JAVA_KEY".equals(wKeyInfo.keyName()))
				pClient.deleteKeyPair(DeleteKeyPairRequest.builder().keyName(infoElementsBean.keyPairName).build());
		
		CreateKeyPairResponse wKeyPairResponse = pClient.createKeyPair(CreateKeyPairRequest.builder().keyName(infoElementsBean.keyPairName).build());
		String wPEMPrivateKey = wKeyPairResponse.keyMaterial();

		File wKeyPairPEM = new File(System.getProperty("user.dir") + File.separator + "KeyPair_" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".pem");

		IOUtils.write(wPEMPrivateKey, new FileOutputStream(wKeyPairPEM), Charset.defaultCharset());

		System.out.println("PRIVATE KEY PEM: '" + wKeyPairPEM.getAbsolutePath() + "'.");
	}


}

