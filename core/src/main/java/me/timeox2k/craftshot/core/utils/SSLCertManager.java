package me.timeox2k.craftshot.core.utils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

public class SSLCertManager {


  /*
  Helper Function to disable SSL certificate validation to debug the API etc. on localhost without having to move around with self-signed certificates

  Just add this function in on Enable when needed before making any requests

  DO NOT USE IN PRODUCTION
   */

  public static void disableSSLCertificateValidation() {
    try {
      TrustManager[] trustAllCerts = new TrustManager[]{
          new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
              return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
          }
      };

      SSLContext sc = SSLContext.getInstance("TLS");
      sc.init(null, trustAllCerts, new java.security.SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

      HostnameVerifier allHostsValid = (hostname, session) -> true;
      HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


}
