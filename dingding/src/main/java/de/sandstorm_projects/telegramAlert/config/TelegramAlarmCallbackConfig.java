package de.sandstorm_projects.telegramAlert.config;

import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.configuration.fields.TextField.Attribute;
import org.graylog2.plugin.configuration.fields.DropdownField;

import java.util.HashMap;
import java.util.Map;

public class TelegramAlarmCallbackConfig {
    private static final String ERROR_NOT_SET = "%s is mandatory and must not be empty.";

    public static ConfigurationRequest createRequest() {
        final ConfigurationRequest configurationRequest = new ConfigurationRequest();

        configurationRequest.addField(new TextField(
                Config.MESSAGE, "Message",
                "[${stream.title}](${stream_url}): ${alert_condition.title}\n" +
                "```\n" +
                "${foreach backlog message}\n" +
                "${message.message}\n\\n" +
                "${end}\n" +
                "```",
                "the content of the message",
                ConfigurationField.Optional.NOT_OPTIONAL,
                Attribute.TEXTAREA
        ));

        //configurationRequest.addField(new TextField(
        //        Config.CHAT, "useless field", "", "",
        //        ConfigurationField.Optional.NOT_OPTIONAL
        //));

        Map<String, String> parseMode = new HashMap<>(3);
        parseMode.put("text", "Text");
        parseMode.put("Markdown", "Markdown");
        //parseMode.put("HTML", "HTML");
        configurationRequest.addField(new DropdownField(
                Config.PARSE_MODE, "Parse Mode", "Markdown", parseMode,
                "text,markdown,html are all suported",
                ConfigurationField.Optional.NOT_OPTIONAL
        ));
        configurationRequest.addField(new TextField(
                Config.TOKEN, "Webhook Token", "",
                "your dingding webhook token",
                ConfigurationField.Optional.NOT_OPTIONAL,
                Attribute.IS_PASSWORD
        ));
       // configurationRequest.addField(new TextField(
       //         Config.GRAYLOG_URL, "Graylog URL", "",
       //         "URL to your Graylog web interface. Used to build links in alarm notification.",
       //         ConfigurationField.Optional.NOT_OPTIONAL
       // ));
       // configurationRequest.addField(new TextField(
        //        Config.PROXY, "Proxy", null,
        //        "Proxy address in the following format: <ProxyAddress>:<Port>",
        //        ConfigurationField.Optional.OPTIONAL
       // ));

        return configurationRequest;
    }

    public static void check(Configuration config) throws ConfigurationException {
        String[] mandatoryFields = {
            Config.MESSAGE,
            //Config.CHAT,
            Config.PARSE_MODE,
            Config.TOKEN,
            //Config.GRAYLOG_URL
        };

        for (String field : mandatoryFields) {
            if (!config.stringIsSet(field)) {
                throw new ConfigurationException(String.format(ERROR_NOT_SET, field));
            }
        }

        if (config.stringIsSet(Config.PROXY)) {
            String proxy = config.getString(Config.PROXY);
            assert proxy != null;
            String[] proxyArr = proxy.split(":");
            if (proxyArr.length != 2 || Integer.parseInt(proxyArr[1]) == 0) {
                throw new ConfigurationException("Invalid Proxy format.");
            }
        }
    }
}