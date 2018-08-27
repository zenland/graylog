package org.graylog.plugins.sample.alerts;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.alarms.callbacks.AlarmCallback;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackConfigurationException;
import org.graylog2.plugin.alarms.callbacks.AlarmCallbackException;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.DropdownField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.streams.Stream;
import org.apache.http.entity.StringEntity;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SampleAlertNotification implements AlarmCallback {

    private Configuration config;

    @Override
    public void initialize(Configuration config) throws AlarmCallbackConfigurationException {
        this.config = config;
    }

    @Override
    public void call(Stream stream, AlertCondition.CheckResult result) throws AlarmCallbackException {
        final CloseableHttpClient client;
        String parseMode=config.getString(Config.PARSE_MODE);
        String msg=config.getString(Config.MESSAGE);
        String webhook=config.getString(Config.WEBHOOK);


        client = HttpClients.createDefault();
//        logger.warning(msg);

        HttpPost request = new HttpPost(webhook);
        request.addHeader("Content-Type", "application/json; charset=utf-8");
        String msgtype=parseMode;
        if (parseMode.equals("Markdown"))
            msgtype="markdown";
        String str="";
        str+="{\"msgtype\":\""+msgtype+"\",";
        if(parseMode.equals("text")){
            str+="\"text\":{\"content\":\""+msg+"\"}}";
        }else{
            str+="\"markdown\":{\"title\":\"markdown format\",\"text\":\""+msg+"\"}}";
        }
        String textMsg = "{ \"msgtype\": \"text\", \"text\": {\"content\": \"this is a test\"}}";
        StringEntity se = new StringEntity(str, "utf-8");
//        logger.warning(str);
        try {
            request.setEntity(se);

            HttpResponse response = client.execute(request);
            int status = response.getStatusLine().getStatusCode();
            if (status != 200) {
                String body = new BasicResponseHandler().handleResponse(response);
                String error = String.format("API request was unsuccessful (%d): %s", status, body);
//                logger.warning(error);
                throw new AlarmCallbackException(error);
            }
        } catch (IOException e) {
            String error = "API request failed: " + e.getMessage();
//            logger.warning(error);
            e.printStackTrace();
            throw new AlarmCallbackException(error);
        }
    }

    @Override
    //主要作用是配置显示的字段
    public ConfigurationRequest getRequestedConfiguration() {
        final ConfigurationRequest configurationRequest=new ConfigurationRequest();

        configurationRequest.addField(new TextField(Config.MESSAGE,"Message",
                "this is a default template","the content of the message",
                ConfigurationField.Optional.NOT_OPTIONAL, TextField.Attribute.TEXTAREA));

        Map<String,String> parseMode=new HashMap<>(2);
        parseMode.put("Text","Text");
        parseMode.put("Markdown","Markdown");
        configurationRequest.addField(new DropdownField(Config.PARSE_MODE,"Parse Mode","Markdown",
                parseMode,"text and markdown are supported",ConfigurationField.Optional.NOT_OPTIONAL));

        configurationRequest.addField(new TextField(Config.WEBHOOK,"webhook","",
                "your dingding webhook",ConfigurationField.Optional.NOT_OPTIONAL));

        return configurationRequest;
    }

    @Override
    public String getName() {
        return "Sample Alert Notification";
    }

    @Override
    public Map<String, Object> getAttributes() {
        return config.getSource();
    }

    @Override
    //TODO check if the field is filled
    public void checkConfiguration() throws ConfigurationException {

    }
}
