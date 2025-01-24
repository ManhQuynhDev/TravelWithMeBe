package com.quynhlm.dev.be.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.quynhlm.dev.be.core.exception.BadResquestException;
import com.quynhlm.dev.be.core.exception.CommentNotFoundException;
import com.quynhlm.dev.be.core.exception.NotFoundException;
import com.quynhlm.dev.be.core.exception.PostNotFoundException;
import com.quynhlm.dev.be.core.exception.ReportExistingException;
import com.quynhlm.dev.be.core.exception.ReportNotFoundException;
import com.quynhlm.dev.be.core.exception.UnknownException;
import com.quynhlm.dev.be.core.exception.UserAccountNotFoundException;
import com.quynhlm.dev.be.enums.ResponseReport;
import com.quynhlm.dev.be.model.dto.requestDTO.ReportRequestDTO;
import com.quynhlm.dev.be.model.dto.responseDTO.MediaResponseDTO;
import com.quynhlm.dev.be.model.dto.responseDTO.ReportResponseDTO;
import com.quynhlm.dev.be.model.dto.responseDTO.StatisticsReportDTO;
import com.quynhlm.dev.be.model.entity.Comment;
import com.quynhlm.dev.be.model.entity.Post;
import com.quynhlm.dev.be.model.entity.Report;
import com.quynhlm.dev.be.model.entity.User;
import com.quynhlm.dev.be.repositories.CommentRepository;
import com.quynhlm.dev.be.repositories.MediaRepository;
import com.quynhlm.dev.be.repositories.PostRepository;
import com.quynhlm.dev.be.repositories.ReportRepository;
import com.quynhlm.dev.be.repositories.UserRepository;

@Service
public class ReportService {

    @Autowired
    private AmazonS3 amazonS3;

    @Value("${aws.s3.bucketName}")
    private String bucketName;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostRepository postRepository;

    public ReportResponseDTO createReport(ReportRequestDTO reportRequestDTO, MultipartFile file)
            throws UserAccountNotFoundException, PostNotFoundException, BadResquestException, ReportExistingException,
            CommentNotFoundException, UnknownException {

        User user = userRepository.getAnUser(reportRequestDTO.getUserId());
        if (user == null) {
            throw new UserAccountNotFoundException(
                    "Found user with " + reportRequestDTO.getUserId() + " not found , please try with other id");
        }

        if (reportRequestDTO.getPostId() != null) {
            Post foundPost = postRepository.getAnPost(reportRequestDTO.getPostId());
            if (foundPost == null) {
                throw new PostNotFoundException(
                        "Found post with " + reportRequestDTO.getPostId() + " not found , please try with other id");
            }
        }

        if (reportRequestDTO.getCommentId() != null) {
            Comment foundComment = commentRepository.findComment(reportRequestDTO.getCommentId());
            if (foundComment == null) {
                throw new CommentNotFoundException(
                        "Found comment with " + reportRequestDTO.getCommentId()
                                + " not found , please try with other id");
            }
        }

        if (reportRequestDTO.getCommentId() == null && reportRequestDTO.getPostId() == null) {
            throw new BadResquestException("CommentId or PostId cannot be null");
        }

        if (reportRequestDTO.getPostId() != null) {
            Report foundReport = reportRepository.foundReportExitByUserIdAndPostId(reportRequestDTO.getUserId(),
                    reportRequestDTO.getPostId());
            if (foundReport != null) {
                throw new BadResquestException("Report with userId " + reportRequestDTO.getUserId() + " and postId "
                        + reportRequestDTO.getPostId() + " already exists , please try with other id");
            }
        }

        if (reportRequestDTO.getCommentId() != null) {
            Report foundReport = reportRepository.foundReportExitByUserIdAndCommentId(reportRequestDTO.getUserId(),
                    reportRequestDTO.getCommentId());
            if (foundReport != null) {
                throw new BadResquestException("Report with userId " + reportRequestDTO.getUserId() + " and commentId "
                        + reportRequestDTO.getCommentId() + " already exists , please try with other id");
            }
        }

        Report report = new Report();
        report.setPostId(reportRequestDTO.getPostId() == null ? null : reportRequestDTO.getPostId());
        report.setCommentId(reportRequestDTO.getCommentId() == null ? null : reportRequestDTO.getCommentId());
        report.setUserId(reportRequestDTO.getUserId());
        report.setViolationType(reportRequestDTO.getViolationType());
        report.setReason(reportRequestDTO.getReason());
        report.setDelflag(0);

        try {
            if (file != null && !file.isEmpty()) {
                String fileName = file.getOriginalFilename();
                long fileSize = file.getSize();
                String contentType = file.getContentType();

                try (InputStream inputStream = file.getInputStream()) {

                    ObjectMetadata metadata = new ObjectMetadata();
                    metadata.setContentLength(fileSize);
                    metadata.setContentType(contentType);

                    amazonS3.putObject(bucketName, fileName, inputStream, metadata);

                    report.setMediaUrl(String.format("https://travle-be.s3.ap-southeast-2.amazonaws.com/%s", fileName));
                }
            }

            report.setCreate_time(new Timestamp(System.currentTimeMillis()).toString());
            report.setStatus(ResponseReport.PENDING.name());

            Report saveReport = reportRepository.save(report);
            if (saveReport.getId() == null) {
                throw new UnknownException("Transaction cannot be completed!");
            }
            return findReportById(saveReport.getId());
        } catch (IOException e) {
            throw new UnknownException("File handling error: " + e.getMessage());
        }
    }

