package com.example.backend.model;

import java.util.ArrayList;
import java.util.List;

public class OrgUnit {
    private Long id;
    private String name;
    private String code;
    private Long parentId;
    private Integer level;
    private String status;
    private String leaderName;
    private String createdAt;
    private String description;
    private List<OrgUnit> children = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getLeaderName() { return leaderName; }
    public void setLeaderName(String leaderName) { this.leaderName = leaderName; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<OrgUnit> getChildren() { return children; }
    public void setChildren(List<OrgUnit> children) { this.children = children; }
}

