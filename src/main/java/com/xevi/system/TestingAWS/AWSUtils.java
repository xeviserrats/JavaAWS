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
import software.amazon.awssdk.services.ec2.model.Tag;

public class AWSUtils 
{
	private static String ACCESS_KEYID = "aws.accessKeyId";
	private static String SECRET_KEY = "aws.secretAccessKey";

	public static AwsCredentialsProvider createCredentialsProvider()
	{
		return SystemPropertyCredentialsProvider.create();
	}
	
	public static void addTag(Ec2Client pClient, String pResourceId, String pKey, String pValue) throws Exception
	{
        pClient.createTags(CreateTagsRequest.builder().resources(pResourceId).tags(Tag.builder().key(pKey).value(pValue).build()).build());
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
