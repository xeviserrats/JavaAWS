package com.xevi.system.TestingAWS.create;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.IOUtils;

import com.xevi.system.TestingAWS.bean.InfoResourcesBean;
import com.xevi.system.TestingAWS.utils.AWSConstants;
import com.xevi.system.TestingAWS.utils.AWSUtils;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateKeyPairRequest;
import software.amazon.awssdk.services.ec2.model.CreateKeyPairResponse;
import software.amazon.awssdk.services.ec2.model.DescribeKeyPairsResponse;
import software.amazon.awssdk.services.ec2.model.KeyPairInfo;

/**
 * Check if a 'JAVA_KEY' Key Pair exists. If the key is created,  the PEM (Private Key File) is saved in a file.
 * @author xevi
 */
public class CreateKeyPair extends BaseCreateResource<Ec2Client>
{
	public CreateKeyPair(Ec2Client pClient, InfoResourcesBean pBean) 
	{
		super(pClient, pBean);
	}

	@Override
	public void create() 
	{
		bean.keyPairName = AWSConstants.KEYPAIR_NAME;
		
		boolean wExistsKey = false;
		// if exists, remove previous
		DescribeKeyPairsResponse wKeysResponse = client.describeKeyPairs();
		for (KeyPairInfo wKeyInfo : wKeysResponse.keyPairs())
			if ("JAVA_KEY".equals(wKeyInfo.keyName()))
			{
				wExistsKey = true;
				break;
			}
		
		if (!wExistsKey)
		{
			CreateKeyPairResponse wKeyPairResponse = client.createKeyPair(CreateKeyPairRequest.builder().keyName(bean.keyPairName).build());
			AWSUtils.addTag(client, wKeyPairResponse.keyPairId(), bean.keyPairName, bean.keyPairName);

			String wPEMPrivateKey = wKeyPairResponse.keyMaterial();

			File wKeyPairPEM = new File(System.getProperty("user.dir") + File.separator + "KeyPair_" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".pem");

			try	
			{
				IOUtils.write(wPEMPrivateKey, new FileOutputStream(wKeyPairPEM), Charset.defaultCharset());
			}
			catch(IOException e)			{
				throw new IllegalStateException(e);
			}

			System.out.println("PRIVATE KEY PEM: '" + wKeyPairPEM.getAbsolutePath() + "'.");
		}
	}

}
