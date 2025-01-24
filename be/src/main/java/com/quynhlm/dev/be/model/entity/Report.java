package com.quynhlm.dev.be.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Report")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer postId;
    private Integer commentId;
    private int userId;
    private String status;
    private String mediaUrl;
    private String reason;
    private String violationType;
    private String create_time;
    private String action;
    private String response_time;
    private Integer delflag;
}
 