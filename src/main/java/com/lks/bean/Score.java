package com.lks.bean;

import java.io.Serializable;
import java.util.List;



@SuppressWarnings("serial")
public class Score implements Serializable,Cloneable{

    private Integer id ;

    private Integer uId ;

    private Integer vId;

    private String videoName ;
    private String videoType;
    private String vImg;

    private String score ;
    private Integer releaseYear;
    private List<Integer> genres;
    private List<String> countries;
    private Double popularity;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getuId() {
        return uId;
    }

    public void setuId(Integer uId) {
        this.uId = uId;
    }

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

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
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
    public Double getPopularity() {
        return popularity;
    }

    public void setPopularity(Double popularity) {
        this.popularity = popularity;
    }

    @Override
    public String toString() {
        return "Score{" +
                "id=" + id +
                ", uId=" + uId +
                ", vId=" + vId +
                ", videoName='" + videoName + '\'' +
                ", videoType='" + videoType + '\'' +
                ", vImg='" + vImg + '\'' +
                ", score='" + score + '\'' +
                ", releaseYear='" + releaseYear + '\'' +
                ", genres='" + genres + '\'' +
                ", countries='" + countries + '\'' +
                ", popularity='" + popularity + '\'' +
                '}';
    }
}
