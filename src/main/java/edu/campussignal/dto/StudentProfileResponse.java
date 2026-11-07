package edu.campussignal.dto;
import java.util.List;
import edu.campussignal.entity.UserProfile;
public record StudentProfileResponse(String name, String email, String college, String department, String programme, int year, Integer graduationYear, Integer semester, List<String> interests) {
    public static StudentProfileResponse from(UserProfile value) { return new StudentProfileResponse(value.getFullName(), value.getEmail(), value.getCollege(), value.getDepartment(), value.getProgramme(), value.getYear(), value.getGraduationYear(), value.getSemester(), value.getInterests()); }
}
