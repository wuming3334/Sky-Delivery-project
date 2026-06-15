package com.sky.service;

import com.sky.dto.ChatRequestDTO;
import com.sky.vo.ChatResponseVO;
import reactor.core.publisher.Flux;

import com.sky.result.Result;

public interface ChatService {
    Flux<ChatResponseVO> chat(ChatRequestDTO request);
    Result<String> chatSync(ChatRequestDTO request);
}
