package com.xevi.system.TestingAWS;

/**
 * AWS identifiers: EC2, routes, VPC, subnets ...
 * @author xevi
 */
public class InfoElementsBean 
{
	public String vpcId 					= null;
	public String routeTableId 				= null;
	public String subnetPublicId 			= null;
	public String subnetPrivateOneId		= null;
	public String subnetPrivateTwoId		= null;

	public String subnetPublicParallelPrivateOne = null;
	public String subnetPublicParallelPrivateTwo = null;

	public String loadBalancerSecurityGroup		= null;
	public String loadBalancerArn				= null;
	public String loadBalancerTargetGroupArn	= null;
	public String loadBalancerListenerArn		= null;
	public String loadBalancerDNS				= null;

	public String gatewayId 				= null;
	public String securityGroupIdPublic 	= null;
	public String securityGroupIdPrivate 	= null;

	public String privateNatGateway 		= null;
	public String privateSubnetsRouteTable 	= null;

	public String keyPairName				= null;

	public String instanceIdPublic 			= null;
	public String instanceIdPrivateOne 		= null;
	public String instanceIdPrivateTwo 		= null;
}
