package Bajaj.example.bajaj;

import Bajaj.example.bajaj.dto.GenerateWebhookRequest;
import Bajaj.example.bajaj.dto.GenerateWebhookResponse;
import Bajaj.example.bajaj.dto.SubmitAnswerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class BajajApplication {

    private static final Logger log = LoggerFactory.getLogger(BajajApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(BajajApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean
    public CommandLineRunner run(RestTemplate restTemplate) {
        return args -> {
            log.info("Application started, executing CommandLineRunner...");

            try {

                String generateWebhookUrl = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";
                String regNo = "1RV22CS224";
                GenerateWebhookRequest request = new GenerateWebhookRequest("Vanshika Khandelwal", regNo, "vanshikak.cs22@rvce.edu.in");

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<GenerateWebhookRequest> requestEntity = new HttpEntity<>(request, headers);

                log.info("Generating webhook...");
                GenerateWebhookResponse response = restTemplate.postForObject(generateWebhookUrl, requestEntity, GenerateWebhookResponse.class);

                if (response == null || response.getWebhook() == null || response.getAccessToken() == null) {
                    log.error("Failed to generate webhook. Response was null or incomplete.");
                    return;
                }

                String webhookUrl = response.getWebhook();
                String accessToken = response.getAccessToken();
                log.info("Webhook URL: {}", webhookUrl);
                log.info("Access Token: {}", accessToken);

                int regNoLastTwoDigits = Integer.parseInt(regNo.substring(regNo.length() - 2));
                String sqlQuestionUrl;
                String finalQuery;
                if (regNoLastTwoDigits % 2 != 0) {
                    sqlQuestionUrl = "https://drive.google.com/file/d/1IeSI6l6KoSQAFfRihIT9tEDICtoz-G/view?usp=sharing";
                    finalQuery = "SELECT p.AMOUNT AS SALARY, CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS NAME, FLOOR(DATEDIFF(CURDATE(), e.DOB) / 365) AS AGE, d.DEPARTMENT_NAME FROM PAYMENTS p JOIN EMPLOYEE e ON p.EMP_ID = e.EMP_ID JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID WHERE DAY(p.PAYMENT_TIME) <> 1 ORDER BY p.AMOUNT DESC LIMIT 1;";
                } else {
                    sqlQuestionUrl = "https://drive.google.com/file/d/143MR5cLFrlNEuHzzWJ5RHnEWuijuM9X/view?usp=sharing";
                    finalQuery = "SELECT e1.EMP_ID, e1.FIRST_NAME, e1.LAST_NAME, d.DEPARTMENT_NAME, COUNT(e2.EMP_ID) AS YOUNGER_EMPLOYEES_COUNT FROM EMPLOYEE e1 JOIN DEPARTMENT d ON e1.DEPARTMENT = d.DEPARTMENT_ID LEFT JOIN EMPLOYEE e2 ON e1.DEPARTMENT = e2.DEPARTMENT AND e2.DOB > e1.DOB GROUP BY e1.EMP_ID, e1.FIRST_NAME, e1.LAST_NAME, d.DEPARTMENT_NAME ORDER BY e1.EMP_ID DESC;";
                }
                log.info("SQL Question Link: {}", sqlQuestionUrl);
                log.info("Using the following SQL query: {}", finalQuery);

                HttpHeaders submissionHeaders = new HttpHeaders();
                submissionHeaders.setContentType(MediaType.APPLICATION_JSON);
                submissionHeaders.set("Authorization", accessToken);

                SubmitAnswerRequest submissionRequest = new SubmitAnswerRequest(finalQuery);
                HttpEntity<SubmitAnswerRequest> submissionEntity = new HttpEntity<>(submissionRequest, submissionHeaders);

                log.info("Submitting final SQL query...");
                restTemplate.exchange(webhookUrl, HttpMethod.POST, submissionEntity, String.class);
                log.info("SQL query submitted successfully!");

            } catch (RestClientException e) {
                log.error("An error occurred during an API call: {}", e.getMessage());
            } catch (NumberFormatException e) {
                log.error("Error parsing registration number: {}", e.getMessage());
            } catch (Exception e) {
                log.error("An unexpected error occurred: {}", e.getMessage(), e);
            }
        };
    }
}
