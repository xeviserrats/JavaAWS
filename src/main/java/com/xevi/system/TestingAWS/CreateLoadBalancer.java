package com.xevi.system.TestingAWS;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AssociateRouteTableRequest;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.CreateRouteRequest;
import software.amazon.awssdk.services.ec2.model.CreateRouteTableRequest;
import software.amazon.awssdk.services.ec2.model.CreateRouteTableResponse;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupResponse;
import software.amazon.awssdk.services.ec2.model.CreateSubnetRequest;
import software.amazon.awssdk.services.ec2.model.CreateSubnetResponse;
import software.amazon.awssdk.services.ec2.model.IpPermission;
import software.amazon.awssdk.services.ec2.model.RouteTable;
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.ec2.model.UserIdGroupPair;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.*;

/**
 * Create a Load Balancer to forward requests to the Linux in the private subnet. 
 * @author xevi
 *
 */
public class CreateLoadBalancer 
{
	private Ec2Client client;
	private InfoElementsBean bean;

	public CreateLoadBalancer(Ec2Client pClient, InfoElementsBean pInfoElementsBean)
	{
		this.client = pClient;
		this.bean = pInfoElementsBean;
	}

	public static void main(String[] args) throws Exception
	{
		new CreateLoadBalancer(null, null).mainTestImpl();
	}
	
	public void mainTestImpl() throws Exception
	{
		try
		{
			bean = new InfoElementsBean();
			bean.vpcId					= "vpc-08676331e2b0dd0a2";
			bean.subnetPrivateOneId 	= "subnet-0081186c6d6f9c95a";
			bean.subnetPrivateTwoId 	= "subnet-0081186c6d6f9c95a";
			bean.securityGroupIdPrivate	= "sg-0f48a68b8edd94936";
			bean.gatewayId 				= "igw-0ac4a603e0b4a81c2";
			bean.instanceIdPrivateOne	= "i-0ba227f523d551761";
			bean.instanceIdPrivateTwo	= "i-01ece65eb7faabcf9";

			
			AWSUtils.readCredentials();
			client = Ec2Client.builder().region(Region.EU_WEST_1).credentialsProvider(AWSUtils.createCredentialsProvider()).build();			

			exec();
		}
		catch(Exception e)
		{
			throw e;
		}
	}
	
	public void exec() throws Exception
	{
		try
		{
			createParallelSubnets();

			createSecurityGroupLoadBalancer();

			createLoadBalancer();
			
			sgPrivateIngressLoadBalancerSG();
		}
		catch(Exception e)
		{
			throw e;
		}
	}

	public void createLoadBalancer()
	{
		try
		{
			//aws elbv2 create-load-balancer --name my-load-balancer  --subnets subnet-12345678 subnet-23456789 --security-groups sg-12345678
			ElasticLoadBalancingV2Client elbClient = ElasticLoadBalancingV2Client.builder().credentialsProvider(AWSUtils.createCredentialsProvider()).build();

			CreateLoadBalancerResponse wCreateResp = elbClient.createLoadBalancer(CreateLoadBalancerRequest.builder().name("TestJavaLoadBalancer")
					.subnets(bean.subnetPublicParallelPrivateOne, bean.subnetPublicParallelPrivateTwo)
					.securityGroups(bean.loadBalancerSecurityGroup)
					.build());

			bean.loadBalancerArn = wCreateResp.loadBalancers().get(0).loadBalancerArn();

			System.out.println("Load Balancer ARN: " + bean.loadBalancerArn);

			AWSUtils.addTagELB(elbClient, bean.loadBalancerArn, "Tipus", "JavaAWSTest");

			// aws elbv2 create-target-group --name my-targets --protocol HTTP --port 80 --vpc-id vpc-12345678
			CreateTargetGroupResponse wCreateTarget = elbClient.createTargetGroup(CreateTargetGroupRequest.builder().name("MyTargets").protocol(ProtocolEnum.HTTP).port(80).vpcId(bean.vpcId).build());

			bean.loadBalancerTargetGroupArn = wCreateTarget.targetGroups().get(0).targetGroupArn();

			AWSUtils.addTagELB(elbClient, bean.loadBalancerTargetGroupArn, "Tipus", "JavaAWSTest");

			// aws elbv2 register-targets --target-group-arn targetgroup-arn --targets Id=i-12345678 Id=i-23456789
			RegisterTargetsResponse wRegTargetResp = elbClient.registerTargets(RegisterTargetsRequest.builder().targetGroupArn(bean.loadBalancerTargetGroupArn)
					.targets(TargetDescription.builder().id(bean.instanceIdPrivateOne).id(bean.instanceIdPrivateTwo).build()).build());

			// aws elbv2 create-listener --load-balancer-arn loadbalancer-arn --protocol HTTP --port 80  --default-actions Type=forward,TargetGroupArn=targetgroup-arn
			CreateListenerResponse wCreateListener = elbClient.createListener(CreateListenerRequest.builder().loadBalancerArn(bean.loadBalancerArn).protocol(ProtocolEnum.HTTP).port(80)
					.defaultActions(Action.builder().type(ActionTypeEnum.FORWARD).targetGroupArn(bean.loadBalancerTargetGroupArn).build()).build());

			System.out.println("LoadBalancer ListenerArn: " + bean.loadBalancerListenerArn);

			bean.loadBalancerListenerArn = wCreateListener.listeners().get(0).listenerArn();

			AWSUtils.addTagELB(elbClient, bean.loadBalancerListenerArn, "Tipus", "JavaAWSTest");
		}
		catch(Exception e)
		{
			throw e;
		}
	}

