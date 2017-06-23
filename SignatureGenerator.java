import java.io.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.security.cert.*;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.PrivateKey;
import java.security.Security;

import java.util.Enumeration;

import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;

import org.bouncycastle.util.encoders.Base64;

import java.util.ArrayList;


public class SignatureGenerator
{
    public static void main(String args[]) throws Exception {        
        File fileIn = new File(args[0]);
        char inphrase[] = args[1].toCharArray();
        char outphrase[] = args[1].toCharArray();
        File fileOut = new File(args[2]);

        KeyStore pkcs12KeyStore = KeyStore.getInstance("pkcs12");
        KeyStore jksKeyStore = KeyStore.getInstance("jks");
        
        
        pkcs12KeyStore.load(new FileInputStream(fileIn), inphrase);
        jksKeyStore.load(fileOut.exists() ? ((java.io.InputStream) (new FileInputStream(fileOut))) : null, outphrase);
        Enumeration eAliases = pkcs12KeyStore.aliases();
        int n = 0;
        do
        {
            if(!eAliases.hasMoreElements())
                break;
            String strAlias = (String)eAliases.nextElement();
            if(pkcs12KeyStore.isKeyEntry(strAlias))
            {
                java.security.Key key = pkcs12KeyStore.getKey(strAlias, inphrase);
                Certificate chain[] = pkcs12KeyStore.getCertificateChain(strAlias);
                jksKeyStore.setKeyEntry(strAlias, key, outphrase, chain);
            }
        } while(true);
        OutputStream jksFile = new FileOutputStream(fileOut);
        jksKeyStore.store(jksFile, outphrase);
        jksFile.close();


        KeyStore newJksKeyStore = KeyStore.getInstance("jks");
        InputStream newjksFile = new FileInputStream(args[2]);

        try {
                char[] password=args[1].toCharArray();
                newJksKeyStore.load(newjksFile, password);
            } catch (IOException e) {
            } finally {

            }
        Enumeration e = newJksKeyStore.aliases();
        String alias = "";
        if(e!=null)
            {
                while (e.hasMoreElements())
                {
                    String  aliasN = (String)e.nextElement();
                    if (newJksKeyStore.isKeyEntry(aliasN))
                    {
                        alias = aliasN;
                    }
                }
            }
            PrivateKey privateKey=(PrivateKey) newJksKeyStore.getKey(alias, args[1].toCharArray());
            X509Certificate myPubCert=(X509Certificate) newJksKeyStore.getCertificate(alias);
            byte[] dataToSign=args[3].getBytes();
            CMSSignedDataGenerator sgen = new CMSSignedDataGenerator();
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider ());
            sgen.addSigner(privateKey, myPubCert,CMSSignedDataGenerator.DIGEST_SHA1);
            Certificate[] certChain =newJksKeyStore.getCertificateChain(alias);
            ArrayList certList = new ArrayList();
            CertStore certs = null;
            for (int i=0; i < certChain.length; i++)
                certList.add(certChain[i]); 
            sgen.addCertificatesAndCRLs(CertStore.getInstance("Collection", new CollectionCertStoreParameters(certList), "BC"));
            CMSSignedData csd = sgen.generate(new CMSProcessableByteArray(dataToSign),true, "BC");
            byte[] signedData = csd.getEncoded();
            byte[] signedData64 = Base64.encode(signedData); 
            FileOutputStream signatureFile = new FileOutputStream(args[4]);
            signatureFile.write(signedData64);
            signatureFile.close(); 
    }
}