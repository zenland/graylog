package org.graylog.plugins.sample.alerts;
import java.util.List;
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


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.floreysoft.jmte.Engine;
import com.google.inject.Inject;

import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.alarms.callbacks.*;
import org.graylog2.plugin.configuration.*;
import org.graylog2.plugin.streams.Stream;


public class SampleAlertNotification implements AlarmCallback {

    private Configuration config;
    private Engine templateEngine;

    @Inject
    public SampleAlertNotification(Engine templateEngine) {
        this.templateEngine = templateEngine;
    }
    @Override
    public void initialize(Configuration config) throws AlarmCallbackConfigurationException {
        this.config = config;
    }

    @Override
    public void call(Stream stream, AlertCondition.CheckResult result) throws AlarmCallbackException {
        final CloseableHttpClient client;
        String parseMode=config.getString(Config.PARSE_MODE);
       // String msg=config.getString(Config.MESSAGE);
        String webhook=config.getString(Config.WEBHOOK);
        //String messageTitle=config.getString(Config.MESSAGETITLE);

        client = HttpClients.createDefault();
//        logger.warning(msg);
//
        String msg=buildMessage(stream, result);
        String messageTitle=buildTitle(stream,result);
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
            str+="\"markdown\":{\"title\":\""+messageTitle+"\",\"text\":\""+msg+"\"}}";
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
//////////////////////////////////////////////////////////////////////
    private String buildMessage(Stream stream, AlertCondition.CheckResult result) {
        List<Message> backlog = getAlarmBacklog(result);
        Map<String, Object> model = getModel(stream, result, backlog);
        try {
            return templateEngine.transform(config.getString(Config.MESSAGE), model);
        } catch (Exception ex) {
            return ex.toString();
        }
    }
    private String buildTitle(Stream stream, AlertCondition.CheckResult result) {
        List<Message> backlog = getAlarmBacklog(result);
        Map<String, Object> model = getModel(stream, result, backlog);
        try {
            return templateEngine.transform(config.getString(Config.MESSAGETITLE), model);
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    private Map<String, Object> getModel(Stream stream, AlertCondition.CheckResult result, List<Message> backlog) {
        Map<String, Object> model = new HashMap<>();
        model.put("stream", stream);
        model.put("check_result", result);
        model.put("alert_condition", result.getTriggeredCondition());
        model.put("backlog", backlog);
        model.put("backlog_size", backlog.size());
        model.put("stream_url", buildStreamLink(stream));

        return model;
    }

    private List<Message> getAlarmBacklog(AlertCondition.CheckResult result) {
        final AlertCondition alertCondition = result.getTriggeredCondition();
        final List<MessageSummary> matchingMessages = result.getMatchingMessages();
        final int effectiveBacklogSize = Math.min(alertCondition.getBacklog(), matchingMessages.size());

        if (effectiveBacklogSize == 0) return Collections.emptyList();
        final List<MessageSummary> backlogSummaries = matchingMessages.subList(0, effectiveBacklogSize);
        final List<Message> backlog = new ArrayList<>(effectiveBacklogSize);
        for (MessageSummary messageSummary : backlogSummaries) {
            backlog.add(messageSummary.getRawMessage());
        }

        return backlog;
    }

    private String buildStreamLink(Stream stream) {
        return "streams/" + stream.getId() + "/messages?q=%2A&rangetype=relative&relative=3600";
    }
///////////////////////////////////////////////////////////////////////////////////
    @Override
    //主要作用是配置显示的字段
    public ConfigurationRequest getRequestedConfiguration() {
        final ConfigurationRequest configurationRequest=new ConfigurationRequest();
        
        configurationRequest.addField(new TextField(Config.MESSAGETITLE,"MessageTitle","",
                "your message title",ConfigurationField.Optional.NOT_OPTIONAL));

        configurationRequest.addField(new TextField(Config.MESSAGE,"Message",
                "this is a default template","the content of the message",
                ConfigurationField.Optional.NOT_OPTIONAL, TextField.Attribute.TEXTAREA));

        Map<String,String> parseMode=new HashMap<>(2);
        parseMode.put("Text","Text");
        parseMode.put("Markdown","Markdown");
        configurationRequest.addField(new DropdownField(Config.PARSE_MODE,"Parse Mode","Markdown",
                parseMode,"text and markdown are supported",ConfigurationField.Optional.NOT_OPTIONAL));

        configurationRequest.addField(new TextField(Config.WEBHOOK,"Webhook","",
                "your dingding webhook",ConfigurationField.Optional.NOT_OPTIONAL));

        return configurationRequest;
    }

    @Override
    public String getName() {
        return "Dingding Alert Notification";
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
