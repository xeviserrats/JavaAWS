package com.xevi.system.TestingAWS.create;

import com.xevi.system.TestingAWS.bean.InfoResourcesBean;
import com.xevi.system.TestingAWS.utils.AWSConstants;
import com.xevi.system.TestingAWS.utils.AWSUtils;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateSubnetRequest;
import software.amazon.awssdk.services.ec2.model.CreateSubnetResponse;
import software.amazon.awssdk.services.ec2.model.CreateVpcRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpcResponse;

public class CreateVPC_PublicSubnets extends BaseCreateResource<Ec2Client> 
{
	public CreateVPC_PublicSubnets(Ec2Client pClient, InfoResourcesBean pBean) 
	{
		super(pClient, pBean);
	}

	@Override
	public void create() 
	{
		// aws ec2 create-vpc --cidr-block 10.0.0.0/16 
		CreateVpcResponse wVPCResponse = client.createVpc(CreateVpcRequest.builder().cidrBlock(AWSConstants.VPC_CIDR). build());

		bean.vpcId = wVPCResponse.vpc().vpcId();

		AWSUtils.addTag(client, bean.vpcId, "Name", "JavaAWSExample");
		
		System.out.println("VPC_ID: "+ bean.vpcId);

		CreateSubnetResponse wSubNetResp = client.createSubnet			(
				CreateSubnetRequest.builder().vpcId(wVPCResponse.vpc().vpcId()).cidrBlock(AWSConstants.SUBNET_PUBLIC_CIDR).build()
			);

		bean.subnetPublicId = wSubNetResp.subnet().subnetId();

		AWSUtils.addTag(client, bean.subnetPublicId, "Name", "SubNetBastion");

		System.out.println("Subnet Public: " + bean.subnetPublicId);
	}

}