    public void deleteReport(Integer report_id) throws ReportNotFoundException {
        Report report = reportRepository.findReportById(report_id);
        if (report == null) {
            throw new ReportNotFoundException(
                    "Found report with " + report_id + " not found , please try with other id");
        }
        report.setDelflag(1);
        reportRepository.save(report);
    }

    @Transactional
    public void handleReport(Integer userId, Integer report_id, String violationType, String action, String status)
            throws UnknownException, UserAccountNotFoundException, NotFoundException {
        Report report = reportRepository.findReportById(report_id);
        if (report == null) {
            throw new NotFoundException(
                    "Found report with " + report_id + " not found , please try with other id");
        }

        User user = userRepository.getAnUser(report.getUserId());
        if (user == null) {
            throw new UserAccountNotFoundException(
                    "Found user with " + report.getUserId() + " not found , please try with other id");
        }

        report.setViolationType(violationType);
        report.setAction(action);
        report.setStatus(status);
        report.setResponse_time(new Timestamp(System.currentTimeMillis()).toString());
        Report saveReport = reportRepository.save(report);
        if (saveReport.getId() == null) {
            throw new UnknownException("Transaction cannot be completed!");
        }
    }

    public ReportResponseDTO findReportById(Integer id) throws ReportNotFoundException {

        List<Object[]> results = reportRepository.getAnReport(id);

        if (results.isEmpty()) {
            throw new ReportNotFoundException(
                    "Id " + id + " not found or invalid data. Please try another!");
        }

        Object[] result = results.get(0);

        ReportResponseDTO report = new ReportResponseDTO();
        String type = (String) result[14];
        report.setId(((Number) result[0]).intValue());
        report.setOwnerId(((Number) result[1]).intValue());
        if (type.equals("POST")) {
            report.setPostId(((Number) result[2]).intValue());
        } else {
            report.setCommentId(((Number) result[2]).intValue());
        }
        report.setAdminId(((Number) result[3]).intValue());
        report.setFullname((String) result[4]);
        report.setAvatarUrl((String) result[5]);
        report.setContentPost((String) result[6]);
        report.setMedia_url_report((String) result[7]);
        report.setReason((String) result[8]);
        report.setViolationType((String) result[9]);
        report.setStatus((String) result[10]);
        report.setAction((String) result[11]);
        report.setCreate_time((String) result[12]);
        report.setResponseTime((String) result[13]);
        report.setType(type);

        String mediaUrlReport = (String) result[7];
        report.setMediaType(
                mediaUrlReport != null ? mediaUrlReport.matches(".*\\.(jpg|jpeg|png|gif|webp)$") ? "IMAGE" : "VIDEO"
                        : null);

        List<String> medias;
        if (type.equals("COMMENT")) {
            List<MediaResponseDTO> mediaResponseDTOs = new ArrayList<>();
            String mediaUrl = commentRepository.findMediaUrlComment(((Number) result[2]).intValue());
            if (mediaUrl != null) {
                MediaResponseDTO mediaResponseDTO = new MediaResponseDTO();
                mediaResponseDTO.setMediaUrl(mediaUrl);
                mediaResponseDTO.setMediaType(mediaUrl.matches(".*\\.(jpg|jpeg|png|gif|webp)$") ? "IMAGE" : "VIDEO");
                mediaResponseDTOs.add(mediaResponseDTO);
            }
            report.setMediaUrls(mediaResponseDTOs);
        } else {
            Post foundPost = postRepository.findAnPostById(((Number) result[2]).intValue());
            if (foundPost.getIsShare() == 0) {
                medias = mediaRepository.findMediaByPostId(foundPost.getId());
            } else {
                medias = mediaRepository.findMediaByPostId(foundPost.getPost_id());
            }
            List<MediaResponseDTO> mediaResponseDTOs = medias.stream().map(mediaUrl -> {
                MediaResponseDTO mediaResponseDTO = new MediaResponseDTO();
                mediaResponseDTO.setMediaUrl(mediaUrl);
                mediaResponseDTO
                        .setMediaType(mediaUrl.matches(".*\\.(jpg|jpeg|png|gif|webp)$") ? "IMAGE" : "VIDEO");
                return mediaResponseDTO;
            }).collect(Collectors.toList());

            report.setMediaUrls(mediaResponseDTOs);
        }
        return report;
    }

