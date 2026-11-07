package edu.campussignal.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Check;
import edu.campussignal.dto.ProfileCreateRequest;

@Entity
@Table(name = "user_profiles", uniqueConstraints = @UniqueConstraint(
        name = "uk_profile_email", columnNames = "email"))
@Check(constraints = "study_year >= 1 and (semester is null or semester >= 1)")
public class UserProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private UserAccount user;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "college", length = 255)
    private String college;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    @NotBlank @jakarta.validation.constraints.Email
    @Column(nullable = false, length = 320)
    private String email;

    @NotBlank @Column(nullable = false)
    private String department;

    @NotBlank @Column(nullable = false)
    private String programme;

    @Min(1) @Column(name = "study_year", nullable = false)
    private int year;

    @Min(1)
    private Integer semester;

    @NotEmpty
    @ElementCollection
    @CollectionTable(name = "profile_interests", joinColumns = @JoinColumn(name = "profile_id"))
    @OrderColumn(name = "interest_order")
    @Column(name = "interest", nullable = false)
    private List<@NotBlank String> interests = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected UserProfile() {}

    public UserProfile(ProfileCreateRequest request) {
        email = request.email();
        department = request.department();
        programme = request.programme();
        year = request.year();
        semester = request.semester();
        interests = new ArrayList<>(request.interests());
    }

    public UserProfile(UserAccount user, edu.campussignal.dto.StudentProfileRequest request) {
        this.user = user; fullName = request.name(); college = request.college(); email = user.getGoogleEmail();
        department = request.department(); programme = request.programme(); year = request.year();
        graduationYear = request.graduationYear(); semester = request.semester(); interests = new ArrayList<>(request.interests());
    }

    @PrePersist
    void initializeTimestamp() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getDepartment() { return department; }
    public String getProgramme() { return programme; }
    public int getYear() { return year; }
    public Integer getSemester() { return semester; }
    public List<String> getInterests() { return List.copyOf(interests); }
    public Instant getCreatedAt() { return createdAt; }
    public UserAccount getUser() { return user; }
    public String getFullName() { return fullName; }
    public String getCollege() { return college; }
    public Integer getGraduationYear() { return graduationYear; }

    public void updateFrom(edu.campussignal.dto.ProfileCreateRequest request) {
        email = request.email();
        department = request.department();
        programme = request.programme();
        year = request.year();
        semester = request.semester();
        interests = new ArrayList<>(request.interests());
    }
    public void updateFrom(edu.campussignal.dto.StudentProfileRequest request) {
        fullName = request.name(); college = request.college(); department = request.department();
        programme = request.programme(); year = request.year(); graduationYear = request.graduationYear();
        semester = request.semester(); interests = new ArrayList<>(request.interests());
    }
}
