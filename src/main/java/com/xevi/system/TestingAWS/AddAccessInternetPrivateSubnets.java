package com.xevi.system.TestingAWS;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Address;
import software.amazon.awssdk.services.ec2.model.AllocateAddressRequest;
import software.amazon.awssdk.services.ec2.model.AssociateRouteTableRequest;
import software.amazon.awssdk.services.ec2.model.CreateNatGatewayRequest;
import software.amazon.awssdk.services.ec2.model.CreateNatGatewayResponse;
import software.amazon.awssdk.services.ec2.model.CreateRouteRequest;
import software.amazon.awssdk.services.ec2.model.CreateRouteTableRequest;
import software.amazon.awssdk.services.ec2.model.CreateRouteTableResponse;
import software.amazon.awssdk.services.ec2.model.DescribeAddressesResponse;
import software.amazon.awssdk.services.ec2.model.DomainType;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Tag;

/**
 * Allow the instances in the private subnet access the internet using a NAT GATEWAY.
 * @author xevi
 */
public class AddAccessInternetPrivateSubnets 
{

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

		createRoute(pClient, pBean);

		pClient.associateRouteTable(AssociateRouteTableRequest.builder().subnetId(pBean.subnetPrivateOneId).routeTableId(wRouteTableResp.routeTable().routeTableId()).build());
		pClient.associateRouteTable(AssociateRouteTableRequest.builder().subnetId(pBean.subnetPrivateTwoId).routeTableId(wRouteTableResp.routeTable().routeTableId()).build());	
	}
	
	/**
	 * Retry the createRoute to be sure the NAT GATEWAY is created and it can be used.
	 * @param pClient
	 * @param pBean
	 * @throws Exception
	 */
	private static void createRoute(Ec2Client pClient, InfoElementsBean pBean) throws Exception
	{
		// we need to wait for NAT GATEWAY being created
		Thread.sleep(5000);

		boolean wIsException = true;
		int wNumRetry = 0;
		do
		{
			try
			{
				wNumRetry++;
				Thread.sleep(1000 * wNumRetry);
				pClient.createRoute(CreateRouteRequest.builder().routeTableId(pBean.privateSubnetsRouteTable).destinationCidrBlock("0.0.0.0/0").natGatewayId(pBean.privateNatGateway).build());
				
				wIsException = false;
			}
			catch(Ec2Exception e)
			{
				System.out.println("MsgCreateGateway: " + e.getMessage());
				
				// if this is the error, retry !!!
				//.Ec2Exception: The natGateway ID 'nat-0c2942ba859b62f6b' does not exist 

				if (!e.getMessage().contains("does not exist"))
					throw e;
			}
		}
		while (wIsException==true && wNumRetry<6);
		
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
		{
			pClient.allocateAddress(AllocateAddressRequest.builder().domain(DomainType.VPC).build());
			wDescAdress = pClient.describeAddresses();
		}
		
		for (Address wAddress : wDescAdress.addresses())
			if (wAddress.hasTags())
				for (Tag wTag : wAddress.tags())
					if (wTag.key().equals(AWSConstants.TAG_ALLOCATION_IP_FOR_GATEWAY_KEY))
						if (wTag.value().equals(AWSConstants.TAG_ALLOCATION_IP_FOR_GATEWAY_VALUE))
							return wAddress;

		Address wAddress = wDescAdress.addresses().get(0);
		AWSUtils.addTag(pClient, wAddress.allocationId(), AWSConstants.TAG_ALLOCATION_IP_FOR_GATEWAY_KEY, AWSConstants.TAG_ALLOCATION_IP_FOR_GATEWAY_VALUE);

		return wAddress;
	}
}