    public Page<StatisticsReportDTO> statisticsReport(int page, int size)
            throws UserAccountNotFoundException {

        Pageable pageable = PageRequest.of(page, size);

        Page<Object[]> results = reportRepository.statisticsReport(pageable);

        return results.map(row -> {
            StatisticsReportDTO report = new StatisticsReportDTO();
            report.setViolation_type((String) row[0]);
            report.setCount(((Number) row[1]).intValue());
            return report;
        });
    }

    public Page<ReportResponseDTO> getAllReport(int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        Page<Object[]> results = reportRepository.getAllReport(pageable);

        return results.map(result -> {
            String type = (String) result[14];
            ReportResponseDTO report = new ReportResponseDTO();
            report.setId(((Number) result[0]).intValue());
            report.setOwnerId(((Number) result[1]).intValue());
            if (type.equals("POST")) {
                report.setPostId(((Number) result[2]).intValue());
            } else {
                report.setCommentId(((Number) result[2]).intValue());
            }
            report.setAdminId(((Number) result[3]).intValue());
            report.setFullname((String) result[4]);
            report.setAvatarUrl((String) result[5]);
            report.setContentPost((String) result[6]);
            report.setMedia_url_report((String) result[7]);
            report.setReason((String) result[8]);
            report.setViolationType((String) result[9]);
            report.setStatus((String) result[10]);
            report.setAction((String) result[11]);
            report.setCreate_time((String) result[12]);
            report.setResponseTime((String) result[13]);
            report.setType(type);
            String mediaUrlReport = (String) result[7];

            report.setMediaType(
                    mediaUrlReport != null ? mediaUrlReport.matches(".*\\.(jpg|jpeg|png|gif|webp)$") ? "IMAGE" : "VIDEO"
                            : null);

            List<String> medias;
            if (type.equals("COMMENT")) {
                List<MediaResponseDTO> mediaResponseDTOs = new ArrayList<>();
                String mediaUrl = commentRepository.findMediaUrlComment(((Number) result[2]).intValue());
                if (mediaUrl != null) {
                    MediaResponseDTO mediaResponseDTO = new MediaResponseDTO();
                    mediaResponseDTO.setMediaUrl(mediaUrl);
                    mediaResponseDTO
                            .setMediaType(mediaUrl.matches(".*\\.(jpg|jpeg|png|gif|webp)$") ? "IMAGE" : "VIDEO");
                    mediaResponseDTOs.add(mediaResponseDTO);
                }
                report.setMediaUrls(mediaResponseDTOs);
            } else {
                Post foundPost = postRepository.findAnPostById(((Number) result[2]).intValue());
                if (foundPost.getIsShare() == 0) {
                    medias = mediaRepository.findMediaByPostId(foundPost.getId());
                } else {
                    medias = mediaRepository.findMediaByPostId(foundPost.getPost_id());
                }
                List<MediaResponseDTO> mediaResponseDTOs = medias.stream().map(mediaUrl -> {
                    MediaResponseDTO mediaResponseDTO = new MediaResponseDTO();
                    mediaResponseDTO.setMediaUrl(mediaUrl);
                    mediaResponseDTO
                            .setMediaType(mediaUrl.matches(".*\\.(jpg|jpeg|png|gif|webp)$") ? "IMAGE" : "VIDEO");
                    return mediaResponseDTO;
                }).collect(Collectors.toList());

                report.setMediaUrls(mediaResponseDTOs);
            }
            return report;
        });
    }

