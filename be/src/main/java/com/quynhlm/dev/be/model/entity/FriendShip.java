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
@Table(name = "FriendShip")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FriendShip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Integer id;
    private Integer userSendId;
    private Integer userReceivedId;
    private String status;
    private String create_time;

    @Override
    public String toString() {
        return "Friendship{" +
                "id=" + id +
                ", userSendId=" + userSendId +
                ", userReceivedId=" + userReceivedId +
                ", status='" + status + '\'' +
                ", create_time='" + create_time + '\'' +
                '}';
    }
}
