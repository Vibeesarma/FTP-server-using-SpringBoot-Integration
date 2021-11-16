package com.vibee.ftpapi.springintegration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.ftp.gateway.FtpOutboundGateway;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.messaging.MessageChannel;

import java.io.File;
import java.util.List;

@Configuration
public class FTPConfig {

    @Bean
    public DefaultFtpSessionFactory defaultSetupFtp(){
        DefaultFtpSessionFactory defFtp = new DefaultFtpSessionFactory();
        defFtp.setHost("localhost");
        defFtp.setPort(2121);
        defFtp.setUsername("anonymous");
        defFtp.setPassword("");
        return defFtp;
    }

    @ServiceActivator(inputChannel = "ftpMGET")
    @Bean
    public FtpOutboundGateway getFiles(){
        FtpOutboundGateway gateway =new FtpOutboundGateway(defaultSetupFtp(),"mget","payload");
        gateway.setAutoCreateDirectory(true);
        gateway.setLocalDirectory(new File(".download/"));
        gateway.setFileExistsMode(FileExistsMode.REPLACE_IF_MODIFIED);
        gateway.setFilter(new AcceptOnceFileListFilter<>());
        gateway.setOutputChannelName("fileResults");
        return gateway;

    }

    @Bean
    public MessageChannel fileResults(){
        DirectChannel channel =new DirectChannel();
        channel.addInterceptor(tap());
        return channel;
    }

    @Bean
    public WireTap tap(){
        return new WireTap("logging");
    }

    @ServiceActivator(inputChannel = "logging")
    @Bean
    public LoggingHandler logger(){
        LoggingHandler loggingHandler = new LoggingHandler(LoggingHandler.Level.INFO);
        loggingHandler.setLogExpressionString("'Files : ' + payload");
        return loggingHandler;
    }

    @MessagingGateway(defaultRequestChannel = "ftpMGET",defaultReplyChannel = "fileResults")
    public interface GateFile{
        List<File> mget(String directory);
    }
}
