package com.quynhlm.dev.be.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.quynhlm.dev.be.core.exception.UnknownException;
import com.quynhlm.dev.be.model.dto.requestDTO.MessageRequestDTO;
import com.quynhlm.dev.be.model.dto.responseDTO.UserMessageGroupResponseDTO;
import com.quynhlm.dev.be.model.entity.Member;
import com.quynhlm.dev.be.model.entity.MessageGroup;
import com.quynhlm.dev.be.model.entity.MessageStatus;
import com.quynhlm.dev.be.repositories.MemberRepository;
import com.quynhlm.dev.be.repositories.MessageGroupRepository;
import com.quynhlm.dev.be.repositories.MessageStatusRepositoty;

@Service
public class MessageGroupService {

    @Autowired
    private MessageGroupRepository messageGroupRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AmazonS3 amazonS3;

    @Value("${aws.s3.bucketName}")
    private String bucketName;

    @Autowired
    private MessageStatusRepositoty messageStatusRepositoty;

    public Page<UserMessageGroupResponseDTO> getAllListData(Integer groupId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Object[]> results = messageGroupRepository.findAllMessageGroup(groupId, pageable);

        return results.map(row -> {
            UserMessageGroupResponseDTO object = new UserMessageGroupResponseDTO();
            object.setId(((Number) row[0]).intValue());
            object.setUserSendId(((Number) row[1]).intValue());
            object.setGroupId(((Number) row[2]).intValue());
            object.setContent(((String) row[3]));
            object.setContent(((String) row[4]));
            object.setFullname(((String) row[5]));
            object.setAvatarUrl((String) row[6]);
            object.setStatus(((Boolean) row[7]));
            object.setSend_time((String) row[8]);
            return object;
        });
    }

    public UserMessageGroupResponseDTO sendMessage(MessageRequestDTO messageRequestDTO) {
        try {
            // Đặt thời gian gửi tin nhắn
            messageRequestDTO.getMessage().setSendTime(new Timestamp(System.currentTimeMillis()).toString());

            // Xử lý file (nếu có)
            String mediaUrl = null;
            if (messageRequestDTO.getFile() != null && !messageRequestDTO.getFile().isEmpty()) {
                // Xử lý việc lưu file base64 và upload lên S3
                mediaUrl = handleFileUpload(messageRequestDTO.getFile());
            }

            System.out.println("Media url :" + mediaUrl);

            return isSuccess(messageRequestDTO.getMessage(), messageRequestDTO.getStatus(), mediaUrl);
        } catch (Exception e) {
            // Xử lý lỗi
            throw new UnknownException("Error sending message: " + e.getMessage());
        }
    }

    private String handleFileUpload(String fileBase64) {
        try {
            String[] parts = fileBase64.split(",");
            String header = parts[0];
            String fileData = parts[1];

            String fileType = header.split(";")[0].split(":")[1];

            if (!fileType.startsWith("image/")) {
                throw new IllegalArgumentException("Invalid file type. Only images are allowed.");
            }

            byte[] decodedBytes = Base64.getDecoder().decode(fileData);

            String fileName = UUID.randomUUID().toString() + "." + fileType.split("/")[1];

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(decodedBytes.length);
            metadata.setContentType(fileType);

            try (InputStream inputStream = new ByteArrayInputStream(decodedBytes)) {
                amazonS3.putObject(bucketName, fileName, inputStream, metadata);
            }
            return String.format("https://%s.s3.amazonaws.com/%s", bucketName, fileName);
        } catch (IOException e) {
            throw new UnknownException("Error handling Base64 file: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new UnknownException("Invalid file type: " + e.getMessage());
        }
    }

    public UserMessageGroupResponseDTO isSuccess(MessageGroup message, Boolean status, String mediaUrl) {
        try {
            // Lưu tin nhắn vào cơ sở dữ liệu
            message.setMediaUrl(mediaUrl);
            MessageGroup saveMessage = messageGroupRepository.save(message);
            if (saveMessage.getId() == null) {
                throw new UnknownException("Transaction cannot be completed!");
            }

            List<Member> users = memberRepository.findApprovedMembersByGroupId(message.getGroupId(),
                    message.getUserSendId());

            for (Member member : users) {
                MessageStatus messageStatus = new MessageStatus();
                messageStatus.setUserId(member.getUserId());
                messageStatus.setMessageId(saveMessage.getId());
                messageStatus.setStatus(status);
                messageStatusRepositoty.save(messageStatus);
            }

            UserMessageGroupResponseDTO userMessageResponseDTO = getAnMessage(saveMessage.getId());

            return userMessageResponseDTO;
        } catch (Exception e) {
            // Xử lý lỗi trong quá trình lưu
            throw new UnknownException("Error saving message: " + e.getMessage());
        }
    }

    public UserMessageGroupResponseDTO getAnMessage(Integer id) {
        List<Object[]> results = messageGroupRepository.findAnMessage(id);

        Object[] result = results.get(0);

        Integer message_id = ((Number) result[0]).intValue();
        Integer user_send_id = ((Number) result[1]).intValue();
        Integer group_id = ((Number) result[2]).intValue();
        String content = (String) result[3];
        String mediaUrl = (String) result[4];
        String fullname = (String) result[5];
        String avatar = (String) result[6];
        Boolean status = (Boolean) result[7];
        String send_time = (String) result[8];

        return new UserMessageGroupResponseDTO(message_id, user_send_id, group_id, content, mediaUrl, fullname, avatar,
                status, send_time);
    }

    public void deleteMessage(Integer messageId) {
        MessageGroup foundMessage = messageGroupRepository.findByMessageId(messageId);

        if (foundMessage != null) {
            messageGroupRepository.delete(foundMessage);
        }
    }
}
