package com.xevi.system.TestingAWS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Scanner;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResponse;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.InstanceStatus;
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.ec2.model.SummaryStatus;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.AddTagsRequest;

public class AWSUtils 
{
	private static String ACCESS_KEYID = "aws.accessKeyId";
	private static String SECRET_KEY = "aws.secretAccessKey";

	public static AwsCredentialsProvider createCredentialsProvider()
	{
		return SystemPropertyCredentialsProvider.create();
	}
	
	public static void addTagELB(ElasticLoadBalancingV2Client pClient, String pResourceArn, String pKey, String pValue)
	{
		pClient.addTags(AddTagsRequest.builder().resourceArns(pResourceArn).tags(software.amazon.awssdk.services.elasticloadbalancingv2.model.Tag.builder().key(pKey).value(pValue).build()).build());
	}

	public static void waitEc2InstanceStatusOK(Ec2Client pClient, String pInstanceId) 
	{
		try
		{
			boolean wEstatOK = false;
			int wIndex = 0;
			do
			{
				wIndex++;
				
				DescribeInstanceStatusResponse wInstanceRes = pClient.describeInstanceStatus(DescribeInstanceStatusRequest.builder().instanceIds(pInstanceId).build());
				if (wInstanceRes.instanceStatuses().size()==1)
				{
					InstanceStatus wInstanceStatus = wInstanceRes.instanceStatuses().get(0);

					wEstatOK = InstanceStateName.RUNNING.equals(wInstanceStatus.instanceState().name());

					/**
					 * If the instance is not running. wait a few seconds
					 */
					if (!wEstatOK)
						try {
							Thread.sleep(2 * 1000 * wIndex);
						}catch(Exception e) {}
				}
				else
					throw new IllegalStateException("Instance Not Found Exception: '"+pInstanceId+"'");
				
			}
			while (!wEstatOK && wIndex<10);
		}
		catch(Exception e)
		{
			throw e;
		}
	}
	
	public static void addTag(Ec2Client pClient, String pResourceId, String pKey, String pValue)
	{
        pClient.createTags(CreateTagsRequest.builder().resources(pResourceId).tags(Tag.builder().key(pKey).value(pValue).build()).build());
	}

	public static Subnet getSubnet(Ec2Client pClient, String pSubnetId) throws IllegalStateException
	{
		DescribeSubnetsResponse wResponse = pClient.describeSubnets(DescribeSubnetsRequest.builder().subnetIds(pSubnetId).build());

		if (wResponse.subnets().size()!=1)
			throw new IllegalStateException("Subnet '"+pSubnetId+"' not found.");

		Subnet wSubnetPrivateOne = wResponse.subnets().get(0);

		return wSubnetPrivateOne;
	}

	/**
	 * Read credentials from keyboard. For security, the credentials are not stored in a file.
	 */
	public static void readCredentials()
	{
		Scanner keyboard = new Scanner(System.in);	

		System.out.println("AWS ACCESS KEY:");
		String wAccessKey = keyboard.nextLine();

		System.out.println("AWS SECRET KEY:");
		String wSecretKey = keyboard.nextLine();

		System.setProperty(ACCESS_KEYID, 	wAccessKey);
		System.setProperty(SECRET_KEY, 		wSecretKey);

		keyboard.close();
	}
	
	public static String getMyIPFromAmazon() throws IOException
	{
		URL whatismyip = new URL("http://checkip.amazonaws.com");
		try (BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream())))
		{
			String ip = in.readLine(); //you get the IP as a String
			return ip;
		}
	}
}
