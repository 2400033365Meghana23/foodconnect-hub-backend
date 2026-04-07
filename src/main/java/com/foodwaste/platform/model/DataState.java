package com.foodwaste.platform.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DataState {

    public Map<String, Object> meta = new LinkedHashMap<>();
    public List<UserEntity> users = new ArrayList<>();
    public List<Donation> donations = new ArrayList<>();
    public List<RequestRecord> requests = new ArrayList<>();
    public List<Map<String, Object>> reports = new ArrayList<>();
    public Map<String, List<Map<String, Object>>> content = new LinkedHashMap<>();
    public Map<String, Map<String, Object>> settings = new LinkedHashMap<>();
}
