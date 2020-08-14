package com.xevi.system.TestingAWS;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Address;
import software.amazon.awssdk.services.ec2.model.AssociateRouteTableRequest;
import software.amazon.awssdk.services.ec2.model.CreateNatGatewayRequest;
import software.amazon.awssdk.services.ec2.model.CreateNatGatewayResponse;
import software.amazon.awssdk.services.ec2.model.CreateRouteRequest;
import software.amazon.awssdk.services.ec2.model.CreateRouteTableRequest;
import software.amazon.awssdk.services.ec2.model.CreateRouteTableResponse;
import software.amazon.awssdk.services.ec2.model.DescribeAddressesResponse;
import software.amazon.awssdk.services.ec2.model.Tag;

/**
 * Allow the instances in the private subnet access the internet using a NAT GATEWAY.
 * @author xevi
 */
public class AddAccessInternetPrivateSubnets 
{
	public static final String TAG_ALLOCATION_IP_FOR_GATEWAY_KEY 	= "TYPE";
	public static final String TAG_ALLOCATION_IP_FOR_GATEWAY_VALUE 	= "FOR_GATEWAY";

	/**
	 * Create a Nat Gateway for the two private subnets. The instances onthose subnets needs internet access.
	 * @param pClient
	 * @param pBean
	 * @throws Exception
	 */
	public static void addConnectibityToPrivateSubnets(Ec2Client pClient, InfoElementsBean pBean) throws Exception
	{
		Address wPublicIP = getAddressForNatGateway(pClient);
		CreateNatGatewayResponse wGatewayResponse = pClient.createNatGateway(CreateNatGatewayRequest.builder().subnetId(pBean.subnetPublicId).allocationId(wPublicIP.allocationId()).build());
		
		pBean.privateNatGateway = wGatewayResponse.natGateway().natGatewayId();
		
		System.out.println("GatewayId: " + pBean.privateNatGateway);

		CreateRouteTableResponse wRouteTableResp = pClient.createRouteTable(CreateRouteTableRequest.builder().vpcId(pBean.vpcId).build());

		pBean.privateSubnetsRouteTable = wRouteTableResp.routeTable().routeTableId();

		System.out.println("ROUTE TABLE: " + pBean.privateSubnetsRouteTable);
		AWSUtils.addTag(pClient, pBean.privateSubnetsRouteTable, "Name", "JavaRouteTablePrivate");

		pClient.createRoute(CreateRouteRequest.builder().routeTableId(wRouteTableResp.routeTable().routeTableId()).destinationCidrBlock("0.0.0.0/0").natGatewayId(pBean.privateNatGateway).build());

		pClient.associateRouteTable(AssociateRouteTableRequest.builder().subnetId(pBean.subnetPrivateOneId).routeTableId(wRouteTableResp.routeTable().routeTableId()).build());
		pClient.associateRouteTable(AssociateRouteTableRequest.builder().subnetId(pBean.subnetPrivateTwoId).routeTableId(wRouteTableResp.routeTable().routeTableId()).build());	
	}
	
	/**
	 * Allocate one IP (from the allocated IP) for the nat gateway. A tag is used to identify the IP.
	 * @param pClient
	 * @return
	 * @throws Exception
	 */
	private static Address getAddressForNatGateway(Ec2Client pClient) throws Exception
	{
		DescribeAddressesResponse wDescAdress = pClient.describeAddresses();
		
		if (wDescAdress.addresses().isEmpty())
			throw new IllegalStateException("ALLOCATION IP NOT FOUND");
		
		for (Address wAddress : wDescAdress.addresses())
			if (wAddress.hasTags())
				for (Tag wTag : wAddress.tags())
					if (wTag.key().equals(TAG_ALLOCATION_IP_FOR_GATEWAY_KEY))
						if (wTag.value().equals(TAG_ALLOCATION_IP_FOR_GATEWAY_VALUE))
							return wAddress;

		Address wAddress = wDescAdress.addresses().get(0);
		AWSUtils.addTag(pClient, wAddress.allocationId(), TAG_ALLOCATION_IP_FOR_GATEWAY_KEY, TAG_ALLOCATION_IP_FOR_GATEWAY_VALUE);

		return wAddress;
	}
}
