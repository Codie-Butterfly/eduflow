package com.eduflow.entity.academic;

import com.eduflow.entity.base.BaseEntity;
import com.eduflow.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "parents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Parent extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String occupation;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "emergency_contact")
    private String emergencyContact;

    @Column(name = "relationship_to_student")
    private String relationshipToStudent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Student> children = new HashSet<>();

    public void addChild(Student student) {
        children.add(student);
        student.setParent(this);
    }

    public void removeChild(Student student) {
        children.remove(student);
        student.setParent(null);
    }
}
