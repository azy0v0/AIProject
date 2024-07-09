package com.springaichatgpt.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Media;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.*;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;
import org.springframework.ai.openai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.openai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/ai")
public class AIController {

    @Autowired
    private ChatClient chatClient;

    //chatclient
    @GetMapping("/chat")
    public String chat(@RequestParam("message") String message) {
        return this.chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    //chatclient流式访问
    @GetMapping(value = "/stream", produces = "text/html;charset=UTF-8")
    public Flux<String> chatStream(@RequestParam("message") String message) {
        Flux<String> output = chatClient.prompt()
                .user(message)
                .stream()
                .content();
        return output;
    }

    @Autowired
    private ChatModel chatModel;

    //ChatModel
    @GetMapping(value = "/chat/model", produces = "text/html;charset=UTF-8")
    public String chatModel(@RequestParam("message") String message) {
        ChatResponse response = chatModel.call(
                new Prompt(
                        message,
                        OpenAiChatOptions.builder()
                                .withModel("gpt-4-32k")
                                .withTemperature(0.8F)
                                .build()
                ));
        return response.getResult().getOutput().getContent();
    }

    @Autowired
    private OpenAiImageModel openAiImageModel;
    //文生图
    @GetMapping(value = "/text2Img", produces = "text/html;charset=UTF-8")
    public String text2Img(@RequestParam("message") String message) {
        ImageResponse response = openAiImageModel.call(
                new ImagePrompt(message,
                        OpenAiImageOptions.builder()
                                .withQuality("hd")
                                .withN(1)
                                .withHeight(1024)
                                .withWidth(1024).build())

        );
        return response.getResult().getOutput().getUrl();
    }

    @Autowired
    private OpenAiAudioSpeechModel openAiAudioSpeechModel;
    //文生语音
    @GetMapping(value = "/text2audio", produces = "text/html;charset=UTF-8")
    public String text2audit(@RequestParam("message") String message) {
        OpenAiAudioSpeechOptions speechOptions = OpenAiAudioSpeechOptions.builder()
                .withModel("tts-1")
                .withVoice(OpenAiAudioApi.SpeechRequest.Voice.ALLOY)
                .withResponseFormat(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3)
                .withSpeed(1.0f)
                .build();

        SpeechPrompt speechPrompt = new SpeechPrompt("大家下午好，我叫王大锤", speechOptions);
        SpeechResponse response = openAiAudioSpeechModel.call(speechPrompt);

        byte[] body = response.getResult().getOutput();

        //将byte[]存为MP3文件
        try {
            writeByteArrayToMp3(body, System.getProperty("user.dir"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return "ok";
    }

    public static void writeByteArrayToMp3(byte[] audioBytes, String outputFilePath) throws IOException {
        //创建FileOutputStream实例
        FileOutputStream fos = new FileOutputStream(outputFilePath + "/yuyin.mp3");
        //将字节数组写入文件
        fos.write(audioBytes);
        //关闭文件输入流
        fos.close();
    }

    @Autowired
    private OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel;
    //语音转文本
    @GetMapping(value = "/text2audio", produces = "text/html;charset=UTF-8")
    public String audio2text(@RequestParam("message") String message) {
        OpenAiAudioApi.TranscriptResponseFormat responseFormat = OpenAiAudioApi.TranscriptResponseFormat.VTT;

        OpenAiAudioTranscriptionOptions transcriptionOptions = OpenAiAudioTranscriptionOptions.builder()
                .withResponseFormat(OpenAiAudioApi.TranscriptResponseFormat.TEXT)
                .withTemperature(0f)
                .withResponseFormat(responseFormat)
                .build();

        var audioFile = new ClassPathResource("/hello.mp3");

        AudioTranscriptionPrompt transcriptionRequest = new AudioTranscriptionPrompt(audioFile, transcriptionOptions);
        AudioTranscriptionResponse response = openAiAudioTranscriptionModel.call(transcriptionRequest);

        return response.getResult().getOutput();
    }

    /**
     * 多模态
     */
    @GetMapping("/mutil")
    public String mutilModel(@RequestParam(value = "message", defaultValue = "你从这个图片中看到了什么呢") String message) throws IOException {

        // 图片的二进制流
        byte[] imageData = new ClassPathResource("/test.png").getContentAsByteArray();

        var userMessage = new UserMessage(
                message,
                List.of(new Media(MimeTypeUtils.IMAGE_PNG, imageData))); //media

        OpenAiChatOptions aiChatOptions = OpenAiChatOptions.builder()
                .withModel(OpenAiApi.ChatModel.GPT_4_TURBO_PREVIEW.getValue())
                .build();

        ChatResponse response = chatModel.call(new Prompt(userMessage, aiChatOptions));

        return response.getResult().getOutput().getContent();
    }

}
