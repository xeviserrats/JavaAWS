package com.xevi.system.TestingAWS.create;

import com.xevi.system.TestingAWS.bean.InfoResourcesBean;
import com.xevi.system.TestingAWS.utils.AWSConstants;
import com.xevi.system.TestingAWS.utils.AWSUtils;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;

public class CreateEc2InstanceBastion extends BaseCreateResource<Ec2Client> 
{
	public CreateEc2InstanceBastion(Ec2Client pClient, InfoResourcesBean pBean) 
	{
		super(pClient, pBean);
	}

	@Override
	public void create() 
	{
		configureSecurityGroupBastion();

		launchInstanceBastion();
	}

	/**
	 * Launch the LINUX Bastion. This is the only machine who can access to the Linux Private SSH Port.
	 * @param pClient
	 * @throws Exception
	 */
	private void launchInstanceBastion() 
	{
		RunInstancesResponse wResp = client.runInstances(RunInstancesRequest.builder().imageId(AWSConstants.EC2_INSTANCE_LINUX_AMAZON2_AMIID).minCount(1).maxCount(1)
				.instanceType(AWSConstants.EC2_INSTANCE_TYPE).keyName(bean.keyPairName).securityGroupIds(bean.securityGroupIdPublic)
				.privateIpAddress(AWSConstants.EC2_PRIVATE_IP_BASTION)
				.subnetId(bean.subnetPublicId).build());

		Instance wInstancia = wResp.instances().get(0);
		bean.instanceIdPublic = wInstancia.instanceId();

		AWSUtils.addTag(client, wInstancia.instanceId(), "Name", "LinuxBastion");

		System.out.println("ID_INSTANCIA: " + bean.instanceIdPublic);
		System.out.println("IP PUBLIC: '"+wInstancia.publicIpAddress()+"' DNS: '"+wInstancia.publicDnsName()+"'.");
	}

	/**
	 * Only the local IP (from de developement machine) is allowed to access the bastion.
	 * @param pClient
	 * @throws Exception
	 */
	private void configureSecurityGroupBastion() 
	{
		// aws ec2 create-security-group --group-name SSHAccess --description "Security group for SSH access" --vpc-id vpc-2f09a348
		CreateSecurityGroupResponse wResp = client.createSecurityGroup(CreateSecurityGroupRequest.builder().groupName("BastionSSHAccess").description("SSH Access to Bastion").vpcId(bean.vpcId).build());
		bean.securityGroupIdPublic = wResp.groupId();

		AWSUtils.addTag(client, bean.securityGroupIdPublic, "Name", "sgBastion");
		
		String wMyPublicIP = AWSUtils.getMyIPFromAmazon();
		String cidrIp = wMyPublicIP + "/32";
		// aws ec2 authorize-security-group-ingress --group-id sg-e1fb8c9a --protocol tcp --port 22 --cidr 0.0.0.0/0
		client.authorizeSecurityGroupIngress(AuthorizeSecurityGroupIngressRequest.builder().groupId(bean.securityGroupIdPublic).ipProtocol("tcp").fromPort(22).toPort(22).cidrIp(cidrIp).build());
	}
}
