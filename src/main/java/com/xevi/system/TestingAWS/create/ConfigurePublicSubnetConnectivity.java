package com.xevi.system.TestingAWS.create;

import com.xevi.system.TestingAWS.bean.InfoResourcesBean;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AssociateRouteTableRequest;
import software.amazon.awssdk.services.ec2.model.AttachInternetGatewayRequest;
import software.amazon.awssdk.services.ec2.model.AttributeBooleanValue;
import software.amazon.awssdk.services.ec2.model.CreateInternetGatewayResponse;
import software.amazon.awssdk.services.ec2.model.CreateRouteRequest;
import software.amazon.awssdk.services.ec2.model.CreateRouteTableRequest;
import software.amazon.awssdk.services.ec2.model.CreateRouteTableResponse;
import software.amazon.awssdk.services.ec2.model.ModifySubnetAttributeRequest;

/**
 * Creates an Internet Gateway and the route tables for the public subnets
 * @author xevi
 */
public class ConfigurePublicSubnetConnectivity extends BaseCreateResource<Ec2Client> 
{
	public ConfigurePublicSubnetConnectivity(Ec2Client pClient, InfoResourcesBean pBean) 
	{
		super(pClient, pBean);
	}

	@Override
	public void create() 
	{
		// Create an Internet gateway.
		CreateInternetGatewayResponse wGatewayResponse = client.createInternetGateway();
		bean.gatewayId = wGatewayResponse.internetGateway().internetGatewayId(); 

		//Using the ID from the previous step, attach the Internet gateway to your VPC.
		client.attachInternetGateway(
				AttachInternetGatewayRequest.builder().vpcId(bean.vpcId).internetGatewayId(wGatewayResponse.internetGateway().internetGatewayId()).build()
			);

		CreateRouteTableResponse wCreateRouteTableResp = client.createRouteTable(CreateRouteTableRequest.builder().vpcId(bean.vpcId).build());

		bean.routeTableId = wCreateRouteTableResp.routeTable().routeTableId();

		// Create a route in the route table that points all traffic (0.0.0.0/0) to the Internet gateway.
		client.createRoute(
				CreateRouteRequest.builder().routeTableId(bean.routeTableId).destinationCidrBlock("0.0.0.0/0").gatewayId(bean.gatewayId).build()
			);

		client.associateRouteTable(AssociateRouteTableRequest.builder().subnetId(bean.subnetPublicId).routeTableId(bean.routeTableId).build());

		// an instance launched into the subnet automatically receives a public IP address
		client.modifySubnetAttribute(ModifySubnetAttributeRequest.builder().subnetId(bean.subnetPublicId).mapPublicIpOnLaunch(AttributeBooleanValue.builder().value(true).build()).build());

	}

}
