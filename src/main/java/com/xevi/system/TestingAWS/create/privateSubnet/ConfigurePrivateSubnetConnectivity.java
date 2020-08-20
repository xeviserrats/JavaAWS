package com.xevi.system.TestingAWS.create.privateSubnet;

import com.xevi.system.TestingAWS.bean.InfoResourcesBean;
import com.xevi.system.TestingAWS.create.BaseCreateResource;
import com.xevi.system.TestingAWS.utils.AWSConstants;
import com.xevi.system.TestingAWS.utils.AWSUtils;

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
 * Creates the NAT Gateway for the two private subnets.
 * @author xevi
 */
public class ConfigurePrivateSubnetConnectivity extends BaseCreateResource<Ec2Client>
{
	public ConfigurePrivateSubnetConnectivity(Ec2Client pClient, InfoResourcesBean pBean) 
	{
		super(pClient, pBean);
	}

	@Override
	public void create() 
	{
		Address wPublicIP = getAddressForNatGateway(client);
		CreateNatGatewayResponse wGatewayResponse = client.createNatGateway(CreateNatGatewayRequest.builder().subnetId(bean.subnetPublicId).allocationId(wPublicIP.allocationId()).build());
		
		bean.privateNatGateway = wGatewayResponse.natGateway().natGatewayId();
		
		System.out.println("GatewayId: " + bean.privateNatGateway);

		CreateRouteTableResponse wRouteTableResp = client.createRouteTable(CreateRouteTableRequest.builder().vpcId(bean.vpcId).build());

		bean.privateSubnetsRouteTable = wRouteTableResp.routeTable().routeTableId();

		System.out.println("ROUTE TABLE: " + bean.privateSubnetsRouteTable);
		AWSUtils.addTag(client, bean.privateSubnetsRouteTable, "Name", "JavaRouteTablePrivate");

		createRoute(client, bean);

		client.associateRouteTable(AssociateRouteTableRequest.builder().subnetId(bean.subnetPrivateOneId).routeTableId(wRouteTableResp.routeTable().routeTableId()).build());
		client.associateRouteTable(AssociateRouteTableRequest.builder().subnetId(bean.subnetPrivateTwoId).routeTableId(wRouteTableResp.routeTable().routeTableId()).build());	
	}
	
	/**
	 * Retry the createRoute to be sure the NAT GATEWAY is created and it can be used.
	 * @param pClient
	 * @param pBean
	 * @throws Exception
	 */
	private static void createRoute(Ec2Client pClient, InfoResourcesBean pBean)  
	{
		// we need to wait for NAT GATEWAY being created
		AWSUtils.sleep(5000);

		boolean wIsException = true;
		int wNumRetry = 0;
		do
		{
			try
			{
				wNumRetry++;
				AWSUtils.sleep(1000 * wNumRetry);
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
	private static Address getAddressForNatGateway(Ec2Client pClient) 
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
