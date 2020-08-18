package com.xevi.system.TestingAWS.create.elb;

import com.xevi.system.TestingAWS.bean.InfoResourcesBean;
import com.xevi.system.TestingAWS.create.BaseCreateResource;
import com.xevi.system.TestingAWS.utils.AWSConstants;
import com.xevi.system.TestingAWS.utils.AWSUtils;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AssociateRouteTableRequest;
import software.amazon.awssdk.services.ec2.model.CreateRouteRequest;
import software.amazon.awssdk.services.ec2.model.CreateRouteTableRequest;
import software.amazon.awssdk.services.ec2.model.CreateRouteTableResponse;
import software.amazon.awssdk.services.ec2.model.CreateSubnetRequest;
import software.amazon.awssdk.services.ec2.model.CreateSubnetResponse;
import software.amazon.awssdk.services.ec2.model.RouteTable;
import software.amazon.awssdk.services.ec2.model.Subnet;

/**
 * Internet facing Elastic Load Balancers can only send request to public subnets. <br>
 * We create two subnets, each in the same AZ as one of the private subnets. <br>
 * 
 * @author xevi
 */
public class CreateELBParallelSubnets extends BaseCreateResource<Ec2Client> 
{
	protected CreateELBParallelSubnets(Ec2Client pClient, InfoResourcesBean pBean) 
	{
		super(pClient, pBean);
	}

	@Override
	public void create() 
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
