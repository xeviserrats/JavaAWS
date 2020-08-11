package com.xevi.system.TestingAWS;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.IOUtils;

import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

/**
 * Executem les comandes de:
 * https://docs.aws.amazon.com/vpc/latest/userguide/vpc-subnets-commands-example.html
 * 
 * @author xevi
 */
public class LauncInstancePublicSubnet 
{
	private String vpcId 					= null;
	private String routeTableId 				= null;
	private String subnetPublicId 			= null;
	private String subnetPrivateId 			= null;
	private String gatewayId 				= null;
	private String securityGroupIdPublic 	= null;
	
	private String keyPairName				= null;
	private String instanceIdPublic 			= null;

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
		AWSUtils.readCredentials();

		Ec2Client client = Ec2Client.builder().region(Region.EU_WEST_1).credentialsProvider(AWSUtils.createCredentialsProvider()).build();

		createKeyPair(client);
		
		createVPN_Subnets(client);

		configureConnectivity(client);

		prepareInstance(client);
	}

	/**
	 * Create an VPC witha  public subnet and a private subnet
	 * @param pClient
	 * @throws Exception
	 */
	public void createVPN_Subnets(Ec2Client pClient) throws Exception
	{
		// aws ec2 create-vpc --cidr-block 10.0.0.0/16 
		CreateVpcResponse wVPCResponse = pClient.createVpc(CreateVpcRequest.builder().cidrBlock("10.0.0.0/16"). build());

		vpcId = wVPCResponse.vpc().vpcId();

		System.out.println("VPC_ID: "+ vpcId);

		CreateSubnetResponse wSubNetResp = pClient.createSubnet			(
				CreateSubnetRequest.builder().vpcId(wVPCResponse.vpc().vpcId()).cidrBlock("10.0.1.0/24").build()
			);

		subnetPublicId = wSubNetResp.subnet().subnetId();
		System.out.println("Subnet Public: " + subnetPublicId);

		// aws ec2 create-subnet --vpc-id vpc-2f09a348 --cidr-block 10.0.0.0/24
		CreateSubnetResponse wSubNetPriv = pClient.createSubnet			(
				CreateSubnetRequest.builder().vpcId(wVPCResponse.vpc().vpcId()).cidrBlock("10.0.0.0/24").build()
			);

		subnetPrivateId = wSubNetPriv.subnet().subnetId();
		System.out.println("Subnet Privada: " + subnetPrivateId);
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
		gatewayId = wGatewayResponse.internetGateway().internetGatewayId(); 

		//Using the ID from the previous step, attach the Internet gateway to your VPC.
		pClient.attachInternetGateway(
				AttachInternetGatewayRequest.builder().vpcId(vpcId).internetGatewayId(wGatewayResponse.internetGateway().internetGatewayId()).build()
			);

		CreateRouteTableResponse wCreateRouteTableResp = pClient.createRouteTable(CreateRouteTableRequest.builder().vpcId(vpcId).build());

		routeTableId = wCreateRouteTableResp.routeTable().routeTableId();

		// Create a route in the route table that points all traffic (0.0.0.0/0) to the Internet gateway.
		pClient.createRoute(
				CreateRouteRequest.builder().routeTableId(routeTableId).destinationCidrBlock("0.0.0.0/0").gatewayId(gatewayId).build()
			);

		pClient.associateRouteTable(AssociateRouteTableRequest.builder().subnetId(subnetPublicId).routeTableId(routeTableId).build());

		// an instance launched into the subnet automatically receives a public IP address
		pClient.modifySubnetAttribute(ModifySubnetAttributeRequest.builder().subnetId(subnetPublicId).mapPublicIpOnLaunch(AttributeBooleanValue.builder().value(true).build()).build());
	}
	
	public void prepareInstance(Ec2Client pClient) throws Exception
	{
		createKeyPair(pClient);

		configureSecurityGroup(pClient);

		launchInstance(pClient);
	}

	public void launchInstance(Ec2Client pClient) throws Exception
	{
		//aws ec2 run-instances --image-id ami-a4827dc9 --count 1 --instance-type t2.micro --key-name MyKeyPair --security-group-ids sg-e1fb8c9a --subnet-id subnet-b46032ec
		RunInstancesResponse wResp = pClient.runInstances(RunInstancesRequest.builder().imageId("ami-07d9160fa81ccffb5").minCount(1).maxCount(1).instanceType("t2.micro").keyName(keyPairName).securityGroupIds(securityGroupIdPublic).subnetId(subnetPublicId).build());
		
		Instance wInstancia = wResp.instances().get(0);
		instanceIdPublic = wInstancia.instanceId();
		
		System.out.println("ID_INSTANCIA: " + instanceIdPublic);
		System.out.println("IP PUBLIC: '"+wInstancia.publicIpAddress()+"' DNS: '"+wInstancia.publicDnsName()+"'.");
	}
	
	public void configureSecurityGroup(Ec2Client pClient) throws Exception
	{
		// aws ec2 create-security-group --group-name SSHAccess --description "Security group for SSH access" --vpc-id vpc-2f09a348
		CreateSecurityGroupResponse wResp = pClient.createSecurityGroup(CreateSecurityGroupRequest.builder().groupName("BastionSSHAccess").description("SSH Access to Bastion").vpcId(vpcId).build());
		securityGroupIdPublic = wResp.groupId();

		String wMyPublicIP = getMyIPFromAmazon();
		String cidrIp = wMyPublicIP + "/32";
		// aws ec2 authorize-security-group-ingress --group-id sg-e1fb8c9a --protocol tcp --port 22 --cidr 0.0.0.0/0
		pClient.authorizeSecurityGroupIngress(AuthorizeSecurityGroupIngressRequest.builder().groupId(securityGroupIdPublic).ipProtocol("tcp").fromPort(22).toPort(22).cidrIp(cidrIp).build());
	}

	/**
	 * 
	 * @param pClient
	 * @throws Exception
	 */
	public void createKeyPair(Ec2Client pClient) throws Exception
	{
		keyPairName = "JAVA_KEY";
		
		// if exists, remove previous
		DescribeKeyPairsResponse wKeysResponse = pClient.describeKeyPairs();
		for (KeyPairInfo wKeyInfo : wKeysResponse.keyPairs())
			if ("JAVA_KEY".equals(wKeyInfo.keyName()))
				pClient.deleteKeyPair(DeleteKeyPairRequest.builder().keyName(keyPairName).build());
		
		CreateKeyPairResponse wKeyPairResponse = pClient.createKeyPair(CreateKeyPairRequest.builder().keyName(keyPairName).build());
		String wPEMPrivateKey = wKeyPairResponse.keyMaterial();

		File wKeyPairPEM = new File(System.getProperty("user.dir") + File.separator + "KeyPair_" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".pem");

		IOUtils.write(wPEMPrivateKey, new FileOutputStream(wKeyPairPEM), Charset.defaultCharset());

		System.out.println("PRIVATE KEY PEM: '" + wPEMPrivateKey + "'.");
	}

	private static String getMyIPFromAmazon() throws IOException
	{
		URL whatismyip = new URL("http://checkip.amazonaws.com");
		try (BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream())))
		{
			String ip = in.readLine(); //you get the IP as a String
			return ip;
		}
	}
}

