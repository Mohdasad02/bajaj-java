package com.hiring.webhook.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String name;
    private String regNo;
    private String email;
    private String generateWebhookUrl;
    private String submitWebhookUrl;
    private String finalQueryOdd;
    private String finalQueryEven;
   
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRegNo() { return regNo; }
    public void setRegNo(String regNo) { this.regNo = regNo; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getGenerateWebhookUrl() { return generateWebhookUrl; }
    public void setGenerateWebhookUrl(String generateWebhookUrl) { this.generateWebhookUrl = generateWebhookUrl; }
    public String getSubmitWebhookUrl() { return submitWebhookUrl; }
    public void setSubmitWebhookUrl(String submitWebhookUrl) { this.submitWebhookUrl = submitWebhookUrl; }
    public String getFinalQueryOdd() { return finalQueryOdd; }
    public void setFinalQueryOdd(String finalQueryOdd) { this.finalQueryOdd = finalQueryOdd; }
    public String getFinalQueryEven() { return finalQueryEven; }
    public void setFinalQueryEven(String finalQueryEven) { this.finalQueryEven = finalQueryEven; }
}
