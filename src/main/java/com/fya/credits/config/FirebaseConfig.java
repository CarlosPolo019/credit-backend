package com.fya.credits.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class FirebaseConfig {
  @Bean
  Firestore firestore(
      @Value("${app.firebase.project-id}") String projectId,
      @Value("${app.firebase.client-email}") String clientEmail,
      @Value("${app.firebase.private-key}") String privateKey,
      @Value("${app.firebase.service-account-json}") String serviceAccountJson) throws Exception {
    if (FirebaseApp.getApps().isEmpty()) {
      FirebaseOptions.Builder builder = FirebaseOptions.builder();
      if (StringUtils.hasText(serviceAccountJson)) {
        builder.setCredentials(GoogleCredentials.fromStream(
            new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8))));
      } else if (StringUtils.hasText(clientEmail) && StringUtils.hasText(privateKey)) {
        String json = """
            {"type":"service_account","project_id":"%s","private_key_id":"","private_key":"%s","client_email":"%s","client_id":"","auth_uri":"https://accounts.google.com/o/oauth2/auth","token_uri":"https://oauth2.googleapis.com/token","auth_provider_x509_cert_url":"https://www.googleapis.com/oauth2/v1/certs","client_x509_cert_url":""}
            """.formatted(projectId, privateKey.replace("\\n", "\n"), clientEmail);
        builder.setCredentials(GoogleCredentials.fromStream(
            new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))));
      } else {
        builder.setCredentials(GoogleCredentials.getApplicationDefault());
      }
      if (StringUtils.hasText(projectId)) {
        builder.setProjectId(projectId);
      }
      FirebaseApp.initializeApp(builder.build());
    }
    return FirestoreClient.getFirestore();
  }
}
