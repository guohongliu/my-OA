package com.example.backend.service;

import com.example.backend.model.OrgUnit;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class OrgService {
    private final Map<Long, OrgUnit> store = new HashMap<>();
    private final AtomicLong seq = new AtomicLong(1);

    public List<OrgUnit> list() { return new ArrayList<>(store.values()); }

    public OrgUnit create(OrgUnit o) {
        long id = seq.getAndIncrement();
        o.setId(id);
        if (o.getStatus() == null) o.setStatus("ACTIVE");
        store.put(id, o);
        return o;
    }

    public OrgUnit get(Long id) { return store.get(id); }

    public OrgUnit update(Long id, OrgUnit o) {
        OrgUnit existing = store.get(id);
        if (existing == null) return null;
        o.setId(id);
        store.put(id, o);
        return o;
    }

    public boolean delete(Long id) { return store.remove(id) != null; }

    public List<OrgUnit> tree() {
        Map<Long, OrgUnit> nodes = new HashMap<>();
        for (OrgUnit o : store.values()) {
            OrgUnit copy = new OrgUnit();
            copy.setId(o.getId());
            copy.setName(o.getName());
            copy.setCode(o.getCode());
            copy.setParentId(o.getParentId());
            copy.setLevel(o.getLevel());
            copy.setStatus(o.getStatus());
            nodes.put(copy.getId(), copy);
        }
        List<OrgUnit> roots = new ArrayList<>();
        for (OrgUnit o : nodes.values()) {
            Long pid = o.getParentId();
            if (pid == null) {
                roots.add(o);
            } else {
                OrgUnit p = nodes.get(pid);
                if (p != null) p.getChildren().add(o);
                else roots.add(o);
            }
        }
        return roots;
    }
}