    public Page<ReportResponseDTO> getAllReportUserCreate(Integer userId, int page, int size) {

        User user = userRepository.getAnUser(userId);
        if (user == null) {
            throw new UserAccountNotFoundException(
                    "Found user with " + userId + " not found , please try with other id");
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<Object[]> results = reportRepository.getAllReportUserCreate(userId, pageable);

        return results.map(result -> {
            String type = (String) result[14];
            ReportResponseDTO report = new ReportResponseDTO();
            report.setId(((Number) result[0]).intValue());
            report.setOwnerId(((Number) result[1]).intValue());
            if (type.equals("POST")) {
                report.setPostId(((Number) result[2]).intValue());
            } else {
                report.setCommentId(((Number) result[2]).intValue());
            }
            report.setAdminId(((Number) result[3]).intValue());
            report.setFullname((String) result[4]);
            report.setAvatarUrl((String) result[5]);
            report.setContentPost((String) result[6]);
            report.setMedia_url_report((String) result[7]);
            report.setReason((String) result[8]);
            report.setViolationType((String) result[9]);
            report.setStatus((String) result[10]);
            report.setAction((String) result[11]);
            report.setCreate_time((String) result[12]);
            report.setResponseTime((String) result[13]);
            report.setType(type);
            String mediaUrlReport = (String) result[7];

            report.setMediaType(
                    mediaUrlReport != null ? mediaUrlReport.matches(".*\\.(jpg|jpeg|png|gif|webp)$") ? "IMAGE" : "VIDEO"
                            : null);

            List<String> medias;
            if (type.equals("COMMENT")) {
                List<MediaResponseDTO> mediaResponseDTOs = new ArrayList<>();
                String mediaUrl = commentRepository.findMediaUrlComment(((Number) result[2]).intValue());
                if (mediaUrl != null) {
                    MediaResponseDTO mediaResponseDTO = new MediaResponseDTO();
                    mediaResponseDTO.setMediaUrl(mediaUrl);
                    mediaResponseDTO
                            .setMediaType(mediaUrl.matches(".*\\.(jpg|jpeg|png|gif|webp)$") ? "IMAGE" : "VIDEO");
                    mediaResponseDTOs.add(mediaResponseDTO);
                }
                report.setMediaUrls(mediaResponseDTOs);
            } else {
                Post foundPost = postRepository.findAnPostById(((Number) result[2]).intValue());
                if (foundPost.getIsShare() == 0) {
                    medias = mediaRepository.findMediaByPostId(foundPost.getId());
                } else {
                    medias = mediaRepository.findMediaByPostId(foundPost.getPost_id());
                }
                List<MediaResponseDTO> mediaResponseDTOs = medias.stream().map(mediaUrl -> {
                    MediaResponseDTO mediaResponseDTO = new MediaResponseDTO();
                    mediaResponseDTO.setMediaUrl(mediaUrl);
                    mediaResponseDTO
                            .setMediaType(mediaUrl.matches(".*\\.(jpg|jpeg|png|gif|webp)$") ? "IMAGE" : "VIDEO");
                    return mediaResponseDTO;
                }).collect(Collectors.toList());

                report.setMediaUrls(mediaResponseDTOs);
            }
            return report;
        });
    }
}
