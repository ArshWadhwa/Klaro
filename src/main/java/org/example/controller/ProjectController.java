package org.example.controller;

import org.example.group.CreateProjectRequest;
import org.example.group.ProjectResponse;
import org.example.repository.ProjectRepository;
import org.example.service.AuthService;
import org.example.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:3001")
@RequestMapping("/projects")
public class ProjectController {

    @Autowired private ProjectService service;
    @Autowired private AuthService authService;

    @PostMapping
    public ResponseEntity<?> createProject(
            @Validated @RequestBody CreateProjectRequest request,
            @RequestHeader("Authorization") String authHeader
            ){
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = authService.getEmailFromToken(token);
                
                // Only admins can create projects
                if (!authService.isAdmin(token)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only admins can create projects");
                }
                
                ProjectResponse response = service.createProject(request, email);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid authorization header");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getUserProjects(
            @RequestHeader("Authorization") String authHeader
            ){
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = authService.getEmailFromToken(token);
                
                // Users see only their assigned projects (projects in their groups)
                // Admins can see all projects they created
                List<ProjectResponse> projects;
                if (authService.isAdmin(token)) {
                    projects = service.getProjectsByUser(email);
                } else {
                    projects = service.getProjectsByUserGroups(email);
                }
                
                return ResponseEntity.ok(projects);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid authorization header");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllProjects(
            @RequestHeader("Authorization") String authHeader
            ){
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = authService.getEmailFromToken(token);
                
                // Only admins can view all projects
                if (!authService.isAdmin(token)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only admins can view all projects");
                }
                
                List<ProjectResponse> projects = service.getAllProjects();
                return ResponseEntity.ok(projects);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid authorization header");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/groups/{groupId}/projects")
    public ResponseEntity<?> getProjectsByGroup(
            @PathVariable Long groupId,
            @RequestHeader("Authorization") String authHeader
            ){
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = authService.getEmailFromToken(token);
                List<ProjectResponse> projects = service.getProjectsByGroup(groupId);
                return ResponseEntity.ok(projects);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid authorization header");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
