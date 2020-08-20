package com.xevi.system.TestingAWS.create.elb;

import com.xevi.system.TestingAWS.bean.InfoResourcesBean;
import com.xevi.system.TestingAWS.create.BaseCreateResource;
import com.xevi.system.TestingAWS.utils.AWSConstants;
import com.xevi.system.TestingAWS.utils.AWSUtils;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Action;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ActionTypeEnum;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateListenerRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateListenerResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateLoadBalancerRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateLoadBalancerResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateTargetGroupRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.CreateTargetGroupResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeLoadBalancersResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ModifyTargetGroupRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ProtocolEnum;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.RegisterTargetsRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TargetDescription;

/**
 * Create the Elastic Load Balancer:<br>
 * 1.- Load Balancer<br>
 * 2.- Target Group<br>
 * 3.- Listener<br>
 * 
 * @author xevi
 */
public class CreateELSImpl extends BaseCreateResource<ElasticLoadBalancingV2Client> 
{
	private Ec2Client ec2Client = null;

	/**
	 * Don't instance this clas outside the package.
	 * @param pClient
	 * @param pBean
	 */
	protected CreateELSImpl(ElasticLoadBalancingV2Client pClient, InfoResourcesBean pBean) 
	{
		super(pClient, pBean);
	}

	public CreateELSImpl setEc2Client(Ec2Client pClient)
	{
		this.ec2Client = pClient;
		return this;
	}

	@Override
	public void create() 
	{
		//aws elbv2 create-load-balancer --name my-load-balancer  --subnets subnet-12345678 subnet-23456789 --security-groups sg-12345678
		CreateLoadBalancerResponse wCreateResp = client.createLoadBalancer(CreateLoadBalancerRequest.builder().name("TestJavaLoadBalancer")
				.subnets(bean.subnetPublicParallelPrivateOne, bean.subnetPublicParallelPrivateTwo)
				.securityGroups(bean.loadBalancerSecurityGroup)
				.build());

		bean.loadBalancerArn = wCreateResp.loadBalancers().get(0).loadBalancerArn();

		System.out.println("Load Balancer ARN: " + bean.loadBalancerArn);

		AWSUtils.addTagELB(client, bean.loadBalancerArn, "Tipus", "JavaAWSTest");

		// aws elbv2 create-target-group --name my-targets --protocol HTTP --port 80 --vpc-id vpc-12345678
		CreateTargetGroupResponse wCreateTarget = client.createTargetGroup(CreateTargetGroupRequest.builder().name("MyTargets").protocol(ProtocolEnum.HTTP).port(8080).vpcId(bean.vpcId).build());

		bean.loadBalancerTargetGroupArn = wCreateTarget.targetGroups().get(0).targetGroupArn();

		AWSUtils.addTagELB(client, bean.loadBalancerTargetGroupArn, "Tipus", "JavaAWSTest");

		// aws elbv2 register-targets --target-group-arn targetgroup-arn --targets Id=i-12345678 Id=i-23456789
		
		AWSUtils.waitEc2InstanceStatusOK(ec2Client, bean.instanceIdPrivateOne);
		AWSUtils.waitEc2InstanceStatusOK(ec2Client, bean.instanceIdPrivateTwo);
		
		client.registerTargets(RegisterTargetsRequest.builder().targetGroupArn(bean.loadBalancerTargetGroupArn).targets(TargetDescription.builder().id(bean.instanceIdPrivateOne).build()).build());
		client.registerTargets(RegisterTargetsRequest.builder().targetGroupArn(bean.loadBalancerTargetGroupArn).targets(TargetDescription.builder().id(bean.instanceIdPrivateTwo).build()).build());

		// aws elbv2 create-listener --load-balancer-arn loadbalancer-arn --protocol HTTP --port 8080  --default-actions Type=forward,TargetGroupArn=targetgroup-arn
		CreateListenerResponse wCreateListener = client.createListener(CreateListenerRequest.builder().loadBalancerArn(bean.loadBalancerArn).protocol(ProtocolEnum.HTTP).port(80)
				.defaultActions(Action.builder().type(ActionTypeEnum.FORWARD).targetGroupArn(bean.loadBalancerTargetGroupArn).build()).build());

		// aws elb configure-health-check --load-balancer-name my-load-balancer --health-check Target=HTTP:80/ping,Interval=30,UnhealthyThreshold=2,HealthyThreshold=2,Timeout=3
		// HEALTH CHECK, REQUEST TO THE "getInfoServer": /Example/getInfoServer 
		client.modifyTargetGroup(
				ModifyTargetGroupRequest.builder().targetGroupArn(bean.loadBalancerTargetGroupArn)
					.healthCheckProtocol(ProtocolEnum.HTTP)
					.healthCheckPort("8080")
					.healthCheckPath(AWSConstants.LOADBALANCER_HEALTHCHECK_PATH) 
					.healthCheckIntervalSeconds(15).unhealthyThresholdCount(2).healthCheckTimeoutSeconds(3).build()
			);
		
		System.out.println("LoadBalancer ListenerArn: " + bean.loadBalancerListenerArn);

		bean.loadBalancerListenerArn = wCreateListener.listeners().get(0).listenerArn();

		DescribeLoadBalancersResponse wLoadBalancerResp = client.describeLoadBalancers(DescribeLoadBalancersRequest.builder().loadBalancerArns(bean.loadBalancerArn).build());
		bean.loadBalancerDNS = wLoadBalancerResp.loadBalancers().get(0).dnsName();

		System.out.println("LoadBalancer DNS: '"+bean.loadBalancerDNS+"'.");
		System.out.println("App URL: http://" + bean.loadBalancerDNS + "/Example/getInfoServer");
	}
}
