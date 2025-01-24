package com.quynhlm.dev.be.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.quynhlm.dev.be.model.entity.Message;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Integer> {
    @Query(value = """
                        SELECT
                        m.id,
                        m.sender_id,
                        m.receiver_id,
                        m.content,
                        u.fullname,
                        u.avatar_url,
                        m.media_url,
                        m.status,
                        m.reaction,
                        m.send_time
                    FROM
                        message m
                    INNER JOIN
                        User u ON u.id = m.sender_id
                    WHERE
            m.id = :id
                        """, nativeQuery = true)
    List<Object[]> findAnMessage(@Param("id") Integer id);

    @Query(value = """
                SELECT
                m.id,
                m.sender_id,
                m.receiver_id,
                m.content,
                u.fullname,
                u.avatar_url,
                m.media_url,
                m.status,
                m.reaction,
                m.send_time
            FROM
                message m
            INNER JOIN
                User u ON u.id = m.sender_id
            WHERE
                (m.sender_id = :senderId AND m.receiver_id = :receiverId)
                OR (m.sender_id = :receiverId AND m.receiver_id = :senderId)
            """, nativeQuery = true)
    Page<Object[]> getAllMessageWithTwoUser(
            @Param("senderId") Integer senderId,
            @Param("receiverId") Integer receiverId,
            Pageable pageable);

    @Query(value = """
            select * from message WHERE id = :id
               """, nativeQuery = true)
    Message findByMessageId(@Param("id") Integer id);

    @Query(value = """
             SELECT *
             FROM message
             WHERE sender_id = :user_id OR receiver_id = :user_id
             ORDER BY send_time DESC
             LIMIT 1
            """, nativeQuery = true)
    Message lastMessage(@Param("user_id") Integer user_id);

}