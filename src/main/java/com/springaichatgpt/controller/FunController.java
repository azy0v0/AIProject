package com.springaichatgpt.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;

public class FunController {
    private final DashScopeAiChatModel dashScopeAiChatModel;

    /**
     * 调用自定义函数回答用户的提问
     *
     * @param prompt       用户的提问
     * @param functionName 函数名称（bean的名称，类名小写）
     * @return SSE流式响应
     */
    @GetMapping(value = "chat/stream/function", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStreamWithFunction(@RequestParam String prompt, @RequestParam String functionName) {
        return ChatClient.create(kimiAiChatModel).prompt()
                .messages(new UserMessage(prompt))
                // spring ai会从已注册为bean的function中查找函数，将它添加到请求中。如果成功触发就会调用函数
                .functions("DocumentAnalyzerFunction")
                .stream()
                .chatResponse()
                .map(chatResponse -> ServerSentEvent.builder(toJson(chatResponse))
                        .event("message")
                        .build());
    }

}
