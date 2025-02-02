package com.quynhlm.dev.be.service;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.quynhlm.dev.be.core.exception.AlreadyExistsException;
import com.quynhlm.dev.be.core.exception.NotFoundException;
import com.quynhlm.dev.be.core.exception.UnknownException;
import com.quynhlm.dev.be.core.exception.UserAccountNotFoundException;
import com.quynhlm.dev.be.model.dto.responseDTO.ActivityResponseDTO;
import com.quynhlm.dev.be.model.entity.Activities;
import com.quynhlm.dev.be.model.entity.Travel_Plan;
import com.quynhlm.dev.be.model.entity.User;
import com.quynhlm.dev.be.repositories.ActivitiesRepository;
import com.quynhlm.dev.be.repositories.TravelPlanRepository;
import com.quynhlm.dev.be.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ActivitiesService {

    @Autowired
    private ActivitiesRepository activitiesRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TravelPlanRepository travelPlanRepository;

    // Create activity
    public Activities createActivities(Activities activities)
            throws NotFoundException,AlreadyExistsException,
            UserAccountNotFoundException, UnknownException {// Status
        // 1 : 0
        User foundUser = userRepository.getAnUser(activities.getUser_id());
        if (foundUser == null) {
            throw new UserAccountNotFoundException("Found user with id not found please try again");
        }

        Travel_Plan foundPlan = travelPlanRepository.getAnTravel_Plan(activities.getPlanId());
        if (foundPlan == null) {
            throw new NotFoundException("Found group with id not found please try again");
        }
        activities.setCreate_time(new Timestamp(System.currentTimeMillis()).toString());
        activities.setDelflag(0);
        Activities saveActivity = activitiesRepository.save(activities);
        if (saveActivity.getId() == null) {
            throw new UnknownException("Transaction cannot complete!");
        }
        return saveActivity;
    }

    public void deleteActivities(int id) throws NotFoundException {
        Activities foundActivity = activitiesRepository.findActivities(id);
        if (foundActivity == null) {
            throw new NotFoundException("Activity with id " + id + " not found. Please try another!");
        }
        foundActivity.setDelflag(1);
        activitiesRepository.save(foundActivity);
    }

    public ActivityResponseDTO getAnActivity(int id) throws NotFoundException {
        Activities foundActivity = activitiesRepository.findActivities(id);
        if (foundActivity == null) {
            throw new NotFoundException("Activity with id " + id + " not found. Please try another!");
        }

        List<Object[]> results = activitiesRepository.findActivitiesWithId(id);

        if (results.isEmpty()) {
            throw new NotFoundException(
                    "Id " + id + " not found or invalid data. Please try another!");
        }

        Object[] result = results.get(0);
        ActivityResponseDTO activityResponseDTO = new ActivityResponseDTO();
        activityResponseDTO.setId(((Number) result[0]).intValue());
        activityResponseDTO.setUserId(((Number) result[1]).intValue());
        activityResponseDTO.setPlanId(((Number) result[2]).intValue());
        activityResponseDTO.setLocationId(((Number) result[3]).intValue());
        activityResponseDTO.setActivity_name((String) result[4]);
        activityResponseDTO.setFullname((String) result[5]);
        activityResponseDTO.setAvatarUrl((String) result[6]);
        activityResponseDTO.setPlanName((String) result[7]);
        activityResponseDTO.setDescription((String) result[8]);
        activityResponseDTO.setTime((String) result[9]);
        activityResponseDTO.setCost((Double) result[10]);
        activityResponseDTO.setStatus((String) result[11]);
        activityResponseDTO.setCreate_time((String) result[12]);
        return activityResponseDTO;
    }

    public void updateActivities(int id, Activities activities)
            throws NotFoundException, AlreadyExistsException, UnknownException {

        Activities foundActivity = activitiesRepository.findActivities(id);
        if (foundActivity == null) {
            throw new NotFoundException("Activity with id " + id + " not found. Please try another!");
        }

        Activities isExits = activitiesRepository.findByNameAndPlanId(activities.getName(),
                activities.getPlanId());

        if (isExits != null) {
            throw new AlreadyExistsException(
                    "Activity with name " + activities.getName() + " already exist !. Please try another!");
        }

        foundActivity.setName(activities.getName());
        foundActivity.setDescription(activities.getDescription());
        foundActivity.setTime(activities.getTime());
        foundActivity.setLocationId(activities.getLocationId());
        foundActivity.setStatus(activities.getStatus());
        foundActivity.setCost(activities.getCost());
        foundActivity.setPlanId(activities.getPlanId());

        Activities saveActivity = activitiesRepository.save(foundActivity);
        if (saveActivity.getId() == null) {
            throw new UnknownException("Transaction cannot be completed!");
        }
    }

    public Page<Activities> searchActivitiesByName(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return activitiesRepository.searchActivitiesByName(keyword, pageable);
    }

    // Get all data
    public Page<Activities> getListData(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return activitiesRepository.findAll(pageable);
    }

    // Get all data with planid
    public Page<Activities> getActivitiesWithPlanId(int planId, int page, int size) throws NotFoundException {

        Travel_Plan foundPlan = travelPlanRepository.getAnTravel_Plan(planId);
        if (foundPlan == null) {
            throw new NotFoundException("Plan with id  " + planId + " not found please try again");
        }

        Pageable pageable = PageRequest.of(page, size);
        return activitiesRepository.findAllActivitiesWithPlanId(planId, pageable);
    }
}
