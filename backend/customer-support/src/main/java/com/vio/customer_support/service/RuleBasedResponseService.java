package com.vio.customer_support.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@Slf4j
public class RuleBasedResponseService {

    private final Map<Pattern, String> rules = new HashMap<>();

    public RuleBasedResponseService() {
        initializeRules();
    }

    private void initializeRules() {
        rules.put(Pattern.compile("(?i).*(hello|hi|hei|hey|greetings).*"), "Hello! Welcome to Energy Management System support. How can I help you today?");
        rules.put(Pattern.compile("(?i).*(register|add|setup)\\s+(device|meter|sensor).*"), "To register a new device, please contact your administrator. Only admins can add new devices to the system.");
        rules.put(Pattern.compile("(?i).*(view|see|check|show)\\s+(consumption|usage|energy).*"), "You can view your energy consumption in the Monitoring section of your dashboard. It shows hourly consumption data for all your assigned devices.");
        rules.put(Pattern.compile("(?i).*(high|too much|excessive)\\s+(consumption|usage|energy).*"), "If you're experiencing high energy consumption, check your device settings and ensure there are no unusual patterns. You'll receive alerts if consumption exceeds your device's maximum threshold.");
        rules.put(Pattern.compile("(?i).*(alert|notification|warning|notif).*"), "You'll receive real-time alerts when your device consumption exceeds the configured maximum threshold. Make sure notifications are enabled in your browser.");
        rules.put(Pattern.compile("(?i).*(forgot|reset|change)\\s+password.*"), "For password reset, please contact your system administrator. They can update your credentials in the user management section.");
        rules.put(Pattern.compile("(?i).*(assign|connect|link)\\s+device.*"), "Device assignment is handled by administrators. Contact them to assign or reassign devices to your account.");
        rules.put(Pattern.compile("(?i).*(can't|cannot|unable)\\s+(login|access|log in).*"), "If you're having trouble logging in, verify your username and password. If the issue persists, contact your administrator for assistance.");
        rules.put(Pattern.compile("(?i).*(wrong|incorrect|inaccurate)\\s+(data|reading|value).*"), "Device data is collected in real-time from your smart meters. If you notice inaccuracies, there might be a device issue. Please report this to your administrator.");
        rules.put(Pattern.compile("(?i).*(update|change|modify)\\s+(profile|account|info).*"), "To update your account information, contact your system administrator. They can modify user details in the user management system.");
        rules.put(Pattern.compile("(?i).*(help|support|assist|guide).*"), "I'm here to help! You can ask about viewing consumption, understanding alerts, device information, or any general questions about the Energy Management System.");
        rules.put(Pattern.compile("(?i).*(bye|goodbye|thanks|thank you).*"), "You're welcome! If you need further assistance, feel free to reach out anytime. Have a great day!");

        log.info("Initialized {} rule-based responses", rules.size());
    }

    public String getResponse(String message) {
        if (message == null || message.trim().isEmpty()) {
            return null;
        }

        String trimmedMessage = message.trim();

        for (Map.Entry<Pattern, String> entry : rules.entrySet()) {
            if (entry.getKey().matcher(trimmedMessage).matches()) {
                log.info("✓ Rule matched for message: {}", trimmedMessage.substring(0, Math.min(50, trimmedMessage.length())));
                return entry.getValue();
            }
        }

        log.info("No rule matched for message: {}", trimmedMessage.substring(0, Math.min(50, trimmedMessage.length())));
        return null;
    }
}