	public void sgPrivateIngressLoadBalancerSG()
	{
		try
		{
			client.authorizeSecurityGroupIngress(AuthorizeSecurityGroupIngressRequest.builder().groupId(bean.securityGroupIdPrivate)
					.ipPermissions(IpPermission.builder().ipProtocol("tcp").fromPort(80).toPort(80).userIdGroupPairs(UserIdGroupPair.builder().groupId(bean.loadBalancerSecurityGroup).vpcId(bean.vpcId).build()).build())
					.build());	
		}
		catch(Exception e)
		{
			throw e;
		}
	}

	private void createSecurityGroupLoadBalancer()
	{
		try
		{
			CreateSecurityGroupResponse wResp = client.createSecurityGroup(CreateSecurityGroupRequest.builder().groupName("sgLoadBalancer").description("Security Group Load Balancer").vpcId(bean.vpcId).build());
			bean.loadBalancerSecurityGroup = wResp.groupId();

			System.out.println("SecurityGroup Load Balancer: " + wResp.groupId());
			
			AWSUtils.addTag(client, bean.loadBalancerSecurityGroup, "Name", "sgLoadBalancer");

			client.authorizeSecurityGroupIngress(AuthorizeSecurityGroupIngressRequest.builder().groupId(bean.loadBalancerSecurityGroup).ipProtocol("tcp").fromPort(80).toPort(80).cidrIp("0.0.0.0/0").build());
			client.authorizeSecurityGroupIngress(AuthorizeSecurityGroupIngressRequest.builder().groupId(bean.loadBalancerSecurityGroup).ipProtocol("tcp").fromPort(443).toPort(443).cidrIp("0.0.0.0/0").build());
		}
		catch(Exception e)
		{
			throw e;
		}
	}

	/**
	 * For every private subnet, we need to create a public subnet in the same Availability Zone. This is the only way for Load Balancer to pass request to a private subnet
	 */
	private void createParallelSubnets() 
	{
		try
		{
			Subnet wSubnetPrivateOne = AWSUtils.getSubnet(client, bean.subnetPrivateOneId);
			Subnet wSubnetPrivateTwo = AWSUtils.getSubnet(client, bean.subnetPrivateTwoId);

			CreateSubnetResponse wSubNetPublicParallelOne = client.createSubnet			(
					CreateSubnetRequest.builder().vpcId(bean.vpcId).cidrBlock(AWSConstants.SUBNET_PUBLIC_PARALLEL_ONE_CIDR).availabilityZoneId(wSubnetPrivateOne.availabilityZoneId()).build()
				);
			bean.subnetPublicParallelPrivateOne = wSubNetPublicParallelOne.subnet().subnetId();
			System.out.println("Subnet SubnetPublicParallelPrivateOneJava Id: " + bean.subnetPublicParallelPrivateOne);
			
			AWSUtils.addTag(client, bean.subnetPublicParallelPrivateOne, "Name", "SubnetPublicParallelPrivateOneJava");

			CreateSubnetResponse wSubNetPublicParallelTwo = client.createSubnet			(
					CreateSubnetRequest.builder().vpcId(bean.vpcId).cidrBlock(AWSConstants.SUBNET_PUBLIC_PARALLEL_TWO_CIDR).availabilityZoneId(wSubnetPrivateTwo.availabilityZoneId()).build()
				);
			bean.subnetPublicParallelPrivateTwo = wSubNetPublicParallelTwo.subnet().subnetId();
			System.out.println("Subnet SubnetPublicParallelPrivateTwoJava Id: " + bean.subnetPublicParallelPrivateTwo);

			AWSUtils.addTag(client, bean.subnetPublicParallelPrivateTwo, "Name", "SubnetPublicParallelPrivateTwoJava");

			RouteTable wRouteTable = createRouteTableResponse();
			
			System.out.println("RouteTable LoadBalancer: " + wRouteTable.routeTableId());

			client.associateRouteTable(AssociateRouteTableRequest.builder().subnetId(bean.subnetPublicParallelPrivateOne).routeTableId(wRouteTable.routeTableId()).build());
			client.associateRouteTable(AssociateRouteTableRequest.builder().subnetId(bean.subnetPublicParallelPrivateTwo).routeTableId(wRouteTable.routeTableId()).build());
		}
		catch(Exception e)
		{
			throw  e;
		}
	}

	/**
	 * The public subnet used by the loadbalancer needs a route table with an internet gateway in the CIDR 0.0.0.0/0
	 * @return
	 */
	private RouteTable createRouteTableResponse()
	{
		try
		{
			CreateRouteTableResponse wRouteTableResp = client.createRouteTable(CreateRouteTableRequest.builder().vpcId(bean.vpcId).build());
			String wRouteTableId = wRouteTableResp.routeTable().routeTableId();

			AWSUtils.addTag(client, wRouteTableId, "Name", "RouteTablePublicSubnetsToPrivateSubnets");
			client.createRoute(CreateRouteRequest.builder().routeTableId(wRouteTableId).destinationCidrBlock("0.0.0.0/0").gatewayId(bean.gatewayId).build());

			return wRouteTableResp.routeTable();
		}
		catch(Exception e)
		{
			throw e;
		}
	}
}
