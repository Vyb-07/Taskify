package com.taskify.taskify.model;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * Simple Role entity. We keep it minimal: an id and a name (e.g. ROLE_USER, ROLE_ADMIN).
 */
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // role name like "ROLE_USER"
    @Column(nullable = false, unique = true)
    private String name;

    // JPA requires a no-arg constructor
    public Role() {}

    public Role(String name) {
        this.name = name;
    }

    // getters & setters
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    // equals/hashCode by id for collections and JPA identity handling
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role)) return false;
        Role role = (Role) o;
        return Objects.equals(id, role.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}