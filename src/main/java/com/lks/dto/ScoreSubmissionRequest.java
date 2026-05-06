package com.lks.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ScoreSubmissionRequest {
    private Integer vId;
    private String videoName;
    private String videoType;
    private String vImg;
    private Integer releaseYear;
    private List<Integer> genres;
    private List<String> countries;
    private List<?> answers;
    private final Map<String, Object> unsupportedFields = new LinkedHashMap<>();

    public Integer getvId() {
        return vId;
    }

    public void setvId(Integer vId) {
        this.vId = vId;
    }

    public String getVideoName() {
        return videoName;
    }

    public void setVideoName(String videoName) {
        this.videoName = videoName;
    }

    public String getVideoType() {
        return videoType;
    }

    public void setVideoType(String videoType) {
        this.videoType = videoType;
    }

    public String getvImg() {
        return vImg;
    }

    public void setvImg(String vImg) {
        this.vImg = vImg;
    }

    public Integer getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(Integer releaseYear) {
        this.releaseYear = releaseYear;
    }

    public List<Integer> getGenres() {
        return genres;
    }

    public void setGenres(List<Integer> genres) {
        this.genres = genres;
    }

    public List<String> getCountries() {
        return countries;
    }

    public void setCountries(List<String> countries) {
        this.countries = countries;
    }

    public List<?> getAnswers() {
        return answers;
    }

    public void setAnswers(List<?> answers) {
        this.answers = answers;
    }

    @JsonAnySetter
    public void addUnsupportedField(String fieldName, Object value) {
        unsupportedFields.put(fieldName, value);
    }

    @JsonIgnore
    public Map<String, Object> getUnsupportedFields() {
        return unsupportedFields;
    }
}
