package com.quynhlm.dev.be.model.dto.requestDTO;

import com.quynhlm.dev.be.model.entity.MessageGroup;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MessageRequestDTO {
    private MessageGroup message;
    private Boolean status;
    private String file;
}
