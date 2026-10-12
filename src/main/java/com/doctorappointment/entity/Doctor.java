package com.doctorappointment.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "doctors")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    private String phone;

    private String specialization;

    private String qualification;

    private Integer experience;

    @Column(name = "consultation_fee")
    private Double consultationFee = 500.0;

    @Column(name = "verification_status")
    private String verificationStatus = "APPROVED";

    private Double rating = 5.0;

    @Column(name = "review_count")
    private Integer reviewCount = 0;

    public Doctor() {
    }

    public Doctor(String name, String email, String phone,
                  String specialization, String qualification,
                  Integer experience) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.specialization = specialization;
        this.qualification = qualification;
        this.experience = experience;
        this.consultationFee = 500.0;
        this.verificationStatus = "APPROVED";
        this.rating = 5.0;
        this.reviewCount = 0;
    }

    public Doctor(String name, String email, String phone,
                  String specialization, String qualification,
                  Integer experience, Double consultationFee) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.specialization = specialization;
        this.qualification = qualification;
        this.experience = experience;
        this.consultationFee = consultationFee != null ? consultationFee : 500.0;
        this.verificationStatus = "APPROVED";
        this.rating = 5.0;
        this.reviewCount = 0;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getQualification() {
        return qualification;
    }

    public void setQualification(String qualification) {
        this.qualification = qualification;
    }

    public Integer getExperience() {
        return experience;
    }

    public void setExperience(Integer experience) {
        this.experience = experience;
    }

    public Double getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(Double consultationFee) {
        this.consultationFee = consultationFee;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }
}
