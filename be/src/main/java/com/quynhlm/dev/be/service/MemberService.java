package com.quynhlm.dev.be.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.sql.Timestamp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quynhlm.dev.be.core.exception.GroupNotFoundException;
import com.quynhlm.dev.be.core.exception.MemberNotFoundException;
import com.quynhlm.dev.be.core.exception.UnknownException;
import com.quynhlm.dev.be.core.exception.UserAccountNotFoundException;
import com.quynhlm.dev.be.core.exception.UserWasAlreadyRequest;
import com.quynhlm.dev.be.enums.Role;
import com.quynhlm.dev.be.model.dto.responseDTO.GroupResponseDTO;
import com.quynhlm.dev.be.model.dto.responseDTO.MemberJoinGroupResponseDTO;
import com.quynhlm.dev.be.model.dto.responseDTO.MemberResponseDTO;
import com.quynhlm.dev.be.model.entity.Group;
import com.quynhlm.dev.be.model.entity.Member;
import com.quynhlm.dev.be.model.entity.Travel_Plan;
import com.quynhlm.dev.be.model.entity.User;
import com.quynhlm.dev.be.repositories.GroupRepository;
import com.quynhlm.dev.be.repositories.MemberRepository;
import com.quynhlm.dev.be.repositories.TravelPlanRepository;
import com.quynhlm.dev.be.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {
    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TravelPlanRepository travelPlanRepository;

    @Autowired
    private TravelPlanService travelPlanService;

    public Member requestToJoinGroup(Member member)
            throws GroupNotFoundException, MemberNotFoundException, UserAccountNotFoundException, UnknownException,
            UserWasAlreadyRequest {

        if (!groupRepository.existsById(member.getGroupId())) {
            throw new GroupNotFoundException("Group with ID " + member.getGroupId() + " not found.");
        }

        if (!userRepository.existsById(member.getUserId())) {
            throw new UserAccountNotFoundException("User with ID " + member.getUserId() + " not found.");
        }

        Optional<Member> existingMember = memberRepository.findByUser_idAndGroup_idAndStatusIn(
                member.getUserId(), member.getGroupId(), Arrays.asList("PENDING", "APPROVED"));

        if (existingMember.isPresent()) {
            throw new UserWasAlreadyRequest("User has already requested to join or is already a member.");
        }

        member.setRequest_time(new Timestamp(System.currentTimeMillis()).toString());
        member.setRole(Role.USER.name());
        member.setStatus("PENDING");
        member.setDelflag(0);
        Member saveMember = memberRepository.save(member);

        if (saveMember.getId() == null) {
            throw new UnknownException("Transaction cannot be completed!");
        }
        return saveMember;
    }

    public void insertMember(Member member) throws UserAccountNotFoundException, GroupNotFoundException {

        member.setJoin_time(new Timestamp(System.currentTimeMillis()).toString());

        if (!groupRepository.existsById(member.getGroupId())) {
            throw new GroupNotFoundException("Group with ID " + member.getGroupId() + " not found.");
        }

        if (!userRepository.existsById(member.getUserId())) {
            throw new UserAccountNotFoundException("User with ID " + member.getUserId() + " not found.");
        }

        member.setRole(Role.USER.name());
        member.setStatus("APPROVED");
        member.setDelflag(0);
        member.setEnableNotification(true);

        Member saveMember = memberRepository.save(member);

        if (saveMember.getId() == null) {
            throw new UnknownException("Transaction cannot be completed!");
        }
    }

    public Page<MemberResponseDTO> getRequestToJoinGroup(Integer groupId, String status, int page, int size)
            throws GroupNotFoundException {
        Group foundGroup = groupRepository.findGroupById(groupId);
        if (foundGroup == null) {
            throw new GroupNotFoundException("Found member with groupId " + groupId + " not found , please try again");
        }
        Pageable pageable = PageRequest.of(page, size);

        Page<Object[]> results = memberRepository.getRequestToJoinGroup(groupId, status, pageable);

        return results.map(row -> {
            MemberResponseDTO object = new MemberResponseDTO();
            object.setUserId(((Number) row[0]).intValue());
            object.setGroupId(((Number) row[1]).intValue());
            object.setMemberId(((Number) row[2]).intValue());
            object.setFullname(((String) row[3]));
            object.setAvatar_url((String) row[4]);
            object.setRole((String) row[5]);
            object.setRequest_time((String) row[6]);
            object.setJoin_time((String) row[7]);
            return object;
        });
    }

    public void setAdminGroup(Member member)
            throws GroupNotFoundException, MemberNotFoundException, UserAccountNotFoundException, UnknownException {

        member.setJoin_time(new Timestamp(System.currentTimeMillis()).toString());

        if (!groupRepository.existsById(member.getGroupId())) {
            throw new GroupNotFoundException("Group with ID " + member.getGroupId() + " not found.");
        }

        if (!userRepository.existsById(member.getUserId())) {
            throw new UserAccountNotFoundException("User with ID " + member.getUserId() + " not found.");
        }

        member.setStatus("APPROVED");
        Member saveMember = memberRepository.save(member);

        if (saveMember.getId() == null) {
            throw new UnknownException("Transaction cannot be completed!");
        }
    }

    public void deleteMember(Integer memberId) throws MemberNotFoundException {
        // Find the member by ID
        Member foundMember = memberRepository.findMemberById(memberId);
        if (foundMember == null) {
            throw new MemberNotFoundException("Member with ID " + memberId + " not found, please try another ID.");
        }

        if (foundMember.getRole().equals(Role.ADMIN.name().toString())) {
            List<Travel_Plan> travelPlans = travelPlanRepository.findByGroupUserId(foundMember.getUserId());
            log.info("Length : " + travelPlans.size());
            if (!travelPlans.isEmpty()) {
                for (Travel_Plan travel_Plan : travelPlans) {
                    travelPlanService.deleteTravelPlan(travel_Plan.getId());
                }
            }
        }

        foundMember.setDelflag(1);
        memberRepository.save(foundMember);
    }

    public void changeRoleAdmin(int adminId, int memberId, int groupId) throws UserAccountNotFoundException {

        Member foundUserMember = memberRepository.findUserMemberById(memberId, groupId);

        if (foundUserMember == null) {
            throw new UserAccountNotFoundException("Found member with " + memberId + " not found please try again");
        }

        Member memberManager = memberRepository.findUserAdminById(adminId, groupId);

        if (memberManager == null) {
            throw new UserAccountNotFoundException(
                    "Found member admin with " + adminId + " not found please try again");
        }

        if (!memberManager.getRole().equals("ADMIN")) {
            throw new UnknownException("You do not have permission to approve/reject members.");
        }

        memberManager.setStatus(Role.USER.name());

        foundUserMember.setStatus(Role.ADMIN.name());

        Member saveUserMember = memberRepository.save(foundUserMember);
        if (saveUserMember == null) {
            throw new UnknownException("Transaction cannot be completed!");
        }

        Member saveUserAdmin = memberRepository.save(memberManager);
        if (saveUserAdmin == null) {
            throw new UnknownException("Transaction cannot be completed!");
        }
    }

    public void rejectMember(int groupId, int adminId, int memberSendId)
            throws UnknownException, UserAccountNotFoundException {

        Member memberManager = memberRepository.findUserAdminById(adminId, groupId);
        if (memberManager == null) {
            new UserAccountNotFoundException("Member admin not found");
        }

        Member memberSendRequest = memberRepository.findMemberByUserId(memberSendId, groupId);
        if (memberSendRequest == null) {
            new UserAccountNotFoundException("Membe send request not found");
        }

        if (!memberManager.getRole().equals("ADMIN") && !memberManager.getRole().equals("MANAGER")) {
            throw new UnknownException("You do not have permission to approve/reject members.");
        }

        memberSendRequest.setDelflag(1);
        memberRepository.save(memberSendRequest);
    }

    // member id == managerId
    @Transactional
    public void updateMemberStatus(int groupId, int adminId, int memberSendId, String action)
            throws UnknownException, UserAccountNotFoundException {

        Member memberManager = memberRepository.findUserAdminById(adminId, groupId);
        if (memberManager == null) {
            new UserAccountNotFoundException("Member admin not found");
        }

        Member memberSendRequest = memberRepository.findMemberByUserId(memberSendId, groupId);
        if (memberSendRequest == null) {
            new UserAccountNotFoundException("Membe send request not found");
        }

        if (!memberManager.getRole().equals("ADMIN") && !memberManager.getRole().equals("MANAGER")) {
            throw new UnknownException("You do not have permission to approve/reject members.");
        }

        if (memberSendRequest.getGroupId() != groupId) {
            throw new UnknownException("Group ID does not match");
        }

        if ("approve".equalsIgnoreCase(action)) {
            memberSendRequest.setStatus("APPROVED");
            memberSendRequest.setRole(Role.USER.name());
            memberSendRequest.setJoin_time(new Timestamp(System.currentTimeMillis()).toString());
        } else {
            throw new UnknownException("Invalid action");
        }

        Member updatMember = memberRepository.save(memberSendRequest);
        if (updatMember == null) {
            throw new UnknownException("Transaction cannot be completed!");
        }
    }

    // Note
    public Page<MemberJoinGroupResponseDTO> getGroupMemberJoin(Integer userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Object[]> results = memberRepository.foundUserJoinGroup(userId, pageable);

        return results.map(row -> {
            MemberJoinGroupResponseDTO object = new MemberJoinGroupResponseDTO();
            object.setUserId(((Number) row[0]).intValue());
            object.setMemberId(((Number) row[1]).intValue());
            object.setGroupId(((Number) row[2]).intValue());
            object.setGroup_name(((String) row[3]));
            object.setAdmin_name((String) row[4]);
            object.setCover_photo((String) row[5]);
            object.setBio((String) row[6]);
            object.setStatus((String) row[7]);
            object.setRole((String) row[8]);
            object.setRequest_time((String) row[9]);
            object.setJoin_time((String) row[10]);

            List<Object[]> rawResults = memberRepository.getMemberJoinGroup(((Number) row[0]).intValue());
            List<MemberResponseDTO> responses = rawResults.stream()
                    .map(r -> new MemberResponseDTO(
                            ((Number) r[0]).intValue(),
                            ((Number) r[1]).intValue(),
                            ((Number) r[2]).intValue(),
                            (String) r[3],
                            (String) r[4],
                            (String) r[5],
                            (String) r[6],
                            (String) r[7]))
                    .collect(Collectors.toList());

            object.setUserJoined(responses);
            return object;
        });
    }

    public Page<MemberJoinGroupResponseDTO> searchGroupMemberJoin(Integer userId, String q, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Object[]> results = memberRepository.searchUserJoinGroup(userId, q, pageable);

        return results.map(row -> {
            MemberJoinGroupResponseDTO object = new MemberJoinGroupResponseDTO();
            object.setUserId(((Number) row[0]).intValue());
            object.setMemberId(((Number) row[1]).intValue());
            object.setGroupId(((Number) row[2]).intValue());
            object.setGroup_name(((String) row[3]));
            object.setAdmin_name((String) row[4]);
            object.setCover_photo((String) row[5]);
            object.setBio((String) row[6]);
            object.setStatus((String) row[7]);
            object.setRole((String) row[8]);
            object.setRequest_time((String) row[9]);
            object.setJoin_time((String) row[10]);

            List<Object[]> rawResults = memberRepository.getMemberJoinGroup(((Number) row[0]).intValue());
            List<MemberResponseDTO> responses = rawResults.stream()
                    .map(r -> new MemberResponseDTO(
                            ((Number) r[0]).intValue(),
                            ((Number) r[1]).intValue(),
                            ((Number) r[2]).intValue(),
                            (String) r[3],
                            (String) r[4],
                            (String) r[5],
                            (String) r[6],
                            (String) r[7]))
                    .collect(Collectors.toList());

            object.setUserJoined(responses);
            return object;
        });
    }

    public Page<MemberResponseDTO> getListMemberFromGroup(Integer groupId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Object[]> results = memberRepository.foundMemberJoinGroup(groupId, pageable);

        return results.map(row -> {
            MemberResponseDTO object = new MemberResponseDTO();
            object.setUserId(((Number) row[0]).intValue());
            object.setGroupId(((Number) row[1]).intValue());
            object.setMemberId(((Number) row[2]).intValue());
            object.setFullname(((String) row[3]));
            object.setAvatar_url((String) row[4]);
            object.setRole((String) row[5]);
            object.setRequest_time((String) row[6]);
            object.setJoin_time((String) row[7]);
            return object;
        });
    }

    public Page<GroupResponseDTO> foundGroupUserCreate(Integer userId, int page, int size)
            throws UserAccountNotFoundException {

        User foundUser = userRepository.getAnUser(userId);
        if (foundUser == null) {
            throw new UserAccountNotFoundException("Found user with " + userId + " not found , please try again !");
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<Object[]> results = memberRepository.fetchGroupUserCreate(userId, pageable);

        return results.map(row -> {
            GroupResponseDTO group = new GroupResponseDTO();
            group.setGroupId(((Number) row[0]).intValue());
            group.setAdminId(((Number) row[1]).intValue());
            group.setGroup_name((String) row[2]);
            group.setAdmin_name((String) row[3]);
            group.setCover_photo((String) row[4]);
            group.setBio((String) row[5]);
            group.setStatus((String) row[6]);
            group.setCreate_time((String) row[7]);
            group.setMember_count(((Number) row[8]).intValue());
            group.setUserJoined(null);

            List<Object[]> rawResults = memberRepository.getMemberJoinGroup(((Number) row[0]).intValue());
            List<MemberResponseDTO> responses = rawResults.stream()
                    .map(r -> new MemberResponseDTO(
                            ((Number) r[0]).intValue(),
                            ((Number) r[1]).intValue(),
                            ((Number) r[2]).intValue(),
                            (String) r[3],
                            (String) r[4],
                            (String) r[5],
                            (String) r[6],
                            (String) r[7]))
                    .collect(Collectors.toList());

            group.setUserJoined(responses);

            return group;
        });
    }

    public List<Integer> fetchGroupIdUserCreate(Integer userId) {
        User foundUser = userRepository.getAnUser(userId);
        if (foundUser == null) {
            throw new UserAccountNotFoundException("Found user with " + userId + " not found , please try again !");
        }

        return memberRepository.fetchGroupIdUserCreate(userId);
    }

    public List<Integer> foundUserJoinGroupId(Integer userId) {
        User foundUser = userRepository.getAnUser(userId);
        if (foundUser == null) {
            throw new UserAccountNotFoundException("Found user with " + userId + " not found , please try again !");
        }

        return memberRepository.foundUserJoinGroupId(userId);
    }

    public Page<GroupResponseDTO> searchGroupUserCreate(Integer userId, String keyworld, int page, int size)
            throws UserAccountNotFoundException {

        User foundUser = userRepository.getAnUser(userId);
        if (foundUser == null) {
            throw new UserAccountNotFoundException("Found user with " + userId + " not found , please try again !");
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<Object[]> results = memberRepository.searchGroupUserCreate(userId, keyworld, pageable);

        return results.map(row -> {
            GroupResponseDTO group = new GroupResponseDTO();
            group.setGroupId(((Number) row[0]).intValue());
            group.setAdminId(((Number) row[1]).intValue());
            group.setGroup_name((String) row[2]);
            group.setAdmin_name((String) row[3]);
            group.setCover_photo((String) row[4]);
            group.setBio((String) row[5]);
            group.setStatus((String) row[6]);
            group.setCreate_time((String) row[7]);
            group.setMember_count(((Number) row[8]).intValue());

            List<Object[]> rawResults = memberRepository.getMemberJoinGroup(((Number) row[0]).intValue());
            List<MemberResponseDTO> responses = rawResults.stream()
                    .map(r -> new MemberResponseDTO(
                            ((Number) r[0]).intValue(),
                            ((Number) r[1]).intValue(),
                            ((Number) r[2]).intValue(),
                            (String) r[3],
                            (String) r[4],
                            (String) r[5],
                            (String) r[6],
                            (String) r[7]))
                    .collect(Collectors.toList());

            group.setUserJoined(responses);

            return group;
        });
    }
}
