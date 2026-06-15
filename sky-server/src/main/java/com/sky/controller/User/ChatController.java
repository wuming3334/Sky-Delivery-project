package com.sky.controller.user;

import com.sky.dto.ChatRequestDTO;
import com.sky.result.Result;
import com.sky.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/user/chat")
@Tag(name = "智能客服接口")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping("/send")
    @Operation(summary = "发送消息")
    public Result<String> send(@RequestBody ChatRequestDTO request) {
        log.info("智能客服消息: {}", request.getMessage());
        return chatService.chatSync(request);
    }
}
