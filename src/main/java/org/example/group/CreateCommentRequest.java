package org.example.group;

import org.example.entity.Issue;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Data;
import org.apache.catalina.User;

@Data
public class CreateCommentRequest {


    String content;





}
