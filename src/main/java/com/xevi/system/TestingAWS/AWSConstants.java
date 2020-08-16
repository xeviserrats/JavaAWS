package com.xevi.system.TestingAWS;

public class AWSConstants 
{
	public static final String TAG_ALLOCATION_IP_FOR_GATEWAY_KEY 	= "TYPE";
	public static final String TAG_ALLOCATION_IP_FOR_GATEWAY_VALUE 	= "FOR_GATEWAY";

	public static final String ZONENAME_ONE = "eu-west-1a";
	public static final String ZONENAME_TWO = "eu-west-1b";

	public static final String ZONENAME_ID_ONE = "euw1-az1";
	public static final String ZONENAME_ID_TWO = "euw1-az2";

	public static final String VPC_CIDR 							= "10.0.0.0/16";
	public static final String SUBNET_PUBLIC_CIDR 					= "10.0.0.0/24";
	public static final String SUBNET_PRIVATE_ONE_CIDR 				= "10.0.1.0/24";
	public static final String SUBNET_PRIVATE_TWO_CIDR 				= "10.0.2.0/24";

	public static final String SUBNET_PUBLIC_PARALLEL_ONE_CIDR 		= "10.0.10.0/27";
	public static final String SUBNET_PUBLIC_PARALLEL_TWO_CIDR 		= "10.0.11.0/27";

	public static final String EC2_INSTANCE_LINUX_AMAZON2_AMIID = "ami-07d9160fa81ccffb5";
	public static final String EC2_INSTANCE_TYPE 				= "t2.micro";
}
