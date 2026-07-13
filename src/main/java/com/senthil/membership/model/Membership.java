package com.senthil.membership.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String memberName;

    @Email
    @NotBlank
    private String email;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ClassType classType;

    // EAGER, not the JPA default LAZY -- open-in-view is deliberately false (see
    // application.yml), and a REST controller returning entities directly means Jackson
    // serializes the response after the transaction (and Hibernate session) has already
    // closed. A lazy collection would throw LazyInitializationException on every read
    // that didn't just create the entity in the same request.
    @OneToMany(mappedBy = "membership", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<Schedule> schedules = new ArrayList<>();

    public Membership() {
    }

    public Membership(String memberName, String email, ClassType classType) {
        this.memberName = memberName;
        this.email = email;
        this.classType = classType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public ClassType getClassType() {
        return classType;
    }

    public void setClassType(ClassType classType) {
        this.classType = classType;
    }

    public List<Schedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<Schedule> schedules) {
        this.schedules.clear();
        if (schedules != null) {
            for (Schedule schedule : schedules) {
                schedule.setMembership(this);
                this.schedules.add(schedule);
            }
        }
    }
}
