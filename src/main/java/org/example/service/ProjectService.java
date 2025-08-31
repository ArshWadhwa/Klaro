package org.example.service;

import org.example.entity.User;
import org.example.entity.Group;
import org.example.group.CreateProjectRequest;
import org.example.entity.Project;
import org.example.group.ProjectResponse;
import org.example.repository.ProjectRepository;
import org.example.repository.UserRepository;
import org.example.repository.GroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProjectService {


    @Autowired private UserRepository userRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private GroupRepository groupRepository;


    public ProjectResponse createProject(CreateProjectRequest request, String currentUserEmail){
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Group group = null;
        if (request.getGroupId() != null) {
            group = groupRepository.findById(request.getGroupId())
                    .orElseThrow(() -> new RuntimeException("Group not found"));
        }

        Project project = new Project();
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setCreatedBy(user);
        project.setGroup(group);

        Project saved= projectRepository.save(project);


        ProjectResponse response= new ProjectResponse();
        response.setName(saved.getName());
        response.setId(saved.getId());
        response.setDescription(saved.getDescription());
        response.setCreatedBy(user.getFullName());

        return response;


    }

    public List<ProjectResponse> getProjectsByUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Project> projects = projectRepository.findByCreatedBy(user);
        return projects.stream()
                .map(project -> {
                    ProjectResponse response = new ProjectResponse();
                    response.setId(project.getId());
                    response.setName(project.getName());
                    response.setDescription(project.getDescription());
                    response.setCreatedBy(user.getFullName());
                    response.setCreatedAt(project.getCreatedAt());
                    return response;
                })
                .collect(Collectors.toList());
    }

    public List<ProjectResponse> getAllProjects() {
        List<Project> projects = projectRepository.findAll();
        return projects.stream()
                .map(project -> {
                    ProjectResponse response = new ProjectResponse();
                    response.setId(project.getId());
                    response.setName(project.getName());
                    response.setDescription(project.getDescription());
                    response.setCreatedBy(project.getCreatedBy().getFullName());
                    response.setCreatedAt(project.getCreatedAt());
                    response.setGroupName(project.getGroup() != null ? project.getGroup().getName() : null);
                    return response;
                })
                .collect(Collectors.toList());
    }

    public List<ProjectResponse> getProjectsByGroup(Long groupId) {
        List<Project> projects = projectRepository.findByGroupId(groupId);
        return projects.stream()
                .map(project -> {
                    ProjectResponse response = new ProjectResponse();
                    response.setId(project.getId());
                    response.setName(project.getName());
                    response.setDescription(project.getDescription());
                    response.setCreatedBy(project.getCreatedBy().getFullName());
                    response.setCreatedAt(project.getCreatedAt());
                    response.setGroupName(project.getGroup() != null ? project.getGroup().getName() : null);
                    return response;
                })
                .collect(Collectors.toList());
    }

    public List<ProjectResponse> getProjectsByUserGroups(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Get all groups where user is a member
        List<Project> projects = projectRepository.findByGroupMembersContaining(user);
        
        return projects.stream()
                .map(project -> {
                    ProjectResponse response = new ProjectResponse();
                    response.setId(project.getId());
                    response.setName(project.getName());
                    response.setDescription(project.getDescription());
                    response.setCreatedBy(project.getCreatedBy().getFullName());
                    response.setCreatedAt(project.getCreatedAt());
                    response.setGroupName(project.getGroup() != null ? project.getGroup().getName() : null);
                    return response;
                })
                .collect(Collectors.toList());
    }

}
