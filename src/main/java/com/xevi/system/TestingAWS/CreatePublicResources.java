package com.xevi.system.TestingAWS;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupRequest;
import software.amazon.awssdk.services.ec2.model.CreateSecurityGroupResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;

/**
 * Create teh security Groups and the instance used by the bastion. <br>
 * The bastion is the only instance allowed to connect to the private subnet.
 * @author xevi
 */
public class CreatePublicResources 
{
	private InfoElementsBean infoElementsBean = null;

	public CreatePublicResources(InfoElementsBean pBean)
	{
		infoElementsBean = pBean;
	}

	public void prepareInstanceBastion(Ec2Client pClient) throws Exception
	{
		configureSecurityGroupBastion(pClient);

		launchInstanceBastion(pClient);
	}

	/**
	 * Launch the LINUX Bastion. This is the only machine who can access to the Linux Private SSH Port.
	 * @param pClient
	 * @throws Exception
	 */
	private void launchInstanceBastion(Ec2Client pClient) throws Exception
	{
		RunInstancesResponse wResp = pClient.runInstances(RunInstancesRequest.builder().imageId(CreateResources.EC2_INSTANCE_LINUX_AMAZON2_AMIID).minCount(1).maxCount(1)
				.instanceType(CreateResources.EC2_INSTANCE_TYPE).keyName(infoElementsBean.keyPairName).securityGroupIds(infoElementsBean.securityGroupIdPublic).subnetId(infoElementsBean.subnetPublicId).build());

		Instance wInstancia = wResp.instances().get(0);
		infoElementsBean.instanceIdPublic = wInstancia.instanceId();

		AWSUtils.addTag(pClient, wInstancia.instanceId(), "TIPUS", "BASTION");

		System.out.println("ID_INSTANCIA: " + infoElementsBean.instanceIdPublic);
		System.out.println("IP PUBLIC: '"+wInstancia.publicIpAddress()+"' DNS: '"+wInstancia.publicDnsName()+"'.");
	}

	/**
	 * ONly the local IP (from de developement machine) is allowed to access the bastion.
	 * @param pClient
	 * @throws Exception
	 */
	private void configureSecurityGroupBastion(Ec2Client pClient) throws Exception
	{
		// aws ec2 create-security-group --group-name SSHAccess --description "Security group for SSH access" --vpc-id vpc-2f09a348
		CreateSecurityGroupResponse wResp = pClient.createSecurityGroup(CreateSecurityGroupRequest.builder().groupName("BastionSSHAccess").description("SSH Access to Bastion").vpcId(infoElementsBean.vpcId).build());
		infoElementsBean.securityGroupIdPublic = wResp.groupId();

		String wMyPublicIP = AWSUtils.getMyIPFromAmazon();
		String cidrIp = wMyPublicIP + "/32";
		// aws ec2 authorize-security-group-ingress --group-id sg-e1fb8c9a --protocol tcp --port 22 --cidr 0.0.0.0/0
		pClient.authorizeSecurityGroupIngress(AuthorizeSecurityGroupIngressRequest.builder().groupId(infoElementsBean.securityGroupIdPublic).ipProtocol("tcp").fromPort(22).toPort(22).cidrIp(cidrIp).build());
	}
}
