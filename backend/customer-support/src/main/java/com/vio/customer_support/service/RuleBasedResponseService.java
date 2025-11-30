package com.vio.customer_support.service;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class RuleBasedResponseService {

    private final Map<String, String> rules = new HashMap<>();

    public RuleBasedResponseService() {
        initializeRules();
    }

    private void initializeRules() {
        // Greeting rules
        rules.put("hello|hi|hey", "Hello! How can I help you with your energy monitoring today?");
        rules.put("good morning|good afternoon|good evening", "Good day! How may I assist you?");

        // Device related
        rules.put("how.*add.*device|create.*device",
                "To add a device, navigate to the admin dashboard and click 'Add Device'. Fill in the device details including name, location, and maximum consumption.");
        rules.put("delete.*device|remove.*device",
                "To delete a device, go to the devices table in the admin dashboard and click the delete button next to the device you want to remove.");
        rules.put("assign.*device",
                "You can assign a device to a user by clicking the assign button in the devices table and selecting the client user.");

        // User related
        rules.put("create.*user|add.*user|new.*user",
                "To create a new user, go to the admin dashboard, click 'Add User', and fill in their details including username, password, and role (CLIENT or ADMIN).");
        rules.put("change.*password|reset.*password",
                "To change a password, edit the user in the admin dashboard and enter a new password in the password field.");
        rules.put("user.*role|client.*admin",
                "There are two user roles: CLIENT (can view assigned devices) and ADMIN (can manage all users and devices).");

        // Consumption related
        rules.put("view.*consumption|see.*consumption|energy.*data",
                "To view energy consumption, click on a device and then click 'View Consumption'. You can select different dates to see historical data.");
        rules.put("overconsumption|exceeded.*limit|too.*much.*energy",
                "When a device exceeds its maximum consumption limit, you'll receive a notification. Check your notifications icon in the dashboard.");
        rules.put("maximum.*consumption|consumption.*limit",
                "The maximum consumption is set when creating or editing a device. It represents the hourly consumption limit in kWh.");

        // Navigation
        rules.put("dashboard|home.*page",
                "The dashboard shows your devices (for clients) or all system devices (for admins). You can manage devices and view consumption from there.");
        rules.put("logout|sign.*out",
                "To logout, click the logout button in the top-right corner of the dashboard.");

        // Technical issues
        rules.put("not.*working|error|problem|issue",
                "I'm sorry you're experiencing issues. Could you please provide more details about the problem? An administrator will assist you shortly.");
        rules.put("slow|loading",
                "If the system is slow, try refreshing the page. If the problem persists, contact the administrator.");

        // Help and support
        rules.put("help|support|assist",
                "I'm here to help! You can ask me about managing devices, users, viewing consumption data, or any other features of the Energy Management System.");
        rules.put("thank|thanks",
                "You're welcome! Let me know if you need anything else.");
    }

    public String getResponse(String userMessage) {
        String normalizedMessage = userMessage.toLowerCase().trim();

        for (Map.Entry<String, String> entry : rules.entrySet()) {
            if (normalizedMessage.matches(".*(" + entry.getKey() + ").*")) {
                return entry.getValue();
            }
        }

        return null; // No rule matched
    }
}